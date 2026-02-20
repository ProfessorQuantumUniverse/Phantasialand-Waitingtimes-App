# 🎢 Phantasialand Wait Times App

A simple Android app for displaying current wait times for attractions at Phantasialand Theme Park. It helps visitors better plan their day at the park by retrieving live data (where available) and offering various viewing and organization options.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.quantum_prof.phantalandwaittimes/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=com.quantum_prof.phantalandwaittimes)

Or download the latest APK from the [Releases Section](https://github.com/ProfessorQuantumUniverse/Phantasialand-Waitingtimes-App/releases/latest).

> [!CAUTION]
> ## Keep Android Open
> Android FOSS is under threat. From 2026/2027 onward, Google will require developer verification for all Android apps on certified devices, including those installed outside of the Play Store. If you care about the freedom to control your devices and care about the privacy of you data, please contact your representative and make your voice heard. To know about it more go to https://keepandroidopen.org/


## ✨ Features

* **Current Wait Times:** Displays wait times reported by the data source.
* **Attraction Status:** Shows whether an attraction is open, closed, or under maintenance.
* **Offline Caching:** Saves the last successfully retrieved data to display the last known wait times even without an internet connection.
* **Pull-to-Refresh:** Manually refresh the data by pulling down the list.
* **Sorting:** Ability to sort attractions by name (A-Z, Z-A) or by wait time (shortest/longest first).
* **Filtering:** Option to display only the currently open attractions.
* **Favorites:** Mark your favorite attractions with a star. Favorites are saved and remain active even after app restarts.
* **Attraction Icons:** Visually indicate the attraction type (e.g., roller coaster, water ride, show) with appropriate icons.
* **Thematic Design:** A background image creates a park atmosphere.
* **Last Updated:** Displays when the data was last successfully retrieved.
* **Error Handling:** Informs the user of any problems with the data retrieval.

## 🛠️ Technology Stack

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose
* **Architecture:** MVVM (Model-View-ViewModel)
* **Asynchrony:** Kotlin Coroutines & StateFlow
* **Dependency Injection:** Hilt
* **Network:** (Probably Retrofit or Ktor - please specify which one you use)
* **Data Storage (Cache/Favorites):** SharedPreferences
* **UI Components:**
* Material 3 Design Components
* Accompanist SwipeRefresh for pull-to-refresh

## 🚀 Future Ideas / Possible Extensions

* Integration of a park map with wait times displayed directly at the attraction locations.
* Push notifications for favorites when their wait time falls below a certain value.
* Display of show times and other park information.
* More detailed information about attractions (e.g., minimum size, type, description).
* Transitioning the cache to a more robust solution such as Room Database.
* Widgets for the Android home screen.
* Improved accessibility.
