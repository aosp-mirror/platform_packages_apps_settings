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
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.overlay.SupportFeatureProvider;

/**
 * Fragment for support tab in SettingsGoogle.
 */
public final class SupportFragment extends InstrumentedFragment implements View.OnClickListener,
        OnAccountsUpdateListener {

    private Activity mActivity;
    private View mContent;
    private RecyclerView mRecyclerView;
    private SupportItemAdapter mSupportItemAdapter;
    private AccountManager mAccountManager;
    private SupportFeatureProvider mSupportFeatureProvider;

    @Override
    protected int getMetricsCategory() {
        return SUPPORT_FRAGMENT;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
        mAccountManager = AccountManager.get(mActivity);
        mSupportFeatureProvider =
                FeatureFactory.getFactory(mActivity).getSupportFeatureProvider(mActivity);
        mSupportItemAdapter = new SupportItemAdapter(mActivity, mSupportFeatureProvider,
                this /* itemClickListener */);
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
    }

    @Override
    public void onPause() {
        super.onPause();
        // Stop monitor account change.
        mAccountManager.removeOnAccountsUpdatedListener(this /* listener */);
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        // Account changed, update support items.
        mSupportItemAdapter.refreshData();
    }

    @Override
    public void onClick(View v) {
        final SupportItemAdapter.ViewHolder vh =
                (SupportItemAdapter.ViewHolder) mRecyclerView.getChildViewHolder(v);
        mSupportItemAdapter.onItemClicked(vh.getAdapterPosition());
    }
}
