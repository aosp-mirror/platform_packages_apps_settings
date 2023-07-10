/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.internal.widget.VerifyCredentialResponse;
import com.android.settings.R;
import com.android.settings.safetycenter.LockScreenSafetySource;

/**
 * An invisible retained worker fragment to track the AsyncWork that saves (and optionally
 * verifies if a challenge is given) the chosen lock credential (pattern/pin/password).
 */
public class SaveAndFinishWorker extends Fragment {
    private static final String TAG = "SaveAndFinishWorker";

    private Listener mListener;
    private boolean mFinished;
    private Intent mResultData;

    private LockPatternUtils mUtils;
    private boolean mRequestGatekeeperPassword;
    private boolean mRequestWriteRepairModePassword;
    private boolean mWasSecureBefore;
    private int mUserId;
    private int mUnificationProfileId = UserHandle.USER_NULL;
    private LockscreenCredential mUnificationProfileCredential;
    private LockscreenCredential mChosenCredential;
    private LockscreenCredential mCurrentCredential;

    private boolean mBlocking;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public SaveAndFinishWorker setListener(Listener listener) {
        if (mListener == listener) {
            return this;
        }

        mListener = listener;
        if (mFinished && mListener != null) {
            mListener.onChosenLockSaveFinished(mWasSecureBefore, mResultData);
        }
        return this;
    }

    @VisibleForTesting
    void prepare(LockPatternUtils utils, LockscreenCredential chosenCredential,
            LockscreenCredential currentCredential, int userId) {
        mUtils = utils;
        mUserId = userId;
        // This will be a no-op for non managed profiles.
        mWasSecureBefore = mUtils.isSecure(mUserId);
        mFinished = false;
        mResultData = null;

        mChosenCredential = chosenCredential;
        mCurrentCredential = currentCredential != null ? currentCredential
                : LockscreenCredential.createNone();
    }

    public void start(LockPatternUtils utils, LockscreenCredential chosenCredential,
            LockscreenCredential currentCredential, int userId) {
        prepare(utils, chosenCredential, currentCredential, userId);
        if (mBlocking) {
            finish(saveAndVerifyInBackground().second);
        } else {
            new Task().execute();
        }
    }

    /**
     * Executes the save and verify work in background.
     * @return pair where the first is a boolean confirming whether the change was successful or not
     * and second is the Intent which has the challenge token or is null.
     */
    @VisibleForTesting
    Pair<Boolean, Intent> saveAndVerifyInBackground() {
        final int userId = mUserId;
        if (!mUtils.setLockCredential(mChosenCredential, mCurrentCredential, userId)) {
            return Pair.create(false, null);
        }

        unifyProfileCredentialIfRequested();

        @LockPatternUtils.VerifyFlag int flags = 0;
        if (mRequestGatekeeperPassword) {
            // If a Gatekeeper Password was requested, invoke the LockSettingsService code
            // path to return a Gatekeeper Password based on the credential that the user
            // chose. This should only be run if the credential was successfully set.
            flags |= LockPatternUtils.VERIFY_FLAG_REQUEST_GK_PW_HANDLE;
        }
        if (mRequestWriteRepairModePassword) {
            flags |= LockPatternUtils.VERIFY_FLAG_WRITE_REPAIR_MODE_PW;
        }
        if (flags == 0) {
            return Pair.create(true, null);
        }

        Intent result = new Intent();
        final VerifyCredentialResponse response = mUtils.verifyCredential(mChosenCredential,
                userId, flags);
        if (response.isMatched()) {
            if (mRequestGatekeeperPassword && response.containsGatekeeperPasswordHandle()) {
                result.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_GK_PW_HANDLE,
                        response.getGatekeeperPasswordHandle());
            } else if (mRequestGatekeeperPassword) {
                Log.e(TAG, "critical: missing GK PW handle for known good credential: " + response);
            }
        } else {
            Log.e(TAG, "critical: bad response for known good credential: " + response);
        }
        if (mRequestWriteRepairModePassword) {
            // Notify the caller if repair mode credential is saved successfully
            result.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_WROTE_REPAIR_MODE_CREDENTIAL,
                    response.isMatched());
        }

        return Pair.create(true, result);
    }

    private void finish(Intent resultData) {
        mFinished = true;
        mResultData = resultData;
        if (mListener != null) {
            mListener.onChosenLockSaveFinished(mWasSecureBefore, mResultData);
        }
        if (mUnificationProfileCredential != null) {
            mUnificationProfileCredential.zeroize();
        }
        LockScreenSafetySource.onLockScreenChange(getContext());
    }

    public SaveAndFinishWorker setRequestGatekeeperPasswordHandle(boolean value) {
        mRequestGatekeeperPassword = value;
        return this;
    }

    public SaveAndFinishWorker setRequestWriteRepairModePassword(boolean value) {
        mRequestWriteRepairModePassword = value;
        return this;
    }

    public SaveAndFinishWorker setBlocking(boolean blocking) {
        mBlocking = blocking;
        return this;
    }

    public SaveAndFinishWorker setProfileToUnify(
            int profileId, LockscreenCredential credential) {
        mUnificationProfileId = profileId;
        mUnificationProfileCredential = credential.duplicate();
        return this;
    }

    private void unifyProfileCredentialIfRequested() {
        if (mUnificationProfileId != UserHandle.USER_NULL) {
            mUtils.setSeparateProfileChallengeEnabled(mUnificationProfileId, false,
                    mUnificationProfileCredential);
        }
    }

    private class Task extends AsyncTask<Void, Void, Pair<Boolean, Intent>> {

        @Override
        protected Pair<Boolean, Intent> doInBackground(Void... params){
            return saveAndVerifyInBackground();
        }

        @Override
        protected void onPostExecute(Pair<Boolean, Intent> resultData) {
            if (!resultData.first) {
                Toast.makeText(getContext(), R.string.lockpassword_credential_changed,
                        Toast.LENGTH_LONG).show();
            }
            finish(resultData.second);
        }
    }

    interface Listener {
        void onChosenLockSaveFinished(boolean wasSecureBefore, Intent resultData);
    }
}
