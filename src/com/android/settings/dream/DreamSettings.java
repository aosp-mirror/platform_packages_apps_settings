/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.dream;

import static com.android.settings.dream.DreamMainSwitchPreferenceController.MAIN_SWITCH_PREF_KEY;
import static com.android.settingslib.dream.DreamBackend.EITHER;
import static com.android.settingslib.dream.DreamBackend.NEVER;
import static com.android.settingslib.dream.DreamBackend.WHILE_CHARGING;
import static com.android.settingslib.dream.DreamBackend.WHILE_DOCKED;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Switch;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.dream.DreamBackend.WhenToDream;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.MainSwitchPreference;
import com.android.settingslib.widget.OnMainSwitchChangeListener;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable
public class DreamSettings extends DashboardFragment implements OnMainSwitchChangeListener {

    private static final String TAG = "DreamSettings";
    static final String WHILE_CHARGING_ONLY = "while_charging_only";
    static final String WHILE_DOCKED_ONLY = "while_docked_only";
    static final String EITHER_CHARGING_OR_DOCKED = "either_charging_or_docked";
    static final String NEVER_DREAM = "never";

    private MainSwitchPreference mMainSwitchPreference;
    private Button mPreviewButton;
    private RecyclerView mRecyclerView;

    @WhenToDream
    static int getSettingFromPrefKey(String key) {
        switch (key) {
            case WHILE_CHARGING_ONLY:
                return WHILE_CHARGING;
            case WHILE_DOCKED_ONLY:
                return WHILE_DOCKED;
            case EITHER_CHARGING_OR_DOCKED:
                return EITHER;
            case NEVER_DREAM:
            default:
                return NEVER;
        }
    }

    static String getKeyFromSetting(@WhenToDream int dreamSetting) {
        switch (dreamSetting) {
            case WHILE_CHARGING:
                return WHILE_CHARGING_ONLY;
            case WHILE_DOCKED:
                return WHILE_DOCKED_ONLY;
            case EITHER:
                return EITHER_CHARGING_OR_DOCKED;
            case NEVER:
            default:
                return NEVER_DREAM;
        }
    }

    static int getDreamSettingDescriptionResId(@WhenToDream int dreamSetting,
            boolean enabledOnBattery) {
        switch (dreamSetting) {
            case WHILE_CHARGING:
                return R.string.screensaver_settings_summary_sleep;
            case WHILE_DOCKED:
                return enabledOnBattery ? R.string.screensaver_settings_summary_dock
                        : R.string.screensaver_settings_summary_dock_and_charging;
            case EITHER:
                return R.string.screensaver_settings_summary_either_long;
            case NEVER:
            default:
                return R.string.screensaver_settings_summary_never;
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DREAM;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.dream_fragment_overview;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_screen_saver;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }

    public static CharSequence getSummaryTextWithDreamName(Context context) {
        DreamBackend backend = DreamBackend.getInstance(context);
        return getSummaryTextFromBackend(backend, context);
    }

    @VisibleForTesting
    static CharSequence getSummaryTextFromBackend(DreamBackend backend, Context context) {
        if (backend.isEnabled()) {
            return context.getString(R.string.screensaver_settings_summary_on,
                    backend.getActiveDreamName());
        } else {
            return context.getString(R.string.screensaver_settings_summary_off);
        }
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new WhenToDreamPreferenceController(context));
        return controllers;
    }

    private void setAllPreferencesEnabled(boolean isEnabled) {
        getPreferenceControllers().forEach(controllers -> {
            controllers.forEach(controller -> {
                final String prefKey = controller.getPreferenceKey();
                if (prefKey.equals(MAIN_SWITCH_PREF_KEY)) {
                    return;
                }
                final Preference pref = findPreference(prefKey);
                if (pref != null) {
                    pref.setEnabled(isEnabled);
                    controller.updateState(pref);
                }
            });
        });
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final DreamBackend dreamBackend = DreamBackend.getInstance(getContext());

        mMainSwitchPreference = findPreference(MAIN_SWITCH_PREF_KEY);
        if (mMainSwitchPreference != null) {
            mMainSwitchPreference.addOnSwitchChangeListener(this);
        }

        setAllPreferencesEnabled(dreamBackend.isEnabled());
    }

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
            Bundle bundle) {
        final DreamBackend dreamBackend = DreamBackend.getInstance(getContext());

        final ViewGroup root = getActivity().findViewById(android.R.id.content);
        mPreviewButton = (Button) getActivity().getLayoutInflater().inflate(
                R.layout.dream_preview_button, root, false);
        mPreviewButton.setVisibility(dreamBackend.isEnabled() ? View.VISIBLE : View.GONE);
        root.addView(mPreviewButton);
        mPreviewButton.setOnClickListener(v -> dreamBackend.preview(dreamBackend.getActiveDream()));

        mRecyclerView = super.onCreateRecyclerView(inflater, parent, bundle);
        updatePaddingForPreviewButton();
        return mRecyclerView;
    }

    private void updatePaddingForPreviewButton() {
        mPreviewButton.post(() -> {
            mRecyclerView.setPadding(0, 0, 0, mPreviewButton.getMeasuredHeight());
        });
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        setAllPreferencesEnabled(isChecked);
        mPreviewButton.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        updatePaddingForPreviewButton();
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.dream_fragment_overview);
}

