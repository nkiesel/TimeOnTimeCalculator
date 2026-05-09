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
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.nkiesel.components.RatingPicker
import org.nkiesel.components.TimePicker
import org.nkiesel.model.BoatData
import org.nkiesel.model.RaceComparisonData
import org.nkiesel.model.RaceTime
import org.nkiesel.service.JibesetService
import org.nkiesel.service.JibesetBoat
import org.nkiesel.test.testElapsedTimeCalculation
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    // Run test to verify elapsed time calculation
    testElapsedTimeCalculation()

    MaterialTheme {
        var raceData by remember { mutableStateOf(RaceComparisonData()) }
        var isFrozen by remember { mutableStateOf(false) }
        var showUnfreezeDialog by remember { mutableStateOf(false) }
        
        val jibesetService = remember { JibesetService() }
        var allBoats by remember { mutableStateOf(emptyList<JibesetBoat>()) }
        val scope = rememberCoroutineScope()

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

            // Jibeset Integration Section
            JibesetSection(
                raceData = raceData,
                onRaceDataChanged = { raceData = it },
                allBoats = allBoats,
                onLoadBoats = { url ->
                    scope.launch {
                        allBoats = jibesetService.fetchBoats(url)
                        // Auto-detect my boat if possible
                        val myBoat = allBoats.find { it.name.equals(raceData.myBoatNameProfile, ignoreCase = true) }
                        if (myBoat != null) {
                            raceData = raceData.copy(
                                boat1 = raceData.boat1.copy(name = myBoat.name, rating = myBoat.rating)
                            )
                        }
                    }
                },
                isFrozen = isFrozen
            )

            Spacer(modifier = Modifier.height(16.dp))
            // Boats side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Boat 1
                BoatCard(
                    title = raceData.boat1.name,
                    boatData = raceData.boat1,
                    onBoatDataChanged = { newBoatData ->
                        raceData = raceData.copy(boat1 = newBoatData)
                    },
                    modifier = Modifier.weight(1f),
                    isFrozen = isFrozen
                )

                // Boat 2
                BoatCard(
                    title = raceData.boat2.name,
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

                        // Update boat 1s finish time
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
fun JibesetSection(
    raceData: RaceComparisonData,
    onRaceDataChanged: (RaceComparisonData) -> Unit,
    allBoats: List<JibesetBoat>,
    onLoadBoats: (String) -> Unit,
    isFrozen: Boolean
) {
    var url by remember { mutableStateOf(raceData.jibesetUrl) }
    var myBoatName by remember { mutableStateOf(raceData.myBoatNameProfile) }
    var expandedMyBoat by remember { mutableStateOf(false) }
    var expandedCompetitor by remember { mutableStateOf(false) }

    val myJibesetBoat = allBoats.find { it.name.equals(myBoatName, ignoreCase = true) }
    val myFleet = myJibesetBoat?.fleet
    val fleetBoats = if (myFleet != null) allBoats.filter { it.fleet == myFleet && !it.name.equals(myBoatName, ignoreCase = true) } else emptyList()

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Jibeset Integration", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            
            TextField(
                value = myBoatName,
                onValueChange = { 
                    myBoatName = it
                    onRaceDataChanged(raceData.copy(myBoatNameProfile = it))
                },
                label = { Text("My Boat Name (Profile)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isFrozen
            )

            TextField(
                value = url,
                onValueChange = { 
                    url = it
                    onRaceDataChanged(raceData.copy(jibesetUrl = it))
                },
                label = { Text("Jibeset Fleets/Flags URL") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isFrozen
            )
            
            Button(
                onClick = { onLoadBoats(url) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isFrozen && url.isNotEmpty()
            ) {
                Text("Load Race Info")
            }

            if (allBoats.isNotEmpty()) {
                Divider()
                
                Text("Select Boats from Jibeset:", style = MaterialTheme.typography.body2, fontWeight = FontWeight.Bold)
                
                Box {
                    OutlinedButton(
                        onClick = { expandedMyBoat = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isFrozen
                    ) {
                        Text(if (myJibesetBoat != null) "My Boat: ${myJibesetBoat.name} (${myJibesetBoat.rating})" else "Select My Boat")
                    }
                    DropdownMenu(expanded = expandedMyBoat, onDismissRequest = { expandedMyBoat = false }) {
                        allBoats.forEach { boat ->
                            DropdownMenuItem(onClick = {
                                expandedMyBoat = false
                                myBoatName = boat.name
                                val newBoat1 = raceData.boat1.copy(name = boat.name, rating = boat.rating)
                                onRaceDataChanged(raceData.copy(boat1 = newBoat1, myBoatNameProfile = boat.name))
                            }) {
                                Text("${boat.name} (${boat.rating}) - ${boat.fleet}")
                            }
                        }
                    }
                }

                if (myFleet != null) {
                    Text("My Fleet: $myFleet", style = MaterialTheme.typography.body2, color = Color.Gray)
                    
                    Box {
                        OutlinedButton(
                            onClick = { expandedCompetitor = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isFrozen && fleetBoats.isNotEmpty()
                        ) {
                            val competitor = raceData.boat2
                            Text(if (competitor.name != "Competitor") "Competitor: ${competitor.name} (${competitor.rating})" else "Select Competitor from Fleet")
                        }
                        DropdownMenu(expanded = expandedCompetitor, onDismissRequest = { expandedCompetitor = false }) {
                            fleetBoats.forEach { boat ->
                                DropdownMenuItem(onClick = {
                                    expandedCompetitor = false
                                    val newBoat2 = raceData.boat2.copy(name = boat.name, rating = boat.rating)
                                    onRaceDataChanged(raceData.copy(boat2 = newBoat2))
                                }) {
                                    Text("${boat.name} (${boat.rating})")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoatCard(
    title: String,
    boatData: BoatData,
    onBoatDataChanged: (BoatData) -> Unit,
    modifier: Modifier,
    isFrozen: Boolean
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
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
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
    showSeconds: Boolean,
    modifier: Modifier,
    isFrozen: Boolean,
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
            hour = time.hours,
            minute = time.minutes,
            second = time.seconds,
            onTimeChanged = { hour, minute, second ->
                onTimeChanged(RaceTime(hour, minute, if (showSeconds) second else 0))
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
    onRaceDataChanged: (RaceComparisonData) -> Unit,
    isFrozen: Boolean,
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
                        1 -> currentRaceData.boat1.name
                        2 -> currentRaceData.boat2.name
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
                    Text(
                        text = if (winner == 1) "▲" else "▼",
                        style = MaterialTheme.typography.h6,
                        color = winnerColor,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    val text = if (winner == 1) currentRaceData.boat1.name else currentRaceData.boat2.name

                    Text(
                        text = "$text wins by ${raceData.timeDifferenceFormatted()}",
                        style = MaterialTheme.typography.body1,
                        color = winnerColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}