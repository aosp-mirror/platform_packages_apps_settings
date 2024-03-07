/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.privatespace;

import static com.android.settings.privatespace.PrivateSpaceMaintainer.ErrorDeletingPrivateSpace.DELETE_PS_ERROR_INTERNAL;
import static com.android.settings.privatespace.PrivateSpaceMaintainer.ErrorDeletingPrivateSpace.DELETE_PS_ERROR_NONE;
import static com.android.settings.privatespace.PrivateSpaceMaintainer.ErrorDeletingPrivateSpace.DELETE_PS_ERROR_NO_PRIVATE_SPACE;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

/** Controller to delete the private space from the PS Settings page */
public class DeletePrivateSpaceController extends BasePreferenceController {
    private static final String TAG = "DeletePrivateSpaceController";
    private final PrivateSpaceMaintainer mPrivateSpaceMaintainer;

    static class Injector {
        PrivateSpaceMaintainer injectPrivateSpaceMaintainer(Context context) {
            return PrivateSpaceMaintainer.getInstance(context);
        }
    }

    public DeletePrivateSpaceController(Context context, String preferenceKey) {
        this(context, preferenceKey, new Injector());
    }

    DeletePrivateSpaceController(Context context, String preferenceKey, Injector injector) {
        super(context, preferenceKey);
        mPrivateSpaceMaintainer = injector.injectPrivateSpaceMaintainer(context);
    }

    @Override
    public int getAvailabilityStatus() {
        return android.os.Flags.allowPrivateProfile() ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }

        PrivateSpaceMaintainer.ErrorDeletingPrivateSpace error =
                mPrivateSpaceMaintainer.deletePrivateSpace();
        if (error == DELETE_PS_ERROR_NONE) {
            showSuccessfulDeletionToast();
        } else if (error == DELETE_PS_ERROR_INTERNAL) {
            showDeletionInternalErrorToast();
        } else if (error == DELETE_PS_ERROR_NO_PRIVATE_SPACE) {
            // Ideally this should never happen as PS Settings is not available when there's no
            // Private Profile.
            Log.e(TAG, "Unexpected attempt to delete non-existent PS");
        }
        return super.handlePreferenceTreeClick(preference);
    }

    /** Shows a toast saying that the private space was deleted */
    @VisibleForTesting
    public void showSuccessfulDeletionToast() {
        Toast.makeText(mContext, R.string.private_space_deleted, Toast.LENGTH_SHORT).show();
    }

    /** Shows a toast saying that the private space could not be deleted */
    @VisibleForTesting
    public void showDeletionInternalErrorToast() {
        Toast.makeText(mContext, R.string.private_space_delete_failed, Toast.LENGTH_SHORT).show();
    }
}
