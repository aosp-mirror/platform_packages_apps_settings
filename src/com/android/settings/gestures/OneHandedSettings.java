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

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityShortcutPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;

/**
 * Fragment for One-handed mode settings
 *
 * <p>The child {@link AccessibilityShortcutPreferenceFragment} shows the actual UI for
 * providing basic accessibility shortcut service setup.
 */
public class OneHandedSettings extends AccessibilityShortcutPreferenceFragment {
    private static final String ONE_HANDED_SHORTCUT_KEY = "one_handed_shortcuts_preference";
    private String mFeatureName;

    @Override
    protected void updatePreferenceStates() {
        OneHandedSettingsUtils.setUserId(UserHandle.myUserId());
        super.updatePreferenceStates();
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        final int dialogMetrics = super.getDialogMetricsCategory(dialogId);
        return dialogMetrics == SettingsEnums.ACTION_UNKNOWN ? SettingsEnums.SETTINGS_ONE_HANDED
                : dialogMetrics;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_ONE_HANDED;
    }

    @Override
    protected String getShortcutPreferenceKey() {
        return ONE_HANDED_SHORTCUT_KEY;
    }

    @Override
    protected boolean showGeneralCategory() {
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    protected ComponentName getComponentName() {
        return AccessibilityShortcutController.ONE_HANDED_COMPONENT_NAME;
    }

    @Override
    protected CharSequence getLabelName() {
        return mFeatureName;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.one_handed_settings;
    }

    @Override
    protected String getLogTag() {
        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mFeatureName = getContext().getString(R.string.one_handed_title);
        super.onCreate(savedInstanceState);
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.one_handed_settings) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return OneHandedSettingsUtils.isSupportOneHandedMode();
                }
            };
}
