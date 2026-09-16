# SunoDo — Android app (Track A)

The real Android Studio project. This is source code meant to be opened and built in Android Studio — it has **not** been compiled by the assistant that wrote it, because this sandbox has no route to Google's Maven repository (`google()`) or an Android SDK/emulator. What it does have is a modern Kotlin compiler (2.0.21, pulled from GitHub's release assets), which was used to check every file in this module for real syntax and structural errors with everything outside the missing Android/Compose/Room classpath filtered out. One genuine bug was caught and fixed that way — see "What was actually verified" below.

## How to open it

1. Android Studio (Ladybird/2024.2 or newer recommended for Kotlin 2.0 + the Compose compiler Gradle plugin).
2. Open the `android/` folder as a project — not the repo root.
3. Let Gradle sync. It needs internet access to `google()` and `mavenCentral()` the first time, to pull AndroidX, Compose, and Room.
4. Run the `app` configuration on a device or emulator running API 26+.

## What this module actually contains

| Layer | Status |
|---|---|
| Gradle project (AGP 8.6, Kotlin 2.0.21, Compose compiler plugin, KSP) | Real, complete |
| `AndroidManifest.xml` — `ShareReceiverActivity` registered for `ACTION_SEND` + any audio MIME type, zero `<uses-permission>` entries | Real, complete |
| Room schema — `VoiceNote` + `Packet` entities, `PacketDao` (including a transactional insert), `SunoDoDatabase` | Real, complete — the ViewModel does a genuine write-then-read round trip through it, not just an in-memory model |
| Compose UI — theme (shared color tokens with `demo/sunodo_demo.html`), `HomeScreen`, `ProcessingScreen` (with device-tier badge), `ActionCardScreen` (with an empty state once every card's dismissed), `PacketCard` (swipe-to-dismiss via Material3's `SwipeToDismissBox`, plus the original × button for anyone who doesn't swipe), `ErrorScreen` | Real, complete — `SwipeToDismissBox`'s exact parameter names have shifted across Compose Material3 releases while experimental, so treat that one piece the same as the MediaPipe integration: matches the documented shape, not checked against a live Gradle sync |
| `ShareReceiverActivity` — reads the shared audio URI and its display name via the transient URI grant on the incoming Intent, and reports a genuine error (rather than silently substituting sample data) if the Intent didn't carry a readable audio URI | Real, complete |
| `DeviceTierDetector` (§3.4's RAM check, cached) | Real, complete — no model file needed for this one |
| `AudioPreprocessor.chunkBoundaries` | Real, complete, and actually verified — see "What was actually verified" |
| `AudioPreprocessor.getDurationMs` / `decodeToPcm16` | Real Android media APIs, standard patterns, **not exercised on a device** |
| `HighTierPacketExtractor` / `BudgetTierPacketExtractor` / `LlmPacketExtractor` (MediaPipe LLM Inference) | Text-generation calls and the audio-ingestion API are both now sourced from Google's official docs, confirmed via live web research (see "Open questions" below) rather than guessed. High-tier audio is the best-supported path; budget-tier audio has a newly-surfaced, more fundamental problem (Gemma 3 1B likely has no audio-capable checkpoint at all) — text input is unaffected on both tiers |
| `PacketExtractorFactory` | Real — picks a tier, falls back to the Stage 2 stub if that tier's model file isn't on the device (model files are too large to commit here). Whether it fell back is now surfaced in the UI (`ModelStatusBanner`) — see the note below the table |
| `OSActionBridge` / `PacketActionHandler` (native Calendar/Reminder/Reply intents) | Real, complete — `ACTION_INSERT` opens the calendar app's own "new event" screen (no calendar permission needed), `ACTION_SEND` hands reminders and replies to whatever app the user picks via the system chooser, with a clipboard-copy fallback if nothing on the device can handle either intent |

Sharing a real voice note from WhatsApp to SunoDo today will genuinely launch `ShareReceiverActivity`, genuinely read the file's name and duration, genuinely run it through `DeviceTierDetector`, land on `PacketExtractorFactory` — which, until a real `.task` model file is pushed onto the device (see `ModelPaths.kt`), gracefully falls back to the stub rather than crashing — and every card's action button now fires a real native intent, not a placeholder.

**A note on that fallback, from actually running this on a device:** the device-tier badge on `ProcessingScreen` is a real capability check and will correctly say something like "High-tier device · Gemma-3n E2B" even when there's no model file installed — that badge and the extraction result are two independent things. Without a visible signal, that looks exactly like a working demo instead of a fallback, which is confusing and was a real gap, not a documentation footnote. `ModelStatusBanner` now shows on both `ProcessingScreen` and `ActionCardScreen` whenever `PacketExtractorFactory.create()` returns `usingRealModel = false`, saying plainly that the output is the Stage 2 stub. If you're seeing that banner, the app is working correctly — it just doesn't have a multi-gigabyte model file to load, which nothing in this repo can provide (see "Getting a real extraction running" below for what that actually takes).

## Getting a real extraction running

`ModelStatusBanner` will keep showing until an actual model file is on the device at the path `ModelPaths.kt` expects. This section was rewritten after live web research (prompted by real feedback from actually running this build) corrected two things that were previously guesses — see "Open questions" below for the full story.

1. **Get the right checkpoint — this is the part most guides get wrong.** For the high tier, the commonly-linked `gemma-3n-E2B-it-int4.task` "preview" checkpoint (the one most download guides and Kaggle Models point to first) is confirmed **text-and-vision only, no audio**, regardless of how correct the code calling it is. The audio-capable release is a newer `.litertlm`-format checkpoint — `google/gemma-3n-E2B-it-litert-lm` on Hugging Face is the one confirmed to state "supports multimodal inputs, including text, vision, and audio." Get that one. Accepting Gemma's license on Hugging Face or Kaggle is required either way, and the file is large (multiple GB).
2. Push it to the path `ModelPaths.kt` reads from:
   ```
   adb push gemma-3n-e2b-it-int4.litertlm /sdcard/Android/data/com.sunodo.app/files/models/gemma-3n-e2b-it-int4.litertlm
   ```
3. Make sure `app/build.gradle.kts` has `com.google.mediapipe:tasks-genai:0.10.27` (or newer) — this was bumped from an earlier, older pin as part of this fix.
4. Relaunch the app. `PacketExtractorFactory` checks for the file on every launch — no rebuild needed for the push itself, just for the dependency bump.
5. **Use a physical device, not an emulator.** Google's own docs state the LLM Inference API is optimized for high-end physical devices (Pixel 8 / Samsung S23 and above are the examples given) and doesn't reliably support emulators — if step 4 fails strangely, this is worth ruling out before anything else.

If you're testing the **budget tier**, read point 2 under "Open questions" below before spending time on it — there's a real, unresolved problem there that a correct `addAudio()` call doesn't fix.

## Open questions from building Stage 3

Two things were flagged here originally as unverified guesses. Live web research (Google's official Android LLM Inference guide, Gemma's own audio docs, and independent confirmation from a third-party SDK's docs) has now confirmed one of them and — in the process of checking it — surfaced a second, more specific problem than what was originally flagged. Both are worth reading before building further on this.

1. **RESOLVED: the audio-ingestion method name and required options.** The original code guessed `session.addAudioClip(pcm)`. The actual, confirmed API (as of this fix) is:
   - `.setAudioModelOptions(AudioModelOptions.builder().build())` on the **engine** options (`LlmInference.LlmInferenceOptions`) — this didn't exist in the code at all before.
   - `.setGraphOptions(GraphOptions.builder().setEnableAudioModality(true).build())` on the **session** options — same, entirely missing before.
   - `session.addAudio(audioData: ByteArray)` — not `addAudioClip`, and it takes a `ByteArray`, not the `AudioData` wrapper object MediaPipe's separate Audio Classifier task uses (easy to conflate the two — they're different APIs).
   - The expected byte format, confirmed independently by Gemma's own audio docs and a third-party SDK's docs: **PCM16, 16kHz, mono**. `AudioPreprocessor.preparePcm16Mono16kHz` now does the resample-to-16kHz and downmix-to-mono work this requires — both are pure functions, verified the same way `chunkBoundaries` was in Stage 3 (8 cases: downsampling, upsampling, no-op same-rate, empty input, stereo downmix, mono passthrough, and little-endian byte order including negative samples — all passed before the logic was copied in).
   - This is now sourced from Google's current official docs rather than guessed, which is a meaningfully better confidence level — but "confirmed by reading the docs" still isn't "confirmed by compiling it." Treat it accordingly.

2. **STILL OPEN, and narrower than first described: the budget tier's audio path likely doesn't work at all, for a different reason than file-based STT.** The original framing was that ML Kit/`SpeechRecognizer` lack a file-based transcription API (still true — see the reasoning in `BudgetTierPacketExtractor.kt`), and the fix was to point the same audio-native mechanism at a smaller model. Researching the audio call above turned up audio-capable checkpoints for Gemma-3n and the newer Gemma 4, but nothing indicating **Gemma 3 1B** — the model the blueprint specifies for this tier — has an audio-capable checkpoint at all. If that holds up, `BudgetTierPacketExtractor`'s audio path will fail at runtime no matter how correct `addAudio()` is, because the model itself may have no audio encoder to call. Two ways to actually resolve this: (a) confirm directly (Hugging Face's Gemma 3 1B model card, or just trying it) whether any Gemma 3 1B checkpoint supports audio input, or (b) if not, the transcribe-then-structure split from the original design needs to come back for this tier specifically, which reopens the file-based-STT gap in point 1 above — a dedicated small file-based ASR model (e.g. a quantized on-device Whisper variant via MediaPipe's Audio Task API) is the most concrete path there. Text input (`VoiceInput.Transcript` — "try a sample", pasted text) is unaffected either way; it never touches audio modality.

## What was actually verified

Standalone `kotlinc` compilation (no Android/AndroidX/Room/Compose/MediaPipe classpath available in this sandbox) was run across every `.kt` file in the module after each stage, and again after the `usingRealModel`/`ModelStatusBanner` fix and this API-correction fix. Because those libraries aren't resolvable here, this can't be a real build — but it does catch genuine parser and structural errors independent of any missing classpath. It found one, in Stage 2: a doc comment mentioning the `audio/*` MIME type accidentally opened an unclosed nested block comment (Kotlin, unlike Java, nests `/* */`), fixed in `share/ShareReceiverActivity.kt`. Stage 3 additionally pulled `AudioPreprocessor.chunkBoundaries` out into a standalone file with no Android dependency, compiled it, and ran it through six cases (zero duration, sub-chunk, exact-chunk-boundary, one-millisecond-over, multi-chunk, uneven-remainder) checking full coverage, no gaps, and no oversized chunk — all six passed before the logic was copied into the real file unchanged. This API-correction fix did the same for three more pure functions the confirmed `addAudio()` format required (resample-to-16kHz, downmix-to-mono, and little-endian PCM16 byte serialization): 8 cases covering downsampling, upsampling, a same-rate no-op, empty input, stereo downmix, mono passthrough, and byte order for both a known positive and a known negative sample — all 8 passed before the logic was copied in. Every other pass, across every stage and every fix, found only errors traced to a missing Android/AndroidX/Room/Compose/MediaPipe symbol, not a defect in this code.

## Structure

Each package under `app/src/main/java/com/sunodo/app/` has its own short `README.md` — this section is just the map; open the package's own file for what's actually in it and its status.

```
app/src/main/java/com/sunodo/app/
  data/          Room entities, DAO, database, type converters
  pipeline/      VoiceInput, PacketExtractor + tiers (Stub/High/Budget), DeviceTierDetector,
                 AudioPreprocessor, PacketPrompt, PacketJsonParser, ModelPaths, PacketExtractorFactory
  actions/       OSActionBridge (native Calendar/Share/Clipboard intents) + PacketActionHandler
                 (maps packet type -> which action fires, with a clipboard fallback)
  viewmodel/     UiState + VoiceNoteViewModel (orchestrates pipeline -> Room -> UI state)
  ui/            Compose theme + screens (Home, Processing, ActionCard, PacketCard, Error)
  share/         ShareReceiverActivity — the Share-target entry point
  MainActivity.kt, SunoDoApplication.kt
```
