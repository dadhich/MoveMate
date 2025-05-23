package com.abhishekdadhich.movemate

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    // Variables to store the selected date
    private var selectedYear: Int = -1
    private var selectedMonth: Int = -1 // 0-11 for Calendar month
    private var selectedDayOfMonth: Int = -1

    // Variables to store the selected time
    private var selectedHour: Int = -1 // 0-23
    private var selectedMinute: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Date Picker Setup ---
        val buttonToday = findViewById<MaterialButton>(R.id.buttonToday)
        buttonToday.setOnClickListener {
            showDatePicker(buttonToday)
        }
        // Initialize selected date to today
        val initialCalendar = Calendar.getInstance() // Use local timezone for initial default
        selectedYear = initialCalendar.get(Calendar.YEAR)
        selectedMonth = initialCalendar.get(Calendar.MONTH)
        selectedDayOfMonth = initialCalendar.get(Calendar.DAY_OF_MONTH)

        // --- Time Picker Setup ---
        val buttonNow = findViewById<MaterialButton>(R.id.buttonNow)
        buttonNow.setOnClickListener {
            showTimePicker(buttonNow)
        }
        // Initialize selected time to current time
        selectedHour = initialCalendar.get(Calendar.HOUR_OF_DAY)
        selectedMinute = initialCalendar.get(Calendar.MINUTE)
    }

    // --- Date Picker Functions ---
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
            calendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
        }
        return calendar.timeInMillis
    }

    private fun updateDateButtonText(button: MaterialButton) {
        if (selectedYear != -1 && selectedMonth != -1 && selectedDayOfMonth != -1) {
            val calendar = Calendar.getInstance()
            calendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
            val date = calendar.time
            val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            button.text = sdf.format(date)
        }
    }

    // --- Time Picker Functions ---
    private fun showTimePicker(buttonToUpdate: MaterialButton) {
        val currentHour = if (selectedHour != -1) selectedHour else Calendar.getInstance()
            .get(Calendar.HOUR_OF_DAY)
        val currentMinute = if (selectedMinute != -1) selectedMinute else Calendar.getInstance()
            .get(Calendar.MINUTE)

        // Check current system time format (12 or 24 hour) to set picker default
        val isSystem24Hour = android.text.format.DateFormat.is24HourFormat(this)
        val clockFormat = if (isSystem24Hour) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(clockFormat)
            .setHour(currentHour)
            .setMinute(currentMinute)
            .setTitleText("Select trip time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            selectedHour = timePicker.hour // hour is 0-23
            selectedMinute = timePicker.minute

            updateTimeButtonText(buttonToUpdate)
        }
        timePicker.show(supportFragmentManager, "MATERIAL_TIME_PICKER")
    }

    private fun updateTimeButtonText(button: MaterialButton) {
        if (selectedHour != -1 && selectedMinute != -1) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)

            // Format the time (e.g., "4:30 PM")
            // "h:mm a" for 12-hour format with AM/PM (e.g., 4:30 PM)
            // "HH:mm" for 24-hour format (e.g., 16:30)
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            button.text = sdf.format(calendar.time)
        }
    }
}