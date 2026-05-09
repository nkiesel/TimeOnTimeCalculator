package org.nkiesel.test

import org.nkiesel.model.BoatData
import org.nkiesel.model.RaceTime

/**
 * Test function to verify elapsed time calculation
 */
fun testElapsedTimeCalculation() {
    // Test case 1: Both boats have identical elapsed times and ratings
    val boat1 = BoatData(
        startTime = RaceTime(12, 0, 0),
        finishTime = RaceTime(14, 0, 0),
        rating = 100
    )

    val boat2 = BoatData(
        startTime = RaceTime(12, 0, 0),
        finishTime = RaceTime(14, 0, 0),
        rating = 100
    )

    println("Boat 1 elapsed time: ${boat1.elapsedTimeSeconds()} seconds")
    println("Boat 2 elapsed time: ${boat2.elapsedTimeSeconds()} seconds")
    println("Boat 1 corrected time: ${boat1.correctedTimeSeconds()} seconds")
    println("Boat 2 corrected time: ${boat2.correctedTimeSeconds()} seconds")

    // Test case 2: Finish time earlier than start time (spans midnight)
    val boat3 = BoatData(
        startTime = RaceTime(22, 0, 0),
        finishTime = RaceTime(2, 0, 0),
        rating = 100
    )

    println("Boat 3 elapsed time: ${boat3.elapsedTimeSeconds()} seconds")
    println("Boat 3 corrected time: ${boat3.correctedTimeSeconds()} seconds")
}
