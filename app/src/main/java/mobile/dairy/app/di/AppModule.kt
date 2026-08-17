package mobile.dairy.app.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import android.content.Context
import androidx.room.Room
import dagger.hilt.android.qualifiers.ApplicationContext
import mobile.dairy.app.data.AppDatabase
import mobile.dairy.app.data.EntryDao
import mobile.dairy.app.data.GoalDao
import mobile.dairy.app.data.FinanceDao
import mobile.dairy.app.data.InsightDao
import mobile.dairy.app.data.PrefsDao

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    // Offline disk persistence is enabled by default on Android; writes made
    // offline queue locally and sync on reconnect.
    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance("asia-south1") // must match functions/src/index.ts

    @Provides @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "dairy_app_db"
        )
        .fallbackToDestructiveMigration() // Note: Use properly implemented migrations for production
        .build()
    }

    @Provides
    fun provideEntryDao(database: AppDatabase): EntryDao = database.entryDao()

    @Provides
    fun provideGoalDao(database: AppDatabase): GoalDao = database.goalDao()

    @Provides
    fun provideFinanceDao(database: AppDatabase): FinanceDao = database.financeDao()

    @Provides
    fun provideInsightDao(database: AppDatabase): InsightDao = database.insightDao()

    @Provides
    fun providePrefsDao(database: AppDatabase): PrefsDao = database.prefsDao()
}
