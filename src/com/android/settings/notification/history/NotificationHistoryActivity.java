/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.notification.history;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.INotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;

import java.util.Arrays;

public class NotificationHistoryActivity extends Activity {

    private static String TAG = "NotifHistory";

    private ViewGroup mTodayView;
    private ViewGroup mSnoozeView;
    private ViewGroup mDismissView;
    private HistoryLoader mHistoryLoader;
    private INotificationManager mNm;
    private PackageManager mPm;

    private HistoryLoader.OnHistoryLoaderListener mOnHistoryLoaderListener = notifications -> {
        // for each package, new header and recycler view
        for (NotificationHistoryPackage nhp : notifications) {
            View viewForPackage = LayoutInflater.from(this)
                    .inflate(R.layout.notification_history_app_layout, null);

            final View container = viewForPackage.findViewById(R.id.list_container);
            container.setVisibility(View.GONE);
            ImageButton expand = viewForPackage.findViewById(R.id.expand);
            expand.setOnClickListener(v -> {
                    container.setVisibility(container.getVisibility() == View.VISIBLE
                            ? View.GONE : View.VISIBLE);
                    expand.setImageResource(container.getVisibility() == View.VISIBLE
                            ? R.drawable.ic_expand_less
                            : com.android.internal.R.drawable.ic_expand_more);
            });

            TextView label = viewForPackage.findViewById(R.id.label);
            label.setText(nhp.label != null ? nhp.label : nhp.pkgName);
            ImageView icon = viewForPackage.findViewById(R.id.icon);
            icon.setImageDrawable(nhp.icon);

            RecyclerView rv = viewForPackage.findViewById(R.id.notification_list);
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(new NotificationHistoryAdapter());
            ((NotificationHistoryAdapter) rv.getAdapter()).onRebuildComplete(nhp.notifications);
            mTodayView.addView(viewForPackage);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notification_history);
        mTodayView = findViewById(R.id.apps);
        mSnoozeView = findViewById(R.id.snoozed_list);
        mDismissView = findViewById(R.id.recently_dismissed_list);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPm = getPackageManager();

        mHistoryLoader = new HistoryLoader(this, new NotificationBackend(), mPm);
        mHistoryLoader.load(mOnHistoryLoaderListener);

        mNm = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        try {
            mListener.registerAsSystemService(this, new ComponentName(getPackageName(),
                    this.getClass().getCanonicalName()), ActivityManager.getCurrentUser());
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot register listener", e);
        }
    }

    @Override
    public void onPause() {
        try {
            mListener.unregisterAsSystemService();
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot unregister listener", e);
        }
        super.onPause();
    }

    private final NotificationListenerService mListener = new NotificationListenerService() {

        @Override
        public void onListenerConnected() {
            StatusBarNotification[] snoozed = getSnoozedNotifications();
            if (snoozed == null || snoozed.length == 0) {
                mSnoozeView.setVisibility(View.GONE);
            } else {
                RecyclerView rv = mSnoozeView.findViewById(R.id.notification_list);
                rv.setLayoutManager(new LinearLayoutManager(NotificationHistoryActivity.this));
                rv.setAdapter(new NotificationSbnAdapter(NotificationHistoryActivity.this, mPm));
                ((NotificationSbnAdapter) rv.getAdapter()).onRebuildComplete(
                        Arrays.asList(snoozed));
            }

            try {
                StatusBarNotification[] dismissed = mNm.getHistoricalNotifications(
                        NotificationHistoryActivity.this.getPackageName(), 10);
                RecyclerView rv = mDismissView.findViewById(R.id.notification_list);
                rv.setLayoutManager(new LinearLayoutManager(NotificationHistoryActivity.this));
                rv.setAdapter(new NotificationSbnAdapter(NotificationHistoryActivity.this, mPm));
                ((NotificationSbnAdapter) rv.getAdapter()).onRebuildComplete(
                        Arrays.asList(dismissed));
                mDismissView.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Slog.e(TAG, "Cannot load recently dismissed", e);
                mDismissView.setVisibility(View.GONE);
            }
        }
    };
}
