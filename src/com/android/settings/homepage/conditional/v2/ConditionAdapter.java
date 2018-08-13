/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.homepage.conditional.v2;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardAdapter;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.List;

public class ConditionAdapter extends RecyclerView.Adapter<DashboardAdapter.DashboardItemHolder> {

    private final Context mContext;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private final ConditionManager mConditionManager;
    private final List<ConditionalCard> mConditions;
    private final boolean mExpanded;

    public ConditionAdapter(Context context, ConditionManager conditionManager,
            List<ConditionalCard> conditions, boolean expanded) {
        mContext = context;
        mConditionManager = conditionManager;
        mConditions = conditions;
        mExpanded = expanded;
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();

        setHasStableIds(true);
    }

    @Override
    public DashboardAdapter.DashboardItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DashboardAdapter.DashboardItemHolder(LayoutInflater.from(parent.getContext())
                .inflate(viewType, parent, false));
    }

    @Override
    public void onBindViewHolder(DashboardAdapter.DashboardItemHolder holder, int position) {
        final ConditionalCard condition = mConditions.get(position);
        final boolean isLastItem = position == mConditions.size() - 1;
        bindViews(condition, holder, isLastItem);
    }

    @Override
    public long getItemId(int position) {
        return mConditions.get(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.condition_tile;
    }

    @Override
    public int getItemCount() {
        if (mExpanded) {
            return mConditions.size();
        }
        return 0;
    }

    private void bindViews(final ConditionalCard condition,
            DashboardAdapter.DashboardItemHolder view, boolean isLastItem) {
        mMetricsFeatureProvider.visible(mContext, MetricsProto.MetricsEvent.DASHBOARD_SUMMARY,
                condition.getMetricsConstant());
        view.itemView.findViewById(R.id.content).setOnClickListener(
                v -> {
                    mMetricsFeatureProvider.action(mContext,
                            MetricsProto.MetricsEvent.ACTION_SETTINGS_CONDITION_CLICK,
                            condition.getMetricsConstant());
                    mConditionManager.onPrimaryClick(mContext, condition.getId());
                });
        view.icon.setImageDrawable(condition.getIcon());
        view.title.setText(condition.getTitle());
        view.summary.setText(condition.getSummary());

        setViewVisibility(view.itemView, R.id.divider, !isLastItem);

        final CharSequence action = condition.getActionText();
        final boolean hasButtons = !TextUtils.isEmpty(action);
        setViewVisibility(view.itemView, R.id.buttonBar, hasButtons);

        final Button button = view.itemView.findViewById(R.id.first_action);
        if (hasButtons) {
            button.setVisibility(View.VISIBLE);
            button.setText(action);
            button.setOnClickListener(v -> {
                final Context context = v.getContext();
                mMetricsFeatureProvider.action(
                        context, MetricsProto.MetricsEvent.ACTION_SETTINGS_CONDITION_BUTTON,
                        condition.getMetricsConstant());
                mConditionManager.onActionClick(condition.getId());
            });
        } else {
            button.setVisibility(View.GONE);
        }

    }

    private void setViewVisibility(View containerView, int viewId, boolean visible) {
        View view = containerView.findViewById(viewId);
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
