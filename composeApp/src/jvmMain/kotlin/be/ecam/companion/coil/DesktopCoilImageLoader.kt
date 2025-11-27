package be.ecam.companion.coil

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.util.DebugLogger
import okhttp3.OkHttpClient

@Composable
@OptIn(ExperimentalCoilApi::class)
fun initDesktopCoilImageLoader() {
    setSingletonImageLoaderFactory { context ->
        val okHttp = OkHttpClient.Builder().build()
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(okHttp)) }
            .logger(DebugLogger())
            .build()
    }
}
