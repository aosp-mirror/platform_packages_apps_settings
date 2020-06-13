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

import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED;

import android.content.Context;
import android.hardware.display.ColorDisplayManager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.LayoutPreference;

public class NightDisplayActivationPreferenceController extends TogglePreferenceController {

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private ColorDisplayManager mColorDisplayManager;
    private NightDisplayTimeFormatter mTimeFormatter;
    private LayoutPreference mPreference;

    private Button mTurnOffButton;
    private Button mTurnOnButton;

    private final OnClickListener mListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mMetricsFeatureProvider.logClickedPreference(mPreference, getMetricsCategory());
            mColorDisplayManager.setNightDisplayActivated(
                    !mColorDisplayManager.isNightDisplayActivated());
            updateStateInternal(true);
        }
    };

    public NightDisplayActivationPreferenceController(Context context, String key) {
        super(context, key);

        mColorDisplayManager = context.getSystemService(ColorDisplayManager.class);
        mTimeFormatter = new NightDisplayTimeFormatter(context);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return ColorDisplayManager.isNightDisplayAvailable(mContext) ? AVAILABLE
                : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "night_display_activated");
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = screen.findPreference(getPreferenceKey());
        mTurnOnButton = mPreference.findViewById(R.id.night_display_turn_on_button);
        mTurnOnButton.setOnClickListener(mListener);
        mTurnOffButton = mPreference.findViewById(R.id.night_display_turn_off_button);
        mTurnOffButton.setOnClickListener(mListener);
    }

    @Override
    public final void updateState(Preference preference) {
        updateStateInternal(false);
    }

    /** FOR SLICES */

    @Override
    public boolean isChecked() {
        return mColorDisplayManager.isNightDisplayActivated();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return mColorDisplayManager.setNightDisplayActivated(isChecked);
    }

    @Override
    public CharSequence getSummary() {
        return mTimeFormatter.getAutoModeSummary(mContext, mColorDisplayManager);
    }

    private void updateStateInternal(boolean selfChanged) {
        if (mTurnOnButton == null || mTurnOffButton == null) {
            return;
        }

        final boolean isActivated = mColorDisplayManager.isNightDisplayActivated();
        final int autoMode = mColorDisplayManager.getNightDisplayAutoMode();

        String buttonText;
        if (autoMode == ColorDisplayManager.AUTO_MODE_CUSTOM_TIME) {
            buttonText = mContext.getString(isActivated
                            ? R.string.night_display_activation_off_custom
                            : R.string.night_display_activation_on_custom,
                    mTimeFormatter.getFormattedTimeString(isActivated
                            ? mColorDisplayManager.getNightDisplayCustomStartTime()
                            : mColorDisplayManager.getNightDisplayCustomEndTime()));
        } else if (autoMode == ColorDisplayManager.AUTO_MODE_TWILIGHT) {
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
            if (selfChanged) {
                mTurnOffButton.sendAccessibilityEvent(TYPE_VIEW_FOCUSED);
            }
        } else {
            mTurnOnButton.setVisibility(View.VISIBLE);
            mTurnOffButton.setVisibility(View.GONE);
            mTurnOnButton.setText(buttonText);
            if (selfChanged) {
                mTurnOnButton.sendAccessibilityEvent(TYPE_VIEW_FOCUSED);
            }
        }
    }
}
