# CodeForge Studio — Android Mobile Development Studio

CodeForge Studio is a professional, mobile-first software development environment built natively for Android smartphones and tablets using Kotlin and Jetpack Compose. Inspired by the productivity of desktop IDEs like VS Code, CodeForge Studio is purpose-built for mobile touch interaction, external hardware keyboards, and offline development.

---

## Key Features

- **Touch-Optimized Code Editor:**
  - 100% offline, touch-adapted editor engine with full syntax highlighting across **14 programming languages**.
  - Multi-tab document workspace with dirty indicators and autosave draft recovery.
  - Line numbers, active line highlight, bracket auto-matching, auto-indentation, and code folding.
  - In-editor search and replace, go-to-line, word wrap, and font scaling.
- **Hierarchical Project Explorer:**
  - Fast file and folder tree navigation.
  - Safe create, rename, delete, and move operations with destructive action confirmation.
  - Path-traversal-protected Safe ZIP import and export (Zip Slip immunity).
- **14-Language Capability Registry:**
  - Capability-based runtime registry (Python, JavaScript, TypeScript, HTML, CSS, JSON, Kotlin, Java, C, C++, Rust, Go, Lua, C#).
  - Explicit capability checks that distinguish installed runtimes from platform limitations.
- **Real Process Runner & Execution Engine:**
  - Executes real local processes on Android (`ProcessBuilder`), streams stdout and stderr live, measures execution duration, and returns true OS exit codes.
  - Process cancellation and timeout protection.
  - Zero fabricated compiler or execution output.
- **Integrated Interactive Terminal:**
  - Real `/system/bin/sh` interactive shell session with persistent history, standard output, and process lifecycle management.
- **Live Web Preview:**
  - In-app sandboxed WebView previewer for HTML5, CSS3, and JavaScript projects with instant live refresh.
- **Problems & Diagnostics Panel:**
  - Real syntax validation (JSON, HTML/XML tag matching, Python delimiter checks) and live compiler error aggregation.
- **Source Control (Git Integration):**
  - Git repository initialization, working tree status, staged/unstaged file tracking, side-by-side diff viewer, and commit history.
- **Command & Action Palette:**
  - Quick action launcher for switching tabs, running scripts, jumping to tools, and toggling themes.
- **Privacy-First AI Coding Assistant:**
  - Explains selected code, suggests bug fixes, generates unit tests, and proposes structured file changes with diff review and explicit user approval before applying.
  - Zero source code leaks—runs in safe offline mode when no external API key is configured.
- **Dark & Light Design System:**
  - Deep charcoal palette (`#101216` background, `#15181E` editor, `#1D222B` panels, `#58A6FF` primary accent).

---

## Architecture & Module Organization

```
app/
  src/main/
    AndroidManifest.xml
    assets/
      editor/          # Offline touch-optimized editor (CodeMirror bridge, CSS, JS)
      templates/       # Starter templates (Python, Web, Kotlin, Rust, Go, C++)
    java/com/codeforge/studio/
      MainActivity.kt
      navigation/      # Navigation graph & Screen routes
      ui/
        theme/         # Design System (Color.kt, Theme.kt, Type.kt)
        components/    # AppTopBar, AppBottomBar, CommandPaletteDialog
        screens/       # 9 Required Screens (Welcome, Home, Editor, Terminal, Build, Problems, Git, AI, Settings)
      workspace/       # Project metadata, recent projects persistence, template generator
      filesystem/      # FileManager, FileNode, SafeZipManager, DraftManager
      editor/          # EditorTab, CodeEditorBridge (WebView bidirectional interface)
      execution/       # ProcessRunner, ExecutionResult, OutputLine
      terminal/        # TerminalSession (interactive Android shell)
      runtimes/        # LanguageRegistry, LanguageCapability, ExecutionMode
      sourcecontrol/   # GitManager, GitDiff, GitCommit, GitFileStatus
      diagnostics/     # DiagnosticsManager, DiagnosticIssue, Severity
      ai/              # AiAssistantManager, AiChangeProposal, AiMessage
      settings/        # SettingsManager (preferences persistence)
  src/test/            # Unit & Integration Tests (SafeZip, Git, Languages)
docs/
  runtime_capabilities.md
  build_instructions.md
  known_limitations.md
```

---

## Build Instructions

```powershell
# Set Java and Android SDK environment
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:ANDROID_HOME = "C:\Users\OLI\AppData\Local\Android\Sdk"

# Run Unit Tests
.\gradlew.bat testDebugUnitTest

# Assemble Debug APK
.\gradlew.bat assembleDebug
```
