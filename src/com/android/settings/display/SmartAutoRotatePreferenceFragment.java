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

import static com.android.settings.display.SmartAutoRotateController.isRotationResolverServiceAvailable;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.widget.FooterPreference;

import java.util.List;

/**
 * Preference fragment used for auto rotation
 */
@SuppressWarnings("WeakerAccess")
@SearchIndexable
public class SmartAutoRotatePreferenceFragment extends DashboardFragment {

    private static final String TAG = "SmartAutoRotatePreferenceFragment";

    @VisibleForTesting
    static final String AUTO_ROTATE_MAIN_SWITCH_PREFERENCE_KEY = "auto_rotate_main_switch";
    @VisibleForTesting
    static final String AUTO_ROTATE_SWITCH_PREFERENCE_KEY = "auto_rotate_switch";
    private static final String KEY_FOOTER_PREFERENCE = "auto_rotate_footer_preference";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.auto_rotate_settings;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        DeviceStateAutoRotationHelper.initControllers(
                getLifecycle(),
                useAll(DeviceStateAutoRotateSettingController.class)
        );
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return DeviceStateAutoRotationHelper.createPreferenceControllers(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        final SettingsActivity activity = (SettingsActivity) getActivity();
        createHeader(activity);
        final Preference footerPreference = findPreference(KEY_FOOTER_PREFERENCE);
        if (footerPreference != null) {
            footerPreference.setVisible(isRotationResolverServiceAvailable(activity));
            setupFooter();
        }
        return view;
    }

    @VisibleForTesting
    void createHeader(SettingsActivity activity) {
        boolean deviceStateRotationEnabled =
                DeviceStateAutoRotationHelper.isDeviceStateRotationEnabled(activity);
        if (isRotationResolverServiceAvailable(activity) && !deviceStateRotationEnabled) {
            findPreference(AUTO_ROTATE_SWITCH_PREFERENCE_KEY).setVisible(false);
        } else {
            findPreference(AUTO_ROTATE_MAIN_SWITCH_PREFERENCE_KEY).setVisible(false);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DISPLAY_AUTO_ROTATE_SETTINGS;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_auto_rotate_settings;
    }

    // Updates the footer for this page.
    @VisibleForTesting
    void setupFooter() {
        final String mHelpUri = getString(getHelpResource());
        if (!TextUtils.isEmpty(mHelpUri)) {
            addHelpLink();
        }
    }

    // Changes the text to include a learn more link if the link is defined.
    @VisibleForTesting
    void addHelpLink() {
        final FooterPreference pref = findPreference(KEY_FOOTER_PREFERENCE);
        if (pref != null) {
            pref.setLearnMoreAction(v -> {
                startActivityForResult(HelpUtils.getHelpIntent(getContext(),
                        getString(getHelpResource()),
                        /*backupContext=*/ ""), /*requestCode=*/ 0);
            });
            pref.setLearnMoreText(getString(R.string.auto_rotate_link_a11y));
        }
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.auto_rotate_settings) {

                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(
                        Context context, boolean enabled) {
                    return DeviceStateAutoRotationHelper.getRawDataToIndex(context, enabled);
                }
            };
}
