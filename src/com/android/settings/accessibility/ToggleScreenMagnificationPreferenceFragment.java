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

import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME;

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
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityUtil.UserShortcutType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class ToggleScreenMagnificationPreferenceFragment extends
        ToggleFeaturePreferenceFragment implements ShortcutPreference.OnClickListener {

    private static final String SETTINGS_KEY = "screen_magnification_settings";
    private static final String EXTRA_SHORTCUT_TYPE = "shortcut_type";
    private ShortcutPreference mShortcutPreference;
    private int mUserShortcutType = UserShortcutType.DEFAULT;
    private CheckBox mSoftwareTypeCheckBox;
    private CheckBox mHardwareTypeCheckBox;
    private CheckBox mTripleTapTypeCheckBox;
    private static final String KEY_SHORTCUT_PREFERENCE = "shortcut_preference";

    // TODO(b/147021230): Will move common functions and variables to
    //  android/internal/accessibility folder. For now, magnification need to be treated
    //  individually.
    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final TextUtils.SimpleStringSplitter sStringColonSplitter =
            new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);

    protected Preference mConfigWarningPreference;
    protected VideoPreference mVideoPreference;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.accessibility_screen_magnification_title);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mVideoPreference = new VideoPreference(getPrefContext());
        mVideoPreference.setSelectable(false);
        mVideoPreference.setPersistent(false);
        mVideoPreference.setLayoutResource(R.layout.magnification_video_preference);

        final PreferenceCategory optionCategory = new PreferenceCategory(getPrefContext());
        optionCategory.setTitle(R.string.accessibility_screen_option);

        initShortcutPreference(savedInstanceState);

        final Preference settingsPreference = new Preference(getPrefContext());
        settingsPreference.setTitle(R.string.accessibility_magnification_service_settings_title);
        settingsPreference.setKey(SETTINGS_KEY);
        settingsPreference.setFragment(MagnificationSettingsFragment.class.getName());
        settingsPreference.setPersistent(false);

        final PreferenceCategory aboutCategory = new PreferenceCategory(getPrefContext());
        aboutCategory.setTitle(R.string.accessibility_screen_magnification_about);

        mConfigWarningPreference = new Preference(getPrefContext());
        mConfigWarningPreference.setSelectable(false);
        mConfigWarningPreference.setPersistent(false);
        mConfigWarningPreference.setVisible(false);
        mConfigWarningPreference.setIcon(R.drawable.ic_warning_24dp);

        final PreferenceScreen preferenceScreen = getPreferenceManager().getPreferenceScreen();
        preferenceScreen.setOrderingAsAdded(false);
        mVideoPreference.setOrder(0);
        optionCategory.setOrder(1);
        aboutCategory.setOrder(2);
        preferenceScreen.addPreference(mVideoPreference);
        preferenceScreen.addPreference(optionCategory);
        optionCategory.addPreference(mShortcutPreference);
        optionCategory.addPreference(settingsPreference);
        preferenceScreen.addPreference(aboutCategory);
        aboutCategory.addPreference(mConfigWarningPreference);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(EXTRA_SHORTCUT_TYPE, mUserShortcutType);
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
        updateShortcutPreference();
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

        // Window magnification mode doesn't support advancedView.
        if (isWindowMagnification(getPrefContext())) {
            advancedView.setVisibility(View.GONE);
            return;
        }
        // Shows the triple tap checkbox directly if clicked.
        if (mTripleTapTypeCheckBox.isChecked()) {
            advancedView.setVisibility(View.GONE);
            dialogTripleTapView.setVisibility(View.VISIBLE);
        }
    }

    private void updateAlertDialogCheckState() {
        updateCheckStatus(mSoftwareTypeCheckBox, UserShortcutType.SOFTWARE);
        updateCheckStatus(mHardwareTypeCheckBox, UserShortcutType.HARDWARE);
        updateCheckStatus(mTripleTapTypeCheckBox, UserShortcutType.TRIPLETAP);
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

    private void updateCheckStatus(CheckBox checkBox, @UserShortcutType int type) {
        checkBox.setChecked((mUserShortcutType & type) == type);
        checkBox.setOnClickListener(v -> {
            updateUserShortcutType(/* saveChanges= */ false);
            updateAlertDialogEnableState();
        });
    }

    private void updateUserShortcutType(boolean saveChanges) {
        mUserShortcutType = UserShortcutType.DEFAULT;
        if (mSoftwareTypeCheckBox.isChecked()) {
            mUserShortcutType |= UserShortcutType.SOFTWARE;
        }
        if (mHardwareTypeCheckBox.isChecked()) {
            mUserShortcutType |= UserShortcutType.HARDWARE;
        }
        if (mTripleTapTypeCheckBox.isChecked()) {
            mUserShortcutType |= UserShortcutType.TRIPLETAP;
        }
        if (saveChanges) {
            setUserShortcutType(getPrefContext(), mUserShortcutType);
        }
    }

    private void setUserShortcutType(Context context, int type) {
        Set<String> info = SharedPreferenceUtils.getUserShortcutType(context);
        if (info.isEmpty()) {
            info = new HashSet<>();
        } else {
            final Set<String> filtered = info.stream().filter(
                    str -> str.contains(getComponentName())).collect(
                    Collectors.toSet());
            info.removeAll(filtered);
        }
        final AccessibilityUserShortcutType shortcut = new AccessibilityUserShortcutType(
                getComponentName(), type);
        info.add(shortcut.flattenToString());
        SharedPreferenceUtils.setUserShortcutType(context, info);
    }

    private String getShortcutTypeSummary(Context context) {
        final int shortcutType = getUserShortcutType(context, UserShortcutType.DEFAULT);
        final CharSequence softwareTitle =
                context.getText(AccessibilityUtil.isGestureNavigateEnabled(context)
                        ? R.string.accessibility_shortcut_edit_dialog_title_software_gesture
                        : R.string.accessibility_shortcut_edit_dialog_title_software);

        List<CharSequence> list = new ArrayList<>();
        if ((shortcutType & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE) {
            list.add(softwareTitle);
        }
        if ((shortcutType & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE) {
            final CharSequence hardwareTitle = context.getText(
                    R.string.accessibility_shortcut_edit_dialog_title_hardware);
            list.add(hardwareTitle);
        }

        if ((shortcutType & UserShortcutType.TRIPLETAP) == UserShortcutType.TRIPLETAP) {
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

    private int getUserShortcutType(Context context, @UserShortcutType int defaultValue) {
        final Set<String> info = SharedPreferenceUtils.getUserShortcutType(context);
        final Set<String> filtered = info.stream().filter(
                str -> str.contains(getComponentName())).collect(
                Collectors.toSet());
        if (filtered.isEmpty()) {
            return defaultValue;
        }

        final String str = (String) filtered.toArray()[0];
        final AccessibilityUserShortcutType shortcut = new AccessibilityUserShortcutType(str);
        return shortcut.getUserShortcutType();
    }

    private void callOnAlertDialogCheckboxClicked(DialogInterface dialog, int which) {
        updateUserShortcutType(/* saveChanges= */ true);
        mShortcutPreference.setSummary(
                getShortcutTypeSummary(getPrefContext()));
        if (mShortcutPreference.getChecked()) {
            optInAllMagnificationValuesToSettings(getContext(), mUserShortcutType);
            optOutAllMagnificationValuesFromSettings(getContext(), ~mUserShortcutType);
        }
    }

    private String getComponentName() {
        return MAGNIFICATION_CONTROLLER_NAME;
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

        // Magnify is temporary-use app which uses shortcut to magnify screen, not by toggle.
        mSwitchBar.hide();
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
    }

    @Override
    public void onCheckboxClicked(ShortcutPreference preference) {
        final int shortcutTypes = getUserShortcutType(getContext(), UserShortcutType.SOFTWARE);
        if (preference.getChecked()) {
            optInAllMagnificationValuesToSettings(getContext(), shortcutTypes);
        } else {
            optOutAllMagnificationValuesFromSettings(getContext(), shortcutTypes);
        }
    }

    @Override
    public void onSettingsClicked(ShortcutPreference preference) {
        mUserShortcutType = getUserShortcutType(getPrefContext(),
                UserShortcutType.SOFTWARE);
        showDialog(DialogType.EDIT_SHORTCUT);
    }

    private void initShortcutPreference(Bundle savedInstanceState) {
        mUserShortcutType = getUserShortcutType(getPrefContext(),
                UserShortcutType.SOFTWARE);

        // Restore the user shortcut type
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_SHORTCUT_TYPE)) {
            mUserShortcutType = savedInstanceState.getInt(EXTRA_SHORTCUT_TYPE,
                    UserShortcutType.SOFTWARE);
        }

        // Initial ShortcutPreference widget
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        mShortcutPreference = new ShortcutPreference(
                preferenceScreen.getContext(), null);
        mShortcutPreference.setPersistent(false);
        mShortcutPreference.setKey(getShortcutPreferenceKey());
        mShortcutPreference.setTitle(R.string.accessibility_magnification_shortcut_title);
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
        mShortcutPreference.setOnClickListener(this);
        // TODO(b/142530063): Check the new setting key to decide which summary should be shown.
    }

    private void updateShortcutPreference() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        final ShortcutPreference shortcutPreference = preferenceScreen.findPreference(
                getShortcutPreferenceKey());

        if (shortcutPreference != null) {
            final int shortcutTypes = getUserShortcutType(getContext(), UserShortcutType.SOFTWARE);
            shortcutPreference.setChecked(
                    hasMagnificationValuesInSettings(getContext(), shortcutTypes));
        }
    }

    private String getShortcutPreferenceKey() {
        return KEY_SHORTCUT_PREFERENCE;
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

    @VisibleForTesting
    static void optInAllMagnificationValuesToSettings(Context context, int shortcutTypes) {
        if ((shortcutTypes & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE) {
            optInMagnificationValueToSettings(context, UserShortcutType.SOFTWARE);
        }
        if (((shortcutTypes & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE)) {
            optInMagnificationValueToSettings(context, UserShortcutType.HARDWARE);
        }
        if (((shortcutTypes & UserShortcutType.TRIPLETAP) == UserShortcutType.TRIPLETAP)) {
            optInMagnificationValueToSettings(context, UserShortcutType.TRIPLETAP);
        }
    }

    private static void optInMagnificationValueToSettings(Context context,
            @UserShortcutType int shortcutType) {
        final String targetKey = AccessibilityUtil.convertKeyFromSettings(shortcutType);
        final String targetString = Settings.Secure.getString(context.getContentResolver(),
                targetKey);

        if (hasMagnificationValueInSettings(context, shortcutType)) {
            return;
        }

        final StringJoiner joiner = new StringJoiner(String.valueOf(COMPONENT_NAME_SEPARATOR));
        if (!TextUtils.isEmpty(targetString)) {
            joiner.add(targetString);
        }
        joiner.add(MAGNIFICATION_CONTROLLER_NAME);

        Settings.Secure.putString(context.getContentResolver(), targetKey, joiner.toString());
    }

    @VisibleForTesting
    static void optOutAllMagnificationValuesFromSettings(Context context,
            int shortcutTypes) {
        if ((shortcutTypes & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE) {
            optOutMagnificationValueFromSettings(context, UserShortcutType.SOFTWARE);
        }
        if (((shortcutTypes & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE)) {
            optOutMagnificationValueFromSettings(context, UserShortcutType.HARDWARE);
        }
        if (((shortcutTypes & UserShortcutType.TRIPLETAP) == UserShortcutType.TRIPLETAP)) {
            optOutMagnificationValueFromSettings(context, UserShortcutType.TRIPLETAP);
        }
    }

    private static void optOutMagnificationValueFromSettings(Context context,
            @UserShortcutType int shortcutType) {
        final StringJoiner joiner = new StringJoiner(String.valueOf(COMPONENT_NAME_SEPARATOR));
        final String targetKey = AccessibilityUtil.convertKeyFromSettings(shortcutType);
        final String targetString = Settings.Secure.getString(context.getContentResolver(),
                targetKey);

        if (TextUtils.isEmpty(targetString)) {
            return;
        }

        sStringColonSplitter.setString(targetString);
        while (sStringColonSplitter.hasNext()) {
            final String name = sStringColonSplitter.next();
            if (TextUtils.isEmpty(name) || MAGNIFICATION_CONTROLLER_NAME.equals(name)) {
                continue;
            }
            joiner.add(name);
        }

        Settings.Secure.putString(context.getContentResolver(), targetKey, joiner.toString());
    }

    @VisibleForTesting
    static boolean hasMagnificationValuesInSettings(Context context, int shortcutTypes) {
        boolean exist = false;

        if ((shortcutTypes & UserShortcutType.SOFTWARE) == UserShortcutType.SOFTWARE) {
            exist = hasMagnificationValueInSettings(context, UserShortcutType.SOFTWARE);
        }
        if (((shortcutTypes & UserShortcutType.HARDWARE) == UserShortcutType.HARDWARE)) {
            exist |= hasMagnificationValueInSettings(context, UserShortcutType.HARDWARE);
        }
        if (((shortcutTypes & UserShortcutType.TRIPLETAP) == UserShortcutType.TRIPLETAP)) {
            exist |= hasMagnificationValueInSettings(context, UserShortcutType.TRIPLETAP);
        }
        return exist;
    }

    private static boolean hasMagnificationValueInSettings(Context context,
            @UserShortcutType int shortcutType) {
        final String targetKey = AccessibilityUtil.convertKeyFromSettings(shortcutType);
        final String targetString = Settings.Secure.getString(context.getContentResolver(),
                targetKey);

        if (TextUtils.isEmpty(targetString)) {
            return false;
        }

        sStringColonSplitter.setString(targetString);
        while (sStringColonSplitter.hasNext()) {
            final String name = sStringColonSplitter.next();
            if (MAGNIFICATION_CONTROLLER_NAME.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean isWindowMagnification(Context context) {
        final int mode = Settings.Secure.getIntForUser(
                context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_FULLSCREEN,
                context.getContentResolver().getUserId());
        return mode == Settings.Secure.ACCESSIBILITY_MAGNIFICATION_MODE_WINDOW;
    }
}
