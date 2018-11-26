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
 * limitations under the License
 */

package com.android.settings.biometrics.face;

import android.content.Context;
import android.view.View;
import android.widget.Button;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.LayoutPreference;

/**
 * Controller for the remove button.
 */
public class FaceSettingsRemoveButtonPreferenceController extends BasePreferenceController
        implements View.OnClickListener {

    private static final String KEY = "security_settings_face_delete_faces_container";

    private Button mButton;

    public FaceSettingsRemoveButtonPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    public FaceSettingsRemoveButtonPreferenceController(Context context) {
        this(context, KEY);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        mButton = ((LayoutPreference) preference)
                .findViewById(R.id.security_settings_face_settings_remove_button);
        mButton.setOnClickListener(this);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onClick(View v) {
        if (v == mButton) {

        }
    }
}
