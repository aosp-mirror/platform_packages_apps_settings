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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.content.Context;

import com.android.settings.SettingsPreferenceFragment.SettingsDialogFragment;
import com.android.settings.DevelopmentSettings;

import java.util.Locale;

public class LocalePicker extends com.android.internal.app.LocalePicker
        implements com.android.internal.app.LocalePicker.LocaleSelectionListener,
        DialogCreatable {

    private static final String TAG = "LocalePicker";

    private SettingsDialogFragment mDialogFragment;
    private static final int DLG_SHOW_GLOBAL_WARNING = 1;
    private static final String SAVE_TARGET_LOCALE = "locale";

    private Locale mTargetLocale;

    public LocalePicker() {
        super();
        setLocaleSelectionListener(this);
    }

    @Override
    protected boolean isInDeveloperMode() {
        final boolean showDev = getActivity().getSharedPreferences(DevelopmentSettings.PREF_FILE,
                Context.MODE_PRIVATE).getBoolean(
                DevelopmentSettings.PREF_SHOW,
                android.os.Build.TYPE.equals("eng"));
        return showDev;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey(SAVE_TARGET_LOCALE)) {
            mTargetLocale = new Locale(savedInstanceState.getString(SAVE_TARGET_LOCALE));
        }
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        final ListView list = (ListView) view.findViewById(android.R.id.list);
        Utils.forcePrepareCustomPreferencesList(container, view, list, false);
        return view;
    }

    @Override
    public void onLocaleSelected(final Locale locale) {
        if (Utils.hasMultipleUsers(getActivity())) {
            mTargetLocale = locale;
            showDialog(DLG_SHOW_GLOBAL_WARNING);
        } else {
            getActivity().onBackPressed();
            LocalePicker.updateLocale(locale);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mTargetLocale != null) {
            outState.putString(SAVE_TARGET_LOCALE, mTargetLocale.toString());
        }
    }

    protected void showDialog(int dialogId) {
        if (mDialogFragment != null) {
            Log.e(TAG, "Old dialog fragment not null!");
        }
        mDialogFragment = new SettingsDialogFragment(this, dialogId);
        mDialogFragment.show(getActivity().getFragmentManager(), Integer.toString(dialogId));
    }

    public Dialog onCreateDialog(final int dialogId) {
        return Utils.buildGlobalChangeWarningDialog(getActivity(),
                R.string.global_locale_change_title,
                new Runnable() {
                    public void run() {
                        removeDialog(dialogId);
                        getActivity().onBackPressed();
                        LocalePicker.updateLocale(mTargetLocale);
                    }
                }
        );
    }

    protected void removeDialog(int dialogId) {
        // mDialogFragment may not be visible yet in parent fragment's onResume().
        // To be able to dismiss dialog at that time, don't check
        // mDialogFragment.isVisible().
        if (mDialogFragment != null && mDialogFragment.getDialogId() == dialogId) {
            mDialogFragment.dismiss();
        }
        mDialogFragment = null;
    }
}
