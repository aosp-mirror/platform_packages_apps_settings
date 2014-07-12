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
import android.util.Log;
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
    private static final String TAG = InputMethodAndSubtypeEnabler.class.getSimpleName();
    private AlertDialog mDialog = null;
    private boolean mHaveHardKeyboard;
    final private HashMap<String, List<Preference>> mInputMethodAndSubtypePrefsMap =
            new HashMap<>();
    final private HashMap<String, CheckBoxPreference> mAutoSelectionPrefsMap = new HashMap<>();
    private InputMethodManager mImm;
    // TODO: Change mInputMethodInfoList to Map
    private List<InputMethodInfo> mInputMethodInfoList;
    private String mInputMethodId;

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
        mInputMethodId = getStringExtraFromIntentOrArguments(
                android.provider.Settings.EXTRA_INPUT_METHOD_ID);

        mInputMethodInfoList = mImm.getInputMethodList();
        setPreferenceScreen(createPreferenceHierarchy());
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
        updateAutoSelectionCB();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Clear all subtypes of all IMEs to make sure
        clearImplicitlyEnabledSubtypes(null);
        InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(this, getContentResolver(),
                mInputMethodInfoList, mHaveHardKeyboard);
    }

    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            final Preference preference) {

        if (preference instanceof CheckBoxPreference) {
            final CheckBoxPreference chkPref = (CheckBoxPreference) preference;

            for (final String imiId : mAutoSelectionPrefsMap.keySet()) {
                if (mAutoSelectionPrefsMap.get(imiId) == chkPref) {
                    // We look for the first preference item in subtype enabler.
                    // The first item is used for turning on/off subtype auto selection.
                    // We are in the subtype enabler and trying selecting subtypes automatically.
                    setSubtypeAutoSelectionEnabled(imiId, chkPref.isChecked());
                    return super.onPreferenceTreeClick(preferenceScreen, preference);
                }
            }

            final String id = chkPref.getKey();
            if (chkPref.isChecked()) {
                InputMethodInfo selImi = null;
                final int N = mInputMethodInfoList.size();
                for (int i = 0; i < N; i++) {
                    final InputMethodInfo imi = mInputMethodInfoList.get(i);
                    if (id.equals(imi.getId())) {
                        selImi = imi;
                        if (InputMethodUtils.isSystemIme(imi)) {
                            InputMethodAndSubtypeUtil.setSubtypesPreferenceEnabled(
                                    this, mInputMethodInfoList, id, true);
                            // This is a built-in IME, so no need to warn.
                            return super.onPreferenceTreeClick(preferenceScreen, preference);
                        }
                        break;
                    }
                }
                if (selImi == null) {
                    return super.onPreferenceTreeClick(preferenceScreen, preference);
                }
                chkPref.setChecked(false);
                if (mDialog == null) {
                    mDialog = (new AlertDialog.Builder(getActivity()))
                            .setTitle(android.R.string.dialog_alert_title)
                            .setCancelable(true)
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            chkPref.setChecked(true);
                                            InputMethodAndSubtypeUtil.setSubtypesPreferenceEnabled(
                                                    InputMethodAndSubtypeEnabler.this,
                                                    mInputMethodInfoList, id, true);
                                        }

                            })
                            .setNegativeButton(android.R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }

                            })
                            .create();
                } else {
                    if (mDialog.isShowing()) {
                        mDialog.dismiss();
                    }
                }
                mDialog.setMessage(getResources().getString(
                        R.string.ime_security_warning,
                        selImi.getServiceInfo().applicationInfo.loadLabel(getPackageManager())));
                mDialog.show();
            } else {
                InputMethodAndSubtypeUtil.setSubtypesPreferenceEnabled(
                        this, mInputMethodInfoList, id, false);
                updateAutoSelectionCB();
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private PreferenceScreen createPreferenceHierarchy() {
        // Root
        final PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
        final Context context = getActivity();

        final Collator collator = Collator.getInstance();
        final int N = (mInputMethodInfoList == null ? 0 : mInputMethodInfoList.size());
        for (int i = 0; i < N; ++i) {
            final InputMethodInfo imi = mInputMethodInfoList.get(i);
            final int subtypeCount = imi.getSubtypeCount();
            if (subtypeCount <= 1) {
                continue;
            }
            final String imiId = imi.getId();
            // Add this subtype to the list when no IME is specified or when the IME of this
            // subtype is the specified IME.
            if (!TextUtils.isEmpty(mInputMethodId) && !mInputMethodId.equals(imiId)) {
                continue;
            }
            final PreferenceCategory keyboardSettingsCategory = new PreferenceCategory(context);
            root.addPreference(keyboardSettingsCategory);
            final PackageManager pm = getPackageManager();
            final CharSequence label = imi.loadLabel(pm);

            keyboardSettingsCategory.setTitle(label);
            keyboardSettingsCategory.setKey(imiId);
            // TODO: Use toggle Preference if images are ready.
            final CheckBoxPreference autoCB = new CheckBoxPreference(context);
            mAutoSelectionPrefsMap.put(imiId, autoCB);
            keyboardSettingsCategory.addPreference(autoCB);

            final PreferenceCategory activeInputMethodsCategory = new PreferenceCategory(context);
            activeInputMethodsCategory.setTitle(R.string.active_input_method_subtypes);
            root.addPreference(activeInputMethodsCategory);

            boolean isAutoSubtype = false;
            CharSequence autoSubtypeLabel = null;
            final ArrayList<Preference> subtypePreferences = new ArrayList<>();
            if (subtypeCount > 0) {
                for (int j = 0; j < subtypeCount; ++j) {
                    final InputMethodSubtype subtype = imi.getSubtypeAt(j);
                    if (subtype.overridesImplicitlyEnabledSubtype()) {
                        if (!isAutoSubtype) {
                            isAutoSubtype = true;
                            autoSubtypeLabel = subtype.getDisplayName(context,
                                    imi.getPackageName(), imi.getServiceInfo().applicationInfo);
                        }
                    } else {
                        final CheckBoxPreference chkbxPref = new InputMethodSubtypePreference(
                                context, subtype, imi);
                        subtypePreferences.add(chkbxPref);
                    }
                }
                Collections.sort(subtypePreferences, new Comparator<Preference>() {
                    @Override
                    public int compare(Preference lhs, Preference rhs) {
                        if (lhs instanceof InputMethodSubtypePreference) {
                            return ((InputMethodSubtypePreference)lhs).compareTo(rhs, collator);
                        }
                        return lhs.compareTo(rhs);
                    }
                });
                for (int j = 0; j < subtypePreferences.size(); ++j) {
                    activeInputMethodsCategory.addPreference(subtypePreferences.get(j));
                }
                mInputMethodAndSubtypePrefsMap.put(imiId, subtypePreferences);
            }
            if (isAutoSubtype) {
                if (TextUtils.isEmpty(autoSubtypeLabel)) {
                    Log.w(TAG, "Title for auto subtype is empty.");
                    autoCB.setTitle("---");
                } else {
                    autoCB.setTitle(autoSubtypeLabel);
                }
            } else {
                autoCB.setTitle(R.string.use_system_language_to_select_input_method_subtypes);
            }
        }
        return root;
    }

    private boolean isNoSubtypesExplicitlySelected(String imiId) {
        boolean allSubtypesOff = true;
        final List<Preference> subtypePrefs = mInputMethodAndSubtypePrefsMap.get(imiId);
        for (final Preference pref : subtypePrefs) {
            if (pref instanceof CheckBoxPreference && ((CheckBoxPreference)pref).isChecked()) {
                allSubtypesOff = false;
                break;
            }
        }
        return allSubtypesOff;
    }

    private void setSubtypeAutoSelectionEnabled(String imiId, boolean autoSelectionEnabled) {
        final CheckBoxPreference autoSelectionCB = mAutoSelectionPrefsMap.get(imiId);
        if (autoSelectionCB == null) {
            return;
        }
        autoSelectionCB.setChecked(autoSelectionEnabled);
        final List<Preference> subtypePrefs = mInputMethodAndSubtypePrefsMap.get(imiId);
        for (final Preference subtypePref : subtypePrefs) {
            if (subtypePref instanceof CheckBoxPreference) {
                // When autoSelectionEnabled is true, all subtype prefs need to be disabled with
                // implicitly checked subtypes. In case of false, all subtype prefs need to be
                // enabled.
                subtypePref.setEnabled(!autoSelectionEnabled);
                if (autoSelectionEnabled) {
                    ((CheckBoxPreference)subtypePref).setChecked(false);
                }
            }
        }
        if (autoSelectionEnabled) {
            InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(
                    this, getContentResolver(), mInputMethodInfoList, mHaveHardKeyboard);
            setCheckedImplicitlyEnabledSubtypes(imiId);
        }
    }

    private void setCheckedImplicitlyEnabledSubtypes(String targetImiId) {
        updateImplicitlyEnabledSubtypes(targetImiId, true);
    }

    private void clearImplicitlyEnabledSubtypes(String targetImiId) {
        updateImplicitlyEnabledSubtypes(targetImiId, false);
    }

    private void updateImplicitlyEnabledSubtypes(String targetImiId, boolean check) {
        // When targetImiId is null, apply to all subtypes of all IMEs
        for (final InputMethodInfo imi : mInputMethodInfoList) {
            final String imiId = imi.getId();
            if (targetImiId != null && !targetImiId.equals(imiId)) {
                continue;
            }
            final CheckBoxPreference autoCB = mAutoSelectionPrefsMap.get(imiId);
            // No need to update implicitly enabled subtypes when the user has unchecked the
            // "subtype auto selection".
            if (autoCB == null || !autoCB.isChecked()) {
                continue;
            }
            final List<Preference> subtypePrefs = mInputMethodAndSubtypePrefsMap.get(imiId);
            final List<InputMethodSubtype> implicitlyEnabledSubtypes =
                    mImm.getEnabledInputMethodSubtypeList(imi, true);
            if (subtypePrefs == null || implicitlyEnabledSubtypes == null) {
                continue;
            }
            for (final Preference subtypePref : subtypePrefs) {
                if (subtypePref instanceof CheckBoxPreference) {
                    final CheckBoxPreference cb = (CheckBoxPreference)subtypePref;
                    cb.setChecked(false);
                    if (check) {
                        for (final InputMethodSubtype subtype : implicitlyEnabledSubtypes) {
                            String implicitlyEnabledSubtypePrefKey = imiId + subtype.hashCode();
                            if (cb.getKey().equals(implicitlyEnabledSubtypePrefKey)) {
                                cb.setChecked(true);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateAutoSelectionCB() {
        for (final String imiId : mInputMethodAndSubtypePrefsMap.keySet()) {
            setSubtypeAutoSelectionEnabled(imiId, isNoSubtypesExplicitlySelected(imiId));
        }
        setCheckedImplicitlyEnabledSubtypes(null);
    }
}
