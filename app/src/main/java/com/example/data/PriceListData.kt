package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val parentId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["folderId"])]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val name: String,
    val description: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val wholesalePrice: Double,
    val boughtFrom: String,
    val priceUnit: String = "piece",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bin_folders")
data class BinFolder(
    @PrimaryKey val id: Long,
    val name: String,
    val parentId: Long?,
    val deletedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bin_products")
data class BinProduct(
    @PrimaryKey val id: Long,
    val folderId: Long,
    val name: String,
    val description: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val wholesalePrice: Double,
    val boughtFrom: String,
    val priceUnit: String = "piece",
    val deletedAt: Long = System.currentTimeMillis()
)

@Dao
interface PriceListDao {
    // Folders
    @Query("SELECT * FROM folders ORDER BY id ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)

    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()

    @Query("SELECT * FROM folders")
    suspend fun getAllFoldersDirect(): List<FolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    // Products
    @Query("SELECT * FROM products WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getProductsForFolder(folderId: Long): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products")
    suspend fun getAllProductsDirect(): List<ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("DELETE FROM products")
    suspend fun deleteAllProducts()

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR boughtFrom LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    // Bin Folders
    @Query("SELECT * FROM bin_folders ORDER BY deletedAt DESC")
    fun getAllBinFolders(): Flow<List<BinFolder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBinFolder(folder: BinFolder)

    @Delete
    suspend fun deleteBinFolder(folder: BinFolder)

    @Query("SELECT * FROM bin_folders WHERE id = :id LIMIT 1")
    suspend fun getBinFolderById(id: Long): BinFolder?

    @Query("DELETE FROM bin_folders WHERE deletedAt < :cutoff")
    suspend fun deleteOldBinFolders(cutoff: Long)

    @Query("DELETE FROM bin_folders")
    suspend fun deleteAllBinFolders()

    // Bin Products
    @Query("SELECT * FROM bin_products ORDER BY deletedAt DESC")
    fun getAllBinProducts(): Flow<List<BinProduct>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBinProduct(product: BinProduct)

    @Delete
    suspend fun deleteBinProduct(product: BinProduct)

    @Query("DELETE FROM bin_products WHERE deletedAt < :cutoff")
    suspend fun deleteOldBinProducts(cutoff: Long)

    @Query("DELETE FROM bin_products")
    suspend fun deleteAllBinProducts()
}

@Database(entities = [FolderEntity::class, ProductEntity::class, BinFolder::class, BinProduct::class], version = 4, exportSchema = false)
abstract class PriceListDatabase : RoomDatabase() {
    abstract val priceListDao: PriceListDao
}

class PriceListRepository(private val dao: PriceListDao) {
    val allFolders: Flow<List<FolderEntity>> = dao.getAllFolders()
    val allProducts: Flow<List<ProductEntity>> = dao.getAllProducts()

    suspend fun getAllFoldersDirect(): List<FolderEntity> = dao.getAllFoldersDirect()
    suspend fun getAllProductsDirect(): List<ProductEntity> = dao.getAllProductsDirect()

    fun getProductsForFolder(folderId: Long): Flow<List<ProductEntity>> = dao.getProductsForFolder(folderId)

    suspend fun insertFolder(name: String, parentId: Long? = null): Long {
        return dao.insertFolder(FolderEntity(name = name, parentId = parentId))
    }

    suspend fun updateFolder(folder: FolderEntity) {
        dao.updateFolder(folder)
    }

    suspend fun deleteFolder(folder: FolderEntity) {
        dao.deleteFolder(folder)
    }

    suspend fun insertProduct(
        folderId: Long,
        name: String,
        description: String,
        costPrice: Double,
        sellingPrice: Double,
        wholesalePrice: Double,
        boughtFrom: String,
        priceUnit: String = "piece"
    ): Long {
        return dao.insertProduct(
            ProductEntity(
                folderId = folderId,
                name = name,
                description = description,
                costPrice = costPrice,
                sellingPrice = sellingPrice,
                wholesalePrice = wholesalePrice,
                boughtFrom = boughtFrom,
                priceUnit = priceUnit
            )
        )
    }

    suspend fun updateProduct(product: ProductEntity) {
        dao.updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) {
        dao.deleteProduct(product)
    }

    suspend fun mergeDataFromCloud(folders: List<FolderEntity>, products: List<ProductEntity>) {
        // Delete products first (FK safety), then folders, then insert folders, then insert products
        dao.deleteAllProducts()
        dao.deleteAllFolders()
        dao.insertFolders(folders)
        dao.insertProducts(products)
    }

    fun searchProducts(query: String): Flow<List<ProductEntity>> = dao.searchProducts(query)

    // Bin/Trash Operations
    val allBinFolders: Flow<List<BinFolder>> = dao.getAllBinFolders()
    val allBinProducts: Flow<List<BinProduct>> = dao.getAllBinProducts()

    suspend fun insertBinFolder(folder: BinFolder) {
        dao.insertBinFolder(folder)
    }

    suspend fun getBinFolderById(id: Long): BinFolder? {
        return dao.getBinFolderById(id)
    }

    suspend fun deleteBinFolder(folder: BinFolder) {
        dao.deleteBinFolder(folder)
    }

    suspend fun deleteOldBinFolders(cutoff: Long) {
        dao.deleteOldBinFolders(cutoff)
    }

    suspend fun deleteAllBinFolders() {
        dao.deleteAllBinFolders()
    }

    suspend fun insertBinProduct(product: BinProduct) {
        dao.insertBinProduct(product)
    }

    suspend fun deleteBinProduct(product: BinProduct) {
        dao.deleteBinProduct(product)
    }

    suspend fun deleteOldBinProducts(cutoff: Long) {
        dao.deleteOldBinProducts(cutoff)
    }

    suspend fun deleteAllBinProducts() {
        dao.deleteAllBinProducts()
    }

    // Direct folder/product insertions to restore with original ID preserved
    suspend fun restoreFolderRaw(folder: FolderEntity) {
        dao.insertFolder(folder)
    }

    suspend fun restoreProductRaw(product: ProductEntity) {
        dao.insertProduct(product)
    }
}
