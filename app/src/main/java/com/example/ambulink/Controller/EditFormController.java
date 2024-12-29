package com.example.ambulink.Controller;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ambulink.Model.HospitalModel;
import com.example.ambulink.Model.PatientModel;
import com.example.ambulink.View.EditFormView;
import com.example.ambulink.View.ParamedicsView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.List;

public class EditFormController {
    //hello
    private Context context;
    private FirebaseFirestore fStore;

    public EditFormController(Context context) {
        this.context = context;
        this.fStore = FirebaseFirestore.getInstance();
    }

    // Method to update patient information and return to ParamedicsView
    public void updatePatientForm(PatientModel patientModel, HospitalModel hospitalModel) {
        // Display confirmation toast without checking for patientId
        Toast.makeText(context, "Patient data updated successfully (locally)", Toast.LENGTH_SHORT).show();

        // Create a lightweight PatientModel with only required fields (excluding patientId)
        PatientModel lightweightPatientModel = new PatientModel();
        lightweightPatientModel.setFirstName(patientModel.getFirstName());
        lightweightPatientModel.setLastName(patientModel.getLastName());
        lightweightPatientModel.setAge(patientModel.getAge());
        lightweightPatientModel.setChiefComplaint(patientModel.getChiefComplaint());
        lightweightPatientModel.setNotes(patientModel.getNotes());

        // Return the lightweight patient model as the result
        if (context instanceof EditFormView) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("patientModel", lightweightPatientModel); // Pass only essential data
            ((EditFormView) context).setResult(AppCompatActivity.RESULT_OK, resultIntent);
            ((EditFormView) context).finish();  // Close EditFormView and pass data back
        }
    }




    public boolean validateFields(String firstName, String lastName, String ageInput, String chiefComplaint, String bloodPressure, List<String> notes) {
        return !(firstName.isEmpty() || lastName.isEmpty() || ageInput.isEmpty() || chiefComplaint.isEmpty() || bloodPressure.isEmpty() || notes.isEmpty() || notes.get(0).isEmpty());
    }

    public double parseDouble(String value, double defaultValue) {
        return value.isEmpty() ? defaultValue : Double.parseDouble(value);
    }

    public int parseInt(String value, int defaultValue) {
        return value.isEmpty() ? defaultValue : Integer.parseInt(value);
    }
}
