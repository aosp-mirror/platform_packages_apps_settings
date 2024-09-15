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

package com.android.settings.privatespace;

import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.widget.IllustrationPreference;

public class HidePrivateSpaceSettings extends DashboardFragment {
    private static final String TAG = "HidePrivateSpaceSettings";
    private static final int IMPORTANT_FOR_ACCESSIBILITY_ITEM_COUNT = 5;
    private static final String PRIVATE_SPACE_HIDE_ILLUSTRATION_KEY =
            "private_space_hide_illustration";

    @Override
    public void onCreate(Bundle icicle) {
        if (android.os.Flags.allowPrivateProfile()
                && android.multiuser.Flags.enablePrivateSpaceFeatures()) {
            super.onCreate(icicle);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        final int itemCount =  countPreferencesRecursive(preferenceScreen);
        root.setAccessibilityDelegate(
                new View.AccessibilityDelegate() {
                    @Override
                    public void onInitializeAccessibilityNodeInfo(
                            @NonNull View host, @NonNull AccessibilityNodeInfo info) {
                        super.onInitializeAccessibilityNodeInfo(host, info);
                        //TODO(b/346712220)  - Replace the hardcoded accessibility count with
                        //value computed from xml Preference
                        info.setCollectionInfo(
                                new AccessibilityNodeInfo.CollectionInfo.Builder()
                                        .setRowCount(itemCount)
                                        .setColumnCount(1)
                                        .setItemCount(itemCount)
                                        .setImportantForAccessibilityItemCount(
                                                IMPORTANT_FOR_ACCESSIBILITY_ITEM_COUNT)
                                        .build()
                        );
                    }
                });
        return  root;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (PrivateSpaceMaintainer.getInstance(getContext()).isPrivateSpaceLocked()) {
            finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        final IllustrationPreference illustrationPreference =
                getPreferenceScreen().findPreference(PRIVATE_SPACE_HIDE_ILLUSTRATION_KEY);
        illustrationPreference.applyDynamicColor();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVATE_SPACE_SETTINGS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.private_space_hide_locked;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    private int countPreferencesRecursive(PreferenceGroup preferenceGroup) {
        int count = preferenceGroup.getPreferenceCount();
        for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
            Preference preference = preferenceGroup.getPreference(i);
            if (preference instanceof PreferenceGroup) {
                count += countPreferencesRecursive((PreferenceGroup) preference);
            }
        }
        return count;
    }
}
