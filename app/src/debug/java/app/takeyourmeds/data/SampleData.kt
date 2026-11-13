package app.takeyourmeds.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Rows to develop and screenshot against. **Deterministic on purpose**, creation times included: a
 * seeder that varies produces a different screenshot on every run, which makes an image diff useless.
 */
object SampleData {
    private val medicines =
        listOf(
            MedicineEntity(name = "Metformin", amount = "1 tablet", note = "With breakfast"),
            MedicineEntity(name = "Vitamin D", amount = "1 capsule"),
            MedicineEntity(name = "Salbutamol inhaler", amount = "2 puffs"),
            MedicineEntity(
                name = "Amoxicillin oral suspension, the bottle from the weekend pharmacy",
                amount = "5 ml",
                note = "The bottle in the fridge",
            ),
            MedicineEntity(name = "Magnesium", amount = "2 tablets", note = "In the evening"),
        )

    private val newest = Instant.parse("2026-11-01T09:00:00Z")

    suspend fun seed(repository: MedicineRepository) {
        wipe(repository)
        medicines.forEachIndexed { index, medicine ->
            repository.save(medicine.copy(createdAt = newest.minusSeconds(index.toLong())))
        }
    }

    private val times =
        mapOf(
            "Metformin" to listOf("08:00", "20:00"),
            "Vitamin D" to listOf("08:00"),
            "Salbutamol inhaler" to listOf("13:00"),
            "Magnesium" to listOf("21:00"),
        )

    /**
     * On top of [seed]: Times for four Medicines, and today's Metformin at 08:00 Taken at 08:04, so a
     * Today seeded in the afternoon shows a Dose Taken, Due and still ahead. Set the phone's clock, not
     * this file, to move it.
     */
    suspend fun seedToday(
        repository: MedicineRepository,
        doses: DoseRepository,
    ) {
        val byName = repository.all().associateBy { it.name }
        val today = LocalDate.now()
        for ((name, hours) in times) {
            val medicine = byName.getValue(name)
            doses.saveTimes(medicine.id, hours.map(LocalTime::parse), today.minusDays(1).atStartOfDay())
        }
        val breakfast = today.atTime(8, 0)
        doses.markTaken(
            byName.getValue("Metformin"),
            breakfast,
            breakfast.plusMinutes(4).atZone(ZoneId.systemDefault()).toInstant(),
        )
    }

    suspend fun wipe(repository: MedicineRepository) {
        for (medicine in repository.all()) repository.delete(medicine)
    }
}
