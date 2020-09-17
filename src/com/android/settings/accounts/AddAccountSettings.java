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

import static android.content.Intent.EXTRA_USER;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.Utils;
import com.android.settings.password.ChooseLockSettingsHelper;

import java.io.IOException;
/**
 * Entry point Activity for account setup. Works as follows
 *
 * 1) When the other Activities launch this Activity, it launches {@link ChooseAccountActivity}
 *    without showing anything.
 * 2) After receiving an account type from ChooseAccountActivity, this Activity launches the
 *    account setup specified by AccountManager.
 * 3) After the account setup, this Activity finishes without showing anything.
 *
 * Note:
 * Previously this Activity did what {@link ChooseAccountActivity} does right now, but we
 * currently delegate the work to the other Activity. When we let this Activity do that work, users
 * would see the list of account types when leaving this Activity, since the UI is already ready
 * when returning from each account setup, which doesn't look good.
 *
 * An extra {@link UserHandle} can be specified in the intent as {@link EXTRA_USER}, if the user for
 * which the action needs to be performed is different to the one the Settings App will run in.
 */
public class AddAccountSettings extends Activity {
    /**
     *
     */
    private static final String KEY_ADD_CALLED = "AddAccountCalled";

    /**
     * Extra parameter to identify the caller. Applications may display a
     * different UI if the calls is made from Settings or from a specific
     * application.
     */
    private static final String KEY_CALLER_IDENTITY = "pendingIntent";
    private static final String SHOULD_NOT_RESOLVE = "SHOULDN'T RESOLVE!";

    private static final String TAG = "AddAccountSettings";

    /* package */ static final String EXTRA_SELECTED_ACCOUNT = "selected_account";

    // show additional info regarding the use of a device with multiple users
    static final String EXTRA_HAS_MULTIPLE_USERS = "hasMultipleUsers";

    private static final int CHOOSE_ACCOUNT_REQUEST = 1;
    private static final int ADD_ACCOUNT_REQUEST = 2;
    private static final int UNLOCK_WORK_PROFILE_REQUEST = 3;

    private PendingIntent mPendingIntent;

    private final AccountManagerCallback<Bundle> mCallback = new AccountManagerCallback<Bundle>() {
        @Override
        public void run(AccountManagerFuture<Bundle> future) {
            boolean done = true;
            try {
                Bundle bundle = future.getResult();
                //bundle.keySet();
                Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
                if (intent != null) {
                    done = false;
                    Bundle addAccountOptions = new Bundle();
                    addAccountOptions.putParcelable(KEY_CALLER_IDENTITY, mPendingIntent);
                    addAccountOptions.putBoolean(EXTRA_HAS_MULTIPLE_USERS,
                            Utils.hasMultipleUsers(AddAccountSettings.this));
                    addAccountOptions.putParcelable(EXTRA_USER, mUserHandle);
                    intent.putExtras(addAccountOptions)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivityForResultAsUser(intent, ADD_ACCOUNT_REQUEST, mUserHandle);
                } else {
                    setResult(RESULT_OK);
                    if (mPendingIntent != null) {
                        mPendingIntent.cancel();
                        mPendingIntent = null;
                    }
                }

                if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "account added: " + bundle);
            } catch (OperationCanceledException e) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "addAccount was canceled");
            } catch (IOException e) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "addAccount failed: " + e);
            } catch (AuthenticatorException e) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "addAccount failed: " + e);
            } finally {
                if (done) {
                    finish();
                }
            }
        }
    };

    private boolean mAddAccountCalled = false;
    private UserHandle mUserHandle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mAddAccountCalled = savedInstanceState.getBoolean(KEY_ADD_CALLED);
            if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "restored");
        }

        final UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        mUserHandle = Utils.getSecureTargetUser(getActivityToken(), um, null /* arguments */,
                getIntent().getExtras());
        if (um.hasUserRestriction(UserManager.DISALLOW_MODIFY_ACCOUNTS, mUserHandle)) {
            // We aren't allowed to add an account.
            Toast.makeText(this, R.string.user_cannot_add_accounts_message, Toast.LENGTH_LONG)
                    .show();
            finish();
            return;
        }
        if (mAddAccountCalled) {
            // We already called add account - maybe the callback was lost.
            finish();
            return;
        }
        if (Utils.startQuietModeDialogIfNecessary(this, um, mUserHandle.getIdentifier())) {
            finish();
            return;
        }
        if (um.isUserUnlocked(mUserHandle)) {
            requestChooseAccount();
        } else {
            // If the user is locked by fbe: we couldn't start the authenticator. So we must ask the
            // user to unlock it first.
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(this);
            if (!helper.launchConfirmationActivity(UNLOCK_WORK_PROFILE_REQUEST,
                    getString(R.string.unlock_set_unlock_launch_picker_title),
                    false,
                    mUserHandle.getIdentifier())) {
                requestChooseAccount();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case UNLOCK_WORK_PROFILE_REQUEST:
            if (resultCode == Activity.RESULT_OK) {
                requestChooseAccount();
            } else {
                finish();
            }
            break;
        case CHOOSE_ACCOUNT_REQUEST:
            if (resultCode == RESULT_CANCELED) {
                if (data != null) {
                    startActivityAsUser(data, mUserHandle);
                }
                setResult(resultCode);
                finish();
                return;
            }
            // Go to account setup screen. finish() is called inside mCallback.
            addAccount(data.getStringExtra(EXTRA_SELECTED_ACCOUNT));
            break;
        case ADD_ACCOUNT_REQUEST:
            setResult(resultCode);
            if (mPendingIntent != null) {
                mPendingIntent.cancel();
                mPendingIntent = null;
            }
            finish();
            break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_ADD_CALLED, mAddAccountCalled);
        if (Log.isLoggable(TAG, Log.VERBOSE)) Log.v(TAG, "saved");
    }

    private void requestChooseAccount() {
        final String[] authorities =
                getIntent().getStringArrayExtra(AccountPreferenceBase.AUTHORITIES_FILTER_KEY);
        final String[] accountTypes =
                getIntent().getStringArrayExtra(AccountPreferenceBase.ACCOUNT_TYPES_FILTER_KEY);
        final Intent intent = new Intent(this, Settings.ChooseAccountActivity.class);
        if (authorities != null) {
            intent.putExtra(AccountPreferenceBase.AUTHORITIES_FILTER_KEY, authorities);
        }
        if (accountTypes != null) {
            intent.putExtra(AccountPreferenceBase.ACCOUNT_TYPES_FILTER_KEY, accountTypes);
        }
        intent.putExtra(EXTRA_USER, mUserHandle);
        startActivityForResult(intent, CHOOSE_ACCOUNT_REQUEST);
    }

    private void addAccount(String accountType) {
        Bundle addAccountOptions = new Bundle();
        /*
         * The identityIntent is for the purposes of establishing the identity
         * of the caller and isn't intended for launching activities, services
         * or broadcasts.
         *
         * Unfortunately for legacy reasons we still need to support this. But
         * we can cripple the intent so that 3rd party authenticators can't
         * fill in addressing information and launch arbitrary actions.
         */
        Intent identityIntent = new Intent();
        identityIntent.setComponent(new ComponentName(SHOULD_NOT_RESOLVE, SHOULD_NOT_RESOLVE));
        identityIntent.setAction(SHOULD_NOT_RESOLVE);
        identityIntent.addCategory(SHOULD_NOT_RESOLVE);

        mPendingIntent = PendingIntent.getBroadcast(this, 0, identityIntent, 0);
        addAccountOptions.putParcelable(KEY_CALLER_IDENTITY, mPendingIntent);
        addAccountOptions.putBoolean(EXTRA_HAS_MULTIPLE_USERS, Utils.hasMultipleUsers(this));
        AccountManager.get(this).addAccountAsUser(
                accountType,
                null, /* authTokenType */
                null, /* requiredFeatures */
                addAccountOptions,
                null,
                mCallback,
                null /* handler */,
                mUserHandle);
        mAddAccountCalled  = true;
    }
}
