/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.security;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.security.AppUriAuthenticationPolicy;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Preference that shows the credential management app's authentication policy.
 */
public class CredentialManagementAppPolicyPreference extends Preference {

    private static final String TAG = "CredentialManagementApp";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private final Context mContext;

    private boolean mHasCredentialManagerPackage;
    private String mCredentialManagerPackageName;
    private AppUriAuthenticationPolicy mCredentialManagerPolicy;

    public CredentialManagementAppPolicyPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.credential_management_app_policy);
        mContext = context;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        mExecutor.execute(() -> {
            try {
                IKeyChainService service = KeyChain.bind(mContext).getService();
                mHasCredentialManagerPackage = service.hasCredentialManagementApp();
                mCredentialManagerPackageName = service.getCredentialManagementAppPackageName();
                mCredentialManagerPolicy = service.getCredentialManagementAppPolicy();
            } catch (InterruptedException | RemoteException e) {
                Log.e(TAG, "Unable to display credential management app policy");
            }
            mHandler.post(() -> displayPolicy(view));
        });
    }

    private void displayPolicy(PreferenceViewHolder view) {
        if (mHasCredentialManagerPackage) {
            RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
            recyclerView.setLayoutManager(new LinearLayoutManager(mContext));

            CredentialManagementAppAdapter recyclerViewAdapter = new CredentialManagementAppAdapter(
                    mContext, mCredentialManagerPackageName,
                    mCredentialManagerPolicy.getAppAndUriMappings(),
                    /* include header= */ false, /* include expander= */ true);
            recyclerView.setAdapter(recyclerViewAdapter);
        }
    }
}
