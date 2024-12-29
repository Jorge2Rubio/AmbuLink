package com.example.ambulink.Controller;

import android.content.Context;
import android.util.Log;

import com.example.ambulink.View.ForgotPasswordView;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordController {

    private FirebaseAuth auth;
    private Context context;
    private static final String TAG = "ForgotPasswordController";

    public ForgotPasswordController(Context context) {
        this.context = context;
        this.auth = FirebaseAuth.getInstance();
    }

    public void sendPasswordResetEmail(String emailAddress) {
        if (emailAddress.isEmpty()) {
            ((ForgotPasswordView) context).showErrorMessage("Email address cannot be empty.");
            return;
        }

        auth.sendPasswordResetEmail(emailAddress)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ((ForgotPasswordView) context).showSuccessMessage();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Log.w(TAG, "Error: ", task.getException());
                        ((ForgotPasswordView) context).showErrorMessage(error);
                    }
                });
    }
}
