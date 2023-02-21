package com.arturkowalczyk300.cryptocurrencyprices

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.arturkowalczyk300.cryptocurrencyprices.Model.Room.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runBlockingTest
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
    private lateinit var dao: Dao

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
    fun test_addCryptocurrencyToTop100ByMarketCapTable() {
        val entity1 = CryptocurrencyEntity(
            1,
            "bitcoin",
            "Bitcoin",
            481475235622,
            24883.0,
            Date(1676576168375)
        )

        runBlockingTest {
            dao.addCryptocurrency(entity1)
        }

        val records = dao.getAllCryptocurrencies().getOrAwaitValue()

        assertThat(records).contains(entity1)
    }

    @Test
    fun test_deleteAllCryptocurrencies() {
        val entity1 = CryptocurrencyEntity(
            1,
            "bitcoin",
            "Bitcoin",
            481475235622,
            24883.0,
            Date(1676576168375)
        )

        val entity2 = CryptocurrencyEntity(
            2,
            "ethereum",
            "Ethereum",
            206708478234,
            1711.4,
            Date(1676576168376)
        )

        runBlockingTest {
            dao.addCryptocurrency(entity1)
            dao.addCryptocurrency(entity2)

            dao.deleteAllCryptocurrencies()
        }

        val records = dao.getAllCryptocurrencies().getOrAwaitValue()
        assertThat(records.size).isEqualTo(0)
    }

    //********************************************************************
    // table cryptocurrencies prices (just simple price)
    //********************************************************************
    @Test
    fun test_addCryptocurrencyPrice() {
        val entity1 = PriceEntity(
            71,
            "bitcoin",
            24916.0,
            Date(1676576166499000)
        )

        runBlockingTest {
            dao.addCryptocurrencyPrice(entity1)
        }

        val records = dao.getAllCryptocurrenciesPrices().getOrAwaitValue()
        assertThat(records).contains(entity1)
    }

    @Test
    fun test_deleteAllCryptocurrenciesPrices() {
        val entity1 = PriceEntity(
            71,
            "bitcoin",
            24916.0,
            Date(1676576166499000)
        )

        val entity2 = PriceEntity(
            73,
            "ethereum",
            1698.0699462890625,
            Date(1676579784442000)
        )

        runBlockingTest {
            dao.addCryptocurrencyPrice(entity1)
            dao.addCryptocurrencyPrice(entity2)

            dao.deleteAllCryptocurrenciesPrices()
        }

        val records = dao.getAllCryptocurrenciesPrices().getOrAwaitValue()
        assertThat(records.size).isEqualTo(0)
    }

    @Test
    fun test_deletePricesOfGivenCryptocurrency() {
        val entity1 = PriceEntity(
            71,
            "bitcoin",
            24916.0,
            Date(1676576166499000)
        )

        val entity2 = PriceEntity(
            73,
            "ethereum",
            1698.0699462890625,
            Date(1676579784442000)
        )

        runBlockingTest {
            dao.addCryptocurrencyPrice(entity1)
            dao.addCryptocurrencyPrice(entity2)

            dao.deletePricesOfGivenCryptocurrency("bitcoin")
        }

        val records = dao.getAllCryptocurrenciesPrices().getOrAwaitValue()

        assertThat(records.size).isEqualTo(1)
        assertThat(records).contains(entity2)
    }

    //********************************************************************
    // table cryptocurrencies info in time range (price also)
    //********************************************************************
    @Test
    fun test_addCryptocurrencyInfoWithinTimeRange() {
        val entity1 = InfoWithinTimeRangeEntity(
            74,
            "bitcoin",
            1675969877,
            1676574677,
            7,
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 4.259609882427843E11),
                    ParameterAtTime(1675976496231, 4.2527630728328253E11)
                )
            ),
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 21986.702456169925),
                    ParameterAtTime(1675976496231, 22037.521045261594)
                )
            ),
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 4.555509126494756E10),
                    ParameterAtTime(1675976496231, 4.7403902211568115E10)
                )
            ),
            Date(1676574678059)
        )

        runBlockingTest {
            dao.addCryptocurrencyInfoWithinTimeRange(entity1)
        }

        val records = dao.getAllCryptocurrenciesInfoWithinTimeRange().getOrAwaitValue()

        assertThat(records).contains(entity1)
    }

    @Test
    fun test_getInfoOfCryptocurrencyWithinTimeRange() {
        val entity1 = InfoWithinTimeRangeEntity(
            74,
            "bitcoin",
            1675969877,
            1676574677,
            7,
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 4.259609882427843E11),
                    ParameterAtTime(1675976496231, 4.2527630728328253E11)
                )
            ),
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 21986.702456169925),
                    ParameterAtTime(1675976496231, 22037.521045261594)
                )
            ),
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 4.555509126494756E10),
                    ParameterAtTime(1675976496231, 4.7403902211568115E10)
                )
            ),
            Date(1676574678059)
        )

        val entity2 = InfoWithinTimeRangeEntity(
            90,
            "bitcoin",
            1675969877,
            1676574677,
            31,
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 4.259609882427843E11),
                    ParameterAtTime(1675976496231, 4.2527630728328253E11)
                )
            ),
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 21986.702456169925),
                    ParameterAtTime(1675976496231, 22037.521045261594)
                )
            ),
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 4.555509126494756E10),
                    ParameterAtTime(1675976496231, 4.7403902211568115E10)
                )
            ),
            Date(1676574678059)
        )

        runBlockingTest {
            dao.addCryptocurrencyInfoWithinTimeRange(entity1)
            dao.addCryptocurrencyInfoWithinTimeRange(entity2)
        }

        val records = dao.getInfoOfCryptocurrencyWithinTimeRange("bitcoin", 7).getOrAwaitValue()

        assertThat(records.size).isEqualTo(1)
        assertThat(records).contains(entity1)
    }

    @Test
    fun test_deleteAllCryptocurrenciesInfo() {
        val entity1 = InfoWithinTimeRangeEntity(
            74,
            "bitcoin",
            1675969877,
            1676574677,
            7,
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 4.259609882427843E11),
                    ParameterAtTime(1675976496231, 4.2527630728328253E11)
                )
            ),
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 21986.702456169925),
                    ParameterAtTime(1675976496231, 22037.521045261594)
                )
            ),
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 4.555509126494756E10),
                    ParameterAtTime(1675976496231, 4.7403902211568115E10)
                )
            ),
            Date(1676574678059)
        )

        val entity2 = InfoWithinTimeRangeEntity(
            84,
            "ethereum",
            1676493744,
            1676580144,
            1,
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1676496902081, 2.0093437790654883E11),
                    ParameterAtTime(1676497267887, 2.0045328986612576E11)
                )
            ),
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1676496902081, 1668.1893613176082),
                    ParameterAtTime(1676497267887, 1662.7177629594555)
                )
            ),
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1676496902081, 1.0632193580122942E10),
                    ParameterAtTime(1676497267887, 1.0634439201026014E10)
                )
            ),
            Date(1676574678059)
        )

        runBlockingTest {
            dao.addCryptocurrencyInfoWithinTimeRange(entity1)
            dao.addCryptocurrencyInfoWithinTimeRange(entity2)

            dao.deleteAllCryptocurrenciesInfo()
        }

        val records = dao.getAllCryptocurrenciesInfoWithinTimeRange().getOrAwaitValue()

        assertThat(records.size).isEqualTo(0)
    }

    @Test
    fun test_deleteCryptocurrencyInfoContainsGivenDay() {
        val entity1 = InfoWithinTimeRangeEntity(
            74,
            "bitcoin",
            1675969877,
            1676574677,
            7,
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 4.259609882427843E11),
                    ParameterAtTime(1675976496231, 4.2527630728328253E11)
                )
            ),
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 21986.702456169925),
                    ParameterAtTime(1675976496231, 22037.521045261594)
                )
            ),
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 4.555509126494756E10),
                    ParameterAtTime(1675976496231, 4.7403902211568115E10)
                )
            ),
            Date(1676574678059)
        )

        runBlockingTest {
            dao.addCryptocurrencyInfoWithinTimeRange(entity1)
            dao.deleteCryptocurrencyInfoContainsGivenDay("bitcoin", 1675969877)
        }

        val records = dao.getAllCryptocurrenciesInfoWithinTimeRange().getOrAwaitValue()

        assertThat(records.size).isEqualTo(0)
    }

    @Test
    fun test_deleteAllCryptocurrenciesInfoInGivenDaysCount() {
        val entity1 = InfoWithinTimeRangeEntity(
            74,
            "bitcoin",
            1675969877,
            1676574677,
            7,
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 4.259609882427843E11),
                    ParameterAtTime(1675976496231, 4.2527630728328253E11)
                )
            ),
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 21986.702456169925),
                    ParameterAtTime(1675976496231, 22037.521045261594)
                )
            ),
            ParametersAtTime(
                listOf(
                    ParameterAtTime(1675972951647, 4.555509126494756E10),
                    ParameterAtTime(1675976496231, 4.7403902211568115E10)
                )
            ),
            Date(1676574678059)
        )

        runBlockingTest {
            dao.addCryptocurrencyInfoWithinTimeRange(entity1)
            dao.deleteAllCryptocurrenciesInfoInGivenDaysCount("bitcoin", 7)
        }

        val records = dao.getAllCryptocurrenciesInfoWithinTimeRange().getOrAwaitValue()

        assertThat(records.size).isEqualTo(0)
    }
}