package mobile.dairy.app.services

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mobile.dairy.app.domain.PersonRef
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "bloom_local")

/**
 * Local mirror of notification settings + the check-in draft.
 *
 * Why a mirror: AlarmManager receivers fire without a signed-in Firestore
 * context, so the pieces they need (times, categories, private mode) live in
 * DataStore and are refreshed whenever the user changes settings. The diary
 * data itself stays in Firestore only.
 */
@Serializable
data class ReminderSettings(
    val enabled: Boolean = true,
    val privateMode: Boolean = false,
    val quietStart: Int? = 22,
    val quietEnd: Int? = 7,
    /** category name -> hour (0-23); legacy */
    val times: Map<String, Int> = mapOf(
        "MORNING_PLANNING" to 8,
        "EVENING_CHECKIN" to 21,
        "EXPENSE_REMINDER" to 20,
    ),
    /** category name -> minute of day (0 - 1439). Overrides times if present */
    val timeMinutes: Map<String, Int> = emptyMap(),
    /** categories that have been explicitly disabled by the user */
    val disabledCategories: Set<String> = emptySet(),
)

@Serializable
data class CheckinDraft(
    val date: String = "",
    val moods: List<String> = emptyList(),
    val moodCauses: List<String> = emptyList(),
    val bestPart: String = "",
    val hardestPart: String = "",
    val goodThings: String = "",
    val mistakes: String = "",
    val lessons: String = "",
    val gratitude: List<String> = emptyList(),
    val people: List<PersonRef> = emptyList(),
    val goodDecision: String = "",
    val improvement: String = "",
    val note: String = "",
    val overall: Int? = 5,
    val happiness: Int? = 5,
    val energy: Int? = 5,
    val productivity: Int? = 5,
    val focus: Int? = 5,
    val discipline: Int? = 5,
    val goalEffort: Int? = 5,
    val stress: Int? = 5,
    val sleep: Int? = 5,
    val financialDiscipline: Int? = 5,
    val spent: String = "",
    val necessary: Boolean = true,
    val earned: String = "",
    val avoided: String = "",
    val goalProgress: Map<String, Int> = emptyMap(),
    val goalMinutes: Map<String, Int> = emptyMap(),
    val customAnswers: Map<String, String> = emptyMap(),
)

@Singleton
class LocalPrefs @Inject constructor(@ApplicationContext private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val reminderKey = stringPreferencesKey("reminder_settings")
    private val draftKey = stringPreferencesKey("checkin_draft")
    private val onboardedKey = booleanPreferencesKey("onboarded")

    val reminderSettings: Flow<ReminderSettings> = context.dataStore.data.map { p ->
        p[reminderKey]?.let { runCatching { json.decodeFromString<ReminderSettings>(it) }.getOrNull() }
            ?: ReminderSettings()
    }

    suspend fun reminderSettingsNow(): ReminderSettings = reminderSettings.first()

    suspend fun saveReminderSettings(settings: ReminderSettings) {
        context.dataStore.edit { it[reminderKey] = json.encodeToString(settings) }
    }

    val draft: Flow<CheckinDraft?> = context.dataStore.data.map { p ->
        p[draftKey]?.let { runCatching { json.decodeFromString<CheckinDraft>(it) }.getOrNull() }
    }

    suspend fun saveDraft(draft: CheckinDraft) {
        context.dataStore.edit { it[draftKey] = json.encodeToString(draft) }
    }

    suspend fun clearDraft() {
        context.dataStore.edit { it.remove(draftKey) }
    }

    val onboarded: Flow<Boolean> = context.dataStore.data.map { it[onboardedKey] ?: false }

    suspend fun setOnboarded() {
        context.dataStore.edit { it[onboardedKey] = true }
    }
}
