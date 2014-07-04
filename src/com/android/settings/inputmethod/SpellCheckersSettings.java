/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings.inputmethod;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.TextServicesManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;

public class SpellCheckersSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceClickListener {
    private static final String TAG = SpellCheckersSettings.class.getSimpleName();
    private static final boolean DBG = false;

    private AlertDialog mDialog = null;
    private SpellCheckerInfo mCurrentSci;
    private SpellCheckerInfo[] mEnabledScis;
    private TextServicesManager mTsm;
    private final ArrayList<SingleSpellCheckerPreference> mSpellCheckers = new ArrayList<>();

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        mTsm = (TextServicesManager) getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
        addPreferencesFromResource(R.xml.spellchecker_prefs);
        updateScreen();
    }

    // Override the behavior of {@link PreferenceFragment}.
    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen screen,
            final Preference preference) {
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateScreen();
    }

    private void updateScreen() {
        getPreferenceScreen().removeAll();
        updateEnabledSpellCheckers();
    }

    private void updateEnabledSpellCheckers() {
        mCurrentSci = mTsm.getCurrentSpellChecker();
        mEnabledScis = mTsm.getEnabledSpellCheckers();
        if (mCurrentSci == null || mEnabledScis == null) {
            return;
        }
        final PackageManager pm = getPackageManager();
        mSpellCheckers.clear();
        for (int i = 0; i < mEnabledScis.length; ++i) {
            final SpellCheckerInfo sci = mEnabledScis[i];
            final SingleSpellCheckerPreference scPref = new SingleSpellCheckerPreference(
                    this, sci, mTsm);
            mSpellCheckers.add(scPref);
            scPref.setTitle(sci.loadLabel(pm));
            scPref.setSelected(mCurrentSci != null && mCurrentSci.getId().equals(sci.getId()));
            getPreferenceScreen().addPreference(scPref);
        }
    }

    @Override
    public boolean onPreferenceClick(final Preference pref) {
        for (final SingleSpellCheckerPreference scp : mSpellCheckers) {
            if (pref.equals(scp)) {
                if (isSystemApp(scp.getSpellCheckerInfo())) {
                    changeCurrentSpellChecker(scp);
                } else {
                    showSecurityWarnDialog(scp);
                }
                return true;
            }
        }
        return true;
    }

    private void showSecurityWarnDialog(final SingleSpellCheckerPreference scp) {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(android.R.string.dialog_alert_title);
        final PackageManager pm = getPackageManager();
        builder.setMessage(getString(R.string.spellchecker_security_warning,
                scp.getSpellCheckerInfo().getServiceInfo().applicationInfo.loadLabel(pm)));
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                changeCurrentSpellChecker(scp);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
            }
        });
        mDialog = builder.create();
        mDialog.show();
    }

    private void changeCurrentSpellChecker(final SingleSpellCheckerPreference scp) {
        mTsm.setCurrentSpellChecker(scp.getSpellCheckerInfo());
        if (DBG) {
            Log.d(TAG, "Current spell check is " + mTsm.getCurrentSpellChecker().getId());
        }
        updateScreen();
    }

    private static boolean isSystemApp(final SpellCheckerInfo sci) {
        return (sci.getServiceInfo().applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
    }
}
