package com.example.ambulink.Model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class HospitalModel implements Parcelable {
    private String id;
    private String eta;
    private String name;
    private String address;
    private int capacity;
    private double latitude;
    private double longitude;
    private double distance; // Added field for distance
    private int slotsAvailable; // Added field for available slots
    private int maxSlots; // Added field for max slots

    // New fields for acceptance/rejection status
    private String isAccepted;
    private String isRejected;
    private String isAcceptedBy;
    private String isRejectedBy;
    private String rejectionReason;
    private Timestamp AcceptanceStatusDate;

    // New field for patient ID
    private String patientId;

    // New field for pending status
    private boolean isPending;

    //Hospital Tyle
    private String hospitalType;

    private int maxDoctors,maxNurses,numDoctors,numNurses;

    public HospitalModel() {}

    // Constructor to initialize the hospital model
    public HospitalModel(String id, String eta, String name, String address, double latitude, double longitude, int capacity, int maxSlots,
                         String rejectionReason, Timestamp AcceptanceStatusDate, String patientId, String hospitalType, int maxDoctors,
                         int maxNurses, int numDoctors, int numNurses) {
        this.id = id;
        this.eta = eta;
        this.name = name;
        this.address = address;
        this.capacity = capacity;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = Double.MAX_VALUE; // Default distance value
        this.slotsAvailable = capacity; // Default slots available
        this.maxSlots = maxSlots; // Default max slots
        this.isPending = false; // Default isPending value
        this.rejectionReason = rejectionReason;
        this.patientId = patientId;
        this.AcceptanceStatusDate = AcceptanceStatusDate;
        this.hospitalType = hospitalType;
        this.maxDoctors = maxDoctors;
        this.maxNurses = maxNurses;
        this.numDoctors = numDoctors;
        this.numNurses = numNurses;
    }

    // Parcelable constructor (used when reconstructing object from a Parcel)
    protected HospitalModel(Parcel in) {
        id = in.readString();
        eta = in.readString();
        name = in.readString();
        address = in.readString();
        capacity = in.readInt();
        latitude = in.readDouble();
        longitude = in.readDouble();
        distance = in.readDouble();
        slotsAvailable = in.readInt();
        maxSlots = in.readInt();
        // Reading new fields
        isAccepted = in.readString();
        isRejected = in.readString();
        isAcceptedBy = in.readString();
        isRejectedBy = in.readString();
        patientId = in.readString(); // Reading patientId from parcel
        isPending = in.readByte() != 0; // Read isPending as boolean
        rejectionReason = in.readString();
        AcceptanceStatusDate = in.readParcelable(Timestamp.class.getClassLoader());
        hospitalType = in.readString();
        maxDoctors = in.readInt();
        maxNurses = in.readInt();
        numDoctors = in.readInt();
        numNurses = in.readInt();
    }

    // Parcelable Creator
    public static final Creator<HospitalModel> CREATOR = new Creator<HospitalModel>() {
        @Override
        public HospitalModel createFromParcel(Parcel in) {
            return new HospitalModel(in);
        }

        @Override
        public HospitalModel[] newArray(int size) {
            return new HospitalModel[size];
        }
    };

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getEta() {
        return eta;
    }

    public void setEta(String eta) {
        this.eta = eta;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public int getCapacity() {
        return capacity;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getSlotsAvailable() {
        return slotsAvailable;
    }

    public void setSlotsAvailable(int slotsAvailable) {
        this.slotsAvailable = slotsAvailable;
    }

    public int getMaxSlots() {
        return maxSlots;
    }

    public void setMaxSlots(int maxSlots) {
        this.maxSlots = maxSlots;
    }

    // Getters and setters for the new fields
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

    // Getters and setters for patientId
    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    // Getters and setters for isPending
    public boolean isPending() {
        return isPending;
    }

    public void setPending(boolean pending) {
        isPending = pending;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Timestamp getAcceptanceStatusDate() {
        return AcceptanceStatusDate;
    }

    public void setAcceptanceStatusDate(Timestamp acceptanceStatusDate) {
        AcceptanceStatusDate = acceptanceStatusDate;
    }

    // Method to convert Timestamp to String
    public String getFormattedAcceptanceStatusDate() {
        if (AcceptanceStatusDate != null) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(AcceptanceStatusDate.toDate());
        }
        return "Date not available";
    }

    public String getHospitalType() {
        return hospitalType;
    }

    public void setHospitalType(String hospitalType) {
        this.hospitalType = hospitalType;
    }

    public int getMaxDoctors() {
        return maxDoctors;
    }

    public void setMaxDoctors(int maxDoctors) {
        this.maxDoctors = maxDoctors;
    }

    public int getMaxNurses() {
        return maxNurses;
    }

    public void setMaxNurses(int maxNurses) {
        this.maxNurses = maxNurses;
    }

    public int getNumDoctors() {
        return numDoctors;
    }

    public void setNumDoctors(int numDoctors) {
        this.numDoctors = numDoctors;
    }

    public int getNumNurses() {
        return numNurses;
    }

    public void setNumNurses(int numNurses) {
        this.numNurses = numNurses;
    }

    // Implement Parcelable methods
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(eta);
        dest.writeString(name);
        dest.writeString(address);
        dest.writeInt(capacity);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeDouble(distance);
        dest.writeInt(slotsAvailable);
        dest.writeInt(maxSlots);
        // Writing new fields
        dest.writeString(isAccepted);
        dest.writeString(isRejected);
        dest.writeString(isAcceptedBy);
        dest.writeString(isRejectedBy);
        dest.writeString(patientId); // Writing patientId to parcel
        dest.writeByte((byte) (isPending ? 1 : 0)); // Write isPending as byte (boolean)
        dest.writeString(rejectionReason);
        dest.writeParcelable(AcceptanceStatusDate, flags);
        dest.writeString(hospitalType);
        dest.writeInt(maxDoctors);
        dest.writeInt(maxNurses);
        dest.writeInt(numDoctors);
        dest.writeInt(numNurses);
    }
}

