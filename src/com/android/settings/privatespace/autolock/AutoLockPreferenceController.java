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

package com.android.settings.privatespace.autolock;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.privatespace.PrivateSpaceMaintainer;

public class AutoLockPreferenceController extends BasePreferenceController {
    private static final String TAG = "AutoLockPreferenceCtrl";
    private final CharSequence[] mAutoLockRadioOptions;
    private final PrivateSpaceMaintainer mPrivateSpaceMaintainer;

    public AutoLockPreferenceController(@NonNull Context context, @NonNull String key) {
        super(context, key);
        mPrivateSpaceMaintainer = PrivateSpaceMaintainer.getInstance(context);
        mAutoLockRadioOptions =
                context.getResources().getStringArray(R.array.private_space_auto_lock_options);
    }

    @Override
    public int getAvailabilityStatus() {
        return android.os.Flags.allowPrivateProfile()
                        && android.multiuser.Flags.supportAutolockForPrivateSpace()
                ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @NonNull
    @Override
    public CharSequence getSummary() {
        try {
            return mAutoLockRadioOptions[mPrivateSpaceMaintainer.getPrivateSpaceAutoLockSetting()];
        } catch (ArrayIndexOutOfBoundsException exception) {
            Log.e(TAG, "Invalid private space auto lock setting value" + exception.getMessage());
        }
        return mAutoLockRadioOptions[PrivateSpaceMaintainer.PRIVATE_SPACE_AUTO_LOCK_DEFAULT_VAL];
    }
}
