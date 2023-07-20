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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.EventLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.android.internal.util.CollectionUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.network.ActiveSubscriptionsListener;
import com.android.settings.network.SubscriptionUtil;
import com.android.settings.network.ims.WifiCallingQueryImsState;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.search.actionbar.SearchMenuController;
import com.android.settings.support.actionbar.HelpResourceProvider;
import com.android.settings.widget.RtlCompatibleViewPager;
import com.android.settings.widget.SlidingTabLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * "Wi-Fi Calling settings" screen. This is the container fragment which holds
 * {@link WifiCallingSettingsForSub} fragments.
 */
public class WifiCallingSettings extends SettingsPreferenceFragment
        implements HelpResourceProvider {
    private static final String TAG = "WifiCallingSettings";
    private int mConstructionSubId;
    private List<SubscriptionInfo> mSil;
    private ActiveSubscriptionsListener mSubscriptionChangeListener;
    private static final int [] EMPTY_SUB_ID_LIST = new int[0];

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
        if (MobileNetworkUtils.isMobileNetworkUserRestricted(getActivity())) {
            return new ViewStub(getActivity());
        }
        final View view = inflater.inflate(R.layout.wifi_calling_settings_tabs, container, false);

        mTabLayout = view.findViewById(R.id.sliding_tabs);
        mViewPager = (RtlCompatibleViewPager) view.findViewById(R.id.view_pager);

        mPagerAdapter = new WifiCallingViewPagerAdapter(getChildFragmentManager(), mViewPager);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.addOnPageChangeListener(new InternalViewPagerListener());
        maybeSetViewForSubId();
        return view;
    }

    private int getConstructionSubId(Bundle bundle) {
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        Intent intent = getActivity().getIntent();
        if (intent != null) {
            subId = intent.getIntExtra(Settings.EXTRA_SUB_ID,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        }
        if ((subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) && (bundle != null)) {
            subId = bundle.getInt(Settings.EXTRA_SUB_ID,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        }
        return subId;
    }

    private void maybeSetViewForSubId() {
        if (mSil == null) {
            return;
        }
        int subId = mConstructionSubId;
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
        mConstructionSubId = getConstructionSubId(icicle);
        super.onCreate(icicle);
        if (MobileNetworkUtils.isMobileNetworkUserRestricted(getActivity())) {
            Log.e(TAG, "This setting isn't available due to user restriction.");
            EventLog.writeEvent(0x534e4554, "262243015", UserHandle.myUserId(), "User restricted");
            finish();
            return;
        }
        Log.d(TAG, "SubId=" + mConstructionSubId);

        if (mConstructionSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            // Only config Wfc if it's enabled by platform.
            mSubscriptionChangeListener = getSubscriptionChangeListener(getContext());
        }
        mSil = updateSubList();
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

        if (mSubscriptionChangeListener != null) {
            mSubscriptionChangeListener.start();
        }
    }

    @Override
    public void onStop() {
        if (mSubscriptionChangeListener != null) {
            mSubscriptionChangeListener.stop();
        }

        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // keep subscription ID for recreation
        outState.putInt(Settings.EXTRA_SUB_ID, mConstructionSubId);
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
            return String.valueOf(SubscriptionUtil.getUniqueSubscriptionDisplayName(
                    mSil.get(position), getContext()));
        }

        @Override
        public Fragment getItem(int position) {
            int subId = mSil.get(position).getSubscriptionId();
            Log.d(TAG, "Adapter getItem " + position + " for subId=" + subId);
            final Bundle args = new Bundle();
            args.putBoolean(SearchMenuController.NEED_SEARCH_ICON_IN_ACTION_BAR, false);
            args.putInt(WifiCallingSettingsForSub.FRAGMENT_BUNDLE_SUBID, subId);
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

    @VisibleForTesting
    protected List<SubscriptionInfo> getSelectableSubscriptions(Context context) {
        return SubscriptionUtil.getSelectableSubscriptionInfoList(context);
    }

    private List<SubscriptionInfo> updateSubList() {
        List<SubscriptionInfo> subInfoList = getSelectableSubscriptions(getContext());

        if (subInfoList == null) {
            return Collections.emptyList();
        }
        List<SubscriptionInfo> selectedList = new ArrayList<SubscriptionInfo>();
        for (SubscriptionInfo subInfo : subInfoList) {
            int subId = subInfo.getSubscriptionId();
            try {
                if (queryImsState(subId).isWifiCallingProvisioned()) {
                    selectedList.add(subInfo);
                }
            } catch (Exception exception) {}
        }
        return selectedList;
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
    protected WifiCallingQueryImsState queryImsState(int subId) {
        return new WifiCallingQueryImsState(getContext(), subId);
    }

    @VisibleForTesting
    protected ActiveSubscriptionsListener getSubscriptionChangeListener(Context context) {
        return new ActiveSubscriptionsListener(context.getMainLooper(), context) {
            public void onChanged() {
                onSubscriptionChange(context);
            }
        };
    }

    protected void onSubscriptionChange(Context context) {
        if (mSubscriptionChangeListener == null) {
            return;
        }
        int [] previousSubIdList = subscriptionIdList(mSil);
        List<SubscriptionInfo> updateList = updateSubList();
        int [] currentSubIdList = subscriptionIdList(updateList);

        if (currentSubIdList.length > 0) {
            // only keep fragment when any provisioned subscription is available
            if (previousSubIdList.length == 0) {
                // initial loading of list
                mSil = updateList;
                return;
            }
            if (previousSubIdList.length == currentSubIdList.length) {
                // same number of subscriptions
                if ( (!containsSubId(previousSubIdList, mConstructionSubId))
                        // original request not yet appears in list
                        || containsSubId(currentSubIdList, mConstructionSubId) )
                        // original request appears in list
                {
                    mSil = updateList;
                    return;
                }
            }
        }
        Log.d(TAG, "Closed subId=" + mConstructionSubId
                + " due to subscription change: " + Arrays.toString(previousSubIdList)
                + " -> " + Arrays.toString(currentSubIdList));

        // close this fragment when no provisioned subscriptions available
        if (mSubscriptionChangeListener != null) {
            mSubscriptionChangeListener.stop();
            mSubscriptionChangeListener = null;
        }

        // close this fragment
        finishFragment();
    }

    protected int [] subscriptionIdList(List<SubscriptionInfo> subInfoList) {
        return (subInfoList == null) ? EMPTY_SUB_ID_LIST :
                subInfoList.stream().mapToInt(subInfo -> (subInfo == null) ?
                SubscriptionManager.INVALID_SUBSCRIPTION_ID : subInfo.getSubscriptionId())
                .toArray();
    }

    protected boolean containsSubId(int [] subIdArray, int subIdLookUp) {
        return Arrays.stream(subIdArray).anyMatch(subId -> (subId == subIdLookUp));
    }
}
