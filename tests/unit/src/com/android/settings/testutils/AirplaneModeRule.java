/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.testutils;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.test.core.app.ApplicationProvider;

import org.junit.rules.ExternalResource;

/** A test rule that is used to manager the Airplane Mode resource for testing. */
public final class AirplaneModeRule extends ExternalResource {

    private static final String TAG = "AirplaneModeRule";

    private Context mContext;
    private ContentResolver mContentResolver;
    private boolean mBackupValue;
    private boolean mShouldRestore;

    @Override
    protected void before() throws Throwable {
        mContext = ApplicationProvider.getApplicationContext();
        mContentResolver = mContext.getContentResolver();
    }

    @Override
    protected void after() {
        if (!mShouldRestore) {
            return;
        }
        Log.d(TAG, "Restore Airplane Mode value:" + mBackupValue);
        Settings.Global.putInt(mContentResolver, Settings.Global.AIRPLANE_MODE_ON,
                mBackupValue ? 1 : 0);
    }

    public void setAirplaneMode(boolean enable) {
        if (enable == isAirplaneModeOn()) {
            return;
        }
        if (!mShouldRestore) {
            mShouldRestore = true;
            mBackupValue = !enable;
            Log.d(TAG, "Backup Airplane Mode value:" + mBackupValue);
        }
        Log.d(TAG, "Set Airplane Mode enable:" + enable);
        Settings.Global.putInt(mContentResolver, Settings.Global.AIRPLANE_MODE_ON, enable ? 1 : 0);
    }

    public boolean isAirplaneModeOn() {
        return Settings.Global.getInt(mContext.getContentResolver(),
            Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }
}
