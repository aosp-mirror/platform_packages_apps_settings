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

package com.android.settings.applications.defaultapps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.PackageManagerWrapper;
import com.android.settings.applications.PackageManagerWrapperImpl;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.widget.RadioButtonPreference;

import java.util.List;
import java.util.Map;

/**
 * A generic app picker fragment that shows a list of app as radio button group.
 */
public abstract class DefaultAppPickerFragment extends InstrumentedPreferenceFragment implements
        RadioButtonPreference.OnClickListener {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static final String EXTRA_FOR_WORK = "for_work";

    private final Map<String, DefaultAppInfo> mCandidates = new ArrayMap<>();

    protected PackageManagerWrapper mPm;
    protected UserManager mUserManager;
    protected int mUserId;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPm = new PackageManagerWrapperImpl(context.getPackageManager());
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        final Bundle arguments = getArguments();

        boolean mForWork = false;
        if (arguments != null) {
            mForWork = arguments.getBoolean(EXTRA_FOR_WORK);
        }
        final UserHandle managedProfile = Utils.getManagedProfile(mUserManager);
        mUserId = mForWork && managedProfile != null
                ? managedProfile.getIdentifier()
                : UserHandle.myUserId();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        addPreferencesFromResource(R.xml.app_picker_prefs);
        mCandidates.clear();
        final List<DefaultAppInfo> candidateList = getCandidates();
        if (candidateList != null) {
            for (DefaultAppInfo info : candidateList) {
                mCandidates.put(info.getKey(), info);
            }
        }
        final String defaultAppKey = getDefaultAppKey();
        final String systemDefaultAppKey = getSystemDefaultAppKey();
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        if (shouldShowItemNone()) {
            final RadioButtonPreference nonePref = new RadioButtonPreference(getPrefContext());
            nonePref.setIcon(R.drawable.ic_remove_circle);
            nonePref.setTitle(R.string.app_list_preference_none);
            nonePref.setChecked(TextUtils.isEmpty(defaultAppKey));
            nonePref.setOnClickListener(this);
            screen.addPreference(nonePref);
        }
        for (Map.Entry<String, DefaultAppInfo> app : mCandidates.entrySet()) {
            final RadioButtonPreference pref = new RadioButtonPreference(getPrefContext());
            final String appKey = app.getKey();

            pref.setTitle(app.getValue().loadLabel(mPm.getPackageManager()));
            pref.setIcon(app.getValue().loadIcon(mPm.getPackageManager()));
            pref.setKey(appKey);
            if (TextUtils.equals(defaultAppKey, appKey)) {
                pref.setChecked(true);
            }
            if (TextUtils.equals(systemDefaultAppKey, appKey)) {
                pref.setSummary(R.string.system_app);
            }
            pref.setOnClickListener(this);
            screen.addPreference(pref);
        }
        mayCheckOnlyRadioButton();
    }

    @Override
    public void onRadioButtonClicked(RadioButtonPreference selected) {
        final String selectedKey = selected.getKey();
        final String confirmationMessage = getConfirmationMessage(mCandidates.get(selectedKey));
        final Activity activity = getActivity();
        if (TextUtils.isEmpty(confirmationMessage)) {
            onRadioButtonConfirmed(selectedKey);
        } else if (activity != null) {
            final DialogFragment fragment = ConfirmationDialogFragment.newInstance(
                    this, selectedKey, confirmationMessage);
            fragment.show(activity.getFragmentManager(), ConfirmationDialogFragment.TAG);
        }
    }

    private void onRadioButtonConfirmed(String selectedKey) {
        final boolean success = setDefaultAppKey(selectedKey);
        if (success) {
            final PreferenceScreen screen = getPreferenceScreen();
            if (screen != null) {
                final int count = screen.getPreferenceCount();
                for (int i = 0; i < count; i++) {
                    final Preference pref = screen.getPreference(i);
                    if (pref instanceof RadioButtonPreference) {
                        final RadioButtonPreference radioPref = (RadioButtonPreference) pref;
                        final boolean newCheckedState =
                                TextUtils.equals(pref.getKey(), selectedKey);
                        if (radioPref.isChecked() != newCheckedState) {
                            radioPref.setChecked(TextUtils.equals(pref.getKey(), selectedKey));
                        }
                    }
                }
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void mayCheckOnlyRadioButton() {
        final PreferenceScreen screen = getPreferenceScreen();
        // If there is only 1 thing on screen, select it.
        if (screen != null && screen.getPreferenceCount() == 1) {
            final Preference onlyPref = screen.getPreference(0);
            if (onlyPref instanceof RadioButtonPreference) {
                ((RadioButtonPreference) onlyPref).setChecked(true);
            }
        }
    }

    protected boolean shouldShowItemNone() {
        return false;
    }

    protected String getSystemDefaultAppKey() {
        return null;
    }

    protected abstract List<DefaultAppInfo> getCandidates();

    protected abstract String getDefaultAppKey();

    protected abstract boolean setDefaultAppKey(String key);

    protected String getConfirmationMessage(DefaultAppInfo appInfo) {
        return null;
    }

    public static class ConfirmationDialogFragment extends InstrumentedDialogFragment
            implements DialogInterface.OnClickListener {

        public static final String TAG = "DefaultAppConfirm";
        public static final String EXTRA_KEY = "extra_key";
        public static final String EXTRA_MESSAGE = "extra_message";

        @Override
        public int getMetricsCategory() {
            return MetricsProto.MetricsEvent.DEFAULT_APP_PICKER_CONFIRMATION_DIALOG;
        }

        public static ConfirmationDialogFragment newInstance(DefaultAppPickerFragment parent,
                String key, String message) {
            final ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
            final Bundle argument = new Bundle();
            argument.putString(EXTRA_KEY, key);
            argument.putString(EXTRA_MESSAGE, message);
            fragment.setArguments(argument);
            fragment.setTargetFragment(parent, 0);
            return fragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle bundle = getArguments();
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setMessage(bundle.getString(EXTRA_MESSAGE))
                    .setPositiveButton(android.R.string.ok, this)
                    .setNegativeButton(android.R.string.cancel, null);
            return builder.create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            final Fragment fragment = getTargetFragment();
            if (fragment instanceof DefaultAppPickerFragment) {
                final Bundle bundle = getArguments();
                ((DefaultAppPickerFragment) fragment).onRadioButtonConfirmed(
                        bundle.getString(EXTRA_KEY));
            }
        }
    }

}
