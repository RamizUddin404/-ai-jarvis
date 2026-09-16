## 2025-05-18 - Jetpack Compose LazyColumn Keying Optimization
**Learning:** `LazyColumn` items in Jetpack Compose without explicit keys fall back to item position as key. Adding stable keys (e.g. database ID `chat.id`) prevents full-list item recompositions on list updates.
**Action:** Always provide stable `key` functions for Compose `LazyColumn` / `LazyRow` items when backed by models with unique IDs.
