# Bolt Journal

## Codebase Performance Patterns
- Room DAOs query `chat_history` by `timestamp` and `study_sessions` by `startTime`. Ensure indices are defined on sorted fields in `@Entity` definitions to avoid full table scans during reactive Flow queries.
