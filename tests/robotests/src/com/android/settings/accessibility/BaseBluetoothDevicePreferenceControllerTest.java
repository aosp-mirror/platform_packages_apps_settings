/*
 * Copyright (C) 2023 The Android Open Source Project
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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.bluetooth.Utils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowBluetoothUtils;
import com.android.settingslib.bluetooth.LocalBluetoothManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link BaseBluetoothDevicePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothUtils.class})
public class BaseBluetoothDevicePreferenceControllerTest {
    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    private static final String FAKE_KEY = "fake_key";
    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Mock
    private LocalBluetoothManager mLocalBluetoothManager;
    @Mock
    private PackageManager mPackageManager;
    private PreferenceCategory mPreferenceCategory;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mScreen;
    private TestBaseBluetoothDevicePreferenceController mController;

    @Before
    public void setUp() {
        FakeFeatureFactory.setupForTest();
        ShadowBluetoothUtils.sLocalBluetoothManager = mLocalBluetoothManager;
        mPreferenceCategory = new PreferenceCategory(mContext);
        mPreferenceCategory.setKey(FAKE_KEY);
        mLocalBluetoothManager = Utils.getLocalBtManager(mContext);
        mPreferenceManager = new PreferenceManager(mContext);
        mScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mScreen.addPreference(mPreferenceCategory);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        mController = new TestBaseBluetoothDevicePreferenceController(mContext, FAKE_KEY);
    }

    @Test
    public void getAvailabilityStatus_hasBluetoothFeature_available() {
        doReturn(true).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void getAvailabilityStatus_noBluetoothFeature_conditionallyUnavailalbe() {
        doReturn(false).when(mPackageManager).hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void displayPreference_preferenceCategoryInVisible() {
        mController.displayPreference(mScreen);

        assertThat(mPreferenceCategory.isVisible()).isFalse();
    }

    @Test
    public void onDeviceAdded_preferenceCategoryVisible() {
        Preference preference = new Preference(mContext);
        mController.displayPreference(mScreen);

        mController.onDeviceAdded(preference);

        assertThat(mPreferenceCategory.isVisible()).isTrue();
    }

    @Test
    public void onDeviceRemoved_addedPreferenceFirst_preferenceCategoryInVisible() {
        Preference preference = new Preference(mContext);
        mController.displayPreference(mScreen);

        mController.onDeviceAdded(preference);
        mController.onDeviceRemoved(preference);

        assertThat(mPreferenceCategory.isVisible()).isFalse();
    }

    public static class TestBaseBluetoothDevicePreferenceController extends
            BaseBluetoothDevicePreferenceController {

        public TestBaseBluetoothDevicePreferenceController(Context context,
                String preferenceKey) {
            super(context, preferenceKey);
        }
    }
}
