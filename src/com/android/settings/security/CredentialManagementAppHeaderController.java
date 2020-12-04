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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.security.IKeyChainService;
import android.security.KeyChain;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.LayoutPreference;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Controller that shows the header of the credential management app, which includes credential
 * management app's name, icon and a description.
 */
public class CredentialManagementAppHeaderController extends BasePreferenceController {

    private static final String TAG = "CredentialManagementApp";

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    public CredentialManagementAppHeaderController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mPackageManager = context.getPackageManager();
    }

    private final PackageManager mPackageManager;
    private boolean mHasCredentialManagerPackage;
    private String mCredentialManagerPackageName;

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE_UNSEARCHABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        mExecutor.execute(() -> {
            try {
                IKeyChainService service = KeyChain.bind(mContext).getService();
                mHasCredentialManagerPackage = service.hasCredentialManagementApp();
                mCredentialManagerPackageName = service.getCredentialManagementAppPackageName();
            } catch (InterruptedException | RemoteException e) {
                Log.e(TAG, "Unable to display credential management app header");
            }
            mHandler.post(() -> displayHeader(screen));
        });
    }

    private void displayHeader(PreferenceScreen screen) {
        LayoutPreference headerPref = screen.findPreference(getPreferenceKey());
        ImageView mAppIconView = headerPref.findViewById(R.id.entity_header_icon);
        TextView mTitleView = headerPref.findViewById(R.id.entity_header_title);
        TextView mDescriptionView = headerPref.findViewById(R.id.entity_header_summary);

        try {
            ApplicationInfo applicationInfo =
                    mPackageManager.getApplicationInfo(mCredentialManagerPackageName, 0);
            mAppIconView.setImageDrawable(mPackageManager.getApplicationIcon(applicationInfo));
            mTitleView.setText(applicationInfo.loadLabel(mPackageManager));
        } catch (PackageManager.NameNotFoundException e) {
            mAppIconView.setImageDrawable(null);
            mTitleView.setText(mCredentialManagerPackageName);
        }
        // TODO (b/165641221): The description should be multi-lined, which is currently a
        // limitation of using Settings entity header. However, the Settings entity header
        // should be used to be consistent with the rest of Settings.
        mDescriptionView.setText(
                mContext.getString(R.string.request_manage_credentials_description));
    }
}
