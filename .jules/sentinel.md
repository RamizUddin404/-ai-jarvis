## 2025-03-05 - API Key Leakage Prevention in Exception Messages
**Vulnerability:** Raw exception strings in network error handling could potentially echo or contain API keys passed in request parameters or headers.
**Learning:** Exception messages thrown by network layers or custom error handlers might leak sensitive credential strings directly to UI error toasts or logs.
**Prevention:** Always sanitize exception messages prior to exposing them in error states by stripping/replacing sensitive API key strings with `***`.
