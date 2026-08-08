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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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
class EntryRepository @Inject constructor(private val paths: FirestorePaths) {

    fun entries(limit: Int): Flow<List<JournalEntry>> = entries(limit.toLong(), null, null)

    fun entries(startDate: String? = null, endDate: String? = null): Flow<List<JournalEntry>> =
        entries(null, startDate, endDate)

    private fun entries(limit: Long?, startDate: String?, endDate: String?): Flow<List<JournalEntry>> {
        var query: Query = paths.col("entries").orderBy("date", Query.Direction.DESCENDING)
        if (limit != null) query = query.limit(limit)
        if (startDate != null) query = query.whereGreaterThanOrEqualTo("date", startDate)
        if (endDate != null) query = query.whereLessThanOrEqualTo("date", endDate)
        return query.asFlow().map { it.toList<JournalEntry>() }
            .catch { Log.e("Repo", "Error", it); emit(emptyList()) }
    }

    fun entry(date: String): Flow<JournalEntry?> =
        paths.col("entries").document(date).asFlow().map { it.toObject(JournalEntry::class.java) }
            .catch { Log.e("Repo", "Error", it); emit(null) }

    fun ratings(days: Long = 90): Flow<List<DailyRating>> {
        return paths.col("dailyRatings")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(days)
            .asFlow().map { it.toList<DailyRating>() }
            .catch { Log.e("Repo", "Error", it); emit(emptyList()) }
    }

    suspend fun upsertEntry(entry: JournalEntry) {
        val now = System.currentTimeMillis()
        val withMeta = entry.copy(
            id = entry.date,
            createdAt = if (entry.createdAt == 0L) now else entry.createdAt,
            updatedAt = now,
        )
        try {
            paths.col("entries").document(entry.date).set(withMeta)
            Log.d("EntryRepo", "Successfully saved entry for ${entry.date}")
        } catch (e: Exception) {
            Log.e("EntryRepo", "Error saving entry: ${e.message}", e)
            throw e
        }
    }

    suspend fun setFlag(date: String, field: String, value: Boolean) {
        try {
            paths.col("entries").document(date)
                .update(mapOf(field to value, "updatedAt" to System.currentTimeMillis()))
        } catch (e: Exception) {
            Log.e("EntryRepo", "Error setting flag $field: ${e.message}", e)
            throw e
        }
    }

    suspend fun upsertRating(rating: DailyRating) {
        try {
            paths.col("dailyRatings").document(rating.date)
                .set(rating.copy(updatedAt = System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge())
        } catch (e: Exception) {
            Log.e("EntryRepo", "Error saving rating: ${e.message}", e)
            throw e
        }
    }
}

@Singleton
class GoalRepository @Inject constructor(private val paths: FirestorePaths) {

    fun goals(statuses: List<String> = listOf("active")): Flow<List<Goal>> =
        paths.col("goals").whereIn("status", statuses)
            .asFlow().map { it.toList<Goal>().sortedByDescending { g -> g.updatedAt } }
            .catch { Log.e("Repo", "Error", it); emit(emptyList()) }

    fun goal(id: String): Flow<Goal?> =
        paths.col("goals").document(id).asFlow().map { it.toObject(Goal::class.java) }
            .catch { Log.e("Repo", "Error", it); emit(null) }

    fun updatesFor(goalId: String, max: Long = 60): Flow<List<GoalUpdate>> =
        paths.goalUpdates(goalId).orderBy("createdAt", Query.Direction.DESCENDING).limit(max)
            .asFlow().map { it.toList<GoalUpdate>() }
            .catch { Log.e("Repo", "Error", it); emit(emptyList()) }

    /** Flat mirror of all goal updates — one listener for dashboard + insights. */
    fun updateLog(limit: Int): Flow<List<GoalUpdate>> = updateLog(limit.toLong(), null, null)

    fun updateLog(startDate: String? = null, endDate: String? = null): Flow<List<GoalUpdate>> =
        updateLog(null, startDate, endDate)

    private fun updateLog(limit: Long?, startDate: String?, endDate: String?): Flow<List<GoalUpdate>> {
        var query: Query = paths.col("goalUpdateLog").orderBy("date", Query.Direction.DESCENDING)
        if (limit != null) query = query.limit(limit)
        if (startDate != null) query = query.whereGreaterThanOrEqualTo("date", startDate)
        if (endDate != null) query = query.whereLessThanOrEqualTo("date", endDate)
        return query.asFlow().map { it.toList<GoalUpdate>() }
            .catch { Log.e("Repo", "Error", it); emit(emptyList()) }
    }

    suspend fun createGoal(goal: Goal): String {
        val id = newId()
        val now = System.currentTimeMillis()
        try {
            paths.col("goals").document(id).set(goal.copy(id = id, createdAt = now, updatedAt = now))
            return id
        } catch (e: Exception) {
            Log.e("GoalRepo", "Error creating goal: ${e.message}", e)
            throw e
        }
    }

    suspend fun patchGoal(goalId: String, patch: Map<String, Any?>) {
        try {
            paths.col("goals").document(goalId)
                .update(patch + ("updatedAt" to System.currentTimeMillis()))
        } catch (e: Exception) {
            Log.e("GoalRepo", "Error patching goal: ${e.message}", e)
            throw e
        }
    }

    suspend fun addUpdate(goal: Goal, update: GoalUpdate) {
        val id = newId()
        val full = update.copy(id = id, goalId = goal.id, createdAt = System.currentTimeMillis())
        val batch = paths.batch()
        batch.set(paths.goalUpdates(goal.id).document(id), full)
        batch.set(paths.col("goalUpdateLog").document(id), full)
        val goalPatch = mutableMapOf<String, Any>(
            "progress" to full.progress,
            "updatedAt" to System.currentTimeMillis(),
        )
        if (!full.status.isNullOrBlank()) {
            goalPatch["status"] = full.status!!
        } else if (full.progress >= 100) {
            goalPatch["status"] = "completed"
        }
        batch.update(paths.col("goals").document(goal.id), goalPatch)
        try {
            batch.commit()
        } catch (e: Exception) {
            Log.e("GoalRepo", "Error committing goal update batch: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteUpdate(goal: Goal, update: GoalUpdate) {
        val batch = paths.batch()
        batch.delete(paths.goalUpdates(goal.id).document(update.id))
        batch.delete(paths.col("goalUpdateLog").document(update.id))
        try {
            batch.commit()
        } catch (e: Exception) {
            Log.e("GoalRepo", "Error deleting goal update: ${e.message}", e)
            throw e
        }
    }

    suspend fun editUpdate(goal: Goal, update: GoalUpdate) {
        val batch = paths.batch()
        batch.set(paths.goalUpdates(goal.id).document(update.id), update)
        batch.set(paths.col("goalUpdateLog").document(update.id), update)
        try {
            batch.commit()
        } catch (e: Exception) {
            Log.e("GoalRepo", "Error editing goal update: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateMilestones(goalId: String, milestones: List<mobile.dairy.app.domain.Milestone>) {
        patchGoal(goalId, mapOf("milestones" to milestones))
    }

    suspend fun updateDailyTasks(goalId: String, tasks: List<mobile.dairy.app.domain.GoalTask>) {
        patchGoal(goalId, mapOf("dailyTasks" to tasks))
    }

    suspend fun deleteGoal(goalId: String) {
        try {
            paths.col("goals").document(goalId).delete()
        } catch (e: Exception) {
            Log.e("GoalRepo", "Error deleting goal: ${e.message}", e)
            throw e
        }
    }
}

@Singleton
class FinanceRepository @Inject constructor(private val paths: FirestorePaths) {

    fun expenses(limit: Int): Flow<List<Expense>> = expenses(limit.toLong(), null, null)

    fun expenses(startDate: String? = null, endDate: String? = null): Flow<List<Expense>> =
        expenses(null, startDate, endDate)

    private fun expenses(limit: Long?, startDate: String?, endDate: String?): Flow<List<Expense>> {
        var query: Query = paths.col("expenses").orderBy("date", Query.Direction.DESCENDING)
        if (limit != null) query = query.limit(limit)
        if (startDate != null) query = query.whereGreaterThanOrEqualTo("date", startDate)
        if (endDate != null) query = query.whereLessThanOrEqualTo("date", endDate)
        return query.asFlow().map { it.toList<Expense>() }
            .catch { Log.e("Repo", "Error", it); emit(emptyList()) }
    }

    fun savings(limit: Int): Flow<List<Saving>> = savings(limit.toLong(), null, null)

    fun savings(startDate: String? = null, endDate: String? = null): Flow<List<Saving>> =
        savings(null, startDate, endDate)

    private fun savings(limit: Long?, startDate: String?, endDate: String?): Flow<List<Saving>> {
        var query: Query = paths.col("savings").orderBy("date", Query.Direction.DESCENDING)
        if (limit != null) query = query.limit(limit)
        if (startDate != null) query = query.whereGreaterThanOrEqualTo("date", startDate)
        if (endDate != null) query = query.whereLessThanOrEqualTo("date", endDate)
        return query.asFlow().map { it.toList<Saving>() }
            .catch { Log.e("Repo", "Error", it); emit(emptyList()) }
    }

    suspend fun addExpense(expense: Expense) {
        val id = newId()
        try {
            paths.col("expenses").document(id)
                .set(expense.copy(id = id, createdAt = System.currentTimeMillis()))
        } catch (e: Exception) {
            Log.e("FinanceRepo", "Error adding expense: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteExpense(id: String) {
        try {
            paths.col("expenses").document(id).delete()
        } catch (e: Exception) {
            Log.e("FinanceRepo", "Error deleting expense: ${e.message}", e)
            throw e
        }
    }

    suspend fun addSaving(saving: Saving) {
        val id = newId()
        try {
            paths.col("savings").document(id)
                .set(saving.copy(id = id, createdAt = System.currentTimeMillis()))
        } catch (e: Exception) {
            Log.e("FinanceRepo", "Error adding saving: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateExpense(expense: Expense) {
        try {
            paths.col("expenses").document(expense.id)
                .set(expense)
        } catch (e: Exception) {
            Log.e("FinanceRepo", "Error updating expense: ${e.message}", e)
            throw e
        }
    }

    suspend fun updateSaving(saving: Saving) {
        try {
            paths.col("savings").document(saving.id)
                .set(saving)
        } catch (e: Exception) {
            Log.e("FinanceRepo", "Error updating saving: ${e.message}", e)
            throw e
        }
    }

    suspend fun deleteSaving(id: String) {
        try {
            paths.col("savings").document(id).delete()
        } catch (e: Exception) {
            Log.e("FinanceRepo", "Error deleting saving: ${e.message}", e)
            throw e
        }
    }
}

@Singleton
class InsightRepository @Inject constructor(private val paths: FirestorePaths) {

    fun unseen(max: Long = 3): Flow<List<Insight>> =
        paths.col("insights").whereEqualTo("seen", false)
            .asFlow().map {
                it.toList<Insight>()
                    .sortedByDescending { i -> i.createdAt }
                    .take(max.toInt())
            }
            .catch { Log.e("Repo", "Error", it); emit(emptyList()) }

    /** Keyed by date+type, so re-running the engine can never spam the user. */
    suspend fun saveAll(insights: List<Insight>) {
        if (insights.isEmpty()) return
        val batch = paths.batch()
        for (i in insights) {
            batch.set(paths.col("insights").document(i.id), i, com.google.firebase.firestore.SetOptions.merge())
        }
        batch.commit()
    }

    suspend fun markSeen(id: String) {
        paths.col("insights").document(id).update("seen", true)
    }

    fun screenTime(days: Long = 30): Flow<List<ScreenTimeDay>> {
        return paths.col("screenTime")
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(days)
            .asFlow().map { it.toList<ScreenTimeDay>() }
            .catch { Log.e("Repo", "Error", it); emit(emptyList()) }
    }

    suspend fun saveScreenTime(day: ScreenTimeDay) {
        paths.col("screenTime").document(day.date)
            .set(day.copy(updatedAt = System.currentTimeMillis()), com.google.firebase.firestore.SetOptions.merge())
    }
}

@Singleton
class PrefsRepository @Inject constructor(private val paths: FirestorePaths) {

    fun appPrefs(): Flow<AppPrefs> =
        paths.col("prefs").document("app").asFlow().map { it.toObject(AppPrefs::class.java) ?: AppPrefs() }
            .catch { Log.e("Repo", "Error", it); emit(AppPrefs()) }

    suspend fun save(patch: Map<String, Any?>) {
        paths.col("prefs").document("app")
            .set(patch, com.google.firebase.firestore.SetOptions.merge())
    }

    suspend fun saveDeviceToken(token: String) {
        paths.col("devices").document(token).set(
            mapOf("token" to token, "platform" to "android", "updatedAt" to System.currentTimeMillis())
        )
    }
}
