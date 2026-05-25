package com.example.data.local.converters

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listStringAdapter = moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
    private val mapStringAdapter = moshi.adapter<Map<String, String>>(Types.newParameterizedType(Map::class.java, String::class.java, String::class.java))

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { listStringAdapter.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let { listStringAdapter.fromJson(it) } ?: emptyList()
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        return value?.let { mapStringAdapter.toJson(it) }
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        return value?.let { mapStringAdapter.fromJson(it) } ?: emptyMap()
    }
}
