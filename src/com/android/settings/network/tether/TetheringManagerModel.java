/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.network.tether;

import android.app.Application;
import android.net.TetheringManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.List;

/**
 * TetheringManager view model implementation
 */
public class TetheringManagerModel extends AndroidViewModel {
    protected TetheringManager mTetheringManager;
    protected EventCallback mEventCallback = new EventCallback();
    protected MutableLiveData<List<String>> mTetheredInterfaces = new MutableLiveData<>();

    public TetheringManagerModel(@NonNull Application application) {
        super(application);
        mTetheringManager = application.getSystemService(TetheringManager.class);
        mTetheringManager
                .registerTetheringEventCallback(application.getMainExecutor(), mEventCallback);
    }

    @Override
    protected void onCleared() {
        mTetheringManager.unregisterTetheringEventCallback(mEventCallback);
    }

    /**
     * Gets the TetheringManager If the system service is successfully obtained.
     */
    public TetheringManager getTetheringManager() {
        return mTetheringManager;
    }

    /**
     * Gets the TetheredInterfaces wrapped by LiveData.
     */
    @NonNull
    public LiveData<List<String>> getTetheredInterfaces() {
        return Transformations.distinctUntilChanged(mTetheredInterfaces);
    }

    /**
     * Callback for use with {@link TetheringManager#registerTetheringEventCallback} to find out
     * tethering upstream status.
     */
    protected class EventCallback implements TetheringManager.TetheringEventCallback {
        @Override
        public void onTetheredInterfacesChanged(List<String> interfaces) {
            mTetheredInterfaces.setValue(interfaces);
        }
    }
}
