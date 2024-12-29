package com.example.ambulink.View;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ambulink.Adapter.NotesAdapter;
import com.example.ambulink.Controller.EditFormController;
import com.example.ambulink.Model.HospitalModel;
import com.example.ambulink.Model.PatientModel;
import com.example.ambulink.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EditFormView extends AppCompatActivity  implements NotesAdapter.OnSpeechRequestListener {

    private EditText pFName, pLName, pAge, pReligion, pCComplaint, pSAS, pAllergies, pMedications,
            pPastMedicalH, pLastOralI, pELTPI, pOxygenS, pRespRate, pHeartRate, pBodyTemp, pBloodPressure;
    private Spinner spinnerSex;
    private NotesAdapter notesAdapter;
    private EditFormController editFormController;
    private PatientModel patientModel;
    private HospitalModel hospitalModel;
    private EditText currentEditText;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_form);

        initializeFields();
        setupSpinner();

        // Initialize the controller
        editFormController = new EditFormController(this);

        // Get patient and hospital data from intent
        Intent intent = getIntent();
        patientModel = intent.getParcelableExtra("patientModel");
        hospitalModel = intent.getParcelableExtra("hospitalModel");

        // Populate fields and set up the RecyclerView with notes
        if (patientModel != null) {
            populateFields();
            setupRecyclerView();
        } else {
            Log.e("EditFormView", "Patient data is missing in intent");
        }

        Button updateBtn = findViewById(R.id.pSubmit);
        updateBtn.setOnClickListener(view -> updatePatientData());
    }

    private void initializeFields() {
        pFName = findViewById(R.id.pFirstName);
        pLName = findViewById(R.id.pLastName);
        pAge = findViewById(R.id.ageInput);
        pReligion = findViewById(R.id.pReligion);
        pCComplaint = findViewById(R.id.pComplaint);
        pSAS = findViewById(R.id.pSigns);
        pAllergies = findViewById(R.id.pAllergies);
        pMedications = findViewById(R.id.pMedications);
        pPastMedicalH = findViewById(R.id.pPastMed);
        pLastOralI = findViewById(R.id.pLastOralI);
        pELTPI = findViewById(R.id.pEventsLeading);
        pOxygenS = findViewById(R.id.pOxygenSatur);
        pRespRate = findViewById(R.id.pRespiratoryRate);
        pHeartRate = findViewById(R.id.pHeartRate);
        pBodyTemp = findViewById(R.id.pBodyTemperature);
        pBloodPressure = findViewById(R.id.pBloodPressure);
        spinnerSex = findViewById(R.id.spinnerSex);

        // Apply speech-to-text setup to each EditText field
        setupSpeechToText(pFName);
        setupSpeechToText(pLName);
        setupSpeechToText(pAge);
        setupSpeechToText(pReligion);
        setupSpeechToText(pCComplaint);
        setupSpeechToText(pSAS);
        setupSpeechToText(pAllergies);
        setupSpeechToText(pMedications);
        setupSpeechToText(pPastMedicalH);
        setupSpeechToText(pLastOralI);
        setupSpeechToText(pELTPI);
        setupSpeechToText(pOxygenS);
        setupSpeechToText(pRespRate);
        setupSpeechToText(pHeartRate);
        setupSpeechToText(pBodyTemp);
        setupSpeechToText(pBloodPressure);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.sex, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSex.setAdapter(adapter);
    }

    private void setupRecyclerView() {
        // Check if there are existing notes in patientModel
        List<String> dataList = (patientModel.getNotes() != null) ? patientModel.getNotes() : new ArrayList<>();
        notesAdapter = new NotesAdapter(dataList, this);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(notesAdapter);

        Button addNotes = findViewById(R.id.addNotes);
        addNotes.setOnClickListener(view -> notesAdapter.addItem());
    }

    private void populateFields() {
        pFName.setText(patientModel.getFirstName());
        pLName.setText(patientModel.getLastName());
        pAge.setText(String.valueOf(patientModel.getAge()));
        pReligion.setText(patientModel.getReligion());
        pCComplaint.setText(patientModel.getChiefComplaint());
        pSAS.setText(patientModel.getSignsAndSymptoms());
        pAllergies.setText(patientModel.getAllergies());
        pMedications.setText(patientModel.getMedications());
        pPastMedicalH.setText(patientModel.getPastMedicalHistory());
        pLastOralI.setText(patientModel.getLastOralIntake());
        pELTPI.setText(patientModel.getEventsLeadingToPresentIllness());
        pOxygenS.setText(String.valueOf(patientModel.getOxygenSaturation()));
        pRespRate.setText(String.valueOf(patientModel.getRespiratoryRate()));
        pHeartRate.setText(String.valueOf(patientModel.getHeartRate()));
        pBodyTemp.setText(String.valueOf(patientModel.getBodyTemperature()));
        pBloodPressure.setText(String.valueOf(patientModel.getBloodPressure()));

        String selectedSex = patientModel.getSex();
        if (selectedSex != null) {
            ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinnerSex.getAdapter();
            int spinnerPosition = adapter.getPosition(selectedSex);
            spinnerSex.setSelection(spinnerPosition);
        }
    }

    private void updatePatientData() {
        String firstName = pFName.getText().toString().trim();
        String lastName = pLName.getText().toString().trim();
        String ageInput = pAge.getText().toString().trim();
        String chiefComplaint = pCComplaint.getText().toString().trim();
        String bloodPressure = pBloodPressure.getText().toString().trim();
        List<String> notes = notesAdapter.getNotes();

        if (!editFormController.validateFields(firstName, lastName, ageInput, chiefComplaint, String.valueOf(bloodPressure), notes)) {
            showToast("Please fill out all required fields.");
            return;
        }

        int age = editFormController.parseInt(ageInput, 0);
        String oxygenSaturation = pOxygenS.getText().toString().trim();
        String respiratoryRate = pRespRate.getText().toString().trim();
        String heartRate = pHeartRate.getText().toString().trim();
        String bodyTemperature = pBodyTemp.getText().toString().trim();
        String selectedSex = spinnerSex.getSelectedItem().toString();

        patientModel.setFirstName(firstName);
        patientModel.setLastName(lastName);
        patientModel.setSex(selectedSex);
        patientModel.setAge(age);
        patientModel.setReligion(pReligion.getText().toString().trim());
        patientModel.setChiefComplaint(chiefComplaint);
        patientModel.setSignsAndSymptoms(pSAS.getText().toString().trim());
        patientModel.setAllergies(pAllergies.getText().toString().trim());
        patientModel.setMedications(pMedications.getText().toString().trim());
        patientModel.setPastMedicalHistory(pPastMedicalH.getText().toString().trim());
        patientModel.setLastOralIntake(pLastOralI.getText().toString().trim());
        patientModel.setEventsLeadingToPresentIllness(pELTPI.getText().toString().trim());
        patientModel.setOxygenSaturation(oxygenSaturation);
        patientModel.setRespiratoryRate(respiratoryRate);
        patientModel.setHeartRate(heartRate);
        patientModel.setBodyTemperature(bodyTemperature);
        patientModel.setBloodPressure(bloodPressure);
        patientModel.setNotes(notes);

        Log.d("patientModel", patientModel.toString());
        if (hospitalModel != null) {
            Log.d("hospitalModel", hospitalModel.toString());
        } else {
            Log.e("EditFormView", "hospitalModel is null.");
        }

        Intent resultIntent = new Intent();
        resultIntent.putExtra("patientModel", patientModel); // Pass the updated model back
        resultIntent.putExtra("hospitalModel", hospitalModel); // Pass HospitalModel back
        setResult(RESULT_OK, resultIntent);
        finish();
    }


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void promptSpeechInput(boolean numericOnly) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Please say the text");

        // Configure for numeric input if required
        if (numericOnly) {
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        }

        try {
            startActivityForResult(intent, 100);
        } catch (ActivityNotFoundException e) {
            showToast("Speech-to-text not supported on this device.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty() && currentEditText != null) {
                currentEditText.setText(result.get(0)); // Set recognized text in the selected EditText
            }
        }
    }
    @Override
    public void onSpeechRequest(EditText editText) {
        currentEditText = editText;
        currentEditText.requestFocus(); // Request focus to ensure single-tap response

        boolean numericOnly = (currentEditText == pAge || currentEditText == pOxygenS || currentEditText == pRespRate ||
                currentEditText == pHeartRate || currentEditText == pBodyTemp ||
                currentEditText == pBloodPressure);

        promptSpeechInput(numericOnly);
    }



    private void setupSpeechToText(EditText editText) {
        // Launch speech-to-text when gaining focus
        editText.setOnFocusChangeListener((view, hasFocus) -> {
            if (hasFocus) {
                onSpeechRequest(editText);
            }
        });

        // Long press to focus for manual typing without triggering speech-to-text
        editText.setOnLongClickListener(view -> {
            editText.requestFocus();
            return true;
        });
    }

}
