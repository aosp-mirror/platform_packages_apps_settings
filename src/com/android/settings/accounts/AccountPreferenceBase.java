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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncStatusObserver;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.format.DateFormat;
import android.util.Log;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.utils.ThreadUtils;

abstract class AccountPreferenceBase extends SettingsPreferenceFragment
        implements AuthenticatorHelper.OnAccountsUpdateListener {

    protected static final String TAG = "AccountPreferenceBase";
    protected static final boolean VERBOSE = Log.isLoggable(TAG, Log.VERBOSE);

    public static final String AUTHORITIES_FILTER_KEY = "authorities";
    public static final String ACCOUNT_TYPES_FILTER_KEY = "account_types";

    private UserManager mUm;
    private Object mStatusChangeListenerHandle;
    protected AuthenticatorHelper mAuthenticatorHelper;
    protected UserHandle mUserHandle;
    protected AccountTypePreferenceLoader mAccountTypePreferenceLoader;

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
        mAccountTypePreferenceLoader =
            new AccountTypePreferenceLoader(this, mAuthenticatorHelper, mUserHandle);
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

    private SyncStatusObserver mSyncStatusObserver =
            which -> ThreadUtils.postOnMainThread(() -> onSyncStateUpdated());

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
}
