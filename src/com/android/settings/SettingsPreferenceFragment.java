/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

interface DialogCreatable {
    public Dialog onCreateDialog(int dialogId);
}

/**
 * Base class for Settings fragments, with some helper functions and dialog management.
 */
public class SettingsPreferenceFragment extends PreferenceFragment
        implements DialogCreatable {

    private static final String TAG = "SettingsPreferenceFragment";

    // Originally from PreferenceActivity.
    private static final String EXTRA_PREFS_SHOW_BUTTON_BAR = "extra_prefs_show_button_bar";
    private static final String EXTRA_PREFS_SHOW_SKIP = "extra_prefs_show_skip";
    private static final String EXTRA_PREFS_SET_NEXT_TEXT = "extra_prefs_set_next_text";
    private static final String EXTRA_PREFS_SET_BACK_TEXT = "extra_prefs_set_back_text";

    private SettingsDialogFragment mDialogFragment;

    private OnStateListener mOnStateListener;

    private Button mNextButton;

    private boolean mReportedCreation;

    interface OnStateListener {

        void onCreated(SettingsPreferenceFragment fragment);

        void onDestroyed(SettingsPreferenceFragment fragment);
    }

    public void setOnStateListener(OnStateListener listener) {
        mOnStateListener = listener;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mOnStateListener != null && !mReportedCreation) {
            mOnStateListener.onCreated(this);
            // So that we don't report it on the way back to this fragment
            mReportedCreation = true;
        }

        setupButtonBar();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mOnStateListener != null) {
            mOnStateListener.onDestroyed(this);
        }
    }

    // Some helpers for functions used by the settings fragments when they were activities

    /**
     * Returns the ContentResolver from the owning Activity.
     */
    protected ContentResolver getContentResolver() {
        return getActivity().getContentResolver();
    }

    /**
     * Returns the specified system service from the owning Activity.
     */
    protected Object getSystemService(final String name) {
        return getActivity().getSystemService(name);
    }

    /**
     * Returns the Resources from the owning Activity.
     */
    protected Resources getResources() {
        return getActivity().getResources();
    }

    /**
     * Returns the PackageManager from the owning Activity.
     */
    protected PackageManager getPackageManager() {
        return getActivity().getPackageManager();
    }

    // Dialog management

    protected void showDialog(int dialogId) {
        if (mDialogFragment != null) {
            Log.e(TAG, "Old dialog fragment not null!");
        }
        mDialogFragment = new SettingsDialogFragment(this, dialogId);
        mDialogFragment.show(getActivity().getFragmentManager(), Integer.toString(dialogId));
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        return null;
    }

    protected void removeDialog(int dialogId) {
        if (mDialogFragment != null && mDialogFragment.getDialogId() == dialogId
                && mDialogFragment.isVisible()) {
            mDialogFragment.dismiss();
        }
        mDialogFragment = null;
    }

    static class SettingsDialogFragment extends DialogFragment {
        private int mDialogId;

        private DialogCreatable mFragment;

        SettingsDialogFragment(DialogCreatable fragment, int dialogId) {
            mDialogId = dialogId;
            mFragment = fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mFragment.onCreateDialog(mDialogId);
        }

        public int getDialogId() {
            return mDialogId;
        }
    }

    protected boolean hasNextButton() {
        return mNextButton != null;
    }

    protected Button getNextButton() {
        return mNextButton;
    }

    /**
     * Sets up Button Bar possibly required in the Fragment. Probably available only in
     * phones.
     *
     * Previously {@link PreferenceActivity} had the capability as hidden functionality.
     */
    private void setupButtonBar() {
        // Originally from PreferenceActivity, which has had button bar inside its layout.
        final Activity activity = getActivity();
        final Intent intent = activity.getIntent();
        final View buttonBar = activity.findViewById(com.android.internal.R.id.button_bar);
        if (!intent.getBooleanExtra(EXTRA_PREFS_SHOW_BUTTON_BAR, false) || buttonBar == null) {
            return;
        }

        buttonBar.setVisibility(View.VISIBLE);
        View tmpView = activity.findViewById(com.android.internal.R.id.back_button);
        if (tmpView != null) {
            // TODO: Assume this is pressed only in single pane, finishing current Activity.
            try {
                final Button backButton = (Button)tmpView;
                backButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        activity.setResult(Activity.RESULT_CANCELED);
                        activity.finish();
                    }
                });
                if (intent.hasExtra(EXTRA_PREFS_SET_BACK_TEXT)) {
                    String buttonText = intent.getStringExtra(EXTRA_PREFS_SET_BACK_TEXT);
                    if (TextUtils.isEmpty(buttonText)) {
                        backButton.setVisibility(View.GONE);
                    }
                    else {
                        backButton.setText(buttonText);
                    }
                }
            } catch (ClassCastException e) {
                Log.w(TAG, "The view originally for back_button is used not as Button. " +
                        "Ignored.");
            }
        }

        tmpView = activity.findViewById(com.android.internal.R.id.skip_button);
        if (tmpView != null) {
            try {
                final Button skipButton = (Button)tmpView;
                skipButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        activity.setResult(Activity.RESULT_OK);
                        activity.finish();
                    }
                });
                if (intent.getBooleanExtra(EXTRA_PREFS_SHOW_SKIP, false)) {
                    skipButton.setVisibility(View.VISIBLE);
                }
            } catch (ClassCastException e) {
                Log.w(TAG, "The view originally for skip_button is used not as Button. " +
                        "Ignored.");
            }
        }

        tmpView = activity.findViewById(com.android.internal.R.id.next_button);
        if (tmpView != null) {
            try {
                mNextButton = (Button)tmpView;
                mNextButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        activity.setResult(Activity.RESULT_OK);
                        activity.finish();
                    }
                });
                // set our various button parameters
                if (intent.hasExtra(EXTRA_PREFS_SET_NEXT_TEXT)) {
                    String buttonText = intent.getStringExtra(EXTRA_PREFS_SET_NEXT_TEXT);
                    if (TextUtils.isEmpty(buttonText)) {
                        mNextButton.setVisibility(View.GONE);
                    }
                    else {
                        mNextButton.setText(buttonText);
                    }
                }
            } catch (ClassCastException e) {
                Log.w(TAG, "The view originally for next_button is used not as Button. " +
                        "Ignored.");
                mNextButton = null;
            }
        }
    }
}
