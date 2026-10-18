# CodeForge Studio — Known Limitations & Troubleshooting Guide

## 1. Android Platform Constraints

- **Process Execution & SELinux Restrictions:**
  Android 10+ enforces W^X (Write XOR Execute) memory restrictions in app-private directories. Direct execution of arbitrary downloaded binaries without proper packaging or execution permissions is restricted by the Android OS. CodeForge Studio leverages the system shell (`/system/bin/sh`) or user-installed toolchains (e.g. via Termux or system PATH).
- **Storage Scoping:**
  Starting with Android 11 (API 30), Scoped Storage restricts direct file system access outside app-specific directories. Workspaces are created safely in app-private storage or accessed through the Android Storage Access Framework (SAF).
- **Background Task Killing:**
  Android OEM battery optimizations (e.g., Doze mode) may terminate long-running build processes if the app is placed in the background. Keep the app in the foreground during intensive builds or compile tasks.

---

## 2. Troubleshooting Common Issues

### Issue A: "Runtime Not Found (Exit 127)"
- **Cause:** The selected language compiler or interpreter (such as `python3`, `rustc`, or `clang`) is not installed on the Android device's PATH.
- **Resolution:**
  1. For Web apps (HTML/CSS/JS), switch to the "Web Live Preview" tab—no external binary required.
  2. For Python or native binaries, ensure the toolchain is accessible in your environment or configure an optional Remote Build Server in Settings.

### Issue B: "Malicious path detected in zip entry"
- **Cause:** A ZIP archive contains directory traversal paths (`../`) attempting to write outside the workspace directory.
- **Resolution:** CodeForge Studio's `SafeZipManager` automatically rejects unsafe archives to protect against Zip Slip vulnerabilities. Re-package the archive cleanly.
