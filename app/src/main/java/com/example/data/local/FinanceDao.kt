package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao : TransactionDao {

    // Accounts
    @Query("SELECT * FROM accounts ORDER BY orderIndex ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY orderIndex ASC")
    suspend fun getAllAccountsDirect(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    suspend fun getAccountById(id: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    // Transactions are inherited from TransactionDao

    // Petrol Expenses
    @Query("SELECT * FROM petrol_expenses ORDER BY serialNumber DESC, id DESC")
    fun getAllPetrolExpenses(): Flow<List<PetrolExpenseEntity>>

    @Query("SELECT * FROM petrol_expenses WHERE date LIKE :yearMonth || '%' ORDER BY serialNumber ASC")
    fun getPetrolByMonth(yearMonth: String): Flow<List<PetrolExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPetrolExpense(expense: PetrolExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPetrolExpenses(expenses: List<PetrolExpenseEntity>)

    @Delete
    suspend fun deletePetrolExpense(expense: PetrolExpenseEntity)

    @Query("DELETE FROM petrol_expenses WHERE id = :id")
    suspend fun deletePetrolById(id: Long)

    @Query("SELECT MAX(serialNumber) FROM petrol_expenses")
    suspend fun getMaxPetrolSerialNumber(): Int?

    // Extra Expenses
    @Query("SELECT * FROM extra_expenses ORDER BY serialNumber DESC, id DESC")
    fun getAllExtraExpenses(): Flow<List<ExtraExpenseEntity>>

    @Query("SELECT * FROM extra_expenses WHERE date LIKE :yearMonth || '%' ORDER BY serialNumber ASC")
    fun getExtraByMonth(yearMonth: String): Flow<List<ExtraExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtraExpense(expense: ExtraExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtraExpenses(expenses: List<ExtraExpenseEntity>)

    @Delete
    suspend fun deleteExtraExpense(expense: ExtraExpenseEntity)

    @Query("DELETE FROM extra_expenses WHERE id = :id")
    suspend fun deleteExtraById(id: Long)

    @Query("SELECT MAX(serialNumber) FROM extra_expenses")
    suspend fun getMaxExtraSerialNumber(): Int?

    // Salary Records
    @Query("SELECT * FROM salary_records ORDER BY salaryDate DESC")
    fun getAllSalaryRecords(): Flow<List<SalaryRecordEntity>>

    @Query("SELECT * FROM salary_records WHERE salaryDate = :salaryDate LIMIT 1")
    suspend fun getSalaryByDate(salaryDate: String): SalaryRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalaryRecord(salary: SalaryRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalaryRecords(salaries: List<SalaryRecordEntity>)

    @Delete
    suspend fun deleteSalaryRecord(salary: SalaryRecordEntity)

    // Loan Records
    @Query("SELECT * FROM loan_records ORDER BY date DESC, id DESC")
    fun getAllLoanRecords(): Flow<List<LoanRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanRecord(loan: LoanRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanRecords(loans: List<LoanRecordEntity>)

    @Update
    suspend fun updateLoanRecord(loan: LoanRecordEntity)

    @Delete
    suspend fun deleteLoanRecord(loan: LoanRecordEntity)

    // Monthly Statements
    @Query("SELECT * FROM monthly_statements ORDER BY year DESC, month DESC")
    fun getAllMonthlyStatements(): Flow<List<MonthlyStatementEntity>>

    @Query("SELECT * FROM monthly_statements WHERE year = :year ORDER BY month ASC")
    fun getMonthlyStatementsForYear(year: Int): Flow<List<MonthlyStatementEntity>>

    @Query("SELECT * FROM monthly_statements WHERE id = :id LIMIT 1")
    suspend fun getMonthlyStatementById(id: String): MonthlyStatementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyStatement(statement: MonthlyStatementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyStatements(statements: List<MonthlyStatementEntity>)

    // User Profile
    @Query("SELECT * FROM user_profile ORDER BY isLoggedIn DESC, id DESC LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile ORDER BY isLoggedIn DESC, id DESC LIMIT 1")
    suspend fun getUserProfileDirect(): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE LOWER(TRIM(email)) = LOWER(TRIM(:email)) ORDER BY id DESC LIMIT 1")
    suspend fun getUserByEmail(email: String): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE LOWER(TRIM(email)) = LOWER(TRIM(:email)) ORDER BY id DESC")
    suspend fun getAllUsersByEmail(email: String): List<UserProfileEntity>

    @Query("SELECT * FROM user_profile ORDER BY id ASC")
    suspend fun getAllUsers(): List<UserProfileEntity>

    @Query("SELECT * FROM user_profile ORDER BY id ASC LIMIT 1")
    suspend fun getAnyUserProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(userProfile: UserProfileEntity): Long

    // Partial updates - guaranteed to never overwrite passwordHash
    @Query("UPDATE user_profile SET fullName = :fullName, email = :email, mobile = :mobile, profileImageBase64 = :profileImageBase64 WHERE id = :id")
    suspend fun updateProfileDetails(id: Int, fullName: String, email: String, mobile: String, profileImageBase64: String)

    @Query("UPDATE user_profile SET passwordHash = :passwordHash WHERE id = :id")
    suspend fun updatePasswordHash(id: Int, passwordHash: String)

    @Query("UPDATE user_profile SET pinCode = :pinCode WHERE id = :id")
    suspend fun updatePinCode(id: Int, pinCode: String)

    @Query("UPDATE user_profile SET isBiometricEnabled = :enabled WHERE id = :id")
    suspend fun updateBiometricEnabled(id: Int, enabled: Boolean)

    @Query("UPDATE user_profile SET isLoggedIn = :isLoggedIn WHERE id = :id")
    suspend fun updateLoginSession(id: Int, isLoggedIn: Boolean)

    @Query("UPDATE user_profile SET isEmailVerified = :verified, accountStatus = :status WHERE id = :id")
    suspend fun updateEmailVerified(id: Int, verified: Boolean, status: String)

    @Query("UPDATE user_profile SET verificationToken = :token, verificationExpiry = :expiry WHERE id = :id")
    suspend fun updateVerificationToken(id: Int, token: String, expiry: Long)

    @Query("DELETE FROM user_profile WHERE LOWER(TRIM(email)) = LOWER(TRIM(:email)) AND id != :keepId")
    suspend fun deleteDuplicateUsers(email: String, keepId: Int)

    @Query("DELETE FROM user_profile WHERE email = 'abdulquddus7745@gmail.com' AND passwordHash = '123456'")
    suspend fun deleteLegacyDemoUser()

    // Categories
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    // Wipe / Clear for reset or restore
    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM petrol_expenses")
    suspend fun clearPetrol()

    @Query("DELETE FROM extra_expenses")
    suspend fun clearExtra()

    @Query("DELETE FROM salary_records")
    suspend fun clearSalary()

    @Query("DELETE FROM loan_records")
    suspend fun clearLoans()

    @Query("DELETE FROM monthly_statements")
    suspend fun clearMonthlyStatements()

    // Udhaar Records
    @Query("SELECT * FROM udhaar_records ORDER BY date DESC, id DESC")
    fun getAllUdhaarRecords(): Flow<List<UdhaarRecordEntity>>

    @Query("SELECT * FROM udhaar_records ORDER BY date DESC, id DESC")
    suspend fun getAllUdhaarRecordsDirect(): List<UdhaarRecordEntity>

    @Query("SELECT * FROM udhaar_records WHERE id = :id LIMIT 1")
    suspend fun getUdhaarRecordById(id: Long): UdhaarRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUdhaarRecord(record: UdhaarRecordEntity): Long

    @Update
    suspend fun updateUdhaarRecord(record: UdhaarRecordEntity)

    @Delete
    suspend fun deleteUdhaarRecord(record: UdhaarRecordEntity)

    @Query("DELETE FROM udhaar_records WHERE id = :id")
    suspend fun deleteUdhaarRecordById(id: Long)

    // Udhaar Repayments
    @Query("SELECT * FROM udhaar_repayments ORDER BY date ASC, id ASC")
    fun getAllUdhaarRepayments(): Flow<List<UdhaarRepaymentEntity>>

    @Query("SELECT * FROM udhaar_repayments WHERE udhaarId = :udhaarId ORDER BY date ASC, id ASC")
    fun getRepaymentsForUdhaar(udhaarId: Long): Flow<List<UdhaarRepaymentEntity>>

    @Query("SELECT * FROM udhaar_repayments WHERE udhaarId = :udhaarId ORDER BY date ASC, id ASC")
    suspend fun getRepaymentsForUdhaarDirect(udhaarId: Long): List<UdhaarRepaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUdhaarRepayment(repayment: UdhaarRepaymentEntity): Long

    @Update
    suspend fun updateUdhaarRepayment(repayment: UdhaarRepaymentEntity)

    @Delete
    suspend fun deleteUdhaarRepayment(repayment: UdhaarRepaymentEntity)

    @Query("DELETE FROM udhaar_repayments WHERE id = :id")
    suspend fun deleteUdhaarRepaymentById(id: Long)

    @Query("DELETE FROM udhaar_records")
    suspend fun clearUdhaarRecords()

    @Query("DELETE FROM udhaar_repayments")
    suspend fun clearUdhaarRepayments()
}
