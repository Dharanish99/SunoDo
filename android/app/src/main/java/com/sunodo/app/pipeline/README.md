# pipeline/

Everything between "we have an audio file or a transcript" and "we have a
TL;DR and a list of packets."

- `VoiceInput` — sealed type: `Audio` (a file URI, for the high-tier path)
  or `Transcript` (already-known text, for the budget tier and for
  "try a sample" / pasted text).
- `PacketExtractor` — the interface every tier implements.
- `DeviceTierDetector` — real RAM check (docs/blueprint.md §3.4), cached.
- `AudioPreprocessor` — duration lookup, chunk-boundary math (the one piece
  in this package that's been run and checked outside this codebase, not
  just written), and a PCM decode helper.
- `PacketPrompt` / `PacketJsonParser` — the shared extraction schema and its
  parser, kept consistent with `demo/sunodo_demo.html`.
- `LlmPacketExtractor`, `HighTierPacketExtractor`, `BudgetTierPacketExtractor`,
  `ModelPaths`, `PacketExtractorFactory` — the MediaPipe integration. This is
  the least-certain code in the whole project; read the doc comment at the
  top of `LlmPacketExtractor.kt` before trusting the audio-ingestion call.
- `StubPacketExtractor` — Stage 2's placeholder, still used as
  `PacketExtractorFactory`'s graceful fallback when a tier's model file
  isn't on the device.

Full status table and the two open questions from building this: see the
root [`android/README.md`](../../../../../../../../README.md).
