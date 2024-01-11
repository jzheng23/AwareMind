package com.jianzheng.studyzero.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.jianzheng.studyzero.R
import com.jianzheng.studyzero.ui.theme.StudyZeroTheme

@Composable
fun ShowOverlayPreview() {

    StudyZeroTheme(isOverlay = true) {
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
                    .padding(mediumPadding),
//                onClick = { },
                elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(id = R.dimen.elevation)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ShowEsmPreview()
                    SubmitButtonPreview()
                }
            }
        }
    }

}

@Composable
fun SubmitButtonPreview() {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(mediumPadding),
        enabled = true,
        onClick = {}) {
        Text(
            text = "Submit",
            style = MaterialTheme.typography.headlineSmall
        )
    }
}

@Composable
fun ShowEsmPreview(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val mediumPadding = dimensionResource(id = R.dimen.padding_medium)
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(color = MaterialTheme.colorScheme.surface)
            .padding(vertical = mediumPadding)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = modifier
                .padding(horizontal = mediumPadding)
        ) {
            ShowInstruction(modifier = Modifier.weight(1f))
            DismissButton(
                onDismiss = onClick,
                modifier = Modifier//.width(40.dp)
            )
        }
        //show question 1
        ShowQuestionPreview(
            questionString = R.string.question1,
        )
        ShowQuestionPreview(
            questionString = R.string.question2,
        )
    }
}

@Composable
fun ShowQuestionPreview(
    questionString: Int,
    modifier: Modifier = Modifier
) {
    val options: List<Int> = listOf(1, 2, 3, 4, 5)
    val labels: List<String> = listOf("Strongly disagree", "", "Neutral", "", "Strongly agree")
    val selectedValue = remember { mutableIntStateOf(0) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = mediumPadding, vertical = mediumPadding),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(id = R.dimen.elevation))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .background(color = MaterialTheme.colorScheme.surfaceVariant)
                .padding(
                    horizontal = mediumPadding,
                    vertical = mediumPadding / 4
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
            )
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = modifier
                    //.height(28.dp)
                    .fillMaxWidth()
                    .padding(top = dimensionResource(id = R.dimen.padding_medium))
            ) {
                options.forEach { item ->
                    ChoiceWithLabelPreview(
                        item = item,
                        label = labels[item - 1],
                        selectedValue = selectedValue
                    )
                }
            }
        }
    }
}

@Composable
fun ChoiceWithLabelPreview(
    item: Int,
    label: String,
    selectedValue: MutableState<Int>,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    )
    {
        RadioButton(
            selected = selectedValue.value == item,
            onClick = {},
            modifier = modifier
                .height(22.dp)
        )
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
                .width(55.dp)

        )
    }
}

@Preview
@Composable
fun PreviewEsm() {
    StudyZeroTheme {
        // A surface container using the 'background' color from the theme
        Surface {
            ShowOverlayPreview()
        }
    }
}