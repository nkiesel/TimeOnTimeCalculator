package org.nkiesel.service

import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

data class JibesetBoat(
    val name: String,
    val sailNumber: String,
    val rating: Int,
    val fleet: String,
    val flag: String,
    val skipper: String,
    val make: String
)

class JibesetService {
    private val client = HttpClient()

    suspend fun fetchBoats(url: String): List<JibesetBoat> {
        return try {
            val response = client.get(url)
            val html = response.bodyAsText()
            val doc = Ksoup.parse(html)
            val boats = mutableListOf<JibesetBoat>()
            
            var currentFlag = ""
            var currentFleet = ""
            
            val rows = doc.select("tr")
            for (row in rows) {
                val fbwCells = row.select("td.fbw")
                if (fbwCells.isNotEmpty()) {
                    val text = fbwCells.text()
                    if (text.startsWith("Flag:")) {
                        currentFlag = text.substringAfter("Flag:").substringBefore("Boats").trim()
                    } else if (text.contains(" - ")) {
                        currentFleet = text.substringBefore("Boats").trim()
                    }
                    continue
                }
                
                if (row.hasClass("d0") || row.hasClass("d1")) {
                    val cells = row.select("td.tdataro")
                    if (cells.size >= 5) {
                        val sail = cells[1].text().trim()
                        val name = cells[2].text().trim()
                        val skipper = cells[3].text().trim()
                        val ratingStr = cells[4].text().trim()
                        val make = if (cells.size > 5) cells[5].text().trim() else ""
                        
                        val rating = ratingStr.toIntOrNull() ?: 0
                        
                        boats.add(JibesetBoat(
                            name = name,
                            sailNumber = sail,
                            rating = rating,
                            fleet = currentFleet,
                            flag = currentFlag,
                            skipper = skipper,
                            make = make
                        ))
                    }
                }
            }
            boats
        } catch (e: Exception) {
            emptyList()
        }
    }
}