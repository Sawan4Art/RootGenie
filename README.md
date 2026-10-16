# RootGenie 🧞‍♂️

**AI-powered root assistant & Magisk module generator — built for rooted Android phones.**

Talk to your phone in Arabic or English. RootGenie understands your intent, runs the right root commands behind a strict safety gate, and can even **generate complete Magisk modules on the fly** and install them for you — no recovery, no PC.

## ✨ Features (v0.1.0)

| Feature | Description |
|---|---|
| 💬 AI Chat Control | Natural language → root shell commands (Arabic + English) |
| 🧠 Offline Brain | Common commands (density, force-stop, battery, dark mode…) work with zero internet |
| ☁️ Cloud Brain | Any OpenAI-compatible endpoint: OpenAI, Gemini, OpenRouter, LM Studio… |
| 📦 Module Generator | Describe a module → AI builds `module.prop` + scripts + files → installs via `magisk --install-module` |
| 🛡️ Three-tier Safety Gate | SAFE (auto-run) / CONFIRM (dialog) / BLOCKED (always refused) |
| 🔤 Fully Bilingual | Arabic (RTL) & English UI |

## 🏗️ Architecture

```
User input
   │
   ▼
OfflineBrain ──(instant match)──► ShellPlan
   │ (no match)
   ▼
AIClient (OpenAI-compatible) ──► JSON contract
   │                              ├── type: chat   → message
   │                              ├── type: shell  → commands[]
   │                              └── type: module → ModuleSpec
   ▼
SafetyChecker ──► SAFE / CONFIRM / BLOCKED
   ▼
RootShell (libsu) ──► magisk --install-module / shell exec
```

- **Root layer:** [libsu](https://github.com/topjohnwu/libsu) by topjohnwu (Magisk's author)
- **Safety:** regex-based three-tier gate; unknown commands default to CONFIRM; destructive patterns are hard-blocked
- **Module format:** official Magisk module structure (`module.prop`, `customize.sh`, `system.prop`, files)

## 📱 Build

### Option A — GitHub Actions (no PC needed)
Push to this repo → the included workflow builds `RootGenie-debug-apk` automatically under **Actions → Artifacts**.

### Option B — Locally
```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```
Requires JDK 17, Android SDK 34.

## ⚙️ Setup (first launch)

1. Open **Settings** tab
2. Set your **API base URL**, **API key**, and **model name**
3. Keep **Strict safety mode** ON until you trust your workflows

## 🔐 Security model

- Every planned command passes the local `SafetyChecker` **before** execution — the AI cannot bypass it
- Destructive patterns (`dd` to block devices, `mkfs`, `rm -rf` on system paths, flashing) are always blocked
- Your API key is stored only in the app's private preferences, never transmitted anywhere except your chosen endpoint

## 🗺️ Roadmap

- [ ] On-device LLM (Gemma 2B via llama.cpp) for full offline mode
- [ ] Module template marketplace
- [ ] LSPosed module generation
- [ ] Voice commands
- [ ] Play Integrity assistant (PIF config helper)

## ⚠️ Disclaimer

RootGenie executes commands with **root privileges**. The safety gate blocks obviously destructive patterns, but no tool can protect you from confirming something reckless. You are the last line of defense — read the confirmation dialogs.
