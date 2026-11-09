package com.example.data.local

import androidx.room.*
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object (DAO) for Transactions table.
 * Provides reactive Flow observers and suspending CRUD operations
 * to persist and query records from the Add Transaction form.
 */
@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY serialNumber DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY serialNumber ASC, id ASC")
    suspend fun getAllTransactionsAsc(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC, time DESC")
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC, time DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date LIKE :yearMonth || '%' ORDER BY serialNumber ASC, id ASC")
    fun getTransactionsByMonth(yearMonth: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date LIKE :yearMonth || '%' ORDER BY serialNumber ASC, id ASC")
    suspend fun getTransactionsByMonthDirect(yearMonth: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, time DESC")
    fun getTransactionsByDateRange(startDate: String, endDate: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE fromAccount = :accountName OR toAccount = :accountName ORDER BY date DESC, time DESC")
    fun getTransactionsByAccount(accountName: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT MAX(serialNumber) FROM transactions")
    suspend fun getMaxSerialNumber(): Int?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
}
