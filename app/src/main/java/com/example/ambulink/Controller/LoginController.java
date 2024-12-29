package com.example.ambulink.Controller;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.example.ambulink.R;
import com.example.ambulink.View.LoginView;
import com.example.ambulink.utils.LoadingDialog;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

public class LoginController {

    private FirebaseAuth mAuth;
    private FirebaseFirestore fStore;
    private FirebaseUser mCurrentUser;
    private WeakReference<Activity> activityRef;
    private String verificationId;
    private LoadingDialog loadingDialog;
    private long lastCodeSentTime = 0;


    public LoginController(Activity activity) {
        this.activityRef = new WeakReference<>(activity);
        mAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        loadingDialog = new LoadingDialog(activity);
    }

    private Activity getActivity() {
        return activityRef.get();
    }

    private boolean isActivityAlive(Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    public void handleLogin(EditText emailField, EditText passwordField, String loginType) {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) return;

        String userEmail = emailField.getText().toString().trim();
        String userPassword = passwordField.getText().toString().trim();

        if (userEmail.isEmpty() || userPassword.isEmpty()) {
            ((LoginView) activity).showToast("Please fill in both email and password.");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            ((LoginView) activity).showToast("Please enter a valid email address.");
            return;
        }

        if (userPassword.length() < 6) {
            ((LoginView) activity).showToast("Password must be at least 6 characters long.");
            return;
        }



        if (!loadingDialog.isShowing()) loadingDialog.show("Logging in...");

        mAuth.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(task -> {
                    if (loadingDialog.isShowing()) loadingDialog.dismiss();

                    if (task.isSuccessful()) {
                        mCurrentUser = mAuth.getCurrentUser();
                        if (mCurrentUser != null && mCurrentUser.isEmailVerified()) {
                            if (mCurrentUser.getMultiFactor().getEnrolledFactors().size() > 0) {
                                startMultiFactorAuthentication();
                            } else {
                                fetchPhoneNumberAndEnrollMFA(mCurrentUser);
                            }
                        } else {
                            promptEmailVerification();
                        }
                    } else {
                        handleError(task.getException());
                    }
                });
    }

    private void handleError(Exception e) {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) return;

        if (e instanceof FirebaseAuthMultiFactorException) {
            FirebaseAuthMultiFactorException mfaException = (FirebaseAuthMultiFactorException) e;
            handleMultiFactorAuthentication(mfaException);
        } else {
            Toast.makeText(activity, "Email or password is incorrect.", Toast.LENGTH_SHORT).show();
            Log.e("LoginError", "Authentication failed.");
        }
    }



    private void startMultiFactorAuthentication() {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) return;

        if (!loadingDialog.isShowing()) loadingDialog.show("Starting multi-factor authentication...");

        mCurrentUser.getMultiFactor().getSession()
                .addOnCompleteListener(task -> {
                    if (loadingDialog.isShowing()) loadingDialog.dismiss();

                    if (task.isSuccessful()) {
                        MultiFactorSession mfaSession = task.getResult();
                        handleMultiFactorSignIn(mfaSession);
                    } else {
                        Toast.makeText(activity, "Failed to start multi-factor authentication.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void handleMultiFactorSignIn(MultiFactorSession mfaSession) {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) {
            return;
        }

        if (!loadingDialog.isShowing()) {
            loadingDialog.show("Sending verification code...");
        }

        if (mCurrentUser.getMultiFactor().getEnrolledFactors().size() > 0) {
            MultiFactorInfo secondFactor = mCurrentUser.getMultiFactor().getEnrolledFactors().get(0);

            if (secondFactor instanceof PhoneMultiFactorInfo) {
                PhoneMultiFactorInfo phoneInfo = (PhoneMultiFactorInfo) secondFactor;

                PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                        .setMultiFactorHint(phoneInfo)
                        .setMultiFactorSession(mfaSession)
                        .setActivity(activity)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                if (loadingDialog.isShowing()) {
                                    loadingDialog.dismiss();
                                }
                                verifySecondFactor(credential);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                if (loadingDialog.isShowing()) {
                                    loadingDialog.dismiss();
                                }
                                Toast.makeText(activity, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                if (loadingDialog.isShowing()) {
                                    loadingDialog.dismiss();
                                }
                                LoginController.this.verificationId = verificationId;
                                promptForVerificationCode();
                            }
                        })
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .build();

                PhoneAuthProvider.verifyPhoneNumber(options);
            }
        }
    }

    private void handleMultiFactorAuthentication(FirebaseAuthMultiFactorException mfaException) {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) {
            return;
        }

        MultiFactorResolver resolver = mfaException.getResolver();
        MultiFactorInfo secondFactor = resolver.getHints().get(0);

        if (secondFactor instanceof PhoneMultiFactorInfo) {
            PhoneMultiFactorInfo phoneInfo = (PhoneMultiFactorInfo) secondFactor;

            if (!loadingDialog.isShowing()) {
                loadingDialog.show("Sending verification code...");
            }

            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                    .setMultiFactorHint(phoneInfo)
                    .setMultiFactorSession(resolver.getSession())
                    .setActivity(activity)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                            if (loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }
                            verifySecondFactor(resolver, credential);
                        }

                        @Override
                        public void onVerificationFailed(@NonNull FirebaseException e) {
                            if (loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }
                            Toast.makeText(activity, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                            if (loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }
                            LoginController.this.verificationId = verificationId;
                            promptForVerificationCode(resolver);
                        }
                    })
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .build();

            PhoneAuthProvider.verifyPhoneNumber(options);
        }
    }

    private void promptForVerificationCode() {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View customLayout = activity.getLayoutInflater().inflate(R.layout.dialog_verification_code, null);
        builder.setView(customLayout);
        AlertDialog dialog = builder.create();
        dialog.setTitle("Multi-Factor Authentication");

        final EditText codeInput = customLayout.findViewById(R.id.verificationCodeInput);
        Button btnVerify = customLayout.findViewById(R.id.btnVerify);
        Button btnCancel = customLayout.findViewById(R.id.btnCancel);
        TextView resendCodeText = customLayout.findViewById(R.id.resendCodeText);

        btnVerify.setOnClickListener(v -> {
            if (!isActivityAlive(activity)) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                return;
            }
            String verificationCode = codeInput.getText().toString().trim();
            if (!verificationCode.isEmpty()) {
                if (!loadingDialog.isShowing()) {
                    loadingDialog.show("Verifying code...");
                }

                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, verificationCode);
                verifySecondFactor(credential);
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            } else {
                Toast.makeText(activity, "Please enter the verification code.", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        });

        resendCodeText.setOnClickListener(v -> resendVerificationCode());

        if (isActivityAlive(activity)) {
            dialog.show();
        }
    }

    private void promptForVerificationCode(MultiFactorResolver resolver) {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View customLayout = activity.getLayoutInflater().inflate(R.layout.dialog_verification_code, null);
        builder.setView(customLayout);
        AlertDialog dialog = builder.create();
        dialog.setTitle("Multi-Factor Authentication");

        final EditText codeInput = customLayout.findViewById(R.id.verificationCodeInput);
        Button btnVerify = customLayout.findViewById(R.id.btnVerify);
        Button btnCancel = customLayout.findViewById(R.id.btnCancel);
        TextView resendCodeText = customLayout.findViewById(R.id.resendCodeText);

        btnVerify.setOnClickListener(v -> {
            if (!isActivityAlive(activity)) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                return;
            }
            String verificationCode = codeInput.getText().toString().trim();
            if (!verificationCode.isEmpty()) {
                if (!loadingDialog.isShowing()) {
                    loadingDialog.show("Verifying code...");
                }

                PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, verificationCode);
                verifySecondFactor(resolver, credential);
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            } else {
                Toast.makeText(activity, "Please enter the verification code.", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        });

        resendCodeText.setOnClickListener(v -> resendVerificationCode(resolver));

        if (isActivityAlive(activity)) {
            dialog.show();
        }
    }

    private void verifySecondFactor(PhoneAuthCredential credential) {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) {
            return;
        }

        if (!loadingDialog.isShowing()) {
            loadingDialog.show("Verifying code...");
        }

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(signInTask -> {
                    if (loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }

                    if (signInTask.isSuccessful()) {
                        Toast.makeText(activity, "Multi-factor authentication successful", Toast.LENGTH_SHORT).show();

                        mCurrentUser = signInTask.getResult().getUser();

                        if (mCurrentUser != null) {
                            checkUserRole(mCurrentUser.getUid(), ((LoginView) activity).getLoginType());
                        } else {
                            Toast.makeText(activity, "Failed to get current user.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(activity, "Failed to verify second factor: " + signInTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void verifySecondFactor(MultiFactorResolver resolver, PhoneAuthCredential credential) {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) {
            return;
        }

        if (!loadingDialog.isShowing()) {
            loadingDialog.show("Verifying code...");
        }

        PhoneMultiFactorAssertion assertion = PhoneMultiFactorGenerator.getAssertion(credential);

        resolver.resolveSignIn(assertion)
                .addOnCompleteListener(signInTask -> {
                    if (loadingDialog.isShowing()) {
                        loadingDialog.dismiss();
                    }

                    if (signInTask.isSuccessful()) {
                        Toast.makeText(activity, "Multi-factor authentication successful", Toast.LENGTH_SHORT).show();

                        mCurrentUser = signInTask.getResult().getUser();

                        if (mCurrentUser != null) {
                            checkUserRole(mCurrentUser.getUid(), ((LoginView) activity).getLoginType());
                        } else {
                            Toast.makeText(activity, "Failed to get current user.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(activity, "Failed to verify second factor: " + signInTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resendVerificationCode() {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) {
            return;
        }

        if (System.currentTimeMillis() - lastCodeSentTime < 60000) { // 1-minute interval
            Toast.makeText(activity, "Please wait before resending the code.", Toast.LENGTH_SHORT).show();
            return;
        }
        lastCodeSentTime = System.currentTimeMillis();

        if (!loadingDialog.isShowing()) {
            loadingDialog.show("Resending verification code...");
        }

        if (mCurrentUser.getMultiFactor().getEnrolledFactors().size() > 0) {
            MultiFactorInfo secondFactor = mCurrentUser.getMultiFactor().getEnrolledFactors().get(0);

            if (secondFactor instanceof PhoneMultiFactorInfo) {
                PhoneMultiFactorInfo phoneInfo = (PhoneMultiFactorInfo) secondFactor;

                mCurrentUser.getMultiFactor().getSession()
                        .addOnCompleteListener(task -> {
                            if (loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }

                            if (task.isSuccessful()) {
                                MultiFactorSession mfaSession = task.getResult();

                                PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                                        .setMultiFactorHint(phoneInfo)
                                        .setMultiFactorSession(mfaSession)
                                        .setActivity(activity)
                                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                            @Override
                                            public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                                                if (loadingDialog.isShowing()) {
                                                    loadingDialog.dismiss();
                                                }
                                                verifySecondFactor(credential);
                                            }

                                            @Override
                                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                                if (loadingDialog.isShowing()) {
                                                    loadingDialog.dismiss();
                                                }
                                                Toast.makeText(activity, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }

                                            @Override
                                            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                                                if (loadingDialog.isShowing()) {
                                                    loadingDialog.dismiss();
                                                }
                                                LoginController.this.verificationId = verificationId;
                                                Toast.makeText(activity, "Verification code resent.", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .setTimeout(60L, TimeUnit.SECONDS)
                                        .build();

                                PhoneAuthProvider.verifyPhoneNumber(options);
                            } else {
                                Toast.makeText(activity, "Failed to get multi-factor session.", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                if (loadingDialog.isShowing()) {
                    loadingDialog.dismiss();
                }
                Toast.makeText(activity, "Cannot resend verification code: second factor is not a phone.", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
            Toast.makeText(activity, "No second factors enrolled.", Toast.LENGTH_SHORT).show();
        }
    }

    private void resendVerificationCode(MultiFactorResolver resolver) {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) {
            return;
        }

        if (!loadingDialog.isShowing()) {
            loadingDialog.show("Resending verification code...");
        }

        MultiFactorInfo secondFactor = resolver.getHints().get(0);

        if (secondFactor instanceof PhoneMultiFactorInfo) {
            PhoneMultiFactorInfo phoneInfo = (PhoneMultiFactorInfo) secondFactor;

            PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                    .setMultiFactorHint(phoneInfo)
                    .setMultiFactorSession(resolver.getSession())
                    .setActivity(activity)
                    .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        @Override
                        public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                            if (loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }
                            verifySecondFactor(resolver, credential);
                        }

                        @Override
                        public void onVerificationFailed(@NonNull FirebaseException e) {
                            if (loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }
                            Toast.makeText(activity, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                            if (loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }
                            LoginController.this.verificationId = verificationId;
                            Toast.makeText(activity, "Verification code resent.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .build();

            PhoneAuthProvider.verifyPhoneNumber(options);
        } else {
            if (loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
            Toast.makeText(activity, "Cannot resend verification code: second factor is not a phone.", Toast.LENGTH_SHORT).show();
        }
    }

    private void promptEmailVerification() {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Email Verification Required");
        builder.setMessage("Your email address is not verified. Would you like us to send a verification email?");

        builder.setPositiveButton("Send Email", (dialog, which) -> sendEmailVerification());

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.cancel();
            mAuth.signOut();
        });

        builder.show();
    }

    private void sendEmailVerification() {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) {
            return;
        }

        try {
            mCurrentUser.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(activity, "Verification email sent. Please verify and sign in again.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                            if (activity instanceof LoginView) {
                                ((LoginView) activity).navigateToMainActivity();
                            }
                        } else {
                            Toast.makeText(activity, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(activity, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
            Log.e("EmailVerificationError", e.getMessage());
        }
    }

    private void checkUserRole(String uid, String loginType) {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) return;

        DocumentReference docRef = fStore.collection("users").document(uid);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String role = document.getString("role");
                    String hospitalId = document.getString("hospitalID");

                    if ("hospital_staff".equals(role)) {
                        if ("hospital".equals(loginType)) {
                            ((LoginView) activity).navigateToHospital(hospitalId);
                        } else {
                            showInvalidAccessToast();
                        }
                    } else if ("paramedic".equals(role)) {
                        if ("paramedics".equals(loginType)) {
                            ((LoginView) activity).navigateToParamedics();
                        } else {
                            showInvalidAccessToast();
                        }
                    } else {
                        showUnknownRoleToast();
                    }

                } else {
                    ((LoginView) activity).showToast("User data not found.");
                }
            } else {
                ((LoginView) activity).showToast("Failed to fetch user data.");
            }
        });
    }


    private void fetchPhoneNumberAndEnrollMFA(FirebaseUser user) {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) return;

        fStore.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String phoneNumber = documentSnapshot.getString("phone");
                        if (phoneNumber != null && !phoneNumber.isEmpty()) {
                            navigateToMfaEnrollment(phoneNumber);
                        } else {
                            Toast.makeText(activity, "Phone number not found. Please update your profile.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(activity, "User data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(activity, "Failed to fetch phone number: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    public void navigateToMfaEnrollment(String userPhoneNumber) {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            if (!loadingDialog.isShowing()) loadingDialog.show("Starting MFA enrollment...");

            user.getMultiFactor().getSession()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (loadingDialog.isShowing()) loadingDialog.dismiss();
                            MultiFactorSession session = task.getResult();
                            sendVerificationCodeForEnrollment(session, userPhoneNumber);
                        } else {
                            if (loadingDialog.isShowing()) loadingDialog.dismiss();
                            Toast.makeText(activity, "Failed to get multi-factor session.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void sendVerificationCodeForEnrollment(MultiFactorSession session, String userPhoneNumber) {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) {
            return;
        }

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(userPhoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        enrollInMfa(credential);
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(activity, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        LoginController.this.verificationId = verificationId;
                        promptForEnrollmentVerificationCode();
                    }
                })
                .setMultiFactorSession(session)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void promptForEnrollmentVerificationCode() {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View customLayout = activity.getLayoutInflater().inflate(R.layout.dialog_verification_code, null);
        builder.setView(customLayout);

        AlertDialog dialog = builder.create();
        dialog.setTitle("Enroll in Multi-Factor Authentication");

        EditText codeInput = customLayout.findViewById(R.id.verificationCodeInput);
        Button btnVerify = customLayout.findViewById(R.id.btnVerify);
        Button btnCancel = customLayout.findViewById(R.id.btnCancel);
        TextView resendCodeText = customLayout.findViewById(R.id.resendCodeText);

        btnVerify.setOnClickListener(v -> {
            String code = codeInput.getText().toString().trim();
            if (!code.isEmpty()) {
                enrollInMfa(code);
                dialog.dismiss();
            } else {
                Toast.makeText(activity, "Please enter the verification code.", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        resendCodeText.setOnClickListener(v -> resendEnrollmentVerificationCode());

        dialog.show();
    }

    private void enrollInMfa(String code) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Activity activity = getActivity();
        if (user != null) {
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            enrollInMfa(credential);
        }
    }

    private void enrollInMfa(PhoneAuthCredential credential) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        Activity activity = getActivity();

        if (user != null) {
            PhoneMultiFactorAssertion assertion = PhoneMultiFactorGenerator.getAssertion(credential);

            user.getMultiFactor().enroll(assertion, "Phone MFA")
                    .addOnCompleteListener(enrollTask -> {
                        if (enrollTask.isSuccessful()) {
                            Toast.makeText(activity, "MFA enrollment successful", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(activity, "MFA enrollment failed: " + enrollTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void resendEnrollmentVerificationCode() {
        // This method should resend the verification code during MFA enrollment
        // You can call sendVerificationCodeForEnrollment() again if needed
    }

    private void showInvalidAccessToast() {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) return;

        Toast.makeText(activity, "Invalid access.", Toast.LENGTH_SHORT).show();
        mAuth.signOut();
        ((LoginView) activity).navigateToMainActivity();
    }

    private void showUnknownRoleToast() {
        Activity activity = getActivity();
        if (!isActivityAlive(activity)) return;

        Toast.makeText(activity, "Unknown role or invalid access.", Toast.LENGTH_SHORT).show();
        mAuth.signOut();
        ((LoginView) activity).navigateToMainActivity();
    }

    // Clean up resources
    public void cleanUp() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        activityRef.clear();
    }
}
