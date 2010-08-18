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

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.util.Log;

/**
 * Base class for Settings fragments, with some helper functions and dialog management.
 */
public class SettingsPreferenceFragment extends PreferenceFragment {

    private static final String TAG = "SettingsPreferenceFragment";

    private SettingsDialogFragment mDialogFragment;

    private OnStateListener mOnStateListener;

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
        if (mOnStateListener != null) {
            mOnStateListener.onCreated(this);
        }
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
        mDialogFragment.show(getActivity(), Integer.toString(dialogId));
    }

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

        private SettingsPreferenceFragment mFragment;

        SettingsDialogFragment(SettingsPreferenceFragment fragment, int dialogId) {
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
}
