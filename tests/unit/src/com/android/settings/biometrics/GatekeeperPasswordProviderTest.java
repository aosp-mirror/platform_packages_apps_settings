/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.biometrics;

import static com.android.settings.biometrics.BiometricUtils.GatekeeperCredentialNotMatchException;
import static com.android.settings.biometrics.GatekeeperPasswordProvider.containsGatekeeperPasswordHandle;
import static com.android.settings.biometrics.GatekeeperPasswordProvider.getGatekeeperPasswordHandle;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.VerifyCredentialResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(AndroidJUnit4.class)
public class GatekeeperPasswordProviderTest {

    @Rule public final MockitoRule mockito = MockitoJUnit.rule();

    @Mock private LockPatternUtils mLockPatternUtils;
    private GatekeeperPasswordProvider mGatekeeperPasswordProvider;

    @Before
    public void setUp() {
        mGatekeeperPasswordProvider = new GatekeeperPasswordProvider(mLockPatternUtils);
    }

    @Test
    public void testRequestGatekeeperHatWithHandle_success() {
        final long gkPwHandle = 1L;
        final long challenge = 2L;
        final int userId = 0;
        final byte[] expectedToken = new byte[] { 3, 2, 1 };
        when(mLockPatternUtils.verifyGatekeeperPasswordHandle(gkPwHandle, challenge, userId))
                .thenReturn(newGoodCredential(gkPwHandle, expectedToken));
        final byte[] actualToken = mGatekeeperPasswordProvider.requestGatekeeperHat(gkPwHandle,
                challenge, userId);
        assertThat(actualToken).isNotNull();
        assertThat(actualToken.length).isEqualTo(expectedToken.length);
        for (int i = 0; i < actualToken.length; ++i) {
            assertWithMessage("actualToken[" + i + "] is " + actualToken[i] + " not "
                    + expectedToken[i]).that(actualToken[i]).isEqualTo(expectedToken[i]);
        }
    }

    @Test(expected = GatekeeperCredentialNotMatchException.class)
    public void testRequestGatekeeperHatWithHandle_GatekeeperCredentialNotMatchException() {
        final long gkPwHandle = 10L;
        final long challenge = 20L;
        final int userId = 300;
        when(mLockPatternUtils.verifyGatekeeperPasswordHandle(gkPwHandle, challenge, userId))
                .thenReturn(newBadCredential(0));

        mGatekeeperPasswordProvider.requestGatekeeperHat(gkPwHandle, challenge, userId);
    }

    @Test
    public void testRequestGatekeeperHatWithIntent_success() {
        final long gkPwHandle = 11L;
        final long challenge = 21L;
        final int userId = 145;
        final byte[] expectedToken = new byte[] { 4, 5, 6, 7 };
        when(mLockPatternUtils.verifyGatekeeperPasswordHandle(gkPwHandle, challenge, userId))
                .thenReturn(newGoodCredential(gkPwHandle, expectedToken));
        final byte[] actualToken = mGatekeeperPasswordProvider.requestGatekeeperHat(
                new Intent().putExtra(EXTRA_KEY_GK_PW_HANDLE, gkPwHandle), challenge, userId);
        assertThat(actualToken).isNotNull();
        assertThat(actualToken.length).isEqualTo(expectedToken.length);
        for (int i = 0; i < actualToken.length; ++i) {
            assertWithMessage("actualToken[" + i + "] is " + actualToken[i] + " not "
                    + expectedToken[i]).that(actualToken[i]).isEqualTo(expectedToken[i]);
        }
    }

    @Test(expected = GatekeeperCredentialNotMatchException.class)
    public void testRequestGatekeeperHatWithIntent_GatekeeperCredentialNotMatchException() {
        final long gkPwHandle = 12L;
        final long challenge = 22L;
        final int userId = 0;
        when(mLockPatternUtils.verifyGatekeeperPasswordHandle(gkPwHandle, challenge, userId))
                .thenReturn(newBadCredential(0));

        mGatekeeperPasswordProvider.requestGatekeeperHat(
                new Intent().putExtra(EXTRA_KEY_GK_PW_HANDLE, gkPwHandle), challenge, userId);
    }

    @Test(expected = IllegalStateException.class)
    public void testRequestGatekeeperHatWithIntent_IllegalStateException() {
        mGatekeeperPasswordProvider.requestGatekeeperHat(new Intent(), 1L, 0);
    }

    @Test
    public void testContainsGatekeeperPasswordHandle() {
        assertThat(containsGatekeeperPasswordHandle(null)).isEqualTo(false);
        assertThat(containsGatekeeperPasswordHandle(new Intent())).isEqualTo(false);
        assertThat(containsGatekeeperPasswordHandle(
                new Intent().putExtra(EXTRA_KEY_GK_PW_HANDLE, 2L))).isEqualTo(true);
    }

    @Test
    public void testGetGatekeeperPasswordHandle() {
        assertThat(getGatekeeperPasswordHandle(new Intent())).isEqualTo(0L);
        assertThat(getGatekeeperPasswordHandle(
                new Intent().putExtra(EXTRA_KEY_GK_PW_HANDLE, 3L))).isEqualTo(3L);
    }

    @Test
    public void testRemoveGatekeeperPasswordHandleAsHandle() {
        final long gkPwHandle = 1L;
        doNothing().when(mLockPatternUtils).removeGatekeeperPasswordHandle(gkPwHandle);

        mGatekeeperPasswordProvider.removeGatekeeperPasswordHandle(gkPwHandle);

        verify(mLockPatternUtils, only()).removeGatekeeperPasswordHandle(gkPwHandle);
    }

    @Test
    public void testRemoveGatekeeperPasswordHandleAsIntent() {
        final long gkPwHandle = 1234L;
        final Intent intent = new Intent().putExtra(EXTRA_KEY_GK_PW_HANDLE, gkPwHandle);
        doNothing().when(mLockPatternUtils).removeGatekeeperPasswordHandle(gkPwHandle);

        mGatekeeperPasswordProvider.removeGatekeeperPasswordHandle(intent, false);

        verify(mLockPatternUtils, only()).removeGatekeeperPasswordHandle(gkPwHandle);
        assertThat(intent.getLongExtra(EXTRA_KEY_GK_PW_HANDLE, 0L)).isEqualTo(gkPwHandle);
    }

    @Test
    public void testRemoveGatekeeperPasswordHandleAsIntent_removeKey() {
        final long gkPwHandle = 1234L;
        final Intent intent = new Intent().putExtra(EXTRA_KEY_GK_PW_HANDLE, gkPwHandle);
        doNothing().when(mLockPatternUtils).removeGatekeeperPasswordHandle(gkPwHandle);

        mGatekeeperPasswordProvider.removeGatekeeperPasswordHandle(intent, true);

        verify(mLockPatternUtils, only()).removeGatekeeperPasswordHandle(gkPwHandle);
        assertThat(intent.hasExtra(EXTRA_KEY_GK_PW_HANDLE)).isEqualTo(false);
    }

    private VerifyCredentialResponse newGoodCredential(long gkPwHandle, @NonNull byte[] hat) {
        return new VerifyCredentialResponse.Builder()
                .setGatekeeperPasswordHandle(gkPwHandle)
                .setGatekeeperHAT(hat)
                .build();
    }

    private VerifyCredentialResponse newBadCredential(int timeout) {
        if (timeout > 0) {
            return VerifyCredentialResponse.fromTimeout(timeout);
        } else {
            return VerifyCredentialResponse.fromError();
        }
    }
}
