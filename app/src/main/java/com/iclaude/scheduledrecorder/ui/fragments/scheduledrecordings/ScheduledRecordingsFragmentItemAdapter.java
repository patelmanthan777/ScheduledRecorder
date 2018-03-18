/*
 * Copyright (c) 2018 Claudio "iClaude" Agostini <agostini.claudio1@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */

package com.iclaude.scheduledrecorder.ui.fragments.scheduledrecordings;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.iclaude.scheduledrecorder.R;
import com.iclaude.scheduledrecorder.database.ScheduledRecording;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter of the RecyclerView within ScheduledRecordingsFragment.
 */

public class ScheduledRecordingsFragmentItemAdapter extends RecyclerView.Adapter<ScheduledRecordingsFragmentItemAdapter.ItemViewHolder> {
    private final String TAG = "SCHEDULED_RECORDER_TAG";

    public interface MyOnItemClickListener {
        void onItemClick(ScheduledRecording item);

        void onItemLongClick(ScheduledRecording item);
    }


    // ViewHolder.
    public class ItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvStart;
        private final TextView tvEnd;
        private final TextView tvColor;

        public ItemViewHolder(View v) {
            super(v);
            tvStart = v.findViewById(R.id.tvStart);
            tvEnd = v.findViewById(R.id.tvEnd);
            tvColor = v.findViewById(R.id.tvColor);

            v.setOnClickListener(view -> {
                int pos = recyclerView.getChildAdapterPosition(view);
                if (pos >= 0 && pos < getItemCount()) {
                    listener.onItemClick(items.get(pos));
                }
            });

            v.setOnLongClickListener(view -> {
                int pos = recyclerView.getChildAdapterPosition(view);
                if (pos >= 0 && pos < getItemCount()) {
                    listener.onItemLongClick(items.get(pos));
                }
                return true;
            });
        }
    }

    // Adapter.
    private List<ScheduledRecording> items;
    private final MyOnItemClickListener listener;
    private final RecyclerView recyclerView;
    private final int[] colors = {Color.argb(255, 255, 193, 7),
            Color.argb(255, 244, 67, 54), Color.argb(255, 99, 233, 112),
            Color.argb(255, 7, 168, 255), Color.argb(255, 255, 7, 251),
            Color.argb(255, 255, 61, 7), Color.argb(255, 205, 7, 255)};

    public ScheduledRecordingsFragmentItemAdapter(List<ScheduledRecording> items, MyOnItemClickListener listener, RecyclerView recyclerView) {
        this.items = items;
        this.recyclerView = recyclerView;
        this.listener = listener;
    }

    public void setItems(List<ScheduledRecording> items) {
        this.items = items;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater
                .from(viewGroup.getContext())
                .inflate(R.layout.fragment_scheduled_recordings_item, viewGroup, false);

        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        ScheduledRecording item = items.get(position);
        if (item == null) return;
        DateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.tvStart.setText(dateFormat.format(new Date(item.getStart())));
        holder.tvEnd.setText(dateFormat.format(new Date(item.getEnd())));

        int posCol = position % (colors.length);
        holder.tvColor.setBackgroundColor(colors[posCol]);
    }
}
