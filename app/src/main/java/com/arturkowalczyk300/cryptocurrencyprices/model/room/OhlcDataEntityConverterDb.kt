package com.arturkowalczyk300.cryptocurrencyprices.model.room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class OhlcDataEntityConverterDb {
    @TypeConverter
    fun fromListOfOhlcChartDataEntities(list: List<OhlcDataEntity>?): String? {
        if (list == null) return null

        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun toListOfOhlcChartDataEntities(stringList: String?): List<OhlcDataEntity>? {
        if (stringList == null) return null

        val listType = object: TypeToken<ArrayList<OhlcDataEntity>>(){}.type

        val gson = Gson()
        return gson.fromJson(stringList, listType)
    }
}