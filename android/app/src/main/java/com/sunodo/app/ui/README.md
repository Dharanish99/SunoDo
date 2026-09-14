# ui/

Compose theme and screens. `theme/` holds the color tokens (kept in sync
by hand with `demo/sunodo_demo.html`'s CSS variables) and typography.
`SunoDoScreen` is the single composable both `MainActivity` and
`ShareReceiverActivity` host — it switches on `UiState` and doesn't know or
care which Activity is hosting it.

- `HomeScreen` — idle state; "try a sample" or a received-file label.
- `ProcessingScreen` — device-tier badge + spinner.
- `ActionCardScreen` / `PacketCard` — the TL;DR banner and card stack.
  Dismissing a card works two ways: swipe (Material3's
  `SwipeToDismissBox`) or the explicit × button, so it doesn't assume
  everyone dismisses things the same way.
- `ErrorScreen` — shown both for extraction failures and for the
  "share intent had no readable audio" case (see `share/README.md`).
