package com.example.ambulink.Adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ambulink.Listener.SelectListener;
import com.example.ambulink.Model.HospitalModel;
import com.example.ambulink.Model.PatientModel;
import com.example.ambulink.R;

import java.util.List;

public class ParamedicsAdapter extends RecyclerView.Adapter<ParamedicsAdapter.ViewHolder> {
    private List<HospitalModel> hospitalList;
    private SelectListener listener;
    private Context context;

    public ParamedicsAdapter(Context context, List<HospitalModel> hospitalList, SelectListener listener) {
        this.context = context;
        this.hospitalList = hospitalList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ParamedicsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_hospitals, parent, false);
        return new ParamedicsAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ParamedicsAdapter.ViewHolder holder, int position) {
        HospitalModel hospital = hospitalList.get(position);
        holder.hospitalName.setText(hospital.getName());
        holder.hospitalEta.setText(hospital.getEta());
        holder.hospitalAddress.setText(hospital.getAddress());
        holder.hospitalCapacity.setText(hospital.getSlotsAvailable() + " Emergency Beds Available");
        holder.hospital_type.setText(hospital.getHospitalType());

        if(hospital.getNumNurses() == 0){
            holder.tvNursesAvailable.setText("No Nurse Available");
        }
        if(hospital.getNumDoctors() == 0){
            holder.tvDoctorsAvailable.setText("No Doctors Available");
        }

        if(hospital.getNumDoctors() > 0){
            holder.tvDoctorsAvailable.setText("Doctors Available: " + hospital.getNumDoctors());
        }
        if(hospital.getNumNurses() > 0){
            holder.tvNursesAvailable.setText("Nurses Available: " + hospital.getNumNurses());
        }

        // Clear previous texts
        holder.acceptanceStatus.setText("");
        holder.hospitalStaff.setText("");
        holder.rejectionReason.setText("");

        if(hospital.getSlotsAvailable() == 0){
            holder.chooseHospitalBtn.setEnabled(false);
            holder.chooseHospitalBtn.setBackgroundColor(context.getResources().getColor(R.color.white, context.getTheme()));
            holder.chooseHospitalBtn.setTextColor(context.getResources().getColor(R.color.gray, context.getTheme()));
            holder.hospitalCapacity.setText("No emergency beds available.");
            holder.navigateButton.setEnabled(false);
            holder.navigateButton.setBackgroundColor(context.getResources().getColor(R.color.white, context.getTheme()));
            holder.navigateButton.setTextColor(context.getResources().getColor(R.color.gray, context.getTheme()));
        }else {
            holder.chooseHospitalBtn.setEnabled(true);
            holder.chooseHospitalBtn.setBackgroundColor(context.getResources().getColor(R.color.gray, context.getTheme()));
            holder.chooseHospitalBtn.setTextColor(context.getResources().getColor(R.color.black, context.getTheme()));
        }

        // Set acceptance status
        if ("true".equals(hospital.getIsAccepted())) {
            holder.chooseHospitalBtn.setBackgroundColor(context.getResources().getColor(R.color.white, context.getTheme()));
            holder.chooseHospitalBtn.setTextColor(context.getResources().getColor(R.color.gray, context.getTheme()));
            holder.acceptanceStatus.setText("Accepted " + hospital.getFormattedAcceptanceStatusDate());
            holder.acceptanceStatus.setTextColor(context.getResources().getColor(R.color.green, context.getTheme()));
            holder.hospitalStaff.setText("Accepted by: " + hospital.getIsAcceptedBy());
            holder.navigateButton.setEnabled(true);
            holder.navigateButton.setBackgroundColor(context.getResources().getColor(R.color.gray, context.getTheme()));
            holder.navigateButton.setTextColor(context.getResources().getColor(R.color.black, context.getTheme()));
        } else if ("true".equals(hospital.getIsRejected())) {
            holder.chooseHospitalBtn.setBackgroundColor(context.getResources().getColor(R.color.white, context.getTheme()));
            holder.chooseHospitalBtn.setTextColor(context.getResources().getColor(R.color.gray, context.getTheme()));
            holder.acceptanceStatus.setText("Rejected " + hospital.getFormattedAcceptanceStatusDate());
            holder.acceptanceStatus.setTextColor(context.getResources().getColor(R.color.red, context.getTheme()));
            holder.hospitalStaff.setText("Rejected by: " + hospital.getIsRejectedBy());
            holder.navigateButton.setEnabled(false);
            holder.navigateButton.setBackgroundColor(context.getResources().getColor(R.color.white, context.getTheme()));
            holder.navigateButton.setTextColor(context.getResources().getColor(R.color.gray, context.getTheme()));
        } else if (hospital.isPending()) {
            holder.acceptanceStatus.setTextColor(context.getResources().getColor(R.color.black, context.getTheme()));
            holder.acceptanceStatus.setText("Pending");

            // Disable chooseHospitalBtn and navigateButton if status is pending
            holder.chooseHospitalBtn.setEnabled(false);
            holder.chooseHospitalBtn.setBackgroundColor(context.getResources().getColor(R.color.white, context.getTheme()));
            holder.chooseHospitalBtn.setTextColor(context.getResources().getColor(R.color.gray, context.getTheme()));
            holder.navigateButton.setEnabled(false);
            holder.navigateButton.setBackgroundColor(context.getResources().getColor(R.color.white, context.getTheme()));
            holder.navigateButton.setTextColor(context.getResources().getColor(R.color.gray, context.getTheme()));
        }else {
            holder.navigateButton.setEnabled(false);
            holder.navigateButton.setBackgroundColor(context.getResources().getColor(R.color.white, context.getTheme()));
            holder.navigateButton.setTextColor(context.getResources().getColor(R.color.gray, context.getTheme()));
        }


        // Handle rejection reason visibility
        if (hospital.getRejectionReason() == null || hospital.getRejectionReason().isEmpty()) {
            holder.rejectionReason.setVisibility(View.GONE);
        } else {
            holder.rejectionReason.setVisibility(View.VISIBLE);
            holder.rejectionReason.setTextColor(context.getResources().getColor(R.color.red, context.getTheme()));
            holder.rejectionReason.setText("Rejection Reason: " + hospital.getRejectionReason());
        }

        // Navigate button click listener
        holder.navigateButton.setOnClickListener(view -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                HospitalModel clickedHospital = hospitalList.get(adapterPosition);
                listener.onNavigate(clickedHospital);
            }
        });

        // Choose hospital button click listener
        holder.chooseHospitalBtn.setOnClickListener(view -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                HospitalModel clickedHospital = hospitalList.get(adapterPosition);

                // Set the hospital as pending
                clickedHospital.setPending(true);


                // Notify listener
                listener.onSelectedHospitalClicked(clickedHospital);

                // Update the RecyclerView item to reflect the change
                notifyItemChanged(adapterPosition);
            }
        });




    }

    @Override
    public int getItemCount() {
        return hospitalList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView hospitalName;
        public TextView hospitalEta;
        public TextView hospitalAddress;
        public TextView hospitalCapacity;
        public TextView acceptanceStatus;
        public TextView hospitalStaff;
        public Button chooseHospitalBtn;
        public Button navigateButton;
        public LinearLayout item_main;
        public TextView rejectionReason;
        public TextView hospital_type;
        public TextView tvNursesAvailable;
        public TextView tvDoctorsAvailable;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            hospitalName = itemView.findViewById(R.id.tvHospitalName);
            hospitalEta = itemView.findViewById(R.id.tvETA);
            hospitalAddress = itemView.findViewById(R.id.tvAddress);
            hospitalCapacity = itemView.findViewById(R.id.tvAvailableSlots);
            acceptanceStatus = itemView.findViewById(R.id.acceptanceStatus);
            hospitalStaff = itemView.findViewById(R.id.hospitalStaff);
            chooseHospitalBtn = itemView.findViewById(R.id.btnChooseHospital);
            navigateButton = itemView.findViewById(R.id.btnNavigate);
            item_main = itemView.findViewById(R.id.hospital_main);
            rejectionReason = itemView.findViewById(R.id.rejectionReason);
            hospital_type = itemView.findViewById(R.id.hospital_type);
            tvNursesAvailable = itemView.findViewById(R.id.tvNursesAvailable);
            tvDoctorsAvailable = itemView.findViewById(R.id.tvDoctorsAvailable);
        }
    }


}
