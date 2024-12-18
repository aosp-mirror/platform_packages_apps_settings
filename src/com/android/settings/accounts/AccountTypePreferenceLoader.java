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

package com.android.settings.accounts;

import android.accounts.Account;
import android.accounts.AuthenticatorDescription;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArraySet;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.location.LocationSettings;
import com.android.settings.utils.LocalClassLoaderContextThemeWrapper;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.core.instrumentation.Instrumentable;

import java.util.Set;

/**
 * Class to load the preference screen to be added to the settings page for the specific account
 * type as specified in the account-authenticator.
 */
public class AccountTypePreferenceLoader {

    private static final String TAG = "AccountTypePrefLoader";
    private static final String ACCOUNT_KEY = "account"; // to pass to auth settings
    // Action name for the broadcast intent when the Google account preferences page is launching
    // the location settings.
    private static final String LAUNCHING_LOCATION_SETTINGS =
        "com.android.settings.accounts.LAUNCHING_LOCATION_SETTINGS";

    private AuthenticatorHelper mAuthenticatorHelper;
    private UserHandle mUserHandle;
    private PreferenceFragmentCompat mFragment;

    public AccountTypePreferenceLoader(PreferenceFragmentCompat fragment,
            AuthenticatorHelper authenticatorHelper, UserHandle userHandle) {
        mFragment = fragment;
        mAuthenticatorHelper = authenticatorHelper;
        mUserHandle = userHandle;
    }

    /**
     * Gets the preferences.xml file associated with a particular account type.
     * @param accountType the type of account
     * @return a PreferenceScreen inflated from accountPreferenceId.
     */
    public PreferenceScreen addPreferencesForType(final String accountType,
            PreferenceScreen parent) {
        PreferenceScreen prefs = null;
        if (mAuthenticatorHelper.containsAccountType(accountType)) {
            AuthenticatorDescription desc = null;
            try {
                desc = mAuthenticatorHelper.getAccountTypeDescription(accountType);
                if (desc != null && desc.accountPreferencesId != 0) {
                    Set<String> fragmentAllowList = generateFragmentAllowlist(parent);
                    // Load the context of the target package, then apply the
                    // base Settings theme (no references to local resources)
                    // and create a context theme wrapper so that we get the
                    // correct text colors. Control colors will still be wrong,
                    // but there's not much we can do about it since we can't
                    // reference local color resources.
                    final Context targetCtx = mFragment.getActivity().createPackageContextAsUser(
                            desc.packageName, 0, mUserHandle);
                    final Theme baseTheme = mFragment.getResources().newTheme();
                    baseTheme.applyStyle(
                            com.android.settingslib.widget.theme.R.style.Theme_SettingsBase, true);
                    final Context themedCtx =
                            new LocalClassLoaderContextThemeWrapper(getClass(), targetCtx, 0);
                    themedCtx.getTheme().setTo(baseTheme);
                    prefs = mFragment.getPreferenceManager().inflateFromResource(themedCtx,
                            desc.accountPreferencesId, parent);
                    // Ignore Fragments provided dynamically, as these are coming from external
                    // applications which must not have access to internal Settings' fragments.
                    // These preferences are rendered into Settings, so they also won't have access
                    // to their own Fragments, meaning there is no acceptable usage of
                    // android:fragment here.
                    filterBlockedFragments(prefs, fragmentAllowList);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Couldn't load preferences.xml file from " + desc.packageName);
            } catch (Resources.NotFoundException e) {
                Log.w(TAG, "Couldn't load preferences.xml file from " + desc.packageName);
            }
        }
        return prefs;
    }

    /**
     * Recursively filters through the preference list provided by GoogleLoginService.
     *
     * This method removes all the invalid intent from the list, adds account name as extra into the
     * intent, and hack the location settings to start it as a fragment.
     */
    public void updatePreferenceIntents(PreferenceGroup prefs, final String acccountType,
            Account account) {
        final PackageManager pm = mFragment.getActivity().getPackageManager();
        for (int i = 0; i < prefs.getPreferenceCount(); ) {
            Preference pref = prefs.getPreference(i);
            if (pref instanceof PreferenceGroup) {
                updatePreferenceIntents((PreferenceGroup) pref, acccountType, account);
            }
            Intent intent = pref.getIntent();
            if (intent != null) {
                // Hack. Launch "Location" as fragment instead of as activity.
                //
                // When "Location" is launched as activity via Intent, there's no "Up" button at the
                // top left, and if there's another running instance of "Location" activity, the
                // back stack would usually point to some other place so the user won't be able to
                // go back to the previous page by "back" key. Using fragment is a much easier
                // solution to those problems.
                //
                // If we set Intent to null and assign a fragment to the PreferenceScreen item here,
                // in order to make it work as expected, we still need to modify the container
                // PreferenceActivity, override onPreferenceStartFragment() and call
                // startPreferencePanel() there. In order to inject the title string there, more
                // dirty further hack is still needed. It's much easier and cleaner to listen to
                // preference click event here directly.
                if (TextUtils.equals(intent.getAction(),
                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) {
                    // The OnPreferenceClickListener overrides the click event completely. No intent
                    // will get fired.
                    pref.setOnPreferenceClickListener(new FragmentStarter(
                        LocationSettings.class.getName(), R.string.location_settings_title));
                } else {
                    ResolveInfo ri = pm.resolveActivityAsUser(intent,
                        PackageManager.MATCH_DEFAULT_ONLY, mUserHandle.getIdentifier());
                    if (ri == null) {
                        prefs.removePreference(pref);
                        continue;
                    }
                    intent.putExtra(ACCOUNT_KEY, account);
                    intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NEW_TASK);
                    pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Intent prefIntent = preference.getIntent();
                                /*
                                 * Check the intent to see if it resolves to a exported=false
                                 * activity that doesn't share a uid with the authenticator.
                                 *
                                 * Otherwise the intent is considered unsafe in that it will be
                                 * exploiting the fact that settings has system privileges.
                                 */
                            if (isSafeIntent(pm, prefIntent, acccountType)) {
                                // Explicitly set an empty ClipData to ensure that we don't offer to
                                // promote any Uris contained inside for granting purposes
                                prefIntent.setClipData(ClipData.newPlainText(null, null));
                                mFragment.getActivity().startActivityAsUser(
                                    prefIntent, mUserHandle);
                            } else {
                                Log.e(TAG,
                                    "Refusing to launch authenticator intent because"
                                        + "it exploits Settings permissions: "
                                        + prefIntent);
                            }
                            return true;
                        }
                    });
                }
            }
            i++;
        }
    }

    // Build allowlist from existing Fragments in PreferenceGroup
    @VisibleForTesting
    Set<String> generateFragmentAllowlist(@Nullable PreferenceGroup prefs) {
        Set<String> fragmentAllowList = new ArraySet<>();
        if (prefs == null) {
            return fragmentAllowList;
        }

        for (int i = 0; i < prefs.getPreferenceCount(); i++) {
            Preference pref = prefs.getPreference(i);
            if (pref instanceof PreferenceGroup) {
                fragmentAllowList.addAll(generateFragmentAllowlist((PreferenceGroup) pref));
            }

            String fragmentName = pref.getFragment();
            if (!TextUtils.isEmpty(fragmentName)) {
                fragmentAllowList.add(fragmentName);
            }
        }
        return fragmentAllowList;
    }

    // Block clicks on any Preference with android:fragment that is not contained in the allowlist
    @VisibleForTesting
    void filterBlockedFragments(@Nullable PreferenceGroup prefs,
            @NonNull Set<String> allowedFragments) {
        if (prefs == null) {
            return;
        }
        for (int i = 0; i < prefs.getPreferenceCount(); i++) {
            Preference pref = prefs.getPreference(i);
            if (pref instanceof PreferenceGroup) {
                filterBlockedFragments((PreferenceGroup) pref, allowedFragments);
            }

            String fragmentName = pref.getFragment();
            if (fragmentName != null && !allowedFragments.contains(fragmentName)) {
                pref.setOnPreferenceClickListener(preference -> true);
            }
        }
    }

    /**
     * Determines if the supplied Intent is safe. A safe intent is one that is
     * will launch a exported=true activity or owned by the same uid as the
     * authenticator supplying the intent.
     */
    private boolean isSafeIntent(PackageManager pm, Intent intent, String acccountType) {
        AuthenticatorDescription authDesc =
            mAuthenticatorHelper.getAccountTypeDescription(acccountType);
        ResolveInfo resolveInfo = pm.resolveActivityAsUser(intent, 0, mUserHandle.getIdentifier());
        if (resolveInfo == null) {
            return false;
        }
        ActivityInfo resolvedActivityInfo = resolveInfo.activityInfo;
        ApplicationInfo resolvedAppInfo = resolvedActivityInfo.applicationInfo;
        try {
            // Allows to launch only authenticator owned activities.
            ApplicationInfo authenticatorAppInf = pm.getApplicationInfo(authDesc.packageName, 0);
            return resolvedAppInfo.uid == authenticatorAppInf.uid;
        } catch (NameNotFoundException e) {
            Log.e(TAG,
                "Intent considered unsafe due to exception.",
                e);
            return false;
        }
    }

    /** Listens to a preference click event and starts a fragment */
    private class FragmentStarter
        implements Preference.OnPreferenceClickListener {
        private final String mClass;
        private final int mTitleRes;

        /**
         * @param className the class name of the fragment to be started.
         * @param title the title resource id of the started preference panel.
         */
        public FragmentStarter(String className, int title) {
            mClass = className;
            mTitleRes = title;
        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            final int metricsCategory = (mFragment instanceof Instrumentable)
                    ? ((Instrumentable) mFragment).getMetricsCategory()
                    : Instrumentable.METRICS_CATEGORY_UNKNOWN;
            new SubSettingLauncher(preference.getContext())
                    .setTitleRes(mTitleRes)
                    .setDestination(mClass)
                    .setSourceMetricsCategory(metricsCategory)
                    .launch();

            // Hack: announce that the Google account preferences page is launching the location
            // settings
            if (mClass.equals(LocationSettings.class.getName())) {
                Intent intent = new Intent(LAUNCHING_LOCATION_SETTINGS);
                mFragment.getActivity().sendBroadcast(
                    intent, android.Manifest.permission.WRITE_SECURE_SETTINGS);
            }
            return true;
        }
    }

}
