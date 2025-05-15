package org.nkiesel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.nkiesel.components.RatingPicker
import org.nkiesel.components.TimePicker
import org.nkiesel.model.BoatData
import org.nkiesel.model.RaceComparisonData
import org.nkiesel.model.RaceTime
import org.nkiesel.test.testElapsedTimeCalculation

@Composable
@Preview
fun App() {
    // Run test to verify elapsed time calculation
    testElapsedTimeCalculation()

    MaterialTheme {
        var raceData by remember { mutableStateOf(RaceComparisonData()) }
        var isFrozen by remember { mutableStateOf(false) }
        var showUnfreezeDialog by remember { mutableStateOf(false) }

        if (showUnfreezeDialog) {
            AlertDialog(
                onDismissRequest = { showUnfreezeDialog = false },
                title = { Text("Confirm Unfreeze") },
                text = { Text("Are you sure you want to unfreeze the application? This will allow changes to boat ratings and times.") },
                confirmButton = {
                    Button(
                        onClick = {
                            isFrozen = false
                            showUnfreezeDialog = false
                        }
                    ) {
                        Text("Unfreeze")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showUnfreezeDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            Text(
                text = "Time-on-Time Calculator",
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(36.dp))
            // Boats side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Boat 1
                BoatCard(
                    title = "My Boat",
                    boatData = raceData.boat1,
                    onBoatDataChanged = { newBoatData ->
                        raceData = raceData.copy(boat1 = newBoatData)
                    },
                    modifier = Modifier.weight(1f),
                    isFrozen = isFrozen
                )

                // Boat 2
                BoatCard(
                    title = "Competitor",
                    boatData = raceData.boat2,
                    onBoatDataChanged = { newBoatData ->
                        raceData = raceData.copy(boat2 = newBoatData)
                    },
                    modifier = Modifier.weight(1f),
                    isFrozen = isFrozen
                )
            }

            // Finished Now button
            Button(
                onClick = {
                    if (!isFrozen) {
                        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        val currentTime = RaceTime(now.hour, now.minute, now.second)

                        // Update boat 1's finish time
                        val newBoat1 = raceData.boat1.copy(finishTime = currentTime)
                        raceData = raceData.copy(boat1 = newBoat1)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF4CAF50)), // Green
                enabled = !isFrozen
            ) {
                Text(
                    text = "My Boat Finished Now",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Results
            ResultsCard(
                raceData = raceData,
                modifier = Modifier.fillMaxWidth(),
                onRaceDataChanged = { newRaceData ->
                    raceData = newRaceData
                },
                isFrozen = isFrozen
            )

            // Freeze button
            Button(
                onClick = {
                    if (isFrozen) {
                        showUnfreezeDialog = true
                    } else {
                        isFrozen = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (isFrozen) Color(0xFFFF5722) else Color(0xFF2196F3)
                )
            ) {
                Text(
                    text = if (isFrozen) "Unfreeze Application" else "Freeze Application",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun BoatCard(
    title: String,
    boatData: BoatData,
    onBoatDataChanged: (BoatData) -> Unit,
    modifier: Modifier = Modifier,
    isFrozen: Boolean = false
) {
    Card(
        modifier = modifier,
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold
            )

            // Rating
            RatingPicker(
                rating = boatData.rating,
                onRatingChanged = { newRating ->
                    if (!isFrozen) {
                        onBoatDataChanged(boatData.copy(rating = newRating))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .alpha(if (isFrozen) 0.6f else 1f)
            )

            // Start Time - never show seconds
            TimePickerSection(
                title = "Start Time",
                time = boatData.startTime,
                onTimeChanged = { newTime ->
                    if (!isFrozen) {
                        onBoatDataChanged(boatData.copy(startTime = newTime))
                    }
                },
                showSeconds = false,
                modifier = Modifier.fillMaxWidth(),
                isFrozen = isFrozen
            )

            // Finish Time - show seconds
            TimePickerSection(
                title = "Finish Time",
                time = boatData.finishTime,
                onTimeChanged = { newTime ->
                    if (!isFrozen) {
                        onBoatDataChanged(boatData.copy(finishTime = newTime))
                    }
                },
                showSeconds = true,
                modifier = Modifier.fillMaxWidth(),
                isFrozen = isFrozen
            )

            // Times
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Elapsed: ${boatData.elapsedTimeFormatted()}",
                        style = MaterialTheme.typography.body2
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Corrected: ${boatData.correctedTimeFormatted()}",
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
    }
}

@Composable
fun TimePickerSection(
    title: String,
    time: RaceTime,
    onTimeChanged: (RaceTime) -> Unit,
    showSeconds: Boolean = false,
    modifier: Modifier = Modifier,
    isFrozen: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        TimePicker(
            hour = time.hour,
            minute = time.minute,
            second = time.second,
            onTimeChanged = { hour, minute, second ->
                // For start time, always use 0 seconds regardless of what was selected
                val actualSecond = if (title.contains("Start", ignoreCase = true)) 0 else second
                onTimeChanged(RaceTime(hour, minute, actualSecond))
            },
            showSeconds = showSeconds,
            modifier = Modifier.fillMaxWidth(),
            isFrozen = isFrozen
        )
    }
}

@Composable
fun ResultsCard(
    raceData: RaceComparisonData,
    modifier: Modifier = Modifier,
    onRaceDataChanged: (RaceComparisonData) -> Unit = {},
    isFrozen: Boolean = false
) {
    var currentRaceData by remember { mutableStateOf(raceData) }

    // Update currentRaceData when raceData changes
    LaunchedEffect(raceData) {
        currentRaceData = raceData
    }

    val winner = currentRaceData.determineWinner()
    val winnerColor = when (winner) {
        1 -> Color(0xFF4CAF50) // Green
        2 -> Color(0xFF2196F3) // Blue
        else -> Color.Gray
    }

    Card(
        modifier = modifier,
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp),
        backgroundColor = Color(0xFFFFF9C4) // Light yellow
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Winner:",
                    style = MaterialTheme.typography.subtitle1
                )

                Text(
                    text = when (winner) {
                        1 -> "My Boat"
                        2 -> "Competitor"
                        else -> "Tie"
                    },
                    style = MaterialTheme.typography.h6,
                    color = winnerColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Time Difference:",
                    style = MaterialTheme.typography.body2
                )

                Text(
                    text = currentRaceData.timeDifferenceFormatted(),
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Bold
                )
            }

            // Button to equalize corrected times
            Button(
                onClick = {
                    if (!isFrozen) {
                        val equalFinishTime = currentRaceData.calculateEqualFinishTimeForBoat2()
                        val newBoat2 = currentRaceData.boat2.copy(finishTime = equalFinishTime)
                        val newRaceData = currentRaceData.copy(boat2 = newBoat2)
                        currentRaceData = newRaceData
                        onRaceDataChanged(newRaceData)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF9C27B0)), // Purple
                enabled = !isFrozen
            ) {
                Text(
                    text = "Equalize Corrected Times",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (winner != 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = winnerColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val indicator = if (winner == 1) "▲" else "▼"
                    val text = if (winner == 1) "My Boat wins by" else "Competitor wins by"

                    Text(
                        text = indicator,
                        style = MaterialTheme.typography.h6,
                        color = winnerColor,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "$text ${raceData.timeDifferenceFormatted()}",
                        style = MaterialTheme.typography.body1,
                        color = winnerColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
