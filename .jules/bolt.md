## 2025-05-10 - Memoize PathEffect Allocations in Canvas Animations
**Learning:** In Jetpack Compose Canvas drawing loops driven by `rememberInfiniteTransition` or animation states, creating native graphics objects like `PathEffect.dashPathEffect(floatArrayOf(...))` on every frame allocates `FloatArray` and underlying native C++ `PathEffect` instances continuously, triggering frequent Garbage Collection pauses (jank).
**Action:** Use `remember` or `remember(key)` to cache `FloatArray` and `PathEffect` instances across frame draws when parameters change predictably or remain constant.
