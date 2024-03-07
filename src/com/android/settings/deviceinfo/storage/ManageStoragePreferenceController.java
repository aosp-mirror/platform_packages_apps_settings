/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.deviceinfo.storage;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

/**
 * Preference controller to control the storage management preference.
 */
public class ManageStoragePreferenceController extends BasePreferenceController {

    private int mUserId;
    private Drawable mManageStorageDrawable;

    public ManageStoragePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
    }

    /**
     * Set user ID.
     */
    public void setUserId(int userId) {
        mUserId = userId;
        mManageStorageDrawable = StorageUtils.getManageStorageIcon(mContext, userId);
    }

    @Override
    public int getAvailabilityStatus() {
        return mManageStorageDrawable == null ? CONDITIONALLY_UNAVAILABLE : AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        Preference preference = screen.findPreference(getPreferenceKey());
        preference.setIcon(mManageStorageDrawable);
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(getPreferenceKey(), preference.getKey())) {
            return super.handlePreferenceTreeClick(preference);
        }

        final MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        metricsFeatureProvider.action(mContext, SettingsEnums.STORAGE_FREE_UP_SPACE_NOW);

        final Intent intent = new Intent(StorageManager.ACTION_MANAGE_STORAGE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivityAsUser(intent, new UserHandle(mUserId));
        return true;
    }
}
