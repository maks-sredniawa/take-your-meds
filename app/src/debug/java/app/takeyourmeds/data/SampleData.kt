package app.takeyourmeds.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Rows to develop and screenshot against. **Deterministic on purpose**, creation times included: a
 * seeder that varies produces a different screenshot on every run, which makes an image diff useless.
 *
 * The Times are fixed and today's Metformin at 08:00 is Taken at 08:04, so a Today seeded in the
 * afternoon shows a Dose Taken, Due and still ahead; set the phone's clock, not this file, to move it.
 * The ids are fixed so a reseed answers the same Dose again rather than leaving a second one.
 */
object SampleData {
    private val medicines =
        listOf(
            MedicineEntity(id = "sample-metformin", name = "Metformin", amount = "1 tablet", note = "With breakfast") to
                listOf("08:00", "20:00"),
            MedicineEntity(id = "sample-vitamin-d", name = "Vitamin D", amount = "1 capsule") to listOf("08:00"),
            MedicineEntity(
                id = "sample-salbutamol",
                name = "Salbutamol inhaler",
                amount = "2 puffs",
            ) to listOf("13:00"),
            MedicineEntity(
                id = "sample-amoxicillin",
                name = "Amoxicillin oral suspension, the bottle from the weekend pharmacy",
                amount = "5 ml",
                note = "The bottle in the fridge",
            ) to emptyList(),
            MedicineEntity(
                id = "sample-magnesium",
                name = "Magnesium",
                amount = "2 tablets",
                note = "In the evening",
            ) to
                listOf("21:00"),
        )

    private val newest = Instant.parse("2026-11-01T09:00:00Z")

    suspend fun seed(
        repository: MedicineRepository,
        doses: DoseRepository,
    ) {
        wipe(repository)
        val today = LocalDate.now()
        medicines.forEachIndexed { index, (medicine, times) ->
            repository.save(medicine.copy(createdAt = newest.minusSeconds(index.toLong())))
            doses.saveTimes(medicine.id, times.map(LocalTime::parse), today.minusDays(1).atStartOfDay())
        }
        val (metformin, _) = medicines.first()
        val breakfast = today.atTime(8, 0)
        doses.markTaken(metformin, breakfast, breakfast.plusMinutes(4).atZone(ZoneId.systemDefault()).toInstant())
    }

    suspend fun wipe(repository: MedicineRepository) {
        for (medicine in repository.all()) repository.delete(medicine)
    }
}
