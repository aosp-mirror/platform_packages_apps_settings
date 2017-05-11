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
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.dashboard.DashboardAdapter.DashboardItemHolder;
import com.android.settings.dashboard.DashboardData;
import com.android.settings.dashboard.DashboardData.HeaderMode;
import com.android.settings.overlay.FeatureFactory;
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
        // TODO: merge methods from ConditionAdapterUtils into this class
        ConditionAdapterUtils.bindViews(mConditions.get(position), holder,
            position == mConditions.size() - 1, mConditionClickListener);
    }

    @Override
    public long getItemId(int position) {
        return Objects.hash(mConditions.get(position).getTitle());
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.condition_tile_new_ui;
    }

    @Override
    public int getItemCount() {
        if (mMode == DashboardData.HEADER_MODE_FULLY_EXPANDED) {
            return mConditions.size();
        }
        return 0;
    }

}
