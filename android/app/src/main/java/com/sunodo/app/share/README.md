# share/

`ShareReceiverActivity` — the actual integration point with WhatsApp,
Telegram, or anything else that can share an audio file. Registered in
`AndroidManifest.xml` for `ACTION_SEND` on any audio MIME type, with zero
declared permissions; it only reads the transient URI grant that comes
attached to the incoming Intent.

If the Intent doesn't carry a readable audio URI, this reports a genuine
error through the ViewModel rather than silently substituting sample data
— see `VoiceNoteViewModel.reportError` in `viewmodel/`.
