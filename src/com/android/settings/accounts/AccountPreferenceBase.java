/*

 * Copyright (C) 2008 The Android Open Source Project
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

import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncStatusObserver;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.util.Log;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.utils.LocalClassLoaderContextThemeWrapper;
import com.android.settingslib.accounts.AuthenticatorHelper;

import java.util.ArrayList;
import java.util.Date;

abstract class AccountPreferenceBase extends SettingsPreferenceFragment
        implements AuthenticatorHelper.OnAccountsUpdateListener {

    protected static final String TAG = "AccountSettings";

    public static final String AUTHORITIES_FILTER_KEY = "authorities";
    public static final String ACCOUNT_TYPES_FILTER_KEY = "account_types";

    private final Handler mHandler = new Handler();

    private UserManager mUm;
    private Object mStatusChangeListenerHandle;
    protected AuthenticatorHelper mAuthenticatorHelper;
    protected UserHandle mUserHandle;

    private java.text.DateFormat mDateFormat;
    private java.text.DateFormat mTimeFormat;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mUm = (UserManager) getSystemService(Context.USER_SERVICE);
        final Activity activity = getActivity();
        mUserHandle = Utils.getSecureTargetUser(activity.getActivityToken(), mUm, getArguments(),
                activity.getIntent().getExtras());
        mAuthenticatorHelper = new AuthenticatorHelper(activity, mUserHandle, this);
    }

    /**
     * Overload to handle account updates.
     */
    @Override
    public void onAccountsUpdate(UserHandle userHandle) {

    }

    /**
     * Overload to handle authenticator description updates
     */
    protected void onAuthDescriptionsUpdated() {

    }

    /**
     * Overload to handle sync state updates.
     */
    protected void onSyncStateUpdated() {

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final Activity activity = getActivity();

        mDateFormat = DateFormat.getDateFormat(activity);
        mTimeFormat = DateFormat.getTimeFormat(activity);
    }

    @Override
    public void onResume() {
        super.onResume();
        mStatusChangeListenerHandle = ContentResolver.addStatusChangeListener(
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE
                | ContentResolver.SYNC_OBSERVER_TYPE_STATUS
                | ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS,
                mSyncStatusObserver);
        onSyncStateUpdated();
    }

    @Override
    public void onPause() {
        super.onPause();
        ContentResolver.removeStatusChangeListener(mStatusChangeListenerHandle);
    }

    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        public void onStatusChanged(int which) {
            mHandler.post(new Runnable() {
                public void run() {
                    onSyncStateUpdated();
                }
            });
        }
    };

    public ArrayList<String> getAuthoritiesForAccountType(String type) {
        return mAuthenticatorHelper.getAuthoritiesForAccountType(type);
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
                    // Load the context of the target package, then apply the
                    // base Settings theme (no references to local resources)
                    // and create a context theme wrapper so that we get the
                    // correct text colors. Control colors will still be wrong,
                    // but there's not much we can do about it since we can't
                    // reference local color resources.
                    final Context targetCtx = getActivity().createPackageContextAsUser(
                            desc.packageName, 0, mUserHandle);
                    final Theme baseTheme = getResources().newTheme();
                    baseTheme.applyStyle(com.android.settings.R.style.Theme_SettingsBase, true);
                    final Context themedCtx =
                            new LocalClassLoaderContextThemeWrapper(getClass(), targetCtx, 0);
                    themedCtx.getTheme().setTo(baseTheme);
                    prefs = getPreferenceManager().inflateFromResource(themedCtx,
                            desc.accountPreferencesId, parent);
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Couldn't load preferences.xml file from " + desc.packageName);
            } catch (Resources.NotFoundException e) {
                Log.w(TAG, "Couldn't load preferences.xml file from " + desc.packageName);
            }
        }
        return prefs;
    }

    public void updateAuthDescriptions() {
        mAuthenticatorHelper.updateAuthDescriptions(getActivity());
        onAuthDescriptionsUpdated();
    }

    protected Drawable getDrawableForType(final String accountType) {
        return mAuthenticatorHelper.getDrawableForType(getActivity(), accountType);
    }

    protected CharSequence getLabelForType(final String accountType) {
        return mAuthenticatorHelper.getLabelForType(getActivity(), accountType);
    }

    protected String formatSyncDate(Date date) {
        // TODO: Switch to using DateUtils.formatDateTime
        return mDateFormat.format(date) + " " + mTimeFormat.format(date);
    }
}
