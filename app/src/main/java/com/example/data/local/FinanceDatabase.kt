package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        PetrolExpenseEntity::class,
        ExtraExpenseEntity::class,
        SalaryRecordEntity::class,
        LoanRecordEntity::class,
        MonthlyStatementEntity::class,
        UserProfileEntity::class,
        CategoryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN petrolLiters REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN petrolRate REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN extraItem TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN profileImageBase64 TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN accountType TEXT NOT NULL DEFAULT 'BANK'")
                db.execSQL("UPDATE accounts SET accountType = 'WALLET' WHERE LOWER(id) IN ('sadapay', 'easypaisa', 'jazzcash', 'wallet') OR LOWER(name) IN ('sadapay', 'easypaisa', 'jazzcash', 'wallet')")
            }
        }

        /**
         * Thread-safe singleton builder for the Room SQLite database.
         * Persists all transactions, accounts, and financial records safely across app restarts.
         */
        fun getDatabase(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "finance_statement_database.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
