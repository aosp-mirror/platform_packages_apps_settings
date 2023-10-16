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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settingslib.utils.StringUtil;

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

    private final boolean mIncludeHeader;
    private final boolean mIncludeExpander;
    private final boolean mIsLayoutRtl;

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
                mTitleView.setText(TextUtils.expandTemplate(
                        mContext.getText(R.string.request_manage_credentials_title),
                        applicationInfo.loadLabel(mPackageManager)));
            } catch (PackageManager.NameNotFoundException e) {
                mAppIconView.setImageDrawable(null);
                mTitleView.setText(TextUtils.expandTemplate(
                        mContext.getText(R.string.request_manage_credentials_title),
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
        private final TextView mNumberOfUrisView;
        private final ImageView mExpanderIconView;
        private final RecyclerView mChildRecyclerView;
        private final List<String> mExpandedApps;

        public AppAuthenticationViewHolder(View view) {
            super(view);
            mAppIconView = view.findViewById(R.id.app_icon);
            mAppNameView = view.findViewById(R.id.app_name);
            mNumberOfUrisView = view.findViewById(R.id.number_of_uris);
            mExpanderIconView = view.findViewById(R.id.expand);
            mChildRecyclerView = view.findViewById(R.id.uris);
            mExpandedApps = new ArrayList<>();

            if (mIsLayoutRtl) {
                RelativeLayout appDetails = view.findViewById(R.id.app_details);
                RelativeLayout.LayoutParams params =
                        (RelativeLayout.LayoutParams) appDetails.getLayoutParams();
                params.addRule(RelativeLayout.LEFT_OF, R.id.app_icon);
                params.addRule(RelativeLayout.RIGHT_OF, R.id.expand);
                view.setLayoutParams(params);
            }

            mExpanderIconView.setOnClickListener(view1 -> {
                final String appName = mSortedAppNames.get(getBindingAdapterPosition());
                if (mExpandedApps.contains(appName)) {
                    mExpandedApps.remove(appName);
                } else {
                    mExpandedApps.add(appName);
                }
                bindPolicyView(appName);
            });
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
            bindPolicyView(appName);
        }

        private void bindPolicyView(String appName) {
            if (mIncludeExpander) {
                mExpanderIconView.setVisibility(View.VISIBLE);
                if (mExpandedApps.contains(appName)) {
                    mNumberOfUrisView.setVisibility(View.GONE);
                    mExpanderIconView.setImageResource(R.drawable.ic_expand_less);
                    bindChildView(mAppUriAuthentication.get(appName));
                } else {
                    mChildRecyclerView.setVisibility(View.GONE);
                    mNumberOfUrisView.setVisibility(View.VISIBLE);
                    mNumberOfUrisView.setText(
                            getNumberOfUrlsText(mAppUriAuthentication.get(appName)));
                    mExpanderIconView.setImageResource(
                            com.android.internal.R.drawable.ic_expand_more);
                }
            } else {
                mNumberOfUrisView.setVisibility(View.GONE);
                mExpanderIconView.setVisibility(View.GONE);
                bindChildView(mAppUriAuthentication.get(appName));
            }
        }

        /**
         * Bind the list of URIs for an app.
         */
        private void bindChildView(Map<Uri, String> urisToAliases) {
            LinearLayoutManager layoutManager = new LinearLayoutManager(
                    mChildRecyclerView.getContext(), RecyclerView.VERTICAL, false);
            layoutManager.setInitialPrefetchItemCount(urisToAliases.size());
            UriAuthenticationPolicyAdapter childItemAdapter =
                    new UriAuthenticationPolicyAdapter(new ArrayList<>(urisToAliases.keySet()));
            mChildRecyclerView.setLayoutManager(layoutManager);
            mChildRecyclerView.setVisibility(View.VISIBLE);
            mChildRecyclerView.setAdapter(childItemAdapter);
            mChildRecyclerView.setRecycledViewPool(mViewPool);
        }

        private String getNumberOfUrlsText(Map<Uri, String> urisToAliases) {
            return StringUtil.getIcuPluralsString(mContext, urisToAliases.size(),
                    R.string.number_of_urls);
        }
    }

    public CredentialManagementAppAdapter(Context context, String credentialManagerPackage,
            Map<String, Map<Uri, String>> appUriAuthentication,
            boolean includeHeader, boolean includeExpander) {
        mContext = context;
        mCredentialManagerPackage = credentialManagerPackage;
        mPackageManager = context.getPackageManager();
        mAppUriAuthentication = appUriAuthentication;
        mSortedAppNames = sortPackageNames(mAppUriAuthentication);
        mViewPool = new RecyclerView.RecycledViewPool();
        mIncludeHeader = includeHeader;
        mIncludeExpander = includeExpander;
        mIsLayoutRtl = context.getResources().getConfiguration().getLayoutDirection()
                == View.LAYOUT_DIRECTION_RTL;
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
            int position = mIncludeHeader ? i - 1 : i;
            ((AppAuthenticationViewHolder) viewHolder).bindView(position);
        }
    }

    @Override
    public int getItemCount() {
        // Add an extra view to show the header view
        return mIncludeHeader ? mAppUriAuthentication.size() + 1 : mAppUriAuthentication.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mIncludeHeader && position == 0) {
            return HEADER_VIEW;
        }
        return super.getItemViewType(position);
    }

}
