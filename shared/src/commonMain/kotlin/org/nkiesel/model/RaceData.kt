package org.nkiesel.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Represents time in hours, minutes, and seconds
 */
@Immutable
@Serializable
data class RaceTime(
    val hours: Int,
    val minutes: Int,
    val seconds: Int = 0
) {
    private fun Int.pad(): String = if (this < 10) "0$this" else toString()

    override fun toString(): String = "${hours.pad()}:${minutes.pad()}"

    /**
     * Returns a string representation including seconds
     */
    fun toStringWithSeconds(): String = "${hours.pad()}:${minutes.pad()}:${seconds.pad()}"

    /**
     * Converts time to seconds since midnight
     */
    fun toSeconds(): Int = hours * 3600 + minutes * 60 + seconds

    companion object {
        /**
         * Creates a RaceTime from seconds since midnight
         */
        fun fromSeconds(seconds: Int): RaceTime {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            return RaceTime(h, m, s)
        }
    }
}

/**
 * Represents a sailboat with its race data
 */
@Immutable
@Serializable
data class BoatData(
    val name: String = "",
    val startTime: RaceTime = RaceTime(10, 0),
    val finishTime: RaceTime = RaceTime(14, 0),
    val rating: Int = 0
) {
    /**
     * Calculates the elapsed time in seconds
     * Handles cases where finish time is earlier than start time (race spans midnight)
     */
    fun elapsedTimeSeconds(): Int {
        val elapsed = finishTime.toSeconds() - startTime.toSeconds()
        return if (elapsed < 0) {
            // If finish time is earlier than start time, assume race spans midnight
            elapsed + 86400
        } else {
            elapsed
        }
    }

    /**
     * Calculates the corrected time in seconds based on the rating
     * using the formula: 650 / (550 + rating) * elapsed time in seconds
     */
    fun correctedTimeSeconds(): Int {
        // Apply the specified rating correction formula
        return (650.0 / (550.0 + rating) * elapsedTimeSeconds()).roundToInt()
    }

    /**
     * Returns the elapsed time as a formatted string
     */
    fun elapsedTimeFormatted(): String {
        return timeFormatted(elapsedTimeSeconds())
    }

    /**
     * Returns the corrected time as a formatted string
     */
    fun correctedTimeFormatted(): String {
        return timeFormatted(correctedTimeSeconds())
    }
}

/**
 * Returns the corrected time as a formatted string
 */
private fun timeFormatted(seconds: Int): String {
    val hours = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) "${hours}h ${mins}m ${secs}s" else "${mins}m ${secs}s"
}

/**
 * Represents race data for two boats
 */
@Immutable
@Serializable
data class RaceComparisonData(
    val boat1: BoatData = BoatData(name = "My Boat", rating = -39),
    val boat2: BoatData = BoatData(name = "Competitor", rating = 39),
    val jibesetUrl: String = "",
    val myBoatNameProfile: String = ""
) {
    /**
     * Determines which boat won based on corrected time
     * Returns 1 if boat1 won, 2 if boat2 won, 0 if tie
     */
    fun determineWinner(): Int {
        val corrected1 = boat1.correctedTimeSeconds()
        val corrected2 = boat2.correctedTimeSeconds()

        return when {
            corrected1 < corrected2 -> 1
            corrected2 < corrected1 -> 2
            else -> 0
        }
    }

    /**
     * Calculates the time difference between the two boats in seconds
     */
    fun timeDifferenceSeconds(): Int {
        val corrected1 = boat1.correctedTimeSeconds()
        val corrected2 = boat2.correctedTimeSeconds()

        return abs(corrected1 - corrected2)
    }

    /**
     * Returns the time difference as a formatted string
     */
    fun timeDifferenceFormatted(): String {
        return timeFormatted(timeDifferenceSeconds())
    }

    /**
     * Calculates the finish time for boat 2 that would make both boats have the same corrected time
     */
    fun calculateEqualFinishTimeForBoat2(): RaceTime {
        val boat1ElapsedSeconds = boat1.elapsedTimeSeconds()
        val boat1Factor = (550 + boat1.rating).toDouble()
        val boat2Factor = (550 + boat2.rating).toDouble()

        val targetElapsedSeconds = (boat2Factor / boat1Factor * boat1ElapsedSeconds).roundToInt()

        // Calculate the new finish time based on the target elapsed time in seconds
        val newFinishSeconds = boat2.startTime.toSeconds() + targetElapsedSeconds

        // Handle case where the new finish time would be on the next day
        val adjustedFinishSeconds = if (newFinishSeconds >= 86400) newFinishSeconds - 86400 else newFinishSeconds

        return RaceTime.fromSeconds(adjustedFinishSeconds)
    }
}