/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.deviceinfo.storage;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settingslib.utils.ThreadUtils;

/**
 * Dialog asks if users want to empty trash files.
 * TODO(b/189388449): Shows "Deleting..." and disables Trash category while deleting trash files.
 */
public class EmptyTrashFragment extends InstrumentedDialogFragment {
    private static final String TAG = "EmptyTrashFragment";

    private static final String TAG_EMPTY_TRASH = "empty_trash";

    private final Fragment mParentFragment;
    private final int mUserId;
    private final long mTrashSize;
    private final OnEmptyTrashCompleteListener mOnEmptyTrashCompleteListener;

    /** The listener to receive empty trash complete callback event. */
    public interface OnEmptyTrashCompleteListener {
        /** The empty trash complete callback. */
        void onEmptyTrashComplete();
    }

    public EmptyTrashFragment(Fragment parent, int userId, long trashSize,
            OnEmptyTrashCompleteListener onEmptyTrashCompleteListener) {
        super();

        mParentFragment = parent;
        setTargetFragment(mParentFragment, 0 /* requestCode */);
        mUserId = userId;
        mTrashSize = trashSize;
        mOnEmptyTrashCompleteListener = onEmptyTrashCompleteListener;
    }

    /** Shows the empty trash dialog. */
    public void show() {
        show(mParentFragment.getFragmentManager(), TAG_EMPTY_TRASH);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DIALOG_EMPTY_TRASH;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        return builder.setTitle(R.string.storage_trash_dialog_title)
                .setMessage(getActivity().getString(R.string.storage_trash_dialog_ask_message,
                        StorageUtils.getStorageSizeLabel(getActivity(), mTrashSize)))
                .setPositiveButton(R.string.storage_trash_dialog_confirm,
                        (dialog, which) -> emptyTrashAsync())
                .setNegativeButton(android.R.string.cancel, null)
                .create();
    }

    private void emptyTrashAsync() {
        final Context context = getActivity();
        final Context perUserContext;
        try {
            perUserContext = context.createPackageContextAsUser(
                context.getApplicationContext().getPackageName(),
                0 /* flags= */,
                UserHandle.of(mUserId));
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Not able to get Context for user ID " + mUserId);
            return;
        }

        final Bundle trashQueryArgs = new Bundle();
        trashQueryArgs.putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY);
        ThreadUtils.postOnBackgroundThread(() -> {
            perUserContext.getContentResolver().delete(
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    trashQueryArgs);
            if (mOnEmptyTrashCompleteListener == null) {
                return;
            }
            ThreadUtils.postOnMainThread(
                    () -> mOnEmptyTrashCompleteListener.onEmptyTrashComplete());
        });
    }
}
