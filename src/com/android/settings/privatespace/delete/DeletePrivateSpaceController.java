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

package com.android.settings.privatespace.delete;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;

/** Controller to delete the private space from the PS Settings page */
public class DeletePrivateSpaceController extends BasePreferenceController {
    private static final String TAG = "PrivateSpaceDeleteCtrl";

    public DeletePrivateSpaceController(@NonNull Context context, @NonNull String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public int getAvailabilityStatus() {
        return android.os.Flags.allowPrivateProfile() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        if (mPreferenceKey.equals(preference.getKey())) {
            startPrivateSpaceDeleteActivity();
            return true;
        }
        return false;
    }

    private void startPrivateSpaceDeleteActivity() {
        final Intent intent = new Intent(mContext, PrivateSpaceDeleteActivity.class);
        mContext.startActivity(intent);
    }
}
