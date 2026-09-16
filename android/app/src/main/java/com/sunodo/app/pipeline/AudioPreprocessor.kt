package com.sunodo.app.pipeline

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.nio.ByteOrder

/**
 * docs/blueprint.md's AudioPreprocessor module: trims/chunks audio into
 * <=30s windows to match the on-device model's batch limit, and — as of
 * this fix — prepares raw audio into the exact PCM16/16kHz/mono format
 * LlmInferenceSession.addAudio() expects (confirmed via live web research
 * while fixing that call — see LlmPacketExtractor.kt and Gemma's own audio
 * docs, ai.google.dev/gemma/docs/capabilities/audio: mono, and clips up to
 * 30s recommended, which is also why chunkBoundaries defaults to 30s).
 *
 * `chunkBoundaries`, `resampleLinear`, `downmixToMono`, and
 * `shortArrayToLittleEndianBytes` are pure arithmetic with no Android
 * dependency — all four were pulled out, compiled standalone, and run
 * through concrete test cases in the sandbox that built this stage before
 * being copied here unchanged (chunkBoundaries: 6 cases in Stage 3; the
 * other three: 8 cases covering downsampling, upsampling, empty input,
 * stereo downmix, mono passthrough, and little-endian byte order including
 * negative samples, in this fix). `getDurationMs` and `decodeToPcm16` are
 * real Android media APIs but weren't exercised against a real audio file
 * or device from this sandbox — treat those two the same as the model
 * integration in this package: matches the standard pattern, not
 * device-verified.
 */
object AudioPreprocessor {

    data class DecodedPcm(val samples: ShortArray, val sampleRateHz: Int, val channelCount: Int)

    fun getDurationMs(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    }

    /** Verified pure function — see the class doc comment. */
    fun chunkBoundaries(durationMs: Long, maxChunkMs: Long = 30_000L): List<LongRange> {
        if (durationMs <= 0L) return emptyList()
        val boundaries = mutableListOf<LongRange>()
        var start = 0L
        while (start < durationMs) {
            val end = minOf(start + maxChunkMs, durationMs)
            boundaries.add(start until end)
            start = end
        }
        return boundaries
    }

    /**
     * The full prep pipeline for LlmInferenceSession.addAudio(): decode
     * whatever compressed format the shared file is in, downmix to mono if
     * needed, resample to 16kHz if needed, and serialize to little-endian
     * PCM16 bytes.
     */
    fun preparePcm16Mono16kHz(context: Context, uri: Uri): ByteArray {
        val decoded = decodeToPcm16(context, uri)
        val mono = downmixToMono(decoded.samples, decoded.channelCount)
        val resampled = resampleLinear(mono, decoded.sampleRateHz, 16_000)
        return shortArrayToLittleEndianBytes(resampled)
    }

    /** Verified pure function — see the class doc comment. */
    fun resampleLinear(input: ShortArray, inputRate: Int, outputRate: Int): ShortArray {
        if (inputRate == outputRate || input.isEmpty()) return input
        val outputLength = ((input.size.toLong() * outputRate) / inputRate).toInt()
        if (outputLength <= 0) return ShortArray(0)
        val output = ShortArray(outputLength)
        val ratio = input.size.toDouble() / outputLength.toDouble()
        for (i in 0 until outputLength) {
            val srcPos = i * ratio
            val srcIndex = srcPos.toInt().coerceIn(0, input.size - 1)
            val frac = srcPos - srcIndex
            val s0 = input[srcIndex]
            val s1 = if (srcIndex + 1 < input.size) input[srcIndex + 1] else s0
            output[i] = (s0 + (s1 - s0) * frac).toInt().toShort()
        }
        return output
    }

    /** Verified pure function — see the class doc comment. */
    fun downmixToMono(input: ShortArray, channelCount: Int): ShortArray {
        if (channelCount <= 1) return input
        val frames = input.size / channelCount
        val output = ShortArray(frames)
        for (i in 0 until frames) {
            var sum = 0
            for (c in 0 until channelCount) sum += input[i * channelCount + c]
            output[i] = (sum / channelCount).toShort()
        }
        return output
    }

    /** Verified pure function — see the class doc comment. */
    fun shortArrayToLittleEndianBytes(samples: ShortArray): ByteArray {
        val bytes = ByteArray(samples.size * 2)
        for (i in samples.indices) {
            val v = samples[i].toInt()
            bytes[i * 2] = (v and 0xFF).toByte()
            bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Standard synchronous MediaCodec decode loop: demux the first audio
     * track with MediaExtractor, decode it, and concatenate the PCM16
     * output — plus the sample rate and channel count downstream steps
     * need to normalize it.
     */
    fun decodeToPcm16(context: Context, uri: Uri): DecodedPcm {
        val extractor = MediaExtractor()
        extractor.setDataSource(context, uri, null)

        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val candidate = extractor.getTrackFormat(i)
            val mime = candidate.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = candidate
                break
            }
        }
        require(trackIndex >= 0 && format != null) { "No audio track found in $uri" }
        extractor.selectTrack(trackIndex)

        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)

        val mime = format.getString(MediaFormat.KEY_MIME)!!
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val bufferInfo = MediaCodec.BufferInfo()
        val output = mutableListOf<Short>()
        var sawInputEos = false
        var sawOutputEos = false

        try {
            while (!sawOutputEos) {
                if (!sawInputEos) {
                    val inputIndex = codec.dequeueInputBuffer(10_000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)!!
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEos = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                if (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex)!!
                    val chunk = ShortArray(bufferInfo.size / 2)
                    outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(chunk)
                    chunk.forEach { output.add(it) }
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        sawOutputEos = true
                    }
                }
            }
        } finally {
            codec.stop()
            codec.release()
            extractor.release()
        }

        return DecodedPcm(output.toShortArray(), sampleRate, channelCount)
    }
}
