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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import static java.util.Collections.singletonList;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityShortcutInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.os.Build;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.Flags;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ApplicationProvider;

import com.android.internal.accessibility.util.AccessibilityUtils;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowAccessibilityManager;
import com.android.settings.testutils.shadow.ShadowApplicationPackageManager;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settings.testutils.shadow.ShadowRestrictedLockUtilsInternal;
import com.android.settings.testutils.shadow.ShadowUserManager;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.bluetooth.LocalBluetoothManager;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.testutils.shadow.ShadowColorDisplayManager;

import com.google.common.truth.BooleanSubject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContentResolver;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Test for {@link AccessibilitySettings}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowAccessibilityManager.class,
        ShadowBluetoothAdapter.class,
        ShadowUserManager.class,
        ShadowColorDisplayManager.class,
        ShadowApplicationPackageManager.class,
        ShadowRestrictedLockUtilsInternal.class,
})
public class AccessibilitySettingsTest {
    private static final String PACKAGE_NAME = "com.android.test";
    private static final ComponentName SERVICE_COMPONENT_NAME =
            new ComponentName(PACKAGE_NAME, PACKAGE_NAME + ".test_a11y_service");
    private static final ComponentName ACTIVITY_COMPONENT_NAME =
            new ComponentName(PACKAGE_NAME, PACKAGE_NAME + ".test_a11y_activity");
    private static final String EMPTY_STRING = "";
    private static final String DEFAULT_SUMMARY = "default summary";
    private static final String DEFAULT_DESCRIPTION = "default description";
    private static final String DEFAULT_LABEL = "default label";
    private static final Boolean SERVICE_ENABLED = true;
    private static final Boolean SERVICE_DISABLED = false;

    @Rule
    public final MockitoRule mocks = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Spy
    private final AccessibilityServiceInfo mServiceInfo = getMockAccessibilityServiceInfo(
            SERVICE_COMPONENT_NAME);
    private ShadowAccessibilityManager mShadowAccessibilityManager;
    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    private ActivityController<SettingsActivity> mActivityController;
    private AccessibilitySettings mFragment;

    @Before
    public void setup() {
        mShadowAccessibilityManager = Shadow.extract(
                mContext.getSystemService(AccessibilityManager.class));
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(new ArrayList<>());
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;

        Intent intent = new Intent();
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT,
                AccessibilitySettings.class.getName());

        mActivityController = ActivityController.of(new SettingsActivity(), intent);
    }

    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = AccessibilitySettings.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext, R.xml.accessibility_settings);

        assertThat(keys).containsAtLeastElementsIn(niks);
    }

    @Test
    public void getRawDataToIndex_isNull() {
        final List<SearchIndexableRaw> indexableRawList =
                AccessibilitySettings.SEARCH_INDEX_DATA_PROVIDER
                        .getRawDataToIndex(mContext, true);

        assertThat(indexableRawList).isNull();
    }

    @Test
    public void getServiceSummary_serviceCrash_showsStopped() {
        mServiceInfo.crashed = true;

        final CharSequence summary = AccessibilitySettings.getServiceSummary(mContext,
                mServiceInfo, SERVICE_ENABLED);

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.accessibility_summary_state_stopped));
    }

    @Test
    public void getServiceSummary_invisibleToggle_shortcutEnabled_showsOnSummary() {
        setInvisibleToggleFragmentType(mServiceInfo);
        doReturn(DEFAULT_SUMMARY).when(mServiceInfo).loadSummary(any());
        setShortcutEnabled(mServiceInfo.getComponentName(), true);

        final CharSequence summary = AccessibilitySettings.getServiceSummary(mContext,
                mServiceInfo, SERVICE_ENABLED);

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.preference_summary_default_combination,
                        mContext.getString(R.string.accessibility_summary_shortcut_enabled),
                        DEFAULT_SUMMARY));
    }

    @Test
    public void getServiceSummary_invisibleToggle_shortcutDisabled_showsOffSummary() {
        setInvisibleToggleFragmentType(mServiceInfo);
        setShortcutEnabled(mServiceInfo.getComponentName(), false);
        doReturn(DEFAULT_SUMMARY).when(mServiceInfo).loadSummary(any());

        final CharSequence summary = AccessibilitySettings.getServiceSummary(mContext,
                mServiceInfo, SERVICE_ENABLED);

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.preference_summary_default_combination,
                        mContext.getString(R.string.generic_accessibility_feature_shortcut_off),
                        DEFAULT_SUMMARY));
    }

    @Test
    public void getServiceSummary_enableServiceShortcutOn_showsServiceEnabledShortcutOn() {
        doReturn(EMPTY_STRING).when(mServiceInfo).loadSummary(any());
        setShortcutEnabled(mServiceInfo.getComponentName(), true);

        String summary = AccessibilitySettings.getServiceSummary(mContext,
                mServiceInfo, SERVICE_ENABLED).toString();

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.generic_accessibility_service_on));
    }

    @Test
    public void getServiceSummary_enableServiceShortcutOff_showsServiceEnabledShortcutOff() {
        doReturn(EMPTY_STRING).when(mServiceInfo).loadSummary(any());
        setShortcutEnabled(mServiceInfo.getComponentName(), false);

        String summary = AccessibilitySettings.getServiceSummary(
                mContext, mServiceInfo, SERVICE_ENABLED).toString();

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.generic_accessibility_service_on));
    }

    @Test
    public void getServiceSummary_disableServiceShortcutOff_showsDisabledShortcutOff() {
        doReturn(EMPTY_STRING).when(mServiceInfo).loadSummary(any());
        setShortcutEnabled(mServiceInfo.getComponentName(), false);

        String summary = AccessibilitySettings.getServiceSummary(mContext,
                mServiceInfo, SERVICE_DISABLED).toString();

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.generic_accessibility_service_off));
    }

    @Test
    public void getServiceSummary_disableServiceShortcutOn_showsDisabledShortcutOn() {
        doReturn(EMPTY_STRING).when(mServiceInfo).loadSummary(any());
        setShortcutEnabled(mServiceInfo.getComponentName(), true);

        String summary = AccessibilitySettings.getServiceSummary(mContext,
                mServiceInfo, SERVICE_DISABLED).toString();

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.generic_accessibility_service_off));
    }

    @Test
    public void getServiceSummary_enableServiceShortcutOffAndHasSummary_showsEnabledSummary() {
        setShortcutEnabled(mServiceInfo.getComponentName(), false);
        doReturn(DEFAULT_SUMMARY).when(mServiceInfo).loadSummary(any());

        String summary = AccessibilitySettings.getServiceSummary(mContext,
                mServiceInfo, SERVICE_ENABLED).toString();

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.preference_summary_default_combination,
                        mContext.getString(R.string.generic_accessibility_service_on),
                        DEFAULT_SUMMARY));
    }

    @Test
    public void getServiceSummary_enableServiceShortcutOnAndHasSummary_showsEnabledSummary() {
        doReturn(DEFAULT_SUMMARY).when(mServiceInfo).loadSummary(any());
        setShortcutEnabled(mServiceInfo.getComponentName(), true);

        String summary = AccessibilitySettings.getServiceSummary(mContext,
                mServiceInfo, SERVICE_ENABLED).toString();

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.preference_summary_default_combination,
                        mContext.getString(R.string.generic_accessibility_service_on),
                        DEFAULT_SUMMARY));
    }

    @Test
    public void getServiceSummary_disableServiceShortcutOnAndHasSummary_showsDisabledSummary() {
        doReturn(DEFAULT_SUMMARY).when(mServiceInfo).loadSummary(any());
        setShortcutEnabled(mServiceInfo.getComponentName(), true);

        String summary = AccessibilitySettings.getServiceSummary(mContext,
                mServiceInfo, SERVICE_DISABLED).toString();

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.preference_summary_default_combination,
                        mContext.getString(R.string.generic_accessibility_service_off),
                        DEFAULT_SUMMARY));
    }

    @Test
    public void getServiceSummary_disableServiceShortcutOffAndHasSummary_showsDisabledSummary() {
        setShortcutEnabled(mServiceInfo.getComponentName(), false);
        doReturn(DEFAULT_SUMMARY).when(mServiceInfo).loadSummary(any());

        String summary = AccessibilitySettings.getServiceSummary(mContext,
                mServiceInfo, SERVICE_DISABLED).toString();

        assertThat(summary).isEqualTo(
                mContext.getString(R.string.preference_summary_default_combination,
                        mContext.getString(R.string.generic_accessibility_service_off),
                        DEFAULT_SUMMARY));
    }

    @Test
    public void getServiceDescription_serviceCrash_showsStopped() {
        mServiceInfo.crashed = true;

        String description = AccessibilitySettings.getServiceDescription(mContext,
                mServiceInfo, SERVICE_ENABLED).toString();

        assertThat(description).isEqualTo(
                mContext.getString(R.string.accessibility_description_state_stopped));
    }

    @Test
    public void getServiceDescription_haveDescription_showsDescription() {
        doReturn(DEFAULT_DESCRIPTION).when(mServiceInfo).loadDescription(any());

        String description = AccessibilitySettings.getServiceDescription(mContext,
                mServiceInfo, SERVICE_ENABLED).toString();

        assertThat(description).isEqualTo(DEFAULT_DESCRIPTION);
    }

    @Test
    @DisableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void onCreate_flagDisabled_haveRegisterToSpecificUrisAndActions() {
        setupFragment();

        assertUriObserversContainsClazz(Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS,
                AccessibilitySettingsContentObserver.class).isTrue();
        assertUriObserversContainsClazz(Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE,
                AccessibilitySettingsContentObserver.class).isTrue();
        assertUriObserversContainsClazz(Settings.Secure.ACCESSIBILITY_QS_TARGETS,
                AccessibilitySettingsContentObserver.class).isFalse();
    }

    @Test
    @EnableFlags(Flags.FLAG_A11Y_QS_SHORTCUT)
    public void onCreate_flagEnabled_haveRegisterToSpecificUrisAndActions() {
        setupFragment();

        assertUriObserversContainsClazz(Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS,
                AccessibilitySettingsContentObserver.class).isTrue();
        assertUriObserversContainsClazz(Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE,
                AccessibilitySettingsContentObserver.class).isTrue();
        assertUriObserversContainsClazz(Settings.Secure.ACCESSIBILITY_QS_TARGETS,
                AccessibilitySettingsContentObserver.class).isTrue();
    }

    @Test
    public void onDestroy_unregisterObserverAndReceiver() {
        setupFragment();

        mActivityController.pause().stop().destroy();

        assertUriObserversContainsClazz(Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS,
                AccessibilitySettingsContentObserver.class).isFalse();
        assertUriObserversContainsClazz(Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE,
                AccessibilitySettingsContentObserver.class).isFalse();
        assertUriObserversContainsClazz(Settings.Secure.ACCESSIBILITY_QS_TARGETS,
                AccessibilitySettingsContentObserver.class).isFalse();
    }

    @Test
    public void onContentChanged_updatePreferenceInForeground_preferenceUpdated() {
        setupFragment();
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(
                singletonList(mServiceInfo));

        mFragment.onContentChanged();

        RestrictedPreference preference = mFragment.getPreferenceScreen().findPreference(
                SERVICE_COMPONENT_NAME.flattenToString());

        assertThat(preference).isNotNull();

    }

    @Test
    public void onContentChanged_updatePreferenceInBackground_preferenceUpdated() {
        setupFragment();
        mFragment.onPause();
        mFragment.onStop();

        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(
                singletonList(mServiceInfo));

        mFragment.onContentChanged();
        mFragment.onStart();
        mFragment.onResume();

        RestrictedPreference preference = mFragment.getPreferenceScreen().findPreference(
                SERVICE_COMPONENT_NAME.flattenToString());

        assertThat(preference).isNotNull();

    }

    @Test
    public void testAccessibilityMenuInSystem_IncludedInInteractionControl() {
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(
                List.of(getMockAccessibilityServiceInfo(
                        AccessibilityUtils.ACCESSIBILITY_MENU_IN_SYSTEM)));
        setupFragment();

        final RestrictedPreference pref = mFragment.getPreferenceScreen().findPreference(
                AccessibilityUtils.ACCESSIBILITY_MENU_IN_SYSTEM.flattenToString());
        final String prefCategory = mFragment.mServicePreferenceToPreferenceCategoryMap.get(
                pref).getKey();
        assertThat(prefCategory).isEqualTo(AccessibilitySettings.CATEGORY_INTERACTION_CONTROL);
    }

    @Test
    public void testAccessibilityMenuInSystem_NoPrefWhenNotInstalled() {
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(List.of());
        setupFragment();

        final RestrictedPreference pref = mFragment.getPreferenceScreen().findPreference(
                AccessibilityUtils.ACCESSIBILITY_MENU_IN_SYSTEM.flattenToString());
        assertThat(pref).isNull();
    }

    @Test
    public void testSameNamedServiceAndActivity_bothPreferencesExist() {
        final PackageManager pm = mContext.getPackageManager();
        AccessibilityServiceInfo a11yServiceInfo = mServiceInfo;
        AccessibilityShortcutInfo a11yShortcutInfo = getMockAccessibilityShortcutInfo();
        // Ensure the test service and activity have the same package name and label.
        // Before this change, any service and activity with the same package name and
        // label would cause the service to be hidden.
        assertThat(a11yServiceInfo.getComponentName())
                .isNotEqualTo(a11yShortcutInfo.getComponentName());
        assertThat(a11yServiceInfo.getComponentName().getPackageName())
                .isEqualTo(a11yShortcutInfo.getComponentName().getPackageName());
        assertThat(a11yServiceInfo.getResolveInfo().serviceInfo.loadLabel(pm))
                .isEqualTo(a11yShortcutInfo.getActivityInfo().loadLabel(pm));
        // Prepare A11yManager with the test service and activity.
        mShadowAccessibilityManager.setInstalledAccessibilityServiceList(
                List.of(mServiceInfo));
        mShadowAccessibilityManager.setInstalledAccessibilityShortcutListAsUser(
                List.of(getMockAccessibilityShortcutInfo()));
        setupFragment();

        // Both service and activity preferences should exist on the page.
        RestrictedPreference servicePref = mFragment.getPreferenceScreen().findPreference(
                a11yServiceInfo.getComponentName().flattenToString());
        RestrictedPreference activityPref = mFragment.getPreferenceScreen().findPreference(
                a11yShortcutInfo.getComponentName().flattenToString());
        assertThat(servicePref).isNotNull();
        assertThat(activityPref).isNotNull();
    }

    private AccessibilityServiceInfo getMockAccessibilityServiceInfo(ComponentName componentName) {
        final ApplicationInfo applicationInfo = Mockito.mock(ApplicationInfo.class);
        final ServiceInfo serviceInfo = Mockito.spy(new ServiceInfo());
        applicationInfo.packageName = componentName.getPackageName();
        serviceInfo.packageName = componentName.getPackageName();
        serviceInfo.name = componentName.getClassName();
        serviceInfo.applicationInfo = applicationInfo;
        when(serviceInfo.loadLabel(any())).thenReturn(DEFAULT_LABEL);

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;
        try {
            final AccessibilityServiceInfo info = new AccessibilityServiceInfo(resolveInfo,
                    mContext);
            info.setComponentName(componentName);
            return info;
        } catch (XmlPullParserException | IOException e) {
            // Do nothing
        }

        return null;
    }

    private AccessibilityShortcutInfo getMockAccessibilityShortcutInfo() {
        AccessibilityShortcutInfo mockInfo = Mockito.mock(AccessibilityShortcutInfo.class);
        final ActivityInfo activityInfo = Mockito.mock(ActivityInfo.class);
        activityInfo.applicationInfo = new ApplicationInfo();
        when(mockInfo.getActivityInfo()).thenReturn(activityInfo);
        when(activityInfo.loadLabel(any())).thenReturn(DEFAULT_LABEL);
        when(mockInfo.loadSummary(any())).thenReturn(DEFAULT_SUMMARY);
        when(mockInfo.loadDescription(any())).thenReturn(DEFAULT_DESCRIPTION);
        when(mockInfo.getComponentName()).thenReturn(ACTIVITY_COMPONENT_NAME);
        return mockInfo;
    }

    private void setInvisibleToggleFragmentType(AccessibilityServiceInfo info) {
        info.getResolveInfo().serviceInfo.applicationInfo.targetSdkVersion = Build.VERSION_CODES.R;
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;
    }

    private void setupFragment() {
        mActivityController.create().start().resume();
        Fragment fragment = mActivityController.get().getSupportFragmentManager().findFragmentById(
                R.id.main_content);

        assertThat(fragment).isNotNull();
        assertThat(fragment).isInstanceOf(AccessibilitySettings.class);

        mFragment = (AccessibilitySettings) fragment;
    }

    private void setShortcutEnabled(ComponentName componentName, boolean enabled) {
        Settings.Secure.putString(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_TARGETS,
                enabled ? componentName.flattenToString() : "");
    }

    private BooleanSubject assertUriObserversContainsClazz(
            String settingUri, Class<?> clazz) {
        ShadowContentResolver shadowContentResolver = shadowOf(mContext.getContentResolver());
        Collection<ContentObserver> observers =
                shadowContentResolver.getContentObservers(
                        Settings.Secure.getUriFor(settingUri));

        return assertThat(observers.stream().anyMatch(clazz::isInstance));
    }
}
