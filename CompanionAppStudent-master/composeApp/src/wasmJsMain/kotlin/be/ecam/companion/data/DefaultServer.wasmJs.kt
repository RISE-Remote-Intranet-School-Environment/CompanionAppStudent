package be.ecam.companion.data

import be.ecam.common.SERVER_PORT

actual fun defaultServerBaseUrl(): String = "http://127.0.0.1:$SERVER_PORT"
