package com.pocketai.assistant.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import com.pocketai.assistant.data.database.dao.ConversationDao
import com.pocketai.assistant.data.database.dao.MessageDao
import com.pocketai.assistant.data.database.entity.ConversationEntity
import com.pocketai.assistant.data.database.entity.MessageEntity
import com.pocketai.assistant.domain.model.AttachmentRef
import org.json.JSONArray
import org.json.JSONObject

class Converters {
    @TypeConverter
    fun fromAttachmentList(list: List<AttachmentRef>?): String {
        if (list.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("uriString", item.uriString)
            obj.put("fileName", item.fileName)
            obj.put("mimeType", item.mimeType)
            obj.put("sizeBytes", item.sizeBytes)
            obj.put("extractedText", item.extractedText ?: JSONObject.NULL)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toAttachmentList(json: String?): List<AttachmentRef> {
        if (json.isNullOrBlank() || json == "[]") return emptyList()
        val result = mutableListOf<AttachmentRef>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    AttachmentRef(
                        id = obj.optString("id", ""),
                        uriString = obj.optString("uriString", ""),
                        fileName = obj.optString("fileName", ""),
                        mimeType = obj.optString("mimeType", "application/octet-stream"),
                        sizeBytes = obj.optLong("sizeBytes", 0L),
                        extractedText = if (obj.isNull("extractedText")) null else obj.optString("extractedText")
                    )
                )
            }
        } catch (_: Exception) {
            return emptyList()
        }
        return result
    }
}

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}

