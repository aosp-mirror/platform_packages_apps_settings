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

import android.annotation.Nullable;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.os.Bundle;
import android.security.AppUriAuthenticationPolicy;
import android.security.Credentials;
import android.security.KeyChain;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

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

    private boolean mDisplayingButtonPanel = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Credentials.ACTION_MANAGE_CREDENTIALS.equals(getIntent().getAction())) {
            setContentView(R.layout.request_manage_credentials);
            // This is not authenticated, as any app can ask to be the credential management app.
            mCredentialManagerPackage = getReferrer().getHost();
            mAuthenticationPolicy =
                    getIntent().getParcelableExtra(KeyChain.EXTRA_AUTHENTICATION_POLICY);
            enforceValidAuthenticationPolicy(mAuthenticationPolicy);

            loadRecyclerView();
            loadButtons();
            loadExtendedFloatingActionButton();
            addOnScrollListener();
        } else {
            Log.e(TAG, "Unable to start activity because intent action is not "
                    + Credentials.ACTION_MANAGE_CREDENTIALS);
            finish();
        }
    }

    private void loadRecyclerView() {
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView = findViewById(R.id.apps_list);
        mRecyclerView.setLayoutManager(mLayoutManager);

        CredentialManagementAppAdapter recyclerViewAdapter = new CredentialManagementAppAdapter(
                this, mCredentialManagerPackage, mAuthenticationPolicy.getAppAndUriMappings(),
                /* include header= */ true, /* include expander= */ false);
        mRecyclerView.setAdapter(recyclerViewAdapter);
    }

    private void loadButtons() {
        mButtonPanel = findViewById(R.id.button_panel);
        Button dontAllowButton = findViewById(R.id.dont_allow_button);
        Button allowButton = findViewById(R.id.allow_button);

        dontAllowButton.setOnClickListener(finishRequestManageCredentials());
        allowButton.setOnClickListener(setCredentialManagementApp());
    }

    private void loadExtendedFloatingActionButton() {
        mExtendedFab = findViewById(R.id.extended_fab);
        mExtendedFab.setOnClickListener(v -> {
            mRecyclerView.scrollToPosition(mAuthenticationPolicy.getAppAndUriMappings().size());
            mExtendedFab.hide();
            showButtonPanel();
        });
    }

    private View.OnClickListener finishRequestManageCredentials() {
        return v -> {
            Toast.makeText(this, R.string.request_manage_credentials_dont_allow,
                    Toast.LENGTH_SHORT).show();
            setResult(RESULT_CANCELED);
            finish();
        };
    }

    private View.OnClickListener setCredentialManagementApp() {
        return v -> {
            // TODO: Implement allow logic
            Toast.makeText(this, R.string.request_manage_credentials_allow,
                    Toast.LENGTH_SHORT).show();
            finish();
        };
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
                        mExtendedFab.setExtended(false);
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

    private void enforceValidAuthenticationPolicy(AppUriAuthenticationPolicy policy) {
        // TODO: Check whether any of the aliases in the policy already exist
        if (policy == null || policy.getAppAndUriMappings().isEmpty()) {
            Log.e(TAG, "Invalid authentication policy");
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}
