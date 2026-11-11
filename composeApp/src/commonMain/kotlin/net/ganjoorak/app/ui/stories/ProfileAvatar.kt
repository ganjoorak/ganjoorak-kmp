package net.ganjoorak.app.ui.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import net.ganjoorak.app.AppGraph

private val UPLOADED_IMAGE = Regex("^img:([0-9a-f]{32})$")

fun mediaUrl(avatar: String?): String? {
    val id = avatar?.let { UPLOADED_IMAGE.matchEntire(it)?.groupValues?.get(1) } ?: return null
    return "${AppGraph.accountApiBaseUrl}/api/account/media/$id.jpg"
}

fun parseAccent(value: String?): Color? {
    val raw = value?.removePrefix("#") ?: return null
    val hex = when (raw.length) {
        3 -> raw.map { "$it$it" }.joinToString("")
        6 -> raw
        else -> return null
    }
    if (hex.any { it !in "0123456789abcdefABCDEF" }) return null
    val n = hex.toLongOrNull(16) ?: return null
    return Color((0xFF000000L or n).toInt())
}

/** Circular profile picture: an uploaded photo, or the first letter of the display name. */
@Composable
fun ProfileAvatar(
    username: String,
    displayName: String = username,
    avatar: String = "",
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.background,
    borderWidth: Dp = 2.dp,
) {
    val photo = mediaUrl(avatar)
    val letter = (displayName.ifBlank { username }).trim().firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .border(borderWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            AsyncImage(
                model = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        } else {
            Text(
                letter,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp,
            )
        }
    }
}
