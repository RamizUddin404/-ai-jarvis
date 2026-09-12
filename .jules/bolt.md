## 2025-05-20 - Jetpack Compose LazyColumn Stable Item Keys
**Learning:** In Jetpack Compose, omitting explicit keys in `LazyColumn` items causes Compose to fallback to item indices. When new messages or items are appended/updated, Compose cannot preserve composable identity across recompositions, causing unnecessary recompositions for all visible items.
**Action:** Always provide explicit, unique, and stable keys (e.g. `key = { it.id }`) for items rendered within `LazyColumn` and `LazyRow`.
