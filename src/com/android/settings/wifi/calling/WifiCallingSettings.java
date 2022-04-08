/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.wifi.calling;

import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.android.internal.util.CollectionUtils;
import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.ims.WifiCallingQueryImsState;
import com.android.settings.search.actionbar.SearchMenuController;
import com.android.settings.support.actionbar.HelpMenuController;
import com.android.settings.support.actionbar.HelpResourceProvider;
import com.android.settings.widget.RtlCompatibleViewPager;
import com.android.settings.widget.SlidingTabLayout;

import java.util.List;

/**
 * "Wi-Fi Calling settings" screen. This is the container fragment which holds
 * {@link WifiCallingSettingsForSub} fragments.
 */
public class WifiCallingSettings extends InstrumentedFragment implements HelpResourceProvider {
    private static final String TAG = "WifiCallingSettings";
    private List<SubscriptionInfo> mSil;

    //UI objects
    private RtlCompatibleViewPager mViewPager;
    private WifiCallingViewPagerAdapter mPagerAdapter;
    private SlidingTabLayout mTabLayout;

    private final class InternalViewPagerListener implements
            RtlCompatibleViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            // Do nothing.
        }

        @Override
        public void onPageSelected(int position) {
            updateTitleForCurrentSub();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            // Do nothing.
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.WIFI_CALLING;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.wifi_calling_settings_tabs, container, false);

        mTabLayout = view.findViewById(R.id.sliding_tabs);
        mViewPager = (RtlCompatibleViewPager) view.findViewById(R.id.view_pager);

        mPagerAdapter = new WifiCallingViewPagerAdapter(getChildFragmentManager(), mViewPager);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(new InternalViewPagerListener());
        maybeSetViewForSubId();
        return view;
    }

    private void maybeSetViewForSubId() {
        if (mSil == null) {
            return;
        }
        final Intent intent = getActivity().getIntent();
        if (intent == null) {
            return;
        }
        final int subId = intent.getIntExtra(Settings.EXTRA_SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            for (SubscriptionInfo subInfo : mSil) {
                if (subId == subInfo.getSubscriptionId()) {
                    mViewPager.setCurrentItem(mSil.indexOf(subInfo));
                    break;
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setHasOptionsMenu(true);
        SearchMenuController.init(this /* host */);
        HelpMenuController.init(this /* host */);

        // TODO: besides in onCreate, we should also update subList when SIM / Sub status
        // changes.
        updateSubList();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mSil != null && mSil.size() > 1) {
            mTabLayout.setViewPager(mViewPager);
        } else {
            mTabLayout.setVisibility(View.GONE);
        }

        updateTitleForCurrentSub();
    }

    @Override
    public int getHelpResource() {
        return R.string.help_uri_wifi_calling;
    }

    @VisibleForTesting
    final class WifiCallingViewPagerAdapter extends FragmentPagerAdapter {
        private final RtlCompatibleViewPager mViewPager;

        public WifiCallingViewPagerAdapter(FragmentManager fragmentManager,
                RtlCompatibleViewPager viewPager) {
            super(fragmentManager);
            mViewPager = viewPager;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return String.valueOf(mSil.get(position).getDisplayName());
        }

        @Override
        public Fragment getItem(int position) {
            Log.d(TAG, "Adapter getItem " + position);
            final Bundle args = new Bundle();
            args.putBoolean(SearchMenuController.NEED_SEARCH_ICON_IN_ACTION_BAR, false);
            args.putInt(WifiCallingSettingsForSub.FRAGMENT_BUNDLE_SUBID,
                    mSil.get(position).getSubscriptionId());
            final WifiCallingSettingsForSub fragment = new WifiCallingSettingsForSub();
            fragment.setArguments(args);

            return fragment;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Log.d(TAG, "Adapter instantiateItem " + position);
            return super.instantiateItem(container,
                    mViewPager.getRtlAwareIndex(position));
        }

        @Override
        public int getCount() {
            if (mSil == null) {
                Log.d(TAG, "Adapter getCount null mSil ");
                return 0;
            } else {
                Log.d(TAG, "Adapter getCount " + mSil.size());
                return mSil.size();
            }
        }
    }

    private void updateSubList() {
        mSil = SubscriptionUtil.getActiveSubscriptions(
                getContext().getSystemService(SubscriptionManager.class));

        // Only config Wfc if it's enabled by platform.
        if (mSil == null) {
            return;
        }
        for (int i = 0; i < mSil.size(); ) {
            final SubscriptionInfo info = mSil.get(i);
            if (!queryImsState(info.getSubscriptionId()).isWifiCallingProvisioned()) {
                mSil.remove(i);
            } else {
                i++;
            }
        }
    }

    private void updateTitleForCurrentSub() {
        if (CollectionUtils.size(mSil) > 1) {
            final int subId = mSil.get(mViewPager.getCurrentItem()).getSubscriptionId();
            final String title = SubscriptionManager.getResourcesForSubId(getContext(), subId)
                    .getString(R.string.wifi_calling_settings_title);
            getActivity().getActionBar().setTitle(title);
        }
    }

    @VisibleForTesting
    WifiCallingQueryImsState queryImsState(int subId) {
        return new WifiCallingQueryImsState(getContext(), subId);
    }
}
