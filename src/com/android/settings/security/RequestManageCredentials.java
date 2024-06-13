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

import static android.view.WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS;

import android.app.Activity;
import android.app.admin.DevicePolicyEventLogger;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.RemoteException;
import android.os.UserManager;
import android.security.AppUriAuthenticationPolicy;
import android.security.Credentials;
import android.security.KeyChain;
import android.stats.devicepolicy.DevicePolicyEnums;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.Map;

/**
 * Displays a full screen to the user asking whether the calling app can manage the user's
 * KeyChain credentials. This screen includes the authentication policy highlighting what apps and
 * URLs the calling app can authenticate the user to.
 * <p>
 * Users can allow or deny the calling app. If denied, the calling app may re-request this
 * capability. If allowed, the calling app will become the credential management app and will be
 * able to manage the user's KeyChain credentials. The following APIs can be called to manage
 * KeyChain credentials:
 * {@link DevicePolicyManager#installKeyPair}
 * {@link DevicePolicyManager#removeKeyPair}
 * {@link DevicePolicyManager#generateKeyPair}
 * {@link DevicePolicyManager#setKeyPairCertificate}
 * <p>
 *
 * @see AppUriAuthenticationPolicy
 */
public class RequestManageCredentials extends Activity {

    private static final String TAG = "ManageCredentials";

    private String mCredentialManagerPackage;
    private AppUriAuthenticationPolicy mAuthenticationPolicy;

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private LinearLayout mButtonPanel;
    private ExtendedFloatingActionButton mExtendedFab;

    private HandlerThread mKeyChainTread;
    private KeyChain.KeyChainConnection mKeyChainConnection;

    private boolean mDisplayingButtonPanel = false;
    private boolean mIsLandscapeMode = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Credentials.ACTION_MANAGE_CREDENTIALS.equals(getIntent().getAction())) {
            Log.e(TAG, "Unable to start activity because intent action is not "
                    + Credentials.ACTION_MANAGE_CREDENTIALS);
            logRequestFailure();
            finishWithResultCancelled();
            return;
        }
        if (isManagedDevice()) {
            Log.e(TAG, "Credential management on managed devices should be done by the Device "
                    + "Policy Controller, not a credential management app");
            logRequestFailure();
            finishWithResultCancelled();
            return;
        }
        mCredentialManagerPackage = getLaunchedFromPackage();
        if (TextUtils.isEmpty(mCredentialManagerPackage)) {
            Log.e(TAG, "Unknown credential manager app");
            logRequestFailure();
            finishWithResultCancelled();
            return;
        }
        DevicePolicyEventLogger
                .createEvent(DevicePolicyEnums.CREDENTIAL_MANAGEMENT_APP_REQUEST_NAME)
                .setStrings(mCredentialManagerPackage)
                .write();
        setContentView(R.layout.request_manage_credentials);
        getWindow().addSystemFlags(SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS);
        mIsLandscapeMode = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;

        mKeyChainTread = new HandlerThread("KeyChainConnection");
        mKeyChainTread.start();
        mKeyChainConnection = getKeyChainConnection(this, mKeyChainTread);

        AppUriAuthenticationPolicy policy =
                getIntent().getParcelableExtra(KeyChain.EXTRA_AUTHENTICATION_POLICY);
        if (!isValidAuthenticationPolicy(policy)) {
            Log.e(TAG, "Invalid authentication policy");
            logRequestFailure();
            finishWithResultCancelled();
            return;
        }
        mAuthenticationPolicy = policy;
        DevicePolicyEventLogger
                .createEvent(DevicePolicyEnums.CREDENTIAL_MANAGEMENT_APP_REQUEST_POLICY)
                .setStrings(getNumberOfAuthenticationPolicyApps(mAuthenticationPolicy),
                        getNumberOfAuthenticationPolicyUris(mAuthenticationPolicy))
                .write();

        if (mIsLandscapeMode) {
            loadHeader();
        }
        loadRecyclerView();
        loadButtons();
        loadExtendedFloatingActionButton();
        addOnScrollListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mKeyChainConnection != null) {
            mKeyChainConnection.close();
            mKeyChainConnection = null;
            mKeyChainTread.quitSafely();
        }
    }

    private boolean isValidAuthenticationPolicy(AppUriAuthenticationPolicy policy) {
        if (policy == null || policy.getAppAndUriMappings().isEmpty()) {
            return false;
        }
        try {
            // Check whether any of the aliases in the policy already exist
            for (String alias : policy.getAliases()) {
                if (mKeyChainConnection.getService().requestPrivateKey(alias) != null) {
                    return false;
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Invalid authentication policy", e);
            return false;
        }
        return true;
    }

    private boolean isManagedDevice() {
        DevicePolicyManager dpm = getSystemService(DevicePolicyManager.class);

        return dpm.getDeviceOwnerUser() != null
                || dpm.getProfileOwner() != null
                || hasManagedProfile();
    }

    private boolean hasManagedProfile() {
        UserManager um = getSystemService(UserManager.class);
        for (final UserInfo userInfo : um.getProfiles(getUserId())) {
            if (userInfo.isManagedProfile()) {
                return true;
            }
        }
        return false;
    }

    private void loadRecyclerView() {
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView = findViewById(R.id.apps_list);
        mRecyclerView.setLayoutManager(mLayoutManager);

        CredentialManagementAppAdapter recyclerViewAdapter = new CredentialManagementAppAdapter(
                this, mCredentialManagerPackage, mAuthenticationPolicy.getAppAndUriMappings(),
                /* include header= */ !mIsLandscapeMode, /* include expander= */ false);
        mRecyclerView.setAdapter(recyclerViewAdapter);
    }

    private void loadButtons() {
        mButtonPanel = findViewById(R.id.button_panel);
        Button dontAllowButton = findViewById(R.id.dont_allow_button);
        dontAllowButton.setFilterTouchesWhenObscured(true);
        Button allowButton = findViewById(R.id.allow_button);
        allowButton.setFilterTouchesWhenObscured(true);

        dontAllowButton.setOnClickListener(b -> {
            DevicePolicyEventLogger
                    .createEvent(DevicePolicyEnums.CREDENTIAL_MANAGEMENT_APP_REQUEST_DENIED)
                    .write();
            finishWithResultCancelled();
        });
        allowButton.setOnClickListener(b -> setOrUpdateCredentialManagementAppAndFinish());
    }

    private void loadExtendedFloatingActionButton() {
        mExtendedFab = findViewById(R.id.extended_fab);
        mExtendedFab.setOnClickListener(v -> {
            final int position = mIsLandscapeMode
                    ? mAuthenticationPolicy.getAppAndUriMappings().size() - 1
                    : mAuthenticationPolicy.getAppAndUriMappings().size();
            mRecyclerView.scrollToPosition(position);
            mExtendedFab.hide();
            showButtonPanel();
        });
    }

    private void loadHeader() {
        final ImageView mAppIconView = findViewById(R.id.credential_management_app_icon);
        final TextView mTitleView = findViewById(R.id.credential_management_app_title);
        try {
            ApplicationInfo applicationInfo =
                    getPackageManager().getApplicationInfo(mCredentialManagerPackage, 0);
            mAppIconView.setImageDrawable(getPackageManager().getApplicationIcon(applicationInfo));
            mTitleView.setText(TextUtils.expandTemplate(
                    getText(R.string.request_manage_credentials_title),
                    applicationInfo.loadLabel(getPackageManager())));
        } catch (PackageManager.NameNotFoundException e) {
            mAppIconView.setImageDrawable(null);
            mTitleView.setText(TextUtils.expandTemplate(
                    getText(R.string.request_manage_credentials_title),
                    mCredentialManagerPackage));
        }
    }

    private void setOrUpdateCredentialManagementAppAndFinish() {
        try {
            mKeyChainConnection.getService().setCredentialManagementApp(
                    mCredentialManagerPackage, mAuthenticationPolicy);
            DevicePolicyEventLogger
                    .createEvent(DevicePolicyEnums.CREDENTIAL_MANAGEMENT_APP_REQUEST_ACCEPTED)
                    .write();
            setResult(RESULT_OK);
        } catch (RemoteException e) {
            Log.e(TAG, "Unable to set credential manager app", e);
            logRequestFailure();
        }
        finish();
    }

    @VisibleForTesting
    KeyChain.KeyChainConnection getKeyChainConnection(Context context, HandlerThread thread) {
        final Handler handler = new Handler(thread.getLooper());
        try {
            KeyChain.KeyChainConnection connection = KeyChain.bindAsUser(
                    context, handler, Process.myUserHandle());
            return connection;
        } catch (InterruptedException e) {
            throw new RuntimeException("Faile to bind to KeyChain", e);
        }
    }

    private void addOnScrollListener() {
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!mDisplayingButtonPanel) {
                    // On down scroll, hide text in floating action button by setting
                    // extended to false.
                    if (dy > 0 && mExtendedFab.getVisibility() == View.VISIBLE) {
                        mExtendedFab.shrink();
                    }
                    if (isRecyclerScrollable()) {
                        mExtendedFab.show();
                        hideButtonPanel();
                    } else {
                        mExtendedFab.hide();
                        showButtonPanel();
                    }
                }
            }
        });
    }

    private void showButtonPanel() {
        // Add padding to remove overlap between recycler view and button panel.
        int padding_in_px = (int) (60 * getResources().getDisplayMetrics().density + 0.5f);
        mRecyclerView.setPadding(0, 0, 0, padding_in_px);
        mButtonPanel.setVisibility(View.VISIBLE);
        mDisplayingButtonPanel = true;
    }

    private void hideButtonPanel() {
        mRecyclerView.setPadding(0, 0, 0, 0);
        mButtonPanel.setVisibility(View.GONE);
    }

    private boolean isRecyclerScrollable() {
        if (mLayoutManager == null || mRecyclerView.getAdapter() == null) {
            return false;
        }
        return mLayoutManager.findLastCompletelyVisibleItemPosition()
                < mRecyclerView.getAdapter().getItemCount() - 1;
    }

    private void finishWithResultCancelled() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void logRequestFailure() {
        DevicePolicyEventLogger
                .createEvent(DevicePolicyEnums.CREDENTIAL_MANAGEMENT_APP_REQUEST_FAILED)
                .write();
    }

    private String getNumberOfAuthenticationPolicyUris(AppUriAuthenticationPolicy policy) {
        int numberOfUris = 0;
        for (Map.Entry<String, Map<Uri, String>> appsToUris :
                policy.getAppAndUriMappings().entrySet()) {
            numberOfUris += appsToUris.getValue().size();
        }
        return String.valueOf(numberOfUris);
    }

    private String getNumberOfAuthenticationPolicyApps(AppUriAuthenticationPolicy policy) {
        return String.valueOf(policy.getAppAndUriMappings().size());
    }

}
