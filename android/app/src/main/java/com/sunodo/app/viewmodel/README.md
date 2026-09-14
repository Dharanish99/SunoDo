# viewmodel/

`UiState` (Idle / Processing / Result / Error) and `VoiceNoteViewModel`,
which resolves the device tier once up front, calls the right
`PacketExtractor`, and does a real write-then-read round trip through Room
rather than holding the extraction result only in memory.

`reportError` exists separately from the `process()` catch block: it's for
failures that happen *before* extraction starts (a share intent with no
readable audio), so the UI can say so honestly instead of quietly running
the sample transcript in its place.
