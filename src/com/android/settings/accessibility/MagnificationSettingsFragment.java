/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.accessibility;

import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME;
import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/** Settings page for magnification. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class MagnificationSettingsFragment extends DashboardFragment {

    private static final String TAG = "MagnificationSettingsFragment";
    private static final String PREF_KEY_MODE = "magnification_mode";
    @VisibleForTesting
    static final int DIALOG_MAGNIFICATION_CAPABILITY = 1;
    @VisibleForTesting
    static final int DIALOG_MAGNIFICATION_SWITCH_SHORTCUT = 2;

    @VisibleForTesting
    static final String EXTRA_CAPABILITY = "capability";
    private static final int NONE = 0;
    private static final char COMPONENT_NAME_SEPARATOR = ':';

    private Preference mModePreference;
    @VisibleForTesting
    Dialog mDialog;

    @VisibleForTesting
    ListView mMagnificationModesListView;
    private int mCapabilities = NONE;

    private final List<MagnificationModeInfo> mModeInfos = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mCapabilities = savedInstanceState.getInt(EXTRA_CAPABILITY, NONE);
        }
        if (mCapabilities == NONE) {
            mCapabilities = MagnificationCapabilities.getCapabilities(getPrefContext());
        }
        initModeInfos();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initModePreference();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_CAPABILITY, mCapabilities);
        super.onSaveInstanceState(outState);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_MAGNIFICATION_SETTINGS;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DIALOG_MAGNIFICATION_CAPABILITY:
                return SettingsEnums.DIALOG_MAGNIFICATION_CAPABILITY;
            case DIALOG_MAGNIFICATION_SWITCH_SHORTCUT:
                return SettingsEnums.DIALOG_MAGNIFICATION_SWITCH_SHORTCUT;
            default:
                return 0;
        }
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_magnification_service_settings;
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        final CharSequence title;

        switch (dialogId) {
            case DIALOG_MAGNIFICATION_CAPABILITY:
                mDialog = createMagnificationModeDialog();
                return mDialog;
            case DIALOG_MAGNIFICATION_SWITCH_SHORTCUT:
                title = getPrefContext().getString(
                        R.string.accessibility_magnification_switch_shortcut_title);
                mDialog = AccessibilityEditDialogUtils.showMagnificationSwitchShortcutDialog(
                        getPrefContext(), title, this::onSwitchShortcutDialogPositiveButtonClicked);
                return mDialog;
        }
        throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
    }

    private Dialog createMagnificationModeDialog() {
        mMagnificationModesListView = AccessibilityEditDialogUtils.createSingleChoiceListView(
                getPrefContext(), mModeInfos, this::onMagnificationModeSelected);

        final View headerView = LayoutInflater.from(getPrefContext()).inflate(
                R.layout.accessibility_magnification_mode_header, mMagnificationModesListView,
                false);
        mMagnificationModesListView.addHeaderView(headerView, null, /* isSelectable= */false);

        mMagnificationModesListView.setItemChecked(computeSelectedMagnificationModeIndex(), true);
        final CharSequence title = getPrefContext().getString(
                R.string.accessibility_magnification_mode_dialog_title);

        return AccessibilityEditDialogUtils.createCustomDialog(getPrefContext(), title,
                mMagnificationModesListView, this::onMagnificationModeDialogPositiveButtonClicked);
    }

    private int computeSelectedMagnificationModeIndex() {
        final int size = mModeInfos.size();
        for (int i = 0; i < size; i++) {
            if (mModeInfos.get(i).mMagnificationMode == mCapabilities) {
                return i + mMagnificationModesListView.getHeaderViewsCount();
            }
        }
        Log.w(TAG, "chosen mode" + mCapabilities + "is not in the list");
        return 0;
    }

    private void onMagnificationModeSelected(AdapterView<?> parent, View view, int position,
            long id) {
        final MagnificationModeInfo modeInfo =
                (MagnificationModeInfo) mMagnificationModesListView.getItemAtPosition(position);
        if (modeInfo.mMagnificationMode == mCapabilities) {
            return;
        }
        mCapabilities = modeInfo.mMagnificationMode;
        if (isTripleTapEnabled() && mCapabilities != MagnificationMode.FULLSCREEN) {
            showDialog(DIALOG_MAGNIFICATION_SWITCH_SHORTCUT);
        }
    }

    private void onMagnificationModeDialogPositiveButtonClicked(DialogInterface dialogInterface,
            int which) {
        final int selectedIndex = mMagnificationModesListView.getCheckedItemPosition();
        if (selectedIndex != AdapterView.INVALID_POSITION) {
            final MagnificationModeInfo modeInfo =
                    (MagnificationModeInfo) mMagnificationModesListView.getItemAtPosition(
                            selectedIndex);
            updateCapabilities(modeInfo.mMagnificationMode);
        } else {
            Log.w(TAG, "no checked item in the list");
        }
    }

    private void updateCapabilities(int mode) {
        mCapabilities = mode;
        MagnificationCapabilities.setCapabilities(getPrefContext(), mCapabilities);
        mModePreference.setSummary(
                MagnificationCapabilities.getSummary(getPrefContext(), mCapabilities));
    }

    private void initModeInfos() {
        mModeInfos.clear();
        mModeInfos.add(new MagnificationModeInfo(getPrefContext().getText(
                R.string.accessibility_magnification_mode_dialog_option_full_screen), null,
                R.drawable.accessibility_magnification_full_screen, MagnificationMode.FULLSCREEN));
        mModeInfos.add(new MagnificationModeInfo(getPrefContext().getText(
                R.string.accessibility_magnification_mode_dialog_option_window), null,
                R.drawable.accessibility_magnification_window_screen, MagnificationMode.WINDOW));
        mModeInfos.add(new MagnificationModeInfo(getPrefContext().getText(
                R.string.accessibility_magnification_mode_dialog_option_switch),
                getPrefContext().getText(
                        R.string.accessibility_magnification_area_settings_mode_switch_summary),
                R.drawable.accessibility_magnification_switch, MagnificationMode.ALL));
    }

    @VisibleForTesting
    static class MagnificationModeInfo extends ItemInfoArrayAdapter.ItemInfo {
        @MagnificationMode
        public final int mMagnificationMode;

        MagnificationModeInfo(@NonNull CharSequence title, @Nullable CharSequence summary,
                @DrawableRes int drawableId, @MagnificationMode int magnificationMode) {
            super(title, summary, drawableId);
            mMagnificationMode = magnificationMode;
        }
    }

    private void initModePreference() {
        mModePreference = findPreference(PREF_KEY_MODE);
        mModePreference.setOnPreferenceClickListener(preference -> {
            mCapabilities = MagnificationCapabilities.getCapabilities(getPrefContext());
            showDialog(DIALOG_MAGNIFICATION_CAPABILITY);
            return true;
        });
    }

    private void onSwitchShortcutDialogPositiveButtonClicked(View view) {
        //TODO(b/147990389): Merge this function into util until magnification change format to
        // Component.
        optOutMagnificationFromTripleTap();
        optInMagnificationToAccessibilityButton();

        mDialog.dismiss();
    }

    private void optOutMagnificationFromTripleTap() {
        Settings.Secure.putInt(getPrefContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, OFF);
    }

    private void optInMagnificationToAccessibilityButton() {
        final String targetKey = Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS;
        final String targetString = Settings.Secure.getString(getPrefContext().getContentResolver(),
                targetKey);
        if (targetString.contains(MAGNIFICATION_CONTROLLER_NAME)) {
            return;
        }

        final StringJoiner joiner = new StringJoiner(String.valueOf(COMPONENT_NAME_SEPARATOR));

        if (!TextUtils.isEmpty(targetString)) {
            joiner.add(targetString);
        }
        joiner.add(MAGNIFICATION_CONTROLLER_NAME);

        Settings.Secure.putString(getPrefContext().getContentResolver(), targetKey,
                joiner.toString());
    }

    private boolean isTripleTapEnabled() {
        return Settings.Secure.getInt(getPrefContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, OFF) == ON;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_magnification_service_settings);
}
