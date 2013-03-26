/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.users;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.settings.R;

import java.util.Map;

public class UserRestrictionsActivity extends Activity implements OnClickListener {

    private static final String TAG = UserRestrictionsActivity.class.getSimpleName();

    static final String EXTRA_USER_NAME = "user_name";
    static final String EXTRA_USER_ID = "user_id";
    static final String EXTRA_ACCOUNTS = "accounts";

    private Button mFinishButton;
    private Button mBackButton;
    private AppRestrictionsFragment mAppsFragment;
    private UserInfo mUserInfo;
    private boolean mNewUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.user_limits);

        mBackButton = (Button) findViewById(R.id.back_button);
        if (mBackButton != null) {
            mBackButton.setOnClickListener(this);
        }
        mFinishButton = (Button) findViewById(R.id.next_button);
        if (mFinishButton != null) {
            mFinishButton.setOnClickListener(this);
        }
        mAppsFragment = (AppRestrictionsFragment)
                getFragmentManager().findFragmentById(R.id.user_limits_fragment);

        UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        String name = getIntent().getStringExtra(EXTRA_USER_NAME);
        int userId = getIntent().getIntExtra(EXTRA_USER_ID, -1);
        // Create the user so we have an id
        if (userId == -1) {
            mNewUser = true;
            mUserInfo = um.createUser(name, UserInfo.FLAG_RESTRICTED);
            um.setUserRestriction(UserManager.DISALLOW_MODIFY_ACCOUNTS, true,
                    new UserHandle(mUserInfo.id));

            Bitmap bitmap = BitmapFactory.decodeResource(getResources(),
                    UserSettings.USER_DRAWABLES[
                            mUserInfo.id % UserSettings.USER_DRAWABLES.length]);
            um.setUserIcon(mUserInfo.id, bitmap);
        } else {
            mUserInfo = um.getUserInfo(userId);
        }
        if (mAppsFragment != null) {
            mAppsFragment.setUser(new UserHandle(mUserInfo.id), mNewUser);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mFinishButton) {
            if (mNewUser) {
                AccountManager am = AccountManager.get(this);
                Account [] accounts = am.getAccounts();
                if (accounts != null) {
                    for (Account account : accounts) {
                        am.addSharedAccount(account,
                                new UserHandle(mUserInfo.id));
                    }
                }
            }

            IPackageManager ipm = IPackageManager.Stub.asInterface(
                    ServiceManager.getService("package"));
            for (Map.Entry<String,Boolean> entry : mAppsFragment.mSelectedPackages.entrySet()) {
                if (entry.getValue()) {
                    // Enable selected apps
                    try {
                        ipm.installExistingPackageAsUser(entry.getKey(), mUserInfo.id);
                    } catch (RemoteException re) {
                    }
                } else {
                    // Blacklist all other apps, system or downloaded
                    try {
                        ipm.deletePackageAsUser(entry.getKey(), null, mUserInfo.id,
                                PackageManager.DELETE_SYSTEM_APP);
                    } catch (RemoteException re) {
                    }
                }
            }
            setResult(RESULT_OK);
            mUserInfo = null;
            finish();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mUserInfo != null && mNewUser) {
            UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
            um.removeUser(mUserInfo.id);
        }
    }
}
