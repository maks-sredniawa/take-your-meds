package app.takeyourmeds.data

import kotlinx.coroutines.flow.Flow

class MedicineRepository(
    private val dao: MedicineDao,
) {
    fun observeAll(): Flow<List<MedicineEntity>> = dao.observeAll()

    fun observeById(id: String): Flow<MedicineEntity?> = dao.observeById(id)

    suspend fun all(): List<MedicineEntity> = dao.all()

    suspend fun count(): Int = dao.count()

    suspend fun save(medicine: MedicineEntity) = dao.insert(medicine)

    suspend fun delete(medicine: MedicineEntity) = dao.delete(medicine)
}
