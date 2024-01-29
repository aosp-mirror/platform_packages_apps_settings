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
import android.hardware.display.ColorDisplayManager;
import android.text.TextUtils;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.MainSwitchPreference;

/**
 * Controller that updates the night display.
 */
public class NightDisplayActivationPreferenceController extends
        TogglePreferenceController implements OnCheckedChangeListener {

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private ColorDisplayManager mColorDisplayManager;
    private NightDisplayTimeFormatter mTimeFormatter;
    private MainSwitchPreference mPreference;

    public NightDisplayActivationPreferenceController(Context context, String key) {
        super(context, key);

        mColorDisplayManager = context.getSystemService(ColorDisplayManager.class);
        mTimeFormatter = new NightDisplayTimeFormatter(context);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public int getAvailabilityStatus() {
        return ColorDisplayManager.isNightDisplayAvailable(mContext) ? AVAILABLE_UNSEARCHABLE
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
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mPreference = (MainSwitchPreference) screen.findPreference(getPreferenceKey());
        mPreference.addOnSwitchChangeListener(this);
        mPreference.updateStatus(mColorDisplayManager.isNightDisplayActivated());
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final boolean activated = mColorDisplayManager.isNightDisplayActivated();
        if (isChecked != activated) {
            // TODO(b/179017365): Create a controller which extends TogglePreferenceController to
            //  control the toggle preference.
            setChecked(isChecked);
        }
    }

    @Override
    public final void updateState(Preference preference) {
        updateStateInternal();
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

    private void updateStateInternal() {
        final boolean isActivated = mColorDisplayManager.isNightDisplayActivated();
        final int autoMode = mColorDisplayManager.getNightDisplayAutoMode();

        if (autoMode == ColorDisplayManager.AUTO_MODE_CUSTOM_TIME) {
            mTimeFormatter.getFormattedTimeString(isActivated
                    ? mColorDisplayManager.getNightDisplayCustomStartTime()
                    : mColorDisplayManager.getNightDisplayCustomEndTime());
        }
    }
}
