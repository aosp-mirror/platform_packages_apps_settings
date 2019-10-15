/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.slices;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceScreen;
import androidx.slice.Slice;
import androidx.slice.widget.SliceLiveData;
import androidx.slice.widget.SliceView;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;

/**
 * Default {@link BasePreferenceController} for {@link SliceView}. It will take {@link Uri} for
 * Slice and display what's inside this {@link Uri}
 */
public class SlicePreferenceController extends BasePreferenceController implements
        LifecycleObserver, OnStart, OnStop, Observer<Slice> {
    private static final String TAG = "SlicePreferenceController";
    @VisibleForTesting
    LiveData<Slice> mLiveData;
    @VisibleForTesting
    SlicePreference mSlicePreference;
    private Uri mUri;

    public SlicePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mSlicePreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public int getAvailabilityStatus() {
        return mUri != null ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    public void setSliceUri(Uri uri) {
        mUri = uri;
        mLiveData = SliceLiveData.fromUri(mContext, mUri, (int type, Throwable source) -> {
            Log.w(TAG, "Slice may be null. uri = " + uri + ", error = " + type);
        });

        //TODO(b/120803703): figure out why we need to remove observer first
        mLiveData.removeObserver(this);
    }

    @Override
    public void onStart() {
        if (mLiveData != null) {
            mLiveData.observeForever(this);
        }
    }

    @Override
    public void onStop() {
        if (mLiveData != null) {
            mLiveData.removeObserver(this);
        }
    }

    @Override
    public void onChanged(Slice slice) {
        mSlicePreference.onSliceUpdated(slice);
    }
}
