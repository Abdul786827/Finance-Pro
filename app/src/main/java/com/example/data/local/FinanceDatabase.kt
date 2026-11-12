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
        CategoryEntity::class,
        UdhaarRecordEntity::class,
        UdhaarRepaymentEntity::class
    ],
    version = 7,
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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Completely separate ledger: udhaar_records and udhaar_repayments
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS udhaar_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        personName TEXT NOT NULL,
                        phoneNumber TEXT NOT NULL DEFAULT '',
                        category TEXT NOT NULL DEFAULT 'MONEY',
                        originalAmount REAL NOT NULL DEFAULT 0.0,
                        remainingAmount REAL NOT NULL DEFAULT 0.0,
                        itemDescription TEXT NOT NULL DEFAULT '',
                        date TEXT NOT NULL,
                        time TEXT NOT NULL,
                        dayOfWeek TEXT NOT NULL DEFAULT '',
                        dueDate TEXT NOT NULL DEFAULT '',
                        note TEXT NOT NULL DEFAULT '',
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        isItemReturned INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS udhaar_repayments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        udhaarId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        date TEXT NOT NULL,
                        time TEXT NOT NULL,
                        dayOfWeek TEXT NOT NULL DEFAULT '',
                        note TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (udhaarId) REFERENCES udhaar_records(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS index_udhaar_repayments_udhaarId ON udhaar_repayments(udhaarId)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE udhaar_records ADD COLUMN accountName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE udhaar_records ADD COLUMN transactionId INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE udhaar_repayments ADD COLUMN accountName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE udhaar_repayments ADD COLUMN transactionId INTEGER NOT NULL DEFAULT 0")
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
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
