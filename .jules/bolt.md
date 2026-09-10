## 2025-05-18 - Compose Canvas Drawing Optimization in List Items

**Learning:** Using `Modifier.drawBehind` with dynamic `Brush.linearGradient` or `Brush.radialGradient` allocations inside the drawing block creates fresh `Brush` and `Shader` allocations on every animation frame during list scrolling. Replacing `drawBehind` with `drawWithCache` allows pre-allocating or caching `Brush` objects in the cache lambda, which are only updated when layout or animation parameters change, drastically reducing Garbage Collection (GC) pressure and frame drops during high-frequency list scrolling with custom-rendered canvas backgrounds.

**Action:** Whenever custom backgrounds using gradients/brushes are drawn on Jetpack Compose composables—especially inside repeated list items like `LazyColumn` cards—prefer `Modifier.drawWithCache` over `Modifier.drawBehind` to avoid garbage allocation during frame rendering.
