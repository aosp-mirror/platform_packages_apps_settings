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
import static com.android.settings.accessibility.AccessibilityDialogUtils.CustomButton;
import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
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
import androidx.preference.PreferenceScreen;

import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/** Controller that shows the magnification area mode summary and the preference click behavior. */
public class MagnificationModePreferenceController extends BasePreferenceController implements
        DialogCreatable, LifecycleObserver, OnCreate, OnResume, OnSaveInstanceState {

    static final String PREF_KEY = "screen_magnification_mode";
    private static final int DIALOG_ID_BASE = 10;
    @VisibleForTesting
    static final int DIALOG_MAGNIFICATION_MODE = DIALOG_ID_BASE + 1;
    @VisibleForTesting
    static final int DIALOG_MAGNIFICATION_SWITCH_SHORTCUT = DIALOG_ID_BASE + 2;
    @VisibleForTesting
    static final String EXTRA_MODE = "mode";

    private static final String TAG = "MagnificationModePreferenceController";
    private static final char COMPONENT_NAME_SEPARATOR = ':';

    private DialogHelper mDialogHelper;
    // The magnification mode in the dialog.
    private int mMode = MagnificationMode.NONE;
    private Preference mModePreference;

    @VisibleForTesting
    ListView mMagnificationModesListView;

    private final List<MagnificationModeInfo> mModeInfos = new ArrayList<>();

    public MagnificationModePreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        initModeInfos();
    }

    private void initModeInfos() {
        mModeInfos.add(new MagnificationModeInfo(mContext.getText(
                R.string.accessibility_magnification_mode_dialog_option_full_screen), null,
                R.drawable.ic_illustration_fullscreen, MagnificationMode.FULLSCREEN));
        mModeInfos.add(new MagnificationModeInfo(
                mContext.getText(R.string.accessibility_magnification_mode_dialog_option_window),
                null, R.drawable.ic_illustration_window, MagnificationMode.WINDOW));
        mModeInfos.add(new MagnificationModeInfo(
                mContext.getText(R.string.accessibility_magnification_mode_dialog_option_switch),
                mContext.getText(
                        R.string.accessibility_magnification_area_settings_mode_switch_summary),
                R.drawable.ic_illustration_switch, MagnificationMode.ALL));
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public CharSequence getSummary() {
        final int capabilities = MagnificationCapabilities.getCapabilities(mContext);
        return MagnificationCapabilities.getSummary(mContext, capabilities);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mMode = savedInstanceState.getInt(EXTRA_MODE, MagnificationMode.NONE);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mModePreference = screen.findPreference(getPreferenceKey());
        mModePreference.setOnPreferenceClickListener(preference -> {
            mMode = MagnificationCapabilities.getCapabilities(mContext);
            mDialogHelper.showDialog(DIALOG_MAGNIFICATION_MODE);
            return true;
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_MODE, mMode);
    }

    /**
     * Sets {@link DialogHelper} used to show the dialog.
     */
    public void setDialogHelper(DialogHelper dialogHelper) {
        mDialogHelper = dialogHelper;
        mDialogHelper.setDialogDelegate(this);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_MAGNIFICATION_MODE:
                return createMagnificationModeDialog();

            case DIALOG_MAGNIFICATION_SWITCH_SHORTCUT:
                return createMagnificationShortCutConfirmDialog();
        }
        return null;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DIALOG_MAGNIFICATION_MODE:
                return SettingsEnums.DIALOG_MAGNIFICATION_CAPABILITY;
            case DIALOG_MAGNIFICATION_SWITCH_SHORTCUT:
                return SettingsEnums.DIALOG_MAGNIFICATION_SWITCH_SHORTCUT;
            default:
                return 0;
        }
    }

    private Dialog createMagnificationModeDialog() {
        mMagnificationModesListView = AccessibilityDialogUtils.createSingleChoiceListView(
                mContext, mModeInfos, this::onMagnificationModeSelected);

        final View headerView = LayoutInflater.from(mContext).inflate(
                R.layout.accessibility_magnification_mode_header, mMagnificationModesListView,
                false);
        mMagnificationModesListView.addHeaderView(headerView, /* data= */ null, /* isSelectable= */
                false);

        mMagnificationModesListView.setItemChecked(computeSelectionIndex(), true);
        final CharSequence title = mContext.getString(
                R.string.accessibility_magnification_mode_dialog_title);

        return AccessibilityDialogUtils.createCustomDialog(mContext, title,
                mMagnificationModesListView, this::onMagnificationModeDialogPositiveButtonClicked);
    }

    private void onMagnificationModeDialogPositiveButtonClicked(DialogInterface dialogInterface,
            int which) {
        final int selectedIndex = mMagnificationModesListView.getCheckedItemPosition();
        if (selectedIndex != AdapterView.INVALID_POSITION) {
            final MagnificationModeInfo modeInfo =
                    (MagnificationModeInfo) mMagnificationModesListView.getItemAtPosition(
                            selectedIndex);
            setMode(modeInfo.mMagnificationMode);
        } else {
            Log.w(TAG, "invalid index");
        }
    }

    private void setMode(int mode) {
        mMode = mode;
        MagnificationCapabilities.setCapabilities(mContext, mMode);
        mModePreference.setSummary(
                MagnificationCapabilities.getSummary(mContext, mMode));
    }

    private void onMagnificationModeSelected(AdapterView<?> parent, View view, int position,
            long id) {
        final MagnificationModeInfo modeInfo =
                (MagnificationModeInfo) mMagnificationModesListView.getItemAtPosition(
                        position);
        if (modeInfo.mMagnificationMode == mMode) {
            return;
        }
        mMode = modeInfo.mMagnificationMode;
        if (isTripleTapEnabled(mContext) && mMode != MagnificationMode.FULLSCREEN) {
            mDialogHelper.showDialog(DIALOG_MAGNIFICATION_SWITCH_SHORTCUT);
        }
    }

    private int computeSelectionIndex() {
        final int modesSize = mModeInfos.size();
        for (int i = 0; i < modesSize; i++) {
            if (mModeInfos.get(i).mMagnificationMode == mMode) {
                return i + mMagnificationModesListView.getHeaderViewsCount();
            }
        }
        Log.w(TAG, "computeSelectionIndex failed");
        return 0;
    }

    @VisibleForTesting
    static boolean isTripleTapEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, OFF) == ON;
    }

    private Dialog createMagnificationShortCutConfirmDialog() {
        return AccessibilityDialogUtils.createMagnificationSwitchShortcutDialog(mContext,
                this::onSwitchShortcutDialogButtonClicked);
    }

    @VisibleForTesting
    void onSwitchShortcutDialogButtonClicked(@CustomButton int which) {
        optOutMagnificationFromTripleTap();
        //TODO(b/147990389): Merge this function into AccessibilityUtils after the format of
        // magnification target is changed to ComponentName.
        optInMagnificationToAccessibilityButton();
    }

    private void optOutMagnificationFromTripleTap() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, OFF);
    }

    private void optInMagnificationToAccessibilityButton() {
        final String targetKey = Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS;
        final String targetString = Settings.Secure.getString(mContext.getContentResolver(),
                targetKey);
        if (targetString != null && targetString.contains(MAGNIFICATION_CONTROLLER_NAME)) {
            return;
        }

        final StringJoiner joiner = new StringJoiner(String.valueOf(COMPONENT_NAME_SEPARATOR));

        if (!TextUtils.isEmpty(targetString)) {
            joiner.add(targetString);
        }
        joiner.add(MAGNIFICATION_CONTROLLER_NAME);

        Settings.Secure.putString(mContext.getContentResolver(), targetKey,
                joiner.toString());
    }

    // TODO(b/186731461): Remove it when this controller is used in DashBoardFragment only.
    @Override
    public void onResume() {
        updateState(mModePreference);
    }


    /**
     * An interface to help the delegate to show the dialog. It will be injected to the delegate.
     */
    interface DialogHelper extends DialogCreatable {
        void showDialog(int dialogId);
        void setDialogDelegate(DialogCreatable delegate);
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
}
