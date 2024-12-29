package com.example.ambulink.View;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.ambulink.R;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthMultiFactorException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.MultiFactorInfo;
import com.google.firebase.auth.MultiFactorResolver;
import com.google.firebase.auth.MultiFactorSession;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.PhoneMultiFactorAssertion;
import com.google.firebase.auth.PhoneMultiFactorGenerator;
import com.google.firebase.auth.PhoneMultiFactorInfo;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SettingsView extends AppCompatActivity {

    private EditText editFullName, editEmail, editPhone, editPassword, editCurrentPassword;
    private Button saveChangesButton;
    private TextView backBtn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;
    private String verificationId;

    private String fullName;
    private String email;
    private String phone;
    private String password;
    private String currentPassword;
    private String loginType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_view);

        editFullName = findViewById(R.id.edit_full_name);
        editEmail = findViewById(R.id.edit_email);
        editPhone = findViewById(R.id.edit_phone);
        editPassword = findViewById(R.id.edit_password);
        editCurrentPassword = findViewById(R.id.edit_current_password);
        saveChangesButton = findViewById(R.id.save_changes_button);
        backBtn = findViewById(R.id.backBtn);

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        Intent intent = getIntent();
        loginType = intent.getStringExtra("LOGIN_TYPE");


        // Pre-fill user data
        if (currentUser != null) {
            loadUserData();
        }

        backBtn.setOnClickListener(v -> navigateBack());

        saveChangesButton.setOnClickListener(v -> updateUserDetails());
    }

    private void loadUserData() {
        if (currentUser != null) {
            String uid = currentUser.getUid();
            firestore.collection("users")
                    .document(uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String fullName = document.getString("fullName");
                                if (fullName != null) {
                                    editFullName.setText(fullName);
                                }
                                String phoneNumber = document.getString("phone");
                                if (phoneNumber != null) {
                                    editPhone.setText(phoneNumber);
                                }
                                String email = document.getString("email");
                                if (email != null) {
                                    editEmail.setText(email);
                                }
                            } else {
                                Log.d("Firestore", "No such document");
                            }
                        } else {
                            Log.d("Firestore", "Error getting document: ", task.getException());
                            Toast.makeText(this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }



    private void updateUserDetails() {
        fullName = editFullName.getText().toString().trim();
        email = editEmail.getText().toString().trim();
        phone = editPhone.getText().toString().trim();
        password = editPassword.getText().toString().trim();
        currentPassword = editCurrentPassword.getText().toString().trim();

        if (TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(this, "Please enter your current password.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reauthenticate the user
        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);

        currentUser.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Proceed with updates
                    updateSensitiveData();
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthMultiFactorException) {
                        handleMultiFactorAuthentication((FirebaseAuthMultiFactorException) e);
                    } else {
                        Log.e("ReauthError", "Reauthentication failed", e);
                        Toast.makeText(this, "Reauthentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void handleMultiFactorAuthentication(FirebaseAuthMultiFactorException e) {
        MultiFactorResolver resolver = e.getResolver();

        if (resolver == null || resolver.getHints().isEmpty()) {
            Toast.makeText(this, "No second factor enrolled.", Toast.LENGTH_SHORT).show();
            return;
        }

        MultiFactorInfo secondFactor = resolver.getHints().get(0);

        if (secondFactor instanceof PhoneMultiFactorInfo) {
            PhoneMultiFactorInfo phoneInfo = (PhoneMultiFactorInfo) secondFactor;

            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                    .setMultiFactorHint(phoneInfo)
                    .setMultiFactorSession(resolver.getSession())
                    .setActivity(this)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                            PhoneMultiFactorAssertion assertion = PhoneMultiFactorGenerator.getAssertion(credential);
                            resolver.resolveSignIn(assertion)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(SettingsView.this, "MFA challenge completed.", Toast.LENGTH_SHORT).show();
                                        // Proceed with updates after MFA
                                        updateSensitiveData();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("MFAError", "Failed to resolve MFA", e);
                                        Toast.makeText(SettingsView.this, "Failed to complete MFA challenge.", Toast.LENGTH_SHORT).show();
                                    });
                        }

                        @Override
                        public void onVerificationFailed(@NonNull FirebaseException e) {
                            Log.e("MFA", "Verification failed", e);
                            Toast.makeText(SettingsView.this, "Verification failed. Please try again.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                            SettingsView.this.verificationId = verificationId;
                            promptForVerificationCode(resolver);
                        }
                    })
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .build();

            PhoneAuthProvider.verifyPhoneNumber(options);
        }
    }


    private void promptForVerificationCode(MultiFactorResolver resolver) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_verification_code, null);
        builder.setView(customLayout);

        AlertDialog dialog = builder.create();
        dialog.setTitle("Multi-Factor Authentication");

        EditText codeInput = customLayout.findViewById(R.id.verificationCodeInput);
        Button btnVerify = customLayout.findViewById(R.id.btnVerify);

        btnVerify.setOnClickListener(v -> {
            String code = codeInput.getText().toString().trim();
            if (!TextUtils.isEmpty(code)) {
                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
                PhoneMultiFactorAssertion assertion = PhoneMultiFactorGenerator.getAssertion(credential);

                resolver.resolveSignIn(assertion)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "MFA challenge completed successfully!", Toast.LENGTH_SHORT).show();
                            // Proceed with updates after MFA
                            updateSensitiveData();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("MFAError", "Failed to resolve MFA", e);
                            Toast.makeText(this, "Failed to complete MFA challenge.", Toast.LENGTH_SHORT).show();
                        });

                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please enter the verification code.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }


    private void updateSensitiveData() {
        List<Task<?>> updateTasks = new ArrayList<>();

        // Update email in FirebaseAuth
        if (!TextUtils.isEmpty(email)) {
            Task<Void> emailUpdateTask = currentUser.verifyBeforeUpdateEmail(email)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Verification email sent to the new email address.", Toast.LENGTH_SHORT).show();
                        // Optional: Sign out the user if you want them to re-login after email verification
                        mAuth.signOut();
                        // Redirect to login screen or close activity
                        redirectToLogin();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("UpdateError", "Failed to send verification email", e);
                        Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                    });
            updateTasks.add(emailUpdateTask);
        }


        // Update password in FirebaseAuth (if applicable)
        if (!TextUtils.isEmpty(password)) {
            Task<Void> passwordUpdateTask = currentUser.updatePassword(password)
                    .addOnSuccessListener(a -> Toast.makeText(this, "Password updated.", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> {
                        Log.e("UpdateError", "Failed to update password", e);
                        Toast.makeText(this, "Failed to update password.", Toast.LENGTH_SHORT).show();
                    });
            updateTasks.add(passwordUpdateTask);
        }

        // Update Firestore document
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", fullName);
        updates.put("phone", phone);
        updates.put("email", email); // Add this line

        Task<Void> firestoreUpdateTask = firestore.collection("users")
                .document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Profile updated.", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Failed to update Firestore", e);
                    Toast.makeText(this, "Failed to update profile data.", Toast.LENGTH_SHORT).show();
                });
        updateTasks.add(firestoreUpdateTask);

        // Wait for all tasks to complete
        Tasks.whenAllComplete(updateTasks)
                .addOnSuccessListener(tasks -> {
                    boolean hasError = false;
                    for (Task<?> task : tasks) {
                        if (!task.isSuccessful()) {
                            hasError = true;
                            Exception e = task.getException();
                            if (e != null) {
                                Log.e("UpdateError", "Task failed with exception: ", e);
                            }
                        }
                    }

                    if (!hasError) {
                        Toast.makeText(this, "Profile updated successfully.", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, "Some updates failed. Please check logs.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UpdateError", "Failed to complete update tasks", e);
                    Toast.makeText(this, "Failed to update profile. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void redirectToLogin() {
        if ("paramedics".equals(loginType)) {
            Intent intent = new Intent(SettingsView.this, LoginView.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("LOGIN_TYPE", loginType); // Pass LOGIN_TYPE back if needed
            startActivity(intent);
            finish();
        } else if ("hospital".equals(loginType)) {
            Intent intent = new Intent(SettingsView.this, LoginView.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("LOGIN_TYPE", loginType); // Pass LOGIN_TYPE back if needed
            startActivity(intent);
            finish();
        } else {
            // Default behavior if LOGIN_TYPE is not recognized
            finish();
        }
    }


    private void navigateBack() {
        if ("paramedics".equals(loginType)) {
            Intent intent = new Intent(SettingsView.this, ParamedicsView.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("LOGIN_TYPE", loginType); // Pass LOGIN_TYPE back if needed
            setResult(RESULT_CANCELED);
            startActivity(intent);
            finish();
        } else if ("hospital".equals(loginType)) {
            Intent intent = new Intent(SettingsView.this, HospitalView.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra("LOGIN_TYPE", loginType);
            setResult(RESULT_CANCELED);
            startActivity(intent);
            finish();
        } else {
            // Default behavior if LOGIN_TYPE is not recognized
            finish();
        }
    }


}
