package com.example.unifiedapp.ui.home

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.unifiedapp.R
import java.io.IOException  // ADD THIS IMPORT

// Import colors from theme
import com.example.unifiedapp.theme.SurfaceWhite
import com.example.unifiedapp.theme.TextPrimary
import com.example.unifiedapp.theme.PurplePrimary
import com.example.unifiedapp.theme.SoftPurpleBg

// Define InputGreen locally since it's not in the theme
private val InputGreen = Color(0xFFE9F2E8)

// These colors are NOT in theme, define them locally with unique names
private val SunsetCoral = Color(0xFFFF9A8B)
private val SunrisePeach = Color(0xFFFFD6A5)
private val LavenderMist = Color(0xFFB8B5E6)
private val SkyBlue = Color(0xFFA7C7E7)
private val WarmYellow = Color(0xFFFFE5B4)
private val SoftMint = Color(0xFFB8E0D2)
private val RoseDust = Color(0xFFE8C7C8)
private val AmberGlow = Color(0xFFFFC9A2)

data class SoundItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val gradientStart: Color,
    val gradientEnd: Color,
    val accentColor: Color,
    val resId: Int
)

// SoundPlayerManager class
class SoundPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentSoundId: String? = null
    private var currentSoundResId: Int? = null
    private var volumeLevel: Float = 0.7f

    private val _isNowPlaying = mutableStateOf(false)
    val isNowPlaying: State<Boolean> = _isNowPlaying

    private val _activeSoundId = mutableStateOf<String?>(null)
    val activeSoundId: State<String?> = _activeSoundId

    private val _currentVolume = mutableStateOf(0.7f)
    val currentVolume: State<Float> = _currentVolume

    init {
        setupMediaPlayer()
    }

    private fun setupMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )

            setOnCompletionListener {
                _isNowPlaying.value = false
                currentSoundId?.let { soundId ->
                    currentSoundResId?.let { resId ->
                        playSound(soundId, resId)
                    }
                }
            }

            setOnErrorListener { _, what, extra ->
                println("MediaPlayer error: $what, $extra")
                _isNowPlaying.value = false
                true
            }
        }
    }

    fun playSound(soundId: String, soundResId: Int) {
        try {
            stopSound()

            currentSoundId = soundId
            currentSoundResId = soundResId

            val uri = Uri.parse("android.resource://${context.packageName}/$soundResId")

            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(context, uri)
            mediaPlayer?.prepareAsync()

            mediaPlayer?.setOnPreparedListener {
                it.isLooping = false
                it.setVolume(volumeLevel, volumeLevel)
                it.start()

                _isNowPlaying.value = true
                _activeSoundId.value = soundId
            }
        } catch (e: IOException) {
            e.printStackTrace()
            _isNowPlaying.value = false
        } catch (e: Exception) {
            e.printStackTrace()
            _isNowPlaying.value = false
        }
    }

    fun pauseSound() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isNowPlaying.value = false
            }
        }
    }

    fun resumeSound() {
        mediaPlayer?.let {
            if (!it.isPlaying && currentSoundId != null) {
                it.start()
                _isNowPlaying.value = true
            }
        }
    }

    fun stopSound() {
        mediaPlayer?.let {
            if (it.isPlaying || currentSoundId != null) {
                it.stop()
                it.reset()
                _isNowPlaying.value = false
                _activeSoundId.value = null
                currentSoundId = null
                currentSoundResId = null
            }
        }
    }

    fun setVolume(vol: Float) {
        volumeLevel = vol.coerceIn(0f, 1f)
        _currentVolume.value = volumeLevel
        mediaPlayer?.setVolume(volumeLevel, volumeLevel)
    }

    fun togglePlayPause(soundId: String, soundResId: Int) {
        if (currentSoundId == soundId && _isNowPlaying.value) {
            pauseSound()
        } else if (currentSoundId == soundId && !_isNowPlaying.value) {
            resumeSound()
        } else {
            playSound(soundId, soundResId)
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

// Sound library
val SOUNDS = listOf(
    SoundItem(
        "rain", "Gentle Rain", "Calming rainfall",
        Icons.Outlined.WaterDrop, SunsetCoral, SunrisePeach, SunsetCoral, R.raw.rain
    ),
    SoundItem(
        "ocean", "Ocean Waves", "Peaceful waves",
        Icons.Outlined.Waves, SkyBlue, LavenderMist, SkyBlue, R.raw.ocean
    ),
    SoundItem(
        "forest", "Forest Night", "Crickets & leaves",
        Icons.Outlined.Park, SoftMint, SoftPurpleBg, SoftMint, R.raw.forest
    ),
    SoundItem(
        "birds", "Morning Birds", "Dawn chorus",
        Icons.Outlined.EmojiNature, WarmYellow, SunrisePeach, WarmYellow, R.raw.birds
    ),
    SoundItem(
        "thunder", "Thunderstorm", "Distant thunder",
        Icons.Outlined.Thunderstorm, LavenderMist, RoseDust, LavenderMist, R.raw.thunder
    ),
    SoundItem(
        "piano", "Piano Dreams", "Soft melodies",
        Icons.Outlined.MusicNote, RoseDust, AmberGlow, RoseDust, R.raw.piano
    ),
    SoundItem(
        "flute", "Zen Flute", "Meditative tones",
        Icons.Outlined.SelfImprovement, AmberGlow, SunsetCoral, AmberGlow, R.raw.flute
    ),
    SoundItem(
        "fire", "Campfire", "Crackling fire",
        Icons.Outlined.Whatshot, SunsetCoral, AmberGlow, SunsetCoral, R.raw.fire
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val soundPlayer = remember { SoundPlayerManager(context) }

    DisposableEffect(Unit) {
        onDispose { soundPlayer.release() }
    }

    val activeSoundId by soundPlayer.activeSoundId
    val isNowPlaying by soundPlayer.isNowPlaying
    val currentVolumeLevel by soundPlayer.currentVolume

    val scrollState = rememberScrollState()
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            SoftPurpleBg.copy(alpha = 0.8f),
            SurfaceWhite
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Calming Sounds",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Find your peaceful moment",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White.copy(alpha = 0.7f)
                ),
                modifier = Modifier.clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 100.dp)
                ) {
                    // Welcome message
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Headphones,
                                contentDescription = null,
                                tint = SunsetCoral,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                "Close your eyes and let the sounds guide you to calmness",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Currently playing indicator (if any)
                    if (activeSoundId != null) {
                        val currentSound = SOUNDS.find { it.id == activeSoundId }
                        currentSound?.let { sound ->
                            CurrentlyPlayingCard(
                                sound = sound,
                                isNowPlaying = isNowPlaying,
                                onPause = { soundPlayer.pauseSound() },
                                onResume = { soundPlayer.resumeSound() },
                                onStop = { soundPlayer.stopSound() }
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    // Volume Control
                    VolumeControlCard(
                        volumeLevel = currentVolumeLevel,
                        onVolumeChange = { soundPlayer.setVolume(it) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // All Sounds Grid
                    Text(
                        "Sound Library",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Colorful sound grid
                    SOUNDS.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowItems.forEach { sound ->
                                SoundCard(
                                    sound = sound,
                                    isActiveSound = activeSoundId == sound.id,
                                    isNowPlaying = (activeSoundId == sound.id && isNowPlaying),
                                    modifier = Modifier.weight(1f),
                                    onClick = { soundPlayer.togglePlayPause(sound.id, sound.resId) }
                                )
                            }
                            if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Tips & Benefits Card
                    BenefitsCard()
                }
            }
        }
    }
}

@Composable
fun CurrentlyPlayingCard(
    sound: SoundItem,
    isNowPlaying: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(sound.gradientStart, sound.gradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isNowPlaying) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val heights = listOf(16.dp, 24.dp, 20.dp)
                        heights.forEachIndexed { index, heightValue ->
                            val animatedHeight by animateDpAsState(
                                targetValue = if (isNowPlaying) heightValue else 8.dp,
                                animationSpec = tween(500, easing = LinearEasing),
                                label = "bar_height_$index"
                            )
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .height(animatedHeight)
                                    .background(Color.White, RoundedCornerShape(3.dp))
                            )
                        }
                    }
                } else {
                    Icon(
                        sound.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    sound.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Text(
                    if (isNowPlaying) "Now Playing" else "Paused",
                    fontSize = 12.sp,
                    color = TextPrimary.copy(alpha = 0.6f)
                )
            }

            IconButton(
                onClick = if (isNowPlaying) onPause else onResume,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(sound.accentColor.copy(alpha = 0.15f))
            ) {
                Icon(
                    if (isNowPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isNowPlaying) "Pause" else "Play",
                    tint = sound.accentColor
                )
            }

            IconButton(
                onClick = onStop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SunsetCoral.copy(alpha = 0.15f))
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "Stop",
                    tint = SunsetCoral
                )
            }
        }
    }
}

@Composable
fun VolumeControlCard(
    volumeLevel: Float,
    onVolumeChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = SunsetCoral,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Volume",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
                Text(
                    "${(volumeLevel * 100).toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SunsetCoral
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Slider(
                value = volumeLevel,
                onValueChange = onVolumeChange,
                colors = SliderDefaults.colors(
                    thumbColor = SunsetCoral,
                    activeTrackColor = SunsetCoral,
                    inactiveTrackColor = InputGreen
                )
            )
        }
    }
}

@Composable
fun SoundCard(
    sound: SoundItem,
    isActiveSound: Boolean,
    isNowPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = isActiveSound

    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() }
            .then(
                if (isActive) Modifier.border(3.dp, sound.accentColor, RoundedCornerShape(24.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isActive) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White,
                            sound.gradientStart.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(sound.gradientStart, sound.gradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    sound.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                sound.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Surface(
                shape = CircleShape,
                color = when {
                    isNowPlaying -> sound.accentColor
                    isActiveSound && !isNowPlaying -> sound.accentColor.copy(alpha = 0.5f)
                    else -> InputGreen
                },
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        isNowPlaying -> Icon(
                            Icons.Default.Pause,
                            contentDescription = "Playing",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        isActiveSound && !isNowPlaying -> Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Paused",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        else -> Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BenefitsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(SunsetCoral, SunrisePeach)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Spa,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Benefits of Sound Therapy",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            val benefits = listOf(
                "Reduces stress and anxiety" to SunsetCoral,
                "Improves sleep quality" to SkyBlue,
                "Enhances focus and clarity" to SoftMint,
                "Lowers heart rate" to LavenderMist,
                "Promotes mindfulness" to WarmYellow
            )

            benefits.forEachIndexed { index, (benefit, color) ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${index + 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        benefit,
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}