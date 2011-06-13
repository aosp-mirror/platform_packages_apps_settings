/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.settings;

import static android.net.NetworkPolicyManager.POLICY_NONE;
import static android.net.NetworkPolicyManager.POLICY_REJECT_PAID_BACKGROUND;
import static android.net.TrafficStats.TEMPLATE_MOBILE_ALL;
import static com.android.settings.DataUsageSummary.getHistoryBounds;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.INetworkPolicyManager;
import android.net.INetworkStatsService;
import android.net.NetworkStatsHistory;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.settings.widget.DataUsageChartView;
import com.android.settings.widget.DataUsageChartView.DataUsageChartListener;

public class DataUsageAppDetail extends Fragment {
    private static final String TAG = "DataUsage";
    private static final boolean LOGD = true;

    private int mUid;

    private INetworkStatsService mStatsService;
    private INetworkPolicyManager mPolicyService;

    private CheckBoxPreference mRestrictBackground;
    private View mRestrictBackgroundView;

    private FrameLayout mChartContainer;
    private TextView mTitle;
    private TextView mText1;
    private Button mAppSettings;
    private LinearLayout mSwitches;

    private DataUsageChartView mChart;

    private int mUidPolicy;
    private NetworkStatsHistory mHistory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStatsService = INetworkStatsService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
        mPolicyService = INetworkPolicyManager.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_POLICY_SERVICE));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        final Context context = inflater.getContext();
        final View view = inflater.inflate(R.layout.data_usage_detail, container, false);

        mChartContainer = (FrameLayout) view.findViewById(R.id.chart_container);
        mTitle = (TextView) view.findViewById(android.R.id.title);
        mText1 = (TextView) view.findViewById(android.R.id.text1);
        mAppSettings = (Button) view.findViewById(R.id.data_usage_app_settings);
        mSwitches = (LinearLayout) view.findViewById(R.id.switches);

        mRestrictBackground = new CheckBoxPreference(context);
        mRestrictBackground.setTitle(R.string.data_usage_app_restrict_background);
        mRestrictBackground.setSummary(R.string.data_usage_app_restrict_background_summary);

        // kick refresh once to force-create views
        refreshPreferenceViews();

        mSwitches.addView(mRestrictBackgroundView);
        mRestrictBackgroundView.setOnClickListener(mRestrictBackgroundListener);

        mAppSettings.setOnClickListener(mAppSettingsListener);

        mChart = new DataUsageChartView(context);
        mChartContainer.addView(mChart);

        mChart.setListener(mChartListener);
        mChart.setChartColor(Color.parseColor("#d88d3a"), Color.parseColor("#c0ba7f3e"),
                Color.parseColor("#88566abc"));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        final Context context = getActivity();

        mUid = getArguments().getInt(Intent.EXTRA_UID);
        mTitle.setText(context.getPackageManager().getNameForUid(mUid));

        updateBody();
    }

    private void updateBody() {
        try {
            // load stats for current uid and template
            // TODO: read template from extras
            mUidPolicy = mPolicyService.getUidPolicy(mUid);
            mHistory = mStatsService.getHistoryForUid(mUid, TEMPLATE_MOBILE_ALL);
        } catch (RemoteException e) {
            // since we can't do much without policy or history, and we don't
            // want to leave with half-baked UI, we bail hard.
            throw new RuntimeException("problem reading network stats", e);
        }

        // bind chart to historical stats
        mChart.bindNetworkStats(mHistory);

        // show entire history known
        final long[] bounds = getHistoryBounds(mHistory);
        mChart.setVisibleRange(bounds[0], bounds[1] + DateUtils.WEEK_IN_MILLIS, bounds[1]);
        updateDetailData();

        // update policy checkbox
        final boolean restrictBackground = (mUidPolicy & POLICY_REJECT_PAID_BACKGROUND) != 0;
        mRestrictBackground.setChecked(restrictBackground);

        // kick preference views so they rebind from changes above
        refreshPreferenceViews();
    }

    private void updateDetailData() {
        if (LOGD) Log.d(TAG, "updateDetailData()");

        final Context context = mChart.getContext();
        final long[] range = mChart.getInspectRange();
        final long[] total = mHistory.getTotalData(range[0], range[1], null);
        final long totalCombined = total[0] + total[1];
        mText1.setText(Formatter.formatFileSize(context, totalCombined));
    }

    /**
     * Force rebind of hijacked {@link Preference} views.
     */
    private void refreshPreferenceViews() {
        mRestrictBackgroundView = mRestrictBackground.getView(mRestrictBackgroundView, mSwitches);
    }

    private DataUsageChartListener mChartListener = new DataUsageChartListener() {
        /** {@inheritDoc} */
        public void onInspectRangeChanged() {
            if (LOGD) Log.d(TAG, "onInspectRangeChanged()");
            updateDetailData();
        }

        /** {@inheritDoc} */
        public void onWarningChanged() {
            // ignored
        }

        /** {@inheritDoc} */
        public void onLimitChanged() {
            // ignored
        }
    };

    private OnClickListener mAppSettingsListener = new OnClickListener() {
        /** {@inheritDoc} */
        public void onClick(View v) {
            // TODO: target torwards entire UID instead of just first package
            final PackageManager pm = getActivity().getPackageManager();
            final String packageName = pm.getPackagesForUid(mUid)[0];

            final Intent intent = new Intent(Intent.ACTION_MANAGE_NETWORK_USAGE);
            intent.setPackage(packageName);
            startActivity(intent);
        }
    };

    private OnClickListener mRestrictBackgroundListener = new OnClickListener() {
        /** {@inheritDoc} */
        public void onClick(View v) {
            final boolean restrictBackground = !mRestrictBackground.isChecked();
            mRestrictBackground.setChecked(restrictBackground);
            refreshPreferenceViews();

            try {
                mPolicyService.setUidPolicy(
                        mUid, restrictBackground ? POLICY_REJECT_PAID_BACKGROUND : POLICY_NONE);
            } catch (RemoteException e) {
                throw new RuntimeException("unable to save policy", e);
            }
        }
    };

}
