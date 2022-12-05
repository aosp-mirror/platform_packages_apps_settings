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

package com.android.settings.biometrics2.ui.model;

import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_CHALLENGE;
import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_SENSOR_ID;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN;
import static com.android.settings.password.ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE;

import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.time.Clock;

/**
 * Secret credential data including
 * 1. userId
 * 2. sensorId
 * 3. challenge
 * 4. token
 * 5. gkPwHandle
 */
public final class CredentialModel {

    /**
     * Default value for an invalid challenge
     */
    @VisibleForTesting
    public static final long INVALID_CHALLENGE = -1L;

    /**
     * Default value if GkPwHandle is invalid.
     */
    @VisibleForTesting
    public static final long INVALID_GK_PW_HANDLE = 0L;

    /**
     * Default value for a invalid sensor id
     */
    @VisibleForTesting
    public static final int INVALID_SENSOR_ID = -1;

    private final Clock mClock;

    private final long mInitMillis;

    private final int mUserId;

    private int mSensorId;
    @Nullable
    private Long mUpdateSensorIdMillis = null;

    private long mChallenge;
    @Nullable
    private Long mUpdateChallengeMillis = null;

    @Nullable
    private byte[] mToken;
    @Nullable
    private Long mUpdateTokenMillis = null;

    private long mGkPwHandle;
    @Nullable
    private Long mClearGkPwHandleMillis = null;

    public CredentialModel(@NonNull Bundle bundle, @NonNull Clock clock) {
        mUserId = bundle.getInt(Intent.EXTRA_USER_ID, UserHandle.myUserId());
        mSensorId = bundle.getInt(EXTRA_KEY_SENSOR_ID, INVALID_SENSOR_ID);
        mChallenge = bundle.getLong(EXTRA_KEY_CHALLENGE, INVALID_CHALLENGE);
        mToken = bundle.getByteArray(EXTRA_KEY_CHALLENGE_TOKEN);
        mGkPwHandle = bundle.getLong(EXTRA_KEY_GK_PW_HANDLE, INVALID_GK_PW_HANDLE);
        mClock = clock;
        mInitMillis = mClock.millis();
    }

    /**
     * Get a bundle which can be used to recreate CredentialModel
     */
    @NonNull
    public Bundle getBundle() {
        final Bundle bundle = new Bundle();
        bundle.putInt(Intent.EXTRA_USER_ID, mUserId);
        bundle.putInt(EXTRA_KEY_SENSOR_ID, mSensorId);
        bundle.putLong(EXTRA_KEY_CHALLENGE, mChallenge);
        bundle.putByteArray(EXTRA_KEY_CHALLENGE_TOKEN, mToken);
        bundle.putLong(EXTRA_KEY_GK_PW_HANDLE, mGkPwHandle);
        return bundle;
    }

    /**
     * Get userId for this credential
     */
    public int getUserId() {
        return mUserId;
    }

    /**
     * Check user id is valid or not
     */
    public boolean isValidUserId() {
        return mUserId != UserHandle.USER_NULL;
    }

    /**
     * Get challenge
     */
    public long getChallenge() {
        return mChallenge;
    }

    /**
     * Set challenge
     */
    public void setChallenge(long value) {
        mUpdateChallengeMillis = mClock.millis();
        mChallenge = value;
    }

    /**
     * Check challenge is valid or not
     */
    public boolean isValidChallenge() {
        return mChallenge != INVALID_CHALLENGE;
    }

    /**
     * Get challenge token
     */
    @Nullable
    public byte[] getToken() {
        return mToken;
    }

    /**
     * Set challenge token
     */
    public void setToken(@Nullable byte[] value) {
        mUpdateTokenMillis = mClock.millis();
        mToken = value;
    }

    /**
     * Check challengeToken is valid or not
     */
    public boolean isValidToken() {
        return mToken != null;
    }

    /**
     * Get gatekeeper password handle
     */
    public long getGkPwHandle() {
        return mGkPwHandle;
    }

    /**
     * Clear gatekeeper password handle data
     */
    public void clearGkPwHandle() {
        mClearGkPwHandleMillis = mClock.millis();
        mGkPwHandle = INVALID_GK_PW_HANDLE;
    }

    /**
     * Check gkPwHandle is valid or not
     */
    public boolean isValidGkPwHandle() {
        return mGkPwHandle != INVALID_GK_PW_HANDLE;
    }

    /**
     * Get sensor id
     */
    public int getSensorId() {
        return mSensorId;
    }

    /**
     * Set sensor id
     */
    public void setSensorId(int value) {
        mUpdateSensorIdMillis = mClock.millis();
        mSensorId = value;
    }

    /**
     * Returns a string representation of the object
     */
    @Override
    public String toString() {
        final int gkPwHandleLen = ("" + mGkPwHandle).length();
        final int tokenLen = mToken == null ? 0 : mToken.length;
        final int challengeLen = ("" + mChallenge).length();
        return getClass().getSimpleName() + ":{initMillis:" + mInitMillis
                + ", userId:" + mUserId
                + ", challenge:{len:" + challengeLen
                + ", updateMillis:" + mUpdateChallengeMillis + "}"
                + ", token:{len:" + tokenLen + ", isValid:" + isValidToken()
                + ", updateMillis:" + mUpdateTokenMillis + "}"
                + ", gkPwHandle:{len:" + gkPwHandleLen + ", isValid:" + isValidGkPwHandle()
                + ", clearMillis:" + mClearGkPwHandleMillis + "}"
                + ", mSensorId:{id:" + mSensorId + ", updateMillis:" + mUpdateSensorIdMillis + "}"
                + " }";
    }
}
