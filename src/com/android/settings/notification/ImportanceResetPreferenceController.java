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

package com.android.settings.notification;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.LayoutPreference;

public class ImportanceResetPreferenceController extends BasePreferenceController implements
        View.OnClickListener {

    public static final String KEY = "asst_importance_reset";
    private static final String TAG = "ResetImportanceButton";

    private NotificationBackend mBackend;
    private Button mButton;

    public ImportanceResetPreferenceController(Context context, String key) {
        super(context, key);
        mBackend = new NotificationBackend();
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        mButton = ((LayoutPreference) preference)
                .findViewById(R.id.reset_importance_button);
        mButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        mBackend.resetNotificationImportance();
        Toast.makeText(mContext, R.string.reset_importance_completed, Toast.LENGTH_SHORT)
                            .show();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

}

