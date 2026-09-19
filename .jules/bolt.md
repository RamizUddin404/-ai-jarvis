## 2025-05-18 - [Jetpack Compose LazyColumn Keys & Thread Safety]
**Learning:** `java.text.SimpleDateFormat` is not thread-safe and should not be shared as a global singleton across composables. In `LazyColumn`, providing explicit stable keys (`key = { chat -> chat.id }`) prevents unnecessary recompositions when chat items update or reorder.
**Action:** Always provide stable keys for LazyColumn items and use `remember(key)` for per-composable instance caching or `java.time.format.DateTimeFormatter` for immutable thread-safe date formatting.
