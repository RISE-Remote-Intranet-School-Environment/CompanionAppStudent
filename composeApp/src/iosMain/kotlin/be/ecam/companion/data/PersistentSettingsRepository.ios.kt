package be.ecam.companion.data

import be.ecam.common.SERVER_PORT
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSUserDefaults

class PersistentSettingsRepository : SettingsRepository {
    private val defaults = NSUserDefaults.standardUserDefaults()

    private val hostFlow = MutableStateFlow(loadHost())
    private val portFlow = MutableStateFlow(loadPort())

    override val serverHostFlow: Flow<String> = hostFlow
    override val serverPortFlow: Flow<Int> = portFlow

    override fun getServerHost(): String = hostFlow.value
    override fun getServerPort(): Int = portFlow.value

    override fun setServerHost(host: String) {
        hostFlow.value = host
        defaults.setObject(host, forKey = KEY_HOST)
    }

    override fun setServerPort(port: Int) {
        portFlow.value = port
        defaults.setInteger(port.toLong(), forKey = KEY_PORT)
    }

    private fun loadHost(): String {
        val def = defaultServerBaseUrl().removePrefix("http://").removePrefix("https://").substringBefore(":")
        val value = defaults.stringForKey(KEY_HOST)
        return value ?: def
    }

    private fun loadPort(): Int {
        val stored = defaults.objectForKey(KEY_PORT)
        return when (stored) {
            is Long -> stored.toInt()
            is Int -> stored
            else -> SERVER_PORT
        }
    }

    companion object {
        private const val KEY_HOST = "server_host"
        private const val KEY_PORT = "server_port"
    }
}
