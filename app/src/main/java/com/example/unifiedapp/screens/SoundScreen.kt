package com.example.unifiedapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unifiedapp.R
import com.example.unifiedapp.utils.SoundPlayerManager

// Data model for our soundscapes - Updated to include resource ID
data class SoundItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val resId: Int  // Added resource ID for audio files
)

// Updated SOUNDS list with resource IDs
val SOUNDS = listOf(
    SoundItem("rain", "Rain", "Gentle rainfall", Icons.Default.WaterDrop,
        listOf(Color(0xFF60A5FA), Color(0xFF22D3EE)), R.raw.rain),
    SoundItem("ocean", "Ocean Waves", "Peaceful waves", Icons.Default.Waves,
        listOf(Color(0xFF22D3EE), Color(0xFF3B82F6)), R.raw.ocean),
    SoundItem("forest", "Forest", "Nature sounds", Icons.Default.Air,
        listOf(Color(0xFF4ADE80), Color(0xFF10B981)), R.raw.forest),
    SoundItem("birds", "Birds", "Morning chirping", Icons.Default.EmojiNature,
        listOf(Color(0xFFFACC15), Color(0xFFFB923C)), R.raw.birds),
    SoundItem("thunder", "Thunderstorm", "Distant thunder", Icons.Default.Cloud,
        listOf(Color(0xFFC084FC), Color(0xFFF472B6)), R.raw.thunder),
    SoundItem("piano", "Piano", "Soft piano music", Icons.Default.MusicNote,
        listOf(Color(0xFFF472B6), Color(0xFFFB923C)), R.raw.piano)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // Create and remember the sound player manager
    val soundPlayer = remember { SoundPlayerManager(context) }

    // Dispose the player when the composable leaves the composition
    DisposableEffect(Unit) {
        onDispose {
            soundPlayer.release()
        }
    }

    // Observe states from the player
    val playingId by soundPlayer.currentSoundIdState
    val isPlaying by soundPlayer.isPlayingState
    val volume by soundPlayer.volumeState

    val scrollState = rememberScrollState()
    val orangePinkGradient = Brush.linearGradient(listOf(Color(0xFFFB923C), Color(0xFFF472B6)))
    val purplePinkGradient = Brush.linearGradient(listOf(Color(0xFFA855F7), Color(0xFFEC4899)))
    val milkyWhite = Color.White.copy(alpha = 0.4f)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = milkyWhite,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button - black like before
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Icon with gradient background
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(purplePinkGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Title and subtitle - original colors
                    Column {
                        Text(
                            "Calming Sounds",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            "Nature sounds & ambient music",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Volume Control Card
                    VolumeCard(
                        volume = volume,
                        onVolumeChange = { soundPlayer.setVolume(it) },
                        gradient = orangePinkGradient
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sound Grid (2 columns)
                    SOUNDS.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { sound ->
                                SoundCard(
                                    sound = sound,
                                    isPlaying = playingId == sound.id,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        soundPlayer.togglePlayPause(sound.id, sound.resId)
                                    }
                                )
                            }
                            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Tips Card
                    TipsCard(gradient = orangePinkGradient)

                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // Floating "Now Playing" Bar
            if (playingId != null) {
                val currentSound = SOUNDS.find { it.id == playingId }
                NowPlayingBar(
                    sound = currentSound,
                    isPlaying = isPlaying,
                    onPlayPause = {
                        if (currentSound != null) {
                            soundPlayer.togglePlayPause(currentSound.id, currentSound.resId)
                        }
                    },
                    onStop = {
                        soundPlayer.stopSound()
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
fun VolumeCard(volume: Float, onVolumeChange: (Float) -> Unit, gradient: Brush) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Volume", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text("${(volume * 100).toInt()}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                }
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFFB923C),
                        activeTrackColor = Color(0xFFFB923C)
                    )
                )
            }
        }
    }
}

@Composable
fun SoundCard(sound: SoundItem, isPlaying: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .then(
                if (isPlaying) Modifier.border(2.dp, Color(0xFFFB923C), RoundedCornerShape(24.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(sound.gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(sound.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(sound.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(sound.description, fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(12.dp))

            // Toggle Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .then(
                        if (isPlaying)
                            Modifier.background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFB923C), Color(0xFFF472B6))
                                )
                            )
                        else
                            Modifier.background(Color(0xFFF3F4F6))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isPlaying) Color.White else Color.DarkGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun TipsCard(gradient: Brush) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lightbulb, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Tips for Best Experience", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            val tips = listOf(
                "Use headphones for immersive experience",
                "Find a comfortable, quiet space",
                "Close your eyes and focus on the sounds"
            )
            tips.forEachIndexed { index, tip ->
                Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(24.dp).clip(CircleShape).background(gradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${index + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(tip, fontSize = 14.sp, color = Color.DarkGray)
                }
            }
        }
    }
}

@Composable
fun NowPlayingBar(
    sound: SoundItem?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (sound == null) return
    Card(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Brush.linearGradient(sound.gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(sound.icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(sound.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    if (isPlaying) "Now Playing" else "Paused",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            // Play/Pause button
            IconButton(onClick = onPlayPause) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color(0xFFFB923C),
                    modifier = Modifier.size(24.dp)
                )
            }



            // Stop button
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFB923C)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Stop, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Stop")
            }
        }
    }
}