/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static com.android.settings.display.SmartAutoRotateController.hasSufficientPermission;
import static com.android.settings.display.SmartAutoRotateController.isRotationResolverServiceAvailable;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.widget.BannerMessagePreference;

/**
 * The controller of camera based rotate permission warning preference. The preference appears when
 * the camera permission is missing for the camera based rotation feature.
 */
public class SmartAutoRotatePermissionController extends BasePreferenceController implements
        LifecycleObserver, OnResume {

    private final Intent mIntent;
    private BannerMessagePreference mPreference;

    public SmartAutoRotatePermissionController(Context context, String key) {
        super(context, key);
        final String packageName = context.getPackageManager().getRotationResolverPackageName();
        mIntent = new Intent(
                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        mIntent.setData(Uri.parse("package:" + packageName));
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        mPreference
                .setPositiveButtonText(R.string.auto_rotate_manage_permission_button)
                .setPositiveButtonOnClickListener(v -> {
                    mContext.startActivity(mIntent);
                });
    }

    @Override
    public void onResume() {
        updateState(mPreference);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (preference != null) {
            preference.setVisible(isAvailable());
        }
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return isRotationResolverServiceAvailable(mContext) && !hasSufficientPermission(mContext)
                ? AVAILABLE_UNSEARCHABLE
                : UNSUPPORTED_ON_DEVICE;
    }
}
