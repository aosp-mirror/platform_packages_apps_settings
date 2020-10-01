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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.datetime.timezone.model.TimeZoneData;
import com.android.settings.datetime.timezone.model.TimeZoneDataLoader;

import java.util.Locale;

/**
 * It's abstract class. Subclass should use it with {@class BaseTimeZoneAdapter} and
 * {@class AdapterItem} to provide a list view with text search capability.
 * The search matches the prefix of words in the search text.
 */
public abstract class BaseTimeZonePicker extends InstrumentedFragment
        implements SearchView.OnQueryTextListener {

    public static final String EXTRA_RESULT_REGION_ID =
            "com.android.settings.datetime.timezone.result_region_id";
    public static final String EXTRA_RESULT_TIME_ZONE_ID =
            "com.android.settings.datetime.timezone.result_time_zone_id";
    private final int mTitleResId;
    private final int mSearchHintResId;
    private final boolean mSearchEnabled;
    private final boolean mDefaultExpandSearch;

    protected Locale mLocale;
    private BaseTimeZoneAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private TimeZoneData mTimeZoneData;

    private SearchView mSearchView;

    /**
     * Constructor called by subclass.
     * @param defaultExpandSearch whether expand the search view when first launching the fragment
     */
    protected BaseTimeZonePicker(int titleResId, int searchHintResId,
            boolean searchEnabled, boolean defaultExpandSearch) {
        mTitleResId = titleResId;
        mSearchHintResId = searchHintResId;
        mSearchEnabled = searchEnabled;
        mDefaultExpandSearch = defaultExpandSearch;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        getActivity().setTitle(mTitleResId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.recycler_view, container, false);
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, /* reverseLayout */ false));
        mRecyclerView.setAdapter(mAdapter);

        // Initialize TimeZoneDataLoader only when mRecyclerView is ready to avoid race
        // during onDateLoaderReady callback.
        getLoaderManager().initLoader(0, null, new TimeZoneDataLoader.LoaderCreator(
                getContext(), this::onTimeZoneDataReady));
        return view;
    }

    public void onTimeZoneDataReady(TimeZoneData timeZoneData) {
        if (mTimeZoneData == null && timeZoneData != null) {
            mTimeZoneData = timeZoneData;
            mAdapter = createAdapter(mTimeZoneData);
            if (mRecyclerView != null) {
                mRecyclerView.setAdapter(mAdapter);
            }
        }
    }

    protected Locale getLocale() {
        return getContext().getResources().getConfiguration().getLocales().get(0);
    }

    /**
     * Called when TimeZoneData is ready.
     */
    protected abstract BaseTimeZoneAdapter createAdapter(TimeZoneData timeZoneData);

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (mSearchEnabled) {
            inflater.inflate(R.menu.time_zone_base_search_menu, menu);

            final MenuItem searchMenuItem = menu.findItem(R.id.time_zone_search_menu);
            mSearchView = (SearchView) searchMenuItem.getActionView();

            mSearchView.setQueryHint(getText(mSearchHintResId));
            mSearchView.setOnQueryTextListener(this);

            if (mDefaultExpandSearch) {
                searchMenuItem.expandActionView();
                mSearchView.setIconified(false);
                mSearchView.setActivated(true);
                mSearchView.setQuery("", true /* submit */);
            }

            // Set zero margin and padding to align with the text horizontally in the preference
            final TextView searchViewView = (TextView) mSearchView.findViewById(
                    com.android.internal.R.id.search_src_text);
            searchViewView.setPadding(0, searchViewView.getPaddingTop(), 0,
                    searchViewView.getPaddingBottom());
            final View editFrame = mSearchView.findViewById(
                    com.android.internal.R.id.search_edit_frame);
            final LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) editFrame
                    .getLayoutParams();
            params.setMarginStart(0);
            params.setMarginEnd(0);
            editFrame.setLayoutParams(params);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (mAdapter != null) {
            mAdapter.getFilter().filter(newText);
        }
        return false;
    }

    public interface OnListItemClickListener<T extends BaseTimeZoneAdapter.AdapterItem> {
        void onListItemClick(T item);
    }

}
