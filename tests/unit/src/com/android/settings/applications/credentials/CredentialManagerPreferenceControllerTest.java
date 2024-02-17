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
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.credentials.CredentialProviderInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Looper;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.android.settings.tests.unit.R;

import com.google.android.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class CredentialManagerPreferenceControllerTest {

    private Context mContext;
    private PreferenceScreen mScreen;
    private PreferenceCategory mCredentialsPreferenceCategory;
    private CredentialManagerPreferenceController.Delegate mDelegate;

    private static final String TEST_PACKAGE_NAME_A = "com.android.providerA";
    private static final String TEST_PACKAGE_NAME_B = "com.android.providerB";
    private static final String TEST_PACKAGE_NAME_C = "com.android.providerC";
    private static final String TEST_TITLE_APP_A = "test app A";
    private static final String TEST_TITLE_APP_B = "test app B";
    private static final String TEST_TITLE_APP_C = "test app C1";
    private static final String PRIMARY_INTENT = "android.settings.CREDENTIAL_PROVIDER";
    private static final String ALTERNATE_INTENT = "android.settings.SYNC_SETTINGS";

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
        mDelegate =
                new CredentialManagerPreferenceController.Delegate() {
                    public void setActivityResult(int resultCode) {}

                    public void forceDelegateRefresh() {}
                };
    }

    @Test
    // Tests that getAvailabilityStatus() does not throw an exception if it's called before the
    // Controller is initialized (this can happen during indexing).
    public void getAvailabilityStatus_withoutInit_returnsUnavailable() {
        if (Looper.myLooper() == null) {
            Looper.prepare(); // needed to create the preference screen
        }

        CredentialManagerPreferenceController controller =
                new CredentialManagerPreferenceController(
                        mContext, mCredentialsPreferenceCategory.getKey());
        assertThat(controller.isConnected()).isFalse();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_noServices_returnsUnavailable() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Collections.emptyList());
        assertThat(controller.isConnected()).isFalse();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_withServices_returnsAvailable() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(createCredentialProviderInfo()));
        controller.setSimulateConnectedForTests(true);
        assertThat(controller.isConnected()).isTrue();
        controller.setSimulateHiddenForTests(Optional.of(false));
        assertThat(controller.isHiddenDueToNoProviderSet()).isFalse();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_isHidden_returnsConditionallyUnavailable() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(createCredentialProviderInfo()));
        controller.setSimulateConnectedForTests(true);
        assertThat(controller.isConnected()).isTrue();
        controller.setSimulateHiddenForTests(Optional.of(true));
        assertThat(controller.isHiddenDueToNoProviderSet()).isTrue();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void displayPreference_withServices_preferencesAdded() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(createCredentialProviderInfo()));
        controller.setSimulateConnectedForTests(true);
        controller.setSimulateHiddenForTests(Optional.of(false));

        assertThat(controller.isHiddenDueToNoProviderSet()).isFalse();
        assertThat(controller.isConnected()).isTrue();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);

        controller.displayPreference(mScreen);
        assertThat(mCredentialsPreferenceCategory.getPreferenceCount()).isEqualTo(1);

        Preference pref = mCredentialsPreferenceCategory.getPreference(0);
        assertThat(pref.getTitle()).isNotEqualTo("Add account");
    }

    @Test
    public void buildPreference() {
        CredentialProviderInfo providerInfo1 =
                createCredentialProviderInfoWithSubtitle(
                        "com.android.provider1", "ClassA", "Service Title", null);
        CredentialProviderInfo providerInfo2 =
                createCredentialProviderInfoWithSubtitle(
                        "com.android.provider2", "ClassA", "Service Title", "Summary Text");
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(providerInfo1, providerInfo2));
        controller.setSimulateConnectedForTests(true);
        assertThat(controller.isConnected()).isTrue();
        controller.setSimulateHiddenForTests(Optional.of(false));
        assertThat(controller.isHiddenDueToNoProviderSet()).isFalse();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);

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
        CredentialManagerPreferenceController.CombiPreference pref =
                controller.createPreference(mContext, providerInfo1);
        assertThat(pref.getTitle().toString()).isEqualTo("Service Title");
        assertThat(pref.isChecked()).isTrue();
        assertThat(pref.getSummary()).isNull();

        // Create the pref (not checked).
        CredentialManagerPreferenceController.CombiPreference pref2 =
                controller.createPreference(mContext, providerInfo2);
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
        controller.setSimulateConnectedForTests(true);
        assertThat(controller.isConnected()).isTrue();
        controller.setSimulateHiddenForTests(Optional.of(false));
        assertThat(controller.isHiddenDueToNoProviderSet()).isFalse();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);

        // Ensure that we stay under 5 providers (one is reserved for primary).
        assertThat(controller.togglePackageNameEnabled("com.android.provider1")).isTrue();
        assertThat(controller.togglePackageNameEnabled("com.android.provider2")).isTrue();
        assertThat(controller.togglePackageNameEnabled("com.android.provider3")).isTrue();
        assertThat(controller.togglePackageNameEnabled("com.android.provider4")).isTrue();
        assertThat(controller.togglePackageNameEnabled("com.android.provider5")).isFalse();
        assertThat(controller.togglePackageNameEnabled("com.android.provider6")).isFalse();

        // Check that they are all actually registered.
        Set<String> enabledProviders = controller.getEnabledProviders();
        assertThat(enabledProviders.size()).isEqualTo(4);
        assertThat(enabledProviders.contains("com.android.provider1")).isTrue();
        assertThat(enabledProviders.contains("com.android.provider2")).isTrue();
        assertThat(enabledProviders.contains("com.android.provider3")).isTrue();
        assertThat(enabledProviders.contains("com.android.provider4")).isTrue();
        assertThat(enabledProviders.contains("com.android.provider5")).isFalse();
        assertThat(enabledProviders.contains("com.android.provider6")).isFalse();

        // Check that the settings string has the right component names.
        List<String> enabledServices = controller.getEnabledSettings();
        assertThat(enabledServices.size()).isEqualTo(5);
        assertThat(enabledServices.contains("com.android.provider1/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider1/ClassB")).isTrue();
        assertThat(enabledServices.contains("com.android.provider2/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider3/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider4/ClassA")).isTrue();
        assertThat(enabledServices.contains("com.android.provider5/ClassA")).isFalse();
        assertThat(enabledServices.contains("com.android.provider6/ClassA")).isFalse();

        // Toggle the provider disabled.
        controller.togglePackageNameDisabled("com.android.provider2");

        // Check that the provider was removed from the list of providers.
        Set<String> currentlyEnabledProviders = controller.getEnabledProviders();
        assertThat(currentlyEnabledProviders.size()).isEqualTo(3);
        assertThat(currentlyEnabledProviders.contains("com.android.provider1")).isTrue();
        assertThat(currentlyEnabledProviders.contains("com.android.provider2")).isFalse();
        assertThat(currentlyEnabledProviders.contains("com.android.provider3")).isTrue();
        assertThat(currentlyEnabledProviders.contains("com.android.provider4")).isTrue();
        assertThat(currentlyEnabledProviders.contains("com.android.provider5")).isFalse();
        assertThat(currentlyEnabledProviders.contains("com.android.provider6")).isFalse();

        // Check that the provider was removed from the list of services stored in the setting.
        List<String> currentlyEnabledServices = controller.getEnabledSettings();
        assertThat(currentlyEnabledServices.size()).isEqualTo(4);
        assertThat(currentlyEnabledServices.contains("com.android.provider1/ClassA")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider1/ClassB")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider3/ClassA")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider4/ClassA")).isTrue();
        assertThat(currentlyEnabledServices.contains("com.android.provider5/ClassA")).isFalse();
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
        controller.setSimulateConnectedForTests(true);
        assertThat(controller.isConnected()).isTrue();
        controller.setSimulateHiddenForTests(Optional.of(false));
        assertThat(controller.isHiddenDueToNoProviderSet()).isFalse();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);

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
                        TEST_PACKAGE_NAME_C, "CredManProviderC1", "test app C1", TEST_TITLE_APP_C);
        CredentialProviderInfo serviceC2 =
                createCredentialProviderInfoWithAppLabel(
                        TEST_PACKAGE_NAME_C, "CredManProviderC2", "test app C2", TEST_TITLE_APP_C);
        CredentialProviderInfo serviceC3 =
                createCredentialProviderInfoBuilder(
                                TEST_PACKAGE_NAME_C,
                                "CredManProviderC3",
                                "test app C3",
                                TEST_TITLE_APP_C)
                        .setEnabled(true)
                        .build();

        CredentialManagerPreferenceController controller =
                createControllerWithServices(
                        Lists.newArrayList(serviceA1, serviceB1, serviceC1, serviceC2, serviceC3));
        controller.setSimulateConnectedForTests(true);
        controller.setSimulateHiddenForTests(Optional.of(false));

        assertThat(controller.isHiddenDueToNoProviderSet()).isFalse();
        assertThat(controller.isConnected()).isTrue();
        assertThat(controller.getAvailabilityStatus()).isEqualTo(AVAILABLE);

        controller.displayPreference(mScreen);
        assertThat(mCredentialsPreferenceCategory.getPreferenceCount()).isEqualTo(3);

        Map<String, CredentialManagerPreferenceController.CombiPreference> prefs =
                controller.buildPreferenceList(mContext, mCredentialsPreferenceCategory);
        assertThat(prefs.keySet())
                .containsExactly(TEST_PACKAGE_NAME_A, TEST_PACKAGE_NAME_B, TEST_PACKAGE_NAME_C);
        assertThat(prefs.size()).isEqualTo(3);
        assertThat(prefs.containsKey(TEST_PACKAGE_NAME_A)).isTrue();
        assertThat(prefs.get(TEST_PACKAGE_NAME_A).getTitle()).isEqualTo(TEST_TITLE_APP_A);
        assertThat(prefs.get(TEST_PACKAGE_NAME_A).isChecked()).isFalse();
        assertThat(prefs.containsKey(TEST_PACKAGE_NAME_B)).isTrue();
        assertThat(prefs.get(TEST_PACKAGE_NAME_B).getTitle()).isEqualTo(TEST_TITLE_APP_B);
        assertThat(prefs.get(TEST_PACKAGE_NAME_B).isChecked()).isFalse();
        assertThat(prefs.containsKey(TEST_PACKAGE_NAME_C)).isTrue();
        assertThat(prefs.get(TEST_PACKAGE_NAME_C).getTitle()).isEqualTo(TEST_TITLE_APP_C);
        assertThat(prefs.get(TEST_PACKAGE_NAME_C).isChecked()).isTrue();
    }

    @Test
    public void handleIntentWithProviderServiceInfo_handleBadIntent_missingData() {
        CredentialProviderInfo cpi = createCredentialProviderInfo();
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(cpi));

        // Create an intent with missing data.
        Intent missingDataIntent = new Intent(PRIMARY_INTENT);
        assertThat(controller.verifyReceivedIntent(missingDataIntent)).isFalse();
    }

    @Test
    public void handleIntentWithProviderServiceInfo_handleBadIntent_successDialog() {
        CredentialProviderInfo cpi = createCredentialProviderInfo();
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(cpi));
        String packageName = cpi.getServiceInfo().packageName;

        // Create an intent with valid data.
        Intent intent = new Intent(PRIMARY_INTENT);
        intent.setData(Uri.parse("package:" + packageName));
        assertThat(controller.verifyReceivedIntent(intent)).isTrue();
        int resultCode =
                controller.completeEnableProviderDialogBox(
                        DialogInterface.BUTTON_POSITIVE, packageName, true);
        assertThat(resultCode).isEqualTo(Activity.RESULT_OK);
    }

    @Test
    public void handleIntentWithProviderServiceInfo_handleIntent_cancelDialog() {
        CredentialProviderInfo cpi = createCredentialProviderInfo();
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(cpi));
        String packageName = cpi.getServiceInfo().packageName;

        // Create an intent with valid data.
        Intent intent = new Intent(PRIMARY_INTENT);
        intent.setData(Uri.parse("package:" + packageName));
        assertThat(controller.verifyReceivedIntent(intent)).isTrue();
        int resultCode =
                controller.completeEnableProviderDialogBox(
                        DialogInterface.BUTTON_NEGATIVE, packageName, true);
        assertThat(resultCode).isEqualTo(Activity.RESULT_CANCELED);
    }

    @Test
    public void handleOtherIntentWithProviderServiceInfo_handleBadIntent_missingData() {
        CredentialProviderInfo cpi = createCredentialProviderInfo();
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(cpi));

        // Create an intent with missing data.
        Intent missingDataIntent = new Intent(ALTERNATE_INTENT);
        assertThat(controller.verifyReceivedIntent(missingDataIntent)).isFalse();
    }

    @Test
    public void handleOtherIntentWithProviderServiceInfo_handleBadIntent_successDialog() {
        CredentialProviderInfo cpi = createCredentialProviderInfo();
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(cpi));
        String packageName = cpi.getServiceInfo().packageName;

        // Create an intent with valid data.
        Intent intent = new Intent(ALTERNATE_INTENT);
        intent.setData(Uri.parse("package:" + packageName));
        assertThat(controller.verifyReceivedIntent(intent)).isTrue();
        int resultCode =
                controller.completeEnableProviderDialogBox(
                        DialogInterface.BUTTON_POSITIVE, packageName, true);
        assertThat(resultCode).isEqualTo(Activity.RESULT_OK);
    }

    @Test
    public void handleOtherIntentWithProviderServiceInfo_handleIntent_cancelDialog() {
        CredentialProviderInfo cpi = createCredentialProviderInfo();
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(cpi));
        String packageName = cpi.getServiceInfo().packageName;

        // Create an intent with valid data.
        Intent intent = new Intent(ALTERNATE_INTENT);
        intent.setData(Uri.parse("package:" + packageName));
        assertThat(controller.verifyReceivedIntent(intent)).isTrue();
        int resultCode =
                controller.completeEnableProviderDialogBox(
                        DialogInterface.BUTTON_NEGATIVE, packageName, true);
        assertThat(resultCode).isEqualTo(Activity.RESULT_CANCELED);
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
    }

    @Test
    public void handleIntentWithProviderServiceInfo_handleNullIntent() {
        CredentialProviderInfo cpi = createCredentialProviderInfo();
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(cpi));

        // Use a null intent.
        assertThat(controller.verifyReceivedIntent(null)).isFalse();
    }

    @Test
    public void testIconResizer_resizeLargeImage() throws Throwable {
        CredentialProviderInfo cpi = createCredentialProviderInfo();
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(cpi));

        final Drawable d =
                InstrumentationRegistry.getInstrumentation()
                        .getContext()
                        .getResources()
                        .getDrawable(R.drawable.credman_icon_32_32);
        assertThat(d).isNotNull();
        assertThat(d.getIntrinsicHeight() >= 0).isTrue();
        assertThat(d.getIntrinsicWidth() >= 0).isTrue();

        Drawable thumbnail = controller.processIcon(d);
        assertThat(thumbnail).isNotNull();
        assertThat(thumbnail.getIntrinsicHeight()).isEqualTo(getIconSize());
        assertThat(thumbnail.getIntrinsicWidth()).isEqualTo(getIconSize());
    }

    @Test
    public void testIconResizer_resizeNullImage() throws Throwable {
        CredentialProviderInfo cpi = createCredentialProviderInfo();
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(cpi));

        Drawable thumbnail = controller.processIcon(null);
        assertThat(thumbnail).isNotNull();
        assertThat(thumbnail.getIntrinsicHeight()).isEqualTo(getIconSize());
        assertThat(thumbnail.getIntrinsicWidth()).isEqualTo(getIconSize());
    }

    @Test
    public void testIconResizer_resizeSmallImage() throws Throwable {
        CredentialProviderInfo cpi = createCredentialProviderInfo();
        CredentialManagerPreferenceController controller =
                createControllerWithServices(Lists.newArrayList(cpi));

        final Drawable d =
                InstrumentationRegistry.getInstrumentation()
                        .getContext()
                        .getResources()
                        .getDrawable(R.drawable.credman_icon_1_1);
        assertThat(d).isNotNull();
        assertThat(d.getIntrinsicHeight() >= 0).isTrue();
        assertThat(d.getIntrinsicWidth() >= 0).isTrue();

        Drawable thumbnail = controller.processIcon(null);
        assertThat(thumbnail).isNotNull();
        assertThat(thumbnail.getIntrinsicHeight()).isEqualTo(getIconSize());
        assertThat(thumbnail.getIntrinsicWidth()).isEqualTo(getIconSize());
    }

    @Test
    public void hasNonPrimaryServices_allServicesArePrimary() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(
                    Lists.newArrayList(createCredentialProviderPrimary()));
        assertThat(controller.hasNonPrimaryServices()).isFalse();
    }

    @Test
    public void hasNonPrimaryServices_mixtureOfServices() {
        CredentialManagerPreferenceController controller =
                createControllerWithServices(
                    Lists.newArrayList(createCredentialProviderInfo(),
                        createCredentialProviderPrimary()));
        assertThat(controller.hasNonPrimaryServices()).isTrue();
    }

    @Test
    public void testProviderLimitReached() {
        // The limit is 5 with one slot reserved for primary.
        assertThat(CredentialManagerPreferenceController.hasProviderLimitBeenReached(0)).isFalse();
        assertThat(CredentialManagerPreferenceController.hasProviderLimitBeenReached(1)).isFalse();
        assertThat(CredentialManagerPreferenceController.hasProviderLimitBeenReached(2)).isFalse();
        assertThat(CredentialManagerPreferenceController.hasProviderLimitBeenReached(3)).isFalse();
        assertThat(CredentialManagerPreferenceController.hasProviderLimitBeenReached(4)).isTrue();
        assertThat(CredentialManagerPreferenceController.hasProviderLimitBeenReached(5)).isTrue();
    }

    private int getIconSize() {
        final Resources resources = mContext.getResources();
        return (int) resources.getDimension(android.R.dimen.app_icon_size);
    }

    private CredentialManagerPreferenceController createControllerWithServices(
            List<CredentialProviderInfo> availableServices) {
        return createControllerWithServicesAndAddServiceOverride(availableServices, null);
    }

    private CredentialManagerPreferenceController createControllerWithServicesAndAddServiceOverride(
            List<CredentialProviderInfo> availableServices, String addServiceOverride) {
        if (Looper.myLooper() == null) {
            Looper.prepare(); // needed to create the preference screen
        }

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

    private CredentialProviderInfo createCredentialProviderInfoWithIsEnabled(
            String packageName, String className, CharSequence serviceLabel, boolean isEnabled) {
        return createCredentialProviderInfoBuilder(packageName, className, serviceLabel, "App Name")
                .setEnabled(isEnabled)
                .build();
    }

    private CredentialProviderInfo createCredentialProviderPrimary() {
        return createCredentialProviderInfoBuilder(
            "com.android.primary", "CredManProvider", "Service Label", "App Name")
                .setPrimary(true)
                .build();
    }

    private CredentialProviderInfo createCredentialProviderInfoWithSubtitle(
            String packageName, String className, CharSequence label, CharSequence subtitle) {
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
