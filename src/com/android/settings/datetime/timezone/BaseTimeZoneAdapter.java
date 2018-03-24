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

package com.android.settings.datetime.timezone;

import android.icu.text.BreakIterator;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.datetime.timezone.BaseTimeZonePicker.OnListItemClickListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Used with {@class BaseTimeZonePicker}. It renders text in each item into list view. A list of
 * {@class AdapterItem} must be provided when an instance is created.
 */
public class BaseTimeZoneAdapter<T extends BaseTimeZoneAdapter.AdapterItem>
        extends RecyclerView.Adapter<BaseTimeZoneAdapter.ItemViewHolder> {

    private final List<T> mOriginalItems;
    private final OnListItemClickListener<T> mOnListItemClickListener;
    private final Locale mLocale;
    private final boolean mShowItemSummary;

    private List<T> mItems;
    private ArrayFilter mFilter;

    public BaseTimeZoneAdapter(List<T> items, OnListItemClickListener<T>
            onListItemClickListener, Locale locale, boolean showItemSummary) {
        mOriginalItems = items;
        mItems = items;
        mOnListItemClickListener = onListItemClickListener;
        mLocale = locale;
        mShowItemSummary = showItemSummary;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.time_zone_search_item, parent, false);
        return new ItemViewHolder(view, mOnListItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.setAdapterItem(mItems.get(position));
        holder.mSummaryFrame.setVisibility(mShowItemSummary ? View.VISIBLE : View.GONE);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getItemId();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public @NonNull ArrayFilter getFilter() {
        if (mFilter == null) {
            mFilter = new ArrayFilter();
        }
        return mFilter;
    }

    public T getItem(int position) {
        return mItems.get(position);
    }

    public interface AdapterItem {
        CharSequence getTitle();
        CharSequence getSummary();
        String getIconText();
        String getCurrentTime();
        long getItemId();
        String[] getSearchKeys();
    }

    public static class ItemViewHolder<T extends BaseTimeZoneAdapter.AdapterItem>
            extends RecyclerView.ViewHolder implements View.OnClickListener {

        final OnListItemClickListener<T> mOnListItemClickListener;
        final View mSummaryFrame;
        final TextView mTitleView;
        final TextView mIconTextView;
        final TextView mSummaryView;
        final TextView mTimeView;
        private T mItem;

        public ItemViewHolder(View itemView, OnListItemClickListener<T> onListItemClickListener) {
            super(itemView);
            itemView.setOnClickListener(this);
            mSummaryFrame = itemView.findViewById(R.id.summary_frame);
            mTitleView = itemView.findViewById(android.R.id.title);
            mIconTextView = itemView.findViewById(R.id.icon_text);
            mSummaryView = itemView.findViewById(android.R.id.summary);
            mTimeView = itemView.findViewById(R.id.current_time);
            mOnListItemClickListener = onListItemClickListener;
        }

        public void setAdapterItem(T item) {
            mItem = item;
            mTitleView.setText(item.getTitle());
            mIconTextView.setText(item.getIconText());
            mSummaryView.setText(item.getSummary());
            mTimeView.setText(item.getCurrentTime());
        }

        @Override
        public void onClick(View v) {
            mOnListItemClickListener.onListItemClick(mItem);
        }
    }

    /**
     * <p>An array filter constrains the content of the array adapter with
     * a prefix. Each item that does not start with the supplied prefix
     * is removed from the list.</p>
     *
     * The filtering operation is not optimized, due to small data size (~260 regions),
     * require additional pre-processing. Potentially, a trie structure can be used to match
     * prefixes of the search keys.
     */
    @VisibleForTesting
    public class ArrayFilter extends Filter {

        private BreakIterator mBreakIterator = BreakIterator.getWordInstance(mLocale);

        @WorkerThread
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            final List<T> newItems;
            if (TextUtils.isEmpty(prefix)) {
                newItems = mOriginalItems;
            } else {
                final String prefixString = prefix.toString().toLowerCase(mLocale);
                newItems = new ArrayList<>();

                for (T item : mOriginalItems) {
                    outer:
                    for (String searchKey : item.getSearchKeys()) {
                        searchKey = searchKey.toLowerCase(mLocale);
                        // First match against the whole, non-splitted value
                        if (searchKey.startsWith(prefixString)) {
                            newItems.add(item);
                            break outer;
                        } else {
                            mBreakIterator.setText(searchKey);
                            for (int wordStart = 0, wordLimit = mBreakIterator.next();
                                    wordLimit != BreakIterator.DONE;
                                    wordStart = wordLimit,
                                            wordLimit = mBreakIterator.next()) {
                                if (mBreakIterator.getRuleStatus() != BreakIterator.WORD_NONE
                                        && searchKey.startsWith(prefixString, wordStart)) {
                                    newItems.add(item);
                                    break outer;
                                }
                            }
                        }
                    }
                }
            }

            final FilterResults results = new FilterResults();
            results.values = newItems;
            results.count = newItems.size();

            return results;
        }

        @VisibleForTesting
        @Override
        public void publishResults(CharSequence constraint, FilterResults results) {
            mItems = (List<T>) results.values;
            notifyDataSetChanged();
        }
    }
}
