package com.arturkowalczyk300.cryptocurrencyprices.Model.Room

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query

@Dao
interface CryptocurrencyPricesDao {
    @Insert(onConflict = IGNORE)
    fun addReading(entity: CryptocurrencyPricesEntityDb)

    @Query("SELECT * FROM main_table ORDER BY date DESC")
    fun getAllReadings(): LiveData<List<CryptocurrencyPricesEntityDb>>

    @Query("DELETE FROM main_table")
    fun deleteAllReadings()
}