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

package com.android.settings.biometrics2.ui.viewmodel;

import android.content.res.Configuration;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.android.systemui.unfold.compat.ScreenSizeFoldProvider;
import com.android.systemui.unfold.updates.FoldProvider;

import java.util.concurrent.Executor;

/**
 * ViewModel explaining the fingerprint sensor location for fingerprint enrollment.
 */
public class DeviceFoldedViewModel extends ViewModel {

    private static final String TAG = "DeviceFoldedViewModel";

    @NonNull private final MutableLiveData<Boolean> mLiveData =
            new MutableLiveData<>(null);

    private final ScreenSizeFoldProvider mScreenSizeFoldProvider;
    private final FoldProvider.FoldCallback mIsFoldedCallback = isFolded -> {
        Log.d(TAG, "onFoldUpdated= " + isFolded);
        mLiveData.postValue(isFolded);
    };

    public DeviceFoldedViewModel(@NonNull ScreenSizeFoldProvider screenSizeFoldProvider,
            @NonNull Executor executor) {
        super();
        mScreenSizeFoldProvider = screenSizeFoldProvider;
        mScreenSizeFoldProvider.registerCallback(mIsFoldedCallback, executor);
    }

    /**
     * Calls this method when activity gets configuration change
     */
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        mScreenSizeFoldProvider.onConfigurationChange(newConfig);
    }

    /**
     * Returns FoldedLiveData
     */
    public LiveData<Boolean> getLiveData() {
        return mLiveData;
    }

    @Override
    protected void onCleared() {
        mScreenSizeFoldProvider.unregisterCallback(mIsFoldedCallback);
        super.onCleared();
    }
}
