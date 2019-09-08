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
 * limitations under the License
 */

package com.android.settings.biometrics.face;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Button;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.widget.LayoutPreference;

/**
 * Preference controller that allows a user to enroll their face.
 */
public class FaceSettingsEnrollButtonPreferenceController extends BasePreferenceController
        implements View.OnClickListener {

    private static final String TAG = "FaceSettings/Remove";
    static final String KEY = "security_settings_face_enroll_faces_container";

    private int mUserId;
    private byte[] mToken;
    private SettingsActivity mActivity;
    private Button mButton;
    private boolean mIsClicked;

    public FaceSettingsEnrollButtonPreferenceController(Context context) {
        this(context, KEY);
    }

    public FaceSettingsEnrollButtonPreferenceController(Context context,
            String preferenceKey) {
        super(context, preferenceKey);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        mButton = ((LayoutPreference) preference)
                .findViewById(R.id.security_settings_face_settings_enroll_button);
        mButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        mIsClicked = true;
        final Intent intent = new Intent();
        intent.setClassName("com.android.settings", FaceEnrollIntroduction.class.getName());
        intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
        mContext.startActivity(intent);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    public void setUserId(int userId) {
        mUserId = userId;
    }

    public void setToken(byte[] token) {
        mToken = token;
    }

    // Return the click state, then clear its state.
    public boolean isClicked() {
        final boolean wasClicked = mIsClicked;
        mIsClicked = false;
        return wasClicked;
    }

    public void setActivity(SettingsActivity activity) {
        mActivity = activity;
    }
}
