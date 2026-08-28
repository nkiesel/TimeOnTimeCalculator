package org.nkiesel.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.nkiesel.model.BoatData
import org.nkiesel.model.RaceComparisonData
import org.nkiesel.model.RaceTime
import org.nkiesel.service.JibesetBoat
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class StorageTest {

    private val json = RaceDataRepository.defaultJson

    @Test
    fun testJibesetBoatsSerializationRoundtrip() {
        val boats = listOf(
            JibesetBoat(
                name = "Wind Dancer",
                sailNumber = "USA 1234",
                rating = -42,
                fleet = "PHRF A",
                flag = "Flag 1",
                skipper = "Captain Jack",
                make = "J/105"
            ),
            JibesetBoat(
                name = "Sea \"Breeze\"",
                sailNumber = "5678",
                rating = 105,
                fleet = "PHRF B",
                flag = "Flag 2",
                skipper = "Jane Doe",
                make = "Catalina 30"
            )
        )

        val jsonString = json.encodeToString(boats)
        val parsed: List<JibesetBoat> = json.decodeFromString(jsonString)

        assertEquals(2, parsed.size)
        assertEquals("Wind Dancer", parsed[0].name)
        assertEquals("USA 1234", parsed[0].sailNumber)
        assertEquals(-42, parsed[0].rating)
        assertEquals("PHRF A", parsed[0].fleet)
        assertEquals("Flag 1", parsed[0].flag)
        assertEquals("Captain Jack", parsed[0].skipper)
        assertEquals("J/105", parsed[0].make)

        assertEquals("Sea \"Breeze\"", parsed[1].name)
        assertEquals("5678", parsed[1].sailNumber)
        assertEquals(105, parsed[1].rating)
        assertEquals("PHRF B", parsed[1].fleet)
        assertEquals("Flag 2", parsed[1].flag)
        assertEquals("Jane Doe", parsed[1].skipper)
        assertEquals("Catalina 30", parsed[1].make)
    }

    @Test
    fun testRaceComparisonDataSerializationRoundtrip() {
        val raceData = RaceComparisonData(
            boat1 = BoatData(
                name = "My Racer",
                startTime = RaceTime(11, 30, 15),
                finishTime = RaceTime(15, 45, 50),
                rating = -30
            ),
            boat2 = BoatData(
                name = "Rival Boat",
                startTime = RaceTime(11, 35, 0),
                finishTime = RaceTime(15, 50, 20),
                rating = 45
            ),
            jibesetUrl = "https://www.jibeset.net/example_race.html",
            myBoatNameProfile = "My Racer"
        )

        val jsonString = json.encodeToString(raceData)
        val parsed: RaceComparisonData = json.decodeFromString(jsonString)

        assertNotNull(parsed)
        assertEquals("My Racer", parsed.boat1.name)
        assertEquals(11, parsed.boat1.startTime.hours)
        assertEquals(30, parsed.boat1.startTime.minutes)
        assertEquals(15, parsed.boat1.startTime.seconds)
        assertEquals(15, parsed.boat1.finishTime.hours)
        assertEquals(45, parsed.boat1.finishTime.minutes)
        assertEquals(50, parsed.boat1.finishTime.seconds)
        assertEquals(-30, parsed.boat1.rating)

        assertEquals("Rival Boat", parsed.boat2.name)
        assertEquals(11, parsed.boat2.startTime.hours)
        assertEquals(35, parsed.boat2.startTime.minutes)
        assertEquals(0, parsed.boat2.startTime.seconds)
        assertEquals(15, parsed.boat2.finishTime.hours)
        assertEquals(50, parsed.boat2.finishTime.minutes)
        assertEquals(20, parsed.boat2.finishTime.seconds)
        assertEquals(45, parsed.boat2.rating)

        assertEquals("https://www.jibeset.net/example_race.html", parsed.jibesetUrl)
        assertEquals("My Racer", parsed.myBoatNameProfile)
    }

    @Test
    fun testUnknownKeysIgnored() {
        val jsonWithExtra = """
            {
                "boat1": {
                    "name": "Boat A",
                    "startTime": {"hours": 10, "minutes": 0, "seconds": 0, "extra": "val"},
                    "finishTime": {"hours": 14, "minutes": 0, "seconds": 0},
                    "rating": 50,
                    "color": "blue"
                },
                "boat2": {
                    "name": "Boat B",
                    "startTime": {"hours": 10, "minutes": 0, "seconds": 0},
                    "finishTime": {"hours": 14, "minutes": 0, "seconds": 0},
                    "rating": 60
                },
                "jibesetUrl": "https://example.com",
                "myBoatNameProfile": "Boat A",
                "unexpectedField": 123
            }
        """.trimIndent()

        val parsed = json.decodeFromString<RaceComparisonData>(jsonWithExtra)
        assertEquals("Boat A", parsed.boat1.name)
        assertEquals("Boat B", parsed.boat2.name)
    }

    @Test
    fun testRaceDataRepository() {
        val storage = InMemoryKeyValueStorage()
        val repository = RaceDataRepository(storage)

        // Initially null / empty
        assertEquals(null, repository.loadRaceData())
        assertEquals(emptyList(), repository.loadBoats())

        val raceData = RaceComparisonData(
            boat1 = BoatData(name = "Boat A", rating = 50),
            boat2 = BoatData(name = "Boat B", rating = 60),
            jibesetUrl = "https://jibeset.net/test.html",
            myBoatNameProfile = "Boat A"
        )
        val boats = listOf(
            JibesetBoat("Boat A", "1", 50, "Fleet 1", "Flag 1", "Skip A", "Make A"),
            JibesetBoat("Boat B", "2", 60, "Fleet 1", "Flag 1", "Skip B", "Make B")
        )

        repository.saveRaceData(raceData)
        repository.saveBoats(boats)

        val loadedRaceData = repository.loadRaceData()
        val loadedBoats = repository.loadBoats()

        assertNotNull(loadedRaceData)
        assertEquals("Boat A", loadedRaceData.boat1.name)
        assertEquals("https://jibeset.net/test.html", loadedRaceData.jibesetUrl)
        assertEquals(2, loadedBoats.size)
        assertEquals("Boat B", loadedBoats[1].name)
    }
}
