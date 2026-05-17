package com.example.unifiedapp.ui.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.unifiedapp.R
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutBack
import androidx.compose.runtime.getValue
import com.example.unifiedapp.ui.views.QuestionnaireResponse
import com.example.unifiedapp.theme.SurfaceWhite
import com.example.unifiedapp.theme.PurplePrimary
import com.example.unifiedapp.theme.TextPrimary
//import androidx.compose.ui.graphics.Color.White
import com.example.unifiedapp.theme.SoftPurpleBg

// NOTE: Colors are imported from the theme file to avoid conflicts
// - SurfaceWhite, PurplePrimary, TextPrimary, Color.White, SoftPurpleBg are from theme

private val Gad7Options = listOf(
    Triple("Not at all", 0, R.drawable.clip1),
    Triple("Several days", 1, R.drawable.clip2),
    Triple("More than half the days", 2, R.drawable.clip3),
    Triple("Nearly every day", 3, R.drawable.clip4)
)

data class QuestionnaireQuestion(
    val id: Int,
    val text: String,
    val options: List<String>
)

data class GadQuestion(val text: String)

private val gad7Questions = listOf(
    GadQuestion("Feeling nervous, anxious, or on edge"),
    GadQuestion("Not being able to stop or control worrying"),
    GadQuestion("Worrying too much about different things"),
    GadQuestion("Trouble relaxing"),
    GadQuestion("Being so restless that it is hard to sit still"),
    GadQuestion("Becoming easily annoyed or irritable"),
    GadQuestion("Feeling afraid as if something awful might happen")
)

@Composable
fun EncouragementScreen(
    message: String,
    subMessage: String,
    onContinue: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PurplePrimary)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.female1),
                contentDescription = null,
                modifier = Modifier.size(114.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = subMessage,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.9f),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = PurplePrimary
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = "CONTINUE",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextPrimary,
                letterSpacing = 1.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoodCheckInFlow(onFlowComplete: (Int, Map<Int, QuestionnaireResponse>) -> Unit, onExit: () -> Unit) {
    val totalPages = gad7Questions.size
    val pagerState = rememberPagerState(pageCount = { totalPages })
    val scope = rememberCoroutineScope()
    val answers = remember { mutableStateMapOf<Int, Int>() }
    val responses = remember { mutableStateMapOf<Int, QuestionnaireResponse>() }

    val gad7QuestionsWithID = remember {
        listOf(
            QuestionnaireQuestion(
                id = 1,
                text = "Feeling nervous, anxious, or on edge",
                options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")
            ),
            QuestionnaireQuestion(
                id = 2,
                text = "Not being able to stop or control worrying",
                options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")
            ),
            QuestionnaireQuestion(
                id = 3,
                text = "Worrying too much about different things",
                options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")
            ),
            QuestionnaireQuestion(
                id = 4,
                text = "Trouble relaxing",
                options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")
            ),
            QuestionnaireQuestion(
                id = 5,
                text = "Being so restless that it's hard to sit still",
                options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")
            ),
            QuestionnaireQuestion(
                id = 6,
                text = "Becoming easily annoyed or irritable",
                options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")
            ),
            QuestionnaireQuestion(
                id = 7,
                text = "Feeling afraid as if something awful might happen",
                options = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")
            )
        )
    }

    var showIntermission by remember { mutableStateOf(false) }
    var intermissionData by remember { mutableStateOf("Great Job!" to "Keep going!") }

    val milestones = mapOf(
        2 to ("Doing Great!" to "We are here to support you."),
        5 to ("Almost There!" to "You're taking great care of yourself.")
    )

    fun advancePage(currentPage: Int) {
        if (milestones.containsKey(currentPage)) {
            intermissionData = milestones[currentPage]!!
            showIntermission = true
        } else {
            scope.launch {
                pagerState.animateScrollToPage(
                    currentPage + 1,
                    animationSpec = tween(durationMillis = 500, easing = EaseInOutQuart)
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false
        ) { page ->
            val questionIndex = page
            val currentQuestion = gad7QuestionsWithID[questionIndex]

            GadQuestionScreen(
                question = gad7Questions[questionIndex].text,
                currentSelection = answers[questionIndex],
                currentQuestionNumber = questionIndex + 1,
                totalQuestions = totalPages,
                isLast = page == totalPages - 1,
                onOptionSelected = { score ->
                    answers[questionIndex] = score
                    responses[currentQuestion.id] = QuestionnaireResponse(
                        selectedOption = score,
                        score = score,
                        timestamp = System.currentTimeMillis()
                    )

                    Log.d("MoodCheckIn", "Question ${currentQuestion.id}: option=$score")

                    scope.launch {
                        delay(250)
                        if (page == totalPages - 1) {
                            val totalScore = answers.values.sum()
                            Log.d("SCORE_DEBUG", "totalScore: $totalScore")
                            onFlowComplete(totalScore, responses.toMap())
                        } else {
                            advancePage(page)
                        }
                    }
                },
                onNext = {
                    if (page == totalPages - 1) {
                        val totalScore = answers.values.sum()
                        onFlowComplete(totalScore, responses.toMap())
                    } else {
                        advancePage(page)
                    }
                },
                onBack = {
                    if (page > 0) {
                        scope.launch {
                            pagerState.animateScrollToPage(page - 1)
                        }
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = showIntermission,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            EncouragementScreen(
                message = intermissionData.first,
                subMessage = intermissionData.second,
                onContinue = {
                    showIntermission = false
                    scope.launch {
                        pagerState.animateScrollToPage(
                            pagerState.currentPage + 1,
                            animationSpec = tween(durationMillis = 600, easing = EaseInOutBack)
                        )
                    }
                }
            )
        }
    }
}

// --- GAD-7 QUESTIONS SCREEN (WITH PROGRESS & QUESTION NUMBER) ---
@Composable
fun GadQuestionScreen(
    question: String,
    currentSelection: Int?,
    currentQuestionNumber: Int,
    totalQuestions: Int,
    isLast: Boolean,
    onOptionSelected: (Int) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftPurpleBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        LinearProgressIndicator(
            progress = currentQuestionNumber.toFloat() / totalQuestions,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = PurplePrimary,
            trackColor = Color.White.copy(alpha = 0.3f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Question $currentQuestionNumber of $totalQuestions",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Over the last 2 weeks...",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = question,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(Gad7Options) { (label, value, iconRes) ->
                val isSelected = currentSelection == value

                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) PurplePrimary else SurfaceWhite
                )
                val textColor = if (isSelected) Color.White else TextPrimary
                val borderColor = if (isSelected) PurplePrimary.copy(alpha = 0.5f) else Color.Transparent

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable { onOptionSelected(value) }
                        .border(2.dp, borderColor, RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(56.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentQuestionNumber > 1) {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("Back", color = TextPrimary)
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text(
                    text = if (isLast) "See Results" else "Next",
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// --- HELPERS ---
@Composable
fun MoodEmojiItem(emoji: String, label: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(
                    if (isSelected) Color.White.copy(alpha = 0.4f) else Color.Transparent,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 24.sp)
        }
        Text(text = label, fontSize = 12.sp, color = TextPrimary)
    }
}