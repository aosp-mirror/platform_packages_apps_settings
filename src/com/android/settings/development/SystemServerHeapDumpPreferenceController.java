/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.development;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.UserManager;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.development.DeveloperOptionsPreferenceController;

public class SystemServerHeapDumpPreferenceController extends DeveloperOptionsPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_SYSTEM_SERVER_HEAP_DUMP = "system_server_heap_dump";

    /** How long to keep the preference disabled before re-enabling. */
    private static final long ENABLE_TIMEOUT_MILLIS = 5000L;

    private final UserManager mUserManager;

    private Handler mHandler;

    public SystemServerHeapDumpPreferenceController(Context context) {
        super(context);

        mUserManager = context.getSystemService(UserManager.class);
        mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public boolean isAvailable() {
        return Build.IS_DEBUGGABLE
                && !mUserManager.hasUserRestriction(UserManager.DISALLOW_DEBUGGING_FEATURES);
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SYSTEM_SERVER_HEAP_DUMP;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!KEY_SYSTEM_SERVER_HEAP_DUMP.equals(preference.getKey())) {
            return false;
        }
        try {
            // Temporarily disable the preference so the user doesn't start two dumps in a row.
            preference.setEnabled(false);
            Toast.makeText(mContext, R.string.capturing_system_heap_dump_message,
                    Toast.LENGTH_SHORT).show();
            ActivityManager.getService().requestSystemServerHeapDump();
            mHandler.postDelayed(() -> preference.setEnabled(true), ENABLE_TIMEOUT_MILLIS);
            return true;
        } catch (RemoteException e) {
            Log.e(TAG, "error taking system heap dump", e);
            Toast.makeText(mContext, R.string.error_capturing_system_heap_dump_message,
                    Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
