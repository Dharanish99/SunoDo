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
 * <=30s windows to match the on-device model's batch limit.
 *
 * `chunkBoundaries` is pure arithmetic with no Android dependency — it was
 * pulled out, compiled, and run standalone with a modern Kotlin compiler in
 * the sandbox that built this stage (duration 0, sub-chunk, exact-chunk,
 * one-over, multi-chunk cases), and its logic here is unchanged from that
 * checked version. `getDurationMs` and `decodeToPcm16` are real Android
 * media APIs but weren't exercised against a real audio file or device from
 * this sandbox — they follow the standard MediaMetadataRetriever /
 * MediaExtractor+MediaCodec synchronous-decode patterns, but treat them as
 * needing a real-device pass before depending on them, the same way the
 * model-integration files in this package do.
 */
object AudioPreprocessor {

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
     * Standard synchronous MediaCodec decode loop: demux the first audio
     * track with MediaExtractor, decode it, and concatenate the PCM16
     * output. Needed if a model API ends up wanting raw samples rather than
     * a file handle — kept here so that decision doesn't leak into the
     * extractor classes.
     */
    fun decodeToPcm16(context: Context, uri: Uri): ShortArray {
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

        return output.toShortArray()
    }
}
