# SunoDo — "Listen once. Act instantly."
### A 36-hour, fully on-device Android solution to India's voice-note productivity drain
**Theme:** Productivity | **Platform:** Mobile (Android) | **AI:** 100% on-edge, zero cloud

---

## 1. The Problem Statement

**Root observation:** India doesn't have a "voice note feature" problem — it has a voice note *culture*. Voice notes make up roughly 22% of all WhatsApp traffic in India, well above the global average, driven by comfort speaking over typing across regional languages, low typing-literacy in Tier‑2/3 towns, and a cultural preference for warmth-over-brevity in communication. Globally, WhatsApp voice-note traffic has grown fast enough that by 2026 it accounts for a large and rising share of all messages sent on the platform.

The friction isn't recording or sending — it's **receiving**. A voice note is a serial, linear, unskimmable format:

- You cannot scan a 3-minute voice note the way you scan a 3-line text.
- Information (a task, a date, a decision) is buried inside filler, small talk, and rambling — and there's no way to extract just that without listening start to finish.
- It's socially awkward to reply "just type it" to a senior, a parent, or a client.
- It's actively disruptive during commutes, meetings, and quiet workspaces — exactly when working professionals in Indian metros (long commute times, open-plan offices, shared family spaces) receive most of them.

The result, reported directly by users in UX research on this exact behavior: people *stack up* unheard voice notes, miss embedded deadlines, and lose real work time re-listening to messages just to extract one sentence of actual information.

**This is a genuinely current, India-weighted productivity problem** — not a hypothetical one — and it sits squarely inside the "modern communication overload" category without being a repackaged to-do-list or Pomodoro-timer idea.

---

## 2. The Solution & the Innovation Approach

### The design-thinking journey
- **Empathize/Define:** The pain isn't "I can't understand the voice note" — WhatsApp already ships on-device transcription in India (English, Hindi shows up unofficially, Spanish, Portuguese, Russian). The real pain is: *even with perfect text in front of me, I still have to read the whole rambling paragraph to find the one task or date buried in it.* Transcription solves legibility, not actionability.
- **Ideate, out of the box:** Instead of borrowing from the transcription-app playbook (Otter.ai, Voicenotes.ai, WhatsApp's own feature — all of which stop at "here's the text"), the idea came from three unrelated systems that all solve the same *abstract* problem — turning a long serial signal into discrete, addressable, actionable units:
  - **A ribosome** doesn't keep a whole mRNA strand around — it reads the serial code and outputs discrete, independently-functional proteins, each with one job.
  - **A mycelium network** doesn't log every nutrient event — it continuously reinforces the highest-value paths and lets the rest fade.
  - **Ant stigmergy** replaces "replay the message" with a persistent, glanceable marker anyone can act on without re-experiencing the original signal.
- **The synthesis:** treat a voice note the way a ribosome treats mRNA — don't hand the user a transcript to *read*, hand them a set of discrete, atomic, one-tap-actionable **packets**: a task, a date, a decision, a question — each independently usable, the moment the audio finishes processing.

### The core idea, in one sentence
**Share any voice note to SunoDo. In seconds, on your own phone, with no internet needed, it hands you back a one-line summary plus a stack of tappable cards — "Add to Calendar," "Add to Reminders," "Copy reply" — instead of a wall of text you still have to read.**

### Why this survives the obvious alternatives (cliché audit)
| Obvious idea | Why it doesn't solve the actual problem |
|---|---|
| "Just use WhatsApp's built-in transcription" | Gives raw text only — still a wall of prose to read; official language support in India is limited to English/Spanish/Portuguese/Russian (Hindi appears unofficially); works only inside WhatsApp, per-message, manually triggered. |
| "Build an Otter.ai clone" | Transcription-as-a-product is already commoditized and typically cloud-based (privacy risk for sensitive family/business audio, and a recurring cloud-inference cost). |
| "AI chatbot dashboard for your messages" | Requires reading a summary chat-style — still linear, still passive, no direct-to-OS action. |
| "Gamify voice note habits" | Doesn't touch the actual bottleneck: extraction of actionable structure from unstructured speech. |

None of these were used. SunoDo's differentiator is **atomic, structured, one-tap-actionable extraction — not transcription** — validated against what currently exists (Section 7 has the full comparison).

---

## 3. System Design

*(High-level architecture diagram shown above this document — this section adds the layers a diagram can't carry: data model, operational sequence, and device-tier logic.)*

### 3.1 Low-level module breakdown

| Module | Responsibility |
|---|---|
| `ShareIntentReceiver` | Registers SunoDo as an Android Share target for `audio/*`. Zero permissions beyond reading the shared file URI — no accessibility service, no notification listener, no WhatsApp automation. |
| `AudioPreprocessor` | Trims silence, normalizes volume, chunks audio into ≤30s windows (matches the on-device audio-input model's batch limit). |
| `STTEngine` | Produces a raw transcript. Tiered (see 3.3). |
| `PacketExtractor` | Feeds the transcript (or raw audio, on capable devices) to the on-device SLM with a fixed JSON schema prompt. Outputs structured packets. |
| `PacketStore` | Local Room (SQLite) database. Nothing leaves the device — no server, no sync, by design. |
| `ActionCardUI` | Jetpack Compose screen rendering the TL;DR banner + one card per packet. |
| `OSActionBridge` | Fires native Android Intents — `ACTION_INSERT` (Calendar), reminder app intents, `ACTION_SEND` (reply draft) — no proprietary API integration required. |

### 3.2 Data model — the "packet" schema
Every voice note collapses to one JSON object, atomic units array:

```json
{
  "source": "WhatsApp",
  "duration_sec": 47,
  "tldr": "Priya needs the report by Tuesday and is unsure about the client's budget.",
  "packets": [
    { "type": "task", "content": "Send the Q3 report", "due": "2026-09-16" },
    { "type": "question", "content": "Confirm client's revised budget" },
    { "type": "decision", "content": "Meeting moved to 4pm Thursday" },
    { "type": "info", "content": "New vendor contact shared: Ramesh, Chennai" }
  ]
}
```

Each `packet.type` maps directly to one card style and one primary action in the UI (task → Add to Calendar/Reminders, question → Copy as reply, decision → Add to Calendar, info → Copy/Archive).

### 3.3 Operational sequence (step-by-step)
1. User long-presses a voice note in WhatsApp/Telegram/any app → **Share → SunoDo**.
2. `ShareIntentReceiver` reads the audio URI, hands off to `AudioPreprocessor`.
3. Device-tier check runs once at install time and is cached (see 3.4).
4. **High-tier path:** Gemma-3n (audio-native) ingests the raw audio directly and returns structured JSON in one pass.
   **Budget-tier path:** ML Kit on-device speech recognizer produces a transcript first; a smaller text-only SLM (Gemma 3 1B, int4) then structures that transcript into the same JSON schema.
5. `PacketStore` saves the result locally; `ActionCardUI` renders the TL;DR + cards, typically within a few seconds of the note finishing processing.
6. User taps a card → `OSActionBridge` fires the matching native intent. No further app interaction needed.

### 3.4 Adaptive device-tier logic (validated constraint)
On-device audio-native Gemma-3n E2B has a real peak-memory footprint (~5.9GB) that will not run smoothly on India's dominant budget-Android segment (4–6GB RAM). Rather than ignore this, the architecture branches on a one-time device capability check:

- **RAM ≥ 6GB + GPU delegate available →** Gemma-3n E2B (int4), single-pass audio → structured JSON. Best latency, best accuracy, handles code-switched speech natively.
- **RAM < 6GB →** ML Kit offline speech recognizer (lightweight, already ships free with Google Play Services, has strong Hindi/regional-language coverage) → transcript → Gemma 3 1B (int4, ~530MB) purely for text-structuring. Slightly higher latency, still fully offline, still fully on-device.

This tiering is the difference between a demo that only works on a flagship phone and a solution that actually fits India's real device base — this was treated as a hard validation requirement, not an afterthought.

### 3.5 UI shape
A single-screen result view: a top banner with the one-line TL;DR, then a vertical stack of cards (color-coded by packet type), each with one primary tap-action and a "dismiss" swipe. No onboarding flow, no login, no account — opens straight into the result after a share, because the entire point is removing steps, not adding a new app to check.

---

## 4. Tech Stack — and why

| Layer | Choice | Why |
|---|---|---|
| Platform | Native Android (Kotlin) | Only path to genuine OS-level Share-target registration, native Intents, and direct access to on-device ML runtimes without a cross-platform bridging tax — critical to hit a 36-hour deadline without fighting the framework. |
| UI | Jetpack Compose | Fast to build, minimal boilerplate for a card-stack UI, modern and demo-ready. |
| On-device LLM runtime | MediaPipe LLM Inference API (`tasks-genai`) | Fully documented, official Android SDK, ships with a public reference app (Google AI Edge Gallery) that can be forked directly — the realistic way to get multimodal (audio+text) on-device inference working inside a hackathon window. |
| High-tier model | Gemma-3n E2B (int4) | The only currently shipping on-device model with native audio understanding through MediaPipe — collapses STT + structuring into one model call. |
| Budget-tier STT | Google ML Kit — on-device Speech Recognition | Free, offline-capable, already has strong Hindi and regional-language support tuned for the Indian market; avoids bundling a second heavy model. |
| Budget-tier structuring | Gemma 3 1B (int4, ~530MB) | Small enough to coexist with ML Kit on a budget device; sufficient for structured JSON extraction from clean text. |
| Local storage | Room (SQLite) | No backend needed — this is a pure client APK by design, which is also the entire privacy pitch. |
| OS integration | Android Intents (`ACTION_INSERT`, `ACTION_SEND`) | Zero third-party API dependency, zero WhatsApp ToS risk — this is the "simple idea beats complex approach" call: a Share-intent app needs no scraping, no accessibility service, no notification-listener grey area. |

**Forward note:** Google has moved the plain MediaPipe LLM Inference API into maintenance mode in favor of LiteRT-LM. For a 36-hour build, MediaPipe is still the right call — more sample coverage, a working reference app to fork — with LiteRT-LM flagged as the natural post-hackathon migration target.

**Explicitly not used:** any cloud LLM API, any backend server, any WhatsApp Business API, any accessibility-service automation. This is a pure client-side Android application — fully a software solution, as required.

---

## 5. What each node in the pipeline actually does

1. **Voice note received (Share intent):** the only integration point with any external app. Completely legal and ToS-safe because it relies on the standard Android Share Sheet — the user explicitly hands the file over, once, per note. No background listening, no silent access to chat content.
2. **On-device speech-to-text:** converts audio to raw text. This stage alone is what WhatsApp's native feature already offers — included here only as an input to the next stage, not as the product itself.
3. **On-device small language model:** the actual innovation layer. Takes either raw audio or a transcript and, using a fixed extraction schema, returns structured JSON — this is where "rambling paragraph" becomes "discrete task, discrete date, discrete question."
4. **Action cards:** the user-facing translation of that JSON into something tappable. This is the payoff moment — the point where a 3-minute voice note becomes a 3-second decision.
5. **Calendar & reminders / Reply draft:** the final handoff to the phone's native apps, using Android's own Intent system — so SunoDo never needs to *become* a calendar app or a task manager; it plugs into the ones the user already has.

---

## 6. Societal Impact

- **Time reclaimed at scale:** with voice notes making up roughly a fifth of WhatsApp communication in India and rising, even a few minutes saved per user per day compounds across hundreds of millions of Indian WhatsApp users.
- **Accessibility:** directly helps people who are hard of hearing, people on factory floors, call-center shifts, or crowded public transport where audio playback isn't practical — skimmable cards work where a 3-minute audio clip doesn't.
- **Respects India's actual communication culture instead of fighting it:** it doesn't ask senders to stop sending voice notes (an unrealistic behavior-change ask) — it fixes the receiving side, which is the only side that's actually broken.
- **Privacy as a trust feature, not a checkbox:** because processing is 100% on-device, sensitive family, health, and business content never leaves the phone — a meaningful trust signal in a market where cloud-AI skepticism is real, especially among older or rural users.
- **Digital-literacy bridge:** helps the large segment of Indian users who are far more comfortable *speaking* than *typing* (common in Tier‑2/3 towns) get the productivity benefits of "text-like" structured information without changing how they communicate.

---

## 7. Business Model & Differentiation

### Business model
- **Freemium core, on-device-first:** unlimited basic extraction (task/date/question/decision) is free forever — there's effectively no marginal cloud-inference cost to Anthropic-style API bills, since everything runs on the user's own hardware. This is the actual unlock: a sustainable free tier that competitors bearing per-note cloud LLM costs cannot match.
- **Pro tier (₹99/month or ₹299 one-time):** batch-processing entire voice-note backlogs in one tap, notes longer than 2 minutes, auto-sync to Calendar without a manual tap, export to Notion/Google Tasks, and additional regional-language packs (Tamil, Telugu, Kannada, Bengali, Marathi) beyond the default Hindi/English.
- **B2B/SDK licensing:** white-label the extraction engine to voice-note-heavy Indian platforms — school-parent communication apps, gig-worker coordination tools, regional business-WhatsApp automation providers — as an embeddable on-device SDK, priced per integrating company rather than per end-user.

### How this differs from what already exists

| | WhatsApp native transcription | Otter.ai / cloud transcription apps | SunoDo |
|---|---|---|---|
| Output | Raw text wall | Raw text + cloud summary | Discrete, tappable action cards |
| Where it runs | On-device (WhatsApp only) | Cloud | 100% on-device |
| Language coverage in India | English official; Hindi unofficial | Mostly English | Tuned for Hindi + Indian English, expandable to regional languages |
| Works across apps | No — WhatsApp only | Partial | Yes — any app that can Share audio |
| Recurring cost to run | None (Meta absorbs it) | Ongoing cloud inference cost | None — runs on the user's own phone |
| Direct OS integration (Calendar/Reminders) | No | No | Yes, one tap |
| Privacy model | On-device, WhatsApp-controlled | Cloud-processed | Fully local, nothing transmitted |

The gap this fills is specific and validated: transcription already exists and is already free inside WhatsApp — so a product that stops at "here's the text" has nothing to sell. SunoDo's business case rests entirely on the layer nobody has shipped yet: turning that text into structured, cross-app, zero-cloud-cost action.
