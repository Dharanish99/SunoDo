# pipeline/

Everything between "we have an audio file or a transcript" and "we have a
TL;DR and a list of packets."

- `VoiceInput` — sealed type: `Audio` (a file URI, for the high-tier path)
  or `Transcript` (already-known text, for the budget tier and for
  "try a sample" / pasted text).
- `PacketExtractor` — the interface every tier implements.
- `DeviceTierDetector` — real RAM check (docs/blueprint.md §3.4), cached.
- `AudioPreprocessor` — duration lookup, chunk-boundary math, and (as of
  the API-correction fix) resample-to-16kHz / downmix-to-mono / PCM16 byte
  serialization for `LlmInferenceSession.addAudio()`. Four pure functions
  in this file have been run and checked outside this codebase, not just
  written — see the class doc comment for exactly which ones and what was
  tested.
- `PacketPrompt` / `PacketJsonParser` — the shared extraction schema and its
  parser, kept consistent with `demo/sunodo_demo.html`.
- `LlmPacketExtractor`, `HighTierPacketExtractor`, `BudgetTierPacketExtractor`,
  `ModelPaths`, `PacketExtractorFactory` — the MediaPipe integration. The
  audio-ingestion call was originally a guess and has since been corrected
  against Google's official docs (method name, required options, and byte
  format all changed) — but a live-research pass surfaced a second,
  narrower problem for the budget tier specifically. Read the doc comments
  on `LlmPacketExtractor.kt` and `BudgetTierPacketExtractor.kt` before
  trusting either.
  `PacketExtractorFactory.create` returns an `ExtractorSelection`, not a
  bare `PacketExtractor` — the `usingRealModel` flag on it is what lets the
  UI show `ModelStatusBanner` instead of silently running the stub next to
  a real device-tier badge.
- `StubPacketExtractor` — Stage 2's placeholder, still used as
  `PacketExtractorFactory`'s graceful fallback when a tier's model file
  isn't on the device.

Full status table and the two open questions from building this: see the
root [`android/README.md`](../../../../../../../../README.md).
