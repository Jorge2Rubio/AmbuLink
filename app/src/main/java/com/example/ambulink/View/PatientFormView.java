package com.example.ambulink.View;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
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
import com.example.ambulink.Model.HospitalModel;
import com.example.ambulink.Model.PatientModel;
import com.example.ambulink.R;
import com.example.ambulink.Controller.PatientFormController;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PatientFormView extends AppCompatActivity implements NotesAdapter.OnSpeechRequestListener {

    private static final int SPEECH_REQUEST_CODE = 100;
    private PatientFormController patientFormController;
    private EditText currentEditText; // Track which EditText is active
    private PatientModel patientModel;
    private HospitalModel hospitalModel;

    // Declare EditText fields at the class level
    private EditText pFName, pLName, pAge, pReligionOther, pCComplaint, pSAS, pAllergies, pMedications,
            pPastMedicalH, pLastOralI, pELTPI, pOxygenS, pRespRate, pHeartRate, pBodyTemp, pBloodPressure;
    private Spinner pReligion, spinnerSex;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.patient_form);

        patientFormController = new PatientFormController();

        // Initialize form fields
        pFName = findViewById(R.id.pFirstName);
        pLName = findViewById(R.id.pLastName);
        pAge = findViewById(R.id.ageInput);
        pReligion = findViewById(R.id.pReligion);
        pReligionOther = findViewById(R.id.pReligionOther);
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
        Button pButtonSubmit = findViewById(R.id.pSubmit);

        setSpeechToTextIconClick(pFName, false);
        setSpeechToTextIconClick(pLName, false);
        setSpeechToTextIconClick(pAge, true);
        setSpeechToTextIconClick(pReligionOther, false);
        setSpeechToTextIconClick(pCComplaint, false);
        setSpeechToTextIconClick(pSAS, false);
        setSpeechToTextIconClick(pAllergies, false);
        setSpeechToTextIconClick(pMedications, false);
        setSpeechToTextIconClick(pPastMedicalH, false);
        setSpeechToTextIconClick(pLastOralI, false);
        setSpeechToTextIconClick(pELTPI, false);
        setSpeechToTextIconClick(pOxygenS, false);
        setSpeechToTextIconClick(pRespRate, false);
        setSpeechToTextIconClick(pHeartRate, false);
        setSpeechToTextIconClick(pBodyTemp, false);
        setSpeechToTextIconClick(pBloodPressure, true);



        // Set up the Religion Spinner
        ArrayAdapter<CharSequence> religionAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.religions,
                android.R.layout.simple_spinner_item
        );
        religionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pReligion.setAdapter(religionAdapter);

        // Initialize the existing spinner for Sex
        ArrayAdapter<CharSequence> adapter_spinner = ArrayAdapter.createFromResource(
                this,
                R.array.sex,
                android.R.layout.simple_spinner_item
        );
        adapter_spinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSex.setAdapter(adapter_spinner);

        // Show or hide the "Other" religion EditText based on the selection
        pReligion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedReligion = parent.getItemAtPosition(position).toString();
                if (selectedReligion.equals("Others")) {
                    pReligionOther.setVisibility(View.VISIBLE);
                } else {
                    pReligionOther.setVisibility(View.GONE);
                    pReligionOther.setText(""); // Clear any previous input
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // RecyclerView setup
        List<String> dataList = new ArrayList<>();
        NotesAdapter adapter = new NotesAdapter(dataList, this);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Handle adding notes
        Button addNotes = findViewById(R.id.addNotes);
        addNotes.setOnClickListener(view -> adapter.addItem());

        // Handle intent data for patient model
        Intent intent = getIntent();
        if (intent != null) {
            patientModel = intent.getParcelableExtra("patientModel");
            hospitalModel = intent.getParcelableExtra("hospitalModel");
            if (patientModel != null) {
                populateFields(patientModel, pFName, pLName, pAge, pReligion, pCComplaint, pSAS, pAllergies, pMedications,
                        pPastMedicalH, pLastOralI, pELTPI, pOxygenS, pRespRate, pHeartRate, pBodyTemp, pBloodPressure, spinnerSex, adapter_spinner, dataList, adapter);
            }
        }

        // Submit button click listener
        pButtonSubmit.setOnClickListener(view -> {
            String firstName = pFName.getText().toString().trim();
            String lastName = pLName.getText().toString().trim();
            String ageInput = pAge.getText().toString().trim();
            String chiefComplaint = pCComplaint.getText().toString().trim();
            List<String> notes = adapter.getNotes();

            int age = patientFormController.parseInt(ageInput, 0);
            String selectedReligion = pReligion.getSelectedItem().toString();
            if (selectedReligion.equals("Others")) {
                selectedReligion = pReligionOther.getText().toString().trim(); // Use custom input
            }

            String bloodPressure = pBloodPressure.getText().toString().trim();
            String oxygenSaturation = pOxygenS.getText().toString().trim();
            String respiratoryRate = pRespRate.getText().toString().trim();
            String heartRate = pHeartRate.getText().toString().trim();
            String bodyTemperature = pBodyTemp.getText().toString().trim();
            String selectedSex = spinnerSex.getSelectedItem().toString();

            if (patientModel == null) {
                patientModel = new PatientModel();
            }

            if (!patientFormController.validateFields(firstName, lastName, ageInput, selectedSex, chiefComplaint, selectedReligion, bloodPressure, oxygenSaturation, heartRate, bodyTemperature, notes)) {
                showToast("Please fill out all required fields.");
                return;
            }



            patientModel.setFirstName(firstName);
            patientModel.setLastName(lastName);
            patientModel.setSex(selectedSex);
            patientModel.setAge(age);
            patientModel.setReligion(selectedReligion);
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

            patientFormController.submitForm(hospitalModel, patientModel, new PatientFormController.FirestoreCallback() {
                @Override
                public void onSuccess(PatientModel model) {
                    Intent newIntent = new Intent(PatientFormView.this, ParamedicsView.class);
                    newIntent.putExtra("patientModel", model);
                    startActivity(newIntent);
                    finish();
                }

                @Override
                public void onFailure(String error) {
                    showToast("Failed to submit form: " + error);
                }
            });
        });
    }

    private void populateFields(PatientModel patientModel, EditText pFName, EditText pLName, EditText pAge, Spinner pReligion, EditText pCComplaint,
                                EditText pSAS, EditText pAllergies, EditText pMedications, EditText pPastMedicalH, EditText pLastOralI,
                                EditText pELTPI, EditText pOxygenS, EditText pRespRate, EditText pHeartRate, EditText pBodyTemp, EditText pBloodPressure,
                                Spinner spinner, ArrayAdapter<CharSequence> adapter_spinner, List<String> dataList, NotesAdapter adapter) {
        // Populate fields with patient data
        pFName.setText(patientModel.getFirstName());
        pLName.setText(patientModel.getLastName());
        pAge.setText(String.valueOf(patientModel.getAge()));

        // Set religion selection
        ArrayAdapter<CharSequence> religionAdapter = (ArrayAdapter<CharSequence>) pReligion.getAdapter();
        int religionPosition = religionAdapter.getPosition(patientModel.getReligion());
        pReligion.setSelection(religionPosition);

        pCComplaint.setText(patientModel.getChiefComplaint());
        pSAS.setText(patientModel.getSignsAndSymptoms());
        pAllergies.setText(patientModel.getAllergies());
        pMedications.setText(patientModel.getMedications());
        pPastMedicalH.setText(patientModel.getPastMedicalHistory());
        pLastOralI.setText(patientModel.getLastOralIntake());
        pELTPI.setText(patientModel.getEventsLeadingToPresentIllness());
        pOxygenS.setText(patientModel.getOxygenSaturation());
        pRespRate.setText(patientModel.getRespiratoryRate());
        pHeartRate.setText(patientModel.getHeartRate());
        pBodyTemp.setText(patientModel.getBodyTemperature());
        pBloodPressure.setText(patientModel.getBloodPressure());

        String selectedSex = patientModel.getSex();
        if (selectedSex != null) {
            int spinnerPosition = adapter_spinner.getPosition(selectedSex);
            spinner.setSelection(spinnerPosition);
        }

        List<String> retrievedNotes = patientModel.getNotes();
        if (retrievedNotes != null && !retrievedNotes.isEmpty()) {
            dataList.clear();
            dataList.addAll(retrievedNotes);
            adapter.notifyDataSetChanged();
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }



    private void promptSpeechInput(boolean numericOnly) {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Please say the text");

        // Restrict grammar or filtering if numeric-only
        if (numericOnly) {
            intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000);
        }

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            showToast("Speech-to-text not supported on this device.");
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty() && currentEditText != null) {
                String recognizedText = result.get(0);

                // Check if numeric-only input is required
                boolean numericOnly = currentEditText == pAge;

                // Validate as numeric if required
                if (numericOnly && !isNumeric(recognizedText)) {
                    showToast("Please speak only numbers for this field.");
                    currentEditText.setText(""); // Clear invalid input
                } else {
                    currentEditText.setText(recognizedText);
                }
            }
        }
    }
    @Override
    public void onSpeechRequest(EditText editText) {
        currentEditText = editText;
        promptSpeechInput(false);  // Call the method to trigger the speech recognizer
    }

    private void setSpeechToTextIconClick(EditText editText, boolean numericOnly) {
        editText.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int drawableEndIndex = 2; // Index for drawableEnd
                if (event.getRawX() >= (editText.getRight() - editText.getCompoundDrawables()[drawableEndIndex].getBounds().width())) {
                    currentEditText = editText; // Track which EditText to fill
                    promptSpeechInput(numericOnly);
                    return true;
                }
            }
            return false;
        });
    }


    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
