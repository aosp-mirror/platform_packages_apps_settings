/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.INotificationListener;
import android.app.INotificationManager;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DateTimeView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.android.internal.statusbar.StatusBarNotification;

import java.util.ArrayList;
import java.util.List;

public class NotificationStation extends SettingsPreferenceFragment {
    private static final String TAG = NotificationStation.class.getSimpleName();
    static final boolean DEBUG = true;
    private static final String PACKAGE_SCHEME = "package";

    private final PackageReceiver mPackageReceiver = new PackageReceiver();
    private INotificationManager mNoMan;
    private INotificationListener.Stub mListener = new INotificationListener.Stub() {
        @Override
        public void onNotificationPosted(StatusBarNotification notification) throws RemoteException {
            Log.v(TAG, "onNotificationPosted: " + notification);
            getListView().post(new Runnable() { public void run() { refreshList(); }});
        }

        @Override
        public void onNotificationRemoved(StatusBarNotification notification) throws RemoteException {
            // no-op; we're just showing new notifications
        }
    };

    private NotificationHistoryAdapter mAdapter;
    private Context mContext;

    @Override
    public void onAttach(Activity activity) {
        logd("onAttach(%s)", activity.getClass().getSimpleName());
        super.onAttach(activity);
        mContext = activity;
        mNoMan = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        try {
            mNoMan.registerListener(mListener, UserHandle.USER_ALL);
        } catch (RemoteException e) {
            // well, that didn't work out
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        logd("onCreate(%s)", icicle);
        super.onCreate(icicle);
        Activity activity = getActivity();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        logd("onActivityCreated(%s)", savedInstanceState);
        super.onActivityCreated(savedInstanceState);

        ListView listView = getListView();

//        TextView emptyView = (TextView) getView().findViewById(android.R.id.empty);
//        emptyView.setText(R.string.screensaver_settings_disabled_prompt);
//        listView.setEmptyView(emptyView);

        mAdapter = new NotificationHistoryAdapter(mContext);
        listView.setAdapter(mAdapter);
    }

    @Override
    public void onPause() {
        logd("onPause()");
        super.onPause();
        mContext.unregisterReceiver(mPackageReceiver);
    }

    @Override
    public void onResume() {
        logd("onResume()");
        super.onResume();
        refreshList();

        // listen for package changes
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme(PACKAGE_SCHEME);
        mContext.registerReceiver(mPackageReceiver , filter);
    }

    private void refreshList() {
        List<HistoricalNotificationInfo> infos = loadNotifications();
        if (infos != null) {
            logd("adding %d infos", infos.size());
            mAdapter.clear();
            mAdapter.addAll(infos);
        }
    }

    private static void logd(String msg, Object... args) {
        if (DEBUG)
            Log.d(TAG, args == null || args.length == 0 ? msg : String.format(msg, args));
    }

    private static class HistoricalNotificationInfo {
        public String pkg;
        public Drawable pkgicon;
        public Drawable icon;
        public CharSequence title;
        public int priority;
        public int user;
        public long timestamp;
    }

    private List<HistoricalNotificationInfo> loadNotifications() {
        final int currentUserId = ActivityManager.getCurrentUser();
        try {
            StatusBarNotification[] nions = mNoMan.getHistoricalNotifications(
                    mContext.getPackageName(), 50);
            List<HistoricalNotificationInfo> list
                    = new ArrayList<HistoricalNotificationInfo>(nions.length);

            for (StatusBarNotification sbn : nions) {
                final HistoricalNotificationInfo info = new HistoricalNotificationInfo();
                info.pkg = sbn.pkg;
                info.user = sbn.getUserId();
                info.icon = loadIconDrawable(info.pkg, info.user, sbn.notification.icon);
                info.pkgicon = loadPackageIconDrawable(info.pkg, info.user);
                if (sbn.notification.extras != null) {
                    info.title = sbn.notification.extras.getString(Notification.EXTRA_TITLE);
                }
                info.timestamp = sbn.postTime;
                info.priority = sbn.notification.priority;
                logd("   [%d] %s: %s", info.timestamp, info.pkg, info.title);

                if (info.user == UserHandle.USER_ALL
                        || info.user == currentUserId) {
                    list.add(info);
                }
            }

            return list;
        } catch (RemoteException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

    private Resources getResourcesForUserPackage(String pkg, int userId) {
        Resources r = null;

        if (pkg != null) {
            try {
                if (userId == UserHandle.USER_ALL) {
                    userId = UserHandle.USER_OWNER;
                }
                r = mContext.getPackageManager()
                        .getResourcesForApplicationAsUser(pkg, userId);
            } catch (PackageManager.NameNotFoundException ex) {
                Log.e(TAG, "Icon package not found: " + pkg);
                return null;
            }
        } else {
            r = mContext.getResources();
        }
        return r;
    }

    private Drawable loadPackageIconDrawable(String pkg, int userId) {
        Drawable icon = null;
        try {
            icon = mContext.getPackageManager().getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
        }

        return icon;
    }

    private Drawable loadIconDrawable(String pkg, int userId, int resId) {
        Resources r = getResourcesForUserPackage(pkg, userId);

        if (resId == 0) {
            return null;
        }

        try {
            return r.getDrawable(resId);
        } catch (RuntimeException e) {
            Log.w(TAG, "Icon not found in "
                    + (pkg != null ? resId : "<system>")
                    + ": " + Integer.toHexString(resId));
        }

        return null;
    }

    private class NotificationHistoryAdapter extends ArrayAdapter<HistoricalNotificationInfo> {
        private final LayoutInflater mInflater;

        public NotificationHistoryAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HistoricalNotificationInfo info = getItem(position);
            logd("getView(%s/%s)", info.pkg, info.title);
            final View row = convertView != null ? convertView : createRow(parent, info.pkg);
            row.setTag(info);

            // bind icon
            if (info.icon != null) {
                ((ImageView) row.findViewById(android.R.id.icon)).setImageDrawable(info.icon);
            }
            if (info.pkgicon != null) {
                ((ImageView) row.findViewById(R.id.pkgicon)).setImageDrawable(info.pkgicon);
            }

            ((DateTimeView) row.findViewById(R.id.timestamp)).setTime(info.timestamp);

            // bind caption
            ((TextView) row.findViewById(android.R.id.title)).setText(info.title);

//            // bind radio button
//            RadioButton radioButton = (RadioButton) row.findViewById(android.R.id.button1);
//            radioButton.setChecked(dreamInfo.isActive);
//            radioButton.setOnTouchListener(new OnTouchListener() {
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    row.onTouchEvent(event);
//                    return false;
//                }});

            // bind settings button + divider
//            boolean showSettings = info.
//                    settingsComponentName != null;
//            View settingsDivider = row.findViewById(R.id.divider);
//            settingsDivider.setVisibility(false ? View.VISIBLE : View.INVISIBLE);
//
//            ImageView settingsButton = (ImageView) row.findViewById(android.R.id.button2);
//            settingsButton.setVisibility(false ? View.VISIBLE : View.INVISIBLE);
//            settingsButton.setAlpha(info.isActive ? 1f : Utils.DISABLED_ALPHA);
//            settingsButton.setEnabled(info.isActive);
//            settingsButton.setOnClickListener(new OnClickListener(){
//                @Override
//                public void onClick(View v) {
//                    mBackend.launchSettings((DreamInfo) row.getTag());
//                }});

            return row;
        }

        private View createRow(ViewGroup parent, final String pkg) {
            final View row =  mInflater.inflate(R.layout.notification_log_row, parent, false);
            row.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    v.setPressed(true);
                    startApplicationDetailsActivity(pkg);
                }});
            return row;
        }

    }

    private void startApplicationDetailsActivity(String packageName) {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null));
        intent.setComponent(intent.resolveActivity(mContext.getPackageManager()));
        startActivity(intent);
    }

    private class PackageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            logd("PackageReceiver.onReceive");
            //refreshList();
        }
    }
}
