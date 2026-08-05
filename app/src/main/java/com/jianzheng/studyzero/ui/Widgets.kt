package com.jianzheng.studyzero.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jianzheng.studyzero.ui.theme.StudyZeroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext


@Composable
fun ButtonCard(
    title: String,
    desc: String,
    onAuthClick: () -> Unit,
    buttonText: String = "Allow",
    buttonEnabled: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (title != "") {
                Text(
                    text = title, fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = desc, fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Button(
            onClick = onAuthClick,
            enabled = buttonEnabled,
            modifier = Modifier
                .wrapContentHeight()
                .width(108.dp)
        ) {
            Text(text = buttonText)
        }
    }
}

@Composable
fun <T> usePollState(
    context: CoroutineContext = Dispatchers.Default,
    interval: Long = 1000L,
    getter: () -> T,
): MutableState<T> {
    val mutableState = remember { mutableStateOf(getter()) }
    LaunchedEffect(Unit) {
        withContext(context) {
            while (isActive) {
                delay(interval)
                mutableState.value = getter()
            }
        }
    }
    return mutableState
}


@Composable
fun SwitchCard(
    title: String,
    desc: String,
    onCheckedChange: (Boolean) -> Unit,
    isEnabled: Boolean = true,
    isChecked: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (title != "") {
                Text(
                    text = title, fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
            }
            Text(
                text = desc, fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        SwitchMinimalExample(onCheckedChange, isEnabled, isChecked)
    }
}

@Composable
fun SwitchMinimalExample(
    onCheckedChange: (Boolean) -> Unit,
    isEnabled: Boolean,
    isChecked: Boolean = false
) {
    var checked by remember { mutableStateOf(isChecked) }

    Switch(
        checked = checked,
        onCheckedChange = {
            checked = it
            onCheckedChange(it)
        },
        enabled = isEnabled,
        modifier = Modifier.wrapContentHeight()
    )
}

@Composable
fun ArrowCard(
    title: String = "Title",
    desc: String = "Desc",
    onAuthClick: () -> Unit = {},
    buttonEnabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { onAuthClick() }
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = mediumPadding, vertical = mediumPadding / 3)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onAuthClick,
                    enabled = buttonEnabled,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "open"
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc, fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun EnhancedArrowCard(
    title: String = "Title",
    desc: String = "Desc",
    onAuthClick: () -> Unit = {},
    buttonEnabled: Boolean = true,
    status: CardStatus = CardStatus.PENDING
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { onAuthClick() }
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = mediumPadding, vertical = mediumPadding / 3)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onAuthClick,
                    enabled = buttonEnabled,
                ) {
                    when (status) {
                        CardStatus.PENDING -> {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "pending"
                            )
                        }

                        CardStatus.WAITING -> {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "waiting",
                                modifier = Modifier.rotate(90f)
                            )
                        }

                        CardStatus.COMPLETED -> {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "completed",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc, fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

enum class CardStatus {
    PENDING,    // Arrow (default state)
    WAITING,    // Three dots
    COMPLETED   // Check mark
}

@Preview
@Composable
fun WidgetPreview() {
    StudyZeroTheme(darkTheme = false) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            ArrowCard()
        }
    }
}