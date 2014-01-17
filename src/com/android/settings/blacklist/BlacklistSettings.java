/*
 * Copyright (C) 2013 The CyanogenMod Project
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

package com.android.settings.blacklist;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.location.CountryDetector;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.Settings;
import android.provider.Telephony.Blacklist;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ResourceCursorAdapter;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.telephony.util.BlacklistUtils;
import com.android.settings.R;

import java.util.HashMap;

/**
 * Blacklist settings UI for the Phone app.
 */
public class BlacklistSettings extends ListFragment
        implements CompoundButton.OnCheckedChangeListener {

    private static final String[] BLACKLIST_PROJECTION = {
        Blacklist._ID,
        Blacklist.NUMBER,
        Blacklist.PHONE_MODE,
        Blacklist.MESSAGE_MODE
    };
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_NUMBER = 1;
    private static final int COLUMN_PHONE = 2;
    private static final int COLUMN_MESSAGE = 3;

    private Switch mEnabledSwitch;
    private boolean mLastEnabledState;

    private BlacklistAdapter mAdapter;
    private Cursor mCursor;
    private TextView mEmptyView;

    @Override
    public View onCreateView(LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(com.android.internal.R.layout.preference_list_fragment,
                container, false);
    }

    @Override
    public void onActivityCreated(Bundle icicle) {
        super.onActivityCreated(icicle);

        setHasOptionsMenu(true);

        final Activity activity = getActivity();
        mEnabledSwitch = new Switch(activity);

        final int padding = activity.getResources().getDimensionPixelSize(
                R.dimen.action_bar_switch_padding);
        mEnabledSwitch.setPaddingRelative(0, 0, padding, 0);
        mEnabledSwitch.setOnCheckedChangeListener(this);

        mCursor = getActivity().managedQuery(Blacklist.CONTENT_URI,
                BLACKLIST_PROJECTION, null, null, null);
        mAdapter = new BlacklistAdapter(getActivity(), null);

        mEmptyView = (TextView) getView().findViewById(android.R.id.empty);

        final ListView listView = getListView();
        listView.setAdapter(mAdapter);
        listView.setEmptyView(mEmptyView);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.blacklist, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.blacklist_add).setEnabled(mLastEnabledState);
        menu.findItem(R.id.blacklist_prefs).setEnabled(mLastEnabledState);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.blacklist_add:
                showEntryEditDialog(-1);
                return true;
            case R.id.blacklist_prefs:
                PreferenceFragment prefs = new PreferenceFragment();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.replace(com.android.internal.R.id.prefs, prefs);
                ft.addToBackStack(null);
                ft.commitAllowingStateLoss();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        final Activity activity = getActivity();
        activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(mEnabledSwitch, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL | Gravity.END));
    }

    @Override
    public void onStop() {
        super.onStop();
        final Activity activity = getActivity();
        activity.getActionBar().setDisplayOptions(0, ActionBar.DISPLAY_SHOW_CUSTOM);
        activity.getActionBar().setCustomView(null);
    }

    @Override
    public void onResume() {
        super.onResume();

        final Context context = getActivity();
        mLastEnabledState = BlacklistUtils.isBlacklistEnabled(context);
        mEnabledSwitch.setChecked(mLastEnabledState);
        updateEnabledState();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        showEntryEditDialog(id);
    }

    private void showEntryEditDialog(long id) {
        EntryEditDialogFragment fragment = EntryEditDialogFragment.newInstance(id);
        fragment.show(getFragmentManager(), "blacklist_edit");
    }

    private void updateEnabledState() {
        getListView().setEnabled(mLastEnabledState);
        getActivity().invalidateOptionsMenu();

        mEmptyView.setText(mLastEnabledState
                ? R.string.blacklist_empty_text
                : R.string.blacklist_disabled_empty_text);
        mAdapter.swapCursor(mLastEnabledState ? mCursor : null);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == mEnabledSwitch && isChecked != mLastEnabledState) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.PHONE_BLACKLIST_ENABLED, isChecked ? 1 : 0);
            mLastEnabledState = isChecked;
            updateEnabledState();
        }
    }

    private static class BlacklistAdapter extends ResourceCursorAdapter
            implements ToggleImageView.OnCheckedChangeListener {
        private Object mLock = new Object();
        private ContentResolver mResolver;
        private String mCurrentCountryIso;
        private SparseArray<String> mRequestedLookups = new SparseArray<String>();
        private HashMap<String, String> mContactNameCache = new HashMap<String, String>();

        private Handler mMainHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int lookupIndex = msg.arg1;
                String name = (String) msg.obj;
                mContactNameCache.put(mRequestedLookups.get(lookupIndex),
                        name == null ? "" : name);
                mRequestedLookups.delete(lookupIndex);
                notifyDataSetChanged();
            }
        };
        private Handler mQueryHandler;

        private class QueryHandler extends Handler {
            public static final int MSG_LOOKUP = 1;
            private static final int MSG_FINISH = 2;

            public QueryHandler(Looper looper) {
                super(looper);
            }

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_LOOKUP:
                        String name = lookupNameForNumber((String) msg.obj);
                        mMainHandler.obtainMessage(0, msg.arg1, 0, name).sendToTarget();
                        synchronized (mLock) {
                            if (mQueryHandler != null) {
                                Message finishMessage = mQueryHandler.obtainMessage(MSG_FINISH);
                                mQueryHandler.sendMessageDelayed(finishMessage, 3000);
                            }
                        }
                        break;
                    case MSG_FINISH:
                        synchronized (mLock) {
                            if (mQueryHandler != null) {
                                mQueryHandler.getLooper().quit();
                                mQueryHandler = null;
                            }
                        }
                        break;
                }
            }

            private String lookupNameForNumber(String number) {
                if (!TextUtils.isEmpty(mCurrentCountryIso)) {
                    // Normalise the number: this is needed because the PhoneLookup query
                    // below does not accept a country code as an input.
                    String numberE164 = PhoneNumberUtils.formatNumberToE164(number,
                            mCurrentCountryIso);
                    if (!TextUtils.isEmpty(numberE164)) {
                        // Only use it if the number could be formatted to E164.
                        number = numberE164;
                    }
                }

                String result = null;
                final String[] projection = new String[] { PhoneLookup.DISPLAY_NAME };
                Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
                Cursor cursor = mResolver.query(uri, projection, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        result = cursor.getString(0);
                    }
                    cursor.close();
                }

                return result;
            }
        }

        public BlacklistAdapter(Context context, Cursor cursor) {
            super(context, R.layout.blacklist_entry_row, cursor);

            final CountryDetector detector =
                    (CountryDetector) context.getSystemService(Context.COUNTRY_DETECTOR);
            mCurrentCountryIso = detector.detectCountry().getCountryIso();
            mResolver = context.getContentResolver();
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = super.newView(context, cursor, parent);

            ViewHolder holder = new ViewHolder();
            holder.mainText = (TextView) view.findViewById(R.id.number);
            holder.subText = (TextView) view.findViewById(R.id.name);
            holder.callStatus = (ToggleImageView) view.findViewById(R.id.block_calls);
            holder.messageStatus = (ToggleImageView) view.findViewById(R.id.block_messages);

            holder.callStatus.setTag(Blacklist.PHONE_MODE);
            holder.callStatus.setOnCheckedChangeListener(this);

            holder.messageStatus.setTag(Blacklist.MESSAGE_MODE);
            holder.messageStatus.setOnCheckedChangeListener(this);

            view.setTag(holder);

            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            String number = cursor.getString(COLUMN_NUMBER);
            String name = mContactNameCache.get(number);
            String formattedNumber = PhoneNumberUtils.formatNumber(number,
                    null, mCurrentCountryIso);

            if (TextUtils.isEmpty(name)) {
                holder.mainText.setText(formattedNumber);
                holder.subText.setVisibility(View.GONE);
            } else {
                holder.mainText.setText(name);
                holder.subText.setText(formattedNumber);
                holder.subText.setVisibility(View.VISIBLE);
            }

            if (name == null) {
                int id = cursor.getInt(COLUMN_ID);
                scheduleNameLookup(id, number);
            }

            holder.callStatus.setCheckedInternal(cursor.getInt(COLUMN_PHONE) != 0, false);
            holder.messageStatus.setCheckedInternal(cursor.getInt(COLUMN_MESSAGE) != 0, false);
            holder.position = cursor.getPosition();
        }

        @Override
        public void onCheckedChanged(ToggleImageView view, boolean isChecked) {
            View parent = (View) view.getParent();
            ViewHolder holder = (ViewHolder) parent.getTag();
            String column = (String) view.getTag();
            long id = getItemId(holder.position);
            Uri uri = ContentUris.withAppendedId(Blacklist.CONTENT_URI, id);
            ContentValues cv = new ContentValues();

            cv.put(column, view.isChecked() ? 1 : 0);
            if (mResolver.update(uri, cv, null, null) <= 0) {
                // something went wrong, force an update to the correct state
                notifyDataSetChanged();
            }
        }

        private void scheduleNameLookup(int id, String number) {
            synchronized (mLock) {
                if (mQueryHandler == null) {
                    HandlerThread thread = new HandlerThread("blacklist_contact_query",
                            Process.THREAD_PRIORITY_BACKGROUND);
                    thread.start();
                    mQueryHandler = new QueryHandler(thread.getLooper());
                }
            }

            mRequestedLookups.put(id, number);
            Message msg = mQueryHandler.obtainMessage(QueryHandler.MSG_LOOKUP, id, 0, number);
            msg.sendToTarget();
        }

        private static class ViewHolder {
            TextView mainText;
            TextView subText;
            ToggleImageView callStatus;
            ToggleImageView messageStatus;
            int position;
        }
    }
}
