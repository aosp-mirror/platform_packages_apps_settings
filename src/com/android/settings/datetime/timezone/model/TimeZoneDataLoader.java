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

package com.android.settings.datetime.timezone.model;

import android.content.Context;
import android.os.Bundle;

import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;

import com.android.settingslib.utils.AsyncLoaderCompat;

public class TimeZoneDataLoader extends AsyncLoaderCompat<TimeZoneData> {

    public TimeZoneDataLoader(Context context) {
        super(context);
    }

    @Override
    public TimeZoneData loadInBackground() {
        // Heavy operation due to reading the underlying file
        return TimeZoneData.getInstance();
    }

    @Override
    protected void onDiscardResult(TimeZoneData result) {
        // This class doesn't hold resource of the result.
    }

    public interface OnDataReadyCallback {
        void onTimeZoneDataReady(TimeZoneData data);
    }

    public static class LoaderCreator implements LoaderManager.LoaderCallbacks<TimeZoneData> {

        private final Context mContext;
        private final OnDataReadyCallback mCallback;

        public LoaderCreator(Context context, OnDataReadyCallback callback) {
            mContext = context;
            mCallback = callback;
        }

        @Override
        public Loader onCreateLoader(int id, Bundle args) {
            return new TimeZoneDataLoader(mContext);
        }

        @Override
        public void onLoadFinished(Loader<TimeZoneData> loader, TimeZoneData data) {
            if (mCallback != null) {
                mCallback.onTimeZoneDataReady(data);
            }
        }

        @Override
        public void onLoaderReset(Loader<TimeZoneData> loader) {
            //It's okay to keep the time zone data when loader is reset
        }
    }
}
