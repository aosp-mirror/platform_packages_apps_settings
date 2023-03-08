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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

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
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

import java.util.ArrayList;
import java.util.List;

/** Controller that shows the magnification area mode summary and the preference click behavior. */
public class MagnificationModePreferenceController extends BasePreferenceController implements
        DialogCreatable, LifecycleObserver, OnCreate, OnResume, OnSaveInstanceState {

    static final String PREF_KEY = "screen_magnification_mode";
    private static final int DIALOG_ID_BASE = 10;
    @VisibleForTesting
    static final int DIALOG_MAGNIFICATION_MODE = DIALOG_ID_BASE + 1;
    @VisibleForTesting
    static final int DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING = DIALOG_ID_BASE + 2;
    @VisibleForTesting
    static final String EXTRA_MODE = "mode";

    private static final String TAG = "MagnificationModePreferenceController";

    private DialogHelper mDialogHelper;
    // The magnification mode in the dialog.
    @MagnificationMode
    private int mModeCache = MagnificationMode.NONE;
    private Preference mModePreference;
    private ShortcutPreference mLinkPreference;

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
                R.drawable.a11y_magnification_mode_fullscreen, MagnificationMode.FULLSCREEN));
        mModeInfos.add(new MagnificationModeInfo(
                mContext.getText(R.string.accessibility_magnification_mode_dialog_option_window),
                null, R.drawable.a11y_magnification_mode_window, MagnificationMode.WINDOW));
        mModeInfos.add(new MagnificationModeInfo(
                mContext.getText(R.string.accessibility_magnification_mode_dialog_option_switch),
                mContext.getText(
                        R.string.accessibility_magnification_area_settings_mode_switch_summary),
                R.drawable.a11y_magnification_mode_switch, MagnificationMode.ALL));
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
            mModeCache = savedInstanceState.getInt(EXTRA_MODE, MagnificationMode.NONE);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mModePreference = screen.findPreference(getPreferenceKey());
        mLinkPreference = screen.findPreference(
                ToggleFeaturePreferenceFragment.KEY_SHORTCUT_PREFERENCE);
        mModePreference.setOnPreferenceClickListener(preference -> {
            mModeCache = MagnificationCapabilities.getCapabilities(mContext);
            mDialogHelper.showDialog(DIALOG_MAGNIFICATION_MODE);
            return true;
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_MODE, mModeCache);
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

            case DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING:
                return createMagnificationTripleTapWarningDialog();
        }
        return null;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DIALOG_MAGNIFICATION_MODE:
                return SettingsEnums.DIALOG_MAGNIFICATION_CAPABILITY;
            case DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING:
                return SettingsEnums.DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING;
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
        final CharSequence positiveBtnText = mContext.getString(R.string.save);
        final CharSequence negativeBtnText = mContext.getString(R.string.cancel);

        return AccessibilityDialogUtils.createCustomDialog(mContext, title,
                mMagnificationModesListView,
                positiveBtnText, this::onMagnificationModeDialogPositiveButtonClicked,
                negativeBtnText, /* negativeListener= */ null);
    }

    @VisibleForTesting
    void onMagnificationModeDialogPositiveButtonClicked(DialogInterface dialogInterface,
            int which) {
        final int selectedIndex = mMagnificationModesListView.getCheckedItemPosition();
        if (selectedIndex == AdapterView.INVALID_POSITION) {
            Log.w(TAG, "invalid index");
            return;
        }

        mModeCache = ((MagnificationModeInfo) mMagnificationModesListView.getItemAtPosition(
                        selectedIndex)).mMagnificationMode;

        // Do not save mode until user clicks positive button in triple tap warning dialog.
        if (isTripleTapEnabled(mContext) && mModeCache != MagnificationMode.FULLSCREEN) {
            mDialogHelper.showDialog(DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING);
        } else { // Save mode (capabilities) value, don't need to show dialog to confirm.
            updateCapabilitiesAndSummary(mModeCache);
        }
    }

    private void updateCapabilitiesAndSummary(@MagnificationMode int mode) {
        mModeCache = mode;
        MagnificationCapabilities.setCapabilities(mContext, mModeCache);
        mModePreference.setSummary(
                MagnificationCapabilities.getSummary(mContext, mModeCache));
    }

    private void onMagnificationModeSelected(AdapterView<?> parent, View view, int position,
            long id) {
        final MagnificationModeInfo modeInfo =
                (MagnificationModeInfo) mMagnificationModesListView.getItemAtPosition(
                        position);
        if (modeInfo.mMagnificationMode == mModeCache) {
            return;
        }
        mModeCache = modeInfo.mMagnificationMode;
    }

    private int computeSelectionIndex() {
        final int modesSize = mModeInfos.size();
        for (int i = 0; i < modesSize; i++) {
            if (mModeInfos.get(i).mMagnificationMode == mModeCache) {
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

    private Dialog createMagnificationTripleTapWarningDialog() {
        final View contentView = LayoutInflater.from(mContext).inflate(
                R.layout.magnification_triple_tap_warning_dialog, /* root= */ null);
        final CharSequence title = mContext.getString(
                R.string.accessibility_magnification_triple_tap_warning_title);
        final CharSequence positiveBtnText = mContext.getString(
                R.string.accessibility_magnification_triple_tap_warning_positive_button);
        final CharSequence negativeBtnText = mContext.getString(
                R.string.accessibility_magnification_triple_tap_warning_negative_button);

        final Dialog dialog = AccessibilityDialogUtils.createCustomDialog(mContext, title,
                contentView,
                positiveBtnText, this::onMagnificationTripleTapWarningDialogPositiveButtonClicked,
                negativeBtnText, this::onMagnificationTripleTapWarningDialogNegativeButtonClicked);

        updateLinkInTripleTapWarningDialog(dialog, contentView);

        return dialog;
    }

    private void updateLinkInTripleTapWarningDialog(Dialog dialog, View contentView) {
        final TextView messageView = contentView.findViewById(R.id.message);
        // TODO(b/225682559): Need to remove performClick() after refactoring accessibility dialog.
        final View.OnClickListener linkListener = view -> {
            updateCapabilitiesAndSummary(mModeCache);
            mLinkPreference.performClick();
            dialog.dismiss();
        };
        final AnnotationSpan.LinkInfo linkInfo = new AnnotationSpan.LinkInfo(
                AnnotationSpan.LinkInfo.DEFAULT_ANNOTATION, linkListener);
        final CharSequence textWithLink = AnnotationSpan.linkify(mContext.getText(
                R.string.accessibility_magnification_triple_tap_warning_message), linkInfo);

        if (messageView != null) {
            messageView.setText(textWithLink);
            messageView.setMovementMethod(LinkMovementMethod.getInstance());
        }
        dialog.setContentView(contentView);
    }

    @VisibleForTesting
    void onMagnificationTripleTapWarningDialogNegativeButtonClicked(
            DialogInterface dialogInterface, int which) {
        mModeCache = MagnificationCapabilities.getCapabilities(mContext);
        mDialogHelper.showDialog(DIALOG_MAGNIFICATION_MODE);
    }

    @VisibleForTesting
    void onMagnificationTripleTapWarningDialogPositiveButtonClicked(
            DialogInterface dialogInterface, int which) {
        updateCapabilitiesAndSummary(mModeCache);
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
