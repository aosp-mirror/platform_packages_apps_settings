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

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.List;

public class InputMethodAndSubtypeEnabler extends SettingsPreferenceFragment {

    private boolean mHaveHardKeyboard;

    private List<InputMethodInfo> mInputMethodProperties;

    private AlertDialog mDialog = null;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Configuration config = getResources().getConfiguration();
        mHaveHardKeyboard = (config.keyboard == Configuration.KEYBOARD_QWERTY);
        onCreateIMM();
        setPreferenceScreen(createPreferenceHierarchy());
    }

    @Override
    public void onResume() {
        super.onResume();
        InputMethodAndSubtypeUtil.loadInputMethodSubtypeList(
                this, getContentResolver(), mInputMethodProperties);
    }

    @Override
    public void onPause() {
        super.onPause();
        InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(this, getContentResolver(),
                mInputMethodProperties, mHaveHardKeyboard);
    }

    @Override
    public boolean onPreferenceTreeClick(
            PreferenceScreen preferenceScreen, Preference preference) {

        if (preference instanceof CheckBoxPreference) {
            final CheckBoxPreference chkPref = (CheckBoxPreference) preference;
            final String id = chkPref.getKey();
            // TODO: Check subtype or not here
            if (chkPref.isChecked()) {
                InputMethodInfo selImi = null;
                final int N = mInputMethodProperties.size();
                for (int i = 0; i < N; i++) {
                    InputMethodInfo imi = mInputMethodProperties.get(i);
                    if (id.equals(imi.getId())) {
                        selImi = imi;
                        if (InputMethodAndSubtypeUtil.isSystemIme(imi)) {
                            InputMethodAndSubtypeUtil.setSubtypesPreferenceEnabled(
                                    this, mInputMethodProperties, id, true);
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
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setCancelable(true)
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            chkPref.setChecked(true);
                                            InputMethodAndSubtypeUtil.setSubtypesPreferenceEnabled(
                                                    InputMethodAndSubtypeEnabler.this,
                                                    mInputMethodProperties, id, true);
                                        }

                            })
                            .setNegativeButton(android.R.string.cancel,
                                    new DialogInterface.OnClickListener() {
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
                        this, mInputMethodProperties, id, false);
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

    private void onCreateIMM() {
        InputMethodManager imm = (InputMethodManager) getSystemService(
                Context.INPUT_METHOD_SERVICE);

        // TODO: Change mInputMethodProperties to Map
        mInputMethodProperties = imm.getInputMethodList();
    }

    private PreferenceScreen createPreferenceHierarchy() {
        // Root
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());

        int N = (mInputMethodProperties == null ? 0 : mInputMethodProperties.size());
        // TODO: Use iterator.
        for (int i = 0; i < N; ++i) {
            PreferenceCategory keyboardSettingsCategory = new PreferenceCategory(getActivity());
            root.addPreference(keyboardSettingsCategory);
            InputMethodInfo property = mInputMethodProperties.get(i);
            String prefKey = property.getId();

            PackageManager pm = getPackageManager();
            CharSequence label = property.loadLabel(pm);
            boolean systemIME = InputMethodAndSubtypeUtil.isSystemIme(property);

            keyboardSettingsCategory.setTitle(label);

            // Add a check box.
            // Don't show the toggle if it's the only keyboard in the system, or it's a system IME.
            if (mHaveHardKeyboard || (N > 1 && !systemIME)) {
                CheckBoxPreference chkbxPref = new CheckBoxPreference(getActivity());
                chkbxPref.setKey(prefKey);
                chkbxPref.setTitle(label);
                keyboardSettingsCategory.addPreference(chkbxPref);
            }

            ArrayList<InputMethodSubtype> subtypes = property.getSubtypes();
            if (subtypes.size() > 0) {
                PreferenceCategory subtypesCategory = new PreferenceCategory(getActivity());
                subtypesCategory.setTitle(getResources().getString(
                        R.string.input_methods_and_subtype_enabler_title_format, label));
                root.addPreference(subtypesCategory);
                for (InputMethodSubtype subtype: subtypes) {
                    CharSequence subtypeLabel;
                    int nameResId = subtype.getNameResId();
                    if (nameResId != 0) {
                        subtypeLabel = pm.getText(property.getPackageName(), nameResId,
                                property.getServiceInfo().applicationInfo);
                    } else {
                        String mode = subtype.getMode();
                        CharSequence language = subtype.getLocale();
                        // TODO: Use more friendly Title and UI
                        subtypeLabel = (mode == null ? "" : mode) + ","
                                + (language == null ? "" : language);
                    }
                    CheckBoxPreference chkbxPref = new CheckBoxPreference(getActivity());
                    chkbxPref.setKey(prefKey + subtype.hashCode());
                    chkbxPref.setTitle(subtypeLabel);
                    chkbxPref.setSummary(label);
                    subtypesCategory.addPreference(chkbxPref);
                }
            }
        }
        return root;
    }
}
