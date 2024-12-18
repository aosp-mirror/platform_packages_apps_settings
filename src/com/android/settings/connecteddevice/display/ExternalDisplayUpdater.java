/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.display;

import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isDisplayAllowed;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.connecteddevice.DevicePreferenceCallback;
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DisplayListener;
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.Injector;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class ExternalDisplayUpdater {

    private static final String PREF_KEY = "external_display_settings";
    private final int mMetricsCategory;
    @NonNull
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    @NonNull
    private final Runnable mUpdateRunnable = this::update;
    @NonNull
    private final DevicePreferenceCallback mDevicePreferenceCallback;
    @Nullable
    private RestrictedPreference mPreference;
    @Nullable
    private Injector mInjector;
    private final DisplayListener mListener =  new DisplayListener() {
        @Override
        public void update(int displayId) {
            scheduleUpdate();
        }
    };

    public ExternalDisplayUpdater(@NonNull DevicePreferenceCallback callback, int metricsCategory) {
        mDevicePreferenceCallback = callback;
        mMetricsCategory = metricsCategory;
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    /**
     * Set the context to generate the {@link Preference}, so it could get the correct theme.
     */
    public void initPreference(@NonNull Context context) {
        initPreference(context, new Injector(context));
    }

    @VisibleForTesting
    void initPreference(@NonNull Context context, Injector injector) {
        mInjector = injector;
        mPreference = new RestrictedPreference(context, null /* AttributeSet */);
        mPreference.setTitle(R.string.external_display_settings_title);
        mPreference.setSummary(getSummary());
        mPreference.setIcon(getDrawable(context));
        mPreference.setKey(PREF_KEY);
        mPreference.setDisabledByAdmin(checkIfUsbDataSignalingIsDisabled(context));
        mPreference.setOnPreferenceClickListener((Preference p) -> {
            mMetricsFeatureProvider.logClickedPreference(p, mMetricsCategory);
            // New version - uses a separate screen.
            new SubSettingLauncher(context)
                    .setDestination(ExternalDisplayPreferenceFragment.class.getName())
                    .setTitleRes(R.string.external_display_settings_title)
                    .setSourceMetricsCategory(mMetricsCategory)
                    .launch();
            return true;
        });

        scheduleUpdate();
    }

    /**
     * Unregister the display listener.
     */
    public void unregisterCallback() {
        if (mInjector != null) {
            mInjector.unregisterDisplayListener(mListener);
        }
    }

    /**
     * Register the display listener.
     */
    public void registerCallback() {
        if (mInjector != null) {
            mInjector.registerDisplayListener(mListener);
        }
    }

    @VisibleForTesting
    @Nullable
    protected RestrictedLockUtils.EnforcedAdmin checkIfUsbDataSignalingIsDisabled(Context context) {
        return RestrictedLockUtilsInternal.checkIfUsbDataSignalingIsDisabled(context,
                    UserHandle.myUserId());
    }

    @VisibleForTesting
    @Nullable
    protected Drawable getDrawable(Context context) {
        return context.getDrawable(R.drawable.ic_external_display_32dp);
    }

    @Nullable
    protected CharSequence getSummary() {
        if (mInjector == null) {
            return null;
        }
        var context = mInjector.getContext();
        if (context == null) {
            return null;
        }

        for (var display : mInjector.getEnabledDisplays()) {
            if (display != null && isDisplayAllowed(display, mInjector)) {
                return context.getString(R.string.external_display_on);
            }
        }

        for (var display : mInjector.getAllDisplays()) {
            if (display != null && isDisplayAllowed(display, mInjector)) {
                return context.getString(R.string.external_display_off);
            }
        }

        return null;
    }

    private void scheduleUpdate() {
        if (mInjector == null) {
            return;
        }
        unscheduleUpdate();
        mInjector.getHandler().post(mUpdateRunnable);
    }

    private void unscheduleUpdate() {
        if (mInjector == null) {
            return;
        }
        mInjector.getHandler().removeCallbacks(mUpdateRunnable);
    }

    private void update() {
        var summary = getSummary();
        if (mPreference == null) {
            return;
        }
        mPreference.setSummary(summary);
        if (summary != null) {
            mDevicePreferenceCallback.onDeviceAdded(mPreference);
        } else {
            mDevicePreferenceCallback.onDeviceRemoved(mPreference);
        }
    }
}
