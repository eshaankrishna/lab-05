package com.example.lab5_starter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    // UI elements
    private ArrayList<City> cityDataList;
    private CityArrayAdapter cityArrayAdapter;

    // Firebase
    private CollectionReference citiesRef;

    // Swipe detection variables
    private float x1, x2, y1, y2;
    private static final int MIN_SWIPE_DISTANCE = 150;
    private boolean isSwipe = false;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        citiesRef = FirebaseFirestore.getInstance().collection("cities");

        // Initialize UI elements
        ListView cityList = findViewById(R.id.city_list);
        EditText addCityEditText = findViewById(R.id.city_name_edit);
        EditText addProvinceEditText = findViewById(R.id.province_name_edit);
        Button addCityButton = findViewById(R.id.add_city_button);

        // Initialize ArrayList
        cityDataList = new ArrayList<>();

        // Initialize adapter with custom CityArrayAdapter
        cityArrayAdapter = new CityArrayAdapter(this, cityDataList);
        cityList.setAdapter(cityArrayAdapter);

        // Set up swipe hint with red arrows
        TextView swipeHint = findViewById(R.id.swipe_hint_text);
        String hintText = "⟸⟸ Swipe to Delete ⟹⟹";
        SpannableString spannableString = new SpannableString(hintText);

        // Make left arrows red (first 2 characters)
        spannableString.setSpan(new ForegroundColorSpan(Color.RED), 0, 2,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        // Make right arrows red (last 2 characters)
        spannableString.setSpan(new ForegroundColorSpan(Color.RED), 19, 21,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        swipeHint.setText(spannableString);

        // Add click listener for the button
        addCityButton.setOnClickListener(v -> {
            String cityName = addCityEditText.getText().toString().trim();
            String provinceName = addProvinceEditText.getText().toString().trim();

            if (!cityName.isEmpty() && !provinceName.isEmpty()) {
                City newCity = new City(cityName, provinceName);
                addNewCity(newCity);

                // Clear input fields
                addCityEditText.setText("");
                addProvinceEditText.setText("");
            }
        });

        // Add item click listener for editing cities
        cityList.setOnItemClickListener((parent, view, position, id) -> {
            // Only show edit dialog if it wasn't a swipe
            if (!isSwipe) {
                City cityToEdit = cityDataList.get(position);
                showEditDialog(cityToEdit);
            }
            isSwipe = false; // Reset swipe flag
        });

        // Add swipe to delete functionality
        cityList.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x1 = event.getX();
                    y1 = event.getY();
                    isSwipe = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    // Detect if user is moving horizontally
                    x2 = event.getX();
                    y2 = event.getY();
                    float deltaX = x2 - x1;
                    float deltaY = y2 - y1;

                    // If horizontal movement is significant, mark as swipe
                    if (Math.abs(deltaX) > 30 && Math.abs(deltaX) > Math.abs(deltaY)) {
                        isSwipe = true;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    x2 = event.getX();
                    float finalDeltaX = x2 - x1;

                    // Check if it's a swipe (significant horizontal movement)
                    if (Math.abs(finalDeltaX) > MIN_SWIPE_DISTANCE) {
                        // Get the position of the item that was swiped
                        int position = cityList.pointToPosition((int) x1, (int) event.getY());
                        if (position != ListView.INVALID_POSITION) {
                            City cityToDelete = cityDataList.get(position);
                            deleteCity(cityToDelete);
                            isSwipe = true; // Mark as swipe to prevent click
                            return true; // Consume the event to prevent click
                        }
                    } else {
                        isSwipe = false; // Not a swipe, allow click
                    }
                    break;
            }
            return false; // Allow click events to propagate for taps
        });

        // Add snapshot listener for real-time updates from Firestore
        citiesRef.addSnapshotListener((QuerySnapshot querySnapshots,
                                       FirebaseFirestoreException error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
                return;
            }

            if (querySnapshots != null) {
                cityDataList.clear();
                for (QueryDocumentSnapshot doc : querySnapshots) {
                    String city = doc.getId();
                    String province = doc.getString("Province");
                    Log.d("Firestore", String.format("City(%s, %s) fetched", city, province));
                    cityDataList.add(new City(city, province));
                }
                cityArrayAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * Shows a dialog to edit city information
     */
    private void showEditDialog(City oldCity) {
        // Inflate custom dialog layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_city, null);

        // Get references to dialog views
        EditText editCityName = dialogView.findViewById(R.id.edit_city_name);
        EditText editProvinceName = dialogView.findViewById(R.id.edit_province_name);

        // Pre-fill with current values
        editCityName.setText(oldCity.getCityName());
        editProvinceName.setText(oldCity.getProvinceName());

        // Create and show dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit City")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newCityName = editCityName.getText().toString().trim();
                    String newProvinceName = editProvinceName.getText().toString().trim();

                    if (!newCityName.isEmpty() && !newProvinceName.isEmpty()) {
                        City newCity = new City(newCityName, newProvinceName);
                        updateCity(oldCity, newCity);
                    } else {
                        Toast.makeText(MainActivity.this,
                                "City and Province cannot be empty",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    /**
     * Adds a new city to Firestore database
     */
    private void addNewCity(City city) {
        // Add to Firestore database
        HashMap<String, String> data = new HashMap<>();
        data.put("Province", city.getProvinceName());

        citiesRef
                .document(city.getCityName())
                .set(data)
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "DocumentSnapshot successfully written!"))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error writing document", e));
    }

    /**
     * Updates a city in Firestore database
     */
    private void updateCity(City oldCity, City newCity) {
        // If city name changed, we need to delete old document and create new one
        // (since city name is the document ID)
        if (!oldCity.getCityName().equals(newCity.getCityName())) {
            // Delete old document
            citiesRef.document(oldCity.getCityName()).delete()
                    .addOnSuccessListener(aVoid -> {
                        // Add new document with new name
                        addNewCity(newCity);
                        Log.d("Firestore", "City name updated successfully!");
                    })
                    .addOnFailureListener(e ->
                            Log.e("Firestore", "Error updating city name", e));
        } else {
            // If only province changed, just update the document
            HashMap<String, String> data = new HashMap<>();
            data.put("Province", newCity.getProvinceName());

            citiesRef.document(newCity.getCityName())
                    .set(data)
                    .addOnSuccessListener(aVoid ->
                            Log.d("Firestore", "Province updated successfully!"))
                    .addOnFailureListener(e ->
                            Log.e("Firestore", "Error updating province", e));
        }
    }

    /**
     * Deletes a city from Firestore database
     */
    private void deleteCity(City city) {
        citiesRef
                .document(city.getCityName())
                .delete()
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "DocumentSnapshot successfully deleted!"))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error deleting document", e));
    }
}

