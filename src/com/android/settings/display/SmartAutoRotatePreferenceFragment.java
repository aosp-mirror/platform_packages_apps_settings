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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.preference.Preference;

import com.android.internal.view.RotationPolicy;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SettingsMainSwitchBar;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

/**
 * Preference fragment used to auto rotation
 */
@SuppressWarnings("WeakerAccess")
@SearchIndexable
public class SmartAutoRotatePreferenceFragment extends DashboardFragment {

    private static final String TAG = "SmartAutoRotatePreferenceFragment";

    private RotationPolicy.RotationPolicyListener mRotationPolicyListener;
    private AutoRotateSwitchBarController mSwitchBarController;
    private static final String FACE_SWITCH_PREFERENCE_ID = "face_based_rotate";
    private static final String SMART_AUTO_ROTATE_CONTROLLER_KEY = "auto_rotate";

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.auto_rotate_settings;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        final SettingsActivity activity = (SettingsActivity) getActivity();
        final SettingsMainSwitchBar switchBar = activity.getSwitchBar();
        switchBar.setTitle(
                getContext().getString(R.string.auto_rotate_settings_primary_switch_title));
        switchBar.show();
        mSwitchBarController = new AutoRotateSwitchBarController(activity, switchBar,
                getSettingsLifecycle());
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mRotationPolicyListener == null) {
            mRotationPolicyListener = new RotationPolicy.RotationPolicyListener() {
                @Override
                public void onChange() {
                    mSwitchBarController.onChange();
                    final boolean isLocked = RotationPolicy.isRotationLocked(getContext());
                    final Preference preference = findPreference(FACE_SWITCH_PREFERENCE_ID);
                    if (preference != null) {
                        preference.setEnabled(!isLocked);
                    }
                }
            };
        }
        RotationPolicy.registerRotationPolicyListener(getPrefContext(),
                mRotationPolicyListener);

        findPreference(FooterPreference.KEY_FOOTER).setTitle(
                Html.fromHtml(getString(R.string.smart_rotate_text_headline),
                        Html.FROM_HTML_MODE_COMPACT));
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
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(
            Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(
                new SmartAutoRotatePreferenceController(context, SMART_AUTO_ROTATE_CONTROLLER_KEY));
        return controllers;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.auto_rotate_settings) {

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(
                        Context context) {
                    return buildPreferenceControllers(context);
                }

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return false;
                }
            };
}
