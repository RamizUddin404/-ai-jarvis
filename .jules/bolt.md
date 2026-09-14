## 2026-03-31 - LazyColumn stable keys in Jetpack Compose
**Learning:** In Jetpack Compose `LazyColumn`/`LazyRow`, omitting `key` causes Compose to rely on position index. When items are added or conditional items (like typing indicators) toggle, Compose may recompose or re-evaluate unchanged items. Adding stable unique keys (e.g. `chat.id`) avoids unnecessary recompositions and item node re-creations.
**Action:** Always provide stable `key` lambdas for `items()` in Compose lists when items have unique IDs.
