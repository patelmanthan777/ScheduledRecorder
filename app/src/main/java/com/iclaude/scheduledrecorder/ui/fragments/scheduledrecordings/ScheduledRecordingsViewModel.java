/*
 * Copyright (c) 2018 Claudio "iClaude" Agostini <agostini.claudio1@gmail.com>
 * Licensed under the Apache License, Version 2.0
 */

package com.iclaude.scheduledrecorder.ui.fragments.scheduledrecordings;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableField;
import android.support.annotation.VisibleForTesting;

import com.iclaude.scheduledrecorder.R;
import com.iclaude.scheduledrecorder.SingleLiveEvent;
import com.iclaude.scheduledrecorder.database.RecordingsRepository;
import com.iclaude.scheduledrecorder.database.RecordingsRepositoryInterface;
import com.iclaude.scheduledrecorder.database.ScheduledRecording;
import com.iclaude.scheduledrecorder.didagger2.App;
import com.iclaude.scheduledrecorder.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

/**
 * ViewModel for ScheduledRecordingsFragment.
 */

public class ScheduledRecordingsViewModel extends ViewModel {

    private static final String TAG = "SCHEDULED_RECORDER_TAG";
    @Inject
    RecordingsRepository recordingsRepository;
    private ScheduledRecording deletedRecording;

    // Observables.
    private LiveData<List<ScheduledRecording>> listLive;
    private final MutableLiveData<List<ScheduledRecording>> listFilteredLive = new MutableLiveData<>();
    public final ObservableBoolean dataLoading = new ObservableBoolean(false);
    public final ObservableBoolean dataAvailable = new ObservableBoolean(false);
    public final ObservableField<Date> selectedDate = new ObservableField<>(new Date(System.currentTimeMillis()));
    public final ObservableField<Date> selectedMonth = new ObservableField<>(new Date(System.currentTimeMillis()));

    // Commands.
    private final SingleLiveEvent<Void> addCommand = new SingleLiveEvent<>();
    private final SingleLiveEvent<ScheduledRecording> editCommand = new SingleLiveEvent<>();
    private final SingleLiveEvent<Integer> deleteCommand = new SingleLiveEvent<>();
    private final SingleLiveEvent<Boolean> undoDeleteCommand = new SingleLiveEvent<>();


    public ScheduledRecordingsViewModel() {
        App.getComponent().inject(this);
    }

    @VisibleForTesting()
    public ScheduledRecordingsViewModel(RecordingsRepository recordingsRepository) {
        this.recordingsRepository = recordingsRepository;
    }

    // Observables.
    public LiveData<List<ScheduledRecording>> getScheduledRecordings() {
        if(listLive != null)
            return listLive; // we already have the list cached and there are no changes

        dataLoading.set(true);
        listLive = recordingsRepository.getAllScheduledRecordings();
        dataLoading.set(false);

        return listLive;
    }

    public LiveData<List<ScheduledRecording>> getScheduledRecordingsFiltered() {
        return listFilteredLive;
    }

    public void filterList() {
        if(listLive == null || listLive.getValue() == null)
            return;

        long filterStart = Utils.getDayStartTimeLong(Objects.requireNonNull(selectedDate.get()));
        long filterEnd = Utils.getDayEndTimeLong(Objects.requireNonNull(selectedDate.get()));

        List<ScheduledRecording> scheduledRecordingsFiltered = new ArrayList<>();
        for(ScheduledRecording rec : listLive.getValue()) {
            if ((rec.getStart() >= filterStart && rec.getStart() <= filterEnd) || (rec.getEnd() >= filterStart && rec.getEnd() <= filterEnd))
                scheduledRecordingsFiltered.add(rec);
        }

        Collections.sort(scheduledRecordingsFiltered);
        listFilteredLive.setValue(scheduledRecordingsFiltered);
    }

    public void setSelectedDate(Date selectedDate) {
        this.selectedDate.set(selectedDate);
        filterList();
    }

    // Commands.
    public SingleLiveEvent<Void> getAddCommand() {
        return addCommand;
    }

    public void addScheduledRecording() {
        addCommand.call();
    }

    public SingleLiveEvent<ScheduledRecording> getEditCommand() {
        return editCommand;
    }

    public void editScheduledRecording(ScheduledRecording scheduledRecording) {
        editCommand.setValue(scheduledRecording);
    }

    public SingleLiveEvent<Integer> getDeleteCommand() {
        return deleteCommand;
    }

    public SingleLiveEvent<Boolean> getUndoDeleteCommand() {
        return undoDeleteCommand;
    }

    public void deleteScheduledRecording(ScheduledRecording scheduledRecording) {
        recordingsRepository.deleteScheduledRecording(scheduledRecording, new RecordingsRepositoryInterface.OperationResult() {
            @Override
            public void onSuccess() {
                deletedRecording = scheduledRecording;
                deleteCommand.setValue(R.string.toast_recording_deleted);
            }

            @Override
            public void onFailure() {
                deleteCommand.setValue(R.string.toast_recording_deleted_error);
            }
        });
    }

    public void undoDelete() {
        if(deletedRecording == null) {
            undoDeleteCommand.setValue(false);
            return;
        }

        recordingsRepository.insertScheduledRecording(deletedRecording, new RecordingsRepositoryInterface.OperationResult() {
            @Override
            public void onSuccess() {
                deletedRecording = null;
                undoDeleteCommand.setValue(true);
            }

            @Override
            public void onFailure() {
                undoDeleteCommand.setValue(false);
            }
        });
    }
}
