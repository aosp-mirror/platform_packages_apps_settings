/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.privatespace.delete;

import static com.android.settings.privatespace.PrivateSpaceMaintainer.ErrorDeletingPrivateSpace.DELETE_PS_ERROR_INTERNAL;
import static com.android.settings.privatespace.PrivateSpaceMaintainer.ErrorDeletingPrivateSpace.DELETE_PS_ERROR_NONE;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.privatespace.PrivateSpaceMaintainer;

/** Fragment to show loading animation screen while deleting private space. */
public class PrivateSpaceDeletionProgressFragment extends InstrumentedFragment {
    private static final String TAG = "PrivateSpaceDeleteProg";
    private static final int PRIVATE_SPACE_DELETE_POST_DELAY_MS = 1000;
    private Handler mHandler;
    private PrivateSpaceMaintainer mPrivateSpaceMaintainer;
    private Runnable mDeletePrivateSpace =
            new Runnable() {
                @Override
                public void run() {
                    deletePrivateSpace();
                    getActivity().finish();
                }
            };

    static class Injector {
        PrivateSpaceMaintainer injectPrivateSpaceMaintainer(Context context) {
            return PrivateSpaceMaintainer.getInstance(context);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (android.os.Flags.allowPrivateProfile()) {
            super.onCreate(savedInstanceState);
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.PRIVATE_SPACE_SETTINGS;
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mPrivateSpaceMaintainer =
                new PrivateSpaceDeletionProgressFragment.Injector()
                        .injectPrivateSpaceMaintainer(getActivity().getApplicationContext());
        if (!mPrivateSpaceMaintainer.doesPrivateSpaceExist()) {
            // Ideally this should never happen as PS Settings is not available when there's no
            // Private Profile.
            Log.e(TAG, "Unexpected attempt to delete non-existent PS");
            getActivity().finish();
        }
        View contentView =
                inflater.inflate(R.layout.private_space_confirm_deletion, container, false);
        OnBackPressedCallback callback =
                new OnBackPressedCallback(true /* enabled by default */) {
                    @Override
                    public void handleOnBackPressed() {
                        // Handle the back button event. We intentionally don't want to allow back
                        // button to work in this screen during the setup flow.
                    }
                };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
        mHandler = new Handler(Looper.getMainLooper());
        // Ensures screen visibility to user by introducing a 1-second delay before deleting private
        // space.
        mHandler.postDelayed(mDeletePrivateSpace, PRIVATE_SPACE_DELETE_POST_DELAY_MS);
        return contentView;
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mDeletePrivateSpace);
        super.onDestroy();
    }

    /** Deletes private space and shows a toast message */
    @VisibleForTesting
    public void deletePrivateSpace() {
        PrivateSpaceMaintainer.ErrorDeletingPrivateSpace error =
                mPrivateSpaceMaintainer.deletePrivateSpace();
        if (error == DELETE_PS_ERROR_NONE) {
            showSuccessfulDeletionToast();
        } else if (error == DELETE_PS_ERROR_INTERNAL) {
            showDeletionInternalErrorToast();
        }
    }

    @VisibleForTesting
    public void setPrivateSpaceMaintainer(@NonNull Injector injector) {
        mPrivateSpaceMaintainer = injector.injectPrivateSpaceMaintainer(getActivity());
    }

    /** Shows a toast saying that the private space was deleted */
    @VisibleForTesting
    public void showSuccessfulDeletionToast() {
        Toast.makeText(getContext(), R.string.private_space_deleted, Toast.LENGTH_SHORT).show();
    }

    /** Shows a toast saying that the private space could not be deleted */
    @VisibleForTesting
    public void showDeletionInternalErrorToast() {
        Toast.makeText(getContext(), R.string.private_space_delete_failed, Toast.LENGTH_SHORT)
                .show();
    }
}
