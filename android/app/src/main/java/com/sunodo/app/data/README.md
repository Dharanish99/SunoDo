# data/

Room persistence layer. `VoiceNote` (one row per processed note) and
`Packet` (one row per extracted packet, foreign-keyed to its voice note,
with a `dismissed` flag rather than a hard delete) via `PacketDao` and
`SunoDoDatabase`. `Converters` maps `PacketType` to/from the string Room
stores it as.

`PacketDao.insertVoiceNoteWithPackets` is a `@Transaction`-annotated
default method on the interface itself — Room supports this pattern
directly, no generated override needed.

Status and what's actually been verified: see the root
[`android/README.md`](../../../../../../../../README.md).
