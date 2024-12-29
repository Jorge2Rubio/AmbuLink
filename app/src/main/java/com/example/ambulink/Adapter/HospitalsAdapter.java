package com.example.ambulink.Adapter;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ambulink.Model.PatientModel;
import com.example.ambulink.R;
import com.example.ambulink.View.PatientDetailActivityView;
import com.example.ambulink.utils.PatientDiffCallback;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HospitalsAdapter extends RecyclerView.Adapter<HospitalsAdapter.PatientViewHolder> {

    private Context context;
    private List<PatientModel> patientList;
    private String hospitalId;  // Store hospitalId
    private MediaPlayer mediaPlayer, mediaPlayer2, mediaPlayer3; // MediaPlayer instance
    private Map<String, Boolean> sound1PlayedMap = new HashMap<>();
    private Map<String, Boolean> sound2PlayedMap = new HashMap<>();
    private Map<String, Boolean> sound3PlayedMap = new HashMap<>();



    // Modified constructor to accept hospitalId
    public HospitalsAdapter(Context context, List<PatientModel> patientList, String hospitalId) {
        this.context = context;
        this.patientList = patientList;
        this.hospitalId = hospitalId;  // Assign hospitalId
        this.mediaPlayer = MediaPlayer.create(context, R.raw.isarrived);
        this.mediaPlayer2 = MediaPlayer.create(context, R.raw.ispending);
        this.mediaPlayer3 = MediaPlayer.create(context, R.raw.isnear);
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_paramedics, parent, false);
        return new PatientViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        PatientModel patient = patientList.get(position);
        String patientId = patient.getPatientId(); // Ensure this ID uniquely identifies the patient

        holder.firstNameTextView.setText(patient.getSenderEmail());

        // Set ETA
        if (patient.getFinalDecision() != null && !patient.getFinalDecision().isEmpty()) {
            holder.tvETA.setText(patient.getFinalDecision());
        } else {
            String eta = patient.getEta();
            holder.tvETA.setText(eta != null && !eta.isEmpty() ? "ETA: " + eta : "ETA: N/A");
        }

        // Check acceptance status
        if ("true".equals(patient.getIsAccepted())) {
            holder.tvStatus.setText("Accepted");

            if (patient.isArrived()) {
                // Update UI
                holder.tvETA.setText("Arrived");
                holder.tvETA.setTextColor(ContextCompat.getColor(context, R.color.white));
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.green));
                holder.firstNameTextView.setTextColor(ContextCompat.getColor(context, R.color.white));
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.white));
                holder.line.setBackgroundColor(ContextCompat.getColor(context, R.color.green));

                // Play sound if not played
                if (!Boolean.TRUE.equals(sound1PlayedMap.get(patientId))) {
                    playSound(mediaPlayer);
                    sound1PlayedMap.put(patientId, true);
                }
            } else {
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green));
            }
        } else if ("true".equals(patient.getIsRejected())) {
            holder.tvStatus.setText("Rejected");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.red));

            // Additional UI updates for rejected status
        } else if (patient.getIsRejected().isEmpty() && patient.getIsAccepted().isEmpty()){
            holder.tvStatus.setText("Pending");
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.black));
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.gray));
            holder.tvETA.setTextColor(ContextCompat.getColor(context, R.color.black));

            // Play pending sound if not played
            if (!Boolean.TRUE.equals(sound2PlayedMap.get(patientId))) {
                playSound(mediaPlayer2);
                sound2PlayedMap.put(patientId, true);
            }
        }

        // Check if near hospital
        if (patient.isNearHospital() && !Boolean.TRUE.equals(sound3PlayedMap.get(patientId))) {
            playSound(mediaPlayer3);
            sound3PlayedMap.put(patientId, true);
        }

        // Handle arrival over 30 minutes
        if (isArrivedForOver30Minutes(patient)) {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
            holder.firstNameTextView.setTextColor(ContextCompat.getColor(context, R.color.black));
            holder.tvETA.setTextColor(ContextCompat.getColor(context, R.color.black));
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green));
            holder.tvETA.setText("Arrived");
            holder.line.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        }

        // Handle item click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PatientDetailActivityView.class);
            intent.putExtra("patientData", patient);
            intent.putExtra("hospitalId", hospitalId);
            context.startActivity(intent);
        });
    }



    @Override
    public int getItemCount() {
        return patientList.size();
    }

    public static class PatientViewHolder extends RecyclerView.ViewHolder {
        TextView firstNameTextView;
        TextView tvStatus;
        TextView tvETA;
        View line;

        public PatientViewHolder(@NonNull View itemView) {
            super(itemView);
            firstNameTextView = itemView.findViewById(R.id.tvSenderName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvETA = itemView.findViewById(R.id.tvETA);
            line = itemView.findViewById(R.id.line);
        }
    }

    public void updatePatientList(List<PatientModel> newPatientList) {
        PatientDiffCallback diffCallback = new PatientDiffCallback(this.patientList, newPatientList);
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffCallback);

        this.patientList.clear();
        this.patientList.addAll(newPatientList);
        diffResult.dispatchUpdatesTo(this);
    }

    private void playSound(MediaPlayer mediaPlayer) {
        if (mediaPlayer != null) {
            mediaPlayer.start(); // Play the sound
        }
    }


    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (mediaPlayer != null) {
            mediaPlayer.release(); // Release MediaPlayer resources
            mediaPlayer = null;
        }

        if (mediaPlayer2 != null) {
            mediaPlayer2.release();
            mediaPlayer2 = null;
        }

        if (mediaPlayer3 != null) {
            mediaPlayer3.release();
            mediaPlayer3 = null;
        }
    }

    private boolean isArrivedForOver30Minutes(PatientModel patient) {
        if (patient.isArrived() && patient.getAcceptanceStatusDate() != null) {
            long currentTime = System.currentTimeMillis();
            long acceptanceTime = patient.getAcceptanceStatusDate().toDate().getTime();
            return (currentTime - acceptanceTime) >= 30 * 60 * 1000; // 30 minutes in milliseconds
        }
        return false;
    }
}
