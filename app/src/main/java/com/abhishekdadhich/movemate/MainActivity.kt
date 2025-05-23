package com.abhishekdadhich.movemate

import android.content.Context
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
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
    private lateinit var scrollViewAvailableRoutes: NestedScrollView
    private lateinit var recyclerViewRoutes: RecyclerView
    private lateinit var routeAdapter: RouteAdapter

    private lateinit var fromSuggestionsAdapter: ArrayAdapter<String>
    private var currentFromSuggestions: List<StopFinderLocation> = emptyList()
    private var selectedOriginStopId: String? = null
    private val debounceHandlerFrom = Handler(Looper.getMainLooper())
    private var searchRunnableFrom: Runnable? = null
    private var isProgrammaticTextChangeFrom = false
    private var isUsingCurrentLocationForFrom = true // New flag

    private lateinit var toSuggestionsAdapter: ArrayAdapter<String>
    private var currentToSuggestions: List<StopFinderLocation> = emptyList()
    private var selectedDestinationStopId: String? = null
    private val debounceHandlerTo = Handler(Looper.getMainLooper())
    private var searchRunnableTo: Runnable? = null
    private var isProgrammaticTextChangeTo = false

    private val DEBOUNCE_DELAY_MS = 500L
    private val MIN_CHAR_TRIGGER_AUTOCOMPLETE = 3
    private val CURRENT_LOCATION_TEXT = "Current location (Tap to change)"


    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val displayTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        autoCompleteTextViewFrom = findViewById(R.id.autoCompleteTextViewFrom)
        autoCompleteTextViewTo = findViewById(R.id.autoCompleteTextViewTo)
        scrollViewAvailableRoutes = findViewById(R.id.scrollViewAvailableRoutes)
        recyclerViewRoutes = findViewById(R.id.recyclerViewRoutes)

        routeAdapter = RouteAdapter(emptyList())
        recyclerViewRoutes.layoutManager = LinearLayoutManager(this)
        recyclerViewRoutes.adapter = routeAdapter
        recyclerViewRoutes.isNestedScrollingEnabled = false

        setupFromAutocomplete()
        setupToAutocomplete()

        // Set initial text for "From" field and manage current location state
        autoCompleteTextViewFrom.setText(CURRENT_LOCATION_TEXT)
        isUsingCurrentLocationForFrom = true

        // Set initial focus to "To" field and show keyboard
        autoCompleteTextViewTo.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(autoCompleteTextViewTo, InputMethodManager.SHOW_IMPLICIT)


        val buttonToday = findViewById<MaterialButton>(R.id.buttonToday)
        buttonToday.setOnClickListener { showDatePicker(buttonToday) }
        val initialCalendar = Calendar.getInstance()
        selectedYear = initialCalendar.get(Calendar.YEAR)
        selectedMonth = initialCalendar.get(Calendar.MONTH)
        selectedDayOfMonth = initialCalendar.get(Calendar.DAY_OF_MONTH)

        val buttonNow = findViewById<MaterialButton>(R.id.buttonNow)
        buttonNow.setOnClickListener { showTimePicker(buttonNow) }
        selectedHour = initialCalendar.get(Calendar.HOUR_OF_DAY)
        selectedMinute = initialCalendar.get(Calendar.MINUTE)

        val buttonFindRoutes = findViewById<MaterialButton>(R.id.buttonFindRoutes)
        buttonFindRoutes.setOnClickListener {
            hideKeyboard()
            lifecycleScope.launch {
                handleFindRoutesClick()
            }
        }
    }

    private fun hideKeyboard() {
        val view = this.currentFocus ?: View(this) // Fallback to a new view if no focus
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        // Clearing focus can also help ensure keyboard doesn't reappear easily
        autoCompleteTextViewFrom.clearFocus()
        autoCompleteTextViewTo.clearFocus()
        findViewById<View>(R.id.main_activity_root_layout)?.requestFocus() // Request focus on a non-input view if available
        // Add android:id="@+id/main_activity_root_layout" to your root ConstraintLayout in activity_main.xml
    }


    private fun setupFromAutocomplete() {
        fromSuggestionsAdapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        autoCompleteTextViewFrom.setAdapter(fromSuggestionsAdapter)
        autoCompleteTextViewFrom.threshold = MIN_CHAR_TRIGGER_AUTOCOMPLETE

        autoCompleteTextViewFrom.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && autoCompleteTextViewFrom.text.toString() == CURRENT_LOCATION_TEXT) {
                isProgrammaticTextChangeFrom = true // Prevent TextWatcher during this clear
                autoCompleteTextViewFrom.setText("")
                isProgrammaticTextChangeFrom = false
                isUsingCurrentLocationForFrom = false // User is now typing
                selectedOriginStopId =
                    null // Clear any previously selected ID from current location mode
            } else if (!hasFocus && autoCompleteTextViewFrom.text.isBlank() && selectedOriginStopId == null) {
                // If focus is lost, field is blank, and no valid stop selected, revert to Current Location
                isProgrammaticTextChangeFrom = true
                autoCompleteTextViewFrom.setText(CURRENT_LOCATION_TEXT)
                isProgrammaticTextChangeFrom = false
                isUsingCurrentLocationForFrom = true
            }
        }

        autoCompleteTextViewFrom.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isProgrammaticTextChangeFrom || isUsingCurrentLocationForFrom && s.toString() == CURRENT_LOCATION_TEXT) {
                    return
                }
                searchRunnableFrom?.let { debounceHandlerFrom.removeCallbacks(it) }
            }

            override fun afterTextChanged(s: Editable?) {
                if (isProgrammaticTextChangeFrom || isUsingCurrentLocationForFrom && s.toString() == CURRENT_LOCATION_TEXT) {
                    return
                }

                val query = s.toString().trim()
                if (query.length >= MIN_CHAR_TRIGGER_AUTOCOMPLETE) {
                    isUsingCurrentLocationForFrom = false // User is typing something else
                    selectedOriginStopId = null // New search, clear old selection
                    searchRunnableFrom = Runnable { fetchStopSuggestions(query, true) }
                    debounceHandlerFrom.postDelayed(searchRunnableFrom!!, DEBOUNCE_DELAY_MS)
                } else {
                    fromSuggestionsAdapter.clear()
                    selectedOriginStopId = null
                    // Don't revert to CURRENT_LOCATION_TEXT here, onFocusChange will handle it
                }
            }
        })

        autoCompleteTextViewFrom.setOnItemClickListener { parent, view, position, id ->
            if (position < currentFromSuggestions.size) {
                val selectedLocation = currentFromSuggestions[position]
                selectedOriginStopId = selectedLocation.id
                isUsingCurrentLocationForFrom = false // User selected a specific location

                isProgrammaticTextChangeFrom = true
                val displayName = selectedLocation.name ?: selectedLocation.disassembledName ?: ""
                autoCompleteTextViewFrom.setText(displayName, false) // false to prevent filtering
                autoCompleteTextViewFrom.setSelection(displayName.length) // Move cursor to end
                autoCompleteTextViewFrom.dismissDropDown()
                isProgrammaticTextChangeFrom = false
                // hideKeyboard() // Keyboard usually hides on item click for AutoCompleteTextView

                Log.d("TripPlannerApp", "Origin selected: $displayName, ID: $selectedOriginStopId")
            }
        }
    }

    private fun setupToAutocomplete() { // Similar structure to setupFromAutocomplete
        toSuggestionsAdapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mutableListOf<String>())
        autoCompleteTextViewTo.setAdapter(toSuggestionsAdapter)
        autoCompleteTextViewTo.threshold = MIN_CHAR_TRIGGER_AUTOCOMPLETE

        autoCompleteTextViewTo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isProgrammaticTextChangeTo) return
                searchRunnableTo?.let { debounceHandlerTo.removeCallbacks(it) }
            }

            override fun afterTextChanged(s: Editable?) {
                if (isProgrammaticTextChangeTo) return

                val query = s.toString().trim()
                if (query.length >= MIN_CHAR_TRIGGER_AUTOCOMPLETE) {
                    selectedDestinationStopId = null // New search, clear old selection
                    searchRunnableTo = Runnable { fetchStopSuggestions(query, false) }
                    debounceHandlerTo.postDelayed(searchRunnableTo!!, DEBOUNCE_DELAY_MS)
                } else {
                    toSuggestionsAdapter.clear()
                    selectedDestinationStopId = null
                }
            }
        })

        autoCompleteTextViewTo.setOnItemClickListener { parent, view, position, id ->
            if (position < currentToSuggestions.size) {
                val selectedLocation = currentToSuggestions[position]
                selectedDestinationStopId = selectedLocation.id

                isProgrammaticTextChangeTo = true
                val displayName = selectedLocation.name ?: selectedLocation.disassembledName ?: ""
                autoCompleteTextViewTo.setText(displayName, false)
                autoCompleteTextViewTo.setSelection(displayName.length)
                autoCompleteTextViewTo.dismissDropDown()
                isProgrammaticTextChangeTo = false
                // hideKeyboard()

                Log.d(
                    "TripPlannerApp",
                    "Destination selected: $displayName, ID: $selectedDestinationStopId"
                )
            }
        }
    }


    private fun fetchStopSuggestions(query: String, isForOriginField: Boolean) {
        // Check if the query is the special "Current Location" text, and if so, don't fetch.
        if (isForOriginField && query == CURRENT_LOCATION_TEXT) {
            fromSuggestionsAdapter.clear()
            return
        }

        lifecycleScope.launch {
            Log.d(
                "TripPlannerApp",
                "Fetching suggestions for: '$query' (isForOrigin: $isForOriginField)"
            )
            // ... (rest of fetchStopSuggestions logic remains largely the same as before)
            // Ensure that after adapter.addAll() and notifyDataSetChanged(),
            // the logic to show dropdown is robust.
            // For the re-opening issue: the key is that the TextWatcher for the programmatic setText
            // in OnItemClickListener is correctly bypassed by the isProgrammaticTextChange flag.
            // And that setText itself does not have side effects that force a new suggestion pass.
            try {
                val apiKeyHeader = "apikey ${BuildConfig.TFNW_API_KEY}"
                val response = RetrofitClient.instance.findStops(
                    apiKey = apiKeyHeader,
                    searchTerm = query,
                    typeSf = "any"
                )

                val adapterToUpdate: ArrayAdapter<String>
                val currentSuggestionsListToUpdate: (List<StopFinderLocation>) -> Unit
                val autoCompleteTextViewToShowDropDown: AutoCompleteTextView

                if (isForOriginField) {
                    adapterToUpdate = fromSuggestionsAdapter
                    currentSuggestionsListToUpdate = { list -> currentFromSuggestions = list }
                    autoCompleteTextViewToShowDropDown = autoCompleteTextViewFrom
                } else {
                    adapterToUpdate = toSuggestionsAdapter
                    currentSuggestionsListToUpdate = { list -> currentToSuggestions = list }
                    autoCompleteTextViewToShowDropDown = autoCompleteTextViewTo
                }

                if (response.isSuccessful) {
                    val stopFinderResponse = response.body()
                    val locations = stopFinderResponse?.locations?.filterNotNull() ?: emptyList()

                    val sortedLocations = locations.sortedWith(
                        compareByDescending<StopFinderLocation> { it.isBest == true }
                            .thenByDescending { it.matchQuality }
                    )
                    currentSuggestionsListToUpdate(sortedLocations)

                    val suggestionStrings = sortedLocations.map { location ->
                        val displayName =
                            location.name ?: location.disassembledName ?: "Unknown location"
                        if (location.isBest == true) "★ $displayName" else displayName
                    }

                    adapterToUpdate.clear()
                    adapterToUpdate.addAll(suggestionStrings)
                    // adapterToUpdate.notifyDataSetChanged() // Not always needed if addAll does it
                    Log.d(
                        "TripPlannerApp",
                        "Suggestions loaded: ${suggestionStrings.size} for query '$query'"
                    )

                    // Only show dropdown if it's not a programmatic change causing this text change,
                    // and the field has focus, and there are suggestions
                    val stillDebouncing =
                        if (isForOriginField) searchRunnableFrom != null else searchRunnableTo != null
                    if (!isProgrammaticTextChangeFrom && !isProgrammaticTextChangeTo && !stillDebouncing &&
                        autoCompleteTextViewToShowDropDown.isFocused && suggestionStrings.isNotEmpty()
                    ) {
                        autoCompleteTextViewToShowDropDown.showDropDown()
                    }

                } else {
                    Log.e(
                        "TripPlannerApp",
                        "StopFinder API Error for query '$query': ${response.code()} - ${
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
        var nameOriginApi = selectedOriginStopId
        var typeOriginApi = if (nameOriginApi != null) "any" else "any" // Or "stop"

        // Handle "Current Location" case for origin
        if (isUsingCurrentLocationForFrom) {
            // TODO: In next step, get actual lat/lon and use type_origin="coord"
            // For now, we'll signify it or use a default placeholder ID if selectedOriginStopId is null
            // and we don't have actual coordinates yet.
            // This Toast shows we know it's current location mode.
            Toast.makeText(
                this,
                "Using Current Location (placeholder) for Origin",
                Toast.LENGTH_SHORT
            ).show()
            // For now, if current location is active and no specific stop ID picked, fall back to default.
            // This will be replaced by actual coordinate usage.
            if (nameOriginApi == null) {
                nameOriginApi =
                    "10101331" // Default placeholder if current location actual coords not yet fetched
                Log.w("TripPlannerApp", "Origin: Using placeholder ID for 'Current Location'")
            }
        }


        val nameDestinationApi = selectedDestinationStopId
        val typeDestinationApi = if (nameDestinationApi != null) "any" else "any" // Or "stop"
        val finalNameOrigin = nameOriginApi ?: "10101331" // Fallback if still null
        val finalNameDestination = nameDestinationApi ?: "10102027" // Fallback if still null

        if ((isUsingCurrentLocationForFrom && selectedOriginStopId == null && finalNameOrigin == "10101331") || // Using default because current loc not resolved
            (!isUsingCurrentLocationForFrom && selectedOriginStopId == null) ||
            selectedDestinationStopId == null
        ) {

            var message = "Using default for: "
            val defaultsUsed = mutableListOf<String>()
            if (isUsingCurrentLocationForFrom && selectedOriginStopId == null) defaultsUsed.add("Origin (Current Loc placeholder)")
            else if (selectedOriginStopId == null) defaultsUsed.add("Origin")

            if (selectedDestinationStopId == null) defaultsUsed.add("Destination")

            if (defaultsUsed.isNotEmpty()) {
                Toast.makeText(
                    this,
                    message + defaultsUsed.joinToString(", ") + ". Please select from suggestions for accurate results.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }


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

        Log.d(
            "TripPlannerApp",
            "Requesting trip: From ID: $finalNameOrigin (type:$typeOriginApi), To ID: $finalNameDestination (type:$typeDestinationApi), Date: $dateForApi, Time: $timeForApi"
        )
        // ... (rest of API call and response handling)
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
                nameOrigin = finalNameOrigin,
                typeDestination = typeDestinationApi,
                nameDestination = finalNameDestination,
                calcNumberOfTrips = 3,
                tfNSWTR = "true",
                version = "10.2.1.42"
            )

            if (response.isSuccessful) {
                val tripResponse = response.body()
                if (tripResponse != null && tripResponse.error == null && tripResponse.journeys != null) {
                    val appRoutes = mapApiJourneysToAppRoutes(tripResponse.journeys)
                    routeAdapter.updateRoutes(appRoutes)
                    if (appRoutes.isNotEmpty()) {
                        Log.d(
                            "TripPlannerApp",
                            "API Success: Displaying ${appRoutes.size} journeys."
                        )
                        scrollViewAvailableRoutes.visibility = View.VISIBLE
                    } else {
                        Log.d("TripPlannerApp", "API Success: No journeys found for the selection.")
                        Toast.makeText(
                            this,
                            "No journeys found for the selection.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    val errorMessage =
                        tripResponse?.error?.message ?: "Unknown API error or no journeys"
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
    }

    private fun parseApiDateTimeStringToMillis(dateTimeString: String?): Long? {
        if (dateTimeString == null) return null
        return try {
            apiDateFormat.parse(dateTimeString)?.time
        } catch (e: ParseException) {
            Log.e("TripPlannerApp", "Error parsing date string: $dateTimeString", e)
            null
        }
    }

    private fun formatTimeMillisToDisplay(timeMillis: Long?): String {
        if (timeMillis == null) return "N/A"
        val localCalendar = Calendar.getInstance()
        localCalendar.timeInMillis = timeMillis
        return displayTimeFormat.format(localCalendar.time)
    }

    private fun mapApiJourneysToAppRoutes(apiJourneys: List<Journey>): List<Route> {
        val appRoutes = mutableListOf<Route>()
        apiJourneys.forEachIndexed { index, apiJourney ->
            if (apiJourney.legs.isNullOrEmpty()) {
                Log.w("TripPlannerApp", "Journey ${index + 1} has no legs, skipping.")
                return@forEachIndexed
            }

            var firstVehicleLeg: JourneyLeg? = null
            for (leg in apiJourney.legs) {
                val productClass = leg.transportation?.product?.classId
                if (productClass != null && productClass != 99 && productClass != 100) {
                    firstVehicleLeg = leg
                    break
                }
            }

            val effectiveFirstLegForDepartureInfo = firstVehicleLeg ?: apiJourney.legs.first()
            val lastLeg = apiJourney.legs.last()

            val firstVehicleScheduledDepartureMillis = parseApiDateTimeStringToMillis(
                effectiveFirstLegForDepartureInfo.origin?.departureTimePlanned
            )
            val firstVehicleEstimatedDepartureMillis = parseApiDateTimeStringToMillis(
                effectiveFirstLegForDepartureInfo.origin?.departureTimeEstimated
            )
            val firstVehicleActualDepartureMillis =
                firstVehicleEstimatedDepartureMillis ?: firstVehicleScheduledDepartureMillis


            val overallJourneyETAMillis = parseApiDateTimeStringToMillis(
                lastLeg.destination?.arrivalTimeEstimated ?: lastLeg.destination?.arrivalTimePlanned
            )

            val transportTags = mutableListOf<TransportTag>()
            apiJourney.legs.forEach { leg ->
                leg.transportation?.let { trans ->
                    val iconRes: Int
                    var tagName = "Unknown" // Default tagName

                    when (trans.product?.classId) {
                        1 -> { // Train
                            iconRes = R.drawable.ic_train_alt_16dp
                            tagName = "Train"
                        }

                        2 -> { // Metro
                            iconRes = R.drawable.ic_metro_24dp // Use new metro icon
                            tagName = trans.number ?: trans.disassembledName
                                    ?: "Metro" // Prefer number if available
                            if (!tagName.contains(
                                    "Metro",
                                    ignoreCase = true
                                ) && (trans.name?.contains(
                                    "Metro",
                                    ignoreCase = true
                                ) == true || trans.disassembledName?.contains(
                                    "Metro",
                                    ignoreCase = true
                                ) == true)
                            ) {
                                tagName = "Metro" // Override if "Metro" in name but not number
                            } else if (!tagName.contains("Metro", ignoreCase = true)) {
                                tagName = "Metro $tagName".trim() // Prepend "Metro" if not present
                            }
                        }

                        4 -> { // Light Rail
                            iconRes =
                                R.drawable.ic_train_alt_16dp // Placeholder, use specific Light Rail icon later
                            tagName = "Light Rail"
                        }

                        5 -> { // Bus
                            iconRes = R.drawable.ic_bus_alt_16dp
                            tagName =
                                if (!trans.number.isNullOrBlank()) "Bus ${trans.number}" else "Bus"
                        }

                        7 -> { // Coach
                            iconRes = R.drawable.ic_coach_24dp // Use new coach icon
                            tagName =
                                if (!trans.number.isNullOrBlank()) "Coach ${trans.number}" else "Coach"
                        }

                        11 -> { // School Bus
                            iconRes =
                                R.drawable.ic_bus_alt_16dp // Can use a specific school bus icon
                            tagName =
                                if (!trans.number.isNullOrBlank()) "Bus ${trans.number}" else "School Bus"
                        }

                        9 -> { // Ferry
                            iconRes = R.drawable.ic_directions_boat_24dp
                            tagName = "Ferry"
                        }

                        99, 100 -> { // Walking
                            iconRes = R.drawable.ic_walk_24dp
                            tagName = "Walk"
                        }

                        else -> {
                            iconRes = R.drawable.ic_walk_24dp // Default for unknown
                            tagName = trans.name ?: "Unknown Vehicle"
                        }
                    }
                    transportTags.add(TransportTag(iconRes, tagName))
                }
            }
            if (transportTags.isEmpty() && apiJourney.legs.isNotEmpty()) {
                val legDurationSeconds = apiJourney.legs.sumOf { it.duration ?: 0 }
                if (legDurationSeconds > 0) {
                    transportTags.add(
                        TransportTag(
                            R.drawable.ic_walk_24dp,
                            "Walk ${TimeUnit.SECONDS.toMinutes(legDurationSeconds.toLong())} min"
                        )
                    )
                }
            }

            val route = Route(
                id = UUID.randomUUID().toString(),
                routeName = "Option ${index + 1}",
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
                    val productClass = it.transportation?.product?.classId
                    productClass != null && productClass != 99 && productClass != 100
                } - 1).coerceAtLeast(0),
                transportTags = transportTags.take(2)
            )
            appRoutes.add(route)
        }
        return appRoutes
    }

    // --- Date & Time Picker Functions ---
    private fun showDatePicker(buttonToUpdate: MaterialButton) {
        val datePickerBuilder = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select trip date")
            .setSelection(getInitialDatePickerSelection())
        val datePicker = datePickerBuilder.build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection
            selectedYear = calendar.get(Calendar.YEAR)
            selectedMonth = calendar.get(Calendar.MONTH)
            selectedDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            updateDateButtonText(buttonToUpdate)
        }
        datePicker.show(supportFragmentManager, "MATERIAL_DATE_PICKER")
    }

    private fun getInitialDatePickerSelection(): Long {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        if (selectedYear != -1 && selectedMonth != -1 && selectedDayOfMonth != -1) {
            calendar.set(selectedYear, selectedMonth, selectedDayOfMonth, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun updateDateButtonText(button: MaterialButton) {
        if (selectedYear != -1 && selectedMonth != -1 && selectedDayOfMonth != -1) {
            val displayCalendar = Calendar.getInstance()
            displayCalendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
            val sdf = SimpleDateFormat("d MMM yy", Locale.getDefault())
            button.text = sdf.format(displayCalendar.time)
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
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(clockFormat)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText("Select trip time")
            .build()
        timePicker.addOnPositiveButtonClickListener {
            selectedHour = timePicker.hour
            selectedMinute = timePicker.minute
            updateTimeButtonText(buttonToUpdate)
        }
        timePicker.show(supportFragmentManager, "MATERIAL_TIME_PICKER")
    }

    private fun updateTimeButtonText(button: MaterialButton) {
        if (selectedHour != -1 && selectedMinute != -1) {
            val displayCalendar = Calendar.getInstance()
            displayCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            displayCalendar.set(Calendar.MINUTE, selectedMinute)
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            button.text = sdf.format(displayCalendar.time)
        }
    }
}