/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.dashboard.conditional;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.dashboard.DashboardAdapter;
import com.android.settings.dashboard.DashboardAdapter.DashboardItemHolder;
import com.android.settings.dashboard.DashboardData;
import com.android.settings.dashboard.DashboardData.HeaderMode;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.WirelessUtils;

import java.util.List;
import java.util.Objects;

public class ConditionAdapter extends RecyclerView.Adapter<DashboardItemHolder> {
    public static final String TAG = "ConditionAdapter";

    private final Context mContext;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private List<Condition> mConditions;
    private @HeaderMode int mMode;

    private View.OnClickListener mConditionClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            //TODO: get rid of setTag/getTag
            Condition condition = (Condition) v.getTag();
            mMetricsFeatureProvider.action(mContext,
                MetricsEvent.ACTION_SETTINGS_CONDITION_CLICK,
                condition.getMetricsConstant());
            condition.onPrimaryClick();
        }
    };

    @VisibleForTesting
    ItemTouchHelper.SimpleCallback mSwipeCallback = new ItemTouchHelper.SimpleCallback(0,
            ItemTouchHelper.START | ItemTouchHelper.END) {
        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                RecyclerView.ViewHolder target) {
            return true;
        }

        @Override
        public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return viewHolder.getItemViewType() == R.layout.condition_tile
                    ? super.getSwipeDirs(recyclerView, viewHolder) : 0;
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            Object item = getItem(viewHolder.getItemId());
            // item can become null when running monkey
            if (item != null) {
                ((Condition) item).silence();
            }
        }
    };

    public ConditionAdapter(Context context, List<Condition> conditions, @HeaderMode int mode) {
        mContext = context;
        mConditions = conditions;
        mMode = mode;
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();

        setHasStableIds(true);
    }

    public Object getItem(long itemId) {
        for (Condition condition : mConditions) {
            if (Objects.hash(condition.getTitle()) == itemId) {
                return condition;
            }
        }
        return null;
    }

    @Override
    public DashboardItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DashboardItemHolder(LayoutInflater.from(parent.getContext()).inflate(
                viewType, parent, false));
    }

    @Override
    public void onBindViewHolder(DashboardItemHolder holder, int position) {
        bindViews(mConditions.get(position), holder,
            position == mConditions.size() - 1, mConditionClickListener);
    }

    @Override
    public long getItemId(int position) {
        return Objects.hash(mConditions.get(position).getTitle());
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.condition_tile;
    }

    @Override
    public int getItemCount() {
        if (mMode == DashboardData.HEADER_MODE_FULLY_EXPANDED) {
            return mConditions.size();
        }
        return 0;
    }

    public void addDismissHandling(final RecyclerView recyclerView) {
        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(mSwipeCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void bindViews(final Condition condition,
            DashboardAdapter.DashboardItemHolder view, boolean isLastItem,
            View.OnClickListener onClickListener) {
        if (condition instanceof AirplaneModeCondition) {
            Log.d(TAG, "Airplane mode condition has been bound with "
                    + "isActive=" + condition.isActive() + ". Airplane mode is currently " +
                    WirelessUtils.isAirplaneModeOn(condition.mManager.getContext()));
        }
        View card = view.itemView.findViewById(R.id.content);
        card.setTag(condition);
        card.setOnClickListener(onClickListener);
        view.icon.setImageIcon(condition.getIcon());
        view.title.setText(condition.getTitle());

        CharSequence[] actions = condition.getActions();
        final boolean hasButtons = actions.length > 0;
        setViewVisibility(view.itemView, R.id.buttonBar, hasButtons);

        view.summary.setText(condition.getSummary());
        for (int i = 0; i < 2; i++) {
            Button button = (Button) view.itemView.findViewById(i == 0
                    ? R.id.first_action : R.id.second_action);
            if (actions.length > i) {
                button.setVisibility(View.VISIBLE);
                button.setText(actions[i]);
                final int index = i;
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context context = v.getContext();
                        FeatureFactory.getFactory(context).getMetricsFeatureProvider()
                                .action(context, MetricsEvent.ACTION_SETTINGS_CONDITION_BUTTON,
                                        condition.getMetricsConstant());
                        condition.onActionClick(index);
                    }
                });
            } else {
                button.setVisibility(View.GONE);
            }
        }
        setViewVisibility(view.itemView, R.id.divider, !isLastItem);
    }

    private void setViewVisibility(View containerView, int viewId, boolean visible) {
        View view = containerView.findViewById(viewId);
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
