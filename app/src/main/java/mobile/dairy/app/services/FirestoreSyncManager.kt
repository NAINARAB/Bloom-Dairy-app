package mobile.dairy.app.services

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import mobile.dairy.app.data.*
import mobile.dairy.app.domain.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSyncManager @Inject constructor(
    private val paths: FirestorePaths,
    private val entryDao: EntryDao,
    private val goalDao: GoalDao,
    private val financeDao: FinanceDao,
    private val insightDao: InsightDao,
    private val prefsDao: PrefsDao,
) {
    suspend fun sync() = withContext(Dispatchers.IO) {
        val uid = paths.uid
        if (uid == null) {
            Log.w("SyncManager", "Cannot sync, user not signed in")
            return@withContext
        }

        Log.i("SyncManager", "Starting sync for user $uid")

        try {
            syncEntries(uid)
            syncRatings(uid)
            syncGoals(uid)
            syncExpenses(uid)
            syncSavings(uid)
            syncInsights(uid)
            syncPrefs(uid)
            Log.i("SyncManager", "Sync completed successfully")
        } catch (e: Exception) {
            Log.e("SyncManager", "Sync failed", e)
            throw e
        }
    }

    suspend fun migrateLocalUserToCloud(oldId: String, newId: String, database: AppDatabase) = withContext(Dispatchers.IO) {
        val db = database.openHelper.writableDatabase
        db.beginTransaction()
        try {
            val tables = listOf("journal_entries", "daily_ratings", "goals", "goal_updates", "expenses", "savings", "insights", "app_prefs")
            for (table in tables) {
                db.execSQL("UPDATE $table SET userId = ?, alterId = alterId + 1, isSynced = 0 WHERE userId = ?", arrayOf(newId, oldId))
            }
            // Mark the local user as mapped to this firebase account
            db.execSQL("UPDATE local_users SET firebaseUid = ? WHERE id = ?", arrayOf(newId, oldId))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private suspend fun syncEntries(uid: String) {
        val remoteDocs = paths.col("entries").get().await().toList<JournalEntry>()
        val localDocs = entryDao.getAllEntries(uid).first().associateBy { it.id }

        val toInsertLocal = mutableListOf<JournalEntry>()
        
        // 1. Process remote docs
        for (remote in remoteDocs) {
            val local = localDocs[remote.id]
            if (local == null || remote.alterId > local.alterId) {
                toInsertLocal.add(remote.copy(isSynced = true))
            }
        }
        if (toInsertLocal.isNotEmpty()) entryDao.insertAllEntries(toInsertLocal)

        // 2. Process local docs that need pushing
        val batch = paths.batch()
        var pushCount = 0
        for (local in localDocs.values) {
            if (!local.isSynced) {
                batch.set(paths.col("entries").document(local.id), local)
                pushCount++
            }
        }
        if (pushCount > 0) {
            batch.commit().await()
            // Mark them as synced locally
            val syncedLocal = localDocs.values.filter { !it.isSynced }.map { it.copy(isSynced = true) }
            entryDao.insertAllEntries(syncedLocal)
        }
    }

    private suspend fun syncRatings(uid: String) {
        val remoteDocs = paths.col("dailyRatings").get().await().toList<DailyRating>()
        val localDocs = entryDao.getRatings(uid, 10000).first().associateBy { it.date } // Fetch enough to sync
        val toInsertLocal = mutableListOf<DailyRating>()
        
        for (remote in remoteDocs) {
            val local = localDocs[remote.date]
            if (local == null || remote.alterId > local.alterId) {
                toInsertLocal.add(remote.copy(isSynced = true))
            }
        }
        if (toInsertLocal.isNotEmpty()) entryDao.insertAllRatings(toInsertLocal)

        val batch = paths.batch()
        var pushCount = 0
        for (local in localDocs.values) {
            if (!local.isSynced) {
                batch.set(paths.col("dailyRatings").document(local.date), local)
                pushCount++
            }
        }
        if (pushCount > 0) {
            batch.commit().await()
            val syncedLocal = localDocs.values.filter { !it.isSynced }.map { it.copy(isSynced = true) }
            entryDao.insertAllRatings(syncedLocal)
        }
    }

    private suspend fun syncGoals(uid: String) {
        val remoteDocs = paths.col("goals").get().await().toList<Goal>()
        val localDocs = goalDao.getGoalsByStatus(uid, listOf("active", "paused", "completed", "archived")).first().associateBy { it.id }
        
        val toInsertLocal = mutableListOf<Goal>()
        
        for (remote in remoteDocs) {
            val local = localDocs[remote.id]
            if (local == null || remote.alterId > local.alterId) {
                toInsertLocal.add(remote.copy(isSynced = true))
            }
        }
        if (toInsertLocal.isNotEmpty()) goalDao.insertAllGoals(toInsertLocal)

        val batch = paths.batch()
        var pushCount = 0
        for (local in localDocs.values) {
            if (!local.isSynced) {
                batch.set(paths.col("goals").document(local.id), local)
                pushCount++
            }
        }
        if (pushCount > 0) {
            batch.commit().await()
            val syncedLocal = localDocs.values.filter { !it.isSynced }.map { it.copy(isSynced = true) }
            goalDao.insertAllGoals(syncedLocal)
        }

        // Sync Goal Updates
        for (goalId in localDocs.keys.union(remoteDocs.map { it.id })) {
            syncGoalUpdates(uid, goalId)
        }
    }

    private suspend fun syncGoalUpdates(uid: String, goalId: String) {
        val remoteDocs = paths.goalUpdates(goalId).get().await().toList<GoalUpdate>()
        val localDocs = goalDao.getUpdatesForGoal(uid, goalId, 10000).first().associateBy { it.id }

        val toInsertLocal = mutableListOf<GoalUpdate>()
        for (remote in remoteDocs) {
            val local = localDocs[remote.id]
            if (local == null || remote.alterId > local.alterId) {
                toInsertLocal.add(remote.copy(isSynced = true))
            }
        }
        if (toInsertLocal.isNotEmpty()) goalDao.insertAllUpdates(toInsertLocal)

        val batch = paths.batch()
        var pushCount = 0
        for (local in localDocs.values) {
            if (!local.isSynced) {
                batch.set(paths.goalUpdates(goalId).document(local.id), local)
                pushCount++
            }
        }
        if (pushCount > 0) {
            batch.commit().await()
            val syncedLocal = localDocs.values.filter { !it.isSynced }.map { it.copy(isSynced = true) }
            goalDao.insertAllUpdates(syncedLocal)
        }
    }

    private suspend fun syncExpenses(uid: String) {
        val remoteDocs = paths.col("expenses").get().await().toList<Expense>()
        val localDocs = financeDao.getAllExpenses(uid).first().associateBy { it.id }
        val toInsertLocal = mutableListOf<Expense>()
        
        for (remote in remoteDocs) {
            val local = localDocs[remote.id]
            if (local == null || remote.alterId > local.alterId) {
                toInsertLocal.add(remote.copy(isSynced = true))
            }
        }
        if (toInsertLocal.isNotEmpty()) financeDao.insertAllExpenses(toInsertLocal)

        val batch = paths.batch()
        var pushCount = 0
        for (local in localDocs.values) {
            if (!local.isSynced) {
                batch.set(paths.col("expenses").document(local.id), local)
                pushCount++
            }
        }
        if (pushCount > 0) {
            batch.commit().await()
            val syncedLocal = localDocs.values.filter { !it.isSynced }.map { it.copy(isSynced = true) }
            financeDao.insertAllExpenses(syncedLocal)
        }
    }

    private suspend fun syncSavings(uid: String) {
        val remoteDocs = paths.col("savings").get().await().toList<Saving>()
        val localDocs = financeDao.getAllSavings(uid).first().associateBy { it.id }
        val toInsertLocal = mutableListOf<Saving>()
        
        for (remote in remoteDocs) {
            val local = localDocs[remote.id]
            if (local == null || remote.alterId > local.alterId) {
                toInsertLocal.add(remote.copy(isSynced = true))
            }
        }
        if (toInsertLocal.isNotEmpty()) financeDao.insertAllSavings(toInsertLocal)

        val batch = paths.batch()
        var pushCount = 0
        for (local in localDocs.values) {
            if (!local.isSynced) {
                batch.set(paths.col("savings").document(local.id), local)
                pushCount++
            }
        }
        if (pushCount > 0) {
            batch.commit().await()
            val syncedLocal = localDocs.values.filter { !it.isSynced }.map { it.copy(isSynced = true) }
            financeDao.insertAllSavings(syncedLocal)
        }
    }

    private suspend fun syncInsights(uid: String) {
        val remoteDocs = paths.col("insights").get().await().toList<Insight>()
        val localDocs = insightDao.getAllInsights(uid).first().associateBy { it.id }
        val toInsertLocal = mutableListOf<Insight>()
        
        for (remote in remoteDocs) {
            val local = localDocs[remote.id]
            if (local == null || remote.alterId > local.alterId) {
                toInsertLocal.add(remote.copy(isSynced = true))
            }
        }
        if (toInsertLocal.isNotEmpty()) insightDao.insertInsights(toInsertLocal)

        val batch = paths.batch()
        var pushCount = 0
        for (local in localDocs.values) {
            if (!local.isSynced) {
                batch.set(paths.col("insights").document(local.id), local)
                pushCount++
            }
        }
        if (pushCount > 0) {
            batch.commit().await()
            val syncedLocal = localDocs.values.filter { !it.isSynced }.map { it.copy(isSynced = true) }
            insightDao.insertInsights(syncedLocal)
        }
    }

    private suspend fun syncPrefs(uid: String) {
        val remoteDoc = paths.userDoc().get().await()
        val remotePrefs = remoteDoc.toObject(AppPrefs::class.java)
        val localPrefs = prefsDao.getAppPrefs(uid) ?: AppPrefs()
        
        if (remotePrefs != null) {
            if (remotePrefs.alterId > localPrefs.alterId) {
                prefsDao.insertAppPrefs(remotePrefs.copy(isSynced = true))
                return
            }
        }
        
        if (!localPrefs.isSynced) {
            paths.userDoc().set(localPrefs.copy(isSynced = true)).await()
            prefsDao.insertAppPrefs(localPrefs.copy(isSynced = true))
        }
    }
}
