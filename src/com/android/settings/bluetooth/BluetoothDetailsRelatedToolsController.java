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

package com.android.settings.bluetooth;


import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityShortcutInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.UserHandle;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;

import com.android.net.module.util.CollectionUtils;
import com.android.settings.accessibility.RestrictedPreferenceHelper;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class adds related tools preference.
 */
public class BluetoothDetailsRelatedToolsController extends BluetoothDetailsController{
    private static final String KEY_RELATED_TOOLS_GROUP = "bluetooth_related_tools";
    private static final String KEY_LIVE_CAPTION = "live_caption";
    private static final int ORDINAL = 99;

    private PreferenceCategory mPreferenceCategory;

    public BluetoothDetailsRelatedToolsController(Context context,
            PreferenceFragmentCompat fragment, CachedBluetoothDevice device, Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        lifecycle.addObserver(this);
    }

    @Override
    public boolean isAvailable() {
        return mCachedDevice.isHearingAidDevice();
    }

    @Override
    protected void init(PreferenceScreen screen) {
        if (!mCachedDevice.isHearingAidDevice()) {
            return;
        }

        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        final Preference liveCaptionPreference = screen.findPreference(KEY_LIVE_CAPTION);
        if (!liveCaptionPreference.isVisible()) {
            mPreferenceCategory.removePreference(liveCaptionPreference);
        }

        final List<ComponentName> relatedToolsList =
                FeatureFactory.getFeatureFactory().getBluetoothFeatureProvider().getRelatedTools();
        if (!CollectionUtils.isEmpty(relatedToolsList)) {
            addAccessibilityInstalledRelatedPreference(relatedToolsList);
        }

        if (mPreferenceCategory.getPreferenceCount() == 0) {
            screen.removePreference(mPreferenceCategory);
        }
    }

    @Override
    protected void refresh() {}

    @Override
    public String getPreferenceKey() {
        return KEY_RELATED_TOOLS_GROUP;
    }

    private void addAccessibilityInstalledRelatedPreference(
            @NonNull List<ComponentName> componentNameList) {
        final AccessibilityManager a11yManager = AccessibilityManager.getInstance(mContext);
        final RestrictedPreferenceHelper preferenceHelper = new RestrictedPreferenceHelper(
                mContext);

        final List<AccessibilityServiceInfo> a11yServiceInfoList =
                a11yManager.getInstalledAccessibilityServiceList().stream()
                        .filter(info -> componentNameList.contains(info.getComponentName()))
                        .collect(Collectors.toList());
        final List<AccessibilityShortcutInfo> a11yShortcutInfoList =
                a11yManager.getInstalledAccessibilityShortcutListAsUser(mContext,
                        UserHandle.myUserId()).stream()
                        .filter(info -> componentNameList.contains(info.getComponentName()))
                        .collect(Collectors.toList());

        final List<RestrictedPreference> preferences = Stream.of(
                preferenceHelper.createAccessibilityServicePreferenceList(a11yServiceInfoList),
                preferenceHelper.createAccessibilityActivityPreferenceList(a11yShortcutInfoList))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        for (RestrictedPreference preference : preferences) {
            preference.setOrder(ORDINAL);
            mPreferenceCategory.addPreference(preference);
        }
    }
}
