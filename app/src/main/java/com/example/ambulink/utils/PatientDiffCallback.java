package com.example.ambulink.utils;

import androidx.recyclerview.widget.DiffUtil;

import com.example.ambulink.Model.PatientModel;

import java.util.List;

public class PatientDiffCallback extends DiffUtil.Callback {

    private final List<PatientModel> oldList;
    private final List<PatientModel> newList;

    public PatientDiffCallback(List<PatientModel> oldList, List<PatientModel> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        // Compare unique IDs
        return oldList.get(oldItemPosition).getPatientId().equals(newList.get(newItemPosition).getPatientId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        PatientModel oldPatient = oldList.get(oldItemPosition);
        PatientModel newPatient = newList.get(newItemPosition);

        // Compare fields that may change
        return oldPatient.getEta().equals(newPatient.getEta()) &&
                oldPatient.getIsAccepted().equals(newPatient.getIsAccepted()) &&
                oldPatient.getIsRejected().equals(newPatient.getIsRejected()) &&
                oldPatient.getDistance() == newPatient.getDistance() &&
                oldPatient.getFinalDecision().equals(newPatient.getFinalDecision());
    }
}
