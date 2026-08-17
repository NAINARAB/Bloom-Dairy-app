package mobile.dairy.app.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import mobile.dairy.app.domain.AppUsage
import mobile.dairy.app.domain.CategoryDef
import mobile.dairy.app.domain.GoalTask
import mobile.dairy.app.domain.JournalQuestionDef
import mobile.dairy.app.domain.Milestone
import mobile.dairy.app.domain.PersonRef

class RoomConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let { 
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, type)
        }
    }

    @TypeConverter
    fun fromPersonRefList(value: List<PersonRef>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toPersonRefList(value: String?): List<PersonRef>? {
        return value?.let { 
            val type = object : TypeToken<List<PersonRef>>() {}.type
            gson.fromJson(it, type)
        }
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        return value?.let { 
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(it, type)
        }
    }

    @TypeConverter
    fun fromStringIntMap(value: Map<String, Int>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringIntMap(value: String?): Map<String, Int>? {
        return value?.let { 
            val type = object : TypeToken<Map<String, Int>>() {}.type
            gson.fromJson(it, type)
        }
    }

    @TypeConverter
    fun fromMilestoneList(value: List<Milestone>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toMilestoneList(value: String?): List<Milestone>? {
        return value?.let { 
            val type = object : TypeToken<List<Milestone>>() {}.type
            gson.fromJson(it, type)
        }
    }

    @TypeConverter
    fun fromGoalTaskList(value: List<GoalTask>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toGoalTaskList(value: String?): List<GoalTask>? {
        return value?.let { 
            val type = object : TypeToken<List<GoalTask>>() {}.type
            gson.fromJson(it, type)
        }
    }

    @TypeConverter
    fun fromAppUsageList(value: List<AppUsage>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toAppUsageList(value: String?): List<AppUsage>? {
        return value?.let { 
            val type = object : TypeToken<List<AppUsage>>() {}.type
            gson.fromJson(it, type)
        }
    }

    @TypeConverter
    fun fromCategoryDefList(value: List<CategoryDef>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toCategoryDefList(value: String?): List<CategoryDef>? {
        return value?.let { 
            val type = object : TypeToken<List<CategoryDef>>() {}.type
            gson.fromJson(it, type)
        }
    }

    @TypeConverter
    fun fromJournalQuestionDefList(value: List<JournalQuestionDef>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toJournalQuestionDefList(value: String?): List<JournalQuestionDef>? {
        return value?.let { 
            val type = object : TypeToken<List<JournalQuestionDef>>() {}.type
            gson.fromJson(it, type)
        }
    }
}
