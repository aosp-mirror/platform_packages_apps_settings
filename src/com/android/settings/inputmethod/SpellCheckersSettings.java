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

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.SpellCheckerSubtype;
import android.view.textservice.TextServicesManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SettingsMainSwitchBar;

public class SpellCheckersSettings extends SettingsPreferenceFragment
        implements OnCheckedChangeListener, OnPreferenceChangeListener {
    private static final String TAG = SpellCheckersSettings.class.getSimpleName();
    private static final boolean DBG = false;

    private static final String KEY_SPELL_CHECKER_LANGUAGE = "spellchecker_language";
    private static final String KEY_DEFAULT_SPELL_CHECKER = "default_spellchecker";
    private static final int ITEM_ID_USE_SYSTEM_LANGUAGE = 0;

    private SettingsMainSwitchBar mSwitchBar;
    private Preference mSpellCheckerLanaguagePref;
    private AlertDialog mDialog = null;
    private SpellCheckerInfo mCurrentSci;
    private SpellCheckerInfo[] mEnabledScis;
    private TextServicesManager mTsm;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.INPUTMETHOD_SPELL_CHECKERS;
    }

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.spellchecker_prefs);
        mSpellCheckerLanaguagePref = findPreference(KEY_SPELL_CHECKER_LANGUAGE);

        mTsm = (TextServicesManager) getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE);
        mCurrentSci = mTsm.getCurrentSpellChecker();
        mEnabledScis = mTsm.getEnabledSpellCheckers();
        populatePreferenceScreen();
    }

    private void populatePreferenceScreen() {
        final SpellCheckerPreference pref = new SpellCheckerPreference(getPrefContext(),
                mEnabledScis);
        pref.setTitle(R.string.default_spell_checker);
        final int count = (mEnabledScis == null) ? 0 : mEnabledScis.length;
        if (count > 0) {
            pref.setSummary("%s");
        } else {
            pref.setSummary(R.string.spell_checker_not_selected);
        }
        pref.setKey(KEY_DEFAULT_SPELL_CHECKER);
        pref.setOnPreferenceChangeListener(this);
        getPreferenceScreen().addPreference(pref);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        mSwitchBar.setTitle(getContext().getString(R.string.spell_checker_primary_switch_title));
        mSwitchBar.show();
        mSwitchBar.addOnSwitchChangeListener(this);
        updatePreferenceScreen();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSwitchBar.removeOnSwitchChangeListener(this);
    }

    @Override
    public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.SPELL_CHECKER_ENABLED,
                isChecked ? 1 : 0);
        updatePreferenceScreen();
    }

    private void updatePreferenceScreen() {
        mCurrentSci = mTsm.getCurrentSpellChecker();
        final boolean isSpellCheckerEnabled = mTsm.isSpellCheckerEnabled();
        mSwitchBar.setChecked(isSpellCheckerEnabled);

        final SpellCheckerSubtype currentScs;
        if (mCurrentSci != null) {
            currentScs = mTsm.getCurrentSpellCheckerSubtype(
                    false /* allowImplicitlySelectedSubtype */);
        } else {
            currentScs = null;
        }
        mSpellCheckerLanaguagePref.setSummary(getSpellCheckerSubtypeLabel(mCurrentSci, currentScs));

        final PreferenceScreen screen = getPreferenceScreen();
        final int count = screen.getPreferenceCount();
        for (int index = 0; index < count; index++) {
            final Preference preference = screen.getPreference(index);
            preference.setEnabled(isSpellCheckerEnabled);
            if (preference instanceof SpellCheckerPreference) {
                final SpellCheckerPreference pref = (SpellCheckerPreference) preference;
                pref.setSelected(mCurrentSci);
                pref.setEnabled(mEnabledScis != null);
            }
        }
        mSpellCheckerLanaguagePref.setEnabled(isSpellCheckerEnabled && mCurrentSci != null);
    }

    private CharSequence getSpellCheckerSubtypeLabel(final SpellCheckerInfo sci,
            final SpellCheckerSubtype subtype) {
        if (sci == null) {
            return getString(R.string.spell_checker_not_selected);
        }
        if (subtype == null) {
            return getString(com.android.settingslib.R
                    .string.use_system_language_to_select_input_method_subtypes);
        }
        return subtype.getDisplayName(
                getActivity(), sci.getPackageName(), sci.getServiceInfo().applicationInfo);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (KEY_SPELL_CHECKER_LANGUAGE.equals(preference.getKey())) {
            writePreferenceClickMetric(preference);
            showChooseLanguageDialog();
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final SpellCheckerInfo sci = (SpellCheckerInfo) newValue;
        final boolean isSystemApp =
                (sci.getServiceInfo().applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        if (isSystemApp) {
            changeCurrentSpellChecker(sci);
            return true;
        } else {
            showSecurityWarnDialog(sci);
            return false;
        }
    }

    private static int convertSubtypeIndexToDialogItemId(final int index) {
        return index + 1;
    }

    private static int convertDialogItemIdToSubtypeIndex(final int item) {
        return item - 1;
    }

    private void showChooseLanguageDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        final SpellCheckerInfo currentSci = mTsm.getCurrentSpellChecker();
        if (currentSci == null) {
            // This can happen in some situations.  One example is that the package that the current
            // spell checker belongs to was uninstalled or being in background.
            return;
        }
        final SpellCheckerSubtype currentScs = mTsm.getCurrentSpellCheckerSubtype(
                false /* allowImplicitlySelectedSubtype */);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.phone_language);
        final int subtypeCount = currentSci.getSubtypeCount();
        final CharSequence[] items = new CharSequence[subtypeCount + 1 /* default */];
        items[ITEM_ID_USE_SYSTEM_LANGUAGE] = getSpellCheckerSubtypeLabel(currentSci, null);
        int checkedItemId = ITEM_ID_USE_SYSTEM_LANGUAGE;
        for (int index = 0; index < subtypeCount; ++index) {
            final SpellCheckerSubtype subtype = currentSci.getSubtypeAt(index);
            final int itemId = convertSubtypeIndexToDialogItemId(index);
            items[itemId] = getSpellCheckerSubtypeLabel(currentSci, subtype);
            if (subtype.equals(currentScs)) {
                checkedItemId = itemId;
            }
        }
        builder.setSingleChoiceItems(items, checkedItemId, new AlertDialog.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int item) {
                final int subtypeId;
                if (item == ITEM_ID_USE_SYSTEM_LANGUAGE) {
                    subtypeId = SpellCheckerSubtype.SUBTYPE_ID_NONE;
                } else {
                    final int index = convertDialogItemIdToSubtypeIndex(item);
                    subtypeId = currentSci.getSubtypeAt(index).hashCode();
                }

                Settings.Secure.putInt(getContentResolver(),
                        Settings.Secure.SELECTED_SPELL_CHECKER_SUBTYPE, subtypeId);

                if (DBG) {
                    final SpellCheckerSubtype subtype = mTsm.getCurrentSpellCheckerSubtype(
                            true /* allowImplicitlySelectedSubtype */);
                    Log.d(TAG, "Current spell check locale is "
                            + subtype == null ? "null" : subtype.getLocale());
                }
                dialog.dismiss();
                updatePreferenceScreen();
            }
        });
        mDialog = builder.create();
        mDialog.show();
    }

    private void showSecurityWarnDialog(final SpellCheckerInfo sci) {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(android.R.string.dialog_alert_title);
        builder.setMessage(getString(R.string.spellchecker_security_warning,
                sci.loadLabel(getPackageManager())));
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                changeCurrentSpellChecker(sci);
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

    private void changeCurrentSpellChecker(final SpellCheckerInfo sci) {
        Settings.Secure.putString(getContentResolver(), Settings.Secure.SELECTED_SPELL_CHECKER,
                sci.getId());
        // Reset the spell checker subtype
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.SELECTED_SPELL_CHECKER_SUBTYPE,
                SpellCheckerSubtype.SUBTYPE_ID_NONE);
        if (DBG) {
            Log.d(TAG, "Current spell check is " + mTsm.getCurrentSpellChecker().getId());
        }
        updatePreferenceScreen();
    }
}
