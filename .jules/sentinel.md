## 2025-03-05 - API Key Leakage Prevention in Exception Messages
**Vulnerability:** Raw exception strings in network error handling could potentially echo or contain API keys passed in request parameters or headers.
**Learning:** Exception messages thrown by network layers or custom error handlers might leak sensitive credential strings directly to UI error toasts or logs.
**Prevention:** Always sanitize exception messages prior to exposing them in error states by stripping/replacing sensitive API key strings with `***`.

## 2026-09-06 - Password Disclosure via Accessibility Text-To-Speech
**Vulnerability:** Accessibility events fired when focusing or typing in password fields could be read aloud by Text-To-Speech screen reader logic.
**Learning:** Custom accessibility services processing `TYPE_VIEW_FOCUSED` or `TYPE_WINDOW_STATE_CHANGED` events might speak password contents if `event.isPassword` or `event.source?.isPassword` is not explicitly checked.
**Prevention:** Always inspect `event.isPassword` and `event.source?.isPassword` in accessibility event handlers and skip speech generation for password fields.
