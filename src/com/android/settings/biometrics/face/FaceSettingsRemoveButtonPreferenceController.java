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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.face.Face;
import android.hardware.face.FaceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.widget.LayoutPreference;

import java.util.List;

/**
 * Controller for the remove button. This assumes that there is only a single face enrolled. The UI
 * will likely change if multiple enrollments are allowed/supported.
 */
public class FaceSettingsRemoveButtonPreferenceController extends BasePreferenceController
        implements View.OnClickListener {

    private static final String TAG = "FaceSettings/Remove";
    static final String KEY = "security_settings_face_delete_faces_container";

    public static class ConfirmRemoveDialog extends InstrumentedDialogFragment {

        private DialogInterface.OnClickListener mOnClickListener;

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_FACE_REMOVE;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            builder.setTitle(R.string.security_settings_face_settings_remove_dialog_title)
                    .setMessage(R.string.security_settings_face_settings_remove_dialog_details)
                    .setPositiveButton(R.string.delete, mOnClickListener)
                    .setNegativeButton(R.string.cancel, mOnClickListener);
            AlertDialog dialog = builder.create();
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }

        public void setOnClickListener(DialogInterface.OnClickListener listener) {
            mOnClickListener = listener;
        }
    }

    interface Listener {
        void onRemoved();
    }

    private Button mButton;
    private Listener mListener;
    private SettingsActivity mActivity;
    private int mUserId;
    private boolean mRemoving;

    private final Context mContext;
    private final FaceManager mFaceManager;
    private final FaceManager.RemovalCallback mRemovalCallback = new FaceManager.RemovalCallback() {
        @Override
        public void onRemovalError(Face face, int errMsgId, CharSequence errString) {
            Log.e(TAG, "Unable to remove face: " + face.getBiometricId()
                    + " error: " + errMsgId + " " + errString);
            Toast.makeText(mContext, errString, Toast.LENGTH_SHORT).show();
            mRemoving = false;
        }

        @Override
        public void onRemovalSucceeded(Face face, int remaining) {
            if (remaining == 0) {
                final List<Face> faces = mFaceManager.getEnrolledFaces(mUserId);
                if (!faces.isEmpty()) {
                    mButton.setEnabled(true);
                } else {
                    mRemoving = false;
                    mListener.onRemoved();
                }
            } else {
                Log.v(TAG, "Remaining: " + remaining);
            }
        }
    };

    private final DialogInterface.OnClickListener mOnClickListener
            = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                mButton.setEnabled(false);
                final List<Face> faces = mFaceManager.getEnrolledFaces(mUserId);
                if (faces.isEmpty()) {
                    Log.e(TAG, "No faces");
                    return;
                }
                if (faces.size() > 1) {
                    Log.e(TAG, "Multiple enrollments: " + faces.size());
                }

                // Remove the first/only face
                mFaceManager.remove(faces.get(0), mUserId, mRemovalCallback);
            } else {
                mButton.setEnabled(true);
                mRemoving = false;
            }
        }
    };

    public FaceSettingsRemoveButtonPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
        mFaceManager = context.getSystemService(FaceManager.class);
    }

    public FaceSettingsRemoveButtonPreferenceController(Context context) {
        this(context, KEY);
    }

    public void setUserId(int userId) {
        mUserId = userId;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);

        mButton = ((LayoutPreference) preference)
                .findViewById(R.id.security_settings_face_settings_remove_button);
        mButton.setOnClickListener(this);

        if (!FaceSettings.isAvailable(mContext)) {
            mButton.setEnabled(false);
        } else {
            mButton.setEnabled(!mRemoving);
        }
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
            mRemoving = true;
            ConfirmRemoveDialog dialog = new ConfirmRemoveDialog();
            dialog.setOnClickListener(mOnClickListener);
            dialog.show(mActivity.getSupportFragmentManager(), ConfirmRemoveDialog.class.getName());
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setActivity(SettingsActivity activity) {
        mActivity = activity;
    }
}
