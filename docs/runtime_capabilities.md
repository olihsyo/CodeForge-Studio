# CodeForge Studio — Runtime Capabilities & Toolchain Specification

## 1. Overview
CodeForge Studio implements a capability-driven execution registry (`LanguageRegistry`) covering 14 target languages. Runtimes are categorized into execution modes based on what the Android platform natively allows.

| Language | Extensions | Execution Mode | Syntax Highlight | Diagnostics | Toolchain Requirements |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Python** | `.py`, `.pyw` | Interpreted | Yes (100% Offline) | Bracket/Indentation Check | `python3` or `python` binary in PATH |
| **JavaScript** | `.js`, `.mjs`, `.cjs` | Web Sandbox / Node | Yes (100% Offline) | Syntax & Console capture | Built-in WebView V8 / Node.js |
| **TypeScript** | `.ts`, `.tsx` | Compiled | Yes (100% Offline) | AST check | `tsc` or Deno / Bun |
| **HTML** | `.html`, `.htm` | Web Sandbox | Yes (100% Offline) | Tag mismatch check | Built-in Android WebView |
| **CSS** | `.css`, `.scss` | Web Sandbox | Yes (100% Offline) | Property check | Built-in WebView Engine |
| **JSON** | `.json` | Web Sandbox | Yes (100% Offline) | Strict Parser Validation | Android `org.json` |
| **Kotlin** | `.kt`, `.kts` | Compiled | Yes (100% Offline) | Bracket check | `kotlinc` / JVM |
| **Java** | `.java` | Compiled | Yes (100% Offline) | Bracket check | `javac` & `java` |
| **C** | `.c`, `.h` | Compiled | Yes (100% Offline) | Bracket check | `clang` / `gcc` (NDK) |
| **C++** | `.cpp`, `.cc`, `.hpp` | Compiled | Yes (100% Offline) | Bracket check | `clang++` / `g++` |
| **Rust** | `.rs` | Compiled | Yes (100% Offline) | Bracket check | `rustc` & `cargo` |
| **Go** | `.go` | Compiled | Yes (100% Offline) | Bracket check | `go` toolchain |
| **Lua** | `.lua` | Interpreted | Yes (100% Offline) | Bracket check | `lua` / `luajit` |
| **C#** | `.cs` | Compiled | Yes (100% Offline) | Bracket check | `dotnet` SDK / Mono |

---

## 2. Real Execution vs Sandboxing Guarantees

In accordance with PRD Section 2, Rule 7:
> *"Never display fake execution results, simulated compiler success, or fabricated debugger output. Every feature must have a real implementation or be clearly marked as unavailable, planned, or dependent on an external service."*

- **When a runtime binary is present on the device** (e.g., Python or Clang in user environment/Termux/PATH): `ProcessRunner` executes the genuine binary, streams standard output and standard error line-by-line, tracks duration in milliseconds, and returns the real operating system exit code.
- **When a runtime binary is not found**: CodeForge Studio displays a clear, honest error message disclosing that the binary is missing, detailing the platform requirements, and providing instructions on how to install or connect an optional remote build runner.
- **Web Applications (HTML/CSS/JS)**: Execute completely offline inside an isolated WebView sandbox with real-time DOM rendering, CSS styling, and live JavaScript execution.
