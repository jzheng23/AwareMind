package com.jianzheng.studyzero.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.data.response.DismissalType
import com.jianzheng.studyzero.tool.MyDelayManager
import com.jianzheng.studyzero.ui.theme.StudyZeroEsmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EsmStressScreen(
    darkTheme: Boolean = isSystemInDarkTheme(),
    tag: String = "",
    unlockTime: Long = 0L,
    onClick: () -> Unit = {}
) {
    ShowStressOverlay(
        darkTheme = darkTheme,
        tag = tag,
        unlockTime = unlockTime,
        onClick = onClick
    )
}

@Composable
fun ShowStressOverlay(
    darkTheme: Boolean = isSystemInDarkTheme(),
    tag: String = "",
    unlockTime: Long = 0L,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val myViewModel: EsmViewModelInterface = if (!LocalInspectionMode.current) {
        EsmStressViewModel(context)
    } else {
        MockEsmStressViewModel()
    }
    val autoDismiss = remember { mutableStateOf(true) }
    val displayTimeMillis = 30000L

    // The driving / navigation skip happens in MyTriggerWorker before the overlay starts
    // (no side effects during composition).
    run {
        StudyZeroEsmTheme(darkTheme = darkTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.White.copy(alpha = 0.5f)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(
                            top = 40.dp,
                            end = mediumPadding,
                            start = mediumPadding,
                        ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = dimensionResource(id = R.dimen.elevation)
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.background(color = MaterialTheme.colorScheme.surface)
                    ) {
                        ShowStressInstruction(
                            autoDismissTimeMs = displayTimeMillis,
                            myViewModel = myViewModel,
                            onClick = {
                                autoDismiss.value = false
                                onClick()
                                if (tag == "count_random_met") MyDelayManager.recycleTrigger(context)
                            }
                        )
                        ShowStressEsm(
                            myViewModel = myViewModel
                        )
                        SubmitButton(
                            tag = tag,
                            unlockTime = unlockTime,
                            myViewModel = myViewModel,
                            isShowing = autoDismiss,
                            onClick = onClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShowStressInstruction(
    autoDismissTimeMs: Long = 30000L,
    myViewModel: EsmViewModelInterface,
    onClick: () -> Unit = {}
) {
    var timeLeft by remember { mutableLongStateOf(autoDismissTimeMs) }
    var isActive by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val startingTime = remember { System.currentTimeMillis() }

    LaunchedEffect(key1 = autoDismissTimeMs) {
        if (isActive && autoDismissTimeMs > 0) {
            timeLeft = autoDismissTimeMs
            while (timeLeft > 0 && isActive) {
                delay(50L)
                timeLeft -= 50L
            }
            if (timeLeft <= 0 && isActive) {
                isActive = false
                coroutineScope.launch(Dispatchers.IO) {
                    val delay = System.currentTimeMillis() - startingTime
                    myViewModel.saveDismissal(DismissalType.AUTO, delay, startingTime)
                }
                onClick()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = mediumPadding, end = mediumPadding, top = mediumPadding)
    ) {
        Text(
            text = stringResource(id = R.string.stress_question_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(vertical = 8.dp)
        )

        Box(
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            IconButton(
                onClick = {
                    isActive = false
                    coroutineScope.launch(Dispatchers.IO) {
                        val delay = System.currentTimeMillis() - startingTime
                        myViewModel.saveDismissal(DismissalType.MANUAL, delay, startingTime)
                    }
                    onClick()
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            if (autoDismissTimeMs > 0) {
                Canvas(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                ) {
                    val progress =
                        (timeLeft.toFloat() / autoDismissTimeMs.toFloat()).coerceIn(0f, 1f)

                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        radius = size.minDimension / 2,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawArc(
                        color = Color.DarkGray.copy(alpha = 0.7f),
                        startAngle = -90f,
                        sweepAngle = -360f * progress,
                        useCenter = false,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ShowStressEsm(
    myViewModel: EsmViewModelInterface,
    modifier: Modifier = Modifier
) {
    val uiState by myViewModel.uiState.collectAsState()
    val options = listOf(
        stringResource(id = R.string.stress_option_1),
        stringResource(id = R.string.stress_option_2),
        stringResource(id = R.string.stress_option_3),
        stringResource(id = R.string.stress_option_4),
        stringResource(id = R.string.stress_option_5)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = mediumPadding,
                vertical = mediumPadding / 2
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(id = R.dimen.elevation)),
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                .padding(
                    horizontal = mediumPadding,
                    vertical = mediumPadding
                )
                .selectableGroup()
        ) {
            options.forEachIndexed { index, optionText ->
                val optionIndex = index + 1
                val isSelected = (uiState.answer == optionIndex)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = isSelected,
                            onClick = { myViewModel.setAnswer(optionIndex) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Color(0xFF8CE631),
                            unselectedColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = optionText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewEsmStressLight() {
    ShowStressOverlay(darkTheme = false)
}

@Preview
@Composable
fun PreviewEsmStressDark() {
    ShowStressOverlay(darkTheme = true)
}
