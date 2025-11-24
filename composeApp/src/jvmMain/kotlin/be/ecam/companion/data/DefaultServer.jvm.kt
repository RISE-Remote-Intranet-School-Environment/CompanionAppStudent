package be.ecam.companion.data

import be.ecam.common.SERVER_PORT

// Desktop apps typically run the server locally; default to localhost to avoid timeouts.
actual fun defaultServerBaseUrl(): String = "http://127.0.0.1:$SERVER_PORT"
