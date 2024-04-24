/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.syncacrossdevices;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;

import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SyncAcrossDevicesPreferenceControllerTest {

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock private SyncAcrossDevicesFeatureUpdater mSyncAcrossDevicesFeatureUpdater;
    @Mock private PreferenceManager mPreferenceManager;

    private static final String PREFERENCE_KEY = "preference_key";

    private Context mContext;
    private SyncAcrossDevicesPreferenceController mSyncAcrossDevicesPreferenceController;
    private PreferenceGroup mPreferenceGroup;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        SyncAcrossDevicesFeatureProvider provider =
                FakeFeatureFactory.setupForTest().getSyncAcrossDevicesFeatureProvider();
        doReturn(mSyncAcrossDevicesFeatureUpdater)
                .when(provider)
                .getSyncAcrossDevicesFeatureUpdater(any(), any());

        mSyncAcrossDevicesPreferenceController =
                new SyncAcrossDevicesPreferenceController(mContext, PREFERENCE_KEY);

        mPreferenceGroup = spy(new PreferenceCategory(mContext));
        doReturn(mPreferenceManager).when(mPreferenceGroup).getPreferenceManager();
        mPreferenceGroup.setVisible(false);

        mPreferenceGroup.setKey(mSyncAcrossDevicesPreferenceController.getPreferenceKey());
        mSyncAcrossDevicesPreferenceController.setPreferenceGroup(mPreferenceGroup);
    }

    @Test
    public void testGetAvailabilityStatus_noFeatureUpdater_returnUnSupported() {
        SyncAcrossDevicesFeatureProvider provider =
                FakeFeatureFactory.setupForTest().getSyncAcrossDevicesFeatureProvider();
        doReturn(null).when(provider).getSyncAcrossDevicesFeatureUpdater(any(), any());

        SyncAcrossDevicesPreferenceController syncAcrossDevicesPreferenceController =
                new SyncAcrossDevicesPreferenceController(mContext, PREFERENCE_KEY);

        assertThat(syncAcrossDevicesPreferenceController.getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void testGetAvailabilityStatus_withFeatureUpdater_returnSupported() {
        assertThat(mSyncAcrossDevicesPreferenceController.getAvailabilityStatus())
                .isEqualTo(AVAILABLE);
    }

    @Test
    public void testUpdatePreferenceVisibility_addFeaturePreference_shouldShowPreference() {
        Preference preference = new Preference(mContext);

        mSyncAcrossDevicesPreferenceController.onFeatureAdded(preference);

        assertThat(mPreferenceGroup.isVisible()).isTrue();
    }

    @Test
    public void testUpdatePreferenceVisibility_removeFeaturePreference_shouldHidePreference() {
        Preference preference = new Preference(mContext);

        mSyncAcrossDevicesPreferenceController.onFeatureAdded(preference);
        mSyncAcrossDevicesPreferenceController.onFeatureRemoved(preference);

        assertThat(mPreferenceGroup.isVisible()).isFalse();
    }

    @Test
    public void testDisplayPreference_availabilityStatusIsAvailable_shouldForceUpdated() {
        PreferenceManager preferenceManager = new PreferenceManager(mContext);
        PreferenceScreen preferenceScreen = preferenceManager.createPreferenceScreen(mContext);
        preferenceScreen.addPreference(mPreferenceGroup);

        mSyncAcrossDevicesPreferenceController.displayPreference(preferenceScreen);

        verify(mSyncAcrossDevicesFeatureUpdater, times(1)).setPreferenceContext(any());
        verify(mSyncAcrossDevicesFeatureUpdater, times(1)).forceUpdate();
    }
}
