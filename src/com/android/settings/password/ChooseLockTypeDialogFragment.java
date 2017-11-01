/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.password;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import java.util.List;

/**
 * A dialog fragment similar to {@link ChooseLockGeneric} where the user can select from a few
 * lock screen types.
 */
public class ChooseLockTypeDialogFragment extends InstrumentedDialogFragment
        implements OnClickListener {

    private static final String ARG_USER_ID = "userId";

    private ScreenLockAdapter mAdapter;
    private ChooseLockGenericController mController;

    public static ChooseLockTypeDialogFragment newInstance(int userId) {
        Bundle args = new Bundle();
        args.putInt(ARG_USER_ID, userId);
        ChooseLockTypeDialogFragment fragment = new ChooseLockTypeDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public interface OnLockTypeSelectedListener {
        void onLockTypeSelected(ScreenLockType lock);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int userId = getArguments().getInt(ARG_USER_ID);
        mController = new ChooseLockGenericController(getContext(), userId);
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        OnLockTypeSelectedListener listener = null;
        Fragment parentFragment = getParentFragment();
        if (parentFragment instanceof OnLockTypeSelectedListener) {
            listener = (OnLockTypeSelectedListener) parentFragment;
        } else {
            Context context = getContext();
            if (context instanceof OnLockTypeSelectedListener) {
                listener = (OnLockTypeSelectedListener) context;
            }
        }
        if (listener != null) {
            listener.onLockTypeSelected(mAdapter.getItem(i));
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getContext();
        Builder builder = new Builder(context);
        List<ScreenLockType> locks =
                mController.getVisibleScreenLockTypes(
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING,
                        false /* includeDisabled */);
        mAdapter = new ScreenLockAdapter(context, locks, mController);
        builder.setAdapter(mAdapter, this);
        builder.setTitle(R.string.setup_lock_settings_options_dialog_title);
        AlertDialog alertDialog = builder.create();
        return alertDialog;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SETTINGS_CHOOSE_LOCK_DIALOG;
    }

    private static class ScreenLockAdapter extends ArrayAdapter<ScreenLockType> {

        private final ChooseLockGenericController mController;

        ScreenLockAdapter(
                Context context,
                List<ScreenLockType> locks,
                ChooseLockGenericController controller) {
            super(context, R.layout.choose_lock_dialog_item, locks);
            mController = controller;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            Context context = parent.getContext();
            if (view == null) {
                view = LayoutInflater.from(context)
                        .inflate(R.layout.choose_lock_dialog_item, parent, false);
            }
            ScreenLockType lock = getItem(position);
            TextView textView = (TextView) view;
            textView.setText(mController.getTitle(lock));
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    getIconForScreenLock(context, lock), null, null, null);
            return view;
        }

        private static Drawable getIconForScreenLock(Context context, ScreenLockType lock) {
            switch (lock) {
                case PATTERN:
                    return context.getDrawable(R.drawable.ic_pattern);
                case PIN:
                    return context.getDrawable(R.drawable.ic_pin);
                case PASSWORD:
                    return context.getDrawable(R.drawable.ic_password);
                case NONE:
                case SWIPE:
                case MANAGED:
                default:
                        return null;
            }
        }
    }
}
