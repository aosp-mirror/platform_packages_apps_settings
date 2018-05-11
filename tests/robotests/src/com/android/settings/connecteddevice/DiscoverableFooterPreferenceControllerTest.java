/*
 * Copyright 2018 The Android Open Source Project
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
 * limitations under the License
 */
package com.android.settings.connecteddevice;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter;
import com.android.settings.testutils.shadow.ShadowBluetoothPan;
import com.android.settings.testutils.shadow.ShadowLocalBluetoothAdapter;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.FooterPreferenceMixin;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothPan.class, ShadowBluetoothAdapter.class,
        ShadowLocalBluetoothAdapter.class})
public class DiscoverableFooterPreferenceControllerTest {
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private FooterPreferenceMixin mFooterPreferenceMixin;

    private Context mContext;
    private DiscoverableFooterPreferenceController mDiscoverableFooterPreferenceController;
    private FooterPreference mPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        doReturn(mPackageManager).when(mContext).getPackageManager();
        mDiscoverableFooterPreferenceController =
                new DiscoverableFooterPreferenceController(mContext);
        mPreference = spy(new FooterPreference(mContext));
        mDiscoverableFooterPreferenceController.init(mFooterPreferenceMixin, mPreference);
    }

    @Test
    public void getAvailabilityStatus_noBluetoothFeature_returnUnSupported() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)).thenReturn(false);

        assertThat(mDiscoverableFooterPreferenceController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_BluetoothFeature_returnSupported() {
        when(mPackageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)).thenReturn(true);

        assertThat(mDiscoverableFooterPreferenceController.getAvailabilityStatus()).isEqualTo(
                BasePreferenceController.AVAILABLE);
    }

    @Test
    public void displayPreference() {
        when(mFooterPreferenceMixin.createFooterPreference()).thenReturn(mPreference);
        mDiscoverableFooterPreferenceController.displayPreference(mScreen);

        verify(mPreference).setKey(anyString());
        verify(mScreen).addPreference(mPreference);
    }
}
