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

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Pair;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockscreenCredential;
import com.android.settings.R;

/**
 * An invisible retained worker fragment to track the AsyncWork that saves (and optionally
 * verifies if a challenge is given) the chosen lock credential (pattern/pin/password).
 */
abstract class SaveChosenLockWorkerBase extends Fragment {

    private Listener mListener;
    private boolean mFinished;
    private Intent mResultData;

    protected LockPatternUtils mUtils;
    protected boolean mHasChallenge;
    protected long mChallenge;
    protected boolean mWasSecureBefore;
    protected int mUserId;
    protected int mUnificationProfileId = UserHandle.USER_NULL;
    protected LockscreenCredential mUnificationProfileCredential;

    private boolean mBlocking;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setListener(Listener listener) {
        if (mListener == listener) {
            return;
        }

        mListener = listener;
        if (mFinished && mListener != null) {
            mListener.onChosenLockSaveFinished(mWasSecureBefore, mResultData);
        }
    }

    protected void prepare(LockPatternUtils utils, boolean credentialRequired,
            boolean hasChallenge, long challenge, int userId) {
        mUtils = utils;
        mUserId = userId;

        mHasChallenge = hasChallenge;
        mChallenge = challenge;
        // This will be a no-op for non managed profiles.
        mWasSecureBefore = mUtils.isSecure(mUserId);

        Context context = getContext();
        // If context is null, we're being invoked to change the setCredentialRequiredToDecrypt,
        // and we made sure that this is the primary user already.
        if (context == null || UserManager.get(context).getUserInfo(mUserId).isPrimary()) {
            mUtils.setCredentialRequiredToDecrypt(credentialRequired);
        }

        mFinished = false;
        mResultData = null;
    }

    protected void start() {
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
    protected abstract Pair<Boolean, Intent> saveAndVerifyInBackground();

    protected void finish(Intent resultData) {
        mFinished = true;
        mResultData = resultData;
        if (mListener != null) {
            mListener.onChosenLockSaveFinished(mWasSecureBefore, mResultData);
        }
        if (mUnificationProfileCredential != null) {
            mUnificationProfileCredential.zeroize();
        }
    }

    public void setBlocking(boolean blocking) {
        mBlocking = blocking;
    }

    public void setProfileToUnify(int profileId, LockscreenCredential credential) {
        mUnificationProfileId = profileId;
        mUnificationProfileCredential = credential.duplicate();
    }

    protected void unifyProfileCredentialIfRequested() {
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
