package com.example.ambulink.View;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ambulink.Adapter.HospitalsAdapter;
import com.example.ambulink.Controller.HospitalController;
import com.example.ambulink.Model.PatientModel;
import com.example.ambulink.R;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HospitalView extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener  {

    private RecyclerView recyclerView;
    private HospitalsAdapter adapter;
    private ListenerRegistration listenerRegistration;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore firestore;
    private Button signoutBtn, btnAddSlots, btnMinusSlots;
    private TextView tvMaxSlot, tvCapacityDetail;
    private EditText searchParamedics;
    private List<PatientModel> originalPatientList = new ArrayList<>();
    private TextView userEmailTextView;
    private TextView userNameTextView;


    private HospitalController hospitalController;
    private String hospitalId;
    private DrawerLayout drawerLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.hospital_dashboard);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Get the header view from the NavigationView
        View headerView = navigationView.getHeaderView(0);

        ImageView hamburgerMenu = findViewById(R.id.hamburger_menu);

        userEmailTextView = headerView.findViewById(R.id.user_email);
        userNameTextView = headerView.findViewById(R.id.user_name);
        signoutBtn = headerView.findViewById(R.id.signOut);

        loadUserData();


        // Set the user's email
        if (user != null) {
            String userEmail = user.getEmail();
            userEmailTextView.setText(userEmail);

            // Query Firestore to get the full name
            firestore.collection("users")
                    .whereEqualTo("email", userEmail)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String fullName = document.getString("fullName");
                                if (fullName != null) {
                                    userNameTextView.setText(fullName);
                                }
                                break; // Exit the loop once we find the match
                            }
                        } else {
                            Log.d("Firestore", "Error getting documents: ", task.getException());
                            userNameTextView.setText("User");
                        }
                    });
        }

        // Set up the click listener for the hamburger menu icon
        hamburgerMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    drawerLayout.openDrawer(GravityCompat.END);
                }
            }
        });

        // Setup ActionBar toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    // Call the default behavior
                    finish();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

        // Initialize controller
        hospitalController = new HospitalController(this);

        // Get the hospital ID from the intent
        Intent intent = getIntent();
        hospitalId = intent.getStringExtra("hospitalId");

        // Initialize UI elements
        setupUI();

        // Load data through controller
        hospitalController.loadInitialData(hospitalId);
        hospitalController.loadAndListenForPatientUpdates(hospitalId);
        signoutBtn.setOnClickListener(view -> signOut());

        btnAddSlots.setOnClickListener(view -> hospitalController.updateSlots(hospitalId, 1));
        btnMinusSlots.setOnClickListener(view -> hospitalController.updateSlots(hospitalId, -1));
    }

    private void loadUserData() {
        if (user != null) {
            String uid = user.getUid();
            firestore.collection("users")
                    .document(uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String fullName = document.getString("fullName");
                                if (fullName != null) {
                                    userNameTextView.setText(fullName);
                                }

                                String email = document.getString("email");
                                if (email != null) {
                                    userEmailTextView.setText(email);
                                }
                            } else {
                                Log.d("Firestore", "No such document");
                                userNameTextView.setText("User");
                            }
                        } else {
                            Log.d("Firestore", "Error getting document: ", task.getException());
                            userNameTextView.setText("User");
                        }
                    });
        }
    }



    private final ActivityResultLauncher<Intent> speechInputLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> speechResults = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (speechResults != null && !speechResults.isEmpty()) {
                        String recognizedText = speechResults.get(0);
                        searchParamedics.setText(recognizedText);
                        searchParamedics.setSelection(recognizedText.length());
                    }
                }
            }
    );

    private void setupUI() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnAddSlots = findViewById(R.id.btnAdd);
        btnMinusSlots = findViewById(R.id.btnMinus);
        tvMaxSlot = findViewById(R.id.max_slot);
        tvCapacityDetail = findViewById(R.id.capacity_detail);
        searchParamedics = findViewById(R.id.searchParamedics);

        searchParamedics.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (searchParamedics.getRight() - searchParamedics.getCompoundDrawables()[2].getBounds().width())) {
                    promptSpeechInput();
                    return true;
                }
            }
            return false;
        });

        // Add TextWatcher for search functionality
        searchParamedics.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed before text changes
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed during text changes
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.equalsIgnoreCase("paramedics")) {
                    sortPatientsBySenderEmail();
                } else {
                    filterPatientsByQuery(query);
                }
            }
        });
    }

    /**
     * Sorts the original patient list by senderEmail and updates the adapter.
     */
    private void sortPatientsBySenderEmail() {
        if (originalPatientList.isEmpty()) {
            return;
        }

        List<PatientModel> sortedList = new ArrayList<>(originalPatientList);
        Collections.sort(sortedList, new Comparator<PatientModel>() {
            @Override
            public int compare(PatientModel p1, PatientModel p2) {
                return p1.getSenderEmail().compareToIgnoreCase(p2.getSenderEmail());
            }
        });

        runOnUiThread(() -> {
            adapter.updatePatientList(sortedList);
            showToast("Sorted by Sender Email");
        });
    }


    /**
     * Filters the original patient list based on the search query.
     * If the query is not "paramedics", you can implement other filtering logic here.
     *
     * @param query The search query entered by the user.
     */
    private void filterPatientsByQuery(String query) {
        if (originalPatientList.isEmpty()) {
            return;
        }

        if (query.isEmpty()) {
            // If query is empty, show the original list
            runOnUiThread(() -> adapter.updatePatientList(new ArrayList<>(originalPatientList)));
            return;
        }

        // Example: Filter patients whose senderEmail contains the query
        List<PatientModel> filteredList = new ArrayList<>();
        for (PatientModel patient : originalPatientList) {
            if (patient.getSenderEmail().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(patient);
            }
        }

        runOnUiThread(() -> adapter.updatePatientList(filteredList));
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the paramedic's name");
        try {
            speechInputLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            showToast("Speech input not supported on this device");
        }
    }

    /**
     * Updates the patient list and maintains the original list for filtering/sorting.
     *
     * @param patientList The new list of patients to display.
     */
    public void updatePatientList(List<PatientModel> patientList) {
        this.originalPatientList = new ArrayList<>(patientList); // Store the original list
        if (adapter == null) {
            adapter = new HospitalsAdapter(this, patientList, hospitalId);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updatePatientList(patientList);
        }
    }

    public void updateSlotDetails(int maxSlots, int currentCapacity) {
        tvMaxSlot.setText("/" + maxSlots);
        tvCapacityDetail.setText(String.valueOf(currentCapacity));
    }

    private void signOut() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null; // Reset the listener reference
        }

        // Proceed with Firebase sign-out
        mAuth.signOut();
        Intent intent = new Intent(HospitalView.this, LoginView.class);
        intent.putExtra("LOGIN_TYPE", "hospital");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void deleteAccount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Delete user data from Firestore
            String uid = user.getUid();
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Delete the user from Firebase Authentication
                        user.delete()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(this, "Account deleted successfully.", Toast.LENGTH_SHORT).show();
                                        // Redirect to login screen
                                        Intent intent = new Intent(this, LoginView.class);
                                        intent.putExtra("LOGIN_TYPE", "hospital");
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(this, "Failed to delete account.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to delete user data.", Toast.LENGTH_SHORT).show();
                    });
        }
    }


    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }


    public void showToast(String message) {
        Toast.makeText(HospitalView.this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.END);

        int id = item.getItemId();

        if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsView.class);
            intent.putExtra("LOGIN_TYPE", "hospital");
            startActivity(intent);
        } else if (id == R.id.nav_delete_account) {
            showDeleteAccountDialog();
        }else if (id == R.id.nav_hospital_staff) {
            Intent intent = new Intent(this, StaffManagementView.class);
            intent.putExtra("hospitalId", hospitalId);
            startActivity(intent);
        }

        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hospitalController.removePatientListener();  // Stop listening on exit
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload user data
        loadUserData();
    }

}
