/*
 * Copyright (C) 2017 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.settings.biometrics.fingerprint;

import android.annotation.Nullable;
import android.app.settings.SettingsEnums;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

import com.android.settings.core.InstrumentedFragment;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Sidecar fragment to handle the state around fingerprint removal.
 */
public class FingerprintRemoveSidecar extends InstrumentedFragment {

    private static final String TAG = "FingerprintRemoveSidecar";
    private Listener mListener;
    private Fingerprint mFingerprintRemoving;
    private Queue<Object> mFingerprintsRemoved;
    FingerprintManager mFingerprintManager;

    private class RemovalError {
        Fingerprint fingerprint;
        int errMsgId;
        CharSequence errString;
        public RemovalError(Fingerprint fingerprint, int errMsgId, CharSequence errString) {
            this.fingerprint = fingerprint;
            this.errMsgId = errMsgId;
            this.errString = errString;
        }
    }

    private FingerprintManager.RemovalCallback
            mRemoveCallback = new FingerprintManager.RemovalCallback() {
        @Override
        public void onRemovalSucceeded(Fingerprint fingerprint, int remaining) {
            if (mListener != null) {
                mListener.onRemovalSucceeded(fingerprint);
            } else {
                mFingerprintsRemoved.add(fingerprint);
            };
            mFingerprintRemoving = null;
        }

        @Override
        public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
            if (mListener != null) {
                mListener.onRemovalError(fp, errMsgId, errString);
            } else {
                mFingerprintsRemoved.add(new RemovalError(fp, errMsgId, errString));
            }
            mFingerprintRemoving = null;
        }
    };

    public void startRemove(Fingerprint fingerprint, int userId) {
        if (mFingerprintRemoving != null) {
            Log.e(TAG, "Remove already in progress");
            return;
        }
        if (userId != UserHandle.USER_NULL) {
            mFingerprintManager.setActiveUser(userId);
        }
        mFingerprintRemoving = fingerprint;
        mFingerprintManager.remove(fingerprint, userId, mRemoveCallback);;
    }

    public FingerprintRemoveSidecar() {
        mFingerprintsRemoved = new LinkedList<>();
    }

    public void setFingerprintManager(FingerprintManager fingerprintManager) {
        mFingerprintManager = fingerprintManager;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setListener(Listener listener) {
        if (mListener == null && listener != null) {
            while (!mFingerprintsRemoved.isEmpty()) {
                Object o = mFingerprintsRemoved.poll();
                if (o instanceof Fingerprint) {
                    listener.onRemovalSucceeded((Fingerprint)o);
                } else if (o instanceof RemovalError) {
                    RemovalError e = (RemovalError) o;
                    listener.onRemovalError(e.fingerprint, e.errMsgId, e.errString);
                }
            }
        }
        mListener = listener;
    }

    public interface Listener {
        void onRemovalSucceeded(Fingerprint fingerprint);
        void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString);
    }

    final boolean isRemovingFingerprint(int fid) {
        return inProgress() && mFingerprintRemoving.getBiometricId() == fid;
    }

    final boolean inProgress() {
        return mFingerprintRemoving != null;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.FINGERPRINT_REMOVE_SIDECAR;
    }

}
