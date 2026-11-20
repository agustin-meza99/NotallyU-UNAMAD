package com.example.notally.ui.components

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.notally.R
import com.example.notally.data.firebase.UsuarioFirebase
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Caché en memoria de las fotos de perfil.
 *
 * Se indexa por URL y guarda como mucho unas pocas imágenes de 36 dp: no toca
 * disco ni depende de una librería de carga de imágenes.
 */
private object CacheAvatares {
    private val cache = LruCache<String, ImageBitmap>(8)

    fun obtener(url: String): ImageBitmap? = cache.get(url)

    fun guardar(url: String, imagen: ImageBitmap) {
        cache.put(url, imagen)
    }
}

/**
 * Avatar circular de 36 dp del usuario con sesión.
 *
 * Si hay foto de Google se descarga en segundo plano (timeout de 10 s) y se
 * recorta en círculo; si falla o no hay foto, muestra la inicial del nombre o
 * del email sobre `primaryContainer`. Sin sesión no pinta nada, de modo que el
 * encabezado no deja un hueco.
 */
@Composable
fun AvatarCircle(
    usuario: UsuarioFirebase?,
    modifier: Modifier = Modifier,
) {
    if (usuario == null) return

    val url = usuario.photoUrl
    var imagen by remember(url) { mutableStateOf(url?.let(CacheAvatares::obtener)) }

    LaunchedEffect(url) {
        if (url == null || imagen != null) return@LaunchedEffect
        val descargada = cargarAvatar(url)
        if (descargada != null) {
            CacheAvatares.guardar(url, descargada)
            imagen = descargada
        }
    }

    val nombre = usuario.displayName?.takeIf { it.isNotBlank() }
    val descripcion = if (nombre != null) {
        stringResource(R.string.a11y_avatar_perfil, nombre)
    } else {
        stringResource(R.string.a11y_avatar_foto)
    }
    val inicial = (nombre ?: usuario.email.orEmpty())
        .trim()
        .firstOrNull()
        ?.uppercaseChar()
        ?.toString()
        ?: "?"

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .semantics(mergeDescendants = true) { contentDescription = descripcion },
        contentAlignment = Alignment.Center,
    ) {
        val foto = imagen
        if (foto != null) {
            Image(
                bitmap = foto,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = inicial,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** Descarga y decodifica la foto sin bloquear el hilo principal. */
private suspend fun cargarAvatar(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
    var conexion: HttpURLConnection? = null
    try {
        conexion = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = true
            requestMethod = "GET"
        }
        if (conexion.responseCode !in 200..299) return@withContext null
        val bytes = conexion.inputStream.use { it.readBytes() }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
        null
    } finally {
        conexion?.disconnect()
    }
}
