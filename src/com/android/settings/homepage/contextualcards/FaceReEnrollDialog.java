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
 * limitations under the License
 */

package com.android.settings.homepage.contextualcards;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.face.Face;
import android.hardware.face.FaceManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.app.AlertActivity;
import com.android.internal.app.AlertController;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.homepage.contextualcards.slices.FaceSetupSlice;

/**
 * This class is used to show a popup dialog for {@link FaceSetupSlice}.
 */
public class FaceReEnrollDialog extends AlertActivity implements
        DialogInterface.OnClickListener {

    private static final String TAG = "FaceReEnrollDialog";

    private static final String BIOMETRIC_ENROLL_ACTION = "android.settings.BIOMETRIC_ENROLL";

    private FaceManager mFaceManager;
    /**
     * The type of re-enrollment that has been requested,
     * see {@link Settings.Secure#FACE_UNLOCK_RE_ENROLL} for more details.
     */
    private int mReEnrollType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final AlertController.AlertParams alertParams = mAlertParams;
        alertParams.mTitle = getText(
                R.string.security_settings_face_enroll_improve_face_alert_title);
        alertParams.mMessage = getText(
                R.string.security_settings_face_enroll_improve_face_alert_body);
        alertParams.mPositiveButtonText = getText(R.string.storage_menu_set_up);
        alertParams.mNegativeButtonText = getText(R.string.cancel);
        alertParams.mPositiveButtonListener = this;

        mFaceManager = Utils.getFaceManagerOrNull(getApplicationContext());

        final Context context = getApplicationContext();
        mReEnrollType = FaceSetupSlice.getReEnrollSetting(context, getUserId());

        Log.d(TAG, "ReEnroll Type : " + mReEnrollType);
        if (mReEnrollType == FaceSetupSlice.FACE_RE_ENROLL_SUGGESTED) {
            // setupAlert will actually display the popup dialog.
            setupAlert();
        } else if (mReEnrollType == FaceSetupSlice.FACE_RE_ENROLL_REQUIRED) {
            // in this case we are skipping the popup dialog and directly going to the
            // re enrollment flow. A grey overlay will appear to indicate that we are
            // transitioning.
            removeFaceAndReEnroll();
        } else {
            Log.d(TAG, "Error unsupported flow for : " + mReEnrollType);
            dismiss();
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        removeFaceAndReEnroll();
    }

    public void removeFaceAndReEnroll() {
        final int userId = getUserId();
        if (mFaceManager == null || !mFaceManager.hasEnrolledTemplates(userId)) {
            finish();
        }
        mFaceManager.remove(new Face("", 0, 0), userId, new FaceManager.RemovalCallback() {
            @Override
            public void onRemovalError(Face face, int errMsgId, CharSequence errString) {
                super.onRemovalError(face, errMsgId, errString);
                finish();
            }

            @Override
            public void onRemovalSucceeded(Face face, int remaining) {
                super.onRemovalSucceeded(face, remaining);
                if (remaining != 0) {
                    return;
                }
                // Send user to the enroll flow.
                final Intent reEnroll = new Intent(BIOMETRIC_ENROLL_ACTION);
                final Context context = getApplicationContext();

                try {
                    startActivity(reEnroll);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to startActivity");
                }

                finish();
            }
        });
    }
}
