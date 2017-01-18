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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
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
        View card = view.itemView.findViewById(R.id.content);
        card.setTag(condition);
        card.setOnClickListener(onClickListener);
        view.icon.setImageIcon(condition.getIcon());
        view.title.setText(condition.getTitle());
        final View collapsedGroup = view.itemView.findViewById(R.id.collapsed_group);
        collapsedGroup.setTag(condition);
        final ImageView expand = (ImageView) view.itemView.findViewById(R.id.expand_indicator);
        expand.setImageResource(isExpanded ? R.drawable.ic_expand_less : R.drawable.ic_expand_more);
        expand.setContentDescription(expand.getContext().getString(isExpanded
                ? R.string.condition_expand_hide : R.string.condition_expand_show));
        collapsedGroup.setOnClickListener(onExpandListener);

        View detailGroup = view.itemView.findViewById(R.id.detail_group);
        CharSequence[] actions = condition.getActions();
        if (isExpanded != (detailGroup.getVisibility() == View.VISIBLE)) {
            animateChange(view.itemView, view.itemView.findViewById(R.id.content),
                    detailGroup, isExpanded, actions.length > 0);
        }
        if (isExpanded) {
            view.summary.setText(condition.getSummary());
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
                            MetricsLogger.action(v.getContext(),
                                    MetricsEvent.ACTION_SETTINGS_CONDITION_BUTTON,
                                    condition.getMetricsConstant());
                            condition.onActionClick(index);
                        }
                    });
                } else {
                    button.setVisibility(View.GONE);
                }
            }
        }
    }

    private static void animateChange(final View view, final View content,
            final View detailGroup, final boolean visible, final boolean hasButtons) {
        setViewVisibility(detailGroup, R.id.divider, hasButtons);
        setViewVisibility(detailGroup, R.id.buttonBar, hasButtons);
        final int beforeBottom = content.getBottom();
        setHeight(detailGroup, visible ? LayoutParams.WRAP_CONTENT : 0);
        detailGroup.setVisibility(View.VISIBLE);
        view.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            public static final long DURATION = 250;

            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {
                final int afterBottom = content.getBottom();
                v.removeOnLayoutChangeListener(this);
                final ObjectAnimator animator = ObjectAnimator.ofInt(content, "bottom",
                        beforeBottom, afterBottom);
                animator.setDuration(DURATION);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (!visible) {
                            detailGroup.setVisibility(View.GONE);
                        }
                    }
                });
                animator.start();
            }
        });
    }

    private static void setHeight(View detailGroup, int height) {
        final LayoutParams params = detailGroup.getLayoutParams();
        params.height = height;
        detailGroup.setLayoutParams(params);
    }

    private static void setViewVisibility(View containerView, int viewId, boolean visible) {
        View view = containerView.findViewById(viewId);
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }
}
