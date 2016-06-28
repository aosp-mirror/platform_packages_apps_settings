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

package com.android.settings.dashboard;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.MetricsProto;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SupportFeatureProvider;

/**
 * Fragment for support tab in SettingsGoogle.
 */
public final class SupportFragment extends InstrumentedFragment implements View.OnClickListener,
        OnAccountsUpdateListener {

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final ConnectivityManager.NetworkCallback mNetworkCallback =
            new ConnectivityManager.NetworkCallback() {

                @Override
                public void onCapabilitiesChanged(Network network,
                        NetworkCapabilities capabilities) {
                    postConnectivityChanged();
                }

                @Override
                public void onAvailable(Network network) {
                    postConnectivityChanged();
                }

                @Override
                public void onLost(Network network) {
                    postConnectivityChanged();
                }
            };

    private Activity mActivity;
    private View mContent;
    private RecyclerView mRecyclerView;
    private SupportItemAdapter mSupportItemAdapter;
    private AccountManager mAccountManager;
    private SupportFeatureProvider mSupportFeatureProvider;
    private ConnectivityManager mConnectivityManager;

    @Override
    protected int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SUPPORT_FRAGMENT;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        mAccountManager = AccountManager.get(mActivity);
        mSupportFeatureProvider =
                FeatureFactory.getFactory(mActivity).getSupportFeatureProvider(mActivity);
        mSupportItemAdapter = new SupportItemAdapter(mActivity, savedInstanceState,
                mSupportFeatureProvider, this /* itemClickListener */);
        mConnectivityManager =
                (ConnectivityManager) mActivity.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContent = inflater.inflate(R.layout.support_fragment, container, false);
        mRecyclerView = (RecyclerView) mContent.findViewById(R.id.support_items);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(
                getActivity(), LinearLayoutManager.VERTICAL, false /* reverseLayout */));
        mRecyclerView.setAdapter(mSupportItemAdapter);
        return mContent;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Monitor account change.
        mAccountManager.addOnAccountsUpdatedListener(
                this /* listener */, null /* handler */, true /* updateImmediately */);
        // Monitor connectivity
        mConnectivityManager.registerNetworkCallback(
                new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build(),
                mNetworkCallback);
        mSupportItemAdapter.setHasInternet(hasInternet());
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop monitor account change.
        mAccountManager.removeOnAccountsUpdatedListener(this /* listener */);
        // Stop monitor connectivity.
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mSupportItemAdapter.onSaveInstanceState(outState);
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        // Account changed, update support items.
        mSupportItemAdapter.setAccount(
                mSupportFeatureProvider.getSupportEligibleAccount(mActivity));
    }

    @Override
    public void onClick(View v) {
        final SupportItemAdapter.ViewHolder vh =
                (SupportItemAdapter.ViewHolder) mRecyclerView.getChildViewHolder(v);
        mSupportItemAdapter.onItemClicked(vh.getAdapterPosition());
    }

    private void postConnectivityChanged() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mSupportItemAdapter != null) {
                    mSupportItemAdapter.setHasInternet(hasInternet());
                }
            }
        });
    }

    private boolean hasInternet() {
        final NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}
