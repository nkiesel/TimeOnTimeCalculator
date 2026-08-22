package org.nkiesel.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A wheel picker component that allows selecting a value from a list of options.
 * 
 * @param items List of items to display in the wheel
 * @param selectedIndex Currently selected index
 * @param onSelectedIndexChange Callback when selected index changes
 * @param modifier Modifier for the wheel picker
 * @param visibleItemsCount Number of visible items in the wheel (must be odd)
 */
@Composable
fun <T> WheelPicker(
    items: List<T>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItemsCount: Int = 1,
    isFrozen: Boolean = false,
    itemContent: @Composable (item: T, isSelected: Boolean) -> Unit = { item, isSelected ->
        DefaultWheelPickerItem(item = item, isSelected = isSelected)
    }
) {
    require(visibleItemsCount % 2 == 1) { "visibleItemsCount must be an odd number" }

    val halfVisibleItemsCount = visibleItemsCount / 2

    var scrollState by remember { mutableStateOf(selectedIndex.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(selectedIndex) {
        if (!isDragging) {
            scrollState = selectedIndex.toFloat()
        }
    }

    LaunchedEffect(scrollState) {
        if (isDragging) {
            val roundedValue = scrollState.roundToInt()
            val wrappedValue = (roundedValue % items.size + items.size) % items.size
            if (wrappedValue != selectedIndex) {
                onSelectedIndexChange(wrappedValue)
            }
        }
    }

    Box(
        modifier = modifier
            .height(48.dp * visibleItemsCount)
            .pointerInput(isFrozen) {
                if (!isFrozen) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            val roundedValue = scrollState.roundToInt()
                            val wrappedValue = (roundedValue % items.size + items.size) % items.size
                            scrollState = roundedValue.toFloat()
                            onSelectedIndexChange(wrappedValue)
                        },
                        onDragCancel = {
                            isDragging = false
                            scrollState = selectedIndex.toFloat()
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        // Convert vertical drag to scroll change (negative because dragging down should increase the value)
                        val delta = -dragAmount.y / 48f
                        scrollState = (scrollState + delta)
                    }
                }
            }
            .alpha(if (isFrozen) 0.6f else 1f),
        contentAlignment = Alignment.Center
    ) {
        // Display visible items
        for (i in -halfVisibleItemsCount..halfVisibleItemsCount) {
            val index = (selectedIndex + i % items.size + items.size) % items.size
            
            val distanceFromCenter = abs(i.toFloat())
            val scale = 1f - (distanceFromCenter * 0.15f)
            val alpha = 1f - (distanceFromCenter * 0.3f)

            Box(
                modifier = Modifier
                    .height(48.dp)
                    .fillMaxWidth()
                    .offset(y = (i * 48).dp)
                    .alpha(alpha)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                contentAlignment = Alignment.Center
            ) {
                itemContent(items[index], i == 0)
            }
        }
    }
}

@Composable
private fun <T> DefaultWheelPickerItem(item: T, isSelected: Boolean) {
    Text(
        text = item.toString(),
        style = if (isSelected) MaterialTheme.typography.h6 else MaterialTheme.typography.body1,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * A time picker component that allows selecting hours, minutes, and optionally seconds.
 * 
 * @param hour Currently selected hour (0-23)
 * @param minute Currently selected minute (0-59)
 * @param second Currently selected second (0-59)
 * @param onTimeChanged Callback when time changes
 * @param showSeconds Whether to show seconds picker
 * @param modifier Modifier for the time picker
 */
@Composable
fun TimePicker(
    hour: Int,
    minute: Int,
    second: Int = 0,
    onTimeChanged: (hour: Int, minute: Int, second: Int) -> Unit,
    showSeconds: Boolean = false,
    modifier: Modifier = Modifier,
    isFrozen: Boolean = false
) {
    // Track the current values internally to ensure only changed values are updated
    var currentHour by remember { mutableStateOf(hour) }
    var currentMinute by remember { mutableStateOf(minute) }
    var currentSecond by remember { mutableStateOf(second) }

    // Update internal state when props change
    LaunchedEffect(hour, minute, second) {
        currentHour = hour
        currentMinute = minute
        currentSecond = second
    }

    @Composable
    fun colon() = Text(
        text = ":",
        style = MaterialTheme.typography.h6,
        modifier = Modifier.padding(horizontal = 4.dp)
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Hour picker
        WheelPicker(
            items = (0..23).toList(),
            selectedIndex = currentHour,
            onSelectedIndexChange = { newHour ->
                currentHour = newHour
                onTimeChanged(newHour, currentMinute, currentSecond)
            },
            modifier = Modifier.weight(1f),
            isFrozen = isFrozen,
            itemContent = { item, isSelected ->
                Text(
                    text = item.toString().padStart(2, '0'),
                    style = if (isSelected) MaterialTheme.typography.h6 else MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )

        colon()

        // Minute picker
        WheelPicker(
            items = (0..59).toList(),
            selectedIndex = currentMinute,
            onSelectedIndexChange = { newMinute ->
                currentMinute = newMinute
                onTimeChanged(currentHour, newMinute, currentSecond)
            },
            modifier = Modifier.weight(1f),
            isFrozen = isFrozen,
            itemContent = { item, isSelected ->
                Text(
                    text = item.toString().padStart(2, '0'),
                    style = if (isSelected) MaterialTheme.typography.h6 else MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )

        if (showSeconds) {
            colon()

            // Seconds picker
            WheelPicker(
                items = (0..59).toList(),
                selectedIndex = currentSecond,
                onSelectedIndexChange = { newSecond ->
                    currentSecond = newSecond
                    onTimeChanged(currentHour, currentMinute, newSecond)
                },
                modifier = Modifier.weight(1f),
                isFrozen = isFrozen,
                itemContent = { item, isSelected ->
                    Text(
                        text = item.toString().padStart(2, '0'),
                        style = if (isSelected) MaterialTheme.typography.h6 else MaterialTheme.typography.body1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    }
}

/**
 * A rating picker component that allows selecting a sailboat rating.
 * Ratings are integers that increase/decrease by 3, with 100 as base rating.
 * 
 * @param rating Currently selected rating
 * @param onRatingChanged Callback when rating changes
 * @param modifier Modifier for the rating picker
 */
@Composable
fun RatingPicker(
    rating: Int,
    onRatingChanged: (Int) -> Unit,
    modifier: Modifier
) {
    // Generate ratings from -200 to 400 with step 3
    val ratings = (-67..133).map { it * 3 }
    val selectedIndex = ratings.indexOf(rating).coerceAtLeast(0)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Rating",
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        WheelPicker(
            items = ratings,
            selectedIndex = selectedIndex,
            onSelectedIndexChange = { index ->
                onRatingChanged(ratings[index])
            },
            modifier = Modifier.fillMaxWidth(),
            itemContent = { item, isSelected ->
                Text(
                    text = item.toString(),
                    style = if (isSelected) MaterialTheme.typography.h6 else MaterialTheme.typography.body1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}
