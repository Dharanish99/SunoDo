# demo/

`sunodo_demo.html` — a browser-based validation tool for the packet-extraction schema, built before the on-device MediaPipe integration to test the prompt and JSON schema quickly against a real LLM call. Self-contained (open it directly, no build step); the schema it validated is exactly what the Android app's `PacketPrompt.kt`/`PacketJsonParser.kt` use today.

**This is not the iQOO Hackathon 2026 phone demo.** It's a cloud API call in a browser — the two things the hackathon's own rubric explicitly weights against (web-only, cloud-only). It stays in the repo as evidence of how the extraction approach was validated before committing to the on-device build, not as a second demo path. The Android app in `android/` is the submission. See the root [`README.md`](../README.md)'s "About `demo/`" section.
