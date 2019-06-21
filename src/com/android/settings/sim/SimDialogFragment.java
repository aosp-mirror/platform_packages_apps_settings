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

package com.android.settings.sim;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;

import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.network.SubscriptionsChangeListener;

/** Common functionality for showing a dialog in SimDialogActivity. */
public abstract class SimDialogFragment extends InstrumentedDialogFragment implements
        SubscriptionsChangeListener.SubscriptionsChangeListenerClient {
    private static final String TAG = "SimDialogFragment";

    private static final String KEY_TITLE_ID = "title_id";
    private static final String KEY_DIALOG_TYPE = "dialog_type";

    private SubscriptionsChangeListener mChangeListener;

    protected static Bundle initArguments(int dialogType, int titleResId) {
        final Bundle args = new Bundle();
        args.putInt(KEY_DIALOG_TYPE, dialogType);
        args.putInt(KEY_TITLE_ID, titleResId);
        return args;
    }

    public int getDialogType() {
        return getArguments().getInt(KEY_DIALOG_TYPE);
    }

    public int getTitleResId() {
        return getArguments().getInt(KEY_TITLE_ID);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mChangeListener = new SubscriptionsChangeListener(context, this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mChangeListener.stop();
    }

    @Override
    public void onResume() {
        super.onResume();
        mChangeListener.start();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        final SimDialogActivity activity = (SimDialogActivity) getActivity();
        if (activity != null && !activity.isFinishing()) {
            activity.onFragmentDismissed(this);
        }
    }

    public abstract void updateDialog();

    @Override
    public void onAirplaneModeChanged(boolean airplaneModeEnabled) {
    }

    @Override
    public void onSubscriptionsChanged() {
        updateDialog();
    }
}
