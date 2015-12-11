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
package com.android.settings.dashboard.conditional;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardAdapter;

public class ConditionAdapterUtils {

    public static void addDismiss(final RecyclerView recyclerView) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.START | ItemTouchHelper.END) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                    RecyclerView.ViewHolder target) {
                return true;
            }

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                return viewHolder.getItemViewType() == R.layout.condition_card
                        ? super.getSwipeDirs(recyclerView, viewHolder) : 0;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                DashboardAdapter adapter = (DashboardAdapter) recyclerView.getAdapter();
                Object item = adapter.getItem(viewHolder.getItemId());
                if (item instanceof Condition) {
                    ((Condition) item).silence();
                }
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    public static void bindViews(final Condition condition,
            DashboardAdapter.DashboardItemHolder view, boolean isExpanded,
            View.OnClickListener onClickListener, View.OnClickListener onExpandListener) {
        view.itemView.setTag(condition);
        view.itemView.setOnClickListener(onClickListener);
        view.icon.setImageIcon(condition.getIcon());
        view.title.setText(condition.getTitle());
        ImageView expand = (ImageView) view.itemView.findViewById(R.id.expand_indicator);
        expand.setTag(condition);
        expand.setImageResource(isExpanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
        expand.setOnClickListener(onExpandListener);

        View detailGroup = view.itemView.findViewById(R.id.detail_group);
        // TODO: Animate expand/collapse
        detailGroup.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        if (isExpanded) {
            view.summary.setText(condition.getSummary());
            CharSequence[] actions = condition.getActions();
            for (int i = 0; i < 2; i++) {
                Button button = (Button) detailGroup.findViewById(i == 0
                        ? R.id.first_action : R.id.second_action);
                if (actions.length > i) {
                    button.setVisibility(View.VISIBLE);
                    button.setText(actions[i]);
                    final int index = i;
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            condition.onActionClick(index);
                        }
                    });
                } else {
                    button.setVisibility(View.GONE);
                }
            }
        }
    }
}
