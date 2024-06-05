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

package com.android.settings.password;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.VerifyCredentialResponse;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SaveAndFinishWorkerTest {
    @Test
    public void testSetRequestWriteRepairModePassword_setLockCredentialFail() {
        int userId = 0;
        int flags = LockPatternUtils.VERIFY_FLAG_WRITE_REPAIR_MODE_PW;
        var chosenCredential = LockscreenCredential.createPassword("1234");
        var currentCredential = LockscreenCredential.createNone();
        var worker = new SaveAndFinishWorker();
        var lpu = mock(LockPatternUtils.class);

        when(lpu.setLockCredential(chosenCredential, currentCredential, userId)).thenReturn(false);

        worker.setRequestWriteRepairModePassword(true);
        worker.prepare(lpu, chosenCredential, currentCredential, userId);
        var result = worker.saveAndVerifyInBackground();

        verify(lpu).setLockCredential(chosenCredential, currentCredential, userId);
        verify(lpu, never()).verifyCredential(chosenCredential, userId, flags);
        assertThat(result.first).isFalse();
    }

    @Test
    public void testSetRequestWriteRepairModePassword_verifyCredentialFail() {
        int userId = 0;
        int flags = LockPatternUtils.VERIFY_FLAG_WRITE_REPAIR_MODE_PW;
        var chosenCredential = LockscreenCredential.createPassword("1234");
        var currentCredential = LockscreenCredential.createNone();
        var worker = new SaveAndFinishWorker();
        var lpu = mock(LockPatternUtils.class);
        var response = VerifyCredentialResponse.fromError();

        when(lpu.setLockCredential(chosenCredential, currentCredential, userId)).thenReturn(true);
        when(lpu.verifyCredential(chosenCredential, userId, flags)).thenReturn(response);

        worker.setRequestWriteRepairModePassword(true);
        worker.prepare(lpu, chosenCredential, currentCredential, userId);
        var result = worker.saveAndVerifyInBackground();

        verify(lpu).setLockCredential(chosenCredential, currentCredential, userId);
        verify(lpu).verifyCredential(chosenCredential, userId, flags);
        assertThat(result.first).isTrue();
        assertThat(result.second.getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_WROTE_REPAIR_MODE_CREDENTIAL, true))
                .isFalse();
    }

    @Test
    public void testSetRequestWriteRepairModePassword_verifyCredentialSucceed() {
        int userId = 0;
        int flags = LockPatternUtils.VERIFY_FLAG_WRITE_REPAIR_MODE_PW;
        var chosenCredential = LockscreenCredential.createPassword("1234");
        var currentCredential = LockscreenCredential.createNone();
        var worker = new SaveAndFinishWorker();
        var lpu = mock(LockPatternUtils.class);
        var response = new VerifyCredentialResponse.Builder().build();

        when(lpu.setLockCredential(chosenCredential, currentCredential, userId)).thenReturn(true);
        when(lpu.verifyCredential(chosenCredential, userId, flags)).thenReturn(response);

        worker.setRequestWriteRepairModePassword(true);
        worker.prepare(lpu, chosenCredential, currentCredential, userId);
        var result = worker.saveAndVerifyInBackground();

        verify(lpu).setLockCredential(chosenCredential, currentCredential, userId);
        verify(lpu).verifyCredential(chosenCredential, userId, flags);
        assertThat(result.first).isTrue();
        assertThat(result.second.getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_WROTE_REPAIR_MODE_CREDENTIAL, false))
                .isTrue();
    }

    @Test
    public void testSetRequestWriteRepairModePassword_verifyCredentialSucceed_noGkPwHandle() {
        int userId = 0;
        int flags = LockPatternUtils.VERIFY_FLAG_WRITE_REPAIR_MODE_PW
                | LockPatternUtils.VERIFY_FLAG_REQUEST_GK_PW_HANDLE;
        var chosenCredential = LockscreenCredential.createPassword("1234");
        var currentCredential = LockscreenCredential.createNone();
        var worker = new SaveAndFinishWorker();
        var lpu = mock(LockPatternUtils.class);
        var response = new VerifyCredentialResponse.Builder().build();

        when(lpu.setLockCredential(chosenCredential, currentCredential, userId)).thenReturn(true);
        when(lpu.verifyCredential(chosenCredential, userId, flags)).thenReturn(response);

        worker.setRequestWriteRepairModePassword(true);
        worker.setRequestGatekeeperPasswordHandle(true);
        worker.prepare(lpu, chosenCredential, currentCredential, userId);
        var result = worker.saveAndVerifyInBackground();

        verify(lpu).setLockCredential(chosenCredential, currentCredential, userId);
        verify(lpu).verifyCredential(chosenCredential, userId, flags);
        assertThat(result.first).isTrue();
        assertThat(result.second.getBooleanExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_WROTE_REPAIR_MODE_CREDENTIAL, false))
                .isTrue();
        assertThat(result.second.getLongExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, -1))
                .isEqualTo(-1);
    }
}
