/*
 * Copyright (C) 2009 The Android Open Source Project
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

import static android.os.Vibrator.VibrationIntensity;

import static com.android.settingslib.TwoTargetPreference.ICON_SIZE_MEDIUM;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.hardware.display.ColorDisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.DeviceConfig;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.internal.content.PackageMonitor;
import com.android.internal.view.RotationPolicy;
import com.android.internal.view.RotationPolicy.RotationPolicyListener;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.display.DarkUIPreferenceController;
import com.android.settings.display.ToggleFontSizePreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.accessibility.AccessibilityUtils;
import com.android.settingslib.search.SearchIndexable;

import com.google.common.primitives.Ints;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Activity with the accessibility settings.
 */
@SearchIndexable
public class AccessibilitySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    // Index of the first preference in a preference category.
    private static final int FIRST_PREFERENCE_IN_CATEGORY_INDEX = -1;

    // Preference categories
    private static final String CATEGORY_SCREEN_READER = "screen_reader_category";
    private static final String CATEGORY_AUDIO_AND_CAPTIONS = "audio_and_captions_category";
    private static final String CATEGORY_DISPLAY = "display_category";
    private static final String CATEGORY_INTERACTION_CONTROL = "interaction_control_category";
    private static final String CATEGORY_EXPERIMENTAL = "experimental_category";
    private static final String CATEGORY_DOWNLOADED_SERVICES = "user_installed_services_category";

    private static final String[] CATEGORIES = new String[] {
            CATEGORY_SCREEN_READER, CATEGORY_AUDIO_AND_CAPTIONS, CATEGORY_DISPLAY,
            CATEGORY_INTERACTION_CONTROL, CATEGORY_EXPERIMENTAL, CATEGORY_DOWNLOADED_SERVICES
    };

    // Preferences
    private static final String TOGGLE_HIGH_TEXT_CONTRAST_PREFERENCE =
            "toggle_high_text_contrast_preference";
    private static final String TOGGLE_INVERSION_PREFERENCE =
            "toggle_inversion_preference";
    private static final String TOGGLE_POWER_BUTTON_ENDS_CALL_PREFERENCE =
            "toggle_power_button_ends_call_preference";
    private static final String TOGGLE_LOCK_SCREEN_ROTATION_PREFERENCE =
            "toggle_lock_screen_rotation_preference";
    private static final String TOGGLE_LARGE_POINTER_ICON =
            "toggle_large_pointer_icon";
    private static final String TOGGLE_DISABLE_ANIMATIONS = "toggle_disable_animations";
    private static final String TOGGLE_MASTER_MONO =
            "toggle_master_mono";
    private static final String SELECT_LONG_PRESS_TIMEOUT_PREFERENCE =
            "select_long_press_timeout_preference";
    private static final String ACCESSIBILITY_SHORTCUT_PREFERENCE =
            "accessibility_shortcut_preference";
    private static final String HEARING_AID_PREFERENCE =
            "hearing_aid_preference";
    private static final String CAPTIONING_PREFERENCE_SCREEN =
            "captioning_preference_screen";
    private static final String DISPLAY_MAGNIFICATION_PREFERENCE_SCREEN =
            "magnification_preference_screen";
    private static final String FONT_SIZE_PREFERENCE_SCREEN =
            "font_size_preference_screen";
    private static final String AUTOCLICK_PREFERENCE_SCREEN =
            "autoclick_preference";
    private static final String VIBRATION_PREFERENCE_SCREEN =
            "vibration_preference_screen";
    private static final String DISPLAY_DALTONIZER_PREFERENCE_SCREEN =
            "daltonizer_preference";
    private static final String ACCESSIBILITY_CONTROL_TIMEOUT_PREFERENCE =
            "accessibility_control_timeout_preference_fragment";
    private static final String DARK_UI_MODE_PREFERENCE =
            "dark_ui_mode_accessibility";
    private static final String LIVE_CAPTION_PREFERENCE_KEY =
            "live_caption";


    // Extras passed to sub-fragments.
    static final String EXTRA_PREFERENCE_KEY = "preference_key";
    static final String EXTRA_CHECKED = "checked";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_TITLE_RES = "title_res";
    static final String EXTRA_RESOLVE_INFO = "resolve_info";
    static final String EXTRA_SUMMARY = "summary";
    static final String EXTRA_SUMMARY_RES = "summary_res";
    static final String EXTRA_SETTINGS_TITLE = "settings_title";
    static final String EXTRA_COMPONENT_NAME = "component_name";
    static final String EXTRA_SETTINGS_COMPONENT_NAME = "settings_component_name";
    static final String EXTRA_VIDEO_RAW_RESOURCE_ID = "video_resource";
    static final String EXTRA_LAUNCHED_FROM_SUW = "from_suw";

    // Timeout before we update the services if packages are added/removed
    // since the AccessibilityManagerService has to do that processing first
    // to generate the AccessibilityServiceInfo we need for proper
    // presentation.
    private static final long DELAY_UPDATE_SERVICES_MILLIS = 1000;

    // Settings that should be changed when toggling animations
    private static final String[] TOGGLE_ANIMATION_TARGETS = {
            Settings.Global.WINDOW_ANIMATION_SCALE, Settings.Global.TRANSITION_ANIMATION_SCALE,
            Settings.Global.ANIMATOR_DURATION_SCALE
    };
    private static final String ANIMATION_ON_VALUE = "1";
    private static final String ANIMATION_OFF_VALUE = "0";

    static final String RAMPING_RINGER_ENABLED = "ramping_ringer_enabled";

    private final Map<String, String> mLongPressTimeoutValueToTitleMap = new HashMap<>();

    private final Handler mHandler = new Handler();

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (getActivity() != null) {
                updateServicePreferences();
            }
        }
    };

    private final PackageMonitor mSettingsPackageMonitor = new PackageMonitor() {
        @Override
        public void onPackageAdded(String packageName, int uid) {
            sendUpdate();
        }

        @Override
        public void onPackageAppeared(String packageName, int reason) {
            sendUpdate();
        }

        @Override
        public void onPackageDisappeared(String packageName, int reason) {
            sendUpdate();
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            sendUpdate();
        }

        private void sendUpdate() {
            mHandler.postDelayed(mUpdateRunnable, DELAY_UPDATE_SERVICES_MILLIS);
        }
    };

    private final SettingsContentObserver mSettingsContentObserver;

    private final RotationPolicyListener mRotationPolicyListener = new RotationPolicyListener() {
        @Override
        public void onChange() {
            updateLockScreenRotationCheckbox();
        }
    };

    private final Map<String, PreferenceCategory> mCategoryToPrefCategoryMap =
            new ArrayMap<>();
    private final Map<Preference, PreferenceCategory> mServicePreferenceToPreferenceCategoryMap =
            new ArrayMap<>();
    private final Map<ComponentName, PreferenceCategory> mPreBundledServiceComponentToCategoryMap =
            new ArrayMap<>();

    private SwitchPreference mToggleHighTextContrastPreference;
    private SwitchPreference mTogglePowerButtonEndsCallPreference;
    private SwitchPreference mToggleLockScreenRotationPreference;
    private SwitchPreference mToggleLargePointerIconPreference;
    private SwitchPreference mToggleDisableAnimationsPreference;
    private SwitchPreference mToggleMasterMonoPreference;
    private ListPreference mSelectLongPressTimeoutPreference;
    private Preference mCaptioningPreferenceScreen;
    private Preference mDisplayMagnificationPreferenceScreen;
    private Preference mFontSizePreferenceScreen;
    private Preference mAutoclickPreferenceScreen;
    private Preference mAccessibilityShortcutPreferenceScreen;
    private Preference mDisplayDaltonizerPreferenceScreen;
    private Preference mHearingAidPreference;
    private Preference mVibrationPreferenceScreen;
    private Preference mLiveCaptionPreference;
    private SwitchPreference mToggleInversionPreference;
    private ColorInversionPreferenceController mInversionPreferenceController;
    private AccessibilityHearingAidPreferenceController mHearingAidPreferenceController;
    private SwitchPreference mDarkUIModePreference;
    private DarkUIPreferenceController mDarkUIPreferenceController;
    private LiveCaptionPreferenceController mLiveCaptionPreferenceController;

    private int mLongPressTimeoutDefault;

    private DevicePolicyManager mDpm;

    /**
     * Check if the color transforms are color accelerated. Some transforms are experimental only
     * on non-accelerated platforms due to the performance implications.
     *
     * @param context The current context
     */
    public static boolean isColorTransformAccelerated(Context context) {
        return context.getResources()
                .getBoolean(com.android.internal.R.bool.config_setColorTransformAccelerated);
    }

    public AccessibilitySettings() {
        // Observe changes to anything that the shortcut can toggle, so we can reflect updates
        final Collection<AccessibilityShortcutController.ToggleableFrameworkFeatureInfo> features =
                AccessibilityShortcutController.getFrameworkShortcutFeaturesMap().values();
        final List<String> shortcutFeatureKeys = new ArrayList<>(features.size());
        for (AccessibilityShortcutController.ToggleableFrameworkFeatureInfo feature : features) {
            shortcutFeatureKeys.add(feature.getSettingKey());
        }
        mSettingsContentObserver = new SettingsContentObserver(mHandler, shortcutFeatureKeys) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                updateAllPreferences();
            }
        };
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_accessibility;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.accessibility_settings);
        initializeAllPreferences();
        mDpm = (DevicePolicyManager) (getActivity()
                .getSystemService(Context.DEVICE_POLICY_SERVICE));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mHearingAidPreferenceController = new AccessibilityHearingAidPreferenceController
                (context, HEARING_AID_PREFERENCE);
        mHearingAidPreferenceController.setFragmentManager(getFragmentManager());
        getLifecycle().addObserver(mHearingAidPreferenceController);

        mLiveCaptionPreferenceController = new LiveCaptionPreferenceController(context,
                LIVE_CAPTION_PREFERENCE_KEY);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAllPreferences();

        mSettingsPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
        mSettingsContentObserver.register(getContentResolver());
        if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.registerRotationPolicyListener(getActivity(),
                    mRotationPolicyListener);
        }
    }

    @Override
    public void onPause() {
        mSettingsPackageMonitor.unregister();
        mSettingsContentObserver.unregister(getContentResolver());
        if (RotationPolicy.isRotationSupported(getActivity())) {
            RotationPolicy.unregisterRotationPolicyListener(getActivity(),
                    mRotationPolicyListener);
        }
        super.onPause();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (mSelectLongPressTimeoutPreference == preference) {
            handleLongPressTimeoutPreferenceChange((String) newValue);
            return true;
        }
        return false;
    }

    private void handleLongPressTimeoutPreferenceChange(String stringValue) {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.LONG_PRESS_TIMEOUT, Integer.parseInt(stringValue));
        mSelectLongPressTimeoutPreference.setSummary(
                mLongPressTimeoutValueToTitleMap.get(stringValue));
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (mToggleHighTextContrastPreference == preference) {
            handleToggleTextContrastPreferenceClick();
            return true;
        } else if (mTogglePowerButtonEndsCallPreference == preference) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        } else if (mToggleLockScreenRotationPreference == preference) {
            handleLockScreenRotationPreferenceClick();
            return true;
        } else if (mToggleLargePointerIconPreference == preference) {
            handleToggleLargePointerIconPreferenceClick();
            return true;
        } else if (mToggleDisableAnimationsPreference == preference) {
            handleToggleDisableAnimations();
            return true;
        } else if (mToggleMasterMonoPreference == preference) {
            handleToggleMasterMonoPreferenceClick();
            return true;
        } else if (mHearingAidPreferenceController.handlePreferenceTreeClick(preference)) {
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    public static CharSequence getServiceSummary(Context context, AccessibilityServiceInfo info,
            boolean serviceEnabled) {
        final String serviceState = serviceEnabled
                ? context.getString(R.string.accessibility_summary_state_enabled)
                : context.getString(R.string.accessibility_summary_state_disabled);
        final CharSequence serviceSummary = info.loadSummary(context.getPackageManager());
        final String stateSummaryCombo = context.getString(
                R.string.preference_summary_default_combination,
                serviceState, serviceSummary);

        return (TextUtils.isEmpty(serviceSummary))
                ? serviceState
                : stateSummaryCombo;
    }

    @VisibleForTesting
    static boolean isRampingRingerEnabled(final Context context) {
        return (Settings.Global.getInt(
                        context.getContentResolver(),
                        Settings.Global.APPLY_RAMPING_RINGER, 0) == 1)
                && DeviceConfig.getBoolean(
                        DeviceConfig.NAMESPACE_TELEPHONY, RAMPING_RINGER_ENABLED, false);
    }

    private void handleToggleTextContrastPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED,
                (mToggleHighTextContrastPreference.isChecked() ? 1 : 0));
    }

    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                (mTogglePowerButtonEndsCallPreference.isChecked()
                        ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                        : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
    }

    private void handleLockScreenRotationPreferenceClick() {
        RotationPolicy.setRotationLockForAccessibility(getActivity(),
                !mToggleLockScreenRotationPreference.isChecked());
    }

    private void handleToggleLargePointerIconPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_LARGE_POINTER_ICON,
                mToggleLargePointerIconPreference.isChecked() ? 1 : 0);
    }

    private void handleToggleDisableAnimations() {
        String newAnimationValue = mToggleDisableAnimationsPreference.isChecked()
                ? ANIMATION_OFF_VALUE : ANIMATION_ON_VALUE;
        for (String animationPreference : TOGGLE_ANIMATION_TARGETS) {
            Settings.Global.putString(getContentResolver(), animationPreference, newAnimationValue);
        }
    }

    private void handleToggleMasterMonoPreferenceClick() {
        Settings.System.putIntForUser(getContentResolver(), Settings.System.MASTER_MONO,
                mToggleMasterMonoPreference.isChecked() ? 1 : 0, UserHandle.USER_CURRENT);
    }

    private void initializeAllPreferences() {
        for (int i = 0; i < CATEGORIES.length; i++) {
            PreferenceCategory prefCategory = (PreferenceCategory) findPreference(CATEGORIES[i]);
            mCategoryToPrefCategoryMap.put(CATEGORIES[i], prefCategory);
        }

        // Text contrast.
        mToggleHighTextContrastPreference =
                (SwitchPreference) findPreference(TOGGLE_HIGH_TEXT_CONTRAST_PREFERENCE);

        // Display inversion.
        mToggleInversionPreference = (SwitchPreference) findPreference(TOGGLE_INVERSION_PREFERENCE);
        mInversionPreferenceController =
                new ColorInversionPreferenceController(getContext(), TOGGLE_INVERSION_PREFERENCE);
        mInversionPreferenceController.displayPreference(getPreferenceScreen());

        // Power button ends calls.
        mTogglePowerButtonEndsCallPreference =
                (SwitchPreference) findPreference(TOGGLE_POWER_BUTTON_ENDS_CALL_PREFERENCE);
        if (!KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
                || !Utils.isVoiceCapable(getActivity())) {
            mCategoryToPrefCategoryMap.get(CATEGORY_INTERACTION_CONTROL)
                    .removePreference(mTogglePowerButtonEndsCallPreference);
        }

        // Lock screen rotation.
        mToggleLockScreenRotationPreference =
                (SwitchPreference) findPreference(TOGGLE_LOCK_SCREEN_ROTATION_PREFERENCE);
        if (!RotationPolicy.isRotationSupported(getActivity())) {
            mCategoryToPrefCategoryMap.get(CATEGORY_INTERACTION_CONTROL)
                    .removePreference(mToggleLockScreenRotationPreference);
        }

        // Large pointer icon.
        mToggleLargePointerIconPreference =
                (SwitchPreference) findPreference(TOGGLE_LARGE_POINTER_ICON);

        mToggleDisableAnimationsPreference =
                (SwitchPreference) findPreference(TOGGLE_DISABLE_ANIMATIONS);

        // Master Mono
        mToggleMasterMonoPreference =
                (SwitchPreference) findPreference(TOGGLE_MASTER_MONO);

        // Long press timeout.
        mSelectLongPressTimeoutPreference =
                (ListPreference) findPreference(SELECT_LONG_PRESS_TIMEOUT_PREFERENCE);
        mSelectLongPressTimeoutPreference.setOnPreferenceChangeListener(this);
        if (mLongPressTimeoutValueToTitleMap.size() == 0) {
            String[] timeoutValues = getResources().getStringArray(
                    R.array.long_press_timeout_selector_values);
            mLongPressTimeoutDefault = Integer.parseInt(timeoutValues[0]);
            String[] timeoutTitles = getResources().getStringArray(
                    R.array.long_press_timeout_selector_titles);
            final int timeoutValueCount = timeoutValues.length;
            for (int i = 0; i < timeoutValueCount; i++) {
                mLongPressTimeoutValueToTitleMap.put(timeoutValues[i], timeoutTitles[i]);
            }
        }

        // Hearing Aid.
        mHearingAidPreference = findPreference(HEARING_AID_PREFERENCE);
        mHearingAidPreferenceController.displayPreference(getPreferenceScreen());

        // Captioning.
        mCaptioningPreferenceScreen = findPreference(CAPTIONING_PREFERENCE_SCREEN);

        // Live caption
        mLiveCaptionPreference = findPreference(LIVE_CAPTION_PREFERENCE_KEY);
        mLiveCaptionPreferenceController.displayPreference(getPreferenceScreen());

        // Display magnification.
        mDisplayMagnificationPreferenceScreen = findPreference(
                DISPLAY_MAGNIFICATION_PREFERENCE_SCREEN);
        configureMagnificationPreferenceIfNeeded(mDisplayMagnificationPreferenceScreen);

        // Font size.
        mFontSizePreferenceScreen = findPreference(FONT_SIZE_PREFERENCE_SCREEN);

        // Autoclick after pointer stops.
        mAutoclickPreferenceScreen = findPreference(AUTOCLICK_PREFERENCE_SCREEN);

        // Display color adjustments.
        mDisplayDaltonizerPreferenceScreen = findPreference(DISPLAY_DALTONIZER_PREFERENCE_SCREEN);

        // Accessibility shortcut.
        mAccessibilityShortcutPreferenceScreen = findPreference(ACCESSIBILITY_SHORTCUT_PREFERENCE);

        // Vibrations.
        mVibrationPreferenceScreen = findPreference(VIBRATION_PREFERENCE_SCREEN);

        // Dark Mode.
        mDarkUIModePreference = findPreference(DARK_UI_MODE_PREFERENCE);
        mDarkUIPreferenceController = new DarkUIPreferenceController(getContext(),
                DARK_UI_MODE_PREFERENCE);
        mDarkUIPreferenceController.setParentFragment(this);
        mDarkUIPreferenceController.displayPreference(getPreferenceScreen());
    }

    private void updateAllPreferences() {
        updateSystemPreferences();
        updateServicePreferences();
    }

    protected void updateServicePreferences() {
        // Since services category is auto generated we have to do a pass
        // to generate it since services can come and go and then based on
        // the global accessibility state to decided whether it is enabled.

        // Generate.
        ArrayList<Preference> servicePreferences =
                new ArrayList<>(mServicePreferenceToPreferenceCategoryMap.keySet());
        for (int i = 0; i < servicePreferences.size(); i++) {
            Preference service = servicePreferences.get(i);
            PreferenceCategory category = mServicePreferenceToPreferenceCategoryMap.get(service);
            category.removePreference(service);
        }

        initializePreBundledServicesMapFromArray(CATEGORY_SCREEN_READER,
                R.array.config_preinstalled_screen_reader_services);
        initializePreBundledServicesMapFromArray(CATEGORY_AUDIO_AND_CAPTIONS,
                R.array.config_preinstalled_audio_and_caption_services);
        initializePreBundledServicesMapFromArray(CATEGORY_DISPLAY,
                R.array.config_preinstalled_display_services);
        initializePreBundledServicesMapFromArray(CATEGORY_INTERACTION_CONTROL,
                R.array.config_preinstalled_interaction_control_services);

        AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(getActivity());

        List<AccessibilityServiceInfo> installedServices =
                accessibilityManager.getInstalledAccessibilityServiceList();
        List<AccessibilityServiceInfo> enabledServiceInfos = accessibilityManager
                .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        Set<ComponentName> enabledServices = AccessibilityUtils.getEnabledServicesFromSettings(
                getActivity());
        List<String> permittedServices = mDpm.getPermittedAccessibilityServices(
                UserHandle.myUserId());

        PreferenceCategory downloadedServicesCategory =
                mCategoryToPrefCategoryMap.get(CATEGORY_DOWNLOADED_SERVICES);
        // Temporarily add the downloaded services category back if it was previously removed.
        if (findPreference(CATEGORY_DOWNLOADED_SERVICES) == null) {
            getPreferenceScreen().addPreference(downloadedServicesCategory);
        }

        for (int i = 0, count = installedServices.size(); i < count; ++i) {
            final AccessibilityServiceInfo info = installedServices.get(i);
            final ResolveInfo resolveInfo = info.getResolveInfo();

            final RestrictedPreference preference =
                    new RestrictedPreference(downloadedServicesCategory.getContext());
            final String title = resolveInfo.loadLabel(getPackageManager()).toString();

            Drawable icon;
            if (resolveInfo.getIconResource() == 0) {
                icon = ContextCompat.getDrawable(getContext(), R.drawable.ic_accessibility_generic);
            } else {
                icon = resolveInfo.loadIcon(getPackageManager());
            }

            final ServiceInfo serviceInfo = resolveInfo.serviceInfo;
            final String packageName = serviceInfo.packageName;
            final ComponentName componentName = new ComponentName(packageName, serviceInfo.name);

            preference.setKey(componentName.flattenToString());

            preference.setTitle(title);
            preference.setIconSize(ICON_SIZE_MEDIUM);
            Utils.setSafeIcon(preference, icon);
            final boolean serviceEnabled = enabledServices.contains(componentName);
            String description = info.loadDescription(getPackageManager());
            if (TextUtils.isEmpty(description)) {
                description = getString(R.string.accessibility_service_default_description);
            }

            if (serviceEnabled && AccessibilityUtils.hasServiceCrashed(
                    packageName, serviceInfo.name, enabledServiceInfos)) {
                // Update the summaries for services that have crashed.
                preference.setSummary(R.string.accessibility_summary_state_stopped);
                description = getString(R.string.accessibility_description_state_stopped);
            } else {
                final CharSequence serviceSummary = getServiceSummary(getContext(), info,
                        serviceEnabled);
                preference.setSummary(serviceSummary);
            }

            // Disable all accessibility services that are not permitted.
            final boolean serviceAllowed =
                    permittedServices == null || permittedServices.contains(packageName);
            if (!serviceAllowed && !serviceEnabled) {
                final EnforcedAdmin admin =
                        RestrictedLockUtilsInternal.checkIfAccessibilityServiceDisallowed(
                                getActivity(), packageName, UserHandle.myUserId());
                if (admin != null) {
                    preference.setDisabledByAdmin(admin);
                } else {
                    preference.setEnabled(false);
                }
            } else {
                preference.setEnabled(true);
            }

            preference.setFragment(ToggleAccessibilityServicePreferenceFragment.class.getName());
            preference.setPersistent(true);

            final Bundle extras = preference.getExtras();
            extras.putString(EXTRA_PREFERENCE_KEY, preference.getKey());
            extras.putBoolean(EXTRA_CHECKED, serviceEnabled);
            extras.putString(EXTRA_TITLE, title);
            extras.putParcelable(EXTRA_RESOLVE_INFO, resolveInfo);
            extras.putString(EXTRA_SUMMARY, description);

            final String settingsClassName = info.getSettingsActivityName();
            if (!TextUtils.isEmpty(settingsClassName)) {
                extras.putString(EXTRA_SETTINGS_TITLE,
                        getString(R.string.accessibility_menu_item_settings));
                extras.putString(EXTRA_SETTINGS_COMPONENT_NAME,
                        new ComponentName(packageName, settingsClassName).flattenToString());
            }
            extras.putParcelable(EXTRA_COMPONENT_NAME, componentName);

            PreferenceCategory prefCategory = downloadedServicesCategory;
            // Set the appropriate category if the service comes pre-installed.
            if (mPreBundledServiceComponentToCategoryMap.containsKey(componentName)) {
                prefCategory = mPreBundledServiceComponentToCategoryMap.get(componentName);
            }
            preference.setOrder(FIRST_PREFERENCE_IN_CATEGORY_INDEX);
            prefCategory.addPreference(preference);
            mServicePreferenceToPreferenceCategoryMap.put(preference, prefCategory);
        }

        // Update the order of all the category according to the order defined in xml file.
        updateCategoryOrderFromArray(CATEGORY_SCREEN_READER,
            R.array.config_order_screen_reader_services);
        updateCategoryOrderFromArray(CATEGORY_AUDIO_AND_CAPTIONS,
            R.array.config_order_audio_and_caption_services);
        updateCategoryOrderFromArray(CATEGORY_INTERACTION_CONTROL,
            R.array.config_order_interaction_control_services);
        updateCategoryOrderFromArray(CATEGORY_DISPLAY,
            R.array.config_order_display_services);

        // If the user has not installed any additional services, hide the category.
        if (downloadedServicesCategory.getPreferenceCount() == 0) {
            final PreferenceScreen screen = getPreferenceScreen();
            screen.removePreference(downloadedServicesCategory);
        }
    }

    private void initializePreBundledServicesMapFromArray(String categoryKey, int key) {
        String[] services = getResources().getStringArray(key);
        PreferenceCategory category = mCategoryToPrefCategoryMap.get(categoryKey);
        for (int i = 0; i < services.length; i++) {
            ComponentName component = ComponentName.unflattenFromString(services[i]);
            mPreBundledServiceComponentToCategoryMap.put(component, category);
        }
    }

    /**
     * Update the order of perferences in the category by matching their preference
     * key with the string array of preference order which is defined in the xml.
     *
     * @param categoryKey The key of the category need to update the order
     * @param key The key of the string array which defines the order of category
     */
    private void updateCategoryOrderFromArray(String categoryKey, int key) {
        String[] services = getResources().getStringArray(key);
        PreferenceCategory category = mCategoryToPrefCategoryMap.get(categoryKey);
        int preferenceCount = category.getPreferenceCount();
        int serviceLength = services.length;
        for (int preferenceIndex = 0; preferenceIndex < preferenceCount; preferenceIndex++) {
            for (int serviceIndex = 0; serviceIndex < serviceLength; serviceIndex++) {
                if (category.getPreference(preferenceIndex).getKey()
                        .equals(services[serviceIndex])) {
                    category.getPreference(preferenceIndex).setOrder(serviceIndex);
                    break;
                }
            }
        }
    }

    protected void updateSystemPreferences() {
        // Move color inversion and color correction preferences to Display category if device
        // supports HWC hardware-accelerated color transform.
        if (ColorDisplayManager.isColorTransformAccelerated(getContext())) {
            PreferenceCategory experimentalCategory =
                    mCategoryToPrefCategoryMap.get(CATEGORY_EXPERIMENTAL);
            PreferenceCategory displayCategory =
                    mCategoryToPrefCategoryMap.get(CATEGORY_DISPLAY);
            experimentalCategory.removePreference(mToggleInversionPreference);
            experimentalCategory.removePreference(mDisplayDaltonizerPreferenceScreen);
            mDisplayDaltonizerPreferenceScreen.setOrder(
                    mDisplayMagnificationPreferenceScreen.getOrder() + 1);
            mToggleInversionPreference.setOrder(
                    mDisplayDaltonizerPreferenceScreen.getOrder() + 1);
            mToggleLargePointerIconPreference.setOrder(
                    mToggleInversionPreference.getOrder() + 1);
            mToggleDisableAnimationsPreference.setOrder(
                    mToggleLargePointerIconPreference.getOrder() + 1);
            mToggleInversionPreference.setSummary(R.string.summary_empty);
            displayCategory.addPreference(mToggleInversionPreference);
            displayCategory.addPreference(mDisplayDaltonizerPreferenceScreen);
        }

        // Text contrast.
        mToggleHighTextContrastPreference.setChecked(
                Settings.Secure.getInt(getContentResolver(),
                        Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED, 0) == 1);

        // If the quick setting is enabled, the preference MUST be enabled.
        mInversionPreferenceController.updateState(mToggleInversionPreference);

        // Dark Mode
        mDarkUIPreferenceController.updateState(mDarkUIModePreference);

        // Power button ends calls.
        if (KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER)
                && Utils.isVoiceCapable(getActivity())) {
            final int incallPowerBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
            final boolean powerButtonEndsCall =
                    (incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
            mTogglePowerButtonEndsCallPreference.setChecked(powerButtonEndsCall);
        }

        // Auto-rotate screen
        updateLockScreenRotationCheckbox();

        // Large pointer icon.
        mToggleLargePointerIconPreference.setChecked(Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_LARGE_POINTER_ICON, 0) != 0);

        updateDisableAnimationsToggle();

        // Master mono
        updateMasterMono();

        // Long press timeout.
        final int longPressTimeout = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.LONG_PRESS_TIMEOUT, mLongPressTimeoutDefault);
        String value = String.valueOf(longPressTimeout);
        mSelectLongPressTimeoutPreference.setValue(value);
        mSelectLongPressTimeoutPreference.setSummary(mLongPressTimeoutValueToTitleMap.get(value));

        updateVibrationSummary(mVibrationPreferenceScreen);

        mHearingAidPreferenceController.updateState(mHearingAidPreference);

        mLiveCaptionPreferenceController.updateState(mLiveCaptionPreference);

        updateFeatureSummary(Settings.Secure.ACCESSIBILITY_CAPTIONING_ENABLED,
                mCaptioningPreferenceScreen);
        updateFeatureSummary(Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
                mDisplayDaltonizerPreferenceScreen);

        updateMagnificationSummary(mDisplayMagnificationPreferenceScreen);

        updateFontSizeSummary(mFontSizePreferenceScreen);

        updateAutoclickSummary(mAutoclickPreferenceScreen);

        updateAccessibilityShortcut(mAccessibilityShortcutPreferenceScreen);

        updateAccessibilityTimeoutSummary(getContentResolver(),
                findPreference(ACCESSIBILITY_CONTROL_TIMEOUT_PREFERENCE));
    }

    void updateAccessibilityTimeoutSummary(ContentResolver resolver, Preference pref) {
        String[] timeoutSummarys = getResources().getStringArray(
                R.array.accessibility_timeout_summaries);
        int[] timeoutValues = getResources().getIntArray(
                R.array.accessibility_timeout_selector_values);

        int timeoutValue = AccessibilityTimeoutController.getSecureAccessibilityTimeoutValue(
                    resolver, AccessibilityTimeoutController.CONTROL_TIMEOUT_SETTINGS_SECURE);

        int idx = Ints.indexOf(timeoutValues, timeoutValue);
        pref.setSummary(timeoutSummarys[idx == -1 ? 0 : idx]);
    }

    private void updateMagnificationSummary(Preference pref) {
        final boolean tripleTapEnabled = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, 0) == 1;
        final boolean buttonEnabled = Settings.Secure.getInt(getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED, 0) == 1;

        int summaryResId = 0;
        if (!tripleTapEnabled && !buttonEnabled) {
            summaryResId = R.string.accessibility_feature_state_off;
        } else if (!tripleTapEnabled && buttonEnabled) {
            summaryResId = R.string.accessibility_screen_magnification_navbar_title;
        } else if (tripleTapEnabled && !buttonEnabled) {
            summaryResId = R.string.accessibility_screen_magnification_gestures_title;
        } else {
            summaryResId = R.string.accessibility_screen_magnification_state_navbar_gesture;
        }
        pref.setSummary(summaryResId);
    }

    private void updateFeatureSummary(String prefKey, Preference pref) {
        final boolean enabled = Settings.Secure.getInt(getContentResolver(), prefKey, 0) == 1;
        pref.setSummary(enabled ? R.string.accessibility_feature_state_on
                : R.string.accessibility_feature_state_off);
    }

    private void updateAutoclickSummary(Preference pref) {
        final boolean enabled = Settings.Secure.getInt(
                getContentResolver(), Settings.Secure.ACCESSIBILITY_AUTOCLICK_ENABLED, 0) == 1;
        if (!enabled) {
            pref.setSummary(R.string.accessibility_feature_state_off);
            return;
        }
        int delay = Settings.Secure.getInt(
                getContentResolver(), Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                AccessibilityManager.AUTOCLICK_DELAY_DEFAULT);
        pref.setSummary(ToggleAutoclickPreferenceFragment.getAutoclickPreferenceSummary(
                getResources(), delay));
    }

    private void updateFontSizeSummary(Preference pref) {
        final float currentScale = Settings.System.getFloat(getContext().getContentResolver(),
                Settings.System.FONT_SCALE, 1.0f);
        final Resources res = getContext().getResources();
        final String[] entries = res.getStringArray(R.array.entries_font_size);
        final String[] strEntryValues = res.getStringArray(R.array.entryvalues_font_size);
        final int index = ToggleFontSizePreferenceFragment.fontSizeValueToIndex(currentScale,
                strEntryValues);
        pref.setSummary(entries[index]);
    }

    @VisibleForTesting
    void updateVibrationSummary(Preference pref) {
        final Context context = getContext();
        final Vibrator vibrator = context.getSystemService(Vibrator.class);

        int ringIntensity = Settings.System.getInt(context.getContentResolver(),
                Settings.System.RING_VIBRATION_INTENSITY,
                vibrator.getDefaultRingVibrationIntensity());
        if (Settings.System.getInt(context.getContentResolver(),
                Settings.System.VIBRATE_WHEN_RINGING, 0) == 0 && !isRampingRingerEnabled(context)) {
            ringIntensity = Vibrator.VIBRATION_INTENSITY_OFF;
        }
        CharSequence ringIntensityString =
                VibrationIntensityPreferenceController.getIntensityString(context, ringIntensity);

        int notificationIntensity = Settings.System.getInt(context.getContentResolver(),
                Settings.System.NOTIFICATION_VIBRATION_INTENSITY,
                vibrator.getDefaultNotificationVibrationIntensity());
        CharSequence notificationIntensityString =
                VibrationIntensityPreferenceController.getIntensityString(context,
                        notificationIntensity);

        int touchIntensity = Settings.System.getInt(context.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_INTENSITY,
                vibrator.getDefaultHapticFeedbackIntensity());
        if (Settings.System.getInt(context.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 0) == 0) {
            touchIntensity = Vibrator.VIBRATION_INTENSITY_OFF;
        }
        CharSequence touchIntensityString =
                VibrationIntensityPreferenceController.getIntensityString(context, touchIntensity);

        if (mVibrationPreferenceScreen == null) {
            mVibrationPreferenceScreen = findPreference(VIBRATION_PREFERENCE_SCREEN);
        }

        if (ringIntensity == touchIntensity && ringIntensity == notificationIntensity) {
            mVibrationPreferenceScreen.setSummary(ringIntensityString);
        } else {
            mVibrationPreferenceScreen.setSummary(
                    getString(R.string.accessibility_vibration_summary,
                            ringIntensityString,
                            notificationIntensityString,
                            touchIntensityString));
        }
    }

    private String getVibrationSummary(Context context, @VibrationIntensity int intensity) {
        final boolean supportsMultipleIntensities = context.getResources().getBoolean(
                R.bool.config_vibration_supports_multiple_intensities);
        if (supportsMultipleIntensities) {
            switch (intensity) {
                case Vibrator.VIBRATION_INTENSITY_OFF:
                    return context.getString(R.string.accessibility_vibration_summary_off);
                case Vibrator.VIBRATION_INTENSITY_LOW:
                    return context.getString(R.string.accessibility_vibration_summary_low);
                case Vibrator.VIBRATION_INTENSITY_MEDIUM:
                    return context.getString(R.string.accessibility_vibration_summary_medium);
                case Vibrator.VIBRATION_INTENSITY_HIGH:
                    return context.getString(R.string.accessibility_vibration_summary_high);
                default:
                    return "";
            }
        } else {
            if (intensity == Vibrator.VIBRATION_INTENSITY_OFF) {
                return context.getString(R.string.switch_on_text);
            } else {
                return context.getString(R.string.switch_off_text);
            }
        }
    }

    private void updateLockScreenRotationCheckbox() {
        Context context = getActivity();
        if (context != null) {
            mToggleLockScreenRotationPreference.setChecked(
                    !RotationPolicy.isRotationLocked(context));
        }
    }

    private void updateDisableAnimationsToggle() {
        boolean allAnimationsDisabled = true;
        for (String animationSetting : TOGGLE_ANIMATION_TARGETS) {
            if (!TextUtils.equals(
                    Settings.Global.getString(getContentResolver(), animationSetting),
                    ANIMATION_OFF_VALUE)) {
                allAnimationsDisabled = false;
                break;
            }
        }
        mToggleDisableAnimationsPreference.setChecked(allAnimationsDisabled);
    }

    private void updateMasterMono() {
        final boolean masterMono = Settings.System.getIntForUser(
                getContentResolver(), Settings.System.MASTER_MONO,
                0 /* default */, UserHandle.USER_CURRENT) == 1;
        mToggleMasterMonoPreference.setChecked(masterMono);
    }

    private void updateAccessibilityShortcut(Preference preference) {
        if (AccessibilityManager.getInstance(getActivity())
                .getInstalledAccessibilityServiceList().isEmpty()) {
            mAccessibilityShortcutPreferenceScreen
                    .setSummary(getString(R.string.accessibility_no_services_installed));
            mAccessibilityShortcutPreferenceScreen.setEnabled(false);
        } else {
            mAccessibilityShortcutPreferenceScreen.setEnabled(true);
            boolean shortcutEnabled =
                    AccessibilityUtils.isShortcutEnabled(getContext(), UserHandle.myUserId());
            CharSequence summary = shortcutEnabled
                    ? AccessibilityShortcutPreferenceFragment.getServiceName(getContext())
                    : getString(R.string.accessibility_feature_state_off);
            mAccessibilityShortcutPreferenceScreen.setSummary(summary);
        }
    }

    private static void configureMagnificationPreferenceIfNeeded(Preference preference) {
        // Some devices support only a single magnification mode. In these cases, we redirect to
        // the magnification mode's UI directly, rather than showing a PreferenceScreen with a
        // single list item.
        final Context context = preference.getContext();
        if (!MagnificationPreferenceFragment.isApplicable(context.getResources())) {
            preference.setFragment(ToggleScreenMagnificationPreferenceFragment.class.getName());
            final Bundle extras = preference.getExtras();
            MagnificationGesturesPreferenceController
                    .populateMagnificationGesturesPreferenceExtras(extras, context);
        }
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    List<SearchIndexableResource> indexables = new ArrayList<>();
                    SearchIndexableResource indexable = new SearchIndexableResource(context);
                    indexable.xmlResId = R.xml.accessibility_settings;
                    indexables.add(indexable);
                    return indexables;
                }
            };
}
