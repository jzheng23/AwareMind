package com.jianzheng.studyzero.ui


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.data.response.DismissalType
import com.jianzheng.studyzero.tool.MyDelayManager
import com.jianzheng.studyzero.tool.MyPreferenceManager
import com.jianzheng.studyzero.tool.MyUsageManager
import com.jianzheng.studyzero.tool.ResponseCounter
import com.jianzheng.studyzero.tool.startMyForegroundService
import com.jianzheng.studyzero.ui.theme.StudyZeroEsmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ShowOverlay(
    darkTheme: Boolean = isSystemInDarkTheme(),
    tag: String = "",
    unlockTime: Long = 0L,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val myViewModel: EsmViewModelInterface = if (!LocalInspectionMode.current) {
        EsmViewModel(context)
    } else {
        MockEsmViewModel()
    }
    val autoDismiss = remember { mutableStateOf(true) }
    //TODO need to tidy up

//    if (!LocalInspectionMode.current) {
//        val handler = Handler(Looper.myLooper()!!)

//        handler.postDelayed({
//            if (autoDismiss.value) {
//                autoDismiss.value = false
//                onClick()
//                if (tag == "count_random_met") MyDelayManager.recycleTrigger()
////                Log.d("unlock", "unanswered")
//            }
//        }, displayTimeMillis) // 10 seconds delay
//    }
    val displayTimeMillis = if (
        MyPreferenceManager.getBoolean(context, "is_driving") or MyUsageManager.isNavigationAppCurrentlyInUse(context)
    ) 100L else if (
        MyPreferenceManager.getBoolean(context, "using_testing_triggers")
    ) 10000L else 30000L //10 seconds for testing, 30 seconds for real data collection

    if (
        !MyPreferenceManager.getBoolean(context, "is_driving")
    ) {
        StudyZeroEsmTheme(darkTheme = darkTheme) {
            Surface(
                modifier = Modifier
                    .fillMaxSize(),
                color = Color.White.copy(alpha = 0.5f),
//            onClick = onClick
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
                        defaultElevation = dimensionResource(
                            id = R.dimen.elevation
                        )
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .background(color = MaterialTheme.colorScheme.surface)
                    ) {
                        ShowInstruction(
                            autoDismissTimeMs = displayTimeMillis,
                            myViewModel = myViewModel,
                            onClick = {
                                autoDismiss.value = false
                                onClick()
                                if (tag == "count_random_met") MyDelayManager.recycleTrigger()
                            }
                        )
                        ShowEsm(
                            myViewModel = myViewModel,
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
fun SubmitButton(
    tag: String = "count_random_met",
    unlockTime: Long = 0L,
    myViewModel: EsmViewModelInterface,
    isShowing: MutableState<Boolean> = mutableStateOf(false),
    onClick: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val startingTime = System.currentTimeMillis()
//    Log.d("delayTime", "displaying: ${System.currentTimeMillis()}")
    val delay = startingTime - unlockTime
    val context = LocalContext.current
    val responseCounter = ResponseCounter(context)
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = mediumPadding,
                end = mediumPadding,
                top = mediumPadding / 2,
                bottom = mediumPadding
            ),
        enabled = myViewModel.uiState.collectAsState().value.isAnswerValid,
        onClick = {
            coroutineScope.launch(Dispatchers.IO) {
                myViewModel.saveAnswer(delay, startingTime)
//                val myForegroundServiceIntent = Intent(context, MyForegroundService::class.java)
//                ContextCompat.startForegroundService(context, myForegroundServiceIntent)
            }
            responseCounter.responsePlusOne(tag)
            if (tag != "count_test_met") {
                startMyForegroundService(context)
            }
            isShowing.value = false
            onClick()
//            Log.d("data", "submit clicked")
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = stringResource(id = R.string.submit),
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
fun ShowInstruction(
    autoDismissTimeMs: Long = 30000L,
    myViewModel: EsmViewModelInterface,
    onClick: () -> Unit = {}
) {
    var timeLeft by remember { mutableLongStateOf(autoDismissTimeMs) }
    var isActive by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val startingTime = remember { System.currentTimeMillis() }

    // Countdown timer
    LaunchedEffect(key1 = autoDismissTimeMs) {
        if (isActive && autoDismissTimeMs > 0) {
            timeLeft = autoDismissTimeMs // Reset when autoDismissTimeMs changes
            while (timeLeft > 0 && isActive) {
                delay(50L) // Update every 50ms for smooth animation
                timeLeft -= 50L
            }
            if (timeLeft <= 0 && isActive) {
                // Auto dismiss
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
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = R.string.question2),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(
                    top = mediumPadding,
                    end = mediumPadding * 3
                )
        )

        // Close button with countdown circle
        Box(
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            // Close button
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

            // Countdown circle - centered over the button
            if (autoDismissTimeMs > 0) {
                Canvas(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center) // Center the canvas over the IconButton
                ) {
                    val progress = (timeLeft.toFloat() / autoDismissTimeMs.toFloat()).coerceIn(0f, 1f)


                    // Background circle
                    drawCircle(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        radius = size.minDimension / 2,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    // Progress circle
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
fun ShowEsm(
    myViewModel: EsmViewModelInterface,
) {
    Column(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.surface)
    ) {
        //show question 1
//        ShowQuestion(
//            questionString = R.string.question1,
//            index = 0,
//            myViewModel = myViewModel,
//            labelSet = 1
////            autoDismiss = autoDismiss
//        )
        ShowQuestion(
//            questionString = R.string.question2,
            myViewModel = myViewModel,
            labelSet = 2
//            autoDismiss = autoDismiss
        )
    }
}


@Composable
fun ShowQuestion(
//    questionString: Int,
    modifier: Modifier = Modifier,
    myViewModel: EsmViewModelInterface,
    labelSet: Int,
) {
    val labels1: List<String> = listOf("passively", "actively")
    val labels2: List<String> = listOf("stop", "continue")
    val labels = if (labelSet == 1) labels1 else labels2
    val sliderPosition = remember { mutableStateOf<Float?>(null) }

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
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                .padding(
                    horizontal = mediumPadding,
                    vertical = mediumPadding / 2
                )
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                if (sliderPosition.value != null) {
                    Slider(
                        value = sliderPosition.value!!,
                        onValueChange = { newValue ->
                            sliderPosition.value = newValue
                            val answer = (newValue * 9).toInt() + 1
                            myViewModel.setAnswer(answer)
                        },
                        valueRange = 0f..1f,
                        steps = 8,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(top = 8.dp)
                            .pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val sliderWidth = size.width
                                    val horizontalOuterPadding = 2.dp.toPx()
                                    val horizontalInnerPadding = 8.dp.toPx()
                                    val availableWidth = sliderWidth - (2 * horizontalOuterPadding)
                                    val adjustedStepWidth =
                                        (availableWidth - 2 * horizontalInnerPadding) / 9f
                                    val adjustedOffset =
                                        (offset.x - horizontalOuterPadding - horizontalInnerPadding).coerceIn(
                                            0f,
                                            availableWidth - 2 * horizontalInnerPadding
                                        )
                                    val tappedStep =
                                        (adjustedOffset / adjustedStepWidth).roundToInt()
                                            .coerceIn(0, 9)
                                    val newValue = tappedStep / 9f
                                    // Set the slider to the tapped position
                                    sliderPosition.value = newValue
                                    val answer = (newValue * 9).toInt() + 1
                                    myViewModel.setAnswer(answer)
                                }
                            }
                    ) {
                        // Draw custom track and tick marks to match Material Design slider
                        val trackColor = MaterialTheme.colorScheme.outlineVariant
                        val tickColor = MaterialTheme.colorScheme.primary// For tick marks

// Draw custom track and tick marks to match Material Design slider
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                        ) {
                            val trackWidth = size.width
                            val trackHeight = 16.dp.toPx()
                            val tickRadius = 5f

                            // Draw 10 tick marks (positions 0-9)
                            val horizontalInnerPadding = 8.dp.toPx()
                            val horizontalOuterPadding = 2.dp.toPx()
                            val availableWidth = trackWidth - (2 * horizontalOuterPadding)
                            val adjustedStepWidth = (availableWidth - 2 * horizontalInnerPadding) / 9f
                            val verticalOffset = 4.dp.toPx()

                            drawRoundRect(
                                color = trackColor, // Uses outline color for inactive track
                                topLeft = Offset(
                                    horizontalOuterPadding,
                                    -trackHeight / 2 + verticalOffset
                                ),
                                size = androidx.compose.ui.geometry.Size(
                                    availableWidth,
                                    trackHeight
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                    trackHeight / 2,
                                    trackHeight / 2
                                )
                            )

                            for (i in 0..9) {
                                val x = horizontalInnerPadding + horizontalOuterPadding + (i * adjustedStepWidth)
                                drawCircle(
                                    color = tickColor, // Uses onSurfaceVariant for tick marks
                                    radius = tickRadius,
                                    center = Offset(x, verticalOffset)
                                )
                            }
                        }
                    }
                }

                // Labels at start and end
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = labels[0],
                        fontSize = 12.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.offset(x = (-4).dp)
                    )
                    Text(
                        text = labels[1],
                        fontSize = 12.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.offset(x = (12).dp)
                    )
                }
            }
        }
    }
}


//@Composable
//fun ChoiceWithLabel(
//    item: Int,
//    label: String,
//    index: Int,
//    myViewModel: EsmViewModelInterface,
//    selectedValue: MutableState<Int>,
////    autoDismiss: MutableState<Boolean>,
//    modifier: Modifier = Modifier,
//) {
//    Column(
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally,
//        modifier = modifier
//    )
//    {
//        RadioButton(
//            selected = selectedValue.value == item,
//            onClick =
//                {
//                    selectedValue.value = item
////                autoDismiss.value = false
//                    myViewModel.setAnswer(item, index)
////                Log.d("viewModel", "$item selected for q$index!")
//                },
//            modifier = Modifier
//                .height(22.dp)
//        )
//        if (label != "") {
//            Text(
//                text = label,//s[item - 1],
//                fontSize = 12.sp,
//                textAlign = TextAlign.Center,
//                style = LocalTextStyle.current.merge(
//                    TextStyle(
//                        lineHeight = 1.0.em,
//                        platformStyle = PlatformTextStyle(
//                            includeFontPadding = false
//                        ),
//                        lineHeightStyle = LineHeightStyle(
//                            alignment = LineHeightStyle.Alignment.Center,
//                            trim = LineHeightStyle.Trim.None
//                        )
//                    )
//                ),
//
//                )
//        }
//
//    }
//}


@Preview
@Composable
fun PreviewEsmLight() {
    ShowOverlay(darkTheme = false)
}

@Preview
@Composable
fun PreviewEsmDark() {
    ShowOverlay(darkTheme = true)
}



