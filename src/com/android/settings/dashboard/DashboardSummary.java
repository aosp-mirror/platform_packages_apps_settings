/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.ArrayUtils;
import com.android.settings.HelpUtils;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsActivity;
import com.android.settingslib.drawer.DashboardCategory;
import com.android.settingslib.drawer.DashboardTile;

import java.util.ArrayList;
import java.util.List;

public class DashboardSummary extends InstrumentedFragment {
    private static final boolean DEBUG = true;
    private static final String TAG = "DashboardSummary";

    public static final String[] INITIAL_ITEMS = new String[] {
            Settings.WifiSettingsActivity.class.getName(),
            Settings.DataUsageSummaryActivity.class.getName(),
            Settings.PowerUsageSummaryActivity.class.getName(),
            Settings.ManageApplicationsActivity.class.getName(),
            Settings.StorageSettingsActivity.class.getName(),
            Settings.DisplaySettingsActivity.class.getName(),
            Settings.NotificationSettingsActivity.class.getName(),
    };

    private static final int MSG_REBUILD_UI = 1;

    private final HomePackageReceiver mHomePackageReceiver = new HomePackageReceiver();

    private RecyclerView mDashboard;
    private DashboardAdapter mAdapter;

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.DASHBOARD_SUMMARY;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (getActivity() == null) return;
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, R.string.help_uri_dashboard,
                getClass().getName());
    }

    @Override
    public void onResume() {
        super.onResume();

        sendRebuildUI();

        final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        getActivity().registerReceiver(mHomePackageReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mHomePackageReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dashboard, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        mDashboard = (RecyclerView) view.findViewById(R.id.dashboard_container);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mDashboard.setLayoutManager(llm);
        mDashboard.setHasFixedSize(true);

        rebuildUI(getContext());
    }

    private void rebuildUI(Context context) {
        if (!isAdded()) {
            Log.w(TAG, "Cannot build the DashboardSummary UI yet as the Fragment is not added");
            return;
        }

        long start = System.currentTimeMillis();
        mAdapter = new DashboardAdapter(getContext(),
                ((SettingsActivity) getActivity()).getDashboardCategories(true));
        mDashboard.setAdapter(mAdapter);

        long delta = System.currentTimeMillis() - start;
        Log.d(TAG, "rebuildUI took: " + delta + " ms");
    }

    private void sendRebuildUI() {
        if (!mHandler.hasMessages(MSG_REBUILD_UI)) {
            mHandler.sendEmptyMessage(MSG_REBUILD_UI);
        }
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REBUILD_UI: {
                    final Context context = getActivity();
                    rebuildUI(context);
                } break;
            }
        }
    };

    private class HomePackageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            rebuildUI(context);
        }
    }

    private static class DashboardItemHolder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView title;
        private final TextView summary;

        public DashboardItemHolder(View itemView) {
            super(itemView);
            icon = (ImageView) itemView.findViewById(android.R.id.icon);
            title = (TextView) itemView.findViewById(android.R.id.title);
            summary = (TextView) itemView.findViewById(android.R.id.summary);
        }
    }

    private static class DashboardAdapter extends RecyclerView.Adapter<DashboardItemHolder> {

        private final List<Object> mItems = new ArrayList<>();
        private final List<Integer> mTypes = new ArrayList<>();
        private final List<Integer> mIds = new ArrayList<>();

        private final List<DashboardCategory> mCategories;
        private final Context mContext;

        private boolean mIsShowingAll;
        // Used for counting items;
        private int mId;

        public DashboardAdapter(Context context, List<DashboardCategory> categories) {
            mContext = context;
            mCategories = categories;

            // TODO: Better place for tinting?
            TypedValue tintColor = new TypedValue();
            context.getTheme().resolveAttribute(com.android.internal.R.attr.colorAccent,
                    tintColor, true);
            for (int i = 0; i < categories.size(); i++) {
                for (int j = 0; j < categories.get(i).tiles.size(); j++) {
                    DashboardTile tile = categories.get(i).tiles.get(j);

                    if (!context.getPackageName().equals(
                            tile.intent.getComponent().getPackageName())) {
                        // If this drawable is coming from outside Settings, tint it to match the
                        // color.
                        tile.icon.setTint(tintColor.data);
                    }
                }
            }

            setShowingAll(false);
            setHasStableIds(true);
        }

        public void setShowingAll(boolean showingAll) {
            mIsShowingAll = showingAll;
            reset();
            countItem(null, R.layout.dashboard_spacer, true);
            for (int i = 0; i < mCategories.size(); i++) {
                DashboardCategory category = mCategories.get(i);
                countItem(category, R.layout.dashboard_category, mIsShowingAll);
                for (int j = 0; j < category.tiles.size(); j++) {
                    DashboardTile tile = category.tiles.get(j);
                    Log.d(TAG, "Maybe adding " + tile.intent.getComponent().getClassName());
                    countItem(tile, R.layout.dashboard_tile, mIsShowingAll
                            || ArrayUtils.contains(INITIAL_ITEMS,
                            tile.intent.getComponent().getClassName()));
                }
            }
            countItem(null, R.layout.see_all, true);
            notifyDataSetChanged();
        }

        private void reset() {
            mItems.clear();
            mTypes.clear();
            mIds.clear();
            mId = 0;
        }

        private void countItem(Object object, int type, boolean add) {
            if (add) {
                mItems.add(object);
                mTypes.add(type);
                mIds.add(mId);
            }
            mId++;
        }

        @Override
        public DashboardItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DashboardItemHolder(LayoutInflater.from(parent.getContext()).inflate(
                    viewType, parent, false));
        }

        @Override
        public void onBindViewHolder(DashboardItemHolder holder, int position) {
            switch (mTypes.get(position)) {
                case R.layout.dashboard_category:
                    onBindCategory(holder, (DashboardCategory) mItems.get(position));
                    break;
                case R.layout.dashboard_tile:
                    final DashboardTile tile = (DashboardTile) mItems.get(position);
                    onBindTile(holder, tile);
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((SettingsActivity) mContext).openTile(tile);
                        }
                    });
                    break;
                case R.layout.see_all:
                    onBindSeeAll(holder);
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            setShowingAll(!mIsShowingAll);
                        }
                    });
                    break;
            }
        }

        private void onBindTile(DashboardItemHolder holder, DashboardTile dashboardTile) {
            holder.icon.setImageIcon(dashboardTile.icon);
            holder.title.setText(dashboardTile.title);
            if (!TextUtils.isEmpty(dashboardTile.summary)) {
                holder.summary.setText(dashboardTile.summary);
                holder.summary.setVisibility(View.VISIBLE);
            } else {
                holder.summary.setVisibility(View.GONE);
            }
        }

        private void onBindCategory(DashboardItemHolder holder, DashboardCategory category) {
            holder.title.setText(category.title);
        }

        private void onBindSeeAll(DashboardItemHolder holder) {
            holder.title.setText(mIsShowingAll ? R.string.see_less : R.string.see_all);
        }

        @Override
        public long getItemId(int position) {
            return mIds.get(position);
        }

        @Override
        public int getItemViewType(int position) {
            return mTypes.get(position);
        }

        @Override
        public int getItemCount() {
            return mIds.size();
        }
    }
}
