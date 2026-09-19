# SunoDo — Listen once. Act instantly.

**Built for iQOO Hackathon 2026 — Hyderabad City Battle (26–27 Sept 2026), Productivity track.**

WhatsApp voice notes in India are long, rambling, and unskimmable — the information in them (a task, a date, a decision) is buried in filler with no way to extract it without listening start to finish. SunoDo turns a shared voice note into a one-line summary and a stack of tappable cards — *Add to calendar*, *Reply*, *Copy* — entirely on the phone, no cloud call in the extraction path. Full problem statement and system design: [`docs/blueprint.md`](docs/blueprint.md).

## Status, precisely

Every part of the Android app **except the on-device model file itself** is built, not stubbed-out UI chrome: real Room persistence (a genuine write-then-read round trip, not an in-memory mock), a real Compose UI with swipe-to-dismiss and empty/error states, real native `ACTION_INSERT`/`ACTION_SEND` intents for Calendar/Reminders/Reply, and a real Android Share-target integration reachable from WhatsApp today. The one piece that can't ship in this repo is the model checkpoint itself — it's multiple gigabytes, gated behind Google's license, and has to be loaded onto a physical device by hand. That's not a gap in the engineering; it's the nature of shipping an on-device LLM, and it's exactly what the Red Light phase is for.

**What's genuinely verified, and how** (not just asserted):
- Every `.kt` file compiles cleanly against a standalone Kotlin 2.0.21 compiler pulled from JetBrains' GitHub releases — the one thing this couldn't be checked against is Google's actual Maven artifacts (AndroidX, Compose, Room, MediaPipe), which need `google()` repository access an Android Studio machine has and this build environment didn't.
- Four pieces of pure logic — audio chunk-boundary math, PCM resampling, mono downmixing, and PCM16 byte serialization — were pulled out, compiled standalone, and run against 14 concrete test cases (edge cases included: zero duration, empty input, negative-sample byte encoding) before being copied into the real files.
- The MediaPipe LLM Inference API calls (`addAudio`, `AudioModelOptions`, `GraphOptions.setEnableAudioModality`) are matched against Google's current official Android documentation, fetched live while building this — not guessed. An earlier version of this code called a method that doesn't exist; see `CHANGELOG.md` for exactly what was wrong and what corrected it.
- **Not yet verified:** the model actually loading and running inference on physical hardware. That needs the loaner iQOO 15 (given at check-in, not before) and a multi-gigabyte checkpoint neither of us has loaded yet. Everything upstream of that point is done and waiting for it.

## Two demo paths, on purpose

A hackathon demo that depends entirely on a live model load succeeding in a 30-hour window is a real risk — so there are two paths, both real, not a "fake" one and a "real" one:

- **Guaranteed path:** the full app — share intent, Room persistence, Compose UI, real Calendar/Reminder/Reply intents — running today on any Android device, using a stub extractor that returns a fixed example. Zero setup, zero risk, demonstrates every piece of the architecture except the model call itself.
- **Stretch path:** the same app with a real Gemma-3n checkpoint loaded via `adb push` onto the iQOO 15, producing a live extraction from real audio during Red Light. See `android/README.md`'s "Getting a real extraction running" for the exact steps and the two open risks worth knowing about going in.

`ModelStatusBanner` in the app makes it obvious at a glance which path is active — it never silently shows one path's output while implying the other.

## How this maps to the judging rubric

| Category | Weight | Where SunoDo addresses it |
|---|---|---|
| Phone-First Execution | 25% | Native Android app (`android/`), not a wrapped web view. Runs and demos on-device. |
| Office Kit usage | 25% | A build-workflow practice for Red Light, not a codebase feature — see `SUBMISSION.md`'s event-day plan. |
| AI-Native Build | 20% | On-device MediaPipe LLM Inference (Gemma-3n, audio-native, no cloud call in the extraction path) — see "Status, precisely" above for exactly what's verified. |
| Problem Fit | 20% | Productivity track: turns unstructured voice notes into structured, actionable output — see `docs/blueprint.md` for the full case. |
| Craft & Pitch | 10% | See `SUBMISSION.md`. |

## Repo layout

```
docs/     design source of truth (docs/blueprint.md) + this module's own README
android/  the submission itself — real Android Studio project (see android/README.md)
demo/     a browser-based validation tool, NOT the phone demo — see below
SUBMISSION.md   judge-facing one-pager, mapped to the rubric
CHANGELOG.md    detailed stage-by-stage build history
```

## About `demo/` — why it exists and why it isn't the phone demo

Before committing engineering time to the on-device MediaPipe integration, the exact packet-extraction JSON schema and prompt were validated live against a real LLM call in a browser page (`demo/sunodo_demo.html`) — cheap to iterate on, immediately testable, no Android build cycle. That validation step is done; the schema it produced is what `android/`'s `PacketPrompt.kt` uses today, word-for-word.

It stays in the repo as evidence of that validation, not as a second demo path — iQOO Hackathon 2026's own rubric explicitly weights phone-first execution and on-device AI, and explicitly flags cloud-API and web-only builds as scoring worse. For this submission, `android/` is the product; `demo/` is how its core idea was validated before the on-device build started.

## Further reading

- [`SUBMISSION.md`](SUBMISSION.md) — the judge-facing pitch, event-day plan, and honest limitations list
- [`android/README.md`](android/README.md) — full technical status, per-package breakdown, and the two open technical questions worth knowing before the event
- [`docs/blueprint.md`](docs/blueprint.md) — original problem statement, system design, and business case
- [`CHANGELOG.md`](CHANGELOG.md) — detailed build history, stage by stage
