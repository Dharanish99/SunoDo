# actions/

Turns a tapped packet card into a real OS action — no model, no network,
no uncertainty; the most stable package in the codebase.

- `OSActionBridge` — `ACTION_INSERT` against `CalendarContract` (opens the
  calendar app's own new-event screen, no calendar permission needed),
  `ACTION_SEND` + `Intent.createChooser` for both the reminder handoff and
  the reply handoff, and a clipboard-copy helper.
- `PacketActionHandler` — the one place that maps a packet's type to which
  of the above fires (docs/blueprint.md §3.2's task/question/decision/info
  -> action table), with a clipboard fallback if `startActivity` finds
  nothing that can handle the intent.
