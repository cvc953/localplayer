import com.cvc953.localplayer.R
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cvc953.localplayer.model.Song
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    onNext: () -> Unit
) {
    val context = LocalContext.current
    var albumArt by remember { mutableStateOf<Bitmap?>(null) }

    // Cargar la carátula de forma segura
    LaunchedEffect(song.uri) {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, song.uri)
            retriever.embeddedPicture?.let {
                albumArt = BitmapFactory.decodeByteArray(it, 0, it.size)
            }
            retriever.release()
        } catch (_: Exception) {}
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121212)) // color similar a la app
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = albumArt?.asImageBitmap()?.let { BitmapPainter(it) }
                ?: painterResource(R.drawable.ic_default_album), // Reemplaza por un recurso que tengas
            contentDescription = null,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.title, color = Color.White, maxLines = 1, fontWeight = FontWeight.SemiBold)
            Text(text = song.artist, color = Color.Gray, fontSize = 12.sp, maxLines = 1)
        }

        IconButton(onClick = onPlayPause) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White
            )
        }

        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.Default.SkipNext,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}
