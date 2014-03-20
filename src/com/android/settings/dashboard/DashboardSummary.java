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

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.search.Index;

import java.util.HashMap;

public class DashboardSummary extends Fragment {

    private static final String LOG_TAG = "DashboardSummary";

    private EditText mEditText;
    private ListView mListView;

    private SearchResultsAdapter mAdapter;
    private Index mIndex;
    private UpdateSearchResultsTask mUpdateSearchResultsTask;

    /**
     * A basic AsyncTask for updating the query results cursor
     */
    private class UpdateSearchResultsTask extends AsyncTask<String, Void, Cursor> {
        @Override
        protected Cursor doInBackground(String... params) {
            return mIndex.search(params[0]);
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            if (!isCancelled()) {
                setCursor(cursor);
            } else if (cursor != null) {
                cursor.close();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIndex = Index.getInstance(getActivity());
        mAdapter = new SearchResultsAdapter(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();

        clearResults();
    }

    @Override
    public void onStart() {
        super.onStart();

        updateSearchResults();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final View view = inflater.inflate(R.layout.dashboard, container, false);

        mEditText = (EditText)view.findViewById(R.id.edittext_query);
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSearchResults();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    closeSoftKeyboard();
                }
            }
        });

        mListView = (ListView) view.findViewById(R.id.list_results);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                closeSoftKeyboard();

                final Cursor cursor = mAdapter.mCursor;
                cursor.moveToPosition(position);

                final String className = cursor.getString(Index.COLUMN_INDEX_CLASS_NAME);
                final String screenTitle = cursor.getString(Index.COLUMN_INDEX_SCREEN_TITLE);

                final String action = cursor.getString(Index.COLUMN_INDEX_INTENT_ACTION);

                if (TextUtils.isEmpty(action)) {
                    ((SettingsActivity) getActivity()).startPreferencePanel(className, null, 0,
                            screenTitle, null, 0);
                } else {
                    final Intent intent = new Intent(action);

                    final String targetPackage = cursor.getString(
                            Index.COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE);
                    final String targetClass = cursor.getString(
                            Index.COLUMN_INDEX_INTENT_ACTION_TARGET_CLASS);
                    if (!TextUtils.isEmpty(targetPackage) && !TextUtils.isEmpty(targetClass)) {
                        final ComponentName component =
                                new ComponentName(targetPackage, targetClass);
                        intent.setComponent(component);
                    }

                    getActivity().startActivity(intent);
                }
            }
        });

        return view;
    }

    private void closeSoftKeyboard() {
        InputMethodManager imm = InputMethodManager.peekInstance();
        if (imm != null && imm.isActive(mEditText)) {
            imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
        }
    }

    private void clearResults() {
        if (mUpdateSearchResultsTask != null) {
            mUpdateSearchResultsTask.cancel(false);
            mUpdateSearchResultsTask = null;
        }
        setCursor(null);
    }

    private void setCursor(Cursor cursor) {
        Cursor oldCursor = mAdapter.swapCursor(cursor);
        if (oldCursor != null) {
            oldCursor.close();
        }
    }

    private String getFilteredQueryString() {
        final CharSequence query = mEditText.getText().toString();
        final StringBuilder filtered = new StringBuilder();
        for (int n = 0; n < query.length(); n++) {
            char c = query.charAt(n);
            if (!Character.isLetterOrDigit(c) && !Character.isSpaceChar(c)) {
                continue;
            }
            filtered.append(c);
        }
        return filtered.toString();
    }

    private void updateSearchResults() {
        if (mUpdateSearchResultsTask != null) {
            mUpdateSearchResultsTask.cancel(false);
            mUpdateSearchResultsTask = null;
        }
        final String query = getFilteredQueryString();
        if (TextUtils.isEmpty(query)) {
            setCursor(null);
        } else {
            mUpdateSearchResultsTask = new UpdateSearchResultsTask();
            mUpdateSearchResultsTask.execute(query);
        }
    }

    private static class SearchResult {
        public String title;
        public String summary;
        public int iconResId;
        public Context context;

        public SearchResult(Context context, String title, String summary, int iconResId) {
            this.context = context;
            this.title = title;
            this.summary = summary;
            this.iconResId = iconResId;
        }
    }

    private static class SearchResultsAdapter extends BaseAdapter {

        private Cursor mCursor;
        private LayoutInflater mInflater;
        private boolean mDataValid;
        private Context mContext;
        private HashMap<String, Context> mContextMap = new HashMap<String, Context>();

        public SearchResultsAdapter(Context context) {
            mContext = context;
            mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mDataValid = false;
        }

        public Cursor swapCursor(Cursor newCursor) {
            if (newCursor == mCursor) {
                return null;
            }
            Cursor oldCursor = mCursor;
            mCursor = newCursor;
            if (newCursor != null) {
                mDataValid = true;
                notifyDataSetChanged();
            } else {
                mDataValid = false;
                notifyDataSetInvalidated();
            }
            return oldCursor;
        }

        @Override
        public int getCount() {
            if (!mDataValid || mCursor == null || mCursor.isClosed()) return 0;
            return mCursor.getCount();
        }

        @Override
        public Object getItem(int position) {
            if (mDataValid && mCursor.moveToPosition(position)) {
                final String title = mCursor.getString(Index.COLUMN_INDEX_TITLE);
                final String summary = mCursor.getString(Index.COLUMN_INDEX_SUMMARY);
                final String iconResStr = mCursor.getString(Index.COLUMN_INDEX_ICON);
                final String className = mCursor.getString(
                        Index.COLUMN_INDEX_CLASS_NAME);
                final String packageName = mCursor.getString(
                        Index.COLUMN_INDEX_INTENT_ACTION_TARGET_PACKAGE);

                Context packageContext;
                if (TextUtils.isEmpty(className) && !TextUtils.isEmpty(packageName)) {
                    packageContext = mContextMap.get(packageName);
                    if (packageContext == null) {
                        try {
                            packageContext = mContext.createPackageContext(packageName, 0);
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.e(LOG_TAG, "Cannot create Context for package: " + packageName);
                            return null;
                        }
                        mContextMap.put(packageName, packageContext);
                    }
                } else {
                    packageContext = mContext;
                }
                final int iconResId = TextUtils.isEmpty(iconResStr) ?
                        R.drawable.empty_icon : Integer.parseInt(iconResStr);
                return new SearchResult(packageContext, title, summary, iconResId);
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (!mDataValid && convertView == null) {
                throw new IllegalStateException(
                        "this should only be called when the cursor is valid");
            }
            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }

            View view;
            TextView textTitle;
            TextView textSummary;
            ImageView imageView;

            if (convertView == null) {
                view = mInflater.inflate(R.layout.search_result, parent, false);
            } else {
                view = convertView;
            }
            textTitle = (TextView) view.findViewById(R.id.title);
            textSummary = (TextView) view.findViewById(R.id.summary);
            imageView = (ImageView) view.findViewById(R.id.icon);

            SearchResult result = (SearchResult) getItem(position);

            textTitle.setText(result.title);
            textSummary.setText(result.summary);
            if (result.iconResId != R.drawable.empty_icon) {
                final Context packageContext = result.context;
                final Drawable drawable;
                try {
                    drawable = packageContext.getDrawable(result.iconResId);
                    imageView.setImageDrawable(drawable);
                } catch (Resources.NotFoundException nfe) {
                    // Not much we can do except logging
                    Log.e(LOG_TAG, "Cannot load Drawable for " + result.title);
                }
                imageView.setBackgroundResource(R.color.background_search_result_icon);
            } else {
                imageView.setImageDrawable(null);
                imageView.setBackgroundResource(R.drawable.empty_icon);
            }

            return view;
        }
    }
}
