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

import static android.provider.Settings.Secure.NOTIFICATION_HISTORY_ENABLED;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.INotificationManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
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

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.widget.SwitchBar;

import java.util.ArrayList;
import java.util.Arrays;

public class NotificationHistoryActivity extends Activity {

    private static String TAG = "NotifHistory";

    private ViewGroup mHistoryOn;
    private ViewGroup mHistoryOff;
    private ViewGroup mTodayView;
    private ViewGroup mSnoozeView;
    private ViewGroup mDismissView;

    private SettingsObserver mSettingsObserver = new SettingsObserver();
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
            expand.setContentDescription(container.getVisibility() == View.VISIBLE
                    ? getString(R.string.condition_expand_hide)
                    : getString(R.string.condition_expand_show));
            expand.setOnClickListener(v -> {
                    container.setVisibility(container.getVisibility() == View.VISIBLE
                            ? View.GONE : View.VISIBLE);
                    expand.setImageResource(container.getVisibility() == View.VISIBLE
                            ? R.drawable.ic_expand_less
                            : com.android.internal.R.drawable.ic_expand_more);
                    expand.setContentDescription(container.getVisibility() == View.VISIBLE
                            ? getString(R.string.condition_expand_hide)
                            : getString(R.string.condition_expand_show));
            });

            TextView label = viewForPackage.findViewById(R.id.label);
            label.setText(nhp.label != null ? nhp.label : nhp.pkgName);
            ImageView icon = viewForPackage.findViewById(R.id.icon);
            icon.setImageDrawable(nhp.icon);

            TextView count = viewForPackage.findViewById(R.id.count);
            count.setText(getResources().getQuantityString(R.plurals.notification_history_count,
                    nhp.notifications.size(), nhp.notifications.size()));

            RecyclerView rv = viewForPackage.findViewById(R.id.notification_list);
            LinearLayoutManager lm = new LinearLayoutManager(this);
            rv.setLayoutManager(lm);
            rv.setAdapter(new NotificationHistoryAdapter());
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                    rv.getContext(), lm.getOrientation());
            rv.addItemDecoration(dividerItemDecoration);
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
        mHistoryOff = findViewById(R.id.history_off);
        mHistoryOn = findViewById(R.id.history_on);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPm = getPackageManager();

        mTodayView.removeAllViews();
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
        mSettingsObserver.observe();

        bindSwitch();
    }

    @Override
    public void onPause() {
        try {
            mListener.unregisterAsSystemService();
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot unregister listener", e);
        }
        mSettingsObserver.stopObserving();
        super.onPause();
    }

    private void bindSwitch() {
        SwitchBar bar = findViewById(R.id.switch_bar);
        if (bar != null) {
            bar.setSwitchBarText(R.string.notification_history_turn_off,
                    R.string.notification_history_turn_on);
            bar.show();
            try {
                bar.addOnSwitchChangeListener(mOnSwitchClickListener);
            } catch (IllegalStateException e) {
                // an exception is thrown if you try to add the listener twice
            }
            bar.setChecked(Settings.Secure.getInt(getContentResolver(),
                    NOTIFICATION_HISTORY_ENABLED, 0) == 1);
            toggleViews(bar.isChecked());
        }
    }

    private void toggleViews(boolean isChecked) {
        if (isChecked) {
            mHistoryOff.setVisibility(View.GONE);
            mHistoryOn.setVisibility(View.VISIBLE);
        } else {
            mHistoryOn.setVisibility(View.GONE);
            mHistoryOff.setVisibility(View.VISIBLE);
            mTodayView.removeAllViews();
        }
    }

    private void onHistoryEnabledChanged(boolean enabled) {
        if (enabled) {
            mHistoryLoader.load(mOnHistoryLoaderListener);
        }
    }

    final class SettingsObserver extends ContentObserver {
        private final Uri NOTIFICATION_HISTORY_URI
                = Settings.Secure.getUriFor(Settings.Secure.NOTIFICATION_HISTORY_ENABLED);

        SettingsObserver() {
            super(null);
        }

        void observe() {
            ContentResolver resolver = getContentResolver();
            resolver.registerContentObserver(NOTIFICATION_HISTORY_URI,
                    false, this, UserHandle.USER_ALL);
        }

        void stopObserving() {
            ContentResolver resolver = getContentResolver();
            resolver.unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            update(uri);
        }

        public void update(Uri uri) {
            ContentResolver resolver = getContentResolver();
            if (uri == null || NOTIFICATION_HISTORY_URI.equals(uri)) {
                boolean historyEnabled = Settings.Secure.getInt(resolver,
                        Settings.Secure.NOTIFICATION_HISTORY_ENABLED, 0)
                        != 0;
                onHistoryEnabledChanged(historyEnabled);
            }
        }
    }

    private final SwitchBar.OnSwitchChangeListener mOnSwitchClickListener =
            (switchView, isChecked) -> {
                Settings.Secure.putInt(getContentResolver(),
                        NOTIFICATION_HISTORY_ENABLED,
                        isChecked ? 1 : 0);
                toggleViews(isChecked);
            };

    private final NotificationListenerService mListener = new NotificationListenerService() {
        private RecyclerView mDismissedRv;
        private RecyclerView mSnoozedRv;

        @Override
        public void onListenerConnected() {
            StatusBarNotification[] snoozed = null;
            StatusBarNotification[] dismissed = null;
            try {
                snoozed = getSnoozedNotifications();
                dismissed = mNm.getHistoricalNotifications(
                    NotificationHistoryActivity.this.getPackageName(), 6, false);
            } catch (SecurityException | RemoteException e) {
                Log.d(TAG, "OnPaused called while trying to retrieve notifications");
            }

            mSnoozedRv = mSnoozeView.findViewById(R.id.notification_list);
            LinearLayoutManager lm = new LinearLayoutManager(NotificationHistoryActivity.this);
            mSnoozedRv.setLayoutManager(lm);
            mSnoozedRv.setAdapter(
                    new NotificationSbnAdapter(NotificationHistoryActivity.this, mPm));
            DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                    mSnoozedRv.getContext(), lm.getOrientation());
            mSnoozedRv.addItemDecoration(dividerItemDecoration);
            mSnoozedRv.setNestedScrollingEnabled(false);

            if (snoozed == null || snoozed.length == 0) {
                mSnoozeView.setVisibility(View.GONE);
            } else {
                ((NotificationSbnAdapter) mSnoozedRv.getAdapter()).onRebuildComplete(
                        new ArrayList<>(Arrays.asList(snoozed)));
            }

            mDismissedRv = mDismissView.findViewById(R.id.notification_list);
            LinearLayoutManager dismissLm =
                new LinearLayoutManager(NotificationHistoryActivity.this);
            mDismissedRv.setLayoutManager(dismissLm);
            mDismissedRv.setAdapter(
                new NotificationSbnAdapter(NotificationHistoryActivity.this, mPm));
            DividerItemDecoration dismissDivider = new DividerItemDecoration(
                mDismissedRv.getContext(), dismissLm.getOrientation());
            mDismissedRv.addItemDecoration(dismissDivider);
            mDismissedRv.setNestedScrollingEnabled(false);

            if (dismissed == null || dismissed.length == 0) {
                mDismissView.setVisibility(View.GONE);
            } else {
                mDismissView.setVisibility(View.VISIBLE);
                ((NotificationSbnAdapter) mDismissedRv.getAdapter()).onRebuildComplete(
                    new ArrayList<>(Arrays.asList(dismissed)));
            }
        }

        @Override
        public void onNotificationPosted(StatusBarNotification sbn) {
            // making lint happy
        }

        @Override
        public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap,
                int reason) {
            if (reason == REASON_SNOOZED) {
                ((NotificationSbnAdapter) mSnoozedRv.getAdapter()).addSbn(sbn);
                mSnoozeView.setVisibility(View.VISIBLE);
            } else {
                ((NotificationSbnAdapter) mDismissedRv.getAdapter()).addSbn(sbn);
                mDismissView.setVisibility(View.VISIBLE);
            }
        }
    };
}
