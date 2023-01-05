package com.arturkowalczyk300.cryptocurrencyprices.Model.Room

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ListOfCryptocurrencyStatValuesWithTimeConverter {
    @TypeConverter
    fun fromListOfCryptocurrencyStatValuesWithTime(list: ListOfCryptocurrencyStatValuesWithTime?): String? {
        if (list == null) return null

        val gson = Gson()
        return gson.toJson(list)
    }

    @TypeConverter
    fun toListOfCryptocurrencyStatValuesWithTime(stringList: String?): ListOfCryptocurrencyStatValuesWithTime? {
        if (stringList == null) return null

        val gson = Gson()
        return gson.fromJson(stringList, ListOfCryptocurrencyStatValuesWithTime::class.java)
    }
}