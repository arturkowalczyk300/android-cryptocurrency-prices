﻿![banner](res/banner.png)

![Operating system](https://img.shields.io/badge/OS-Android%208.0%2B-brightgreen)  ![Main language](https://img.shields.io/badge/Main%20language-Kotlin-blue) 
![Release version](https://img.shields.io/github/v/tag/arturkowalczyk300/android-cryptocurrency-prices?color=darkviolet&label=Release)

# Description
**CryptocurrencyPrices** is application for devices with Android operating system to check historical cryptocurrency price (in USD).  

Main Activity contains Spinner view to select currency (searchable list contains 100 most worth cryptocurrencies - sorted descending by market cap). It also allows to set date range - actual or archival, chosen from date picker.  
There is, as well, chart which shows selected cryptocurrency price changes (with calculated percentage value of price trend) in selected time period - app allows to select last 1 year, 1 month, 1 week and 24 hours.  

Recently added feature is handling of price alerts. Click on bell icon causes start of activity containing list of alerts and switch to enable functionality. Button in corner allows to add new record and trash icon - delete it. After enabling, service of application is doing check of all observed prices in every 20 minutes. If condition of alert is fulfilled - notification appears.

App contains local database. It is used for caching downloaded data - it saves data which has been visible on the screen to allow browsing it later, when no Internet connection is available.

# Install
Download and install newest CryptocurrencyPrices*.apk file from **Releases** section (*install from unknown sources* option must be enabled in settings of Android system).

# Todo
I'm planning constant development of this project. In closest future I want to add following features:
- [x] Customizable notifications with possibility to set price alarm
- [ ] Phone's main screen widget with price of favorite currency
- [ ] Support more FIAT currencies (now it's only USD)

# Demo    
**Attached GIF file has big size, it may take a longer while to be loaded.**<br/></br>

<img src="res/demo.gif" alt="Dark theme" width="40%" height="40%"> 
