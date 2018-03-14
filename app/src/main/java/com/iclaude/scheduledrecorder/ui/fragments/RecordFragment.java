/*
 * Copyright (c) 2018 Claudio "iClaude" Agostini <agostini.claudio1@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */

package com.iclaude.scheduledrecorder.ui.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.iclaude.scheduledrecorder.R;
import com.melnykov.fab.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link RecordFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class RecordFragment extends Fragment {
    // Constants.
    private static final String TAG = "SCHEDULED_RECORDER_TAG";
    private static final int REQUEST_DANGEROUS_PERMISSIONS = 0;

    private final boolean marshmallow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_POSITION = "position";

    //RecordingsDao controls
    private FloatingActionButton mRecordButton = null;
    private TextView mRecordingPrompt;

    private boolean isRecording = false;
    private boolean mPauseRecording;

    private static final SimpleDateFormat mTimerFormat = new SimpleDateFormat("mm:ss", Locale.getDefault());
    private TextView tvChronometer;

    private ServiceOperations serviceOperations;


    /*
        Interface used to communicate with the Activity with regard to the connected Service.
     */
    public interface ServiceOperations {
        void requestStartRecording();

        void requestStopRecording();

        boolean isServiceConnected();

        boolean isServiceRecording();
    }

    @TargetApi(23)
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            serviceOperations = (ServiceOperations) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ServiceOperations");
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            serviceOperations = (ServiceOperations) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ServiceOperations");
        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment Record_Fragment.
     */
    public static RecordFragment newInstance(int position) {
        RecordFragment f = new RecordFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        f.setArguments(b);

        return f;
    }

    public RecordFragment() {
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View recordView = inflater.inflate(R.layout.fragment_record, container, false);

        tvChronometer = (TextView) recordView.findViewById(R.id.tvChronometer);
        tvChronometer.setText("00:00");
        //update recording prompt text
        mRecordingPrompt = (TextView) recordView.findViewById(R.id.recording_status_text);

        mRecordButton = (FloatingActionButton) recordView.findViewById(R.id.btnRecord);
        mRecordButton.setColorNormal(ContextCompat.getColor(getActivity(), R.color.primary));
        mRecordButton.setColorPressed(ContextCompat.getColor(getActivity(), R.color.primary_dark));
        if (serviceOperations != null) {
            mRecordButton.setEnabled(serviceOperations.isServiceConnected());
        }
        mRecordButton.setOnClickListener(v -> {
            if (!marshmallow) {
                startStopRecording();
            } else {
                checkPermissions();
            }
        });

        Button mPauseButton = (Button) recordView.findViewById(R.id.btnPause);
        mPauseButton.setVisibility(View.GONE); //hide pause button before recording starts
        mPauseButton.setOnClickListener(v -> {
            //onPauseRecord(mPauseRecording);
            mPauseRecording = !mPauseRecording;
        });

        /*  Are we already recording? Check necessary if Service is connected to the Activity
            before the Fragment is created: in this case the method serviceConnection(boolean
            isConnected of this Fragment is not called).
         */
        checkRecording();

        return recordView;
    }

    // Check dangerous permissions for Android Marshmallow+.
    @SuppressWarnings("ConstantConditions")
    private void checkPermissions() {
        // Check permissions.
        boolean writePerm = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean audioPerm = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        String[] arrPermissions;
        if (!writePerm && !audioPerm) {
            arrPermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO};
        } else if (!writePerm && audioPerm) {
            arrPermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};
        } else if (writePerm && !audioPerm) {
            arrPermissions = new String[]{Manifest.permission.RECORD_AUDIO};
        } else {
            startStopRecording();
            return;
        }

        // Request permissions.
        requestPermissions(arrPermissions, REQUEST_DANGEROUS_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean granted = true;
        for (int grantResult : grantResults) { // we nee all permissions granted
            if (grantResult != PackageManager.PERMISSION_GRANTED)
                granted = false;
        }

        if (granted)
            startStopRecording();
        else
            Toast.makeText(getActivity(), getString(R.string.toast_permissions_denied), Toast.LENGTH_LONG).show();
    }

    // RecordingsDao Start/Stop
    //TODO: recording pause
    private void startStopRecording() {
        if (!isRecording) { // start recording
            // Start RecordingService: send request to main Activity.
            if (serviceOperations != null) {
                serviceOperations.requestStartRecording();
            }

            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //keep screen on while recording
            isRecording = true;
        } else { //stop recording
            // Stop RecordingService: send request to main Activity.
            if (serviceOperations != null) {
                serviceOperations.requestStopRecording();
            }

            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); //allow the screen to turn off again once recording is finished
            isRecording = false;
        }
    }

    private void updateUI(boolean recording, String filePath) {
        if (recording) {
            mRecordButton.setImageResource(R.drawable.ic_media_stop);
            mRecordingPrompt.setText(getString(R.string.record_in_progress) + "...");
            Toast.makeText(getActivity(), R.string.toast_recording_start, Toast.LENGTH_SHORT).show();
        } else {
            mRecordButton.setImageResource(R.drawable.ic_mic_white_36dp);
            tvChronometer.setText("00:00");
            mRecordingPrompt.setText(getString(R.string.record_prompt));
            if (filePath != null)
                Toast.makeText(getActivity(), getString(R.string.toast_recording_finish) + " " + filePath, Toast.LENGTH_LONG).show();
        }
    }

    //TODO: implement pause recording
/*    private void onPauseRecord(boolean pause) {

    }*/

    /*
        When the Activity establishes the connection with the Service, it informs this Fragment
        so that the record button can be enabled.
     */
    public void serviceConnection(boolean isConnected) {
        mRecordButton.setEnabled(isConnected);

        // Are we already recording?
        checkRecording();
    }

    /*
        If the Service is currently recording update the UI accordingly and update the value
        of isRecording.
     */
    private void checkRecording() {
        if (serviceOperations == null) return;

        if (serviceOperations.isServiceConnected() && serviceOperations.isServiceRecording()) {
            mRecordButton.setImageResource(R.drawable.ic_media_stop);
            mRecordingPrompt.setText(getString(R.string.record_in_progress) + "...");
            isRecording = true;
        }
    }

    public void timerChanged(int seconds) {
        if (isRecording)
            tvChronometer.setText(mTimerFormat.format(new Date(seconds * 1000L)));
    }

    public void recordingStarted() {
        updateUI(true, null);
        isRecording = true;
    }

    public void recordingStopped(String filePath) {
        updateUI(false, filePath);
        isRecording = false;
    }

    @Override
    public void onDetach() {
        super.onDetach();

        if (serviceOperations != null) serviceOperations = null;
    }
}