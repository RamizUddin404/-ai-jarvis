## 2026-09-06 - Stable item keys in Jetpack Compose LazyColumn

**Learning:** Jetpack Compose `LazyColumn` items without explicit `key` parameters default to using position as key, causing all items in the list to re-compose when items are inserted or list state changes.
**Action:** Always provide explicit stable keys (e.g., `key = { chat -> chat.id }`) in Compose `LazyColumn` / `LazyRow` items to enable item composition reuse and prevent unwanted recompositions.
