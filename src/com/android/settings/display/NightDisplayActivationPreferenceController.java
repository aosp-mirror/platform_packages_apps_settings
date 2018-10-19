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

package com.android.settings.display;

import android.content.Context;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import com.android.internal.app.ColorDisplayController;
import com.android.settings.R;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.core.TogglePreferenceController;

public class NightDisplayActivationPreferenceController extends TogglePreferenceController {

    private ColorDisplayController mController;
    private NightDisplayTimeFormatter mTimeFormatter;
    private Button mTurnOffButton;
    private Button mTurnOnButton;

    private final OnClickListener mListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mController.setActivated(!mController.isActivated());
            updateStateInternal();
        }
    };

    public NightDisplayActivationPreferenceController(Context context, String key) {
        super(context, key);
        mController = new ColorDisplayController(context);
        mTimeFormatter = new NightDisplayTimeFormatter(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return ColorDisplayController.isAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "night_display_activated");
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        final LayoutPreference preference = (LayoutPreference) screen.findPreference(
                getPreferenceKey());
        mTurnOnButton = preference.findViewById(R.id.night_display_turn_on_button);
        mTurnOnButton.setOnClickListener(mListener);
        mTurnOffButton = preference.findViewById(R.id.night_display_turn_off_button);
        mTurnOffButton.setOnClickListener(mListener);
    }

    @Override
    public final void updateState(Preference preference) {
        updateStateInternal();
    }

    /** FOR SLICES */

    @Override
    public boolean isChecked() {
        return mController.isActivated();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return mController.setActivated(isChecked);
    }

    @Override
    public CharSequence getSummary() {
        return mTimeFormatter.getAutoModeTimeSummary(mContext, mController);
    }

    private void updateStateInternal() {
        if (mTurnOnButton == null || mTurnOffButton == null) {
            return;
        }

        final boolean isActivated = mController.isActivated();
        final int autoMode = mController.getAutoMode();

        String buttonText;
        if (autoMode == ColorDisplayController.AUTO_MODE_CUSTOM) {
            buttonText = mContext.getString(isActivated
                            ? R.string.night_display_activation_off_custom
                            : R.string.night_display_activation_on_custom,
                    mTimeFormatter.getFormattedTimeString(isActivated
                            ? mController.getCustomStartTime()
                            : mController.getCustomEndTime()));
        } else if (autoMode == ColorDisplayController.AUTO_MODE_TWILIGHT) {
            buttonText = mContext.getString(isActivated
                    ? R.string.night_display_activation_off_twilight
                    : R.string.night_display_activation_on_twilight);
        } else {
            buttonText = mContext.getString(isActivated
                    ? R.string.night_display_activation_off_manual
                    : R.string.night_display_activation_on_manual);
        }

        if (isActivated) {
            mTurnOnButton.setVisibility(View.GONE);
            mTurnOffButton.setVisibility(View.VISIBLE);
            mTurnOffButton.setText(buttonText);
        } else {
            mTurnOnButton.setVisibility(View.VISIBLE);
            mTurnOffButton.setVisibility(View.GONE);
            mTurnOnButton.setText(buttonText);
        }
    }
}
