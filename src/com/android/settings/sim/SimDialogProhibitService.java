/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.sim;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.concurrent.RejectedExecutionException;

/**
 * A class for routing dismiss dialog request to SimDialogActivity.
 */
public class SimDialogProhibitService {

    private static final String TAG = "SimDialogProhibitService";

    private static WeakReference<SimDialogActivity> sSimDialogActivity;

    /**
     * Support the dismiss of {@link SimDialogActivity} (singletone.)
     *
     * @param activity {@link SimDialogActivity}
     */
    public static void supportDismiss(SimDialogActivity activity) {
        sSimDialogActivity = new WeakReference<SimDialogActivity>(activity);
    }

    /**
     * Dismiss SimDialogActivity dialog.
     *
     * @param context is a {@link Context}
     */
    public static void dismissDialog(Context context) {
        // Dismiss existing dialog.
        if (!dismissDialogThroughRunnable()) {
            dismissDialogThroughIntent(context);
        }
    }

    /**
     * Dismiss dialog (if there's any).
     *
     * @return {@code true} when success, {@code false} when failure.
     */
    protected static boolean dismissDialogThroughRunnable() {
        final SimDialogActivity activity = (sSimDialogActivity == null) ?
                null : sSimDialogActivity.get();
        if (activity == null) {
            Log.i(TAG, "No SimDialogActivity for dismiss.");
            return true;
        }

        try {
            activity.getMainExecutor().execute(() -> activity.forceClose());
            return true;
        } catch (RejectedExecutionException exception) {
            Log.w(TAG, "Fail to close SimDialogActivity through executor", exception);
        }
        return false;
    }

    /**
     * Dismiss dialog through {@link Intent}.
     *
     * @param uiContext is {@link Context} for start SimDialogActivity.
     */
    protected static void dismissDialogThroughIntent(Context uiContext) {
        Intent newIntent = new Intent(uiContext, SimDialogActivity.class);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        newIntent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.PICK_DISMISS);
        uiContext.startActivity(newIntent);
    }
}
