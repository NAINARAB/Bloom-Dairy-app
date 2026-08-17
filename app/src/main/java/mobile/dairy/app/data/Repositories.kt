package mobile.dairy.app.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import mobile.dairy.app.core.Dates
import mobile.dairy.app.domain.AppPrefs
import mobile.dairy.app.domain.DailyRating
import mobile.dairy.app.domain.Expense
import mobile.dairy.app.domain.Goal
import mobile.dairy.app.domain.GoalUpdate
import mobile.dairy.app.domain.Insight
import mobile.dairy.app.domain.JournalEntry
import mobile.dairy.app.domain.Saving
import mobile.dairy.app.domain.ScreenTimeDay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import mobile.dairy.app.services.LocalPrefs
import kotlinx.coroutines.ExperimentalCoroutinesApi

/* ------------------------------------------------------------------ */
/* Snapshot listeners as Flows (self-contained, no KTX API drift)      */
/* ------------------------------------------------------------------ */

fun Query.asFlow(): Flow<com.google.firebase.firestore.QuerySnapshot> = callbackFlow {
    val reg = addSnapshotListener { snap, err ->
        if (err != null) { close(err); return@addSnapshotListener }
        if (snap != null) trySend(snap)
    }
    awaitClose { reg.remove() }
}

fun DocumentReference.asFlow(): Flow<com.google.firebase.firestore.DocumentSnapshot> = callbackFlow {
    val reg = addSnapshotListener { snap, err ->
        if (err != null) { close(err); return@addSnapshotListener }
        if (snap != null) trySend(snap)
    }
    awaitClose { reg.remove() }
}

inline fun <reified T> com.google.firebase.firestore.QuerySnapshot.toList(): List<T> =
    documents.mapNotNull { it.toObject(T::class.java) }

fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(20)

/* ------------------------------------------------------------------ */
/* Session Manager                                                     */
/* ------------------------------------------------------------------ */

@Singleton
class SessionManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val localPrefs: LocalPrefs
) {
    suspend fun currentUserId(): String {
        return auth.currentUser?.uid ?: localPrefs.activeLocalUserId.firstOrNull() ?: "guest"
    }

    fun currentUserIdFlow(): Flow<String> {
        return localPrefs.activeLocalUserId.map { local -> auth.currentUser?.uid ?: local ?: "guest" }
    }
}

/* ------------------------------------------------------------------ */
/* Paths                                                               */
/* ------------------------------------------------------------------ */

@Singleton
class FirestorePaths @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
) {
    val uid: String? get() = auth.currentUser?.uid
    fun requireUid(): String = uid ?: error("Not signed in")

    fun userDoc(uid: String = requireUid()): DocumentReference = db.collection("users").document(uid)
    fun col(name: String, uid: String = requireUid()): CollectionReference = userDoc(uid).collection(name)
    fun goalUpdates(goalId: String): CollectionReference = col("goals").document(goalId).collection("updates")
    fun batch() = db.batch()
}

/* ------------------------------------------------------------------ */
/* Repositories                                                        */
/* ------------------------------------------------------------------ */

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class EntryRepository @Inject constructor(
    private val entryDao: EntryDao,
    private val sessionManager: SessionManager,
    private val paths: FirestorePaths
) {
    fun entries(limit: Int): Flow<List<JournalEntry>> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        entryDao.getEntries(uid, limit)
    }

    fun entries(startDate: String? = null, endDate: String? = null): Flow<List<JournalEntry>> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        if (startDate != null && endDate != null) {
            entryDao.getEntriesBetween(uid, startDate, endDate)
        } else {
            entryDao.getAllEntries(uid)
        }
    }

    fun entry(date: String): Flow<JournalEntry?> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        entryDao.getEntry(uid, date)
    }

    fun rating(date: String): Flow<DailyRating?> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        entryDao.getRating(uid, date)
    }

    fun ratings(days: Long = 90): Flow<List<DailyRating>> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        entryDao.getRatings(uid, days.toInt())
    }

    suspend fun upsertEntry(entry: JournalEntry) {
        val uid = sessionManager.currentUserId()
        val now = System.currentTimeMillis()
        val isNew = entry.createdAt == 0L
        val withMeta = entry.copy(
            id = entry.date,
            createdAt = if (isNew) now else entry.createdAt,
            updatedAt = now,
            alterId = if (isNew) 1 else entry.alterId + 1,
            isSynced = false,
            userId = uid
        )
        entryDao.upsertEntry(withMeta)
    }

    suspend fun setFlag(date: String, field: String, value: Boolean) {
        val uid = sessionManager.currentUserId()
        val current = entryDao.getEntry(uid, date).firstOrNull()
        if (current != null) {
            val updated = when (field) {
                "favorite" -> current.copy(favorite = value, updatedAt = System.currentTimeMillis(), alterId = current.alterId + 1, isSynced = false)
                "important" -> current.copy(important = value, updatedAt = System.currentTimeMillis(), alterId = current.alterId + 1, isSynced = false)
                else -> current.copy(updatedAt = System.currentTimeMillis(), alterId = current.alterId + 1, isSynced = false)
            }
            entryDao.upsertEntry(updated)
        }
    }

    suspend fun upsertRating(rating: DailyRating) {
        val uid = sessionManager.currentUserId()
        val current = entryDao.getRating(uid, rating.date).firstOrNull()
        val newAlterId = if (current != null) current.alterId + 1 else 1
        entryDao.upsertRating(rating.copy(updatedAt = System.currentTimeMillis(), alterId = newAlterId, isSynced = false, userId = uid))
    }
}

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao,
    private val sessionManager: SessionManager,
    private val paths: FirestorePaths
) {
    fun goals(statuses: List<String> = listOf("active")): Flow<List<Goal>> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        goalDao.getGoalsByStatus(uid, statuses)
    }

    fun goal(id: String): Flow<Goal?> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        goalDao.getGoal(uid, id)
    }

    fun updatesFor(goalId: String, max: Long = 60): Flow<List<GoalUpdate>> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        goalDao.getUpdatesForGoal(uid, goalId, max.toInt())
    }

    fun updateLog(limit: Int): Flow<List<GoalUpdate>> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        goalDao.getUpdates(uid, limit)
    }

    fun updateLog(startDate: String? = null, endDate: String? = null): Flow<List<GoalUpdate>> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        if (startDate != null && endDate != null) {
            goalDao.getUpdatesBetween(uid, startDate, endDate)
        } else {
            goalDao.getAllUpdates(uid)
        }
    }

    suspend fun createGoal(goal: Goal): String {
        val uid = sessionManager.currentUserId()
        val id = newId()
        val now = System.currentTimeMillis()
        goalDao.insertGoal(goal.copy(id = id, createdAt = now, updatedAt = now, alterId = 1, isSynced = false, userId = uid))
        return id
    }

    suspend fun patchGoal(goalId: String, patch: Map<String, Any?>) {
        val uid = sessionManager.currentUserId()
        val current = goalDao.getGoal(uid, goalId).firstOrNull() ?: return
        
        var updated = current.copy(updatedAt = System.currentTimeMillis(), alterId = current.alterId + 1, isSynced = false)
        patch.forEach { (key, value) ->
            when (key) {
                "progress" -> updated = updated.copy(progress = value as Int)
                "status" -> updated = updated.copy(status = value as String)
                "milestones" -> updated = updated.copy(milestones = value as List<mobile.dairy.app.domain.Milestone>)
                "dailyTasks" -> updated = updated.copy(dailyTasks = value as List<mobile.dairy.app.domain.GoalTask>)
            }
        }
        goalDao.updateGoal(updated)
    }

    suspend fun addUpdate(goal: Goal, update: GoalUpdate) {
        val uid = sessionManager.currentUserId()
        val id = newId()
        val full = update.copy(id = id, goalId = goal.id, createdAt = System.currentTimeMillis(), alterId = 1, isSynced = false, userId = uid)
        
        goalDao.insertUpdate(full)
        
        val patch = mutableMapOf<String, Any?>("progress" to full.progress)
        if (!full.status.isNullOrBlank()) {
            patch["status"] = full.status!!
        } else if (full.progress >= 100) {
            patch["status"] = "completed"
        }
        patchGoal(goal.id, patch)
    }

    suspend fun deleteUpdate(goal: Goal, update: GoalUpdate) {
        goalDao.deleteUpdate(update)
    }

    suspend fun editUpdate(goal: Goal, update: GoalUpdate) {
        goalDao.updateGoalUpdate(update.copy(alterId = update.alterId + 1, isSynced = false))
    }

    suspend fun updateMilestones(goalId: String, milestones: List<mobile.dairy.app.domain.Milestone>) {
        patchGoal(goalId, mapOf("milestones" to milestones))
    }

    suspend fun updateDailyTasks(goalId: String, tasks: List<mobile.dairy.app.domain.GoalTask>) {
        patchGoal(goalId, mapOf("dailyTasks" to tasks))
    }

    suspend fun deleteGoal(goalId: String) {
        val uid = sessionManager.currentUserId()
        goalDao.deleteGoalById(uid, goalId)
    }
}

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class FinanceRepository @Inject constructor(
    private val financeDao: FinanceDao,
    private val sessionManager: SessionManager
) {

    fun expenses(limit: Int): Flow<List<Expense>> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        financeDao.getExpenses(uid, limit)
    }

    fun expenses(startDate: String? = null, endDate: String? = null): Flow<List<Expense>> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        if (startDate != null && endDate != null) {
            financeDao.getExpensesBetween(uid, startDate, endDate)
        } else {
            financeDao.getAllExpenses(uid)
        }
    }

    fun savings(limit: Int): Flow<List<Saving>> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        financeDao.getSavings(uid, limit)
    }

    fun savings(startDate: String? = null, endDate: String? = null): Flow<List<Saving>> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        if (startDate != null && endDate != null) {
            financeDao.getSavingsBetween(uid, startDate, endDate)
        } else {
            financeDao.getAllSavings(uid)
        }
    }

    suspend fun addExpense(expense: Expense) {
        val uid = sessionManager.currentUserId()
        val id = newId()
        financeDao.insertExpense(expense.copy(id = id, createdAt = System.currentTimeMillis(), alterId = 1, isSynced = false, userId = uid))
    }

    suspend fun deleteExpense(id: String) {
        val uid = sessionManager.currentUserId()
        financeDao.deleteExpense(uid, id)
    }

    suspend fun addSaving(saving: Saving) {
        val uid = sessionManager.currentUserId()
        val id = newId()
        financeDao.insertSaving(saving.copy(id = id, createdAt = System.currentTimeMillis(), alterId = 1, isSynced = false, userId = uid))
    }

    suspend fun updateExpense(expense: Expense) {
        financeDao.updateExpense(expense.copy(alterId = expense.alterId + 1, isSynced = false))
    }

    suspend fun updateSaving(saving: Saving) {
        financeDao.updateSaving(saving.copy(alterId = saving.alterId + 1, isSynced = false))
    }

    suspend fun deleteSaving(id: String) {
        val uid = sessionManager.currentUserId()
        financeDao.deleteSaving(uid, id)
    }
}

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class InsightRepository @Inject constructor(
    private val insightDao: InsightDao,
    private val sessionManager: SessionManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) {

    fun unseen(max: Long = 3): Flow<List<Insight>> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        insightDao.getUnseenInsights(uid, max.toInt())
    }

    suspend fun saveAll(insights: List<Insight>) {
        if (insights.isEmpty()) return
        val uid = sessionManager.currentUserId()
        val withMeta = insights.map { it.copy(alterId = it.alterId + 1, isSynced = false, userId = uid) }
        insightDao.insertInsights(withMeta)
    }

    suspend fun markSeen(id: String) {
        val uid = sessionManager.currentUserId()
        insightDao.markSeen(uid, id)
    }

    fun screenTime(days: Long = 30): Flow<List<ScreenTimeDay>> = kotlinx.coroutines.flow.flow {
        // As per requirements, ScreenTime is fetched dynamically from the OS and not saved in DB.
        // We will just return an empty list or mock data here until the system fetch is implemented properly in the domain layer.
        emit(emptyList())
    }

    suspend fun saveScreenTime(day: ScreenTimeDay) {
        // No-op. ScreenTime is not persisted.
    }
}

@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class PrefsRepository @Inject constructor(
    private val prefsDao: PrefsDao,
    private val sessionManager: SessionManager,
    private val paths: FirestorePaths
) {

    fun appPrefs(): Flow<AppPrefs> = sessionManager.currentUserIdFlow().flatMapLatest { uid ->
        prefsDao.getAppPrefsFlow(uid).map { prefs ->
            val safePrefs = prefs ?: AppPrefs(id = uid, userId = uid)
            if (safePrefs.journalQuestions.any { it.id == "q_rating" }) {
                val newQuestions = safePrefs.journalQuestions.flatMap { q ->
                    if (q.id == "q_rating") {
                        listOf(
                            mobile.dairy.app.domain.JournalQuestionDef("q_rating_performance", "Performance", null, "rating_group", isMandatory = true, isActive = q.isActive),
                            mobile.dairy.app.domain.JournalQuestionDef("q_rating_wellbeing", "Wellbeing", null, "rating_group", isMandatory = true, isActive = q.isActive)
                        )
                    } else listOf(q)
                }
                val migratedPrefs = safePrefs.copy(journalQuestions = newQuestions)
                migratedPrefs
            } else safePrefs
        }
    }

    suspend fun getAppPrefs(): AppPrefs {
        val uid = sessionManager.currentUserId()
        return prefsDao.getAppPrefs(uid) ?: AppPrefs(id = uid, userId = uid)
    }

    suspend fun updateAppPrefs(prefs: AppPrefs) {
        val uid = sessionManager.currentUserId()
        prefsDao.insertAppPrefs(prefs.copy(id = uid, userId = uid))
    }

    suspend fun save(patch: Map<String, Any?>) {
        val current = getAppPrefs()
        var updated = current.copy(alterId = current.alterId + 1, isSynced = false)
        patch.forEach { (key, value) ->
            when (key) {
                "theme" -> updated = updated.copy(theme = value as String)
                "accent" -> updated = updated.copy(accent = value as String)
                "currency" -> updated = updated.copy(currency = value as String)
                "lockEnabled" -> updated = updated.copy(lockEnabled = value as Boolean)
                "journalQuestions" -> updated = updated.copy(journalQuestions = value as List<mobile.dairy.app.domain.JournalQuestionDef>)
                // ... map other fields as necessary. This is simplified.
            }
        }
        updateAppPrefs(updated)
    }

    suspend fun saveDeviceToken(token: String) {
        // Keep device token saving to firestore if needed, or no-op if fully offline
    }
}
