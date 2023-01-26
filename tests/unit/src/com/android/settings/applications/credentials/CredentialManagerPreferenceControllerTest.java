/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.applications.credentials;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ServiceInfo;
import android.credentials.CredentialProviderInfo;
import android.os.Looper;

import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class CredentialManagerPreferenceControllerTest {

    private Context mContext;
    private PreferenceScreen mScreen;
    private PreferenceCategory mCredentialsPreferenceCategory;

    private static final String TEST_PACKAGE_NAME_A = "com.android.providerA";
    private static final String TEST_PACKAGE_NAME_B = "com.android.providerB";
    private static final String TEST_PACKAGE_NAME_C = "com.android.providerC";
    private static final String TEST_TITLE_APP_A = "test app A";
    private static final String TEST_TITLE_APP_B = "test app B";
    private static final String TEST_TITLE_SERVICE_C = "test service C1";

    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        if (Looper.myLooper() == null) {
            Looper.prepare(); // needed to create the preference screen
        }
        mScreen = new PreferenceManager(mContext).createPreferenceScreen(mContext);
        mCredentialsPreferenceCategory = new PreferenceCategory(mContext);
        mCredentialsPreferenceCategory.setKey("credentials_test");
        mScreen.addPreference(mCredentialsPreferenceCategory);
    }

    @Test
    // Tests that getAvailabilityStatus() does not throw an exception if it's called before the
    // Controller is initialized (this can happen during indexing).
    public void getAvailabilityStatus_withoutInit_returnsUnavailable() {
        CredentialManagerPreferenceController controller =
                new CredentialManagerPreferenceController(
                        mContext, mCredentialsPreferenceCategory.getKey());
        assertThat(controller.isConnected()).isFalse();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noServices_returnsUnavailable() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Collections.emptyList());
        assertThat(controller.isConnected()).isFalse();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_withServices_returnsAvailable() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(createCredentialProviderInfo()));
        assertThat(controller.isConnected()).isFalse();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void displayPreference_noServices_noPreferencesAdded() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Collections.emptyList());
        controller.displayPreference(mScreen);
        assertThat(mCredentialsPreferenceCategory.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void displayPreference_withServices_preferencesAdded() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(createCredentialProviderInfo()));
        controller.displayPreference(mScreen);
        assertThat(controller.isConnected()).isFalse();
        assertThat(mCredentialsPreferenceCategory.getPreferenceCount()).isEqualTo(1);
    }

    @Test
    public void buildSwitchPreference() {
        CredentialProviderInfo providerInfo1 =
                createCredentialProviderInfoWithSettingsSubtitle(
                        "com.android.provider1", "ClassA", "Service Title", null);
        CredentialProviderInfo providerInfo2 =
                createCredentialProviderInfoWithSettingsSubtitle(
                        "com.android.provider2", "ClassA", "Service Title", "Summary Text");
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(providerInfo1, providerInfo2));
        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(controller.isConnected()).isFalse();

        // Test the data is correct.
        assertThat(providerInfo1.isEnabled()).isFalse();
        assertThat(providerInfo2.isEnabled()).isFalse();
        assertThat(controller.getEnabledProviders().size()).isEqualTo(0);

        // Toggle one provider and make sure it worked.
        assertThat(controller.togglePackageNameEnabled("com.android.provider1")).isTrue();
        Set<String> enabledProviders = controller.getEnabledProviders();
        assertThat(enabledProviders.size()).isEqualTo(1);
        assertThat(enabledProviders.contains("com.android.provider1")).isTrue();

        // Create the pref (checked).
        SwitchPreference pref = controller.createPreference(mContext, Arrays.asList(providerInfo1));
        assertThat(pref.getTitle().toString()).isEqualTo("Service Title");
        assertThat(pref.isChecked()).isTrue();
        assertThat(pref.getSummary()).isNull();

        // Create the pref (not checked).
        SwitchPreference pref2 =
                controller.createPreference(mContext, Arrays.asList(providerInfo2));
        assertThat(pref2.getTitle().toString()).isEqualTo("Service Title");
        assertThat(pref2.isChecked()).isFalse();
        assertThat(pref2.getSummary().toString()).isEqualTo("Summary Text");
    }

    @Test
    public void getAvailabilityStatus_handlesToggleAndSave() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(
                        Lists.newArrayList(
                                createCredentialProviderInfo("com.android.provider1", "ClassA"),
                                createCredentialProviderInfo("com.android.provider1", "ClassB"),
                                createCredentialProviderInfo("com.android.provider2", "ClassA"),
                                createCredentialProviderInfo("com.android.provider3", "ClassA"),
                                createCredentialProviderInfo("com.android.provider4", "ClassA"),
                                createCredentialProviderInfo("com.android.provider5", "ClassA"),
                                createCredentialProviderInfo("com.android.provider6", "ClassA")));
        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(controller.isConnected()).isFalse();

        // Ensure that we stay under 5 providers.
        assertThat(controller.togglePackageNameEnabled("com.android.provider1")).isTrue();
        assertThat(controller.togglePackageNameEnabled("com.android.provider2")).isTrue();
        assertThat(controller.togglePackageNameEnabled("com.android.provider3")).isTrue();
        assertThat(controller.togglePackageNameEnabled("com.android.provider4")).isTrue();
        assertThat(controller.togglePackageNameEnabled("com.android.provider5")).isTrue();
        assertThat(controller.togglePackageNameEnabled("com.android.provider6")).isFalse();

        // Check that they are all actually registered.
        Set<String> enabledProviders = controller.getEnabledProviders();
        assertThat(enabledProviders.size()).isEqualTo(5);
        assertThat(enabledProviders.contains("com.android.provider1")).isTrue();
        assertThat(enabledProviders.contains("com.android.provider2")).isTrue();
        assertThat(enabledProviders.contains("com.android.provider3")).isTrue();
        assertThat(enabledProviders.contains("com.android.provider4")).isTrue();
        assertThat(enabledProviders.contains("com.android.provider5")).isTrue();
        assertThat(enabledProviders.contains("com.android.provider6")).isFalse();

        // Check that the settings string has the right component names.
        List<String> enabledServices = controller.getEnabledSettings();
        assertThat(enabledServices.size()).isEqualTo(6);
        assertThat(enabledServices.contains("com.android.provider1/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider1/ClassB")).isTrue();
        assertThat(enabledServices.contains("com.android.provider2/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider3/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider4/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider5/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider6/ClassA")).isFalse();

        // Toggle the provider disabled.
        controller.togglePackageNameDisabled("com.android.provider2");

        // Check that the provider was removed from the list of providers.
        Set<String> currentlyEnabledProviders = controller.getEnabledProviders();
        assertThat(currentlyEnabledProviders.size()).isEqualTo(4);
        assertThat(currentlyEnabledProviders.contains("com.android.provider1")).isTrue();
        assertThat(currentlyEnabledProviders.contains("com.android.provider2")).isFalse();
        assertThat(currentlyEnabledProviders.contains("com.android.provider3")).isTrue();
        assertThat(currentlyEnabledProviders.contains("com.android.provider4")).isTrue();
        assertThat(currentlyEnabledProviders.contains("com.android.provider5")).isTrue();
        assertThat(currentlyEnabledProviders.contains("com.android.provider6")).isFalse();

        // Check that the provider was removed from the list of services stored in the setting.
        List<String> currentlyEnabledServices = controller.getEnabledSettings();
        assertThat(currentlyEnabledServices.size()).isEqualTo(5);
        assertThat(currentlyEnabledServices.contains("com.android.provider1/ClassA")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider1/ClassB")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider3/ClassA")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider4/ClassA")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider5/ClassA")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider6/ClassA")).isFalse();
    }

    @Test
    public void handlesCredentialProviderInfoEnabledDisabled() {
        CredentialProviderInfo providerInfo1 =
                createCredentialProviderInfoWithIsEnabled(
                        "com.android.provider1", "ClassA", "Service Title", false);
        CredentialProviderInfo providerInfo2 =
                createCredentialProviderInfoWithIsEnabled(
                        "com.android.provider2", "ClassA", "Service Title", true);
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(providerInfo1, providerInfo2));
        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(controller.isConnected()).isFalse();

        // Test the data is correct.
        assertThat(providerInfo1.isEnabled()).isFalse();
        assertThat(providerInfo2.isEnabled()).isTrue();

        // Check that they are all actually registered.
        Set<String> enabledProviders = controller.getEnabledProviders();
        assertThat(enabledProviders.size()).isEqualTo(1);
        assertThat(enabledProviders.contains("com.android.provider1")).isFalse();
        assertThat(enabledProviders.contains("com.android.provider2")).isTrue();

        // Check that the settings string has the right component names.
        List<String> enabledServices = controller.getEnabledSettings();
        assertThat(enabledServices.size()).isEqualTo(1);
        assertThat(enabledServices.contains("com.android.provider1/ClassA")).isFalse();
        assertThat(enabledServices.contains("com.android.provider2/ClassA")).isTrue();
    }

    @Test
    public void displayPreference_withServices_preferencesAdded_sameAppShouldBeMerged() {
        CredentialProviderInfo serviceA1 =
                createCredentialProviderInfoWithAppLabel(
                        TEST_PACKAGE_NAME_A,
                        "CredManProviderA1",
                        TEST_TITLE_APP_A,
                        "test service A1");
        CredentialProviderInfo serviceB1 =
                createCredentialProviderInfoWithAppLabel(
                        TEST_PACKAGE_NAME_B,
                        "CredManProviderB1",
                        TEST_TITLE_APP_B,
                        "test service B");
        CredentialProviderInfo serviceC1 =
                createCredentialProviderInfoWithAppLabel(
                        TEST_PACKAGE_NAME_C,
                        "CredManProviderC1",
                        "test app C1",
                        TEST_TITLE_SERVICE_C);
        CredentialProviderInfo serviceC2 =
                createCredentialProviderInfoWithAppLabel(
                        TEST_PACKAGE_NAME_C,
                        "CredManProviderC2",
                        "test app C2",
                        TEST_TITLE_SERVICE_C);
        CredentialProviderInfo serviceC3 =
                createCredentialProviderInfoBuilder(
                                TEST_PACKAGE_NAME_C,
                                "CredManProviderC3",
                                "test app C3",
                                TEST_TITLE_SERVICE_C)
                        .setEnabled(true)
                        .build();

        CredentialManagerPreferenceController controller =
                createControllerWithServices(
                        Lists.newArrayList(serviceA1, serviceB1, serviceC1, serviceC2, serviceC3));
        controller.displayPreference(mScreen);

        assertThat(controller.isConnected()).isFalse();
        assertThat(mCredentialsPreferenceCategory.getPreferenceCount()).isEqualTo(3);

        Map<String, SwitchPreference> prefs =
                controller.buildPreferenceList(mContext, mCredentialsPreferenceCategory);
        assertThat(prefs.size()).isEqualTo(3);
        SwitchPreference pref1 = prefs.get(TEST_PACKAGE_NAME_C);
        assertThat(pref1).isNotNull();
        assertThat(pref1.getTitle()).isEqualTo(TEST_TITLE_APP_A);
        assertThat(pref1.isChecked()).isFalse();
        SwitchPreference pref2 = prefs.get(TEST_PACKAGE_NAME_C);
        assertThat(pref2).isNotNull();
        assertThat(pref2.getTitle()).isEqualTo(TEST_TITLE_APP_B);
        assertThat(pref2.isChecked()).isFalse();
        SwitchPreference pref3 = prefs.get(TEST_PACKAGE_NAME_C);
        assertThat(pref3).isNotNull();
        assertThat(pref3.getTitle()).isEqualTo(TEST_TITLE_SERVICE_C);
        assertThat(pref3.isChecked()).isTrue();
    }

    private CredentialManagerPreferenceController createControllerWithServices(
            List<CredentialProviderInfo> availableServices) {
        CredentialManagerPreferenceController controller =
                new CredentialManagerPreferenceController(
                        mContext, mCredentialsPreferenceCategory.getKey());
        controller.setAvailableServices(() -> mock(Lifecycle.class), availableServices);
        return controller;
    }

    private CredentialProviderInfo createCredentialProviderInfo() {
        return createCredentialProviderInfo("com.android.provider", "CredManProvider");
    }

    private CredentialProviderInfo createCredentialProviderInfo(
            String packageName, String className) {
        return createCredentialProviderInfoBuilder(packageName, className, null, "App Name")
                .build();
    }

    private CredentialProviderInfo createCredentialProviderInfo(
            String packageName, String className, CharSequence label, boolean isEnabled) {
        return createCredentialProviderInfo(packageName, className, label, isEnabled, null);
    }

    private CredentialProviderInfo createCredentialProviderInfo(
            String packageName,
            String className,
            CharSequence label,
            boolean isEnabled,
            CharSequence subtitle) {
        ServiceInfo si = new ServiceInfo();
        si.packageName = packageName;
        si.name = className;
        si.nonLocalizedLabel = "test";

        si.applicationInfo = new ApplicationInfo();
        si.applicationInfo.packageName = packageName;
        si.applicationInfo.nonLocalizedLabel = "test";

        return new CredentialProviderInfo.Builder(si)
                .setOverrideLabel(label)
                .setSettingsSubtitle(subtitle)
                .build();
    }

    private CredentialProviderInfo createCredentialProviderInfoWithIsEnabled(
            String packageName, String className, CharSequence serviceLabel, boolean isEnabled) {
        return createCredentialProviderInfoBuilder(packageName, className, serviceLabel, "App Name")
                .setEnabled(isEnabled)
                .build();
    }

    private CredentialProviderInfo createCredentialProviderInfoWithSettingsSubtitle(
            String packageName, String className, CharSequence serviceLabel, String subtitle) {
        return createCredentialProviderInfoBuilder(packageName, className, serviceLabel, "App Name")
                .setSettingsSubtitle(subtitle)
                .build();
    }

    private CredentialProviderInfo createCredentialProviderInfoWithAppLabel(
            String packageName, String className, CharSequence serviceLabel, String appLabel) {
        return createCredentialProviderInfoBuilder(packageName, className, serviceLabel, appLabel)
                .build();
    }

    private CredentialProviderInfo.Builder createCredentialProviderInfoBuilder(
            String packageName, String className, CharSequence serviceLabel, String appLabel) {
        ServiceInfo si = new ServiceInfo();
        si.packageName = packageName;
        si.name = className;
        si.nonLocalizedLabel = serviceLabel;

        si.applicationInfo = new ApplicationInfo();
        si.applicationInfo.packageName = packageName;
        si.applicationInfo.nonLocalizedLabel = appLabel;

        return new CredentialProviderInfo.Builder(si).setOverrideLabel(serviceLabel);
    }
}
