package com.example.ambulink.Adapter;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ambulink.R;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.ViewHolder> {

    private final List<String> dataList;
    private final OnSpeechRequestListener speechRequestListener;

    // Interface for speech-to-text requests
    public interface OnSpeechRequestListener {
        void onSpeechRequest(EditText editText);
    }

    public NotesAdapter(List<String> dataList, OnSpeechRequestListener speechRequestListener) {
        if (dataList.isEmpty()) {
            dataList.add(""); // Add an empty note if the list is empty
        }
        this.dataList = dataList;
        this.speechRequestListener = speechRequestListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String data = dataList.get(position);
        holder.editText.setText(data);

        // Remove any existing TextWatcher
        if (holder.editText.getTag() instanceof TextWatcher) {
            holder.editText.removeTextChangedListener((TextWatcher) holder.editText.getTag());
        }

        // Create a new TextWatcher
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) { // Check for valid position
                    dataList.set(adapterPosition, s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        // Set the new TextWatcher
        holder.editText.addTextChangedListener(textWatcher);
        holder.editText.setTag(textWatcher);

        // Set click listener for speech-to-text
        holder.editText.setOnTouchListener((view, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (holder.editText.getRight() - holder.editText.getCompoundDrawables()[2].getBounds().width())) {
                    if (speechRequestListener != null) {
                        speechRequestListener.onSpeechRequest(holder.editText);
                    }
                    return true; // Event consumed
                }
            }
            return false;
        });

        // Handle Remove Button Click
        holder.removeButton.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                removeItem(adapterPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public void addItem() {
        dataList.add(""); // Add a new empty item
        notifyItemInserted(dataList.size() - 1);
    }

    public void removeItem(int position) {
        if (position >= 0 && position < dataList.size()) {
            dataList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, dataList.size());
        }
    }

    public List<String> getNotes() {
        return dataList;
    }

    // Add the setNotes method to allow setting a new list of notes
    public void setNotes(List<String> notes) {
        dataList.clear();
        dataList.addAll(notes);
        notifyDataSetChanged(); // Notify adapter of data changes
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public EditText editText;
        public Button removeButton;

        public ViewHolder(View itemView) {
            super(itemView);
            editText = itemView.findViewById(R.id.notes);
            removeButton = itemView.findViewById(R.id.removeButton);
        }
    }
}
