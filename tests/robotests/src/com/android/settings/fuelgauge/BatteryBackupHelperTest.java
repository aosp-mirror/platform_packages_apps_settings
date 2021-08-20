/*
 * Copyright (C) 2021 The Android Open Source Project
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
 *
 */

package com.android.settings.fuelgauge;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.backup.BackupDataOutput;
import android.content.Context;
import android.os.IDeviceIdleController;
import android.os.RemoteException;
import android.os.UserHandle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {BatteryBackupHelperTest.ShadowUserHandle.class})
public final class BatteryBackupHelperTest {

    private Context mContext;
    private BatteryBackupHelper mBatteryBackupHelper;

    @Mock
    private BackupDataOutput mBackupDataOutput;
    @Mock
    private IDeviceIdleController mDeviceController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        mBatteryBackupHelper = new BatteryBackupHelper(mContext);
        mBatteryBackupHelper.mIDeviceIdleController = mDeviceController;
    }

    @After
    public void resetShadows() {
        ShadowUserHandle.reset();
    }

    @Test
    public void performBackup_nullPowerList_notBackupPowerList() throws Exception {
        doReturn(null).when(mDeviceController).getFullPowerWhitelist();
        mBatteryBackupHelper.performBackup(null, mBackupDataOutput, null);

        verify(mBackupDataOutput, never()).writeEntityHeader(anyString(), anyInt());
    }

    @Test
    public void performBackup_emptyPowerList_notBackupPowerList() throws Exception {
        doReturn(new String[0]).when(mDeviceController).getFullPowerWhitelist();
        mBatteryBackupHelper.performBackup(null, mBackupDataOutput, null);

        verify(mBackupDataOutput, never()).writeEntityHeader(anyString(), anyInt());
    }

    @Test
    public void performBackup_remoteException_notBackupPowerList() throws Exception {
        doThrow(new RemoteException()).when(mDeviceController).getFullPowerWhitelist();
        mBatteryBackupHelper.performBackup(null, mBackupDataOutput, null);

        verify(mBackupDataOutput, never()).writeEntityHeader(anyString(), anyInt());
    }

    @Test
    public void performBackup_oneFullPowerListElement_backupFullPowerListData()
            throws Exception {
        final String[] fullPowerList = {"com.android.package"};
        doReturn(fullPowerList).when(mDeviceController).getFullPowerWhitelist();

        mBatteryBackupHelper.performBackup(null, mBackupDataOutput, null);

        final byte[] expectedBytes = fullPowerList[0].getBytes();
        verify(mBackupDataOutput).writeEntityHeader(
                BatteryBackupHelper.KEY_FULL_POWER_LIST, expectedBytes.length);
        verify(mBackupDataOutput).writeEntityData(expectedBytes, expectedBytes.length);
    }

    @Test
    public void performBackup_backupFullPowerListData() throws Exception {
        final String[] fullPowerList = {"com.android.package1", "com.android.package2"};
        doReturn(fullPowerList).when(mDeviceController).getFullPowerWhitelist();

        mBatteryBackupHelper.performBackup(null, mBackupDataOutput, null);

        final String expectedResult = fullPowerList[0]
                + BatteryBackupHelper.DELIMITER + fullPowerList[1];
        final byte[] expectedBytes = expectedResult.getBytes();
        verify(mBackupDataOutput).writeEntityHeader(
                BatteryBackupHelper.KEY_FULL_POWER_LIST, expectedBytes.length);
        verify(mBackupDataOutput).writeEntityData(expectedBytes, expectedBytes.length);
    }

    @Test
    public void performBackup_nonOwner_ignoreAllBackupAction() throws Exception {
        ShadowUserHandle.setUid(1);
        final String[] fullPowerList = {"com.android.package"};
        doReturn(fullPowerList).when(mDeviceController).getFullPowerWhitelist();

        mBatteryBackupHelper.performBackup(null, mBackupDataOutput, null);

        verify(mBackupDataOutput, never()).writeEntityHeader(anyString(), anyInt());
    }

    @Implements(UserHandle.class)
    public static class ShadowUserHandle {
        // Sets the default as thte OWNER role.
        private static int sUid = 0;

        public static void setUid(int uid) {
            sUid = uid;
        }

        @Implementation
        public static int myUserId() {
            return sUid;
        }

        @Resetter
        public static void reset() {
            sUid = 0;
        }
    }
}
