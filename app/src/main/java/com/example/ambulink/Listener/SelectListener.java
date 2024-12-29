package com.example.ambulink.Listener;

import com.example.ambulink.Model.HospitalModel;

public interface SelectListener {
    void onItemClicked(HospitalModel hospitalModel);
    void onSelectedHospitalClicked(HospitalModel hospitalModel);
    void onNavigate(HospitalModel hospitalModel);
}
