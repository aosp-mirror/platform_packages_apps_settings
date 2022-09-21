/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.axiom.settings.preference;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

public class CustomDialogPreferenceCompat<T extends DialogInterface> extends DialogPreference {

    private CustomPreferenceDialogFragment mFragment;

    public CustomDialogPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public CustomDialogPreferenceCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CustomDialogPreferenceCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomDialogPreferenceCompat(Context context) {
        super(context);
    }

    public boolean isDialogOpen() {
        return getDialog() != null && getDialog() instanceof Dialog && ((Dialog)getDialog()).isShowing();
    }

    public T getDialog() {
        return (T) (mFragment != null ? mFragment.getDialog() : null);
    }

    protected void onPrepareDialogBuilder(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
    }

    protected void onDialogClosed(boolean positiveResult) {
    }

    protected void onClick(T dialog, int which) {
    }

    protected void onBindDialogView(View view) {
    }

    protected void onStart() {
    }

    protected void onStop() {
    }

    protected void onPause() {
    }

    protected void onResume() {
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return null;
    }

    protected View onCreateDialogView(Context context) {
        return null;
    }

    private void setFragment(CustomPreferenceDialogFragment fragment) {
        mFragment = fragment;
    }

    protected boolean onDismissDialog(T dialog, int which) {
        return true;
    }

    public static class CustomPreferenceDialogFragment extends PreferenceDialogFragmentCompat {

        public static CustomPreferenceDialogFragment newInstance(String key) {
            final CustomPreferenceDialogFragment fragment = new CustomPreferenceDialogFragment();
            final Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            fragment.setArguments(b);
            return fragment;
        }

        private CustomDialogPreferenceCompat getCustomizablePreference() {
            return (CustomDialogPreferenceCompat) getPreference();
        }

        private class OnDismissListener implements View.OnClickListener {
            private final int mWhich;
            private final DialogInterface mDialog;

            public OnDismissListener(DialogInterface dialog, int which) {
                mWhich = which;
                mDialog = dialog;
            }

            @Override
            public void onClick(View view) {
                CustomPreferenceDialogFragment.this.onClick(mDialog, mWhich);
                if (getCustomizablePreference().onDismissDialog(mDialog, mWhich)) {
                    mDialog.dismiss();
                }
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            if (getDialog() instanceof AlertDialog) {
                AlertDialog a = (AlertDialog)getDialog();
                if (a.getButton(Dialog.BUTTON_NEUTRAL) != null) {
                    a.getButton(Dialog.BUTTON_NEUTRAL).setOnClickListener(
                            new OnDismissListener(a, Dialog.BUTTON_NEUTRAL));
                }
                if (a.getButton(Dialog.BUTTON_POSITIVE) != null) {
                    a.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener(
                            new OnDismissListener(a, Dialog.BUTTON_POSITIVE));
                }
                if (a.getButton(Dialog.BUTTON_NEGATIVE) != null) {
                    a.getButton(Dialog.BUTTON_NEGATIVE).setOnClickListener(
                            new OnDismissListener(a, Dialog.BUTTON_NEGATIVE));
                }
            }
            getCustomizablePreference().onStart();
        }

        @Override
        public void onStop() {
            super.onStop();
            getCustomizablePreference().onStop();
        }

        @Override
        public void onPause() {
            super.onPause();
            getCustomizablePreference().onPause();
        }

        @Override
        public void onResume() {
            super.onResume();
            getCustomizablePreference().onResume();
        }

        @Override
        protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
            super.onPrepareDialogBuilder(builder);
            getCustomizablePreference().setFragment(this);
            getCustomizablePreference().onPrepareDialogBuilder(builder, this);
        }

        @Override
        public void onDialogClosed(boolean positiveResult) {
            getCustomizablePreference().onDialogClosed(positiveResult);
        }

        @Override
        protected void onBindDialogView(View view) {
            super.onBindDialogView(view);
            getCustomizablePreference().onBindDialogView(view);
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            super.onClick(dialog, which);
            getCustomizablePreference().onClick(dialog, which);
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            getCustomizablePreference().setFragment(this);
            final Dialog sub = getCustomizablePreference().onCreateDialog(savedInstanceState);
            if (sub == null) {
                return super.onCreateDialog(savedInstanceState);
            }
            return sub;
        }

        @Override
        protected View onCreateDialogView(Context context) {
            final View v = getCustomizablePreference().onCreateDialogView(context);
            if (v == null) {
                return super.onCreateDialogView(context);
            }
            return v;
        }
    }
}
