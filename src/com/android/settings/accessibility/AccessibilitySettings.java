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

import static com.android.settingslib.TwoTargetPreference.ICON_SIZE_MEDIUM;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Drawable;
import android.hardware.display.ColorDisplayManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.internal.content.PackageMonitor;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.display.DarkUIPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.accessibility.AccessibilityUtils;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Activity with the accessibility settings.
 */
@SearchIndexable
public class AccessibilitySettings extends DashboardFragment {

    private static final String TAG = "AccessibilitySettings";

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
    private static final String TOGGLE_INVERSION_PREFERENCE =
            "toggle_inversion_preference";
    private static final String TOGGLE_LARGE_POINTER_ICON =
            "toggle_large_pointer_icon";
    private static final String TOGGLE_DISABLE_ANIMATIONS = "toggle_disable_animations";
    private static final String HEARING_AID_PREFERENCE =
            "hearing_aid_preference";
    private static final String DISPLAY_MAGNIFICATION_PREFERENCE_SCREEN =
            "magnification_preference_screen";
    private static final String DISPLAY_DALTONIZER_PREFERENCE_SCREEN =
            "daltonizer_preference";
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

    static final String RAMPING_RINGER_ENABLED = "ramping_ringer_enabled";

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

    private final Map<String, PreferenceCategory> mCategoryToPrefCategoryMap =
            new ArrayMap<>();
    private final Map<Preference, PreferenceCategory> mServicePreferenceToPreferenceCategoryMap =
            new ArrayMap<>();
    private final Map<ComponentName, PreferenceCategory> mPreBundledServiceComponentToCategoryMap =
            new ArrayMap<>();

    private SwitchPreference mToggleLargePointerIconPreference;
    private SwitchPreference mToggleDisableAnimationsPreference;
    private Preference mDisplayMagnificationPreferenceScreen;
    private Preference mDisplayDaltonizerPreferenceScreen;
    private Preference mHearingAidPreference;
    private Preference mLiveCaptionPreference;
    private SwitchPreference mToggleInversionPreference;
    private AccessibilityHearingAidPreferenceController mHearingAidPreferenceController;
    private SwitchPreference mDarkUIModePreference;
    private DarkUIPreferenceController mDarkUIPreferenceController;
    private LiveCaptionPreferenceController mLiveCaptionPreferenceController;

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
    public void onStart() {
        super.onStart();
        updateAllPreferences();

        mSettingsPackageMonitor.register(getActivity(), getActivity().getMainLooper(), false);
        mSettingsContentObserver.register(getContentResolver());
    }

    @Override
    public void onStop() {
        mSettingsPackageMonitor.unregister();
        mSettingsContentObserver.unregister(getContentResolver());
        super.onStop();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (mHearingAidPreferenceController.handlePreferenceTreeClick(preference)) {
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
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

    private void initializeAllPreferences() {
        for (int i = 0; i < CATEGORIES.length; i++) {
            PreferenceCategory prefCategory = (PreferenceCategory) findPreference(CATEGORIES[i]);
            mCategoryToPrefCategoryMap.put(CATEGORIES[i], prefCategory);
        }

        // Display inversion.
        mToggleInversionPreference = findPreference(TOGGLE_INVERSION_PREFERENCE);

        // Large pointer icon.
        mToggleLargePointerIconPreference = findPreference(TOGGLE_LARGE_POINTER_ICON);

        mToggleDisableAnimationsPreference =
                (SwitchPreference) findPreference(TOGGLE_DISABLE_ANIMATIONS);

        // Hearing Aid.
        mHearingAidPreference = findPreference(HEARING_AID_PREFERENCE);
        mHearingAidPreferenceController.displayPreference(getPreferenceScreen());

        // Live caption
        mLiveCaptionPreference = findPreference(LIVE_CAPTION_PREFERENCE_KEY);
        mLiveCaptionPreferenceController.displayPreference(getPreferenceScreen());

        // Display magnification.
        mDisplayMagnificationPreferenceScreen = findPreference(
                DISPLAY_MAGNIFICATION_PREFERENCE_SCREEN);

        // Display color adjustments.
        mDisplayDaltonizerPreferenceScreen = findPreference(DISPLAY_DALTONIZER_PREFERENCE_SCREEN);

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

        // Dark Mode
        mDarkUIPreferenceController.updateState(mDarkUIModePreference);

        mHearingAidPreferenceController.updateState(mHearingAidPreference);

        mLiveCaptionPreferenceController.updateState(mLiveCaptionPreference);
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
