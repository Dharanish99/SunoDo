# SunoDo — Listen once. Act instantly.

A 100%-on-device Android concept for India's voice-note productivity drain. Full problem statement, system design, and business case are in [`docs/blueprint.md`](docs/blueprint.md).

This repo builds the prototype in stages. Each stage is a separate commit/PR so progress is reviewable incrementally.

## Repo layout

```
docs/     design source of truth (the hackathon blueprint) + architecture notes as they're added
demo/     Track B — a live, runnable simulation of the core pipeline (see below)
android/  Track A — the real Android Studio project (added in a later stage)
```

## Why two tracks

The core innovation in SunoDo isn't transcription — it's turning a rambling voice note into discrete, structured, one-tap-actionable packets (see `docs/blueprint.md` §2–3). That extraction step is what this prototype needs to prove works, end to end, without hand-waving.

Two things are true about how this was built:
- A real Android build (Jetpack Compose, MediaPipe, ML Kit, Room) needs Google's Maven repository and an Android SDK/emulator — not available in the sandbox this was built in. So **Track A** is complete, real, idiomatic source code, meant to be opened and built in Android Studio — not compiled or emulator-tested by the assistant that wrote it.
- What *was* available: a real LLM call. So **Track B** is a browser-based simulation of the same pipeline — synthetic voice-note transcripts in, a live call using the exact JSON packet schema from the blueprint, tappable result cards out (including a real downloadable `.ics` calendar file and clipboard actions). It's not the Android app, but the extraction logic and UI/UX it demonstrates are real and testable today, not mocked.

## Demo (Track B)

Open [`demo/sunodo_demo.html`](demo/sunodo_demo.html) in a browser. Pick one of four synthetic voice notes (or paste your own transcript) and watch it resolve into a TL;DR and action cards. An "About this demo" toggle at the bottom of the result screen states plainly which parts are staged for the browser (share sheet, device-tier badge, waveform) versus which part is a real network call (the extraction itself).

## Stage roadmap

- [x] **Stage 1** — repo scaffold, docs, and the Track B live demo
- [x] **Stage 2** — Android project skeleton: manifest, Share-target registration, Room schema, Compose card UI (no model integration yet, packets stubbed) — see [`android/README.md`](android/README.md)
- [ ] **Stage 3** — on-device model integration: MediaPipe LLM Inference for the high-tier path, ML Kit + Gemma 3 1B for the budget-tier path, device-tier capability check
- [ ] **Stage 4** — `OSActionBridge`: native Calendar/Reminder/Reply intents wired to real packet data
- [ ] **Stage 5** — polish pass: empty states, error states, swipe-to-dismiss, README per module
