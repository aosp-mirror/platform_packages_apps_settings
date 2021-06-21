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
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.internal.view.RotationPolicy;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.FooterPreference;

/**
 * Preference fragment used for auto rotation
 */
@SuppressWarnings("WeakerAccess")
@SearchIndexable
public class SmartAutoRotatePreferenceFragment extends DashboardFragment {

    private static final String TAG = "SmartAutoRotatePreferenceFragment";

    private RotationPolicy.RotationPolicyListener mRotationPolicyListener;
    private AutoRotateSwitchBarController mSwitchBarController;
    @VisibleForTesting static final String AUTO_ROTATE_SWITCH_PREFERENCE_ID = "auto_rotate_switch";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.auto_rotate_settings;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        use(SmartAutoRotateController.class).init(getLifecycle());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        final SettingsActivity activity = (SettingsActivity) getActivity();
        createHeader(activity);
        final Preference footerPreference = findPreference(FooterPreference.KEY_FOOTER);
        if (footerPreference != null) {
            footerPreference.setTitle(Html.fromHtml(getString(R.string.smart_rotate_text_headline),
                    Html.FROM_HTML_MODE_COMPACT));
            footerPreference.setVisible(isRotationResolverServiceAvailable(activity));
        }
        return view;
    }

    @VisibleForTesting
    void createHeader(SettingsActivity activity) {
        if (isRotationResolverServiceAvailable(activity)) {
            final SettingsMainSwitchBar switchBar = activity.getSwitchBar();
            switchBar.setTitle(
                    getContext().getString(R.string.auto_rotate_settings_primary_switch_title));
            switchBar.show();
            mSwitchBarController = new AutoRotateSwitchBarController(activity, switchBar,
                    getSettingsLifecycle());
            findPreference(AUTO_ROTATE_SWITCH_PREFERENCE_ID).setVisible(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mRotationPolicyListener == null) {
            mRotationPolicyListener = new RotationPolicy.RotationPolicyListener() {
                @Override
                public void onChange() {
                    if (mSwitchBarController != null) {
                        mSwitchBarController.onChange();
                    }
                }
            };
        }
        RotationPolicy.registerRotationPolicyListener(getPrefContext(),
                mRotationPolicyListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mRotationPolicyListener != null) {
            RotationPolicy.unregisterRotationPolicyListener(getPrefContext(),
                    mRotationPolicyListener);
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

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.auto_rotate_settings);
}
