package be.ecam.companion.data

import be.ecam.common.SERVER_PORT

actual fun defaultServerBaseUrl(): String = "http://192.168.1.1:$SERVER_PORT"
