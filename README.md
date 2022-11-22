![banner](res/banner.png)

![Operating system](https://img.shields.io/badge/OS-Android%208.0%2B-brightgreen)  ![Main language](https://img.shields.io/badge/Main%20language-Kotlin-blue) 
![Release version](https://img.shields.io/github/v/tag/arturkowalczyk300/android-cryptocurrency-prices?color=darkviolet&label=Release)

# Description
**CryptocurrencyPrices** is application for devices with Android operating system to check historical cryptocurrencies price (in USD).
Main Activity contains Spinner view to select currency (searchable list contains 100 most worth cryptocurrencies - sorted descending by market cap). It also allows to set date range - actual or archival, chosen from date picker.
App contains local database. It is used for caching downloaded records - user can later view them, even without Internet access. Records are sorted in descending order - by date.
There is also chart which shows selected cryptocurrency price changes in time range from date stored in reading record to chosen time earlier (currently app allows to select 1 year, 1 month, 1 week and 24 hours).

# Install
Download and install newest CryptocurrencyPrices*.apk file from **Releases** section (*install from unknown sources* option must be enabled in settings of Android system).

# Demo
<img src="res/demo.gif" alt="Light theme" width="40%" height="40%"> 
