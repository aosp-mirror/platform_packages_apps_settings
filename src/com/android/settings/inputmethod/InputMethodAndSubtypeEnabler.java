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

package com.android.settings.inputmethod;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import com.android.internal.inputmethod.InputMethodUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class InputMethodAndSubtypeEnabler extends SettingsPreferenceFragment {
    private boolean mHaveHardKeyboard;
    private final HashMap<String, List<Preference>> mInputMethodAndSubtypePrefsMap =
            new HashMap<>();
    private final HashMap<String, CheckBoxPreference> mAutoSelectionPrefsMap = new HashMap<>();
    private InputMethodManager mImm;
    // TODO: Change mInputMethodInfoList to Map
    private List<InputMethodInfo> mInputMethodInfoList;
    private Collator mCollator;
    private AlertDialog mDialog = null;

    @Override
    public void onCreate(final Bundle icicle) {
        super.onCreate(icicle);
        mImm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        final Configuration config = getResources().getConfiguration();
        mHaveHardKeyboard = (config.keyboard == Configuration.KEYBOARD_QWERTY);

        // Input method id should be available from an Intent when this preference is launched as a
        // single Activity (see InputMethodAndSubtypeEnablerActivity). It should be available
        // from a preference argument when the preference is launched as a part of the other
        // Activity (like a right pane of 2-pane Settings app)
        final String targetImi = getStringExtraFromIntentOrArguments(
                android.provider.Settings.EXTRA_INPUT_METHOD_ID);

        mInputMethodInfoList = mImm.getInputMethodList();
        mCollator = Collator.getInstance();

        final PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
        final int imiCount = mInputMethodInfoList.size();
        for (int index = 0; index < imiCount; ++index) {
            final InputMethodInfo imi = mInputMethodInfoList.get(index);
            // Add subtype preferences of this IME when it is specified or no IME is specified.
            if (imi.getId().equals(targetImi) || TextUtils.isEmpty(targetImi)) {
                addInputMethodSubtypePreferences(imi, root);
            }
        }
        setPreferenceScreen(root);
    }

    private String getStringExtraFromIntentOrArguments(final String name) {
        final Intent intent = getActivity().getIntent();
        final String fromIntent = intent.getStringExtra(name);
        if (fromIntent != null) {
            return fromIntent;
        }
        final Bundle arguments = getArguments();
        return (arguments == null) ? null : arguments.getString(name);
    }

    @Override
    public void onActivityCreated(final Bundle icicle) {
        super.onActivityCreated(icicle);
        final String title = getStringExtraFromIntentOrArguments(Intent.EXTRA_TITLE);
        if (!TextUtils.isEmpty(title)) {
            getActivity().setTitle(title);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh internal states in mInputMethodSettingValues to keep the latest
        // "InputMethodInfo"s and "InputMethodSubtype"s
        InputMethodSettingValuesWrapper
                .getInstance(getActivity()).refreshAllInputMethodAndSubtypes();
        InputMethodAndSubtypeUtil.loadInputMethodSubtypeList(
                this, getContentResolver(), mInputMethodInfoList, mInputMethodAndSubtypePrefsMap);
        updateAutoSelectionPreferences();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Clear all subtypes of all IMEs to make sure
        updateImplicitlyEnabledSubtypes(null /* targetImiId */, false /* check */);
        InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(this, getContentResolver(),
                mInputMethodInfoList, mHaveHardKeyboard);
    }

    // TODO: Stop overriding this method. Instead start using {@link OnPreferenceChangedListener}.
    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            final Preference preference) {
        if (!(preference instanceof CheckBoxPreference)) {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        final CheckBoxPreference chkPref = (CheckBoxPreference) preference;

        for (final String imiId : mAutoSelectionPrefsMap.keySet()) {
            if (mAutoSelectionPrefsMap.get(imiId) == chkPref) {
                // We look for the first preference item in subtype enabler. The first item is used
                // for turning on/off subtype auto selection. We are in the subtype enabler and
                // trying selecting subtypes automatically.
                setAutoSelectionSubtypesEnabled(imiId, chkPref.isChecked());
                return super.onPreferenceTreeClick(preferenceScreen, preference);
            }
        }

        final String id = chkPref.getKey();
        // Turns off a subtype.
        if (!chkPref.isChecked()) {
            // TODO: Because no preference on this screen has {@link InputMethodInfo} id as a key,
            // the following setSubtypesPreferenceEnabled call is effectively no-operation and
            // can be removed.
            InputMethodAndSubtypeUtil.setSubtypesPreferenceEnabled(
                    this, mInputMethodInfoList, id, false);
            updateAutoSelectionPreferences();
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        // Turns on a subtype.
        final InputMethodInfo imi = getInputMethodInfoById(id);
        // TODO: Because no preference on this screen has {@link InputMethodInfo} id as a key,
        // <code>imi</code> is always null and the following code can be removed.
        if (imi == null) {
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        // Turns on a system IME's subtype.
        if (InputMethodUtils.isSystemIme(imi)) {
            InputMethodAndSubtypeUtil.setSubtypesPreferenceEnabled(
                    this, mInputMethodInfoList, id, true);
            // This is a built-in IME, so no need to warn.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        // Turns on a 3rd party IME's subtype.
        // Turns off a subtype before showing a security warning dialog.
        chkPref.setChecked(false);
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(true);
        builder.setTitle(android.R.string.dialog_alert_title);
        final CharSequence label = imi.getServiceInfo().applicationInfo
                .loadLabel(getPackageManager());
        builder.setMessage(getString(R.string.ime_security_warning, label));
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                // The user explicitly enable the subtype.
                chkPref.setChecked(true);
                InputMethodAndSubtypeUtil.setSubtypesPreferenceEnabled(
                        InputMethodAndSubtypeEnabler.this, mInputMethodInfoList, id, true);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {}
        });
        mDialog = builder.create();
        mDialog.show();
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private InputMethodInfo getInputMethodInfoById(final String imiId) {
        final int imiCount = mInputMethodInfoList.size();
        for (int index = 0; index < imiCount; ++index) {
            final InputMethodInfo imi = mInputMethodInfoList.get(index);
            if (imi.getId().equals(imiId)) {
                return imi;
            }
        }
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private void addInputMethodSubtypePreferences(final InputMethodInfo imi,
            final PreferenceScreen root) {
        final Context context = getActivity();
        final int subtypeCount = imi.getSubtypeCount();
        if (subtypeCount <= 1) {
            return;
        }
        final String imiId = imi.getId();
        final PreferenceCategory keyboardSettingsCategory = new PreferenceCategory(context);
        root.addPreference(keyboardSettingsCategory);
        final PackageManager pm = getPackageManager();
        final CharSequence label = imi.loadLabel(pm);

        keyboardSettingsCategory.setTitle(label);
        keyboardSettingsCategory.setKey(imiId);
        // TODO: Use toggle Preference if images are ready.
        final CheckBoxPreference autoSelectionPref = new CheckBoxPreference(context);
        mAutoSelectionPrefsMap.put(imiId, autoSelectionPref);
        keyboardSettingsCategory.addPreference(autoSelectionPref);

        final PreferenceCategory activeInputMethodsCategory = new PreferenceCategory(context);
        activeInputMethodsCategory.setTitle(R.string.active_input_method_subtypes);
        root.addPreference(activeInputMethodsCategory);

        CharSequence autoSubtypeLabel = null;
        final ArrayList<Preference> subtypePreferences = new ArrayList<>();
        for (int index = 0; index < subtypeCount; ++index) {
            final InputMethodSubtype subtype = imi.getSubtypeAt(index);
            if (subtype.overridesImplicitlyEnabledSubtype()) {
                if (autoSubtypeLabel == null) {
                    autoSubtypeLabel = subtype.getDisplayName(
                            context, imi.getPackageName(), imi.getServiceInfo().applicationInfo);
                }
            } else {
                final Preference subtypePref = new InputMethodSubtypePreference(
                        context, subtype, imi);
                subtypePreferences.add(subtypePref);
            }
        }
        Collections.sort(subtypePreferences, new Comparator<Preference>() {
            @Override
            public int compare(final Preference lhs, final Preference rhs) {
                if (lhs instanceof InputMethodSubtypePreference) {
                    return ((InputMethodSubtypePreference) lhs).compareTo(rhs, mCollator);
                }
                return lhs.compareTo(rhs);
            }
        });
        final int prefCount = subtypePreferences.size();
        for (int index = 0; index < prefCount; ++index) {
            final Preference pref = subtypePreferences.get(index);
            activeInputMethodsCategory.addPreference(pref);
            InputMethodAndSubtypeUtil.removeUnnecessaryNonPersistentPreference(pref);
        }
        mInputMethodAndSubtypePrefsMap.put(imiId, subtypePreferences);
        if (TextUtils.isEmpty(autoSubtypeLabel)) {
            autoSelectionPref.setTitle(
                    R.string.use_system_language_to_select_input_method_subtypes);
        } else {
            autoSelectionPref.setTitle(autoSubtypeLabel);
        }
    }

    private boolean isNoSubtypesExplicitlySelected(final String imiId) {
        final List<Preference> subtypePrefs = mInputMethodAndSubtypePrefsMap.get(imiId);
        for (final Preference pref : subtypePrefs) {
            if (pref instanceof CheckBoxPreference && ((CheckBoxPreference)pref).isChecked()) {
                return false;
            }
        }
        return true;
    }

    private void setAutoSelectionSubtypesEnabled(final String imiId,
            final boolean autoSelectionEnabled) {
        final CheckBoxPreference autoSelectionPref = mAutoSelectionPrefsMap.get(imiId);
        if (autoSelectionPref == null) {
            return;
        }
        autoSelectionPref.setChecked(autoSelectionEnabled);
        final List<Preference> subtypePrefs = mInputMethodAndSubtypePrefsMap.get(imiId);
        for (final Preference pref : subtypePrefs) {
            if (pref instanceof CheckBoxPreference) {
                // When autoSelectionEnabled is true, all subtype prefs need to be disabled with
                // implicitly checked subtypes. In case of false, all subtype prefs need to be
                // enabled.
                pref.setEnabled(!autoSelectionEnabled);
                if (autoSelectionEnabled) {
                    ((CheckBoxPreference)pref).setChecked(false);
                }
            }
        }
        if (autoSelectionEnabled) {
            InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(
                    this, getContentResolver(), mInputMethodInfoList, mHaveHardKeyboard);
            updateImplicitlyEnabledSubtypes(imiId, true /* check */);
        }
    }

    private void updateImplicitlyEnabledSubtypes(final String targetImiId, final boolean check) {
        // When targetImiId is null, apply to all subtypes of all IMEs
        for (final InputMethodInfo imi : mInputMethodInfoList) {
            final String imiId = imi.getId();
            final CheckBoxPreference autoSelectionPref = mAutoSelectionPrefsMap.get(imiId);
            // No need to update implicitly enabled subtypes when the user has unchecked the
            // "subtype auto selection".
            if (autoSelectionPref == null || !autoSelectionPref.isChecked()) {
                continue;
            }
            if (imiId.equals(targetImiId) || targetImiId == null) {
                updateImplicitlyEnabledSubtypesOf(imi, check);
            }
        }
    }

    private void updateImplicitlyEnabledSubtypesOf(final InputMethodInfo imi, final boolean check) {
        final String imiId = imi.getId();
        final List<Preference> subtypePrefs = mInputMethodAndSubtypePrefsMap.get(imiId);
        final List<InputMethodSubtype> implicitlyEnabledSubtypes =
                mImm.getEnabledInputMethodSubtypeList(imi, true);
        if (subtypePrefs == null || implicitlyEnabledSubtypes == null) {
            return;
        }
        for (final Preference pref : subtypePrefs) {
            if (!(pref instanceof CheckBoxPreference)) {
                continue;
            }
            final CheckBoxPreference subtypePref = (CheckBoxPreference)pref;
            subtypePref.setChecked(false);
            if (check) {
                for (final InputMethodSubtype subtype : implicitlyEnabledSubtypes) {
                    final String implicitlyEnabledSubtypePrefKey = imiId + subtype.hashCode();
                    if (subtypePref.getKey().equals(implicitlyEnabledSubtypePrefKey)) {
                        subtypePref.setChecked(true);
                        break;
                    }
                }
            }
        }
    }

    private void updateAutoSelectionPreferences() {
        for (final String imiId : mInputMethodAndSubtypePrefsMap.keySet()) {
            setAutoSelectionSubtypesEnabled(imiId, isNoSubtypesExplicitlySelected(imiId));
        }
        updateImplicitlyEnabledSubtypes(null /* targetImiId */, true /* check */);
    }
}
