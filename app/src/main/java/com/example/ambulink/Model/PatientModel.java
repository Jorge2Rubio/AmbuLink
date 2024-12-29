package com.example.ambulink.Model;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.firebase.Timestamp;
import androidx.annotation.NonNull;

import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PatientModel implements Parcelable {
    private String patientId;  // Add patientId to store Firestore document ID
    private String senderEmail;
    private String firstName;
    private String lastName;
    private String sex;
    private int age;
    private String religion;
    private String chiefComplaint;
    private String signsAndSymptoms;
    private String allergies;
    private String medications;
    private String pastMedicalHistory;
    private String lastOralIntake;
    private String eventsLeadingToPresentIllness;
    private String oxygenSaturation;
    private String respiratoryRate;
    private String heartRate;
    private String bodyTemperature;
    private String bloodPressure;
    private List<String> notes;
    private String isAccepted = "";
    private String isRejected = "";
    private String isAcceptedBy = "";
    private String isRejectedBy = "";
    private String rejectionReason = "";
    private Timestamp AcceptanceStatusDate;
    private String formattedAcceptanceStatusDate;
    private Timestamp Date;
    private double longitude;
    private double latitude;
    private String eta = "";
    private double distance = 0;
    private String finalDecision = "";
    private boolean isNearHospital;
    private boolean isArrived;

    // No-argument constructor required for Firestore
    public PatientModel() {}

    // Constructor with full initialization, including patientId
    public PatientModel(PatientModel other) {
        this.patientId = other.getPatientId(); // Assign null so a new patientId will be set
        this.senderEmail = other.senderEmail;
        this.firstName = other.firstName;
        this.lastName = other.lastName;
        this.sex = other.sex;
        this.age = other.age;
        this.religion = other.religion;
        this.chiefComplaint = other.chiefComplaint;
        this.signsAndSymptoms = other.signsAndSymptoms;
        this.allergies = other.allergies;
        this.medications = other.medications;
        this.pastMedicalHistory = other.pastMedicalHistory;
        this.lastOralIntake = other.lastOralIntake;
        this.eventsLeadingToPresentIllness = other.eventsLeadingToPresentIllness;
        this.oxygenSaturation = other.oxygenSaturation;
        this.respiratoryRate = other.respiratoryRate;
        this.heartRate = other.heartRate;
        this.bodyTemperature = other.bodyTemperature;
        this.bloodPressure = other.bloodPressure;
        this.notes = other.notes != null ? new ArrayList<>(other.notes) : null;
        // Reset status fields
        this.isAccepted = "";
        this.isRejected = "";
        this.isAcceptedBy = "";
        this.isRejectedBy = "";
        this.rejectionReason = "";
        this.AcceptanceStatusDate = null;
        this.Date = null;
        this.longitude = 0;
        this.latitude = 0;
        this.eta = "";
        this.distance = 0;
        this.finalDecision = "";
        this.formattedAcceptanceStatusDate = "";
    }

    // Getters and Setters for each field, including patientId


    public String getFinalDecision() {
        return finalDecision;
    }

    public void setFinalDecision(String finalDecision) {
        this.finalDecision = finalDecision;
    }

    public String getEta() {
        return eta;
    }

    public void setEta(String eta) {
        this.eta = eta;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public Timestamp getDate() {
        return Date;
    }

    public void setDate(Timestamp date) {
        Date = date;
    }

    public Timestamp getAcceptanceStatusDate() {
        return AcceptanceStatusDate;
    }

    public void setAcceptanceStatusDate(Timestamp acceptanceStatusDate) {
        AcceptanceStatusDate = acceptanceStatusDate;
    }

    public String getFormattedAcceptanceStatusDate() {
        return formattedAcceptanceStatusDate;
    }

    public void setFormattedAcceptanceStatusDate(String formattedAcceptanceStatusDate) {
        this.formattedAcceptanceStatusDate = formattedAcceptanceStatusDate;
    }

    public boolean isArrived() {
        return isArrived;
    }

    public void setArrived(boolean arrived) {
        isArrived = arrived;
    }

    public boolean isNearHospital() {
        return isNearHospital;
    }

    public void setNearHospital(boolean nearHospital) {
        isNearHospital = nearHospital;
    }

    //    // Method to convert Timestamp to String
//    public String getFormattedAcceptanceStatusDate() {
//        if (AcceptanceStatusDate != null) {
//            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(AcceptanceStatusDate.toDate());
//        }
//        return "Date not available";
//    }

    public String getFormattedDate() {
        if (Date != null) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date.toDate());
        }
        return "Date not available";
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getReligion() {
        return religion;
    }

    public void setReligion(String religion) {
        this.religion = religion;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public void setChiefComplaint(String chiefComplaint) {
        this.chiefComplaint = chiefComplaint;
    }

    public String getSignsAndSymptoms() {
        return signsAndSymptoms;
    }

    public void setSignsAndSymptoms(String signsAndSymptoms) {
        this.signsAndSymptoms = signsAndSymptoms;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getMedications() {
        return medications;
    }

    public void setMedications(String medications) {
        this.medications = medications;
    }

    public String getPastMedicalHistory() {
        return pastMedicalHistory;
    }

    public void setPastMedicalHistory(String pastMedicalHistory) {
        this.pastMedicalHistory = pastMedicalHistory;
    }

    public String getLastOralIntake() {
        return lastOralIntake;
    }

    public void setLastOralIntake(String lastOralIntake) {
        this.lastOralIntake = lastOralIntake;
    }

    public String getEventsLeadingToPresentIllness() {
        return eventsLeadingToPresentIllness;
    }

    public void setEventsLeadingToPresentIllness(String eventsLeadingToPresentIllness) {
        this.eventsLeadingToPresentIllness = eventsLeadingToPresentIllness;
    }

    public String getOxygenSaturation() {
        return oxygenSaturation;
    }

    public void setOxygenSaturation(String oxygenSaturation) {
        this.oxygenSaturation = oxygenSaturation;
    }

    public String getRespiratoryRate() {
        return respiratoryRate;
    }

    public void setRespiratoryRate(String respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }

    public String getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(String heartRate) {
        this.heartRate = heartRate;
    }

    public String getBodyTemperature() {
        return bodyTemperature;
    }

    public void setBodyTemperature(String bodyTemperature) {
        this.bodyTemperature = bodyTemperature;
    }

    public String getBloodPressure() {
        return bloodPressure;
    }

    public void setBloodPressure(String bloodPressure) {
        this.bloodPressure = bloodPressure;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes;
    }

    public String getIsAccepted() {
        return isAccepted;
    }

    public void setIsAccepted(String isAccepted) {
        this.isAccepted = isAccepted;
    }

    public String getIsRejected() {
        return isRejected;
    }

    public void setIsRejected(String isRejected) {
        this.isRejected = isRejected;
    }

    public String getIsAcceptedBy() {
        return isAcceptedBy;
    }

    public void setIsAcceptedBy(String isAcceptedBy) {
        this.isAcceptedBy = isAcceptedBy;
    }

    public String getIsRejectedBy() {
        return isRejectedBy;
    }

    public void setIsRejectedBy(String isRejectedBy) {
        this.isRejectedBy = isRejectedBy;
    }



    // Parcelable implementation
    protected PatientModel(Parcel in) {
        patientId = in.readString();
        senderEmail = in.readString();
        firstName = in.readString();
        lastName = in.readString();
        sex = in.readString();
        age = in.readInt();
        religion = in.readString();
        chiefComplaint = in.readString();
        signsAndSymptoms = in.readString();
        allergies = in.readString();
        medications = in.readString();
        pastMedicalHistory = in.readString();
        lastOralIntake = in.readString();
        eventsLeadingToPresentIllness = in.readString();
        oxygenSaturation = in.readString();
        respiratoryRate = in.readString();
        heartRate = in.readString();
        bodyTemperature = in.readString();
        bloodPressure = in.readString();
        notes = in.createStringArrayList();
        isAccepted = in.readString();
        isRejected = in.readString();
        isAcceptedBy = in.readString();
        isRejectedBy = in.readString();
        rejectionReason = in.readString();
        AcceptanceStatusDate = in.readParcelable(Timestamp.class.getClassLoader());
        Date = in.readParcelable(Timestamp.class.getClassLoader());
        longitude = in.readDouble();
        latitude = in.readDouble();
        eta = in.readString();
        distance = in.readDouble();
        finalDecision = in.readString();
        formattedAcceptanceStatusDate = in.readString();
    }

    public static final Creator<PatientModel> CREATOR = new Creator<PatientModel>() {
        @Override
        public PatientModel createFromParcel(Parcel in) {
            return new PatientModel(in);
        }

        @Override
        public PatientModel[] newArray(int size) {
            return new PatientModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(patientId);
        dest.writeString(senderEmail);
        dest.writeString(firstName);
        dest.writeString(lastName);
        dest.writeString(sex);
        dest.writeInt(age);
        dest.writeString(religion);
        dest.writeString(chiefComplaint);
        dest.writeString(signsAndSymptoms);
        dest.writeString(allergies);
        dest.writeString(medications);
        dest.writeString(pastMedicalHistory);
        dest.writeString(lastOralIntake);
        dest.writeString(eventsLeadingToPresentIllness);
        dest.writeString(oxygenSaturation);
        dest.writeString(respiratoryRate);
        dest.writeString(heartRate);
        dest.writeString(bodyTemperature);
        dest.writeString(bloodPressure);
        dest.writeStringList(notes);
        dest.writeString(isAccepted);
        dest.writeString(isRejected);
        dest.writeString(isAcceptedBy);
        dest.writeString(isRejectedBy);
        dest.writeString(rejectionReason);
        dest.writeParcelable(AcceptanceStatusDate, flags);
        dest.writeParcelable(Date, flags);
        dest.writeDouble(longitude);
        dest.writeDouble(latitude);
        dest.writeString(eta);
        dest.writeDouble(distance);
        dest.writeString(finalDecision);
        dest.writeString(formattedAcceptanceStatusDate);
    }

    public int getEtaInMinutes() {
        try {
            return Integer.parseInt(eta);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE; // Assign a large value if parsing fails
        }
    }

    public int getEtaInMinutesHospital() {
        int totalMinutes = 0;

        if (eta.contains("hour")) {
            String[] parts = eta.split("hour");
            int hours = Integer.parseInt(parts[0].trim());
            totalMinutes += hours * 60;

            if (parts.length > 1 && parts[1].contains("min")) {
                String minutesStr = parts[1].replace("mins", "").replace("min", "").trim();
                totalMinutes += Integer.parseInt(minutesStr);
            }
        } else if (eta.contains("min")) {
            String minutesStr = eta.replace("mins", "").replace("min", "").trim();
            totalMinutes += Integer.parseInt(minutesStr);
        }

        return totalMinutes;
    }
}