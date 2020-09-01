/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.app.admin.FactoryResetProtectionPolicy;
import android.content.Context;
import android.service.persistentdata.PersistentDataBlockManager;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class MasterClearConfirmTest {

    private FragmentActivity mActivity;

    @Mock
    private FragmentActivity mMockActivity;

    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    @Mock
    private PersistentDataBlockManager mPersistentDataBlockManager;

    private MasterClearConfirm mMasterClearConfirm;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mMasterClearConfirm = spy(new MasterClearConfirm());
    }

    @Test
    public void setSubtitle_eraseEsim() {
        MasterClearConfirm masterClearConfirm = new MasterClearConfirm();
        masterClearConfirm.mEraseEsims = true;
        masterClearConfirm.mContentView =
                LayoutInflater.from(mActivity).inflate(R.layout.master_clear_confirm, null);

        masterClearConfirm.setSubtitle();

        assertThat(((TextView) masterClearConfirm.mContentView
                .findViewById(R.id.sud_layout_description)).getText())
                .isEqualTo(mActivity.getString(R.string.master_clear_final_desc_esim));
    }

    @Test
    public void setSubtitle_notEraseEsim() {
        MasterClearConfirm masterClearConfirm = new MasterClearConfirm();
        masterClearConfirm.mEraseEsims = false;
        masterClearConfirm.mContentView =
                LayoutInflater.from(mActivity).inflate(R.layout.master_clear_confirm, null);

        masterClearConfirm.setSubtitle();

        assertThat(((TextView) masterClearConfirm.mContentView
                .findViewById(R.id.sud_layout_description)).getText())
                .isEqualTo(mActivity.getString(R.string.master_clear_final_desc));
    }

    @Test
    public void shouldWipePersistentDataBlock_noPersistentDataBlockManager_shouldReturnFalse() {
        assertThat(mMasterClearConfirm.shouldWipePersistentDataBlock(null)).isFalse();
    }

    @Test
    public void shouldWipePersistentDataBlock_deviceIsStillBeingProvisioned_shouldReturnFalse() {
        doReturn(true).when(mMasterClearConfirm).isDeviceStillBeingProvisioned();

        assertThat(mMasterClearConfirm.shouldWipePersistentDataBlock(
                mPersistentDataBlockManager)).isFalse();
    }

    @Test
    public void shouldWipePersistentDataBlock_oemUnlockAllowed_shouldReturnFalse() {
        doReturn(false).when(mMasterClearConfirm).isDeviceStillBeingProvisioned();
        doReturn(true).when(mMasterClearConfirm).isOemUnlockedAllowed();

        assertThat(mMasterClearConfirm.shouldWipePersistentDataBlock(
                mPersistentDataBlockManager)).isFalse();
    }

    @Test
    public void shouldWipePersistentDataBlock_frpPolicyNotSupported_shouldReturnFalse() {
        when(mMasterClearConfirm.getActivity()).thenReturn(mMockActivity);

        doReturn(false).when(mMasterClearConfirm).isDeviceStillBeingProvisioned();
        doReturn(false).when(mMasterClearConfirm).isOemUnlockedAllowed();
        when(mMockActivity.getSystemService(Context.DEVICE_POLICY_SERVICE))
                .thenReturn(mDevicePolicyManager);
        when(mDevicePolicyManager.isFactoryResetProtectionPolicySupported()).thenReturn(false);

        assertThat(mMasterClearConfirm.shouldWipePersistentDataBlock(
                mPersistentDataBlockManager)).isFalse();
    }

    @Test
    public void shouldWipePersistentDataBlock_hasFactoryResetProtectionPolicy_shouldReturnFalse() {
        when(mMasterClearConfirm.getActivity()).thenReturn(mMockActivity);

        doReturn(false).when(mMasterClearConfirm).isDeviceStillBeingProvisioned();
        doReturn(false).when(mMasterClearConfirm).isOemUnlockedAllowed();
        ArrayList<String> accounts = new ArrayList<>();
        accounts.add("test");
        FactoryResetProtectionPolicy frp = new FactoryResetProtectionPolicy.Builder()
                .setFactoryResetProtectionAccounts(accounts)
                .setFactoryResetProtectionEnabled(true)
                .build();
        when(mMockActivity.getSystemService(Context.DEVICE_POLICY_SERVICE))
                .thenReturn(mDevicePolicyManager);
        when(mDevicePolicyManager.isFactoryResetProtectionPolicySupported()).thenReturn(true);
        when(mDevicePolicyManager.getFactoryResetProtectionPolicy(null)).thenReturn(frp);
        when(mDevicePolicyManager.isOrganizationOwnedDeviceWithManagedProfile()).thenReturn(true);

        assertThat(mMasterClearConfirm.shouldWipePersistentDataBlock(
                mPersistentDataBlockManager)).isFalse();
    }

    @Test
    public void shouldWipePersistentDataBlock_isNotOrganizationOwnedDevice_shouldReturnTrue() {
        when(mMasterClearConfirm.getActivity()).thenReturn(mMockActivity);

        doReturn(false).when(mMasterClearConfirm).isDeviceStillBeingProvisioned();
        doReturn(false).when(mMasterClearConfirm).isOemUnlockedAllowed();

        when(mMockActivity.getSystemService(Context.DEVICE_POLICY_SERVICE))
                .thenReturn(mDevicePolicyManager);
        when(mDevicePolicyManager.isFactoryResetProtectionPolicySupported()).thenReturn(true);
        when(mDevicePolicyManager.getFactoryResetProtectionPolicy(null)).thenReturn(null);
        when(mDevicePolicyManager.isOrganizationOwnedDeviceWithManagedProfile()).thenReturn(false);

        assertThat(mMasterClearConfirm.shouldWipePersistentDataBlock(
                mPersistentDataBlockManager)).isTrue();
    }
}
