package mobile.dairy.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow
import mobile.dairy.app.domain.AppPrefs
import mobile.dairy.app.domain.DailyRating
import mobile.dairy.app.domain.Expense
import mobile.dairy.app.domain.Goal
import mobile.dairy.app.domain.GoalUpdate
import mobile.dairy.app.domain.Insight
import mobile.dairy.app.domain.JournalEntry
import mobile.dairy.app.domain.LocalUser
import mobile.dairy.app.domain.Saving

@Dao
interface LocalUserDao {
    @Query("SELECT * FROM local_users ORDER BY createdAt DESC")
    fun getAllLocalUsers(): Flow<List<LocalUser>>

    @Query("SELECT * FROM local_users WHERE id = :id LIMIT 1")
    suspend fun getLocalUser(id: String): LocalUser?

    @Query("SELECT * FROM local_users WHERE id = :id LIMIT 1")
    fun getLocalUserFlow(id: String): Flow<LocalUser?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLocalUser(user: LocalUser)

    @Delete
    suspend fun deleteLocalUser(user: LocalUser)
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY date DESC")
    fun getAllEntries(userId: String): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getEntries(userId: String, limit: Int): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getEntriesBetween(userId: String, startDate: String, endDate: String): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE userId = :userId AND date = :date LIMIT 1")
    fun getEntry(userId: String, date: String): Flow<JournalEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: JournalEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEntries(entries: List<JournalEntry>)

    @Query("SELECT * FROM daily_ratings WHERE userId = :userId ORDER BY date DESC LIMIT :days")
    fun getRatings(userId: String, days: Int): Flow<List<DailyRating>>

    @Query("SELECT * FROM daily_ratings WHERE userId = :userId AND date = :date LIMIT 1")
    fun getRating(userId: String, date: String): Flow<DailyRating?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRating(rating: DailyRating)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRatings(ratings: List<DailyRating>)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE userId = :userId AND status IN (:statuses) ORDER BY updatedAt DESC")
    fun getGoalsByStatus(userId: String, statuses: List<String>): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE userId = :userId AND id = :id LIMIT 1")
    fun getGoal(userId: String, id: String): Flow<Goal?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllGoals(goals: List<Goal>)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE userId = :userId AND id = :goalId")
    suspend fun deleteGoalById(userId: String, goalId: String)

    @Query("SELECT * FROM goal_updates WHERE userId = :userId AND goalId = :goalId ORDER BY createdAt DESC LIMIT :limit")
    fun getUpdatesForGoal(userId: String, goalId: String, limit: Int): Flow<List<GoalUpdate>>

    @Query("SELECT * FROM goal_updates WHERE userId = :userId ORDER BY date DESC")
    fun getAllUpdates(userId: String): Flow<List<GoalUpdate>>

    @Query("SELECT * FROM goal_updates WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getUpdates(userId: String, limit: Int): Flow<List<GoalUpdate>>

    @Query("SELECT * FROM goal_updates WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getUpdatesBetween(userId: String, startDate: String, endDate: String): Flow<List<GoalUpdate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpdate(update: GoalUpdate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUpdates(updates: List<GoalUpdate>)

    @Update
    suspend fun updateGoalUpdate(update: GoalUpdate)

    @Delete
    suspend fun deleteUpdate(update: GoalUpdate)
}

@Dao
interface FinanceDao {
    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC")
    fun getAllExpenses(userId: String): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getExpenses(userId: String, limit: Int): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getExpensesBetween(userId: String, startDate: String, endDate: String): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllExpenses(expenses: List<Expense>)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE userId = :userId AND id = :id")
    suspend fun deleteExpense(userId: String, id: String)

    @Query("SELECT * FROM savings WHERE userId = :userId ORDER BY date DESC")
    fun getAllSavings(userId: String): Flow<List<Saving>>

    @Query("SELECT * FROM savings WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    fun getSavings(userId: String, limit: Int): Flow<List<Saving>>

    @Query("SELECT * FROM savings WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getSavingsBetween(userId: String, startDate: String, endDate: String): Flow<List<Saving>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaving(saving: Saving)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllSavings(savings: List<Saving>)

    @Update
    suspend fun updateSaving(saving: Saving)

    @Query("DELETE FROM savings WHERE userId = :userId AND id = :id")
    suspend fun deleteSaving(userId: String, id: String)
}

@Dao
interface InsightDao {
    @Query("SELECT * FROM insights WHERE userId = :userId AND seen = 0 ORDER BY createdAt DESC LIMIT :limit")
    fun getUnseenInsights(userId: String, limit: Int): Flow<List<Insight>>

    @Query("SELECT * FROM insights WHERE userId = :userId ORDER BY createdAt DESC")
    fun getAllInsights(userId: String): Flow<List<Insight>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsights(insights: List<Insight>)

    @Query("UPDATE insights SET seen = 1 WHERE userId = :userId AND id = :id")
    suspend fun markSeen(userId: String, id: String)
}

@Dao
interface PrefsDao {
    @Query("SELECT * FROM app_prefs WHERE userId = :userId LIMIT 1")
    fun getAppPrefsFlow(userId: String): Flow<AppPrefs?>

    @Query("SELECT * FROM app_prefs WHERE userId = :userId LIMIT 1")
    suspend fun getAppPrefs(userId: String): AppPrefs?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppPrefs(prefs: AppPrefs)
}
