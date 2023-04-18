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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ServiceInfo;
import android.credentials.CredentialProviderInfo;
import android.net.Uri;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
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
import java.util.Optional;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class CredentialManagerPreferenceControllerTest {

    private Context mContext;
    private PreferenceScreen mScreen;
    private PreferenceCategory mCredentialsPreferenceCategory;
    private CredentialManagerPreferenceController.Delegate mDelegate;
    private Optional<Integer> mReceivedResultCode;

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
        mReceivedResultCode = Optional.empty();
        mDelegate =
                new CredentialManagerPreferenceController.Delegate() {
                    @Override
                    public void setActivityResult(int resultCode) {
                        mReceivedResultCode = Optional.of(resultCode);
                    }
                };
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
    public void displayPreference_noServices_noPreferencesAdded_useAutofillUri() {
        Settings.Secure.putStringForUser(
                mContext.getContentResolver(),
                Settings.Secure.AUTOFILL_SERVICE_SEARCH_URI,
                "test",
                UserHandle.myUserId());

        CredentialManagerPreferenceController controller =
                createControllerWithServices(Collections.emptyList());
        controller.displayPreference(mScreen);
        assertThat(mCredentialsPreferenceCategory.getPreferenceCount()).isEqualTo(1);

        Preference pref = mCredentialsPreferenceCategory.getPreference(0);
        assertThat(pref.getTitle()).isEqualTo("Add service");

        assertThat(controller.getAddServiceUri(mContext)).isEqualTo("test");
    }

    @Test
    public void displayPreference_noServices_noPreferencesAdded_useCredManUri() {
        Settings.Secure.putStringForUser(
                mContext.getContentResolver(),
                Settings.Secure.AUTOFILL_SERVICE_SEARCH_URI,
                "test",
                UserHandle.myUserId());

        CredentialManagerPreferenceController controller =
                createControllerWithServicesAndAddServiceOverride(
                        Collections.emptyList(), "credman");
        controller.displayPreference(mScreen);
        assertThat(mCredentialsPreferenceCategory.getPreferenceCount()).isEqualTo(1);

        Preference pref = mCredentialsPreferenceCategory.getPreference(0);
        assertThat(pref.getTitle()).isEqualTo("Add service");

        assertThat(controller.getAddServiceUri(mContext)).isEqualTo("credman");
    }

    @Test
    public void displayPreference_withServices_preferencesAdded() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(createCredentialProviderInfo()));
        controller.displayPreference(mScreen);
        assertThat(controller.isConnected()).isFalse();
        assertThat(mCredentialsPreferenceCategory.getPreferenceCount()).isEqualTo(1);

        Preference pref = mCredentialsPreferenceCategory.getPreference(0);
        assertThat(pref.getTitle()).isNotEqualTo("Add account");
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
        assertThat(controller.toggleServiceInfoEnabled(providerInfo1.getServiceInfo())).isTrue();
        Set<ServiceInfo> enabledProviders = controller.getEnabledProviders();
        assertThat(enabledProviders.size()).isEqualTo(1);
        assertThat(enabledProviders.contains(providerInfo1.getServiceInfo())).isTrue();

        // Create the pref (checked).
        SwitchPreference pref = controller.createPreference(mContext, providerInfo1);
        assertThat(pref.getTitle().toString()).isEqualTo("Service Title");
        assertThat(pref.isChecked()).isTrue();
        assertThat(pref.getSummary()).isNull();

        // Create the pref (not checked).
        SwitchPreference pref2 =
                controller.createPreference(mContext, providerInfo2);
        assertThat(pref2.getTitle().toString()).isEqualTo("Service Title");
        assertThat(pref2.isChecked()).isFalse();
        assertThat(pref2.getSummary().toString()).isEqualTo("Summary Text");
    }

    @Test
    public void getAvailabilityStatus_handlesToggleAndSave() {
        ServiceInfo providerService1a = createServiceInfo("com.android.provider1", "ClassA");
        ServiceInfo providerService1b = createServiceInfo("com.android.provider1", "ClassB");
        ServiceInfo providerService2 = createServiceInfo("com.android.provider2", "ClassA");
        ServiceInfo providerService3 = createServiceInfo("com.android.provider3", "ClassA");
        ServiceInfo providerService4 = createServiceInfo("com.android.provider4", "ClassA");
        ServiceInfo providerService5 = createServiceInfo("com.android.provider5", "ClassA");
        ServiceInfo providerService6 = createServiceInfo("com.android.provider6", "ClassA");

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
        assertThat(controller.toggleServiceInfoEnabled(providerService1a)).isTrue();
        assertThat(controller.toggleServiceInfoEnabled(providerService2)).isTrue();
        assertThat(controller.toggleServiceInfoEnabled(providerService3)).isTrue();
        assertThat(controller.toggleServiceInfoEnabled(providerService4)).isTrue();
        assertThat(controller.toggleServiceInfoEnabled(providerService5)).isTrue();
        assertThat(controller.toggleServiceInfoEnabled(providerService6)).isFalse();

        // Check that they are all actually registered.
        Set<ServiceInfo> enabledProviders = controller.getEnabledProviders();
        assertThat(enabledProviders.size()).isEqualTo(5);
        assertThat(enabledProviders.contains(providerService1a)).isTrue();
        assertThat(enabledProviders.contains(providerService2)).isTrue();
        assertThat(enabledProviders.contains(providerService3)).isTrue();
        assertThat(enabledProviders.contains(providerService4)).isTrue();
        assertThat(enabledProviders.contains(providerService5)).isTrue();
        assertThat(enabledProviders.contains(providerService6)).isFalse();

        // Check that the settings string has the right component names.
        List<String> enabledServices = controller.getEnabledSettings();
        assertThat(enabledServices.size()).isEqualTo(5);
        assertThat(enabledServices.contains("com.android.provider1/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider1/ClassB")).isFalse();
        assertThat(enabledServices.contains("com.android.provider2/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider3/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider4/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider5/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider6/ClassA")).isFalse();

        // Toggle the provider disabled.
        controller.toggleServiceInfoDisabled(providerService2);

        // Check that the provider was removed from the list of providers.
        Set<ServiceInfo> currentlyEnabledProviders = controller.getEnabledProviders();
        assertThat(currentlyEnabledProviders.size()).isEqualTo(4);
        assertThat(enabledProviders.contains(providerService1a)).isTrue();
        assertThat(enabledProviders.contains(providerService2)).isFalse();
        assertThat(enabledProviders.contains(providerService3)).isTrue();
        assertThat(enabledProviders.contains(providerService4)).isTrue();
        assertThat(enabledProviders.contains(providerService5)).isTrue();
        assertThat(enabledProviders.contains(providerService6)).isFalse();

        // Check that the provider was removed from the list of services stored in the setting.
        List<String> currentlyEnabledServices = controller.getEnabledSettings();
        assertThat(currentlyEnabledServices.size()).isEqualTo(4);
        assertThat(currentlyEnabledServices.contains("com.android.provider1/ClassA")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider1/ClassB")).isFalse();
        assertThat(currentlyEnabledServices.contains("com.android.provider2/ClassA")).isFalse();
        assertThat(currentlyEnabledServices.contains("com.android.provider3/ClassA")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider4/ClassA")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider5/ClassA")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider6/ClassA")).isFalse();
    }

    @Test
    public void handlesCredentialProviderInfoEnabledDisabled() {
        ServiceInfo providerService1 = createServiceInfo("com.android.provider1", "ClassA");
        ServiceInfo providerService2 = createServiceInfo("com.android.provider2", "ClassA");
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
        Set<ServiceInfo> enabledProviders = controller.getEnabledProviders();
        assertThat(enabledProviders.size()).isEqualTo(1);
        assertThat(enabledProviders.contains(providerInfo1.getServiceInfo())).isFalse();
        assertThat(enabledProviders.contains(providerInfo2.getServiceInfo())).isTrue();

        // Check that the settings string has the right component names.
        List<String> enabledServices = controller.getEnabledSettings();
        assertThat(enabledServices.size()).isEqualTo(1);
        assertThat(enabledServices.contains("com.android.provider1/ClassA")).isFalse();
        assertThat(enabledServices.contains("com.android.provider2/ClassA")).isTrue();
    }

    @Test
    public void handleIntentWithProviderServiceInfo_handleBadIntent_missingData() {
        CredentialProviderInfo cpi = createCredentialProviderInfo();
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(cpi));

        // Create an intent with missing data.
        Intent missingDataIntent = new Intent(Settings.ACTION_CREDENTIAL_PROVIDER);
        assertThat(controller.verifyReceivedIntent(missingDataIntent)).isFalse();
    }

    @Test
    public void handleIntentWithProviderServiceInfo_handleBadIntent_successDialog() {
        CredentialProviderInfo cpi = createCredentialProviderInfo();
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(cpi));
        String packageName = cpi.getServiceInfo().packageName;

        // Create an intent with valid data.
        Intent intent = new Intent(Settings.ACTION_CREDENTIAL_PROVIDER);
        intent.setData(Uri.parse("package:" + packageName));
        assertThat(controller.verifyReceivedIntent(intent)).isTrue();
        controller.completeEnableProviderDialogBox(
                DialogInterface.BUTTON_POSITIVE, cpi.getServiceInfo(), true);
        assertThat(mReceivedResultCode.get()).isEqualTo(Activity.RESULT_OK);
    }

    @Test
    public void handleIntentWithProviderServiceInfo_handleIntent_cancelDialog() {
        CredentialProviderInfo cpi = createCredentialProviderInfo();
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(cpi));
        String packageName = cpi.getServiceInfo().packageName;

        // Create an intent with valid data.
        Intent intent = new Intent(Settings.ACTION_CREDENTIAL_PROVIDER);
        intent.setData(Uri.parse("package:" + packageName));
        assertThat(controller.verifyReceivedIntent(intent)).isTrue();
        controller.completeEnableProviderDialogBox(DialogInterface.BUTTON_NEGATIVE, cpi.getServiceInfo(), true);
        assertThat(mReceivedResultCode.get()).isEqualTo(Activity.RESULT_CANCELED);
    }

    @Test
    public void handleIntentWithProviderServiceInfo_handleIntent_incorrectAction() {
        CredentialProviderInfo cpi = createCredentialProviderInfo();
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(cpi));
        String packageName = cpi.getServiceInfo().packageName;

        // Create an intent with valid data.
        Intent intent = new Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE);
        intent.setData(Uri.parse("package:" + packageName));
        assertThat(controller.verifyReceivedIntent(intent)).isFalse();
        assertThat(mReceivedResultCode.isPresent()).isFalse();
    }

    @Test
    public void handleIntentWithProviderServiceInfo_handleNullIntent() {
        CredentialProviderInfo cpi = createCredentialProviderInfo();
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(cpi));

        // Use a null intent.
        assertThat(controller.verifyReceivedIntent(null)).isFalse();
        assertThat(mReceivedResultCode.isPresent()).isFalse();
    }

    private CredentialManagerPreferenceController createControllerWithServices(
            List<CredentialProviderInfo> availableServices) {
        return createControllerWithServicesAndAddServiceOverride(availableServices, null);
    }

    private CredentialManagerPreferenceController createControllerWithServicesAndAddServiceOverride(
            List<CredentialProviderInfo> availableServices, String addServiceOverride) {
        CredentialManagerPreferenceController controller =
                new CredentialManagerPreferenceController(
                        mContext, mCredentialsPreferenceCategory.getKey());
        controller.setAvailableServices(availableServices, addServiceOverride);
        controller.setDelegate(mDelegate);
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

    private ServiceInfo createServiceInfo(String packageName, String className) {
        ServiceInfo si = new ServiceInfo();
        si.packageName = packageName;
        si.name = className;

        si.applicationInfo = new ApplicationInfo();
        si.applicationInfo.packageName = packageName;
        return si;
    }
}
