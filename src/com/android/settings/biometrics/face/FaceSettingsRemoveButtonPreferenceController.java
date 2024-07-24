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
import android.content.pm.PackageManager;
import android.hardware.face.Face;
import android.hardware.face.FaceManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.window.OnBackInvokedCallback;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.biometrics.BiometricUtils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.widget.LayoutPreference;

import com.google.android.setupdesign.util.ButtonStyler;
import com.google.android.setupdesign.util.PartnerStyleHelper;

import java.util.List;

/**
 * Controller for the remove button. This assumes that there is only a single face enrolled. The UI
 * will likely change if multiple enrollments are allowed/supported.
 */
public class FaceSettingsRemoveButtonPreferenceController extends BasePreferenceController
        implements View.OnClickListener {

    private static final String TAG = "FaceSettings/Remove";
    static final String KEY = "security_settings_face_delete_faces_container";

    public static class ConfirmRemoveDialog extends InstrumentedDialogFragment
            implements OnBackInvokedCallback {
        private static final String KEY_IS_CONVENIENCE = "is_convenience";
        private DialogInterface.OnClickListener mOnClickListener;
        @Nullable
        private AlertDialog mDialog = null;
        @Nullable
        private Preference mFaceUnlockPreference = null;

        /** Returns the new instance of the class */
        public static ConfirmRemoveDialog newInstance(boolean isConvenience) {
            final ConfirmRemoveDialog dialog = new ConfirmRemoveDialog();
            final Bundle args = new Bundle();
            args.putBoolean(KEY_IS_CONVENIENCE, isConvenience);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_FACE_REMOVE;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            boolean isConvenience = getArguments().getBoolean(KEY_IS_CONVENIENCE);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            final PackageManager pm = getContext().getPackageManager();
            final boolean hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
            final int dialogMessageRes;

            if (hasFingerprint) {
                dialogMessageRes = isConvenience
                        ? R.string.security_settings_face_remove_dialog_details_fingerprint_conv
                        : R.string.security_settings_face_remove_dialog_details_fingerprint;
            } else {
                dialogMessageRes = isConvenience
                        ? R.string.security_settings_face_settings_remove_dialog_details_convenience
                        : R.string.security_settings_face_settings_remove_dialog_details;
            }

            builder.setTitle(R.string.security_settings_face_settings_remove_dialog_title)
                    .setMessage(dialogMessageRes)
                    .setPositiveButton(R.string.delete, mOnClickListener)
                    .setNegativeButton(R.string.cancel, mOnClickListener);
            mDialog = builder.create();
            mDialog.setCanceledOnTouchOutside(false);
            mDialog.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0, this);
            return mDialog;
        }

        public void setOnClickListener(DialogInterface.OnClickListener listener) {
            mOnClickListener = listener;
        }

        public void setPreference(@Nullable Preference preference) {
            mFaceUnlockPreference = preference;
        }

        public void unregisterOnBackInvokedCallback() {
            if (mDialog != null) {
                mDialog.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(this);
            }
        }

        @Override
        public void onBackInvoked() {
            if (mDialog != null) {
                mDialog.cancel();
            }
            unregisterOnBackInvokedCallback();

            if (mFaceUnlockPreference != null) {
                final Button removeButton = ((LayoutPreference) mFaceUnlockPreference)
                        .findViewById(R.id.security_settings_face_settings_remove_button);
                if (removeButton != null) {
                    removeButton.setEnabled(true);
                }
            }
        }
    }

    interface Listener {
        void onRemoved();
    }

    private Preference mPreference;
    private Button mButton;
    private Listener mListener;
    private SettingsActivity mActivity;
    private int mUserId;
    @VisibleForTesting
    boolean mRemoving;

    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final Context mContext;
    private final FaceManager mFaceManager;
    private final FaceUpdater mFaceUpdater;
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

    private final DialogInterface.OnClickListener mOnConfirmDialogClickListener
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
                mFaceUpdater.remove(faces.get(0), mUserId, mRemovalCallback);
            } else {
                mButton.setEnabled(true);
                mRemoving = false;
            }

            final ConfirmRemoveDialog removeDialog =
                    (ConfirmRemoveDialog) mActivity.getSupportFragmentManager()
                            .findFragmentByTag(ConfirmRemoveDialog.class.getName());
            if (removeDialog != null) {
                removeDialog.unregisterOnBackInvokedCallback();
            }
        }
    };

    public FaceSettingsRemoveButtonPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mContext = context;
        mFaceManager = context.getSystemService(FaceManager.class);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mFaceUpdater = new FaceUpdater(context, mFaceManager);
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

        mPreference = preference;
        mButton = ((LayoutPreference) preference)
                .findViewById(R.id.security_settings_face_settings_remove_button);

        if (PartnerStyleHelper.shouldApplyPartnerResource(mButton)) {
            ButtonStyler.applyPartnerCustomizationPrimaryButtonStyle(mContext, mButton);
        }

        mButton.setOnClickListener(this);

        // If there is already a ConfirmRemoveDialog showing, reset the listener since the
        // controller has been recreated.
        ConfirmRemoveDialog removeDialog =
                (ConfirmRemoveDialog) mActivity.getSupportFragmentManager()
                        .findFragmentByTag(ConfirmRemoveDialog.class.getName());
        if (removeDialog != null) {
            removeDialog.setPreference(mPreference);
            mRemoving = true;
            removeDialog.setOnClickListener(mOnConfirmDialogClickListener);
        }

        if (!FaceSettings.isFaceHardwareDetected(mContext)) {
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
            mMetricsFeatureProvider.logClickedPreference(mPreference, getMetricsCategory());
            mRemoving = true;
            ConfirmRemoveDialog confirmRemoveDialog =
                    ConfirmRemoveDialog.newInstance(BiometricUtils.isConvenience(mFaceManager));
            confirmRemoveDialog.setOnClickListener(mOnConfirmDialogClickListener);
            confirmRemoveDialog.show(mActivity.getSupportFragmentManager(),
                            ConfirmRemoveDialog.class.getName());
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void setActivity(SettingsActivity activity) {
        mActivity = activity;
    }
}
