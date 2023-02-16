package com.arturkowalczyk300.cryptocurrencyprices

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.CryptocurrencyPricesDao
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.CryptocurrencyPricesDatabase
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*


@RunWith(AndroidJUnit4::class)
@SmallTest
class DatabaseCryptocurrencyPricesTest {
    private lateinit var database: CryptocurrencyPricesDatabase
    private lateinit var dao: CryptocurrencyPricesDao

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CryptocurrencyPricesDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = database.userDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun test_add_reading() {
        val entity = CryptocurrencyPricesEntityDb(
            id = 5,
            cryptocurrencyId = "BTC",
            date = Date(),
            priceUsd = 21322.0
        )
        dao.addReading(entity)

        val allReadings = dao.getAllReadings().getOrAwaitValue()

        assertThat(allReadings).contains(entity)
    }

    @Test
    fun test_delete_all_readings(){
        val entity1 = CryptocurrencyPricesEntityDb(
            id = 5,
            cryptocurrencyId = "BTC",
            date = Date(),
            priceUsd = 21322.0
        )
        val entity2 = CryptocurrencyPricesEntityDb(
            id = 6,
            cryptocurrencyId = "ETH",
            date = Date(),
            priceUsd = 5322.5
        )
        dao.addReading(entity1)
        dao.addReading(entity2)

        var allReadings = dao.getAllReadings().getOrAwaitValue()
        assertThat(allReadings.size).isEqualTo(2)

        dao.deleteAllReadings()
        allReadings = dao.getAllReadings().getOrAwaitValue()
        assertThat(allReadings.size).isEqualTo(0)
    }
}