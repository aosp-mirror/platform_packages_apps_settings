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
package com.android.settings.connecteddevice.virtual;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.companion.AssociationInfo;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.search.SearchIndexableRaw;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class VirtualDeviceListControllerTest {

    private static final String PREFERENCE_KEY = "virtual_device_list";

    private static final CharSequence DEVICE_NAME = "Device Name";
    private static final int DEVICE_ID = 42;
    private static final String PERSISTENT_DEVICE_ID = "PersistentDeviceId";
    private static final String DEVICE_PREFERENCE_KEY = PERSISTENT_DEVICE_ID + "_" + DEVICE_NAME;

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    PreferenceManager mPreferenceManager;
    @Mock
    AssociationInfo mAssociationInfo;
    @Mock
    PreferenceScreen mScreen;

    private VirtualDeviceWrapper mDevice;
    private VirtualDeviceListController mController;
    private PreferenceGroup mPreferenceGroup;
    private Context mContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(ApplicationProvider.getApplicationContext());
        mPreferenceGroup = spy(new PreferenceScreen(mContext, null));
        when(mPreferenceManager.getSharedPreferences()).thenReturn(mock(SharedPreferences.class));
        when(mPreferenceGroup.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mScreen.findPreference(PREFERENCE_KEY)).thenReturn(mPreferenceGroup);
        when(mScreen.getContext()).thenReturn(mContext);
        mController = new VirtualDeviceListController(mContext, PREFERENCE_KEY);
        DashboardFragment fragment = mock(DashboardFragment.class);
        when(fragment.getContext()).thenReturn(mContext);
        mController.setFragment(fragment);

        when(mAssociationInfo.getDisplayName()).thenReturn(DEVICE_NAME);
        mDevice = new VirtualDeviceWrapper(mAssociationInfo, PERSISTENT_DEVICE_ID, DEVICE_ID);
    }

    @Test
    public void getAvailabilityStatus_vdmDisabled() {
        Resources resources = spy(mContext.getResources());
        when(mContext.getResources()).thenReturn(resources);
        when(resources.getBoolean(com.android.internal.R.bool.config_enableVirtualDeviceManager))
                .thenReturn(false);

        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @DisableFlags(android.companion.virtualdevice.flags.Flags.FLAG_VDM_SETTINGS)
    public void getAvailabilityStatus_flagDisabled() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    @EnableFlags(android.companion.virtualdevice.flags.Flags.FLAG_VDM_SETTINGS)
    public void getAvailabilityStatus_available() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    public void onDeviceAdded_createPreference() {
        mController.displayPreference(mScreen);
        mController.onDeviceAdded(mDevice);
        ShadowLooper.idleMainLooper();

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);
        Preference preference = mPreferenceGroup.findPreference(DEVICE_PREFERENCE_KEY);
        assertThat(preference).isNotNull();
        assertThat(preference.getTitle().toString()).isEqualTo(DEVICE_NAME.toString());
        assertThat(Shadows.shadowOf(preference.getIcon()).getCreatedFromResId())
                .isEqualTo(R.drawable.ic_devices_other);
        assertThat(preference.getSummary().toString())
                .isEqualTo(mContext.getString(R.string.virtual_device_connected));

        assertThat(preference).isEqualTo(mController.mPreferences.get(PERSISTENT_DEVICE_ID));
    }

    @Test
    public void onDeviceChanged_updateSummary() {
        mController.displayPreference(mScreen);
        mController.onDeviceAdded(mDevice);
        ShadowLooper.idleMainLooper();

        mDevice.setDeviceId(Context.DEVICE_ID_INVALID);
        mController.onDeviceChanged(mDevice);
        ShadowLooper.idleMainLooper();

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(1);
        Preference preference = mPreferenceGroup.findPreference(DEVICE_PREFERENCE_KEY);
        assertThat(preference).isNotNull();
        assertThat(preference.getSummary().toString())
                .isEqualTo(mContext.getString(R.string.virtual_device_disconnected));

        assertThat(preference).isEqualTo(mController.mPreferences.get(PERSISTENT_DEVICE_ID));
    }

    @Test
    public void onDeviceRemoved_removePreference() {
        mController.displayPreference(mScreen);
        mController.onDeviceAdded(mDevice);
        ShadowLooper.idleMainLooper();

        mDevice.setDeviceId(Context.DEVICE_ID_INVALID);
        mController.onDeviceRemoved(mDevice);
        ShadowLooper.idleMainLooper();

        assertThat(mPreferenceGroup.getPreferenceCount()).isEqualTo(0);
        assertThat(mController.mPreferences).isEmpty();
    }

    @Test
    public void updateDynamicRawDataToIndex_available() {
        assumeTrue(mController.isAvailable());

        mController.mVirtualDeviceUpdater = mock(VirtualDeviceUpdater.class);
        when(mController.mVirtualDeviceUpdater.loadDevices()).thenReturn(List.of(mDevice));

        ArrayList<SearchIndexableRaw> searchData = new ArrayList<>();
        mController.updateDynamicRawDataToIndex(searchData);

        assertThat(searchData).hasSize(1);
        SearchIndexableRaw data = searchData.getFirst();
        assertThat(data.key).isEqualTo(DEVICE_PREFERENCE_KEY);
        assertThat(data.title).isEqualTo(DEVICE_NAME.toString());
        assertThat(data.summaryOn)
                .isEqualTo(mContext.getString(R.string.connected_device_connections_title));
    }
}
