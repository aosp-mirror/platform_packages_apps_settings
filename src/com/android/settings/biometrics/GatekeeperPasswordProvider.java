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

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.settings.password.ChooseLockSettingsHelper;

/**
 * Gatekeeper hat related methods
 */
public class GatekeeperPasswordProvider {

    private static final String TAG = "GatekeeperPasswordProvider";

    private final LockPatternUtils mLockPatternUtils;

    public GatekeeperPasswordProvider(LockPatternUtils lockPatternUtils) {
        mLockPatternUtils = lockPatternUtils;
    }

    /**
     * Given the result from confirming or choosing a credential, request Gatekeeper to generate
     * a HardwareAuthToken with the Gatekeeper Password together with a biometric challenge.
     *
     * @param result The onActivityResult intent from ChooseLock* or ConfirmLock*
     * @param challenge Unique biometric challenge from FingerprintManager/FaceManager
     * @param userId User ID that the credential/biometric operation applies to
     * @throws GatekeeperCredentialNotMatchException if Gatekeeper response is not match
     * @throws IllegalStateException if Gatekeeper Password is missing
     */
    public byte[] requestGatekeeperHat(@NonNull Intent result, long challenge, int userId) {
        if (!containsGatekeeperPasswordHandle(result)) {
            throw new IllegalStateException("Gatekeeper Password is missing!!");
        }
        final long gatekeeperPasswordHandle = result.getLongExtra(
                ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, 0L);
        return requestGatekeeperHat(gatekeeperPasswordHandle, challenge, userId);
    }

    /**
     * Given the result from confirming or choosing a credential, request Gatekeeper to generate
     * a HardwareAuthToken with the Gatekeeper Password together with a biometric challenge.
     *
     * @param gkPwHandle The Gatekeeper password handle from ChooseLock* or ConfirmLock*
     * @param challenge Unique biometric challenge from FingerprintManager/FaceManager
     * @param userId User ID that the credential/biometric operation applies to
     * @throws GatekeeperCredentialNotMatchException if Gatekeeper response is not match
     */
    public byte[] requestGatekeeperHat(long gkPwHandle, long challenge, int userId) {
        final VerifyCredentialResponse response = mLockPatternUtils.verifyGatekeeperPasswordHandle(
                gkPwHandle, challenge, userId);
        if (!response.isMatched()) {
            throw new GatekeeperCredentialNotMatchException("Unable to request Gatekeeper HAT");
        }
        return response.getGatekeeperHAT();
    }

    /**
     * Intent data contains gatekeeper password handle or not
     */
    public static boolean containsGatekeeperPasswordHandle(@Nullable Intent data) {
        return data != null && data.hasExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE);
    }

    /**
     * Returns the gatekeeper password handle from intent
     */
    public static long getGatekeeperPasswordHandle(@NonNull Intent data) {
        return data.getLongExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE, 0L);
    }

    /**
     * Requests {@link com.android.server.locksettings.LockSettingsService} to remove the
     * gatekeeper password associated with a previous
     * {@link ChooseLockSettingsHelper.Builder#setRequestGatekeeperPasswordHandle(boolean)}
     *
     * @param data The onActivityResult intent from ChooseLock* or ConfirmLock*
     * @param alsoRemoveItFromIntent set it to true if gkPwHandle needs to be removed from intent
     */
    public void removeGatekeeperPasswordHandle(@Nullable Intent data,
            boolean alsoRemoveItFromIntent) {
        if (data == null) {
            return;
        }
        if (!containsGatekeeperPasswordHandle(data)) {
            return;
        }
        removeGatekeeperPasswordHandle(getGatekeeperPasswordHandle(data));
        if (alsoRemoveItFromIntent) {
            data.removeExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE);
        }
    }

    /**
     * Requests {@link com.android.server.locksettings.LockSettingsService} to remove the
     * gatekeeper password associated with a previous
     * {@link ChooseLockSettingsHelper.Builder#setRequestGatekeeperPasswordHandle(boolean)}
     *
     * @param handle The Gatekeeper password handle from ChooseLock* or ConfirmLock*
     */
    public void removeGatekeeperPasswordHandle(long handle) {
        mLockPatternUtils.removeGatekeeperPasswordHandle(handle);
        Log.d(TAG, "Removed handle");
    }
}
