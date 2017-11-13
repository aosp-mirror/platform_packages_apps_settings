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
package com.android.settings.accounts;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.accounts.AuthenticatorHelper;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.drawer.Tile;

import java.util.ArrayList;
import java.util.List;

public class AccountDetailDashboardFragment extends DashboardFragment {

    private static final String TAG = "AccountDetailDashboard";
    private static final String METADATA_IA_ACCOUNT = "com.android.settings.ia.account";
    private static final String EXTRA_ACCOUNT_NAME = "extra.accountName";

    public static final String KEY_ACCOUNT = "account";
    public static final String KEY_ACCOUNT_TYPE = "account_type";
    public static final String KEY_ACCOUNT_LABEL = "account_label";
    public static final String KEY_ACCOUNT_TITLE_RES = "account_title_res";
    public static final String KEY_USER_HANDLE = "user_handle";

    @VisibleForTesting
    Account mAccount;
    private String mAccountLabel;
    @VisibleForTesting
    String mAccountType;
    private AccountSyncPreferenceController mAccountSynController;
    private RemoveAccountPreferenceController mRemoveAccountController;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        getPreferenceManager().setPreferenceComparisonCallback(null);
        Bundle args = getArguments();
        final Activity activity = getActivity();
        UserHandle userHandle = Utils.getSecureTargetUser(activity.getActivityToken(),
                (UserManager) getSystemService(Context.USER_SERVICE), args,
                activity.getIntent().getExtras());
        if (args != null) {
            if (args.containsKey(KEY_ACCOUNT)) {
                mAccount = args.getParcelable(KEY_ACCOUNT);
            }
            if (args.containsKey(KEY_ACCOUNT_LABEL)) {
                mAccountLabel = args.getString(KEY_ACCOUNT_LABEL);
            }
            if (args.containsKey(KEY_ACCOUNT_TYPE)) {
                mAccountType = args.getString(KEY_ACCOUNT_TYPE);
            }
        }
        mAccountSynController.init(mAccount, userHandle);
        mRemoveAccountController.init(mAccount, userHandle);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mAccountLabel != null) {
            getActivity().setTitle(mAccountLabel);
        }
        updateUi();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ACCOUNT;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_account_detail;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.account_type_settings;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        mAccountSynController = new AccountSyncPreferenceController(context);
        controllers.add(mAccountSynController);
        mRemoveAccountController = new RemoveAccountPreferenceController(context, this);
        controllers.add(mRemoveAccountController);
        controllers.add(new AccountHeaderPreferenceController(
                context, getLifecycle(), getActivity(), this /* host */, getArguments()));
        return controllers;
    }

    @Override
    protected boolean displayTile(Tile tile) {
        if (mAccountType == null) {
            return false;
        }
        final Bundle metadata = tile.metaData;
        if (metadata == null) {
            return false;
        }
        final boolean display = mAccountType.equals(metadata.getString(METADATA_IA_ACCOUNT));
        if (display && tile.intent != null) {
            tile.intent.putExtra(EXTRA_ACCOUNT_NAME, mAccount.name);
        }
        return display;
    }

    @VisibleForTesting
    void updateUi() {
        final Context context = getContext();
        UserHandle userHandle = null;
        Bundle args = getArguments();
        if (args != null && args.containsKey(KEY_USER_HANDLE)) {
            userHandle = args.getParcelable(KEY_USER_HANDLE);
        }
        final AuthenticatorHelper helper = new AuthenticatorHelper(context, userHandle, null);
        final AccountTypePreferenceLoader accountTypePreferenceLoader =
                new AccountTypePreferenceLoader(this, helper, userHandle);
        PreferenceScreen prefs = accountTypePreferenceLoader.addPreferencesForType(
                mAccountType, getPreferenceScreen());
        if (prefs != null) {
            accountTypePreferenceLoader.updatePreferenceIntents(prefs, mAccountType, mAccount);
        }
    }
}