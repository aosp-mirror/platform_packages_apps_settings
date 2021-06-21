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

import static com.android.settingslib.widget.TwoTargetPreference.ICON_SIZE_MEDIUM;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityShortcutInfo;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import com.android.internal.accessibility.AccessibilityShortcutController;
import com.android.internal.content.PackageMonitor;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.accessibility.AccessibilityUtil.AccessibilityServiceFragmentType;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.accessibility.AccessibilityUtils;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Activity with the accessibility settings. */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class AccessibilitySettings extends DashboardFragment {

    private static final String TAG = "AccessibilitySettings";

    // Index of the first preference in a preference category.
    private static final int FIRST_PREFERENCE_IN_CATEGORY_INDEX = -1;

    // Preference categories
    private static final String CATEGORY_SCREEN_READER = "screen_reader_category";
    private static final String CATEGORY_CAPTIONS = "captions_category";
    private static final String CATEGORY_AUDIO = "audio_category";
    private static final String CATEGORY_DISPLAY = "display_category";
    private static final String CATEGORY_INTERACTION_CONTROL = "interaction_control_category";
    private static final String CATEGORY_DOWNLOADED_SERVICES = "user_installed_services_category";

    private static final String[] CATEGORIES = new String[]{
            CATEGORY_SCREEN_READER, CATEGORY_CAPTIONS, CATEGORY_AUDIO, CATEGORY_DISPLAY,
            CATEGORY_INTERACTION_CONTROL, CATEGORY_DOWNLOADED_SERVICES
    };

    // Extras passed to sub-fragments.
    static final String EXTRA_PREFERENCE_KEY = "preference_key";
    static final String EXTRA_CHECKED = "checked";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_TITLE_RES = "title_res";
    static final String EXTRA_RESOLVE_INFO = "resolve_info";
    static final String EXTRA_SUMMARY = "summary";
    static final String EXTRA_SETTINGS_TITLE = "settings_title";
    static final String EXTRA_COMPONENT_NAME = "component_name";
    static final String EXTRA_SETTINGS_COMPONENT_NAME = "settings_component_name";
    static final String EXTRA_VIDEO_RAW_RESOURCE_ID = "video_resource";
    static final String EXTRA_LAUNCHED_FROM_SUW = "from_suw";
    static final String EXTRA_ANIMATED_IMAGE_RES = "animated_image_res";
    static final String EXTRA_HTML_DESCRIPTION = "html_description";

    // Timeout before we update the services if packages are added/removed
    // since the AccessibilityManagerService has to do that processing first
    // to generate the AccessibilityServiceInfo we need for proper
    // presentation.
    private static final long DELAY_UPDATE_SERVICES_MILLIS = 1000;

    private final Handler mHandler = new Handler();

    private final Runnable mUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (getActivity() != null) {
                onContentChanged();
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

    @VisibleForTesting
    final SettingsContentObserver mSettingsContentObserver;

    private final Map<String, PreferenceCategory> mCategoryToPrefCategoryMap =
            new ArrayMap<>();
    private final Map<Preference, PreferenceCategory> mServicePreferenceToPreferenceCategoryMap =
            new ArrayMap<>();
    private final Map<ComponentName, PreferenceCategory> mPreBundledServiceComponentToCategoryMap =
            new ArrayMap<>();

    private boolean mNeedPreferencesUpdate = false;
    private boolean mIsForeground = true;

    public AccessibilitySettings() {
        // Observe changes to anything that the shortcut can toggle, so we can reflect updates
        final Collection<AccessibilityShortcutController.ToggleableFrameworkFeatureInfo> features =
                AccessibilityShortcutController.getFrameworkShortcutFeaturesMap().values();
        final List<String> shortcutFeatureKeys = new ArrayList<>(features.size());
        for (AccessibilityShortcutController.ToggleableFrameworkFeatureInfo feature : features) {
            shortcutFeatureKeys.add(feature.getSettingKey());
        }

        // Observe changes from accessibility selection menu
        shortcutFeatureKeys.add(Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS);
        shortcutFeatureKeys.add(Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE);
        mSettingsContentObserver = new SettingsContentObserver(mHandler, shortcutFeatureKeys) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                onContentChanged();
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
    public void onAttach(Context context) {
        super.onAttach(context);
        use(AccessibilityHearingAidPreferenceController.class)
                .setFragmentManager(getFragmentManager());
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        initializeAllPreferences();
        updateAllPreferences();
        registerContentMonitors();
    }

    @Override
    public void onStart() {
        if (mNeedPreferencesUpdate) {
            updateAllPreferences();
            mNeedPreferencesUpdate = false;
        }
        mIsForeground = true;
        super.onStart();
    }

    @Override
    public void onStop() {
        mIsForeground = false;
        super.onStop();
    }

    @Override
    public void onDestroy() {
        unregisterContentMonitors();
        super.onDestroy();
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.accessibility_settings;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    /**
     * Returns the summary for the current state of this accessibilityService.
     *
     * @param context        A valid context
     * @param info           The accessibilityService's info
     * @param serviceEnabled Whether the accessibility service is enabled.
     * @return The service summary
     */
    @VisibleForTesting
    static CharSequence getServiceSummary(Context context, AccessibilityServiceInfo info,
            boolean serviceEnabled) {
        if (serviceEnabled && info.crashed) {
            return context.getText(R.string.accessibility_summary_state_stopped);
        }

        final CharSequence serviceState;
        final int fragmentType = AccessibilityUtil.getAccessibilityServiceFragmentType(info);
        if (fragmentType == AccessibilityServiceFragmentType.INVISIBLE_TOGGLE) {
            final ComponentName componentName = new ComponentName(
                    info.getResolveInfo().serviceInfo.packageName,
                    info.getResolveInfo().serviceInfo.name);
            final boolean shortcutEnabled = AccessibilityUtil.getUserShortcutTypesFromSettings(
                    context, componentName) != AccessibilityUtil.UserShortcutType.EMPTY;
            serviceState = shortcutEnabled
                    ? context.getText(R.string.accessibility_summary_shortcut_enabled)
                    : context.getText(R.string.accessibility_summary_shortcut_disabled);
        } else {
            serviceState = serviceEnabled
                    ? context.getText(R.string.accessibility_summary_state_enabled)
                    : context.getText(R.string.accessibility_summary_state_disabled);
        }

        final CharSequence serviceSummary = info.loadSummary(context.getPackageManager());
        final String stateSummaryCombo = context.getString(
                R.string.preference_summary_default_combination,
                serviceState, serviceSummary);

        return TextUtils.isEmpty(serviceSummary) ? serviceState : stateSummaryCombo;
    }

    /**
     * Returns the description for the current state of this accessibilityService.
     *
     * @param context        A valid context
     * @param info           The accessibilityService's info
     * @param serviceEnabled Whether the accessibility service is enabled.
     * @return The service description
     */
    @VisibleForTesting
    static CharSequence getServiceDescription(Context context, AccessibilityServiceInfo info,
            boolean serviceEnabled) {
        if (serviceEnabled && info.crashed) {
            return context.getText(R.string.accessibility_description_state_stopped);
        }

        return info.loadDescription(context.getPackageManager());
    }

    static boolean isRampingRingerEnabled(final Context context) {
        return Settings.Global.getInt(
                context.getContentResolver(), Settings.Global.APPLY_RAMPING_RINGER, 0) == 1;
    }

    @VisibleForTesting
    void onContentChanged() {
        // If the fragment is visible then update preferences immediately, else set the flag then
        // wait for the fragment to show up to update preferences.
        if (mIsForeground) {
            updateAllPreferences();
        } else {
            mNeedPreferencesUpdate = true;
        }
    }

    private void initializeAllPreferences() {
        for (int i = 0; i < CATEGORIES.length; i++) {
            PreferenceCategory prefCategory = findPreference(CATEGORIES[i]);
            mCategoryToPrefCategoryMap.put(CATEGORIES[i], prefCategory);
        }
    }

    @VisibleForTesting
    void updateAllPreferences() {
        updateSystemPreferences();
        updateServicePreferences();
    }

    private void registerContentMonitors() {
        final Context context = getActivity();

        mSettingsPackageMonitor.register(context, context.getMainLooper(), /* externalStorage= */
                false);
        mSettingsContentObserver.register(getContentResolver());
    }

    private void unregisterContentMonitors() {
        mSettingsPackageMonitor.unregister();
        mSettingsContentObserver.unregister(getContentResolver());
    }

    protected void updateServicePreferences() {
        // Since services category is auto generated we have to do a pass
        // to generate it since services can come and go and then based on
        // the global accessibility state to decided whether it is enabled.
        final ArrayList<Preference> servicePreferences =
                new ArrayList<>(mServicePreferenceToPreferenceCategoryMap.keySet());
        for (int i = 0; i < servicePreferences.size(); i++) {
            Preference service = servicePreferences.get(i);
            PreferenceCategory category = mServicePreferenceToPreferenceCategoryMap.get(service);
            category.removePreference(service);
        }

        initializePreBundledServicesMapFromArray(CATEGORY_SCREEN_READER,
                R.array.config_preinstalled_screen_reader_services);
        initializePreBundledServicesMapFromArray(CATEGORY_CAPTIONS,
                R.array.config_preinstalled_captions_services);
        initializePreBundledServicesMapFromArray(CATEGORY_AUDIO,
                R.array.config_preinstalled_audio_services);
        initializePreBundledServicesMapFromArray(CATEGORY_DISPLAY,
                R.array.config_preinstalled_display_services);
        initializePreBundledServicesMapFromArray(CATEGORY_INTERACTION_CONTROL,
                R.array.config_preinstalled_interaction_control_services);

        final List<RestrictedPreference> preferenceList = getInstalledAccessibilityList(
                getPrefContext());

        final PreferenceCategory downloadedServicesCategory =
                mCategoryToPrefCategoryMap.get(CATEGORY_DOWNLOADED_SERVICES);

        for (int i = 0, count = preferenceList.size(); i < count; ++i) {
            final RestrictedPreference preference = preferenceList.get(i);
            final ComponentName componentName = preference.getExtras().getParcelable(
                    EXTRA_COMPONENT_NAME);
            PreferenceCategory prefCategory = downloadedServicesCategory;
            // Set the appropriate category if the service comes pre-installed.
            if (mPreBundledServiceComponentToCategoryMap.containsKey(componentName)) {
                prefCategory = mPreBundledServiceComponentToCategoryMap.get(componentName);
            }
            prefCategory.addPreference(preference);
            mServicePreferenceToPreferenceCategoryMap.put(preference, prefCategory);
        }

        // Update the order of all the category according to the order defined in xml file.
        updateCategoryOrderFromArray(CATEGORY_SCREEN_READER,
                R.array.config_order_screen_reader_services);
        updateCategoryOrderFromArray(CATEGORY_CAPTIONS,
                R.array.config_order_captions_services);
        updateCategoryOrderFromArray(CATEGORY_AUDIO,
                R.array.config_order_audio_services);
        updateCategoryOrderFromArray(CATEGORY_INTERACTION_CONTROL,
                R.array.config_order_interaction_control_services);
        updateCategoryOrderFromArray(CATEGORY_DISPLAY,
                R.array.config_order_display_services);

        // Need to check each time when updateServicePreferences() called.
        if (downloadedServicesCategory.getPreferenceCount() == 0) {
            getPreferenceScreen().removePreference(downloadedServicesCategory);
        } else {
            getPreferenceScreen().addPreference(downloadedServicesCategory);
        }

        // Hide screen reader category if it is empty.
        updatePreferenceCategoryVisibility(CATEGORY_SCREEN_READER);
    }

    private List<RestrictedPreference> getInstalledAccessibilityList(Context context) {
        final AccessibilityManager a11yManager = AccessibilityManager.getInstance(context);
        final RestrictedPreferenceHelper preferenceHelper = new RestrictedPreferenceHelper(context);

        final List<AccessibilityShortcutInfo> installedShortcutList =
                a11yManager.getInstalledAccessibilityShortcutListAsUser(context,
                        UserHandle.myUserId());

        // Remove duplicate item here, new a ArrayList to copy unmodifiable list result
        // (getInstalledAccessibilityServiceList).
        final List<AccessibilityServiceInfo> installedServiceList = new ArrayList<>(
                a11yManager.getInstalledAccessibilityServiceList());
        installedServiceList.removeIf(
                target -> containsTargetNameInList(installedShortcutList, target));

        final List<RestrictedPreference> activityList =
                preferenceHelper.createAccessibilityActivityPreferenceList(installedShortcutList);

        final List<RestrictedPreference> serviceList =
                preferenceHelper.createAccessibilityServicePreferenceList(installedServiceList);

        final List<RestrictedPreference> preferenceList = new ArrayList<>();
        preferenceList.addAll(activityList);
        preferenceList.addAll(serviceList);

        return preferenceList;
    }

    private boolean containsTargetNameInList(List<AccessibilityShortcutInfo> shortcutInfos,
            AccessibilityServiceInfo targetServiceInfo) {
        final ServiceInfo serviceInfo = targetServiceInfo.getResolveInfo().serviceInfo;
        final String servicePackageName = serviceInfo.packageName;
        final CharSequence serviceLabel = serviceInfo.loadLabel(getPackageManager());

        for (int i = 0, count = shortcutInfos.size(); i < count; ++i) {
            final ActivityInfo activityInfo = shortcutInfos.get(i).getActivityInfo();
            final String activityPackageName = activityInfo.packageName;
            final CharSequence activityLabel = activityInfo.loadLabel(getPackageManager());
            if (servicePackageName.equals(activityPackageName)
                    && serviceLabel.equals(activityLabel)) {
                return true;
            }
        }
        return false;
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
     * Update the order of preferences in the category by matching their preference
     * key with the string array of preference order which is defined in the xml.
     *
     * @param categoryKey The key of the category need to update the order
     * @param key         The key of the string array which defines the order of category
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

    /**
     * Updates the visibility of a category according to its child preference count.
     *
     * @param categoryKey The key of the category which needs to check
     */
    private void updatePreferenceCategoryVisibility(String categoryKey) {
        final PreferenceCategory category = mCategoryToPrefCategoryMap.get(categoryKey);
        category.setVisible(category.getPreferenceCount() != 0);
    }

    /**
     * Updates preferences related to system configurations.
     */
    protected void updateSystemPreferences() {
        // Do nothing.
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.accessibility_settings) {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {
                    return FeatureFactory.getFactory(context)
                            .getAccessibilitySearchFeatureProvider().getSearchIndexableRawData(
                                    context);
                }
            };

    /**
     * This class helps setup RestrictedPreference.
     */
    @VisibleForTesting
    static class RestrictedPreferenceHelper {
        private final Context mContext;
        private final DevicePolicyManager mDpm;
        private final PackageManager mPm;

        RestrictedPreferenceHelper(Context context) {
            mContext = context;
            mDpm = context.getSystemService(DevicePolicyManager.class);
            mPm = context.getPackageManager();
        }

        /**
         * Creates the list of {@link RestrictedPreference} with the installedServices arguments.
         *
         * @param installedServices The list of {@link AccessibilityServiceInfo}s of the
         *                          installed accessibility services
         * @return The list of {@link RestrictedPreference}
         */
        @VisibleForTesting
        List<RestrictedPreference> createAccessibilityServicePreferenceList(
                List<AccessibilityServiceInfo> installedServices) {

            final Set<ComponentName> enabledServices =
                    AccessibilityUtils.getEnabledServicesFromSettings(mContext);
            final List<String> permittedServices = mDpm.getPermittedAccessibilityServices(
                    UserHandle.myUserId());
            final int installedServicesSize = installedServices.size();

            final List<RestrictedPreference> preferenceList = new ArrayList<>(
                    installedServicesSize);

            for (int i = 0; i < installedServicesSize; ++i) {
                final AccessibilityServiceInfo info = installedServices.get(i);
                final ResolveInfo resolveInfo = info.getResolveInfo();
                final String packageName = resolveInfo.serviceInfo.packageName;
                final ComponentName componentName = new ComponentName(packageName,
                        resolveInfo.serviceInfo.name);

                final String key = componentName.flattenToString();
                final CharSequence title = resolveInfo.loadLabel(mPm);
                final boolean serviceEnabled = enabledServices.contains(componentName);
                final CharSequence summary = getServiceSummary(mContext, info, serviceEnabled);
                final String fragment = getAccessibilityServiceFragmentTypeName(info);

                Drawable icon = resolveInfo.loadIcon(mPm);
                if (resolveInfo.getIconResource() == 0) {
                    icon = ContextCompat.getDrawable(mContext,
                            R.drawable.ic_accessibility_generic);
                }

                final RestrictedPreference preference = createRestrictedPreference(key, title,
                        summary, icon, fragment);

                // permittedServices null means all accessibility services are allowed.
                final boolean serviceAllowed =
                        permittedServices == null || permittedServices.contains(packageName);

                setRestrictedPreferenceEnabled(preference, packageName, serviceAllowed,
                        serviceEnabled);
                final String prefKey = preference.getKey();
                final int imageRes = info.getAnimatedImageRes();
                final CharSequence description = getServiceDescription(mContext, info,
                        serviceEnabled);
                final String htmlDescription = info.loadHtmlDescription(mPm);
                final String settingsClassName = info.getSettingsActivityName();

                putBasicExtras(preference, prefKey, title, description, imageRes, htmlDescription,
                        componentName);
                putServiceExtras(preference, resolveInfo, serviceEnabled);
                putSettingsExtras(preference, packageName, settingsClassName);

                preferenceList.add(preference);
            }
            return preferenceList;
        }

        /**
         * Create the list of {@link RestrictedPreference} with the installedShortcuts arguments.
         *
         * @param installedShortcuts The list of {@link AccessibilityShortcutInfo}s of the
         *                           installed accessibility shortcuts
         * @return The list of {@link RestrictedPreference}
         */
        @VisibleForTesting
        List<RestrictedPreference> createAccessibilityActivityPreferenceList(
                List<AccessibilityShortcutInfo> installedShortcuts) {
            final Set<ComponentName> enabledServices =
                    AccessibilityUtils.getEnabledServicesFromSettings(mContext);
            final List<String> permittedServices = mDpm.getPermittedAccessibilityServices(
                    UserHandle.myUserId());

            final int installedShortcutsSize = installedShortcuts.size();
            final List<RestrictedPreference> preferenceList = new ArrayList<>(
                    installedShortcutsSize);

            for (int i = 0; i < installedShortcutsSize; ++i) {
                final AccessibilityShortcutInfo info = installedShortcuts.get(i);
                final ActivityInfo activityInfo = info.getActivityInfo();
                final ComponentName componentName = info.getComponentName();

                final String key = componentName.flattenToString();
                final CharSequence title = activityInfo.loadLabel(mPm);
                final String summary = info.loadSummary(mPm);
                final String fragment =
                        LaunchAccessibilityActivityPreferenceFragment.class.getName();

                Drawable icon = activityInfo.loadIcon(mPm);
                if (activityInfo.getIconResource() == 0) {
                    icon = ContextCompat.getDrawable(mContext, R.drawable.ic_accessibility_generic);
                }

                final RestrictedPreference preference = createRestrictedPreference(key, title,
                        summary, icon, fragment);

                final String packageName = componentName.getPackageName();
                // permittedServices null means all accessibility services are allowed.
                final boolean serviceAllowed =
                        permittedServices == null || permittedServices.contains(packageName);
                final boolean serviceEnabled = enabledServices.contains(componentName);

                setRestrictedPreferenceEnabled(preference, packageName, serviceAllowed,
                        serviceEnabled);

                final String prefKey = preference.getKey();
                final String description = info.loadDescription(mPm);
                final int imageRes = info.getAnimatedImageRes();
                final String htmlDescription = info.loadHtmlDescription(mPm);
                final String settingsClassName = info.getSettingsActivityName();

                putBasicExtras(preference, prefKey, title, description, imageRes, htmlDescription,
                        componentName);
                putSettingsExtras(preference, packageName, settingsClassName);

                preferenceList.add(preference);
            }
            return preferenceList;
        }

        private String getAccessibilityServiceFragmentTypeName(AccessibilityServiceInfo info) {
            // Shorten the name to avoid exceeding 100 characters in one line.
            final String volumeShortcutToggleAccessibilityServicePreferenceFragment =
                    VolumeShortcutToggleAccessibilityServicePreferenceFragment.class.getName();

            switch (AccessibilityUtil.getAccessibilityServiceFragmentType(info)) {
                case AccessibilityServiceFragmentType.VOLUME_SHORTCUT_TOGGLE:
                    return volumeShortcutToggleAccessibilityServicePreferenceFragment;
                case AccessibilityServiceFragmentType.INVISIBLE_TOGGLE:
                    return InvisibleToggleAccessibilityServicePreferenceFragment.class.getName();
                case AccessibilityServiceFragmentType.TOGGLE:
                    return ToggleAccessibilityServicePreferenceFragment.class.getName();
                default:
                    // impossible status
                    throw new AssertionError();
            }
        }

        private RestrictedPreference createRestrictedPreference(String key, CharSequence title,
                CharSequence summary, Drawable icon, String fragment) {
            final RestrictedPreference preference = new RestrictedPreference(mContext);

            preference.setKey(key);
            preference.setTitle(title);
            preference.setSummary(summary);
            preference.setIcon(Utils.getAdaptiveIcon(mContext, icon, Color.WHITE));
            preference.setFragment(fragment);
            preference.setIconSize(ICON_SIZE_MEDIUM);
            preference.setPersistent(false); // Disable SharedPreferences.
            preference.setOrder(FIRST_PREFERENCE_IN_CATEGORY_INDEX);

            return preference;
        }

        private void setRestrictedPreferenceEnabled(RestrictedPreference preference,
                String packageName, boolean serviceAllowed, boolean serviceEnabled) {
            if (serviceAllowed || serviceEnabled) {
                preference.setEnabled(true);
            } else {
                // Disable accessibility service that are not permitted.
                final EnforcedAdmin admin =
                        RestrictedLockUtilsInternal.checkIfAccessibilityServiceDisallowed(
                                mContext, packageName, UserHandle.myUserId());
                if (admin != null) {
                    preference.setDisabledByAdmin(admin);
                } else {
                    preference.setEnabled(false);
                }
            }
        }

        /** Puts the basic extras into {@link RestrictedPreference}'s getExtras(). */
        private void putBasicExtras(RestrictedPreference preference, String prefKey,
                CharSequence title, CharSequence summary, int imageRes, String htmlDescription,
                ComponentName componentName) {
            final Bundle extras = preference.getExtras();
            extras.putString(EXTRA_PREFERENCE_KEY, prefKey);
            extras.putCharSequence(EXTRA_TITLE, title);
            extras.putCharSequence(EXTRA_SUMMARY, summary);
            extras.putParcelable(EXTRA_COMPONENT_NAME, componentName);
            extras.putInt(EXTRA_ANIMATED_IMAGE_RES, imageRes);
            extras.putString(AccessibilitySettings.EXTRA_HTML_DESCRIPTION, htmlDescription);
        }

        /**
         * Puts the service extras into {@link RestrictedPreference}'s getExtras().
         *
         * Called by {@link AccessibilityServiceInfo} for now.
         */
        private void putServiceExtras(RestrictedPreference preference, ResolveInfo resolveInfo,
                Boolean serviceEnabled) {
            final Bundle extras = preference.getExtras();

            extras.putParcelable(EXTRA_RESOLVE_INFO, resolveInfo);
            extras.putBoolean(EXTRA_CHECKED, serviceEnabled);
        }

        /**
         * Puts the settings extras into {@link RestrictedPreference}'s getExtras().
         *
         * Called when settings UI is needed.
         */
        private void putSettingsExtras(RestrictedPreference preference, String packageName,
                String settingsClassName) {
            final Bundle extras = preference.getExtras();

            if (!TextUtils.isEmpty(settingsClassName)) {
                extras.putString(EXTRA_SETTINGS_TITLE,
                        mContext.getText(R.string.accessibility_menu_item_settings).toString());
                extras.putString(EXTRA_SETTINGS_COMPONENT_NAME,
                        new ComponentName(packageName, settingsClassName).flattenToString());
            }
        }
    }
}
