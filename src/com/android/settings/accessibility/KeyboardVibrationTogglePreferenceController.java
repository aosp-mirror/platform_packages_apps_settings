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

package com.android.settings.accessibility;

import static android.provider.Settings.System.KEYBOARD_VIBRATION_ENABLED;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationAttributes;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;


/**
 * A preference controller to turn on/off keyboard vibration state with a single toggle.
 */
public class KeyboardVibrationTogglePreferenceController extends TogglePreferenceController
        implements DefaultLifecycleObserver {

    private static final String TAG = "KeyboardVibrateControl";

    private static final Uri MAIN_VIBRATION_SWITCH_URI =
            Settings.System.getUriFor(VibrationPreferenceConfig.MAIN_SWITCH_SETTING_KEY);

    private final ContentObserver mContentObserver;

    private final Vibrator mVibrator;

    @Nullable
    private TwoStatePreference mPreference;

    private MetricsFeatureProvider mMetricsFeatureProvider;

    public KeyboardVibrationTogglePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mVibrator = context.getSystemService(Vibrator.class);
        Handler handler = Looper.myLooper() != null ? new Handler(/* async= */ true) : null;
        mContentObserver = new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                if (uri.equals(MAIN_VIBRATION_SWITCH_URI)) {
                    updateState(mPreference);
                } else {
                    Log.w(TAG, "Unexpected uri change:" + uri);
                }
            }
        };
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        mContext.getContentResolver().registerContentObserver(MAIN_VIBRATION_SWITCH_URI,
                /* notifyForDescendants= */ false, mContentObserver);
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        mContext.getContentResolver().unregisterContentObserver(mContentObserver);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void updateState(@Nullable Preference preference) {
        if (preference != null) {
            super.updateState(preference);
            preference.setEnabled(isPreferenceEnabled());
        }
    }

    @Override
    public int getAvailabilityStatus() {
        if (mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_keyboardVibrationSettingsSupported)) {
            return AVAILABLE;
        }
        return UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean isChecked() {
        // Always unchecked if the preference disabled
        return isPreferenceEnabled() && isKeyboardVibrationSwitchEnabled();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        final boolean success = updateKeyboardVibrationSetting(isChecked);
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_KEYBOARD_VIBRATION_CHANGED, isChecked);
        if (success && isChecked) {
            // Play the preview vibration effect for the IME feedback when the toggle is on.
            VibrationPreferenceConfig.playVibrationPreview(
                    mVibrator, VibrationAttributes.USAGE_IME_FEEDBACK);
        }
        return true;
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_accessibility;
    }

    private boolean isPreferenceEnabled() {
        return VibrationPreferenceConfig.isMainVibrationSwitchEnabled(
                mContext.getContentResolver());
    }

    private boolean isKeyboardVibrationSwitchEnabled() {
        return Settings.System.getInt(
                mContext.getContentResolver(), KEYBOARD_VIBRATION_ENABLED, ON) == ON;
    }

    private boolean updateKeyboardVibrationSetting(boolean enable) {
        final ContentResolver contentResolver = mContext.getContentResolver();
        final boolean success = Settings.System.putInt(contentResolver,
                KEYBOARD_VIBRATION_ENABLED, enable ? ON : OFF);
        contentResolver.notifyChange(Settings.System.getUriFor(KEYBOARD_VIBRATION_ENABLED),
                null /* observer */, ContentResolver.NOTIFY_NO_DELAY);
        if (!success) {
            Log.w(TAG, "Update settings database error!");
        }
        return success;
    }
}
