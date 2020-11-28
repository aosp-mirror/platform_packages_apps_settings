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
import android.net.Uri;
import android.security.AppUriAuthenticationPolicy;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapter for the requesting credential management app. This adapter displays the details of the
 * requesting app, including its authentication policy, when {@link RequestManageCredentials}
 * is started.
 * <p>
 *
 * @hide
 * @see RequestManageCredentials
 * @see AppUriAuthenticationPolicy
 */
public class CredentialManagementAppAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int HEADER_VIEW = 1;

    private final String mCredentialManagerPackage;
    private final Map<String, Map<Uri, String>> mAppUriAuthentication;
    private final List<String> mSortedAppNames;

    private final Context mContext;
    private final PackageManager mPackageManager;
    private final RecyclerView.RecycledViewPool mViewPool;

    /**
     * View holder for the header in the request manage credentials screen.
     */
    public class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mAppIconView;
        private final TextView mTitleView;

        public HeaderViewHolder(View view) {
            super(view);
            mAppIconView = view.findViewById(R.id.credential_management_app_icon);
            mTitleView = view.findViewById(R.id.credential_management_app_title);
        }

        /**
         * Bind the header view and add details on the requesting app's icon and name.
         */
        public void bindView() {
            try {
                ApplicationInfo applicationInfo =
                        mPackageManager.getApplicationInfo(mCredentialManagerPackage, 0);
                mAppIconView.setImageDrawable(mPackageManager.getApplicationIcon(applicationInfo));
                mTitleView.setText(mContext.getString(R.string.request_manage_credentials_title,
                        applicationInfo.loadLabel(mPackageManager)));
            } catch (PackageManager.NameNotFoundException e) {
                mAppIconView.setImageDrawable(null);
                mTitleView.setText(mContext.getString(R.string.request_manage_credentials_title,
                        mCredentialManagerPackage));
            }
        }
    }

    /**
     * View holder for the authentication policy in the request manage credentials screen.
     */
    public class AppAuthenticationViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mAppIconView;
        private final TextView mAppNameView;
        RecyclerView mChildRecyclerView;

        public AppAuthenticationViewHolder(View view) {
            super(view);
            mAppIconView = view.findViewById(R.id.app_icon);
            mAppNameView = view.findViewById(R.id.app_name);
            mChildRecyclerView = view.findViewById(R.id.uris);
        }

        /**
         * Bind the app's authentication policy view at the given position. Add details on the
         * app's icon, name and list of URIs.
         */
        public void bindView(int position) {
            final String appName = mSortedAppNames.get(position);
            try {
                ApplicationInfo applicationInfo = mPackageManager.getApplicationInfo(appName, 0);
                mAppIconView.setImageDrawable(mPackageManager.getApplicationIcon(applicationInfo));
                mAppNameView.setText(String.valueOf(applicationInfo.loadLabel(mPackageManager)));
            } catch (PackageManager.NameNotFoundException e) {
                mAppIconView.setImageDrawable(null);
                mAppNameView.setText(appName);
            }
            bindChildView(mAppUriAuthentication.get(appName));
        }

        /**
         * Bind the list of URIs for an app.
         */
        public void bindChildView(Map<Uri, String> urisToAliases) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(
                    mChildRecyclerView.getContext(), RecyclerView.VERTICAL, false);
            layoutManager.setInitialPrefetchItemCount(urisToAliases.size());
            UriAuthenticationPolicyAdapter childItemAdapter =
                    new UriAuthenticationPolicyAdapter(new ArrayList<>(urisToAliases.keySet()));
            mChildRecyclerView.setLayoutManager(layoutManager);
            mChildRecyclerView.setAdapter(childItemAdapter);
            mChildRecyclerView.setRecycledViewPool(mViewPool);
        }
    }

    public CredentialManagementAppAdapter(Context context, String credentialManagerPackage,
            Map<String, Map<Uri, String>> appUriAuthentication) {
        mContext = context;
        mCredentialManagerPackage = credentialManagerPackage;
        mPackageManager = context.getPackageManager();
        mAppUriAuthentication = appUriAuthentication;
        mSortedAppNames = sortPackageNames(mAppUriAuthentication);
        mViewPool = new RecyclerView.RecycledViewPool();
    }

    /**
     * Sort package names in the following order:
     * - installed apps
     * - alphabetically
     */
    private List<String> sortPackageNames(Map<String, Map<Uri, String>> authenticationPolicy) {
        List<String> packageNames = new ArrayList<>(authenticationPolicy.keySet());
        packageNames.sort((firstPackageName, secondPackageName) -> {
            boolean isFirstPackageInstalled = isPackageInstalled(firstPackageName);
            boolean isSecondPackageInstalled = isPackageInstalled(secondPackageName);
            if (isFirstPackageInstalled == isSecondPackageInstalled) {
                return firstPackageName.compareTo(secondPackageName);
            } else if (isFirstPackageInstalled) {
                return -1;
            } else {
                return 1;
            }
        });
        return packageNames;
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            mPackageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view;
        if (viewType == HEADER_VIEW) {
            view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.request_manage_credentials_header, viewGroup, false);
            view.setEnabled(false);
            return new HeaderViewHolder(view);
        } else {
            view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.app_authentication_item, viewGroup, false);
            return new AppAuthenticationViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        if (viewHolder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) viewHolder).bindView();
        } else if (viewHolder instanceof AppAuthenticationViewHolder) {
            ((AppAuthenticationViewHolder) viewHolder).bindView(i - 1);
        }
    }

    @Override
    public int getItemCount() {
        // Add an extra view to show the header view
        return mAppUriAuthentication.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return HEADER_VIEW;
        }
        return super.getItemViewType(position);
    }

}
