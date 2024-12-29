package com.example.ambulink.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.example.ambulink.R;

public class LoadingDialog {

    final private Activity activity;
    private AlertDialog dialog;

    public LoadingDialog(Activity activity) {
        this.activity = activity;
    }

    public void show(String message) {
        if (activity.isFinishing()) return; // Prevent showing dialog if activity is finishing

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_loading, null);
        builder.setView(view);
        builder.setCancelable(false); // Disallow dismissing by tapping outside or back button

        TextView loadingMessage = view.findViewById(R.id.loadingMessage);
        loadingMessage.setText(message);

        dialog = builder.create();
        dialog.show();
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }
}
