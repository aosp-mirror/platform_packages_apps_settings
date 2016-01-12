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

package com.android.settings.localepicker;

import android.content.Context;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.LocaleList;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.android.settings.R;

import com.android.internal.app.LocalePicker;
import com.android.internal.app.LocaleStore;

import java.util.Collections;
import java.util.List;
import java.util.Locale;


class LocaleDragAndDropAdapter
        extends RecyclerView.Adapter<LocaleDragAndDropAdapter.CustomViewHolder> {

    private final Context mContext;
    private final List<LocaleStore.LocaleInfo> mFeedItemList;
    private final ItemTouchHelper mItemTouchHelper;
    private boolean mRemoveMode = false;
    private boolean mDragEnabled = true;

    class CustomViewHolder extends RecyclerView.ViewHolder implements View.OnTouchListener {
        private final LocaleDragCell mLocaleDragCell;

        public CustomViewHolder(LocaleDragCell view) {
            super(view);
            mLocaleDragCell = view;
            mLocaleDragCell.getDragHandle().setOnTouchListener(this);
            mLocaleDragCell.getTextLabel().setOnTouchListener(this);
            mLocaleDragCell.getTranslateableLabel().setOnTouchListener(this);
        }

        public LocaleDragCell getLocaleDragCell() {
            return mLocaleDragCell;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mDragEnabled) {
                switch (MotionEventCompat.getActionMasked(event)) {
                    case MotionEvent.ACTION_DOWN:
                        mItemTouchHelper.startDrag(this);
                }
            }
            return false;
        }
    }

    public LocaleDragAndDropAdapter(Context context, List<LocaleStore.LocaleInfo> feedItemList) {
        this.mFeedItemList = feedItemList;

        this.mContext = context;
        this.mItemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0 /* no swipe */) {

            @Override
            public boolean onMove(RecyclerView view, RecyclerView.ViewHolder source,
                    RecyclerView.ViewHolder target) {
                onItemMove(source.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
                // Swipe is disabled, this is intentionally empty.
            }
        });
    }

    public void setRecyclerView(RecyclerView rv) {
        mItemTouchHelper.attachToRecyclerView(rv);
    }

    @Override
    public CustomViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        final LocaleDragCell item = (LocaleDragCell) LayoutInflater.from(mContext)
                .inflate(R.layout.locale_drag_cell, viewGroup, false);
        return new CustomViewHolder(item);
    }

    @Override
    public void onBindViewHolder(final CustomViewHolder holder, int i) {
        final LocaleStore.LocaleInfo feedItem = mFeedItemList.get(i);
        final LocaleDragCell dragCell = holder.getLocaleDragCell();

        String label = feedItem.getFullNameNative();
        dragCell.setLabel(label);
        dragCell.setLocalized(feedItem.isTranslated());
        dragCell.setMiniLabel(Integer.toString(i + 1));
        dragCell.setShowCheckbox(mRemoveMode);
        dragCell.setShowMiniLabel(!mRemoveMode);
        dragCell.setShowHandle(!mRemoveMode);
        dragCell.setChecked(false);
        dragCell.setTag(feedItem);
        dragCell.getCheckbox()
                .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        LocaleStore.LocaleInfo feedItem =
                                (LocaleStore.LocaleInfo) holder.getLocaleDragCell().getTag();
                        feedItem.setChecked(isChecked);
                    }
                });
    }

    @Override
    public int getItemCount() {
        int itemCount = (null != mFeedItemList ? mFeedItemList.size() : 0);
        if (itemCount < 2 || mRemoveMode) {
            setDragEnabled(false);
        } else {
            setDragEnabled(true);
        }
        return itemCount;
    }

    private void onItemMove(int fromPosition, int toPosition) {
        Collections.swap(mFeedItemList, fromPosition, toPosition);
        notifyItemChanged(fromPosition); // to update the numbers
        notifyItemChanged(toPosition);
        notifyItemMoved(fromPosition, toPosition);
    }

    void setRemoveMode(boolean removeMode) {
        mRemoveMode = removeMode;
        int itemCount = mFeedItemList.size();
        for (int i = 0; i < itemCount; i++) {
            mFeedItemList.get(i).setChecked(false);
            notifyItemChanged(i);
        }
    }

    void removeChecked() {
        int itemCount = mFeedItemList.size();
        for (int i = itemCount - 1; i >= 0; i--) {
            if (mFeedItemList.get(i).getChecked()) {
                mFeedItemList.remove(i);
            }
        }
        notifyDataSetChanged();
        doTheUpdate();
    }

    int getCheckedCount() {
        int result = 0;
        for (LocaleStore.LocaleInfo li : mFeedItemList) {
            if (li.getChecked()) {
                result++;
            }
        }
        return result;
    }

    void addLocale(LocaleStore.LocaleInfo li) {
        mFeedItemList.add(li);
        notifyItemInserted(mFeedItemList.size() - 1);
        doTheUpdate();
    }

    public void doTheUpdate() {
        int count = mFeedItemList.size();
        Locale[] newList = new Locale[count];

        for (int i = 0; i < count; i++) {
            LocaleStore.LocaleInfo li = mFeedItemList.get(i);
            newList[i] = li.getLocale();
        }

        LocaleList ll = new LocaleList(newList);
        LocalePicker.updateLocales(ll);
    }

    private void setDragEnabled(boolean enabled) {
        mDragEnabled = enabled;
    }
}
