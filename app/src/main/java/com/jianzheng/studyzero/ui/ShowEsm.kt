package com.jianzheng.studyzero.ui

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.service.MyForegroundService
import com.jianzheng.studyzero.tool.MyDelayManager
import com.jianzheng.studyzero.tool.MyPreferenceManager
import com.jianzheng.studyzero.tool.ResponseCounter
import com.jianzheng.studyzero.ui.theme.StudyZeroEsmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ShowOverlay(
    darkTheme: Boolean = isSystemInDarkTheme(),
    tag: String = "",
    unlockTime: Long = 0L,
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var myViewModel: EsmViewModelInterface
    val autoDismiss = remember { mutableStateOf(true) }
    //TODO need to tidy up

    if (!LocalInspectionMode.current) {
        myViewModel = EsmViewModel(context)
        val handler = Handler(Looper.myLooper()!!)
        val displayTimeMillis = if (
            MyPreferenceManager.getBoolean(context, "using_testing_triggers")
        ) 10000L else 30000L //10 seconds for testing, 30 seconds for real data collection
        handler.postDelayed({
            if (autoDismiss.value) {
                autoDismiss.value = false
                onClick()
                if (tag == "randomMet") MyDelayManager.recycleTrigger()
                Log.d("unlock", "unanswered")
            }
        }, displayTimeMillis) // 10 seconds delay
    } else {

        myViewModel = MockEsmViewModel()
    }
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
//                        bottom = mediumPadding
                    ),
                elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(id = R.dimen.elevation)),
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(color = MaterialTheme.colorScheme.surface)
                ) {
                    ShowEsm(
                        myViewModel = myViewModel,
//                        autoDismiss = autoDismiss,
                        onClick = onClick
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

@Composable
fun SubmitButton(
    tag: String = "",
    unlockTime: Long = 0L,
    myViewModel: EsmViewModelInterface,
    isShowing: MutableState<Boolean> = mutableStateOf(false),
    onClick: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val startingTime = System.currentTimeMillis()
    Log.d("delayTime", "displaying: ${System.currentTimeMillis()}")
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
                val myForegroundServiceIntent = Intent(context, MyForegroundService::class.java)
                ContextCompat.startForegroundService(context, myForegroundServiceIntent)
            }
            isShowing.value = false
            responseCounter.responsePlusOne(tag)
            onClick()
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
fun ShowEsm(
    myViewModel: EsmViewModelInterface,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .background(color = MaterialTheme.colorScheme.surface)
    ) {

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.instruction),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = mediumPadding),
                textAlign = TextAlign.Center
            )

            IconButton(
                onClick = {
                    onClick()
                    MyDelayManager.recycleTrigger()
                },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        //show question 1
        ShowQuestion(
            questionString = R.string.question1,
            index = 0,
            myViewModel = myViewModel,
            labelSet = 1
//            autoDismiss = autoDismiss
        )
        ShowQuestion(
            questionString = R.string.question2,
            index = 1,
            myViewModel = myViewModel,
            labelSet = 2
//            autoDismiss = autoDismiss
        )
    }
}


@Composable
fun ShowQuestion(
    questionString: Int,
    modifier: Modifier = Modifier,
    index: Int,
    myViewModel: EsmViewModelInterface,
    labelSet: Int,
//    autoDismiss: MutableState<Boolean>
) {
    val options: List<Int> = listOf(1, 2, 3, 4, 5)
    val labels1: List<String> = listOf("Very passively", "", "Moderately", "", "Very actively")
    val labels2: List<String> = listOf("Strongly disagree", "", "Neutral", "", "Strongly agree")
    val labels = if (labelSet == 1) labels1 else labels2
    val selectedValue = remember { mutableIntStateOf(0) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = mediumPadding, vertical = mediumPadding / 2),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(id = R.dimen.elevation)),
        shape = RoundedCornerShape(8.dp),
//        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
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
            Text(
                stringResource(questionString),
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = LocalTextStyle.current.merge(
                    TextStyle(
                        lineHeight = 1.0.em,
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        ),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None
                        )
                    )
                ),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = modifier
                    //.height(28.dp)
                    .fillMaxWidth()
//                    .padding(top = dimensionResource(id = R.dimen.padding_medium))
            ) {
                options.forEach { item ->
                    ChoiceWithLabel(
                        item = item,
                        label = labels[item - 1],
                        index = index,
                        myViewModel = myViewModel,
//                        autoDismiss = autoDismiss,
                        selectedValue = selectedValue
                    )
                }
            }
        }
    }
}

@Composable
fun ChoiceWithLabel(
    item: Int,
    label: String,
    index: Int,
    myViewModel: EsmViewModelInterface,
    selectedValue: MutableState<Int>,
//    autoDismiss: MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        RadioButton(
            selected = selectedValue.value == item,
            onClick =
            {
                selectedValue.value = item
//                autoDismiss.value = false
                myViewModel.setAnswer(item, index)
                Log.d("viewModel", "$item selected for q$index!")
            },
            modifier = modifier
                .height(22.dp)
        )
        if (label != "") {
            Text(
                text = label,//s[item - 1],
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                style = LocalTextStyle.current.merge(
                    TextStyle(
                        lineHeight = 1.0.em,
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        ),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None
                        )
                    )
                ),
                modifier = modifier
                    .width(68.dp)

            )
        }

    }
}


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



