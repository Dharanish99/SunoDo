# SunoDo — Submission Notes

**iQOO Hackathon 2026 · Hyderabad City Battle · 26–27 Sept 2026 · Productivity track**

## The pitch, in one breath

Voice notes are how India actually communicates on WhatsApp — and they're the one message format you can't skim. SunoDo turns a shared voice note into a TL;DR and a stack of tappable cards (task, question, decision, info) with real one-tap actions into the phone's own Calendar, Reminders, and messaging apps — entirely on-device, no cloud call in the path that reads your audio.

## Why Productivity

Hyderabad's Productivity track is defined as "work smarter, automate tasks, manage information." SunoDo is a direct fit, not a stretch: it doesn't ask anyone to change how they communicate (voice notes stay voice notes), it removes the actual bottleneck (re-listening to extract one sentence), and it hands back native, actionable output instead of another inbox to check. Full case: `docs/blueprint.md` §1–2 and §6.

## What's built (see root `README.md` and `android/README.md` for the precise, verified-how breakdown)

- Real Android Share-target integration — reachable from WhatsApp today, zero declared permissions
- Real Room persistence — write-then-read, not an in-memory mock
- Real Compose UI — swipe-to-dismiss, empty states, honest error states, not just a happy-path screen
- Real native OS actions — `ACTION_INSERT` opens the calendar app's own event screen, `ACTION_SEND` hands reminders/replies to whatever app the judge's own test phone has installed
- On-device LLM Inference integration (MediaPipe, Gemma-3n) with API calls matched against Google's current documentation — the one piece pending physical hardware to actually run inference on

## Event-day plan

**Before arriving (this week):**
- [ ] Pre-download and test **Office Kit** — its usage is tracked automatically and is worth as much of the score (25%) as phone-first execution itself. Don't touch it for the first time on the clock.
- [ ] Pre-download the Gemma-3n `.litertlm` checkpoint from Hugging Face (`google/gemma-3n-E2B-it-litert-lm`) onto a laptop, ahead of time. It's multiple GB — starting that download on event wifi during the 30-hour window is a real, avoidable risk to the schedule.
- [ ] Read `android/README.md`'s "Getting a real extraction running" and "Open questions" sections once, calmly, before the clock starts — not for the first time mid-crunch.
- [ ] Pull the latest commit and confirm the project opens cleanly in Android Studio.

**Red Light (phone-only, ~55% of the 30 hours):** this is exactly when to attempt the real model load — `adb push` the checkpoint onto the loaner iQOO 15, relaunch, and see whether `ModelStatusBanner` disappears. Also the time to test the Share-target flow with real WhatsApp voice notes recorded on the same device, exercise the Calendar/Reminder/Reply intents against whatever's actually installed, and record real sample audio for the pitch. If the on-device audio call needs adjusting (see the two open questions in `android/README.md`), this is where that gets found out — with the app's guaranteed stub path as the fallback if it doesn't resolve in time.

**Green Light (phone + laptop, ~45%):** Android Studio + Logcat for anything that needs real debugging, any code fixes the Red Light testing surfaces, and pitch deck prep.

## What we're not pretending is done (defending the trade-offs, not hiding them)

- **On-device inference latency and output quality on the iQOO 15's actual hardware hasn't been measured yet.** The architecture and API calls are verified against documentation; the first real measurement happens live at the event, on purpose — that's what Red Light is for.
- **Whether MediaPipe's GPU delegate meaningfully engages the Snapdragon NPU specifically, versus Qualcomm's own more NPU-direct SDK, is an open question.** Worth asking one of the event's 12+ mentors early rather than guessing — if there's a better-supported path to the NPU specifically, better to know in hour 2 than hour 28.
- **The budget-tier (lower-RAM device) on-device audio path has a known gap:** research while building this found no evidence Gemma 3 1B has an audio-capable checkpoint at all. Out of scope for a flagship loaner device, but worth being upfront about if asked — the architecture accounts for it (`android/README.md`), the model doesn't currently support it.
- **The browser demo in `demo/`** validated the extraction schema and prompt before the on-device build started — it is not, and is not presented as, the phone demo. See the root `README.md`'s "About `demo/`" section.
