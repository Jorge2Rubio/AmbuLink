package com.example.ambulink.Controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.example.ambulink.utils.LoadingDialog;
import com.example.ambulink.View.LoginView;
import com.example.ambulink.View.RegisterView;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class RegisterController {

    private static final String TAG = "RegisterController";

    private FirebaseAuth mAuth;
    private FirebaseFirestore fStore;
    private Context context;
    private Activity activity;
    private LoadingDialog loadingDialog;
    private String registerType;

    public RegisterController(Activity activity, Context context, String registerType) {
        this.context = context;
        this.activity = activity;
        this.registerType = registerType;
        initializeFirebase();
        loadingDialog = new LoadingDialog(activity);
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
    }

    public void logOutUser(){
        mAuth.signOut();
    }

    // Validate input for paramedics
    public boolean validateParamedicInput(String fullName, String email, String password, String phone) {
        return validateCommonInput(fullName, email, password, phone);
    }

    // Validate input for hospital staff, assumes a selected hospital ID is required
    public boolean validateHospitalInput(String fullName, String email, String password, String phone, String hospitalId) {
        if (validateCommonInput(fullName, email, password, phone)) {
            if (hospitalId == null || hospitalId.isEmpty()) {
                ((RegisterView) context).showToast("Please select a hospital.");
                return false;
            }
            return true;
        }
        return false;
    }

    // Common input validation logic used by both roles
    private boolean validateCommonInput(String fullName, String email, String password, String phone) {
        if (fullName.isEmpty()) {
            ((RegisterView) context).showToast("Full name is required.");
            return false;
        }
        if (email.isEmpty()) {
            ((RegisterView) context).showToast("Email is required.");
            return false;
        }
        if (password.isEmpty() || password.length() < 6) {
            ((RegisterView) context).showToast("Password must be at least 6 characters.");
            return false;
        }
        if (phone.isEmpty() || phone.length() != 13) {
            ((RegisterView) context).showToast("Phone number must be 12 digits.");
            return false;
        }
        return true;
    }

    // Check if user exists for paramedics
    public void checkIfParamedicExists(String fullName, String phone, String email, String password) {
        checkUserExistence(fullName, phone, email, () -> registerNewParamedic(fullName, phone, email, password));
    }

    // Check if user exists for hospital staff
    public void checkIfHospitalExists(String fullName, String phone, String email, String password, String hospitalId) {
        checkUserExistence(fullName, phone, email, () -> registerNewHospitalStaff(fullName, phone, email, password, hospitalId));
    }

    // Common check for user existence with callback for further actions
    private void checkUserExistence(String fullName, String phone, String email, Runnable onSuccess) {
        loadingDialog.show("Wait..");
        CollectionReference usersRef = fStore.collection("users");

        Task<QuerySnapshot> fullNameQuery = usersRef.whereEqualTo("fullName", fullName).get();
        Task<QuerySnapshot> phoneQuery = usersRef.whereEqualTo("phone", phone).get();
        Task<QuerySnapshot> emailQuery = usersRef.whereEqualTo("email", email).get();

        Tasks.whenAllComplete(fullNameQuery, phoneQuery, emailQuery).addOnCompleteListener(task -> {
            if (fullNameQuery.isSuccessful() && !fullNameQuery.getResult().isEmpty()) {
                loadingDialog.dismiss();
                ((RegisterView) context).showToast("User already exists with this full name.");
            } else if (phoneQuery.isSuccessful() && !phoneQuery.getResult().isEmpty()) {
                loadingDialog.dismiss();
                ((RegisterView) context).showToast("User already exists with this phone number.");
            } else if (emailQuery.isSuccessful() && !emailQuery.getResult().isEmpty()) {
                loadingDialog.dismiss();
                ((RegisterView) context).showToast("User already exists with this email.");
            } else {
                onSuccess.run();
            }
        });
    }

    // Register new paramedic user
    private void registerNewParamedic(String fullName, String phone, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        addUserToFirestore(fullName, phone, email, "paramedic", null);
                    } else {
                        loadingDialog.dismiss();
                        ((RegisterView) context).showToast("Registration failed: " + getFirebaseError(task));
                    }
                });
    }

    // Register new hospital staff user
    private void registerNewHospitalStaff(String fullName, String phone, String email, String password, String hospitalId) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        addUserToFirestore(fullName, phone, email, "hospital_staff", hospitalId);
                    } else {
                        loadingDialog.dismiss();
                        ((RegisterView) context).showToast("Registration failed: " + getFirebaseError(task));
                    }
                });
    }

    // Add user to Firestore with specified role and hospital ID (if applicable)
    private void addUserToFirestore(String fullName, String phone, String email, String role, String hospitalId) {
        DocumentReference docRef = fStore.collection("users").document(mAuth.getCurrentUser().getUid());
        Map<String, Object> user = new HashMap<>();
        user.put("fullName", fullName);
        user.put("phone", phone);
        user.put("email", email);
        user.put("role", role);
        if (hospitalId != null) user.put("hospitalID", hospitalId);

        docRef.set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User added to Firestore");
                    mAuth.signOut();
                    Intent intent = new Intent(context, LoginView.class);
                    intent.putExtra("LOGIN_TYPE", role.equals("paramedic") ? "paramedics" : "hospital");
                    context.startActivity(intent);
                    activity.finish();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error adding user to Firestore", e));
    }

    private String getFirebaseError(Task<AuthResult> task) {
        if (task.getException() instanceof FirebaseAuthException) {
            return ((FirebaseAuthException) task.getException()).getErrorCode();
        }
        return "Unknown error";
    }
}
