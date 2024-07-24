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

package com.android.settings.bluetooth;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.companion.CompanionDeviceManager;
import android.companion.datatransfer.PermissionSyncRequest;

import androidx.preference.PreferenceCategory;
import androidx.preference.TwoStatePreference;

import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;

@RunWith(RobolectricTestRunner.class)
public class BluetoothDetailsDataSyncControllerTest extends BluetoothDetailsControllerTestBase {

    private static final String MAC_ADDRESS = "AA:BB:CC:DD:EE:FF";
    private static final int DUMMY_ASSOCIATION_ID = -1;
    private static final int ASSOCIATION_ID = 1;
    private static final String KEY_PERM_SYNC = "perm_sync";

    private BluetoothDetailsDataSyncController mController;
    @Mock
    private Lifecycle mLifecycle;
    @Mock
    private PreferenceCategory mPreferenceCategory;
    @Mock
    private CompanionDeviceManager mCompanionDeviceManager;

    private PermissionSyncRequest mPermissionSyncRequest;
    private TwoStatePreference mPermSyncPreference;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getSystemService(CompanionDeviceManager.class)).thenReturn(
                mCompanionDeviceManager);
        when(mCachedDevice.getAddress()).thenReturn(MAC_ADDRESS);
        when(mCompanionDeviceManager.getAllAssociations()).thenReturn(Collections.emptyList());
        mPermissionSyncRequest = new PermissionSyncRequest(ASSOCIATION_ID);
        when(mCompanionDeviceManager.getPermissionSyncRequest(ASSOCIATION_ID)).thenReturn(
                mPermissionSyncRequest);

        mController = new BluetoothDetailsDataSyncController(mContext, mFragment,
                mCachedDevice, mLifecycle);
        mController.mAssociationId = ASSOCIATION_ID;
        mController.mPreferenceCategory = mPreferenceCategory;

        mPermSyncPreference = mController.createPermSyncPreference(mContext);
        when(mPreferenceCategory.findPreference(KEY_PERM_SYNC)).thenReturn(mPermSyncPreference);
    }

    @Test
    public void isAvailable_noAssociations_returnsFalse() {
        mController.mAssociationId = DUMMY_ASSOCIATION_ID;
        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void isAvailable_hasAssociations_returnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void refresh_noAssociations_checkPreferenceInvisible() {
        mController.mAssociationId = DUMMY_ASSOCIATION_ID;
        mController.refresh();

        assertThat(mPermSyncPreference.isVisible()).isFalse();
    }

    @Test
    public void refresh_permSyncNull_checkPreferenceInvisible() {
        mPermissionSyncRequest = null;
        when(mCompanionDeviceManager.getPermissionSyncRequest(ASSOCIATION_ID)).thenReturn(
                mPermissionSyncRequest);
        mController.refresh();

        assertThat(mPermSyncPreference.isVisible()).isFalse();
    }

    @Test
    public void refresh_permSyncEnabled_checkPreferenceOn() {
        mPermissionSyncRequest.setUserConsented(true);
        mController.refresh();

        assertThat(mPermSyncPreference.isVisible()).isTrue();
        assertThat(mPermSyncPreference.isChecked()).isTrue();
    }

    @Test
    public void refresh_permSyncDisabled_checkPreferenceOff() {
        mPermissionSyncRequest.setUserConsented(false);
        mController.refresh();

        assertThat(mPermSyncPreference.isVisible()).isTrue();
        assertThat(mPermSyncPreference.isChecked()).isFalse();
    }
}
