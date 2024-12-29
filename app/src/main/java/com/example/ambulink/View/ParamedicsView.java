package com.example.ambulink.View;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View; // Import View
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ambulink.Adapter.ParamedicsAdapter;
import com.example.ambulink.Controller.ParamedicsController;
import com.example.ambulink.Listener.HospitalDataListener;
import com.example.ambulink.Listener.SelectListener;
import com.example.ambulink.Model.HospitalModel;
import com.example.ambulink.Model.PatientModel;
import com.example.ambulink.R;
import com.example.ambulink.Service.LocationUpdateService;
import com.example.ambulink.View.PatientFormView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Locale;


public class ParamedicsView extends AppCompatActivity implements SelectListener, HospitalDataListener, NavigationView.OnNavigationItemSelectedListener {

    private RecyclerView recyclerView;
    private ParamedicsAdapter paramedicsAdapter;
    private EditText searchHospital;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private ParamedicsController paramedicsController;
    private Button newPatientBtn;
    private Button signOutBtn;
    private Button editFormBtn; // Added editFormBtn
    private PatientModel patientModel; // Added patientModel
    private HospitalModel hospitalModel;
    private static final int REQUEST_EDIT_FORM = 1;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private FirebaseFirestore firestore;
    private String previousAcceptanceStatus = "";
    private String previousRejectionStatus = "";
    private static final int REQUEST_CODE_SETTINGS = 2;
    private TextView userEmailTextView, userNameTextView;
    private String loginType;

    private final ActivityResultLauncher<Intent> editFormLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    patientModel = result.getData().getParcelableExtra("patientModel");
                    hospitalModel = result.getData().getParcelableExtra("hospitalModel");
                    // Update UI or perform additional actions with the updated model
                }
            }
    );

    private final ActivityResultLauncher<Intent> speechInputLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> speechResults = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (speechResults != null && !speechResults.isEmpty()) {
                        String recognizedText = speechResults.get(0);
                        searchHospital.setText(recognizedText);
                        searchHospital.setSelection(recognizedText.length());
                    }
                }
            }
    );

    @Override
    protected void onCreate(@NonNull Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.paramedics_dashboard);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        firestore = FirebaseFirestore.getInstance();

        Intent intentLogin = getIntent();
        loginType = intentLogin.getStringExtra("LOGIN_TYPE");
        if (loginType == null) {
            loginType = "paramedics"; // Default to 'paramedics' if null
        }

        if (user == null) {
            Toast.makeText(ParamedicsView.this, "Please log in first.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ParamedicsView.this, LoginView.class);
            intent.putExtra("LOGIN_TYPE", "paramedics");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Initialize DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.paramedics_drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        if (loginType.equals("paramedics")) {
            Menu menu = navigationView.getMenu();
            MenuItem hospitalStaffItem = menu.findItem(R.id.nav_hospital_staff);
            if (hospitalStaffItem != null) {
                hospitalStaffItem.setVisible(false);
            }
        }

        // Get the header view from the NavigationView
        View headerView = navigationView.getHeaderView(0);

        userEmailTextView = headerView.findViewById(R.id.user_email);
        userNameTextView = headerView.findViewById(R.id.user_name);
        signOutBtn = headerView.findViewById(R.id.signOut); // Initialize signOutBtn here

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

        // Set navigation item selection listener
        navigationView.setNavigationItemSelectedListener(this);

        // Set up hamburger menu icon click listener
        ImageView hamburgerMenu = findViewById(R.id.hamburger_menu);
        hamburgerMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        paramedicsController = new ParamedicsController(this, this);
        setupUI();

        Intent intentPatientModel = getIntent();
        patientModel = intentPatientModel.getParcelableExtra("patientModel");
        hospitalModel = intentPatientModel.getParcelableExtra("hospitalModel");

        // EditForm Button setup
        if (patientModel != null) {
            editFormBtn.setVisibility(View.VISIBLE);
            editFormBtn.setOnClickListener(view -> {
                // Start EditFormView and pass both PatientModel and HospitalModel
                Intent intent = new Intent(this, EditFormView.class);
                intent.putExtra("patientModel", patientModel);
                intent.putExtra("hospitalModel", hospitalModel);
                startActivityForResult(intent, REQUEST_EDIT_FORM);
            });
        } else {
            editFormBtn.setVisibility(View.GONE);
        }

        // Request location permissions if not granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1000);
        } else {
            paramedicsController.fetchHospitalData();
        }

        searchHospital.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                paramedicsController.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        searchHospital.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (searchHospital.getRight() - searchHospital.getCompoundDrawables()[2].getBounds().width())) {
                    promptSpeechInput();
                    return true;
                }
            }
            return false;
        });

        newPatientBtn.setOnClickListener(view -> paramedicsController.startNewPatientForm());

        signOutBtn.setOnClickListener(view -> {
            Intent serviceIntent = new Intent(this, LocationUpdateService.class);
            stopService(serviceIntent);

            mAuth.signOut();
            Intent intent = new Intent(ParamedicsView.this, LoginView.class);
            intent.putExtra("LOGIN_TYPE", "paramedics");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
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


    private void setupUI() {
        recyclerView = findViewById(R.id.recyclerView);
        searchHospital = findViewById(R.id.searchHospital);
        newPatientBtn = findViewById(R.id.newPatientBtn);
        editFormBtn = findViewById(R.id.editFormBtn); // Ensure editFormBtn is initialized here

        paramedicsAdapter = new ParamedicsAdapter(this, paramedicsController.getHospitalList(), this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(paramedicsAdapter);
    }

    @Override
    public void onSelectedHospitalClicked(HospitalModel hospitalModel) {
        if (patientModel != null) {
            paramedicsController.sendPatientFormToHospital(patientModel, hospitalModel);
        } else {
            Toast.makeText(this, "Patient data is missing.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNavigate(HospitalModel hospitalModel) {
        String selectedHospitalId = hospitalModel.getId();

        // Call the controller to update other hospitals
        paramedicsController.updateOtherHospitalsWithFinalDecision(selectedHospitalId);

        // Proceed with navigation
        Intent intent = new Intent(ParamedicsView.this, HospitalDirectionActivityView.class);
        intent.putExtra("hospital_model", hospitalModel);
        startActivity(intent);
    }

    @Override
    public void onItemClicked(HospitalModel hospitalModel) {
        // Handle hospital item click if needed
    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Say the hospital name");
        try {
            speechInputLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this,"Speech input not supported on this device",Toast.LENGTH_SHORT).show();
        }
    }

    // Implement HospitalDataListener methods
    @Override
    public void onHospitalDataChanged() {
        // Only update the adapter if a real data change occurred
        if (paramedicsAdapter != null) {
            paramedicsAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPatientFormSent(HospitalModel hospitalModel) {
        Toast.makeText(this, "Patient form sent to " + hospitalModel.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPatientStatusUpdated(HospitalModel hospitalModel) {
        // Ensure the status only triggers the toast if it is a meaningful change
        String acceptanceStatus = hospitalModel.getIsAccepted();
        String rejectionStatus = hospitalModel.getIsRejected();

        // Check if the patient was accepted
        if ("true".equals(acceptanceStatus) && !acceptanceStatus.equals(previousAcceptanceStatus)) {
            Toast.makeText(this, "Patient accepted by " + hospitalModel.getIsAcceptedBy(), Toast.LENGTH_SHORT).show();
            previousAcceptanceStatus = acceptanceStatus; // Store the previous status to prevent duplicate toasts
        }
        // Check if the patient was rejected
        else if ("true".equals(rejectionStatus) && !rejectionStatus.equals(previousRejectionStatus)) {
            Toast.makeText(this, "Patient rejected by " + hospitalModel.getIsRejectedBy(), Toast.LENGTH_SHORT).show();
            previousRejectionStatus = rejectionStatus; // Store the previous rejection status
        }

        // Notify adapter of data changes (to update the view)
        paramedicsAdapter.notifyDataSetChanged();
    }

    // Handle location permission result
    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode == 1000){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                paramedicsController.fetchHospitalData();
            } else {
                Toast.makeText(this, "Location permission is required to fetch hospital data.", Toast.LENGTH_SHORT).show();
            }
        }
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
                                        intent.putExtra("LOGIN_TYPE", "paramedics");
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EDIT_FORM && resultCode == RESULT_OK) {
            patientModel = data.getParcelableExtra("patientModel");
            hospitalModel = data.getParcelableExtra("hospitalModel");
        } else if (requestCode == REQUEST_CODE_SETTINGS) {
            // Handle any specific actions if needed
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Returned from SettingsView with success.", Toast.LENGTH_SHORT).show();
            }
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



    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        drawerLayout.closeDrawer(GravityCompat.END);

        int id = item.getItemId();

        if (id == R.id.nav_settings) {
            Intent intent = new Intent(this, SettingsView.class);
            intent.putExtra("LOGIN_TYPE", loginType); // Pass LOGIN_TYPE to SettingsView
            startActivityForResult(intent, REQUEST_CODE_SETTINGS);
        } else if (id == R.id.nav_delete_account) {
            showDeleteAccountDialog();
        }

        return true;
    }
}
