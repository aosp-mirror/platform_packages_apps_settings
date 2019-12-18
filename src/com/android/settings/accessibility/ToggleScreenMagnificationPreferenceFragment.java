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

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Switch;
import android.widget.VideoView;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.widget.SwitchBar;

public class ToggleScreenMagnificationPreferenceFragment extends
        ToggleFeaturePreferenceFragment implements SwitchBar.OnSwitchChangeListener {

    private static final int DIALOG_ID_GESTURE_NAVIGATION_TUTORIAL = 1;
    private static final int DIALOG_ID_ACCESSIBILITY_BUTTON_TUTORIAL = 2;
    private static final int DIALOG_ID_EDIT_SHORTCUT = 3;

    private final DialogInterface.OnClickListener mDialogListener =
            (DialogInterface dialog, int id) -> {
                if (id == DialogInterface.BUTTON_POSITIVE) {
                    // TODO(b/142531156): Save the shortcut type preference.
                }
            };

    private final View.OnClickListener mSettingButtonListener =
            (View view) -> showDialog(DIALOG_ID_EDIT_SHORTCUT);

    private final View.OnClickListener mCheckBoxListener = (View view) -> {
        CheckBox checkBox = (CheckBox) view;
        if (checkBox.isChecked()) {
            // TODO(b/142530063): Enable shortcut when checkbox is checked.
        } else {
            // TODO(b/142530063): Disable shortcut when checkbox is unchecked.
        }
    };

    private Dialog mDialog;

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

    private boolean mLaunchFromSuw = false;
    private boolean mInitialSetting = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVideoPreference = new VideoPreference(getPrefContext());
        mVideoPreference.setSelectable(false);
        mVideoPreference.setPersistent(false);
        mVideoPreference.setLayoutResource(R.layout.magnification_video_preference);

        mConfigWarningPreference = new Preference(getPrefContext());
        mConfigWarningPreference.setSelectable(false);
        mConfigWarningPreference.setPersistent(false);
        mConfigWarningPreference.setVisible(false);
        mConfigWarningPreference.setIcon(R.drawable.ic_warning_24dp);

        final PreferenceScreen preferenceScreen = getPreferenceManager().getPreferenceScreen();
        preferenceScreen.setOrderingAsAdded(false);
        mVideoPreference.setOrder(0);
        mConfigWarningPreference.setOrder(2);
        preferenceScreen.addPreference(mVideoPreference);
        preferenceScreen.addPreference(mConfigWarningPreference);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        initShortcutPreference();
        return super.onCreateView(inflater, container, savedInstanceState);
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
            case DIALOG_ID_GESTURE_NAVIGATION_TUTORIAL:
                mDialog = AccessibilityGestureNavigationTutorial
                        .showGestureNavigationTutorialDialog(getActivity());
                break;
            case DIALOG_ID_ACCESSIBILITY_BUTTON_TUTORIAL:
                mDialog = AccessibilityGestureNavigationTutorial
                        .showAccessibilityButtonTutorialDialog(getActivity());
                break;
            case DIALOG_ID_EDIT_SHORTCUT:
                final CharSequence dialogTitle = getActivity().getString(
                        R.string.accessibility_shortcut_edit_dialog_title_magnification);
                mDialog = AccessibilityEditDialogUtils.showMagnificationEditShortcutDialog(
                        getActivity(), dialogTitle, mDialogListener);
                break;
            default:
                throw new IllegalArgumentException();
        }

        return mDialog;
    }

    @Override
    public int getMetricsCategory() {
        // TODO: Distinguish between magnification modes
        return SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DIALOG_ID_GESTURE_NAVIGATION_TUTORIAL:
                return SettingsEnums.DIALOG_TOGGLE_SCREEN_MAGNIFICATION_GESTURE_NAVIGATION;
            case DIALOG_ID_ACCESSIBILITY_BUTTON_TUTORIAL:
                return SettingsEnums.DIALOG_TOGGLE_SCREEN_MAGNIFICATION_ACCESSIBILITY_BUTTON;
            case DIALOG_ID_EDIT_SHORTCUT:
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
            showDialog(isGestureNavigateEnabled() ? DIALOG_ID_GESTURE_NAVIGATION_TUTORIAL
                    : DIALOG_ID_ACCESSIBILITY_BUTTON_TUTORIAL);
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
            final int resId = arguments.getInt(
                    AccessibilitySettings.EXTRA_VIDEO_RAW_RESOURCE_ID);
        } else {
            mVideoPreference.setVisible(false);
        }

        if (arguments.containsKey(AccessibilitySettings.EXTRA_LAUNCHED_FROM_SUW)) {
            mLaunchFromSuw = arguments.getBoolean(AccessibilitySettings.EXTRA_LAUNCHED_FROM_SUW);
        }

        if (arguments.containsKey(AccessibilitySettings.EXTRA_CHECKED)) {
            mInitialSetting = arguments.getBoolean(AccessibilitySettings.EXTRA_CHECKED);
        }

        if (arguments.containsKey(AccessibilitySettings.EXTRA_TITLE_RES)) {
            final int titleRes = arguments.getInt(AccessibilitySettings.EXTRA_TITLE_RES);
            if (titleRes > 0) {
                getActivity().setTitle(titleRes);
            }
        }
    }

    private void initShortcutPreference() {
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        final ShortcutPreference shortcutPreference = new ShortcutPreference(
                preferenceScreen.getContext(), null);
        // Put the shortcutPreference before videoPreference.
        shortcutPreference.setOrder(mVideoPreference.getOrder() - 1);
        shortcutPreference.setTitle(R.string.accessibility_magnification_shortcut_title);
        // TODO(b/142530063): Check the new setting key to decide which summary should be shown.
        // TODO(b/142530063): Check if gesture mode is on to decide which summary should be shown.
        // TODO(b/142530063): Check the new key to decide whether checkbox should be checked.
        shortcutPreference.setSettingButtonListener(mSettingButtonListener);
        shortcutPreference.setCheckBoxListener(mCheckBoxListener);
        preferenceScreen.addPreference(shortcutPreference);
    }

    private boolean isGestureNavigateEnabled() {
        return getContext().getResources().getInteger(
                com.android.internal.R.integer.config_navBarInteractionMode)
                == NAV_BAR_MODE_GESTURAL;
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

    private static int getScreenWidth(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }
}
