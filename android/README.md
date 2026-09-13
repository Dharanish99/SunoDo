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
| Packet extraction (`PacketExtractor` / `StubPacketExtractor`) | **Stubbed.** Returns the same canned example from `docs/blueprint.md` §3.2 after an artificial delay, regardless of what's actually in the shared audio. This is Stage 3's job. |
| Device-tier capability check (`docs/blueprint.md` §3.4) | Not yet implemented — `ProcessingScreen` can display a tier badge, but nothing sets a real one yet |
| `OSActionBridge` (native Calendar/Reminder/Reply intents) | Not yet implemented — tapping a card's action button currently just shows a Toast explaining it arrives in Stage 4 |

Sharing a real voice note from WhatsApp to SunoDo today will genuinely launch `ShareReceiverActivity`, genuinely read the file's name, and genuinely render a TL;DR and cards through Room — the cards just won't reflect that specific audio's real content until Stage 3.

## What was actually verified

Standalone `kotlinc` compilation (no Android/AndroidX/Room/Compose classpath available in this sandbox) was run across every `.kt` file in the module. Because those libraries aren't resolvable here, this can't be a real build — but it does catch genuine parser and structural errors independent of any missing classpath. It found one: a doc comment mentioning the `audio/*` MIME type accidentally opened an unclosed nested block comment (Kotlin, unlike Java, nests `/* */`), which the compiler flagged as reaching end-of-file inside an open comment. Fixed in `share/ShareReceiverActivity.kt`. Every remaining compiler error was cross-checked and traced to a missing Android/AndroidX/Room/Compose symbol (i.e. classpath noise), not a defect in this code — see the Stage 2 commit message for the exact check.

## Structure

```
app/src/main/java/com/sunodo/app/
  data/          Room entities, DAO, database, type converters
  pipeline/      PacketExtractor interface + Stage 2's stub implementation
  viewmodel/     UiState + VoiceNoteViewModel (orchestrates pipeline -> Room -> UI state)
  ui/            Compose theme + screens (Home, Processing, ActionCard, PacketCard, Error)
  share/         ShareReceiverActivity — the Share-target entry point
  MainActivity.kt, SunoDoApplication.kt
```
