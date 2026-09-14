# demo/

`sunodo_demo.html` — Track B, the live browser simulation of the core
pipeline. Self-contained (open it directly, no build step); calls the
Anthropic API client-side using the same packet JSON schema the Android
app's `PacketPrompt`/`PacketJsonParser` use, so the two aren't just
similar-looking, they're structurally the same idea. See the root
[`README.md`](../README.md) for why this exists alongside the Android app
rather than instead of it.
