package com.abhishekdadhich.movemate

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent // START: CHANGING FOR ROUTE OPTIONS CARD CLICKING
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson // START: CHANGING FOR ROUTE OPTIONS CARD CLICKING
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private var selectedYear: Int = -1
    private var selectedMonth: Int = -1
    private var selectedDayOfMonth: Int = -1
    private var selectedHour: Int = -1
    private var selectedMinute: Int = -1

    private lateinit var autoCompleteTextViewFrom: AutoCompleteTextView
    private lateinit var autoCompleteTextViewTo: AutoCompleteTextView
    private lateinit var buttonFindRoutes: MaterialButton
    private lateinit var scrollViewAvailableRoutes: NestedScrollView
    private lateinit var recyclerViewRoutes: RecyclerView
    private lateinit var routeAdapter: RouteAdapter

    private lateinit var fromSuggestionsAdapter: ArrayAdapter<String>
    private var currentFromSuggestions: List<StopFinderLocation> = emptyList()
    private var selectedOriginStopId: String? = null
    private val debounceHandlerFrom = Handler(Looper.getMainLooper())
    private var searchRunnableFrom: Runnable? = null
    private var isProgrammaticTextChangeFrom = false
    private var isProcessingFromClick = false

    private lateinit var toSuggestionsAdapter: ArrayAdapter<String>
    private var currentToSuggestions: List<StopFinderLocation> = emptyList()
    private var selectedDestinationStopId: String? = null
    private val debounceHandlerTo = Handler(Looper.getMainLooper())
    private var searchRunnableTo: Runnable? = null
    private var isProgrammaticTextChangeTo = false
    private var isProcessingToClick = false

    private var isUsingDeviceLocationForOrigin = false
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    private val DEBOUNCE_DELAY_MS = 500L
    private val MIN_CHAR_TRIGGER_AUTOCOMPLETE = 3

    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val displayTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    // START: CHANGING FOR ROUTE OPTIONS CARD CLICKING
    private var lastFetchedApiJourneys: List<Journey> = emptyList()
    // END: CHANGING FOR ROUTE OPTIONS CARD CLICKING

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.d("TripPlannerApp", "Location permission granted by user.")
                fetchCurrentLocationAndUpdateState()
                isUsingDeviceLocationForOrigin = true
            } else {
                Log.d("TripPlannerApp", "Location permission denied by user.")
                isUsingDeviceLocationForOrigin = false
                Toast.makeText(
                    this,
                    "Location permission denied. Please enter origin.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getUIControls()
        initialiseRouteControls() // This function will be modified below

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkAndRequestLocationPermission()

        setupFromAutocomplete()
        setupToAutocomplete()

        autoCompleteTextViewTo.requestFocus()

        setButtons()
    }

    private fun getUIControls() {
        autoCompleteTextViewFrom = findViewById(R.id.autoCompleteTextViewFrom)
        autoCompleteTextViewTo = findViewById(R.id.autoCompleteTextViewTo)
        buttonFindRoutes = findViewById(R.id.buttonFindRoutes)
        scrollViewAvailableRoutes = findViewById(R.id.scrollViewAvailableRoutes)
        recyclerViewRoutes = findViewById(R.id.recyclerViewRoutes)
    }

    private fun initialiseRouteControls() {
        // START: CHANGING FOR ROUTE OPTIONS CARD CLICKING
        // This is where RouteAdapter is initialized. We pass the click listener lambda.
        routeAdapter = RouteAdapter(emptyList()) { clickedPosition ->
            // This lambda will be called when an item in RouteAdapter is clicked
            if (clickedPosition >= 0 && clickedPosition < lastFetchedApiJourneys.size) {
                val selectedJourney = lastFetchedApiJourneys[clickedPosition]

                Log.d("MainActivity", "Route card clicked at position: $clickedPosition. Launching details.")

                val intent = Intent(this@MainActivity, TripDetailsActivity::class.java)
                try {
                    val journeyJson = Gson().toJson(selectedJourney)
                    intent.putExtra("SELECTED_JOURNEY_JSON", journeyJson)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("TripPlannerApp", "Error serializing Journey to JSON for TripDetails", e)
                    Toast.makeText(this, "Error preparing trip details.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("TripPlannerApp", "Clicked position $clickedPosition is out of bounds for lastFetchedApiJourneys size ${lastFetchedApiJourneys.size}")
                Toast.makeText(this, "Could not load details for this route.", Toast.LENGTH_SHORT).show()
            }
        }
        // END: CHANGING FOR ROUTE OPTIONS CARD CLICKING

        recyclerViewRoutes.layoutManager = LinearLayoutManager(this)
        recyclerViewRoutes.adapter = routeAdapter
        recyclerViewRoutes.isNestedScrollingEnabled = false
    }

    private fun setButtons() {

        // set the date button -- default to today
        val buttonToday = findViewById<MaterialButton>(R.id.buttonToday)
        buttonToday.setOnClickListener { showDatePicker(buttonToday) }
        val initialCalendar = Calendar.getInstance(); selectedYear =
            initialCalendar.get(Calendar.YEAR)
        selectedMonth = initialCalendar.get(Calendar.MONTH); selectedDayOfMonth =
            initialCalendar.get(Calendar.DAY_OF_MONTH)

        // set the time button -- default to now
        val buttonNow = findViewById<MaterialButton>(R.id.buttonNow)
        buttonNow.setOnClickListener { showTimePicker(buttonNow) }
        selectedHour = initialCalendar.get(Calendar.HOUR_OF_DAY); selectedMinute =
            initialCalendar.get(Calendar.MINUTE)

        // set the find routes button
        buttonFindRoutes.setOnClickListener {
            // START: CHANGING FOR ROUTE OPTIONS CARD CLICKING (Keyboard hide moved here from your file)
            hideKeyboard()
            // END: CHANGING FOR ROUTE OPTIONS CARD CLICKING
            lifecycleScope.launch {
                handleFindRoutesClick()
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // Using currentFocus is more reliable than a fixed view ID like autoCompleteTextViewTo.windowToken
        var view = currentFocus
        if (view == null) { // If no view has focus, create a new one to get a window token
            view = View(this)
        }
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        // Optionally clear focus from the text fields too
        autoCompleteTextViewFrom.clearFocus()
        autoCompleteTextViewTo.clearFocus()
        findViewById<View>(R.id.main_activity_root_layout)?.requestFocus() // Request focus on root
    }


    private fun checkAndRequestLocationPermission() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission needed.", Toast.LENGTH_LONG).show()
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        fetchCurrentLocationAndUpdateState()
    }

    @SuppressLint("MissingPermission")
    private fun fetchCurrentLocationAndUpdateState() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("TripPlannerApp", "No permission to fetch current location. Trying last known (which also needs permission).")
            isUsingDeviceLocationForOrigin = false
            fetchLastKnownLocationFallback()
            return
        }
        Log.d("TripPlannerApp", "Fetching current location...")
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLatitude = location.latitude; currentLongitude = location.longitude
                    isUsingDeviceLocationForOrigin = true

                    // TODO: AD: Check this
                    //  selectedOriginStopId = null

                    Log.i("TripPlannerApp", "Got location: Lat: $currentLatitude, Lon: $currentLongitude")
                } else {
                    Log.w("TripPlannerApp", "getCurrentLocation returned null. Trying last known.")
                    fetchLastKnownLocationFallback()
                }
            }
            .addOnFailureListener { e ->
                Log.e("TripPlannerApp", "Failed getCurrentLocation: ${e.message}. Trying last known.", e)
                fetchLastKnownLocationFallback()
            }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLastKnownLocationFallback() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLatitude = location.latitude; currentLongitude = location.longitude
                    isUsingDeviceLocationForOrigin = true

                    // TODO: AD - check this
                    // selectedOriginStopId = null

                    Log.i("TripPlannerApp", "Last known location obtained: Lat: $currentLatitude, Lon: $currentLongitude")
                } else {
                    Log.w("TripPlannerApp", "Last known location also null.")
                    isUsingDeviceLocationForOrigin = false
                    Toast.makeText(this, "Could not get current location. Please enter origin.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                isUsingDeviceLocationForOrigin = false
                Log.e("TripPlannerApp", "Error fetching last known location.", e)
                Toast.makeText(this, "Error fetching location. Please enter origin.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupFromAutocomplete() {
        fromSuggestionsAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line)
        autoCompleteTextViewFrom.setAdapter(fromSuggestionsAdapter)
        autoCompleteTextViewFrom.threshold = MIN_CHAR_TRIGGER_AUTOCOMPLETE

        autoCompleteTextViewFrom.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isProgrammaticTextChangeFrom || isProcessingFromClick)
                    return
                selectedOriginStopId = null
                searchRunnableFrom?.let { debounceHandlerFrom.removeCallbacks(it) }
            }

            override fun afterTextChanged(s: Editable?) {
                if (isProgrammaticTextChangeFrom || isProcessingFromClick)
                    return

                val query = s.toString().trim()

                if (query.length >= MIN_CHAR_TRIGGER_AUTOCOMPLETE) {
                    searchRunnableFrom = Runnable { fetchStopSuggestions(query, true) }
                    debounceHandlerFrom.postDelayed(searchRunnableFrom!!, DEBOUNCE_DELAY_MS)
                }
            }
        })

        autoCompleteTextViewFrom.setOnItemClickListener { parent, _, position, _ ->
            isProcessingFromClick = true
            val selectedString = fromSuggestionsAdapter.getItem(position)
            val actualSelectedLocation = currentFromSuggestions.find {
                val displayName = it.name ?: it.disassembledName ?: ""
                displayName == selectedString
            }

            if (actualSelectedLocation != null) {
                selectedOriginStopId = actualSelectedLocation.id
                isProgrammaticTextChangeFrom = true
                autoCompleteTextViewFrom.setText(
                    actualSelectedLocation.name ?: actualSelectedLocation.disassembledName ?: "", false
                )
                autoCompleteTextViewFrom.setSelection(autoCompleteTextViewFrom.text.length)
                autoCompleteTextViewFrom.dismissDropDown()
                Log.d("TripPlannerApp", "Origin selected: ${actualSelectedLocation.name}, ID: $selectedOriginStopId")
            } else {
                Log.e("TripPlannerApp", "From: Could not find selected object for string: $selectedString")
                selectedOriginStopId = null // Ensure ID is null
            }
            buttonFindRoutes.requestFocus() // Explicitly move focus to "Find Routes" button
            resetProcessingFlags()
        }
    }

    private fun setupToAutocomplete() {
        toSuggestionsAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line)
        autoCompleteTextViewTo.setAdapter(toSuggestionsAdapter)
        autoCompleteTextViewTo.threshold = MIN_CHAR_TRIGGER_AUTOCOMPLETE

        autoCompleteTextViewTo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isProgrammaticTextChangeTo || isProcessingToClick)
                    return
                selectedDestinationStopId = null
                searchRunnableTo?.let { debounceHandlerTo.removeCallbacks(it) }
            }

            override fun afterTextChanged(s: Editable?) {
                if (isProgrammaticTextChangeTo || isProcessingToClick)
                    return
                val query = s.toString().trim()
                if (query.length >= MIN_CHAR_TRIGGER_AUTOCOMPLETE) {
                    searchRunnableTo = Runnable { fetchStopSuggestions(query, false) }
                    debounceHandlerTo.postDelayed(searchRunnableTo!!, DEBOUNCE_DELAY_MS)
                }
            }
        })

        autoCompleteTextViewTo.setOnItemClickListener { parent, _, position, _ ->
            isProcessingToClick = true
            val selectedString = toSuggestionsAdapter.getItem(position)
            val actualSelectedLocation = currentToSuggestions.find {
                val displayName = it.name ?: it.disassembledName ?: ""
                displayName == selectedString
            }

            if (actualSelectedLocation != null) {
                selectedDestinationStopId = actualSelectedLocation.id
                isProgrammaticTextChangeTo = true
                autoCompleteTextViewTo.setText(
                    actualSelectedLocation.name ?: actualSelectedLocation.disassembledName ?: "",
                    false
                )
                autoCompleteTextViewTo.dismissDropDown()
                Log.d("TripPlannerApp", "Destination selected: ${actualSelectedLocation.name}, ID: $selectedDestinationStopId")
            } else {
                Log.e("TripPlannerApp", "To: Could not find selected object for string: $selectedString")
                selectedDestinationStopId = null
            }
            buttonFindRoutes.requestFocus() // Explicitly move focus to "Find Routes" button
            resetProcessingFlags()
        }
    }

    private fun fetchStopSuggestions(query: String, isForOriginField: Boolean) {
        lifecycleScope.launch {
            Log.d(
                "TripPlannerApp",
                "Fetching suggestions for: '$query' (isForOrigin: $isForOriginField)"
            )
            try {
                val apiKeyHeader = "apikey ${BuildConfig.TFNW_API_KEY}"
                val response = RetrofitClient.instance.findStops(
                    apiKey = apiKeyHeader,
                    searchTerm = query
                )

                val adapterToUpdate: ArrayAdapter<String>
                val currentSuggestionsListRef: (List<StopFinderLocation>) -> Unit
                val autoCompleteTextViewToShowDropDown: AutoCompleteTextView

                if (isForOriginField) {
                    adapterToUpdate = fromSuggestionsAdapter
                    currentSuggestionsListRef = { list -> currentFromSuggestions = list }
                    autoCompleteTextViewToShowDropDown = autoCompleteTextViewFrom
                } else {
                    adapterToUpdate = toSuggestionsAdapter
                    currentSuggestionsListRef = { list -> currentToSuggestions = list }
                    autoCompleteTextViewToShowDropDown = autoCompleteTextViewTo
                }

                if (response.isSuccessful) {
                    val stopFinderResponse = response.body()
                    val locations = stopFinderResponse?.locations?.filterNotNull() ?: emptyList()

                    val sortedLocations = locations.sortedWith(
                        compareByDescending<StopFinderLocation> { it.isBest == true }
                            .thenByDescending { it.matchQuality }
                    )
                    currentSuggestionsListRef(sortedLocations)

                    val suggestionStrings = sortedLocations.map { location ->
                        location.name ?: location.disassembledName ?: "Unknown location"
                    }

                    adapterToUpdate.clear()
                    adapterToUpdate.addAll(suggestionStrings)

                    Log.d("TripPlannerApp", "Suggestions loaded: ${suggestionStrings.size} for query '$query'")

                } else {
                    Log.e("TripPlannerApp", "StopFinder API Error for query '$query': ${response.code()} - ${
                            response.errorBody()?.string()
                        }"
                    )
                    adapterToUpdate.clear()
                }
            } catch (e: Exception) {
                Log.e(
                    "TripPlannerApp",
                    "StopFinder Network Exception for query '$query': ${e.message}",
                    e
                )
                if (isForOriginField) fromSuggestionsAdapter.clear() else toSuggestionsAdapter.clear()
            }
        }
    }


    private suspend fun handleFindRoutesClick() {

        // find the origin (it will be either current location or a valid stop, else we won't proceed)
        var nameOriginApi: String?
        var typeOriginApi = "any"

        if(autoCompleteTextViewFrom.text.trim().isNotBlank()) {
            // user has typed something, so need to use stop id and not location
            if (selectedOriginStopId != null) {
                nameOriginApi = selectedOriginStopId
                typeOriginApi = "any"
                Log.i("TripPlannerApp", "Using Selected Stop ID for Origin: $nameOriginApi")
            }
            else {
                // user has typed something, but there is no corresponding stop id for it, thus it is invalid location, so can't proceed with API call
                Log.i("TripPlannerApp", "User has typed ${autoCompleteTextViewFrom.text} in 'To', but the stop-id is null, so can't proceed with API call to find routes")
                Toast.makeText(this, "Please select a valid starting point from suggestions.",  Toast.LENGTH_LONG).show()
                autoCompleteTextViewFrom.requestFocus()
                return // Stop API call
            }
        }
        else {
            // user has not typed anything, so need to use current location
            if (currentLatitude != null && currentLongitude != null) {
                nameOriginApi = String.format(Locale.US, "%.6f:%.6f:EPSG:4326", currentLongitude, currentLatitude)
                typeOriginApi = "coord"
                Log.i("TripPlannerApp", "Using Current GPS Location for Origin: $nameOriginApi")
            }
            else {
                // user has not typed anything, and the location coords are blank so can't proceed with API call
                Log.i("TripPlannerApp", "User has not typed anything in 'From', and the location coords are blank so can't proceed with API call to find routes")
                Toast.makeText(this, "Please type the starting point of trip (current location is not available)",  Toast.LENGTH_LONG).show()
                autoCompleteTextViewFrom.requestFocus()
                return // Stop API call
            }
        }

        // find the destination -- it needs to be a valid stop, else we won't proceed
        var nameDestinationApi = selectedDestinationStopId
        val typeDestinationApi = "any"

        if (nameDestinationApi == null) {
            // stop-id is null for destination
            if (autoCompleteTextViewTo.text.trim().isNotBlank()) {
                Toast.makeText(this, "Please select a valid destination from suggestions.", Toast.LENGTH_LONG).show()
                Log.i("TripPlannerApp", "User entered ${autoCompleteTextViewTo.text}, but stop-id is null, thus invalid stop in 'To', so can't proceed with API call to find routes.")
                autoCompleteTextViewTo.requestFocus()
                return
            }
            Log.w("TripPlannerApp", "Destination not selected. 'To' is blank.")
            Toast.makeText(this, "Please select a destination", Toast.LENGTH_LONG).show()
            autoCompleteTextViewTo.requestFocus()
            return
        }

        // when is the travel (default is today and now)
        val dateForApi = getSelectedDate()
        val timeForApi = getSelectedTime()

        Log.d(
            "TripPlannerApp",
            "Requesting trip: From: $nameOriginApi (type:$typeOriginApi), To: $nameDestinationApi (type:$typeDestinationApi), Date: $dateForApi, Time: $timeForApi"
        )
        Toast.makeText(this, "Fetching routes...", Toast.LENGTH_SHORT).show()
        scrollViewAvailableRoutes.visibility = View.GONE
        routeAdapter.updateRoutes(emptyList())

        try {
            val apiKeyHeader = "apikey ${BuildConfig.TFNW_API_KEY}"
            val response = RetrofitClient.instance.getTrip(
                apiKey = apiKeyHeader,
                depArrMacro = "dep",
                itdDate = dateForApi,
                itdTime = timeForApi,
                typeOrigin = typeOriginApi,
                nameOrigin = nameOriginApi!!,
                typeDestination = typeDestinationApi,
                nameDestination = nameDestinationApi!!
            )

            if (response.isSuccessful) {
                val tripResponse = response.body()
                if (tripResponse != null && tripResponse.error == null && tripResponse.journeys != null) {

                    // START: CHANGING FOR ROUTE OPTIONS CARD CLICKING
                    lastFetchedApiJourneys = tripResponse.journeys // Store the raw journeys
                    // END: CHANGING FOR ROUTE OPTIONS CARD CLICKING

                    val appRoutes = mapApiJourneysToAppRoutes(tripResponse.journeys)
                    routeAdapter.updateRoutes(appRoutes)
                    if (appRoutes.isNotEmpty()) {
                        scrollViewAvailableRoutes.visibility = View.VISIBLE
                    } else {
                        Toast.makeText(
                            this,
                            "No journeys found for the selection.",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.i("TripPlannerApp", "No journeys found for the selection.")
                    }
                } else {
                    val errorMessage =
                        tripResponse?.error?.message ?: "API returned no journeys or an error"
                    Log.e("TripPlannerApp", "API Error: $errorMessage")
                    Toast.makeText(this, "API Error: $errorMessage", Toast.LENGTH_LONG).show()
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("TripPlannerApp", "API Call Failed: ${response.code()} - $errorBody")
                Toast.makeText(
                    this,
                    "Error: ${response.code()} - Failed to fetch routes.",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            Log.e("TripPlannerApp", "Network Exception: ${e.message}", e)
            Toast.makeText(this, "Network error: Please check connection.", Toast.LENGTH_LONG)
                .show()
        }

        resetAllFlags()
    }

    private fun resetAllFlags() {
        selectedOriginStopId = null
        resetProcessingFlags()
    }

    private fun resetProcessingFlags() {
        isProcessingFromClick = false
        isProcessingToClick = false
        isProgrammaticTextChangeTo = false
        isProgrammaticTextChangeFrom = false
    }

    private fun getSelectedTime(): String {
        val timeForApi = SimpleDateFormat("HHmm", Locale.getDefault()).apply {
            val cal = Calendar.getInstance(); if (selectedHour != -1) cal.set(
            Calendar.HOUR_OF_DAY,
            selectedHour
        ); if (selectedMinute != -1) cal.set(Calendar.MINUTE, selectedMinute)
            timeZone = TimeZone.getTimeZone("Australia/Sydney")
        }.format(
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, selectedHour); set(
                Calendar.MINUTE,
                selectedMinute
            )
            }.time
        )
        return timeForApi
    }

    private fun getSelectedDate(): String {
        val dateForApi = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).apply {
            val cal = Calendar.getInstance(); if (selectedYear != -1) cal.set(
            selectedYear,
            selectedMonth,
            selectedDayOfMonth
        )
            timeZone = TimeZone.getTimeZone("Australia/Sydney")
        }.format(
            Calendar.getInstance()
                .apply { set(selectedYear, selectedMonth, selectedDayOfMonth) }.time
        )
        return dateForApi
    }

    private fun parseApiDateTimeStringToMillis(dateTimeString: String?): Long? {
        if (dateTimeString == null) return null
        return try {
            apiDateFormat.parse(dateTimeString)?.time
        } catch (e: ParseException) {
            Log.e("TripPlannerApp", "Error parsing date: $dateTimeString", e); null
        }
    }

    private fun formatTimeMillisToDisplay(timeMillis: Long?): String {
        if (timeMillis == null) return "N/A"
        val localCalendar = Calendar.getInstance(); localCalendar.timeInMillis = timeMillis
        return displayTimeFormat.format(localCalendar.time)
    }

    private fun mapApiJourneysToAppRoutes(apiJourneys: List<Journey>): List<Route> {
        val appRoutes = mutableListOf<Route>()
        apiJourneys.forEachIndexed { index, apiJourney ->
            if (apiJourney.legs.isNullOrEmpty()) {
                return@forEachIndexed
            }

            var firstVehicleLeg: JourneyLeg? = null
            for (leg in apiJourney.legs) {
                val productClass = leg.transportation?.product?.classId
                if (productClass != null && productClass != 99 && productClass != 100) {
                    firstVehicleLeg = leg; break
                }
            }
            val effectiveFirstLegForDepartureInfo = firstVehicleLeg ?: apiJourney.legs.first()
            val lastLeg = apiJourney.legs.last()

            val firstVehicleScheduledDepartureMillis =
                parseApiDateTimeStringToMillis(effectiveFirstLegForDepartureInfo.origin?.departureTimePlanned)
            val firstVehicleEstimatedDepartureMillis =
                parseApiDateTimeStringToMillis(effectiveFirstLegForDepartureInfo.origin?.departureTimeEstimated)
            val firstVehicleActualDepartureMillis =
                firstVehicleEstimatedDepartureMillis ?: firstVehicleScheduledDepartureMillis
            val overallJourneyETAMillis = parseApiDateTimeStringToMillis(
                lastLeg.destination?.arrivalTimeEstimated ?: lastLeg.destination?.arrivalTimePlanned
            )

            val transportTags = mutableListOf<TransportTag>()
            apiJourney.legs.forEach { leg ->
                leg.transportation?.let { trans ->
                    val iconRes: Int;
                    var tagName = "Unknown"
                    when (trans.product?.classId) {
                        1 -> {
                            iconRes = R.drawable.ic_train_alt_16dp; tagName = "Train"
                        }

                        2 -> {
                            iconRes = R.drawable.ic_metro_24dp; tagName = "Metro"
                        }

                        4 -> {
                            iconRes = R.drawable.ic_train_alt_16dp; tagName = "Light Rail"
                        }

                        5 -> {
                            iconRes = R.drawable.ic_bus_alt_16dp; tagName =
                                if (!trans.number.isNullOrBlank()) "Bus ${trans.number}" else "Bus"
                        }

                        7 -> {
                            iconRes = R.drawable.ic_coach_24dp; tagName = "Coach"
                        }

                        11 -> {
                            iconRes = R.drawable.ic_bus_alt_16dp; tagName =
                                if (!trans.number.isNullOrBlank()) "School Bus ${trans.number}" else "School Bus"
                        }

                        9 -> {
                            iconRes = R.drawable.ic_directions_boat_24dp; tagName = "Ferry"
                        }

                        99, 100 -> {
                            iconRes = R.drawable.ic_walk_24dp; tagName = "Walk"
                        }

                        else -> {
                            iconRes = R.drawable.ic_unknown_24dp; tagName =
                                trans.name ?: "Unknown Vehicle"
                        }
                    }
                    transportTags.add(TransportTag(iconRes, tagName))
                }
            }
            if (transportTags.isEmpty() && apiJourney.legs.isNotEmpty()) {
                val legDurationSeconds = apiJourney.legs.sumOf { it.duration ?: 0 }
                if (legDurationSeconds > 0) transportTags.add(
                    TransportTag(
                        R.drawable.ic_walk_24dp,
                        "Walk ${TimeUnit.SECONDS.toMinutes(legDurationSeconds.toLong())} min"
                    )
                )
            }

            appRoutes.add(
                Route(
                id = UUID.randomUUID().toString(), routeName = "Option ${index + 1}",
                firstVehicleActualDepartureUTC = firstVehicleActualDepartureMillis,
                firstVehicleScheduledDepartureUTC = firstVehicleScheduledDepartureMillis,
                firstVehicleEstimatedDepartureUTC = firstVehicleEstimatedDepartureMillis,
                overallJourneyETAForDisplay = formatTimeMillisToDisplay(overallJourneyETAMillis),
                overallJourneyDepartureTimeForDisplay = formatTimeMillisToDisplay(
                    parseApiDateTimeStringToMillis(
                        apiJourney.legs.first().origin?.departureTimeEstimated
                            ?: apiJourney.legs.first().origin?.departureTimePlanned
                    )
                ),
                transfersCount = (apiJourney.legs.count {
                    val pc =
                        it.transportation?.product?.classId; pc != null && pc != 99 && pc != 100
                } - 1).coerceAtLeast(0),
                transportTags = transportTags.take(2)
            ))
        }
        return appRoutes
    }

    private fun showDatePicker(buttonToUpdate: MaterialButton) {
        val datePickerBuilder =
            MaterialDatePicker.Builder.datePicker().setTitleText("Select trip date")
                .setSelection(getInitialDatePickerSelection())
        val datePicker = datePickerBuilder.build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar =
                Calendar.getInstance(TimeZone.getTimeZone("UTC")); calendar.timeInMillis = selection
            selectedYear = calendar.get(Calendar.YEAR); selectedMonth =
            calendar.get(Calendar.MONTH); selectedDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            updateDateButtonText(buttonToUpdate)
        }
        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun getInitialDatePickerSelection(): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        if (selectedYear != -1 && selectedMonth != -1 && selectedDayOfMonth != -1) {
            calendar.set(selectedYear, selectedMonth, selectedDayOfMonth, 0, 0, 0); calendar.set(
                Calendar.MILLISECOND,
                0
            )
        }
        return calendar.timeInMillis
    }

    private fun updateDateButtonText(button: MaterialButton) {
        if (selectedYear != -1 && selectedMonth != -1 && selectedDayOfMonth != -1) {
            val displayCalendar = Calendar.getInstance(); displayCalendar.set(
                selectedYear,
                selectedMonth,
                selectedDayOfMonth
            )
            button.text =
                SimpleDateFormat("d MMM yy", Locale.getDefault()).format(displayCalendar.time)
        }
    }

    private fun showTimePicker(buttonToUpdate: MaterialButton) {
        val displayCalendar = Calendar.getInstance()
        val currentHour =
            if (selectedHour != -1) selectedHour else displayCalendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute =
            if (selectedMinute != -1) selectedMinute else displayCalendar.get(Calendar.MINUTE)
        val isSystem24Hour = android.text.format.DateFormat.is24HourFormat(this)
        val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H
        val timePicker =
            MaterialTimePicker.Builder().setTimeFormat(clockFormat).setHour(currentHour)
                .setMinute(currentMinute).setTitleText("Select trip time").build()
        timePicker.addOnPositiveButtonClickListener {
            selectedHour = timePicker.hour; selectedMinute = timePicker.minute
            updateTimeButtonText(buttonToUpdate)
        }
        timePicker.show(supportFragmentManager, "MATERIAL_TIME_PICKER")
    }

    private fun updateTimeButtonText(button: MaterialButton) {
        if (selectedHour != -1 && selectedMinute != -1) {
            val displayCalendar = Calendar.getInstance(); displayCalendar.set(
                Calendar.HOUR_OF_DAY,
                selectedHour
            ); displayCalendar.set(Calendar.MINUTE, selectedMinute)
            button.text =
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(displayCalendar.time)
        }
    }
}