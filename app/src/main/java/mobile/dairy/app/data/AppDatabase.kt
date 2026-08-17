package mobile.dairy.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import mobile.dairy.app.domain.AppPrefs
import mobile.dairy.app.domain.DailyRating
import mobile.dairy.app.domain.Expense
import mobile.dairy.app.domain.Goal
import mobile.dairy.app.domain.GoalUpdate
import mobile.dairy.app.domain.Insight
import mobile.dairy.app.domain.JournalEntry
import mobile.dairy.app.domain.Saving

import mobile.dairy.app.domain.LocalUser

@Database(
    entities = [
        JournalEntry::class,
        DailyRating::class,
        Goal::class,
        GoalUpdate::class,
        Expense::class,
        Saving::class,
        Insight::class,
        AppPrefs::class,
        LocalUser::class,
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun goalDao(): GoalDao
    abstract fun financeDao(): FinanceDao
    abstract fun insightDao(): InsightDao
    abstract fun prefsDao(): PrefsDao
    abstract fun localUserDao(): LocalUserDao
}
