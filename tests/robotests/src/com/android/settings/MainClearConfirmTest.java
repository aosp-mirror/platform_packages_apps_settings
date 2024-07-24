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
import org.robolectric.annotation.Config;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class MainClearConfirmTest {

    private FragmentActivity mActivity;

    @Mock
    private FragmentActivity mMockActivity;

    @Mock
    private DevicePolicyManager mDevicePolicyManager;

    @Mock
    private PersistentDataBlockManager mPersistentDataBlockManager;

    private MainClearConfirm mMainClearConfirm;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
        mMainClearConfirm = spy(new MainClearConfirm());
    }

    @Test
    public void setSubtitle_eraseEsim() {
        MainClearConfirm mainClearConfirm = new MainClearConfirm();
        mainClearConfirm.mEraseEsims = true;
        mainClearConfirm.mContentView =
                LayoutInflater.from(mActivity).inflate(R.layout.main_clear_confirm, null);

        mainClearConfirm.setSubtitle();

        assertThat(((TextView) mainClearConfirm.mContentView
                .findViewById(R.id.sud_layout_description)).getText())
                .isEqualTo(mActivity.getString(R.string.main_clear_final_desc_esim));
    }

    @Test
    public void setSubtitle_notEraseEsim() {
        MainClearConfirm mainClearConfirm = new MainClearConfirm();
        mainClearConfirm.mEraseEsims = false;
        mainClearConfirm.mContentView =
                LayoutInflater.from(mActivity).inflate(R.layout.main_clear_confirm, null);

        mainClearConfirm.setSubtitle();

        assertThat(((TextView) mainClearConfirm.mContentView
                .findViewById(R.id.sud_layout_description)).getText())
                .isEqualTo(mActivity.getString(R.string.main_clear_final_desc));
    }

    @Test
    public void shouldWipePersistentDataBlock_noPersistentDataBlockManager_shouldReturnFalse() {
        assertThat(mMainClearConfirm.shouldWipePersistentDataBlock(null)).isFalse();
    }

    @Test
    public void shouldWipePersistentDataBlock_deviceIsStillBeingProvisioned_shouldReturnFalse() {
        doReturn(true).when(mMainClearConfirm).isDeviceStillBeingProvisioned();

        assertThat(mMainClearConfirm.shouldWipePersistentDataBlock(
                mPersistentDataBlockManager)).isFalse();
    }

    @Test
    public void shouldWipePersistentDataBlock_oemUnlockAllowed_shouldReturnFalse() {
        doReturn(false).when(mMainClearConfirm).isDeviceStillBeingProvisioned();
        doReturn(true).when(mMainClearConfirm).isOemUnlockedAllowed();

        assertThat(mMainClearConfirm.shouldWipePersistentDataBlock(
                mPersistentDataBlockManager)).isFalse();
    }

    @Test
    public void shouldWipePersistentDataBlock_frpPolicyNotSupported_shouldReturnFalse() {
        when(mMainClearConfirm.getActivity()).thenReturn(mMockActivity);

        doReturn(false).when(mMainClearConfirm).isDeviceStillBeingProvisioned();
        doReturn(false).when(mMainClearConfirm).isOemUnlockedAllowed();
        when(mMockActivity.getSystemService(Context.DEVICE_POLICY_SERVICE))
                .thenReturn(mDevicePolicyManager);
        when(mDevicePolicyManager.isFactoryResetProtectionPolicySupported()).thenReturn(false);

        assertThat(mMainClearConfirm.shouldWipePersistentDataBlock(
                mPersistentDataBlockManager)).isFalse();
    }

    @Test
    public void shouldWipePersistentDataBlock_hasFactoryResetProtectionPolicy_shouldReturnFalse() {
        when(mMainClearConfirm.getActivity()).thenReturn(mMockActivity);

        doReturn(false).when(mMainClearConfirm).isDeviceStillBeingProvisioned();
        doReturn(false).when(mMainClearConfirm).isOemUnlockedAllowed();
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

        assertThat(mMainClearConfirm.shouldWipePersistentDataBlock(
                mPersistentDataBlockManager)).isFalse();
    }

    @Test
    public void shouldWipePersistentDataBlock_isNotOrganizationOwnedDevice_shouldReturnTrue() {
        when(mMainClearConfirm.getActivity()).thenReturn(mMockActivity);

        doReturn(false).when(mMainClearConfirm).isDeviceStillBeingProvisioned();
        doReturn(false).when(mMainClearConfirm).isOemUnlockedAllowed();

        when(mMockActivity.getSystemService(Context.DEVICE_POLICY_SERVICE))
                .thenReturn(mDevicePolicyManager);
        when(mDevicePolicyManager.isFactoryResetProtectionPolicySupported()).thenReturn(true);
        when(mDevicePolicyManager.getFactoryResetProtectionPolicy(null)).thenReturn(null);
        when(mDevicePolicyManager.isOrganizationOwnedDeviceWithManagedProfile()).thenReturn(false);

        assertThat(mMainClearConfirm.shouldWipePersistentDataBlock(
                mPersistentDataBlockManager)).isTrue();
    }
}
