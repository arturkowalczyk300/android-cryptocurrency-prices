package com.arturkowalczyk300.cryptocurrencyprices.model.room

import androidx.room.TypeConverter
import com.google.gson.Gson

class ParametersAtTimeConverterDb {
    @TypeConverter
    fun fromListOfCryptocurrencyStatValuesWithTime(list: ParametersAtTime?): String? {
        if (list == null) return null

        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun toListOfCryptocurrencyStatValuesWithTime(stringList: String?): ParametersAtTime? {
        if (stringList == null) return null

        val gson = Gson()
        return gson.fromJson(stringList, ParametersAtTime::class.java)
    }
}