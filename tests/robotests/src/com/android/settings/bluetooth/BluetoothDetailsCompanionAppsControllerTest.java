/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.companion.AssociationInfo;
import android.companion.CompanionDeviceManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.MacAddress;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Ignore("b/191992001")
@RunWith(RobolectricTestRunner.class)
public class BluetoothDetailsCompanionAppsControllerTest extends
        BluetoothDetailsControllerTestBase {
    private static final String PACKAGE_NAME_ONE = "com.google.android.deskclock";
    private static final String PACKAGE_NAME_TWO = "com.google.android.calculator";
    private static final String PACKAGE_NAME_THREE = "com.google.android.GoogleCamera";
    private static final CharSequence APP_NAME_ONE = "deskclock";
    private static final CharSequence APP_NAME_TWO = "calculator";
    private static final CharSequence APP_NAME_THREE = "GoogleCamera";

    @Mock
    private CompanionDeviceManager mCompanionDeviceManager;
    @Mock
    private PackageManager mPackageManager;

    private BluetoothDetailsCompanionAppsController mController;
    private PreferenceCategory mProfiles;
    private List<String> mPackages;
    private List<CharSequence> mAppNames;
    private List<AssociationInfo> mAssociations;


    @Override
    public void setUp() {
        super.setUp();
        mPackages = Arrays.asList(PACKAGE_NAME_ONE, PACKAGE_NAME_TWO, PACKAGE_NAME_THREE);
        mAppNames = Arrays.asList(APP_NAME_ONE, APP_NAME_TWO, APP_NAME_THREE);
        mProfiles = spy(new PreferenceCategory(mContext));
        mAssociations = new ArrayList<>();
        when(mCompanionDeviceManager.getAllAssociations()).thenReturn(mAssociations);
        when(mProfiles.getPreferenceManager()).thenReturn(mPreferenceManager);
        setupDevice(mDeviceConfig);
        mController =
                new BluetoothDetailsCompanionAppsController(mContext, mFragment, mCachedDevice,
                        mLifecycle);
        mController.mCompanionDeviceManager = mCompanionDeviceManager;
        mController.mPackageManager = mPackageManager;
        mController.mProfilesContainer = mProfiles;
        mProfiles.setKey(mController.getPreferenceKey());
        mScreen.addPreference(mProfiles);
    }

    private void setupFakeLabelAndInfo(String packageName, CharSequence appName) {
        ApplicationInfo appInfo = mock(ApplicationInfo.class);
        try {
            when(mPackageManager.getApplicationInfo(packageName, 0)).thenReturn(appInfo);
            when(mPackageManager.getApplicationLabel(appInfo)).thenReturn(appName);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void addFakeAssociation(String packageName, CharSequence appName) {
        setupFakeLabelAndInfo(packageName, appName);

        final int associationId = mAssociations.size() + 1;
        final AssociationInfo association = new AssociationInfo(
                associationId,
                /* userId */ 0,
                packageName,
                /* tag */ null,
                MacAddress.fromString(mCachedDevice.getAddress()),
                /* displayName */ null,
                /* deviceProfile */ "",
                /* associatedDevice */ null,
                /* selfManaged */ false,
                /* notifyOnDeviceNearby */ true,
                /* revoked */ false,
                /* timeApprovedMs */ System.currentTimeMillis(),
                /* lastTimeConnected */ Long.MAX_VALUE,
                /* systemDataSyncFlags */ -1);

        mAssociations.add(association);
        showScreen(mController);
    }

    private Preference getPreference(int index) {
        PreferenceCategory preferenceCategory = mProfiles.findPreference(
                mController.getPreferenceKey());
        return Objects.requireNonNull(preferenceCategory).getPreference(index);
    }

    private void removeAssociation(String packageName) {
        mAssociations = mAssociations.stream()
                .filter(a -> !a.getPackageName().equals(packageName))
                .collect(Collectors.toList());

        when(mCompanionDeviceManager.getAllAssociations()).thenReturn(mAssociations);

        showScreen(mController);
    }

    @Test
    public void addOneAssociation_preferenceShouldBeAdded() {
        addFakeAssociation(PACKAGE_NAME_ONE, APP_NAME_ONE);

        Preference preferenceOne = getPreference(0);

        assertThat(preferenceOne.getClass()).isEqualTo(CompanionAppWidgetPreference.class);
        assertThat(preferenceOne.getKey()).isEqualTo(PACKAGE_NAME_ONE);
        assertThat(preferenceOne.getTitle()).isEqualTo(APP_NAME_ONE.toString());
        assertThat(mProfiles.getPreferenceCount()).isEqualTo(1);

        removeAssociation(PACKAGE_NAME_ONE);

        assertThat(mProfiles.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void removeOneAssociation_preferenceShouldBeRemoved() {
        addFakeAssociation(PACKAGE_NAME_ONE, APP_NAME_ONE);

        removeAssociation(PACKAGE_NAME_ONE);

        assertThat(mProfiles.getPreferenceCount()).isEqualTo(0);
    }

    @Test
    public void addMultipleAssociations_preferencesShouldBeAdded() {
        for (int i = 0; i < mPackages.size(); i++) {
            addFakeAssociation(mPackages.get(i), mAppNames.get(i));

            Preference preference = getPreference(i);

            assertThat(preference.getClass()).isEqualTo(CompanionAppWidgetPreference.class);
            assertThat(preference.getKey()).isEqualTo(mPackages.get(i));
            assertThat(preference.getTitle()).isEqualTo(mAppNames.get(i).toString());
            assertThat(mProfiles.getPreferenceCount()).isEqualTo(i + 1);
        }
    }

    @Test
    public void removeMultipleAssociations_preferencesShouldBeRemoved() {
        for (int i = 0; i < mPackages.size(); i++) {
            addFakeAssociation(mPackages.get(i), mAppNames.get(i).toString());
        }

        for (int i = 0; i < mPackages.size(); i++) {
            removeAssociation(mPackages.get(i));

            assertThat(mProfiles.getPreferenceCount()).isEqualTo(mPackages.size() - i - 1);

            if (i == mPackages.size() - 1) {
                break;
            }

            assertThat(getPreference(0).getKey()).isEqualTo(mPackages.get(i + 1));
            assertThat(getPreference(0).getTitle()).isEqualTo(mAppNames.get(i + 1).toString());
        }
    }
}
