package com.arturkowalczyk300.cryptocurrencyprices.model.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class CandlestickEntityConverterDb {
    @TypeConverter
    fun fromListOfCandlestickEntities(list: List<CandlestickEntity>?): String? {
        if (list == null) return null

        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun toListOfCandlestickEntities(stringList: String?): List<CandlestickEntity>? {
        if (stringList == null) return null

        val listType = object: TypeToken<ArrayList<CandlestickEntity>>(){}.type

        val gson = Gson()
        return gson.fromJson(stringList, listType)
    }
}