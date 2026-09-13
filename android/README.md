# SunoDo — Android app (Track A)

The real Android Studio project. This is source code meant to be opened and built in Android Studio — it has **not** been compiled by the assistant that wrote it, because this sandbox has no route to Google's Maven repository (`google()`) or an Android SDK/emulator. What it does have is a modern Kotlin compiler (2.0.21, pulled from GitHub's release assets), which was used to check every file in this module for real syntax and structural errors with everything outside the missing Android/Compose/Room classpath filtered out. One genuine bug was caught and fixed that way — see "What was actually verified" below.

## How to open it

1. Android Studio (Ladybird/2024.2 or newer recommended for Kotlin 2.0 + the Compose compiler Gradle plugin).
2. Open the `android/` folder as a project — not the repo root.
3. Let Gradle sync. It needs internet access to `google()` and `mavenCentral()` the first time, to pull AndroidX, Compose, and Room.
4. Run the `app` configuration on a device or emulator running API 26+.

## What Stage 2 actually contains

| Layer | Status |
|---|---|
| Gradle project (AGP 8.6, Kotlin 2.0.21, Compose compiler plugin, KSP) | Real, complete |
| `AndroidManifest.xml` — `ShareReceiverActivity` registered for `ACTION_SEND` + any audio MIME type, zero `<uses-permission>` entries | Real, complete |
| Room schema — `VoiceNote` + `Packet` entities, `PacketDao` (including a transactional insert), `SunoDoDatabase` | Real, complete — the ViewModel does a genuine write-then-read round trip through it, not just an in-memory model |
| Compose UI — theme (shared color tokens with `demo/sunodo_demo.html`), `HomeScreen`, `ProcessingScreen` (with device-tier badge), `ActionCardScreen`, `PacketCard`, `ErrorScreen` | Real, complete |
| `ShareReceiverActivity` — reads the shared audio URI and its display name via the transient URI grant on the incoming Intent | Real, complete |
| `DeviceTierDetector` (§3.4's RAM check, cached) | Real, complete — no model file needed for this one |
| `AudioPreprocessor.chunkBoundaries` | Real, complete, and actually verified — see "What was actually verified" |
| `AudioPreprocessor.getDurationMs` / `decodeToPcm16` | Real Android media APIs, standard patterns, **not exercised on a device** |
| `HighTierPacketExtractor` / `BudgetTierPacketExtractor` / `LlmPacketExtractor` (MediaPipe LLM Inference) | Text-generation calls written with reasonable confidence against MediaPipe's documented API shape. The audio-ingestion call is a flagged **TODO(verify)** — see "Open questions" below |
| `PacketExtractorFactory` | Real — picks a tier, falls back to the Stage 2 stub if that tier's model file isn't on the device (model files are too large to commit here) |
| `OSActionBridge` (native Calendar/Reminder/Reply intents) | Not yet implemented — tapping a card's action button currently just shows a Toast explaining it arrives in Stage 4 |

Sharing a real voice note from WhatsApp to SunoDo today will genuinely launch `ShareReceiverActivity`, genuinely read the file's name and duration, genuinely run it through `DeviceTierDetector`, and land on `PacketExtractorFactory` — which, until a real `.task` model file is pushed onto the device (see `ModelPaths.kt`), gracefully falls back to the stub rather than crashing.

## Open questions from building Stage 3

Two things surfaced while wiring in the real model that are worth flagging plainly rather than papering over, in the same spirit as the blueprint's own §3.4 RAM-constraint analysis:

1. **MediaPipe's exact audio-ingestion API for Gemma-3n wasn't confirmed.** `LlmPacketExtractor.extractFromAudio` calls `session.addAudioClip(pcm)` — the surrounding text-generation calls (`LlmInference.createFromOptions`, `addQueryChunk`, `generateResponse`) match MediaPipe's stable documented shape with reasonable confidence, but this specific method name is a best-effort placeholder. It's marked `TODO(verify)` in the code. Check it against MediaPipe's current Tasks GenAI docs or the Google AI Edge Gallery reference app before relying on it — this is the single highest-uncertainty line in the whole prototype, which tracks: it's also the newest capability in the stack the blueprint itself flags as already shifting toward LiteRT-LM.

2. **The budget-tier path's original two-step design (ML Kit STT, then Gemma 3 1B structuring) assumes an on-device, file-based speech-to-text API that doesn't appear to exist as a public, documented offering.** Both ML Kit and Android's platform `SpeechRecognizer` are built around live microphone dictation (`SpeechRecognizer.startListening()` drives its own `AudioRecord` session) — there's no `recognizeFile(uri)` entry point for an already-recorded voice note. Since the audio SunoDo receives is always a file, never something spoken live into the app, that's a real mismatch with the original plan. `BudgetTierPacketExtractor` currently resolves this by pointing the same audio-native MediaPipe mechanism used by the high tier at a smaller model checkpoint instead — collapsing "transcribe, then structure" into one step for both tiers, differing only in which model file they load. Two ways to validate this further on a real device: (a) confirm whether `addAudioClip` (or whatever the real method turns out to be) performs acceptably on a smaller/quantized checkpoint at budget-device speeds, or (b) bundle a dedicated small file-based ASR model (e.g. a quantized on-device Whisper variant via MediaPipe's Audio Task API) as a true separate transcription step if a smaller audio-native LLM checkpoint turns out not to exist or not to fit budget-device RAM.

## What was actually verified

Standalone `kotlinc` compilation (no Android/AndroidX/Room/Compose/MediaPipe classpath available in this sandbox) was run across every `.kt` file in the module, twice — once after Stage 2, once after Stage 3's changes. Because those libraries aren't resolvable here, this can't be a real build — but it does catch genuine parser and structural errors independent of any missing classpath. It found one, in Stage 2: a doc comment mentioning the `audio/*` MIME type accidentally opened an unclosed nested block comment (Kotlin, unlike Java, nests `/* */`), fixed in `share/ShareReceiverActivity.kt`. Stage 3 additionally pulled `AudioPreprocessor.chunkBoundaries` out into a standalone file with no Android dependency, compiled it, and ran it through six cases (zero duration, sub-chunk, exact-chunk-boundary, one-millisecond-over, multi-chunk, uneven-remainder) checking full coverage, no gaps, and no oversized chunk — all six passed before the logic was copied into the real file unchanged. Every other compiler error in both passes was traced to a missing Android/AndroidX/Room/Compose/MediaPipe symbol (classpath noise), not a defect in this code.

## Structure

```
app/src/main/java/com/sunodo/app/
  data/          Room entities, DAO, database, type converters
  pipeline/      VoiceInput, PacketExtractor + tiers (Stub/High/Budget), DeviceTierDetector,
                 AudioPreprocessor, PacketPrompt, PacketJsonParser, ModelPaths, PacketExtractorFactory
  viewmodel/     UiState + VoiceNoteViewModel (orchestrates pipeline -> Room -> UI state)
  ui/            Compose theme + screens (Home, Processing, ActionCard, PacketCard, Error)
  share/         ShareReceiverActivity — the Share-target entry point
  MainActivity.kt, SunoDoApplication.kt
```
