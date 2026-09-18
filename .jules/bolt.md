## 2025-05-18 - Jetpack Compose LazyColumn Keying & Formatter Reuse
**Learning:** Supplying stable item keys (`key = { chat -> chat.id }`) in `LazyColumn` prevents full-list item recomposition on list updates in Jetpack Compose. Additionally, caching `SimpleDateFormat` in a `ThreadLocal` prevents repeated allocations during frequent item renders.
**Action:** Always provide stable keys for Compose `LazyColumn`/`LazyRow` items and avoid creating date formatters inside Composable scopes.
