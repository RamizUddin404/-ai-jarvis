## 2025-05-18 - Avoid SimpleDateFormat allocations inside Jetpack Compose list items
**Learning:** Instantiating `SimpleDateFormat` inside `@Composable` functions (or even inside `remember(timestamp)`) allocates new pattern parsers, calendars, and date format symbols on every timestamp change across list items, adding GC pressure during scrolling.
**Action:** Use a thread-safe static or `ThreadLocal<SimpleDateFormat>` instance for date/time formatting in Jetpack Compose list items.
