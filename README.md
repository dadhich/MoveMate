# Move Mate

## Overview

Move Mate is an Android application designed to help users plan their journeys using public transport in Sydney, NSW, Australia. The app integrates with the Transport for NSW (TfNSW) Open Data APIs to provide trip suggestions, real-time departure information, and location-based services. Key features include planning trips with custom or current locations, date/time selection for departures or arrivals, autocomplete for addresses and stops, and the ability to save favourite trips.

## Features Implemented

* **Trip Planning:**
    * Users can input an origin and a destination.
    * Origin can be the user's current location (requires location permission) or a custom address/stop.
    * Destination is entered as an address/stop.
    * The app calls the TfNSW Trip Planner API to fetch journey options.
* **Real-time Data:**
    * Trip options display real-time departure/arrival information where available.
    * The display logic prioritises estimated times from the API.
* **Location Services:**
    * Fetches the device's current location for use as a trip origin.
    * Handles location permission requests.
    * Checks if device location services are enabled and prompts the user if not.
* **Address/Stop Autocomplete:**
    * Uses the TfNSW API to provide autocomplete suggestions for origin and destination input fields.
    * Includes debouncing and minimum character thresholds for performance.
    * Uses selected suggestion IDs and types for more precise trip planning.
* **Date & Time Selection:**
    * Users can specify whether the trip is for now or later.
* **Route Options Display:**
    * Displays a list of trip options with key details:
        * Time until the first public transport service departs.
        * Primary public transport mode and route number.
        * Estimated departure time and stop name for the first public transport leg.
        * Scheduled departure time and real-time status (e.g., "X min late") for the first PT leg.
        * Overall journey arrival time and final destination name.
        * Summary of all transport modes used in the journey.
* **Detailed Trip View:**
    * Users can tap on a trip option to view a detailed breakdown of all its legs (walk, bus, train, etc.).
    * Each leg shows mode, origin/destination names and times, duration, and real-time status if applicable.
* **Save Last Query:**
    * The last successfully planned origin and destination (excluding date/time) are saved and pre-filled when the app next starts.
* **Favorites System:**
    * Users can save their current origin/destination query as a "Favourite" with a custom name.
    * Users can view their list of saved favourites.
    * Users can select a favourite to quickly populate the origin and destination fields.
    * Users can delete their saved favorites.

## Key Technologies & Libraries Used

* **Kotlin:** Primary programming language.
* **Android SDK:** For building native Android applications.
* **View Binding:** To easily interact with XML layouts.
* **Material Design Components:** For UI elements.
* **ConstraintLayout:** For flexible and responsive layouts.
* **RecyclerView:** For displaying lists of trip options and trip legs.
* **LiveData (Android Jetpack):** For observable data patterns.
* **Retrofit:** For type-safe HTTP client and API communication.
* **Gson:** For JSON serialization and deserialization.
* **OkHttp Logging Interceptor:** For debugging network requests.
* **Kotlin Coroutines:** For managing asynchronous operations.
* **Google Play Services Location:** For fetching device location.
* **SharedPreferences:** For storing user preferences (last query, favorites).
* **Git & GitHub:** For version control.

## Future Enhancements (Potential Iterations)

* **Loading Indicators:** Implement more sophisticated loading indicators during API calls.
* **Error Screens:** Improve full-screen error messages for API failures or no results.
* **Map Integration:** Display trip routes on a map.
* **Settings Screen:** Allow users to configure preferences (e.g., preferred modes).
* **GTFS-realtime Vehicle Positions:** Potentially integrate direct vehicle position feeds for even more precise real-time updates for imminent services.
* **Database for Favourites:** For a more robust favourites system with more data, consider using Room persistence library.


