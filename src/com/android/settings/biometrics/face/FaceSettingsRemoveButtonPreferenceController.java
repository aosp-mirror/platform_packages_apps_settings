/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.biometrics.face;

import android.content.Context;
import android.hardware.face.Face;
import android.hardware.face.FaceManager;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.LayoutPreference;

import java.util.List;

/**
 * Controller for the remove button. This assumes that there is only a single face enrolled. The UI
 * will likely change if multiple enrollments are allowed/supported.
 */
public class FaceSettingsRemoveButtonPreferenceController extends BasePreferenceController
        implements View.OnClickListener {

    private static final String TAG = "FaceSettings/Remove";
    private static final String KEY = "security_settings_face_delete_faces_container";

    interface Listener {
        void onRemoved();
    }

    private Button mButton;
    private List<Face> mFaces;
    private Listener mListener;

    private final Context mContext;
    private final int mUserId;
    private final FaceManager mFaceManager;
    private final FaceManager.RemovalCallback mRemovalCallback = new FaceManager.RemovalCallback() {
        @Override
        public void onRemovalError(Face face, int errMsgId, CharSequence errString) {
            Log.e(TAG, "Unable to remove face: " + face.getBiometricId()
                    + " error: " + errMsgId + " " + errString);
            Toast.makeText(mContext, errString, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRemovalSucceeded(Face face, int remaining) {
            if (remaining == 0) {
                mFaces = mFaceManager.getEnrolledFaces(mUserId);
                if (!mFaces.isEmpty()) {
                    mButton.setEnabled(true);
                } else {
                    mListener.onRemoved();
                }
            } else {
                Log.v(TAG, "Remaining: " + remaining);
            }
        }
    };

    public FaceSettingsRemoveButtonPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
        mFaceManager = context.getSystemService(FaceManager.class);
        // TODO: Use the profile-specific userId instead
        mUserId = UserHandle.myUserId();
        if (mFaceManager != null) {
            mFaces = mFaceManager.getEnrolledFaces(mUserId);
        }
    }

    public FaceSettingsRemoveButtonPreferenceController(Context context) {
        this(context, KEY);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        mButton = ((LayoutPreference) preference)
                .findViewById(R.id.security_settings_face_settings_remove_button);
        mButton.setOnClickListener(this);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public String getPreferenceKey() {
        return KEY;
    }

    @Override
    public void onClick(View v) {
        if (v == mButton) {
            mButton.setEnabled(false);
            if (mFaces.isEmpty()) {
                Log.e(TAG, "No faces");
                return;
            }
            if (mFaces.size() > 1) {
                Log.e(TAG, "Multiple enrollments: " + mFaces.size());
            }

            // Remove the first/only face
            mFaceManager.remove(mFaces.get(0), mUserId, mRemovalCallback);
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }
}
