package com.example.ecgarrhythmiaclassification.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromString(value: String): List<String> {
        return Gson().fromJson(value, object : TypeToken<List<String>>() {}.type)
    }

    @TypeConverter
    fun fromArrayList(list: List<String>): String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun fromFloatArray(array: FloatArray): String {
        return Gson().toJson(array)
    }

    @TypeConverter
    fun toFloatArray(json: String): FloatArray {
        return Gson().fromJson(json, FloatArray::class.java)
    }
}
