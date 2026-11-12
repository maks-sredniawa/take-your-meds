package app.takeyourmeds.data

import kotlinx.coroutines.flow.Flow

/** The one place screens talk to for items. */
class ItemRepository(
    private val dao: ItemDao,
) {
    fun observeAll(): Flow<List<ItemEntity>> = dao.observeAll()

    fun observeById(id: String): Flow<ItemEntity?> = dao.observeById(id)

    suspend fun all(): List<ItemEntity> = dao.all()

    suspend fun count(): Int = dao.count()

    suspend fun save(item: ItemEntity) = dao.insert(item)

    suspend fun delete(item: ItemEntity) = dao.delete(item)
}
