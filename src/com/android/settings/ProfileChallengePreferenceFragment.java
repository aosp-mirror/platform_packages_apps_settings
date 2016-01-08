/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.widget.LockPatternUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Settings for the Profile Challenge.
 */
public class ProfileChallengePreferenceFragment extends SettingsPreferenceFragment
        implements OnPreferenceChangeListener {
    private static final String TAG = "WorkChallengePreferenceFragment";

    private static final String KEY_UNLOCK_SET_OR_CHANGE = "unlock_set_or_change";
    private static final String KEY_VISIBLE_PATTERN = "visiblepattern";
    private static final String KEY_SECURITY_CATEGORY = "security_category";
    private static final String KEY_UNIFICATION = "unification";
    public static final String TAG_UNIFICATION_DIALOG = "unification_dialog";

    private static final int SET_OR_CHANGE_LOCK_METHOD_REQUEST = 123;
    private static final int UNIFY_LOCK_METHOD_REQUEST = 124;

    // Not all preferences make sense for the Work Challenge, this is a whitelist.
    private static final Set<String> ALLOWED_PREFERENCE_KEYS = new HashSet<>();
    {
        ALLOWED_PREFERENCE_KEYS.add(KEY_UNLOCK_SET_OR_CHANGE);
        ALLOWED_PREFERENCE_KEYS.add(KEY_VISIBLE_PATTERN);
    }
    // These switch preferences need special handling since they're not all stored in Settings.
    private static final Set<String> SWITCH_PREFERENCE_KEYS = new HashSet<>();
    {
        SWITCH_PREFERENCE_KEYS.add(KEY_VISIBLE_PATTERN);
    }

    private LockPatternUtils mLockPatternUtils;
    private int mProfileUserId;

    private SwitchPreference mVisiblePattern;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.PROFILE_CHALLENGE;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLockPatternUtils = new LockPatternUtils(getActivity());

        mProfileUserId = getArguments().getInt(Intent.EXTRA_USER_ID, -1);
        if (mProfileUserId == -1) {
            finish();
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        final String key = preference.getKey();
        if (KEY_UNLOCK_SET_OR_CHANGE.equals(key)) {
            Bundle extras = new Bundle();
            extras.putInt(Intent.EXTRA_USER_ID, mProfileUserId);
            startFragment(this, "com.android.settings.ChooseLockGeneric$ChooseLockGenericFragment",
                    R.string.lock_settings_picker_title, SET_OR_CHANGE_LOCK_METHOD_REQUEST, extras);
            return true;
        } else if (KEY_UNIFICATION.equals(key)) {
            UnificationConfirmationDialog dialog =
                    UnificationConfirmationDialog.newIntance(mProfileUserId);
            dialog.show(getChildFragmentManager(), TAG_UNIFICATION_DIALOG);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        final String key = preference.getKey();
        if (KEY_VISIBLE_PATTERN.equals(key)) {
            mLockPatternUtils.setVisiblePatternEnabled((Boolean) value, mProfileUserId);
            return true;
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UNIFY_LOCK_METHOD_REQUEST && resultCode == Activity.RESULT_OK) {
            mLockPatternUtils.clearLock(mProfileUserId);
            mLockPatternUtils.setSeparateProfileChallengeEnabled(mProfileUserId, false);
            return;
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();

        if (mVisiblePattern != null) {
            mVisiblePattern.setChecked(mLockPatternUtils.isVisiblePatternEnabled(
                    mProfileUserId));
        }
    }

    private void createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.profile_challenge_settings);
        root = getPreferenceScreen();

        // Add options for lock/unlock screen
        final int resid = getResIdForLockUnlockScreen(getActivity(), mLockPatternUtils);
        addPreferencesFromResource(resid);

        mVisiblePattern = (SwitchPreference) root.findPreference(KEY_VISIBLE_PATTERN);

        removeNonWhitelistedItems(root);

        PreferenceGroup securityCategory = (PreferenceGroup)
                root.findPreference(KEY_SECURITY_CATEGORY);
        if (securityCategory != null) {
            if (mLockPatternUtils.isSeparateProfileChallengeEnabled(mProfileUserId)) {
                addUnificationPreference(securityCategory);
            } else {
                Preference lockPreference =
                        securityCategory.findPreference(KEY_UNLOCK_SET_OR_CHANGE);
                String summary =
                        getContext().getString(R.string.lock_settings_profile_unified_summary);
                lockPreference.setSummary(summary);
            }
        }
    }

    private void addUnificationPreference(PreferenceGroup securityCategory) {
        Preference unificationPreference = new Preference(securityCategory.getContext());
        unificationPreference.setKey(KEY_UNIFICATION);
        unificationPreference.setTitle(R.string.lock_settings_profile_unification_title);
        unificationPreference.setSummary(R.string.lock_settings_profile_unification_summary);
        securityCategory.addPreference(unificationPreference);
    }

    private void removeNonWhitelistedItems(PreferenceGroup prefScreen) {
        int numPreferences = prefScreen.getPreferenceCount();
        int i = 0;
        while (i < numPreferences) {
            final Preference pref = prefScreen.getPreference(i);
            // Recursively look into categories and remove them if they are empty.
            if (pref instanceof PreferenceCategory) {
                PreferenceCategory category = (PreferenceCategory) pref;
                removeNonWhitelistedItems(category);
                if (category.getPreferenceCount() == 0) {
                    prefScreen.removePreference(category);
                    --i;
                    --numPreferences;
                }
            } else if (ALLOWED_PREFERENCE_KEYS.contains(pref.getKey())) {
                if (SWITCH_PREFERENCE_KEYS.contains(pref.getKey())) {
                    pref.setOnPreferenceChangeListener(this);
                }
            } else {
                prefScreen.removePreference(pref);
                --i;
                --numPreferences;
            }
            ++i;
        }
    }

    private int getResIdForLockUnlockScreen(Context context,
            LockPatternUtils lockPatternUtils) {
        int resid = 0;
        if (!lockPatternUtils.isSecure(mProfileUserId)) {
            resid = R.xml.security_settings_lockscreen;
        } else {
            switch (lockPatternUtils.getKeyguardStoredPasswordQuality(mProfileUserId)) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    resid = R.xml.security_settings_pattern;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    resid = R.xml.security_settings_pin;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    resid = R.xml.security_settings_password;
                    break;
            }
        }
        return resid;
    }

    public static class UnificationConfirmationDialog extends DialogFragment {
        private static final String ARG_USER_ID = "userId";

        public static UnificationConfirmationDialog newIntance(int userId) {
            UnificationConfirmationDialog dialog = new UnificationConfirmationDialog();
            Bundle args = new Bundle();
            args.putInt(ARG_USER_ID, userId);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public void show(FragmentManager manager, String tag) {
            if (manager.findFragmentByTag(tag) == null) {
                // Prevent opening multiple dialogs if tapped on button quickly
                super.show(manager, tag);
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.lock_settings_profile_unification_dialog_title)
                    .setMessage(R.string.lock_settings_profile_unification_dialog_body)
                    .setPositiveButton(R.string.okay,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String title = getContext().getString(
                                            R.string.lock_settings_profile_screen_lock_title);
                                    ChooseLockSettingsHelper helper =
                                            new ChooseLockSettingsHelper(
                                                    getActivity(), getParentFragment());
                                    helper.launchConfirmationActivity(UNIFY_LOCK_METHOD_REQUEST,
                                            title, true, args.getInt(ARG_USER_ID));
                                }
                            }
                    )
                    .setNegativeButton(R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    dismiss();
                                }
                            }
                    )
                    .create();
        }
    }
}
