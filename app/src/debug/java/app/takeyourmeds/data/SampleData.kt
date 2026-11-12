package app.takeyourmeds.data

import java.time.Instant

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

    suspend fun wipe(repository: MedicineRepository) {
        for (medicine in repository.all()) repository.delete(medicine)
    }
}
