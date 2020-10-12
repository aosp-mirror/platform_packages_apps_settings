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

package com.android.settings.gestures;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;

/**
 * Preference controller for emergency sos gesture setting
 */
public class PanicGesturePreferenceController extends GesturePreferenceController {
    private static final String TAG = "PanicGesturePreferenceC";

    @VisibleForTesting
    static final int ON = 1;
    @VisibleForTesting
    static final int OFF = 0;
    @VisibleForTesting
    static final String ACTION_PANIC_SETTINGS =
            "com.android.settings.action.panic_settings";
    @VisibleForTesting
    Intent mIntent;

    private boolean mUseCustomIntent;

    private static final String PREF_KEY_VIDEO = "panic_button_screen_video";

    private static final String SECURE_KEY = Settings.Secure.PANIC_GESTURE_ENABLED;

    public PanicGesturePreferenceController(Context context, String key) {
        super(context, key);
        final String panicSettingsPackageName = context.getResources().getString(
                R.string.panic_gesture_settings_package);
        if (!TextUtils.isEmpty(panicSettingsPackageName)) {
            mUseCustomIntent = true;
            // Use custom intent if it's configured and system can resolve it.
            final Intent intent = new Intent(ACTION_PANIC_SETTINGS)
                    .setPackage(panicSettingsPackageName);
            if (canResolveIntent(intent)) {
                mIntent = intent;
            }
        }
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (TextUtils.equals(getPreferenceKey(), preference.getKey()) && mIntent != null) {
            mIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mContext.startActivity(mIntent);
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }

    @Override
    public int getAvailabilityStatus() {
        final boolean isConfigEnabled = mContext.getResources()
                .getBoolean(R.bool.config_show_panic_gesture_settings);

        if (!isConfigEnabled) {
            return UNSUPPORTED_ON_DEVICE;
        }
        return AVAILABLE;
    }

    @Override
    public boolean isSliceable() {
        return TextUtils.equals(getPreferenceKey(), "gesture_panic_button");
    }

    @Override
    protected boolean canHandleClicks() {
        return !mUseCustomIntent || mIntent != null;
    }

    @Override
    public CharSequence getSummary() {
        if (mUseCustomIntent) {
            final String packageName = mContext.getResources().getString(
                    R.string.panic_gesture_settings_package);
            try {
                final PackageManager pm = mContext.getPackageManager();
                final ApplicationInfo appInfo = pm.getApplicationInfo(
                        packageName, PackageManager.MATCH_DISABLED_COMPONENTS
                                | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS);
                return mContext.getString(R.string.panic_gesture_entrypoint_summary,
                        appInfo.loadLabel(pm));
            } catch (Exception e) {
                Log.d(TAG, "Failed to get custom summary, falling back.");
                return super.getSummary();
            }
        }

        return super.getSummary();
    }

    @Override
    protected String getVideoPrefKey() {
        return PREF_KEY_VIDEO;
    }

    @Override
    public boolean isChecked() {
        return Settings.Secure.getInt(mContext.getContentResolver(), SECURE_KEY, OFF) == ON;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        return Settings.Secure.putInt(mContext.getContentResolver(), SECURE_KEY,
                isChecked ? ON : OFF);
    }

    /**
     * Whether or not gesture page content should be suppressed from search.
     */
    public boolean shouldSuppressFromSearch() {
        return mUseCustomIntent;
    }

    private boolean canResolveIntent(Intent intent) {
        final ResolveInfo resolveActivity = mContext.getPackageManager()
                .resolveActivity(intent, 0);
        return resolveActivity != null;
    }
}
