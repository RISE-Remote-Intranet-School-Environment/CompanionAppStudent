package be.ecam.companion.data

// Provides a platform-specific default base URL for the server used by settings repositories.
// The value is expected to be a full URL including scheme and port (e.g., "http://localhost:28088").
expect fun defaultServerBaseUrl(): String
