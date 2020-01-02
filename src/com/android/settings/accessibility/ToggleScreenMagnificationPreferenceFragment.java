/*
 * Copyright (C) 2013 The Android Open Source Project
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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Switch;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.ShortcutType;
import com.android.settings.widget.SwitchBar;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

public class ToggleScreenMagnificationPreferenceFragment extends
        ToggleFeaturePreferenceFragment implements SwitchBar.OnSwitchChangeListener,
        ShortcutPreference.OnClickListener {

    private static final String SETTINGS_KEY = "screen_magnification_settings";
    private static final String EXTRA_SHORTCUT_TYPE = "shortcutType";
    // TODO(b/142530063): Check the new setting key to decide which summary should be shown.
    private static final String KEY_SHORTCUT_TYPE = Settings.System.MASTER_MONO;
    private ShortcutPreference mShortcutPreference;
    private int mShortcutType = ShortcutType.DEFAULT;
    private CheckBox mSoftwareTypeCheckBox;
    private CheckBox mHardwareTypeCheckBox;
    private CheckBox mTripleTapTypeCheckBox;

    protected class VideoPreference extends Preference {
        private ImageView mVideoBackgroundView;
        private OnGlobalLayoutListener mLayoutListener;

        public VideoPreference(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder view) {
            super.onBindViewHolder(view);
            Resources res = getPrefContext().getResources();
            final int backgroundAssetWidth = res.getDimensionPixelSize(
                    R.dimen.screen_magnification_video_background_width);
            final int videoAssetWidth = res
                    .getDimensionPixelSize(R.dimen.screen_magnification_video_width);
            final int videoAssetHeight = res
                    .getDimensionPixelSize(R.dimen.screen_magnification_video_height);
            final int videoAssetMarginTop = res.getDimensionPixelSize(
                    R.dimen.screen_magnification_video_margin_top);
            view.setDividerAllowedAbove(false);
            view.setDividerAllowedBelow(false);
            mVideoBackgroundView = (ImageView) view.findViewById(R.id.video_background);
            final VideoView videoView = (VideoView) view.findViewById(R.id.video);

            // Loop the video.
            videoView.setOnPreparedListener(new OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.setLooping(true);
                }
            });

            // Make sure the VideoView does not request audio focus.
            videoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE);

            // Resolve and set the video content
            Bundle args = getArguments();
            if ((args != null) && args.containsKey(
                    AccessibilitySettings.EXTRA_VIDEO_RAW_RESOURCE_ID)) {
                videoView.setVideoURI(Uri.parse(String.format("%s://%s/%s",
                        ContentResolver.SCHEME_ANDROID_RESOURCE,
                        getPrefContext().getPackageName(),
                        args.getInt(AccessibilitySettings.EXTRA_VIDEO_RAW_RESOURCE_ID))));
            }

            // Make sure video controls (e.g. for pausing) are not displayed.
            videoView.setMediaController(null);

            // LayoutListener for adjusting the position of the VideoView on the background image.
            mLayoutListener = new OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    final int backgroundViewWidth = mVideoBackgroundView.getWidth();

                    LayoutParams videoLp = (LayoutParams) videoView.getLayoutParams();
                    videoLp.width = videoAssetWidth * backgroundViewWidth / backgroundAssetWidth;
                    videoLp.height = videoAssetHeight * backgroundViewWidth / backgroundAssetWidth;
                    videoLp.setMargins(0,
                            videoAssetMarginTop * backgroundViewWidth / backgroundAssetWidth, 0, 0);
                    videoView.setLayoutParams(videoLp);
                    videoView.invalidate();
                    videoView.start();
                }
            };

            mVideoBackgroundView.getViewTreeObserver().addOnGlobalLayoutListener(mLayoutListener);
        }

        @Override
        protected void onPrepareForRemoval() {
            mVideoBackgroundView.getViewTreeObserver()
                    .removeOnGlobalLayoutListener(mLayoutListener);
        }
    }

    protected VideoPreference mVideoPreference;
    protected Preference mConfigWarningPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVideoPreference = new VideoPreference(getPrefContext());
        mVideoPreference.setSelectable(false);
        mVideoPreference.setPersistent(false);
        mVideoPreference.setLayoutResource(R.layout.magnification_video_preference);

        final Preference settingsPreference = new Preference(getPrefContext());
        final String SettingsText = getString(R.string.settings_button);
        settingsPreference.setTitle(SettingsText);
        settingsPreference.setKey(SETTINGS_KEY);
        settingsPreference.setFragment(MagnificationSettingsFragment.class.getName());
        settingsPreference.setPersistent(false);

        mConfigWarningPreference = new Preference(getPrefContext());
        mConfigWarningPreference.setSelectable(false);
        mConfigWarningPreference.setPersistent(false);
        mConfigWarningPreference.setVisible(false);
        mConfigWarningPreference.setIcon(R.drawable.ic_warning_24dp);

        final PreferenceScreen preferenceScreen = getPreferenceManager().getPreferenceScreen();
        preferenceScreen.setOrderingAsAdded(false);
        mVideoPreference.setOrder(0);
        settingsPreference.setOrder(1);
        mConfigWarningPreference.setOrder(2);
        preferenceScreen.addPreference(mVideoPreference);
        preferenceScreen.addPreference(settingsPreference);
        preferenceScreen.addPreference(mConfigWarningPreference);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        initShortcutPreference(savedInstanceState);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_SHORTCUT_TYPE, mShortcutType);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();

        VideoView videoView = (VideoView) getView().findViewById(R.id.video);
        if (videoView != null) {
            videoView.start();
        }

        updateConfigurationWarningIfNeeded();
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DialogType.GESTURE_NAVIGATION_TUTORIAL:
                return AccessibilityGestureNavigationTutorial
                        .showGestureNavigationTutorialDialog(getActivity());
            case DialogType.ACCESSIBILITY_BUTTON_TUTORIAL:
                return AccessibilityGestureNavigationTutorial
                        .showAccessibilityButtonTutorialDialog(getActivity());
            case DialogType.EDIT_SHORTCUT:
                final CharSequence dialogTitle = getActivity().getString(
                        R.string.accessibility_shortcut_edit_dialog_title_magnification);
                final AlertDialog dialog =
                        AccessibilityEditDialogUtils.showMagnificationEditShortcutDialog(
                                getActivity(), dialogTitle, this::callOnAlertDialogCheckboxClicked);
                initializeDialogCheckBox(dialog);
                return dialog;
        }
        throw new IllegalArgumentException("Unsupported dialogId " + dialogId);
    }

    private void initializeDialogCheckBox(AlertDialog dialog) {
        final View dialogSoftwareView = dialog.findViewById(R.id.software_shortcut);
        mSoftwareTypeCheckBox = dialogSoftwareView.findViewById(R.id.checkbox);
        final View dialogHardwareView = dialog.findViewById(R.id.hardware_shortcut);
        mHardwareTypeCheckBox = dialogHardwareView.findViewById(R.id.checkbox);
        final View dialogTripleTapView = dialog.findViewById(R.id.triple_tap_shortcut);
        mTripleTapTypeCheckBox = dialogTripleTapView.findViewById(R.id.checkbox);
        final View advancedView = dialog.findViewById(R.id.advanced_shortcut);
        updateAlertDialogCheckState();
        updateAlertDialogEnableState();

        // Shows the triple tap checkbox directly if clicked.
        if (mTripleTapTypeCheckBox.isChecked()) {
            advancedView.setVisibility(View.GONE);
            dialogTripleTapView.setVisibility(View.VISIBLE);
        }
    }

    private void updateAlertDialogCheckState() {
        updateCheckStatus(mSoftwareTypeCheckBox, ShortcutType.SOFTWARE);
        updateCheckStatus(mHardwareTypeCheckBox, ShortcutType.HARDWARE);
        updateCheckStatus(mTripleTapTypeCheckBox, ShortcutType.TRIPLETAP);
    }

    private void updateAlertDialogEnableState() {
        if (!mSoftwareTypeCheckBox.isChecked() && !mTripleTapTypeCheckBox.isChecked()) {
            mHardwareTypeCheckBox.setEnabled(false);
        } else if (!mHardwareTypeCheckBox.isChecked() && !mTripleTapTypeCheckBox.isChecked()) {
            mSoftwareTypeCheckBox.setEnabled(false);
        } else if (!mSoftwareTypeCheckBox.isChecked() && !mHardwareTypeCheckBox.isChecked()) {
            mTripleTapTypeCheckBox.setEnabled(false);
        } else {
            mSoftwareTypeCheckBox.setEnabled(true);
            mHardwareTypeCheckBox.setEnabled(true);
            mTripleTapTypeCheckBox.setEnabled(true);
        }
    }

    private void updateCheckStatus(CheckBox checkBox, @ShortcutType int type) {
        checkBox.setChecked((mShortcutType & type) == type);
        checkBox.setOnClickListener(v -> {
            updateShortcutType(false);
            updateAlertDialogEnableState();
        });
    }

    private void updateShortcutType(boolean saveToDB) {
        mShortcutType = ShortcutType.DEFAULT;
        if (mSoftwareTypeCheckBox.isChecked()) {
            mShortcutType |= ShortcutType.SOFTWARE;
        }
        if (mHardwareTypeCheckBox.isChecked()) {
            mShortcutType |= ShortcutType.HARDWARE;
        }
        if (mTripleTapTypeCheckBox.isChecked()) {
            mShortcutType |= ShortcutType.TRIPLETAP;
        }
        if (saveToDB) {
            setShortcutType(mShortcutType);
        }
    }

    private void setSecureIntValue(String key, @ShortcutType int value) {
        Settings.Secure.putIntForUser(getPrefContext().getContentResolver(),
                key, value, getPrefContext().getContentResolver().getUserId());
    }

    private void setShortcutType(@ShortcutType int type) {
        setSecureIntValue(KEY_SHORTCUT_TYPE, type);
    }

    private String getShortcutTypeSummary(Context context) {
        final int shortcutType = getShortcutType(context);
        final CharSequence softwareTitle =
                context.getText(AccessibilityUtil.isGestureNavigateEnabled(context)
                        ? R.string.accessibility_shortcut_edit_dialog_title_software_gesture
                        : R.string.accessibility_shortcut_edit_dialog_title_software);

        List<CharSequence> list = new ArrayList<>();
        if ((shortcutType & ShortcutType.SOFTWARE) == ShortcutType.SOFTWARE) {
            list.add(softwareTitle);
        }
        if ((shortcutType & ShortcutType.HARDWARE) == ShortcutType.HARDWARE) {
            final CharSequence hardwareTitle = context.getText(
                    R.string.accessibility_shortcut_edit_dialog_title_hardware);
            list.add(hardwareTitle);
        }

        if ((shortcutType & ShortcutType.TRIPLETAP) == ShortcutType.TRIPLETAP) {
            final CharSequence tripleTapTitle = context.getText(
                    R.string.accessibility_shortcut_edit_dialog_title_triple_tap);
            list.add(tripleTapTitle);
        }

        // Show software shortcut if first time to use.
        if (list.isEmpty()) {
            list.add(softwareTitle);
        }
        final String joinStrings = TextUtils.join(/* delimiter= */", ", list);
        return AccessibilityUtil.capitalize(joinStrings);
    }

    @ShortcutType
    private int getShortcutType(Context context) {
        return getSecureIntValue(context, KEY_SHORTCUT_TYPE, ShortcutType.SOFTWARE);
    }

    @ShortcutType
    private int getSecureIntValue(Context context, String key, @ShortcutType int defaultValue) {
        return Settings.Secure.getIntForUser(
                context.getContentResolver(),
                key, defaultValue, context.getContentResolver().getUserId());
    }

    private void callOnAlertDialogCheckboxClicked(DialogInterface dialog, int which) {
        updateShortcutType(true);
        mShortcutPreference.setSummary(
                getShortcutTypeSummary(getPrefContext()));
    }

    @Override
    public int getMetricsCategory() {
        // TODO: Distinguish between magnification modes
        return SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DialogType.GESTURE_NAVIGATION_TUTORIAL:
                return SettingsEnums.DIALOG_TOGGLE_SCREEN_MAGNIFICATION_GESTURE_NAVIGATION;
            case DialogType.ACCESSIBILITY_BUTTON_TUTORIAL:
                return SettingsEnums.DIALOG_TOGGLE_SCREEN_MAGNIFICATION_ACCESSIBILITY_BUTTON;
            case DialogType.EDIT_SHORTCUT:
                return SettingsEnums.DIALOG_MAGNIFICATION_EDIT_SHORTCUT;
            default:
                return 0;
        }
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        onPreferenceToggled(mPreferenceKey, isChecked);
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        if (enabled && TextUtils.equals(
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED,
                preferenceKey)) {
            showDialog(AccessibilityUtil.isGestureNavigateEnabled(getContext())
                    ? DialogType.GESTURE_NAVIGATION_TUTORIAL
                    : DialogType.ACCESSIBILITY_BUTTON_TUTORIAL);
        }
        MagnificationPreferenceFragment.setChecked(getContentResolver(), preferenceKey, enabled);
        updateConfigurationWarningIfNeeded();
    }

    @Override
    protected void onInstallSwitchBarToggleSwitch() {
        super.onInstallSwitchBarToggleSwitch();

        mSwitchBar.setCheckedInternal(
                MagnificationPreferenceFragment.isChecked(getContentResolver(), mPreferenceKey));
        mSwitchBar.addOnSwitchChangeListener(this);
    }

    @Override
    protected void onRemoveSwitchBarToggleSwitch() {
        super.onRemoveSwitchBarToggleSwitch();
        mSwitchBar.removeOnSwitchChangeListener(this);
    }

    @Override
    protected void updateSwitchBarText(SwitchBar switchBar) {
        final String switchBarText = getString(R.string.accessibility_service_master_switch_title,
                getString(R.string.accessibility_screen_magnification_title));
        switchBar.setSwitchBarText(switchBarText, switchBarText);
    }

    @Override
    protected void onProcessArguments(Bundle arguments) {
        super.onProcessArguments(arguments);
        if (arguments == null) {
            return;
        }

        if (arguments.containsKey(AccessibilitySettings.EXTRA_VIDEO_RAW_RESOURCE_ID)) {
            mVideoPreference.setVisible(true);
        } else {
            mVideoPreference.setVisible(false);
        }

        if (arguments.containsKey(AccessibilitySettings.EXTRA_TITLE_RES)) {
            final int titleRes = arguments.getInt(AccessibilitySettings.EXTRA_TITLE_RES);
            if (titleRes > 0) {
                getActivity().setTitle(titleRes);
            }
        }
    }

    @Override
    public void onCheckboxClicked(ShortcutPreference preference) {
        if (preference.getChecked()) {
            // TODO(b/142530063): Enable shortcut when checkbox is checked.
        } else {
            // TODO(b/142530063): Disable shortcut when checkbox is unchecked.
        }
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        mShortcutType = getShortcutType(getPrefContext());
        showDialog(DialogType.EDIT_SHORTCUT);
    }

    private void initShortcutPreference(Bundle savedInstanceState) {
        // Restore the Shortcut type
        if (savedInstanceState != null) {
            mShortcutType = savedInstanceState.getInt(EXTRA_SHORTCUT_TYPE, ShortcutType.DEFAULT);
        }
        if (mShortcutType == ShortcutType.DEFAULT) {
            mShortcutType = getShortcutType(getPrefContext());
        }

        // Initial ShortcutPreference widget
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        mShortcutPreference = new ShortcutPreference(
                preferenceScreen.getContext(), null);
        mShortcutPreference.setTitle(R.string.accessibility_magnification_shortcut_title);
        mShortcutPreference.setOnClickListener(this);
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
        // Put the shortcutPreference before videoPreference.
        mShortcutPreference.setOrder(mVideoPreference.getOrder() - 1);
        preferenceScreen.addPreference(mShortcutPreference);
        // TODO(b/142530063): Check the new key to decide whether checkbox should be checked.
    }

    private void updateConfigurationWarningIfNeeded() {
        final CharSequence warningMessage =
                MagnificationPreferenceFragment.getConfigurationWarningStringForSecureSettingsKey(
                        mPreferenceKey, getPrefContext());
        if (warningMessage != null) {
            mConfigWarningPreference.setSummary(warningMessage);
        }
        mConfigWarningPreference.setVisible(warningMessage != null);
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface DialogType {
        int GESTURE_NAVIGATION_TUTORIAL = 1;
        int ACCESSIBILITY_BUTTON_TUTORIAL = 2;
        int EDIT_SHORTCUT = 3;
    }

}
