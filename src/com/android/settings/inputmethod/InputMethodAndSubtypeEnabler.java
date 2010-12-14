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
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InputMethodAndSubtypeEnabler extends SettingsPreferenceFragment {

    public static final String EXTRA_INPUT_METHOD_ID = "input_method_id";

    private AlertDialog mDialog = null;
    private boolean mHaveHardKeyboard;
    final private HashMap<String, List<Preference>> mInputMethodPrefsMap =
        new HashMap<String, List<Preference>>();
    private List<InputMethodInfo> mInputMethodProperties;
    private String mInputMethodId;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Configuration config = getResources().getConfiguration();
        mHaveHardKeyboard = (config.keyboard == Configuration.KEYBOARD_QWERTY);
        mInputMethodId = getActivity().getIntent().getStringExtra(EXTRA_INPUT_METHOD_ID);
        onCreateIMM();
        setPreferenceScreen(createPreferenceHierarchy());
    }

    @Override
    public void onResume() {
        super.onResume();
        InputMethodAndSubtypeUtil.loadInputMethodSubtypeList(
                this, getContentResolver(), mInputMethodProperties, mInputMethodPrefsMap);
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

        for (int i = 0; i < N; ++i) {
            final InputMethodInfo imi = mInputMethodProperties.get(i);
            if (imi.getSubtypes().size() <= 1) continue;
            String imiId = imi.getId();
            // Add to this subtype the list when no IME is specified or when the IME of this
            // subtype is the specified IME.
            if (!TextUtils.isEmpty(mInputMethodId) && !mInputMethodId.equals(imiId)) {
                continue;
            }
            PreferenceCategory keyboardSettingsCategory = new PreferenceCategory(getActivity());
            root.addPreference(keyboardSettingsCategory);

            PackageManager pm = getPackageManager();
            CharSequence label = imi.loadLabel(pm);

            keyboardSettingsCategory.setTitle(getResources().getString(
                    R.string.input_methods_and_subtype_enabler_title_format, label));
            keyboardSettingsCategory.setKey(imiId);

            ArrayList<InputMethodSubtype> subtypes = imi.getSubtypes();
            ArrayList<Preference> subtypePreferences = new ArrayList<Preference>();
            if (subtypes.size() > 0) {
                for (InputMethodSubtype subtype: subtypes) {
                    CharSequence subtypeLabel;
                    int nameResId = subtype.getNameResId();
                    if (nameResId != 0) {
                        subtypeLabel = pm.getText(imi.getPackageName(), nameResId,
                                imi.getServiceInfo().applicationInfo);
                    } else {
                        String mode = subtype.getMode();
                        CharSequence language = subtype.getLocale();
                        subtypeLabel = (mode == null ? "" : mode) + ","
                                + (language == null ? "" : language);
                    }
                    CheckBoxPreference chkbxPref = new CheckBoxPreference(getActivity());
                    chkbxPref.setKey(imiId + subtype.hashCode());
                    chkbxPref.setTitle(subtypeLabel);
                    keyboardSettingsCategory.addPreference(chkbxPref);
                    subtypePreferences.add(chkbxPref);
                }
                mInputMethodPrefsMap.put(imiId, subtypePreferences);
            }
        }
        return root;
    }
}
