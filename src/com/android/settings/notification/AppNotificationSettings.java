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

package com.android.settings.notification;

import android.animation.LayoutTransition;
import android.app.AlertDialog;
import android.app.INotificationManager;
import android.app.ListFragment;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.android.settings.PinnedHeaderListFragment;
import com.android.settings.R;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Just a sectioned list of installed applications, nothing else to index **/
public class AppNotificationSettings extends PinnedHeaderListFragment {
    private static final String TAG = "AppNotificationSettings";
    private static final boolean DEBUG = true;

    /**
     * Show a checkbox in the per-app notification control dialog to allow the user
     * to promote this app's notifications to higher priority.
     */
    private static final boolean ENABLE_APP_NOTIFICATION_PRIORITY_OPTION = true;
    /**
     * Show a checkbox in the per-app notification control dialog to allow the user to
     * selectively redact this app's notifications on the lockscreen.
     */
    private static final boolean ENABLE_APP_NOTIFICATION_PRIVACY_OPTION = false;

    private static final String SECTION_BEFORE_A = "*";
    private static final String SECTION_AFTER_Z = "**";
    private static final Intent APP_NOTIFICATION_PREFS_CATEGORY_INTENT
            = new Intent(Intent.ACTION_MAIN)
                .addCategory(Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES);

    private final Handler mHandler = new Handler();
    private final ArrayMap<String, AppRow> mRows = new ArrayMap<String, AppRow>();
    private final ArrayList<AppRow> mSortedRows = new ArrayList<AppRow>();
    private final ArrayList<String> mSections = new ArrayList<String>();

    private Context mContext;
    private LayoutInflater mInflater;
    private NotificationAppAdapter mAdapter;
    private Signature[] mSystemSignature;
    private Parcelable mListViewState;
    private Backend mBackend = new Backend();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mAdapter = new NotificationAppAdapter(mContext);
        getActivity().setTitle(R.string.app_notifications_title);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.notification_app_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        repositionScrollbar();
        getListView().setAdapter(mAdapter);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG) Log.d(TAG, "Saving listView state");
        mListViewState = getListView().onSaveInstanceState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mListViewState = null;  // you're dead to me
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAppsList();
    }

    public void setBackend(Backend backend) {
        mBackend = backend;
    }

    private void loadAppsList() {
        AsyncTask.execute(mCollectAppsRunnable);
    }

    private String getSection(CharSequence label) {
        if (label == null || label.length() == 0) return SECTION_BEFORE_A;
        final char c = Character.toUpperCase(label.charAt(0));
        if (c < 'A') return SECTION_BEFORE_A;
        if (c > 'Z') return SECTION_AFTER_Z;
        return Character.toString(c);
    }

    private void repositionScrollbar() {
        final int sbWidthPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                getListView().getScrollBarSize(),
                getResources().getDisplayMetrics());
        final View parent = (View)getView().getParent();
        final int eat = Math.min(sbWidthPx, parent.getPaddingEnd());
        if (eat <= 0) return;
        if (DEBUG) Log.d(TAG, String.format("Eating %dpx into %dpx padding for %dpx scroll, ld=%d",
                eat, parent.getPaddingEnd(), sbWidthPx, getListView().getLayoutDirection()));
        parent.setPaddingRelative(parent.getPaddingStart(), parent.getPaddingTop(),
                parent.getPaddingEnd() - eat, parent.getPaddingBottom());
    }

    private boolean isSystemApp(PackageInfo pkg) {
        if (mSystemSignature == null) {
            mSystemSignature = new Signature[]{ getSystemSignature() };
        }
        return mSystemSignature[0] != null && mSystemSignature[0].equals(getFirstSignature(pkg));
    }

    private static Signature getFirstSignature(PackageInfo pkg) {
        if (pkg != null && pkg.signatures != null && pkg.signatures.length > 0) {
            return pkg.signatures[0];
        }
        return null;
    }

    private Signature getSystemSignature() {
        final PackageManager pm = mContext.getPackageManager();
        try {
            final PackageInfo sys = pm.getPackageInfo("android", PackageManager.GET_SIGNATURES);
            return getFirstSignature(sys);
        } catch (NameNotFoundException e) {
        }
        return null;
    }


    private void showDialog(final View v, final AppRow row) {
        final RelativeLayout layout = (RelativeLayout)
                mInflater.inflate(R.layout.notification_app_dialog, null);
        final ImageView icon = (ImageView) layout.findViewById(android.R.id.icon);
        icon.setImageDrawable(row.icon);
        final TextView title = (TextView) layout.findViewById(android.R.id.title);
        title.setText(row.label);
        final CheckBox showBox = (CheckBox) layout.findViewById(android.R.id.button1);
        final CheckBox priBox = (CheckBox) layout.findViewById(android.R.id.button2);
        final CheckBox senBox = (CheckBox) layout.findViewById(android.R.id.button3);

        if (!ENABLE_APP_NOTIFICATION_PRIORITY_OPTION) {
            priBox.setVisibility(View.GONE);
        }

        if (!ENABLE_APP_NOTIFICATION_PRIVACY_OPTION) {
            senBox.setVisibility(View.GONE);
        }

        showBox.setChecked(!row.banned);
        final OnCheckedChangeListener showListener = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean success = mBackend.setNotificationsBanned(row.pkg, row.uid, !isChecked);
                if (success) {
                    row.banned = !isChecked;
                    mAdapter.bindView(v, row, true /*animate*/);
                    priBox.setEnabled(!row.banned);
                    senBox.setEnabled(!row.banned);
                } else {
                    showBox.setOnCheckedChangeListener(null);
                    showBox.setChecked(!isChecked);
                    showBox.setOnCheckedChangeListener(this);
                }
            }
        };
        showBox.setOnCheckedChangeListener(showListener);

        priBox.setChecked(row.priority);
        final OnCheckedChangeListener priListener = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean success = mBackend.setHighPriority(row.pkg, row.uid, isChecked);
                if (success) {
                    row.priority = isChecked;
                    mAdapter.bindView(v, row, true /*animate*/);
                } else {
                    priBox.setOnCheckedChangeListener(null);
                    priBox.setChecked(!isChecked);
                    priBox.setOnCheckedChangeListener(this);
                }
            }
        };
        priBox.setOnCheckedChangeListener(priListener);

        senBox.setChecked(row.sensitive);
        final OnCheckedChangeListener senListener = new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean success = mBackend.setSensitive(row.pkg, row.uid, isChecked);
                if (success) {
                    row.sensitive = isChecked;
                    mAdapter.bindView(v, row, true /*animate*/);
                } else {
                    senBox.setOnCheckedChangeListener(null);
                    senBox.setChecked(!isChecked);
                    senBox.setOnCheckedChangeListener(this);
                }
            }
        };
        senBox.setOnCheckedChangeListener(senListener);

        priBox.setEnabled(!row.banned);
        senBox.setEnabled(!row.banned);

        final AlertDialog d = new AlertDialog.Builder(mContext)
            .setView(layout)
            .setPositiveButton(R.string.app_notifications_dialog_done, null)
            .create();
        d.show();
    }

    private static class ViewHolder {
        ViewGroup row;
        ViewGroup appButton;
        ImageView icon;
        TextView title;
        TextView subtitle;
        View settingsDivider;
        ImageView settingsButton;
        View rowDivider;
    }

    private class NotificationAppAdapter extends ArrayAdapter<Row> implements SectionIndexer {
        public NotificationAppAdapter(Context context) {
            super(context, 0, 0);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            Row r = getItem(position);
            return r instanceof AppRow ? 1 : 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            Row r = getItem(position);
            View v;
            if (convertView == null) {
                v = newView(parent, r);
            } else {
                v = convertView;
            }
            bindView(v, r, false /*animate*/);
            return v;
        }

        public View newView(ViewGroup parent, Row r) {
            if (!(r instanceof AppRow)) {
                return mInflater.inflate(R.layout.notification_app_section, parent, false);
            }
            final View v = mInflater.inflate(R.layout.notification_app, parent, false);
            final ViewHolder vh = new ViewHolder();
            vh.row = (ViewGroup) v;
            vh.row.setLayoutTransition(new LayoutTransition());
            vh.appButton = (ViewGroup) v.findViewById(android.R.id.button1);
            vh.appButton.setLayoutTransition(new LayoutTransition());
            vh.icon = (ImageView) v.findViewById(android.R.id.icon);
            vh.title = (TextView) v.findViewById(android.R.id.title);
            vh.subtitle = (TextView) v.findViewById(android.R.id.text1);
            vh.settingsDivider = v.findViewById(R.id.settings_divider);
            vh.settingsButton = (ImageView) v.findViewById(android.R.id.button2);
            vh.rowDivider = v.findViewById(R.id.row_divider);
            v.setTag(vh);
            return v;
        }

        private void enableLayoutTransitions(ViewGroup vg, boolean enabled) {
            if (enabled) {
                vg.getLayoutTransition().enableTransitionType(LayoutTransition.APPEARING);
                vg.getLayoutTransition().enableTransitionType(LayoutTransition.DISAPPEARING);
            } else {
                vg.getLayoutTransition().disableTransitionType(LayoutTransition.APPEARING);
                vg.getLayoutTransition().disableTransitionType(LayoutTransition.DISAPPEARING);
            }
        }

        public void bindView(final View view, Row r, boolean animate) {
            if (!(r instanceof AppRow)) {
                // it's a section row
                final TextView tv = (TextView)view.findViewById(android.R.id.title);
                tv.setText(r.section);
                return;
            }

            final AppRow row = (AppRow)r;
            final ViewHolder vh = (ViewHolder) view.getTag();
            enableLayoutTransitions(vh.row, animate);
            vh.rowDivider.setVisibility(row.first ? View.GONE : View.VISIBLE);
            vh.appButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDialog(view, row);
                }
            });
            enableLayoutTransitions(vh.appButton, animate);
            vh.icon.setImageDrawable(row.icon);
            vh.title.setText(row.label);
            final String sub = getSubtitle(row);
            vh.subtitle.setText(sub);
            vh.subtitle.setVisibility(!sub.isEmpty() ? View.VISIBLE : View.GONE);
            final boolean showSettings = !row.banned && row.settingsIntent != null;
            vh.settingsDivider.setVisibility(showSettings ? View.VISIBLE : View.GONE);
            vh.settingsButton.setVisibility(showSettings ? View.VISIBLE : View.GONE);
            vh.settingsButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (row.settingsIntent != null) {
                        getContext().startActivity(row.settingsIntent);
                    }
                }
            });
        }

        private String getSubtitle(AppRow row) {
            if (row.banned) return mContext.getString(R.string.app_notification_row_banned);
            if (!row.priority && !row.sensitive) return "";
            final String priString = mContext.getString(R.string.app_notification_row_priority);
            final String senString = mContext.getString(R.string.app_notification_row_sensitive);
            if (row.priority != row.sensitive) {
                return row.priority ? priString : senString;
            }
            return priString + mContext.getString(R.string.summary_divider_text) + senString;
        }

        @Override
        public Object[] getSections() {
            return mSections.toArray(new Object[mSections.size()]);
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            final String section = mSections.get(sectionIndex);
            final int n = getCount();
            for (int i = 0; i < n; i++) {
                final Row r = getItem(i);
                if (r.section.equals(section)) {
                    return i;
                }
            }
            return 0;
        }

        @Override
        public int getSectionForPosition(int position) {
            Row row = getItem(position);
            return mSections.indexOf(row.section);
        }
    }

    private static class Row {
        public String section;
    }

    private static class AppRow extends Row {
        public String pkg;
        public int uid;
        public Drawable icon;
        public CharSequence label;
        public Intent settingsIntent;
        public boolean banned;
        public boolean priority;
        public boolean sensitive;
        public boolean first;  // first app in section
    }

    private static final Comparator<AppRow> mRowComparator = new Comparator<AppRow>() {
        private final Collator sCollator = Collator.getInstance();
        @Override
        public int compare(AppRow lhs, AppRow rhs) {
            return sCollator.compare(lhs.label, rhs.label);
        }
    };

    private final Runnable mCollectAppsRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (mRows) {
                final long start = SystemClock.uptimeMillis();
                if (DEBUG) Log.d(TAG, "Collecting apps...");
                mRows.clear();
                mSortedRows.clear();

                // collect all non-system apps
                final PackageManager pm = mContext.getPackageManager();
                for (PackageInfo pkg : pm.getInstalledPackages(PackageManager.GET_SIGNATURES)) {
                    if (pkg.applicationInfo == null || isSystemApp(pkg)) {
                        if (DEBUG) Log.d(TAG, "Skipping " + pkg.packageName);
                        continue;
                    }
                    final AppRow row = new AppRow();
                    row.pkg = pkg.packageName;
                    row.uid = pkg.applicationInfo.uid;
                    try {
                        row.label = pkg.applicationInfo.loadLabel(pm);
                    } catch (Throwable t) {
                        Log.e(TAG, "Error loading application label for " + row.pkg, t);
                        row.label = row.pkg;
                    }
                    row.icon = pkg.applicationInfo.loadIcon(pm);
                    row.banned = mBackend.getNotificationsBanned(row.pkg, row.uid);
                    row.priority = mBackend.getHighPriority(row.pkg, row.uid);
                    row.sensitive = mBackend.getSensitive(row.pkg, row.uid);
                    mRows.put(row.pkg, row);
                }
                // collect config activities
                Log.d(TAG, "APP_NOTIFICATION_PREFS_CATEGORY_INTENT is " + APP_NOTIFICATION_PREFS_CATEGORY_INTENT);
                final List<ResolveInfo> resolveInfos = pm.queryIntentActivities(
                        APP_NOTIFICATION_PREFS_CATEGORY_INTENT,
                        PackageManager.MATCH_DEFAULT_ONLY);
                if (DEBUG) Log.d(TAG, "Found " + resolveInfos.size() + " preference activities");
                for (ResolveInfo ri : resolveInfos) {
                    final ActivityInfo activityInfo = ri.activityInfo;
                    final ApplicationInfo appInfo = activityInfo.applicationInfo;
                    final AppRow row = mRows.get(appInfo.packageName);
                    if (row == null) {
                        Log.v(TAG, "Ignoring notification preference activity ("
                                + activityInfo.name + ") for unknown package "
                                + activityInfo.packageName);
                        continue;
                    }
                    if (row.settingsIntent != null) {
                        Log.v(TAG, "Ignoring duplicate notification preference activity ("
                                + activityInfo.name + ") for package "
                                + activityInfo.packageName);
                        continue;
                    }
                    row.settingsIntent = new Intent(Intent.ACTION_MAIN)
                            .setClassName(activityInfo.packageName, activityInfo.name);
                }
                // sort rows
                mSortedRows.addAll(mRows.values());
                Collections.sort(mSortedRows, mRowComparator);
                // compute sections
                mSections.clear();
                String section = null;
                for (AppRow r : mSortedRows) {
                    r.section = getSection(r.label);
                    if (!r.section.equals(section)) {
                        section = r.section;
                        mSections.add(section);
                    }
                }
                mHandler.post(mRefreshAppsListRunnable);
                final long elapsed = SystemClock.uptimeMillis() - start;
                if (DEBUG) Log.d(TAG, "Collected " + mRows.size() + " apps in " + elapsed + "ms");
            }
        }
    };

    private void refreshDisplayedItems() {
        if (DEBUG) Log.d(TAG, "Refreshing apps...");
        mAdapter.clear();
        synchronized (mSortedRows) {
            String section = null;
            final int N = mSortedRows.size();
            boolean first = true;
            for (int i = 0; i < N; i++) {
                final AppRow row = mSortedRows.get(i);
                if (!row.section.equals(section)) {
                    section = row.section;
                    Row r = new Row();
                    r.section = section;
                    mAdapter.add(r);
                    first = true;
                }
                row.first = first;
                mAdapter.add(row);
                first = false;
            }
        }
        if (mListViewState != null) {
            if (DEBUG) Log.d(TAG, "Restoring listView state");
            getListView().onRestoreInstanceState(mListViewState);
            mListViewState = null;
        }
        if (DEBUG) Log.d(TAG, "Refreshed " + mSortedRows.size() + " displayed items");
    }

    private final Runnable mRefreshAppsListRunnable = new Runnable() {
        @Override
        public void run() {
            refreshDisplayedItems();
        }
    };

    public static class Backend {
        public boolean setNotificationsBanned(String pkg, int uid, boolean banned) {
            INotificationManager nm = INotificationManager.Stub.asInterface(
                    ServiceManager.getService(Context.NOTIFICATION_SERVICE));
            try {
                nm.setNotificationsEnabledForPackage(pkg, uid, !banned);
                return true;
            } catch (Exception e) {
               Log.w(TAG, "Error calling NoMan", e);
               return false;
            }
        }

        public boolean getNotificationsBanned(String pkg, int uid) {
            INotificationManager nm = INotificationManager.Stub.asInterface(
                    ServiceManager.getService(Context.NOTIFICATION_SERVICE));
            try {
                final boolean enabled = nm.areNotificationsEnabledForPackage(pkg, uid);
                return !enabled;
            } catch (Exception e) {
                Log.w(TAG, "Error calling NoMan", e);
                return false;
            }
        }

        public boolean getHighPriority(String pkg, int uid) {
            INotificationManager nm = INotificationManager.Stub.asInterface(
                    ServiceManager.getService(Context.NOTIFICATION_SERVICE));
            try {
                return nm.getPackagePriority(pkg, uid) == Notification.PRIORITY_MAX;
            } catch (Exception e) {
                Log.w(TAG, "Error calling NoMan", e);
                return false;
            }
        }

        public boolean setHighPriority(String pkg, int uid, boolean highPriority) {
            INotificationManager nm = INotificationManager.Stub.asInterface(
                    ServiceManager.getService(Context.NOTIFICATION_SERVICE));
            try {
                nm.setPackagePriority(pkg, uid,
                        highPriority ? Notification.PRIORITY_MAX : Notification.PRIORITY_DEFAULT);
                return true;
            } catch (Exception e) {
                Log.w(TAG, "Error calling NoMan", e);
                return false;
            }
        }

        public boolean getSensitive(String pkg, int uid) {
            // TODO get visibility state from NoMan
            return false;
        }

        public boolean setSensitive(String pkg, int uid, boolean sensitive) {
            // TODO save visibility state to NoMan
            return true;
        }
    }
}
