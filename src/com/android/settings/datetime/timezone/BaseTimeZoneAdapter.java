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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.recyclerview.widget.RecyclerView;

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
        extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    @VisibleForTesting
    static final int TYPE_HEADER = 0;
    @VisibleForTesting
    static final int TYPE_ITEM = 1;

    private final List<T> mOriginalItems;
    private final OnListItemClickListener<T> mOnListItemClickListener;
    private final Locale mLocale;
    private final boolean mShowItemSummary;
    private final boolean mShowHeader;
    private final CharSequence mHeaderText;

    private List<T> mItems;
    private ArrayFilter mFilter;

    /**
     * @param headerText the text shown in the header, or null to show no header.
     */
    public BaseTimeZoneAdapter(List<T> items, OnListItemClickListener<T> onListItemClickListener,
            Locale locale, boolean showItemSummary, @Nullable CharSequence headerText) {
        mOriginalItems = items;
        mItems = items;
        mOnListItemClickListener = onListItemClickListener;
        mLocale = locale;
        mShowItemSummary = showItemSummary;
        mShowHeader = headerText != null;
        mHeaderText = headerText;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case TYPE_HEADER: {
                final View view = inflater.inflate(
                        R.layout.time_zone_search_header,
                        parent, false);
                return new HeaderViewHolder(view);
            }
            case TYPE_ITEM: {
                final View view = inflater.inflate(R.layout.time_zone_search_item, parent, false);
                return new ItemViewHolder(view, mOnListItemClickListener);
            }
            default:
                throw new IllegalArgumentException("Unexpected viewType: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).setText(mHeaderText);
        } else if (holder instanceof ItemViewHolder) {
            ItemViewHolder<T> itemViewHolder = (ItemViewHolder<T>) holder;
            itemViewHolder.setAdapterItem(getDataItem(position));
            itemViewHolder.mSummaryFrame.setVisibility(mShowItemSummary ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public long getItemId(int position) {
        // Data item can't have negative id
        return isPositionHeader(position) ? -1 : getDataItem(position).getItemId();
    }

    @Override
    public int getItemCount() {
        return mItems.size() + getHeaderCount();
    }

    @Override
    public int getItemViewType(int position) {
        return isPositionHeader(position) ? TYPE_HEADER : TYPE_ITEM;
    }

    /*
     * Avoid being overridden by making the method final, since constructor shouldn't invoke
     * overridable method.
     */
    @Override
    public final void setHasStableIds(boolean hasStableIds) {
        super.setHasStableIds(hasStableIds);
    }

    private int getHeaderCount() {
        return mShowHeader ? 1 : 0;
    }

    private boolean isPositionHeader(int position) {
        return mShowHeader && position == 0;
    }

    @NonNull
    public ArrayFilter getFilter() {
        if (mFilter == null) {
            mFilter = new ArrayFilter();
        }
        return mFilter;
    }

    /**
     * @throws IndexOutOfBoundsException if the view type at the position is a header
     */
    @VisibleForTesting
    public T getDataItem(int position) {
        return mItems.get(position - getHeaderCount());
    }

    public interface AdapterItem {
        CharSequence getTitle();

        CharSequence getSummary();

        String getIconText();

        String getCurrentTime();

        /**
         * @return unique non-negative number
         */
        long getItemId();

        String[] getSearchKeys();
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTextView;

        public HeaderViewHolder(View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(android.R.id.title);
        }

        public void setText(CharSequence text) {
            mTextView.setText(text);
        }
    }

    @VisibleForTesting
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
