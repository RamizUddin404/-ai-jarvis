## 2025-02-21 - API Key Header Sanitization & Exception Information Leakage
**Vulnerability:** User-provided API keys in network requests could contain unsanitized control characters/newlines (`\r`, `\n`), risking HTTP Header Injection or OkHttp crashes, and raw exception messages leaked internal error details.
**Learning:** OkHttp strictly validates header characters and throws runtime exceptions on illegal characters; simultaneously, returning raw `e.message()` can leak sensitive network error details to consumers.
**Prevention:** Strip all whitespace and ASCII control characters (`[\s\r\n\t\u0000-\u001F]`) from API key values before header construction, and return sanitized error strings in exception handlers.
