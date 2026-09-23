# ⚡ OffAI (lojou)

> **Autonomous, 100% Private Offline AI Coding Assistant for IntelliJ IDEA**  
> Powered by local LLMs via **Ollama** or **llama.cpp (GGUF)**. Zero cloud telemetry. Zero subscriptions. Completely free.

[![IntelliJ Platform](https://img.shields.io/badge/IntelliJ%20Platform-2023.2%20--%202026.2-blue.svg?logo=intellijidea)](https://plugins.jetbrains.com/)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg?logo=openjdk)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Offline](https://img.shields.io/badge/Privacy-100%25%20Offline-success.svg)](README.md)
[![Backends](https://img.shields.io/badge/Engines-Ollama%20%7C%20llama.cpp-purple.svg)](README.md)

---

## 🌟 Overview

**OffAI** is an embedded AI programming partner directly inside **IntelliJ IDEA**. Unlike cloud-based assistants (Copilot, Cursor, etc.), OffAI executes **entirely on your local machine** using either **Ollama** or **llama.cpp**. Your proprietary codebase never leaves your computer.

Equipped with an autonomous **ReAct (Reasoning + Acting)** engine, OffAI does not just write code snippets in chat—it can read your workspace, create new classes, apply surgical diffs to existing files, search patterns, and organize directory trees.

---

## ✨ Key Features

- 🔒 **100% Offline & Air-Gapped**: Perfect for enterprise environments, sensitive financial systems, and offline development.
- 🤖 **Autonomous ReAct Agent**:
  - `read_file`: Inspects files and methods with context budgeting.
  - `write_file`: Generates complete, idiomatic classes and source files.
  - `edit_file`: Applies targeted search-and-replace edits without rewriting whole files.
  - `list_files`: Navigates project structures and directory trees.
  - `search_code`: Fast text pattern search across the workspace.
- 🎯 **Smart Java Package Deduction**: Automatically analyzes your project's `src/main/java` structure. If an LLM forgets or misplaces a package declaration, OffAI auto-detects and inserts the correct `package <name>;` statement automatically.
- 🎨 **Adaptive Dynamic Theme**: Automatically mirrors IntelliJ IDEA's active Look & Feel (pure white minimalist light mode or luxury obsidian dark mode) with live real-time switching without restarts.
- ⚡ **Dual Engine Flexibility**: Native integration with **Ollama** (via REST) and **llama.cpp** (`llama-server.exe` with auto-spawn lifecycle).
- 🛡 **Resilient Parsing Engine**: Self-healing JSON parser handles raw unescaped newlines and repairs truncated model outputs automatically.

---

## 🚀 Quick Start: Installing the Plugin

1. Download the latest `lojou-1.0.0.zip` from [Releases](https://github.com/saeedaliakbari/offai/releases) (or build it locally).
2. In IntelliJ IDEA, go to **Settings** (`Ctrl + Alt + S`) → **Plugins**.
3. Click the gear icon ⚙️ at the top right and select **Install Plugin from Disk...**.
4. Choose the `lojou-1.0.0.zip` file and click **OK**.
5. Restart IntelliJ IDEA.
6. Open the tool window by clicking **lojou** on the right sidebar or press `Ctrl + Alt + A`.

---

## ⚙️ Backend Setup Guide

OffAI supports two local engines. Choose the one that best fits your workflow:

---

### Option 1: Ollama Setup (Recommended)

Ollama is the simplest and fastest way to get started on Windows, macOS, or Linux.

#### 1. Install Ollama
Download and run the installer from [ollama.com/download](https://ollama.com/download).

#### 2. Pull a Recommended Coding Model
Open PowerShell or Command Prompt and run one of the following:

```powershell
# Best overall balance of speed and coding accuracy (Requires ~5 GB RAM / VRAM)
ollama run qwen2.5-coder:7b

# Lightweight model for CPU-only or low RAM systems (Requires ~2 GB RAM)
ollama run qwen2.5-coder:1.5b

# High quality Google Gemma reasoning model
ollama run gemma4:e4b
```

#### 3. Connect in IntelliJ IDEA
1. Open IntelliJ IDEA and open the OffAI tool window (`Ctrl + Alt + A`).
2. Click **⚙ Settings** in the toolbar (or **Settings → Tools → AI Agent**).
3. Select **Ollama**, make sure port is `11434`, click **Refresh Models**, select your model, and click **Apply**.

---

### 📦 Special Guide: How to Add Downloaded `.gguf` Models to Ollama (Windows)

If you downloaded a custom `.gguf` file from Hugging Face (e.g. from *TheBloke*, *bartowski*, or *Qwen*) and want to run it through Ollama, follow these simple steps:

#### Step 1: Place your `.gguf` file in a clean folder
For example:
```text
C:\AI\models\Qwen2.5-Coder-7B-Instruct-Q4_K_M.gguf
```

#### Step 2: Create a `Modelfile`
In the same folder (`C:\AI\models\`), create a new text file named `Modelfile` (make sure it has **no** `.txt` extension):

```dockerfile
# Path to your downloaded GGUF file
FROM C:\AI\models\Qwen2.5-Coder-7B-Instruct-Q4_K_M.gguf

# Model Parameters
PARAMETER temperature 0.7
PARAMETER top_p 0.9
PARAMETER num_ctx 8192

# Optional custom system prompt
SYSTEM """You are an expert AI coding assistant. Write clean, complete, and idiomatic code."""
```

> **Note on Windows paths:** Use backslashes or forward slashes (e.g., `FROM C:\AI\models\my-model.gguf` or `FROM C:/AI/models/my-model.gguf`).

#### Step 3: Register the model in Ollama
Open PowerShell, navigate to the folder, and run:

```powershell
cd C:\AI\models
ollama create my-custom-model -f Modelfile
```

Ollama will read the GGUF file and create your model instantly.

#### Step 4: Verify and Test
```powershell
ollama list
ollama run my-custom-model "Write a binary search in Java"
```

#### Step 5: Use it inside IntelliJ IDEA
In the OffAI plugin header, click **⚙ Settings** → **Refresh Models**. Your `my-custom-model` will appear in the dropdown list!

---

### Option 2: llama.cpp / llama-server Setup

If you prefer lightweight standalone binaries without running the Ollama service:

1. **Download `llama.cpp` for Windows:**
   - Go to [llama.cpp Releases](https://github.com/ggerganov/llama.cpp/releases).
   - Download the precompiled binary:
     - For CPU: `llama-bXXXX-bin-win-avx2-x64.zip`
     - For NVIDIA GPU: `llama-bXXXX-bin-win-cuda-cu12.x-x64.zip`
   - Extract it to a folder, e.g. `C:\llama-server\`.
2. **Download a GGUF Model:**
   - Download any GGUF model from [HuggingFace](https://huggingface.co/models?search=gguf) (e.g. `qwen2.5-coder-7b-instruct-q4_k_m.gguf`).
3. **Configure in IntelliJ:**
   - Go to **Settings → Tools → AI Agent**.
   - Select **llama-server (llama.cpp)**.
   - **llama-server Path:** `C:\llama-server\llama-server.exe`
   - **Model (.gguf) Path:** `C:\AI\models\qwen2.5-coder-7b-instruct-q4_k_m.gguf`
   - **CPU Threads:** `4` (or number of your physical CPU cores).
   - **GPU Layers:** `0` (CPU-only) or `33`+ (to offload layers to your GPU VRAM).
   - Check **Auto-start server** or click **Test Connection**. OffAI manages starting and stopping the server automatically!

---

## 🛠️ Usage Examples

In the chat panel, simply instruct the agent using natural language:

```text
• Create a BinarySearchTree class in the test package with generics and in-order traversal.
• Refactor UserService.java to use Java 17 records and streams.
• Find all TODO comments across the project and summarize what needs to be implemented.
• Add JUnit 5 unit tests for OrderRepository.java covering edge cases.
```

### Shortcuts
- `Ctrl + Enter` (or `Cmd + Enter`): Send message
- `Shift + Enter`: Insert a newline inside the message box
- `Ctrl + Alt + A`: Toggle the OffAI Tool Window

---

## 💻 Recommended Hardware & Models

| Hardware Specs | Recommended Model | Engine | Expected Speed |
|----------------|-------------------|--------|----------------|
| **CPU Only (4-8 Cores, 8-16 GB RAM)** | `qwen2.5-coder:1.5b` or `qwen2.5-coder:3b` | Ollama / llama.cpp | ~15-30 tokens/sec |
| **CPU (12-16 Cores, 16-32 GB RAM)** | `qwen2.5-coder:7b` (Q4_K_M) | Ollama / llama.cpp | ~8-15 tokens/sec |
| **NVIDIA GPU (6-8 GB VRAM)** | `qwen2.5-coder:7b` or `gemma4:e4b` | Ollama / llama.cpp (CUDA) | ~40-70 tokens/sec |
| **NVIDIA GPU (12-24 GB VRAM)** | `qwen2.5-coder:14b` or `deepseek-coder:33b` | Ollama / llama.cpp (CUDA) | ~35-60 tokens/sec |

---

## 🔨 Building from Source

To build the plugin yourself:

```bash
# Clone the repository
git clone https://github.com/saeedaliakbari/offai.git
cd offai

# Build plugin ZIP using Gradle
# Windows:
.\gradlew.bat buildPlugin

# Linux / macOS:
./gradlew buildPlugin
```

The output artifact will be generated in:
```text
build/distributions/lojou-1.0.0.zip
```

---

## 🇮🇷 راهنمای فارسی (Persian Quick Guide)

پلاگین **OffAI** یک دستیار هوش مصنوعی ۱۰۰٪ آفلاین و رایگان برای محیط IntelliJ IDEA است.

### نحوه افزودن مدل GGUF دانلود شده به Ollama در ویندوز:
۱. فایل `.gguf` دلخواه خود را دانلود کنید (مثلاً در مسیر `C:\models\my-model.gguf`).  
۲. در همان پوشه، فایلی با نام `Modelfile` (بدون پسوند txt) بسازید و متن زیر را در آن قرار دهید:
```dockerfile
FROM C:\models\my-model.gguf
PARAMETER temperature 0.7
PARAMETER num_ctx 8192
```
۳. ترمینال PowerShell را باز کرده و دستور زیر را اجرا کنید:
```powershell
ollama create my-custom-model -f C:\models\Modelfile
```
۴. در پنجره پلاگین در IntelliJ دکمه **⚙ Settings** را زده، روی **Refresh Models** کلیک کنید و مدل جدید را انتخاب نمایید!

---

## 📄 License

This project is open-source under the [MIT License](LICENSE).
