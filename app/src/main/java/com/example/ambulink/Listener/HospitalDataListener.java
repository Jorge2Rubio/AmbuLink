package com.example.ambulink.Listener;

import com.example.ambulink.Model.HospitalModel;

public interface HospitalDataListener {
    void onHospitalDataChanged();
    void onPatientFormSent(HospitalModel hospitalModel);
    void onPatientStatusUpdated(HospitalModel hospitalModel);
}
