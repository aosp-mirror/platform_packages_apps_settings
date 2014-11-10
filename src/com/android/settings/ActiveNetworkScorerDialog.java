/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppManager;
import android.net.NetworkScorerAppManager.NetworkScorerAppData;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;

/**
 * Dialog to allow a user to select a new network scorer.
 *
 * <p>Finishes with {@link #RESULT_CANCELED} in all circumstances unless the scorer is successfully
 * changed or was already set to the new value (in which case it finishes with {@link #RESULT_OK}).
 */
public final class ActiveNetworkScorerDialog extends AlertActivity implements
        DialogInterface.OnClickListener {
    private static final String TAG = "ActiveNetScorerDlg";

    private String mNewPackageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mNewPackageName = intent.getStringExtra(NetworkScoreManager.EXTRA_PACKAGE_NAME);

        if (!buildDialog()) {
            finish();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case BUTTON_POSITIVE:
                NetworkScoreManager nsm =
                    (NetworkScoreManager) getSystemService(Context.NETWORK_SCORE_SERVICE);
                if (nsm.setActiveScorer(mNewPackageName)) {
                    setResult(RESULT_OK);
                }
                break;
            case BUTTON_NEGATIVE:
                break;
        }
    }

    private boolean buildDialog() {
        if (UserHandle.myUserId() != UserHandle.USER_OWNER) {
            Log.i(TAG, "Can only set scorer for owner user.");
            return false;
        }
        NetworkScorerAppData newScorer = NetworkScorerAppManager.getScorer(this, mNewPackageName);
        if (newScorer == null) {
            Log.e(TAG, "New package " + mNewPackageName + " is not a valid scorer.");
            return false;
        }

        NetworkScorerAppData oldScorer = NetworkScorerAppManager.getActiveScorer(this);
        if (oldScorer != null && TextUtils.equals(oldScorer.mPackageName, mNewPackageName)) {
            Log.i(TAG, "New package " + mNewPackageName + " is already the active scorer.");
            // Set RESULT_OK to indicate to the caller that the "switch" was successful.
            setResult(RESULT_OK);
            return false;
        }

        // Compose dialog.
        CharSequence newName = newScorer.mScorerName;
        final AlertController.AlertParams p = mAlertParams;
        p.mTitle = getString(R.string.network_scorer_change_active_dialog_title);
        if (oldScorer != null) {
            p.mMessage = getString(R.string.network_scorer_change_active_dialog_text, newName,
                    oldScorer.mScorerName);
        } else {
            p.mMessage = getString(R.string.network_scorer_change_active_no_previous_dialog_text,
                    newName);
        }
        p.mPositiveButtonText = getString(R.string.yes);
        p.mNegativeButtonText = getString(R.string.no);
        p.mPositiveButtonListener = this;
        p.mNegativeButtonListener = this;
        setupAlert();

        return true;
    }
}
