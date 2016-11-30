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

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.Tile;

import java.util.ArrayList;
import java.util.List;

public class AccountDetailDashboardFragment extends DashboardFragment {

    private static final String TAG = "AccountDetailDashboard";
    private static final String METADATA_IA_ACCOUNT = "com.android.settings.ia.account";

    public static final String KEY_ACCOUNT = "account";
    public static final String KEY_ACCOUNT_TYPE = "account_type";
    public static final String KEY_ACCOUNT_LABEL = "account_label";
    public static final String KEY_ACCOUNT_TITLE_RES = "account_title_res";

    private String mAccountLabel;
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    String mAccountType;
    private AccountSyncPreferenceController mAccountSynController;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Bundle args = getArguments();
        final Activity activity = getActivity();
        UserHandle userHandle = Utils.getSecureTargetUser(activity.getActivityToken(),
            (UserManager) getSystemService(Context.USER_SERVICE), args,
            activity.getIntent().getExtras());
        Account account = null;
        if (args != null) {
            if (args.containsKey(KEY_ACCOUNT)) {
                account = args.getParcelable(KEY_ACCOUNT);
            }
            if (args.containsKey(KEY_ACCOUNT_LABEL)) {
                mAccountLabel = args.getString(KEY_ACCOUNT_LABEL);
            }
            if (args.containsKey(KEY_ACCOUNT_TYPE)) {
                mAccountType = args.getString(KEY_ACCOUNT_TYPE);
            }
        }
        mAccountSynController.init(account, userHandle);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mAccountLabel != null) {
            getActivity().setTitle(mAccountLabel);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ACCOUNT;
    }

    @Override
    protected String getCategoryKey() {
        return CategoryKey.CATEGORY_ACCOUNT;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.account_type_settings;
    }

    @Override
    protected List<PreferenceController> getPreferenceControllers(Context context) {
        final List<PreferenceController> controllers = new ArrayList<>();
        mAccountSynController = new AccountSyncPreferenceController(context);
        controllers.add(mAccountSynController);
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
        return mAccountType.equals(metadata.getString(METADATA_IA_ACCOUNT));
    }

}