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

import static androidx.core.view.accessibility.AccessibilityEventCompat.TYPE_VIEW_ACCESSIBILITY_FOCUSED;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.INotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.util.Slog;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.logging.UiEvent;
import com.android.internal.logging.UiEventLogger;
import com.android.internal.logging.UiEventLoggerImpl;
import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.utils.ThreadUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class NotificationHistoryActivity extends Activity {

    private static String TAG = "NotifHistory";

    private ViewGroup mHistoryOn;
    private ViewGroup mHistoryOff;
    private ViewGroup mHistoryEmpty;
    private ViewGroup mTodayView;
    private ViewGroup mSnoozeView;
    private ViewGroup mDismissView;
    private SwitchBar mSwitchBar;

    private HistoryLoader mHistoryLoader;
    private INotificationManager mNm;
    private UserManager mUm;
    private PackageManager mPm;
    private CountDownLatch mCountdownLatch;
    private Future mCountdownFuture;
    private UiEventLogger mUiEventLogger = new UiEventLoggerImpl();

    enum NotificationHistoryEvent implements UiEventLogger.UiEventEnum {
        @UiEvent(doc = "User turned on notification history")
        NOTIFICATION_HISTORY_ON(504),

        @UiEvent(doc = "User turned off notification history")
        NOTIFICATION_HISTORY_OFF(505),

        @UiEvent(doc = "User opened notification history page")
        NOTIFICATION_HISTORY_OPEN(506),

        @UiEvent(doc = "User closed notification history page")
        NOTIFICATION_HISTORY_CLOSE(507),

        @UiEvent(doc = "User clicked on a notification history item in recently dismissed section")
        NOTIFICATION_HISTORY_RECENT_ITEM_CLICK(508),

        @UiEvent(doc = "User clicked on a notification history item in snoozed section")
        NOTIFICATION_HISTORY_SNOOZED_ITEM_CLICK(509),

        @UiEvent(doc = "User clicked to expand the notification history of a package (app)")
        NOTIFICATION_HISTORY_PACKAGE_HISTORY_OPEN(510),

        @UiEvent(doc = "User clicked to close the notification history of a package (app)")
        NOTIFICATION_HISTORY_PACKAGE_HISTORY_CLOSE(511),

        @UiEvent(doc = "User clicked on a notification history item in an expanded by-app section")
        NOTIFICATION_HISTORY_OLDER_ITEM_CLICK(512),

        @UiEvent(doc = "User dismissed a notification history item in an expanded by-app section")
        NOTIFICATION_HISTORY_OLDER_ITEM_DELETE(513);

        private int mId;
        NotificationHistoryEvent(int id) {
            mId = id;
        }
        @Override
        public int getId() {
            return mId;
        }
    }

    private HistoryLoader.OnHistoryLoaderListener mOnHistoryLoaderListener = notifications -> {
        findViewById(R.id.today_list).setVisibility(
                notifications.isEmpty() ? View.GONE : View.VISIBLE);
        mCountdownLatch.countDown();
        mTodayView.setClipToOutline(true);
        mTodayView.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                final TypedArray ta = NotificationHistoryActivity.this.obtainStyledAttributes(
                        new int[]{android.R.attr.dialogCornerRadius});
                final float dialogCornerRadius = ta.getDimension(0, 0);
                ta.recycle();
                TypedValue v = new TypedValue();
                NotificationHistoryActivity.this.getTheme().resolveAttribute(
                        com.android.internal.R.attr.listDivider, v, true);
                int bottomPadding = NotificationHistoryActivity.this.getDrawable(v.resourceId)
                        .getIntrinsicHeight();
                outline.setRoundRect(0, 0, view.getWidth(), (view.getHeight() - bottomPadding),
                        dialogCornerRadius);
            }
        });
        // for each package, new header and recycler view
        for (int i = 0, notificationsSize = notifications.size(); i < notificationsSize; i++) {
            NotificationHistoryPackage nhp = notifications.get(i);
            View viewForPackage = LayoutInflater.from(this)
                    .inflate(R.layout.notification_history_app_layout, null);

            final View container = viewForPackage.findViewById(R.id.notification_list);
            container.setVisibility(View.GONE);
            View header = viewForPackage.findViewById(R.id.app_header);
            ImageView expand = viewForPackage.findViewById(R.id.expand);
            header.setStateDescription(container.getVisibility() == View.VISIBLE
                    ? getString(R.string.condition_expand_hide)
                    : getString(R.string.condition_expand_show));
            int finalI = i;
            header.setOnClickListener(v -> {
                container.setVisibility(container.getVisibility() == View.VISIBLE
                        ? View.GONE : View.VISIBLE);
                expand.setImageResource(container.getVisibility() == View.VISIBLE
                        ? R.drawable.ic_expand_less
                        : com.android.internal.R.drawable.ic_expand_more);
                header.setStateDescription(container.getVisibility() == View.VISIBLE
                        ? getString(R.string.condition_expand_hide)
                        : getString(R.string.condition_expand_show));
                header.sendAccessibilityEvent(TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                mUiEventLogger.logWithPosition(
                        (container.getVisibility() == View.VISIBLE)
                            ? NotificationHistoryEvent.NOTIFICATION_HISTORY_PACKAGE_HISTORY_OPEN
                            : NotificationHistoryEvent.NOTIFICATION_HISTORY_PACKAGE_HISTORY_CLOSE,
                        nhp.uid, nhp.pkgName, finalI);
            });

            TextView label = viewForPackage.findViewById(R.id.label);
            label.setText(nhp.label != null ? nhp.label : nhp.pkgName);
            label.setContentDescription(mUm.getBadgedLabelForUser(label.getText(),
                    UserHandle.getUserHandleForUid(nhp.uid)));
            ImageView icon = viewForPackage.findViewById(R.id.icon);
            icon.setImageDrawable(nhp.icon);

            TextView count = viewForPackage.findViewById(R.id.count);
            count.setText(getResources().getQuantityString(R.plurals.notification_history_count,
                    nhp.notifications.size(), nhp.notifications.size()));

            final NotificationHistoryRecyclerView rv =
                    viewForPackage.findViewById(R.id.notification_list);
            rv.setAdapter(new NotificationHistoryAdapter(mNm, rv,
                    newCount -> {
                        count.setText(getResources().getQuantityString(
                                R.plurals.notification_history_count,
                                newCount, newCount));
                        if (newCount == 0) {
                            viewForPackage.setVisibility(View.GONE);
                        }
                    }, mUiEventLogger));
            ((NotificationHistoryAdapter) rv.getAdapter()).onRebuildComplete(
                    new ArrayList<>(nhp.notifications));

            mTodayView.addView(viewForPackage);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.notification_history);
        setContentView(R.layout.notification_history);
        mTodayView = findViewById(R.id.apps);
        mSnoozeView = findViewById(R.id.snoozed_list);
        mDismissView = findViewById(R.id.recently_dismissed_list);
        mHistoryOff = findViewById(R.id.history_off);
        mHistoryOn = findViewById(R.id.history_on);
        mHistoryEmpty = findViewById(R.id.history_on_empty);
        mSwitchBar = findViewById(R.id.switch_bar);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPm = getPackageManager();
        mUm = getSystemService(UserManager.class);
        // wait for history loading and recent/snooze loading
        mCountdownLatch = new CountDownLatch(2);

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

        bindSwitch();

        mCountdownFuture = ThreadUtils.postOnBackgroundThread(() -> {
            try {
                mCountdownLatch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Slog.e(TAG, "timed out waiting for loading", e);
            }
            ThreadUtils.postOnMainThread(() -> {
                if (mSwitchBar.isChecked()
                        && findViewById(R.id.today_list).getVisibility() == View.GONE
                        && mSnoozeView.getVisibility() == View.GONE
                        && mDismissView.getVisibility() == View.GONE) {
                    mHistoryOn.setVisibility(View.GONE);
                    mHistoryEmpty.setVisibility(View.VISIBLE);
                }
            });
        });

        mUiEventLogger.log(NotificationHistoryEvent.NOTIFICATION_HISTORY_OPEN);
    }

    @Override
    public void onPause() {
        try {
            mListener.unregisterAsSystemService();
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot unregister listener", e);
        }
        mUiEventLogger.log(NotificationHistoryEvent.NOTIFICATION_HISTORY_CLOSE);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mCountdownFuture != null) {
            mCountdownFuture.cancel(true);
        }
        super.onDestroy();
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    private void bindSwitch() {
        if (mSwitchBar != null) {
            mSwitchBar.setSwitchBarText(R.string.notification_history_toggle,
                    R.string.notification_history_toggle);
            mSwitchBar.show();
            try {
                mSwitchBar.addOnSwitchChangeListener(mOnSwitchClickListener);
            } catch (IllegalStateException e) {
                // an exception is thrown if you try to add the listener twice
            }
            mSwitchBar.setChecked(Settings.Secure.getInt(getContentResolver(),
                    NOTIFICATION_HISTORY_ENABLED, 0) == 1);
            toggleViews(mSwitchBar.isChecked());
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
        mHistoryEmpty.setVisibility(View.GONE);
    }

    private final SwitchBar.OnSwitchChangeListener mOnSwitchClickListener =
            (switchView, isChecked) -> {
                int oldState = 0;
                try {
                    oldState = Settings.Secure.getInt(getContentResolver(),
                            NOTIFICATION_HISTORY_ENABLED);
                } catch (Settings.SettingNotFoundException ignored) {
                }
                final int newState = isChecked ? 1 : 0;
                if (oldState != newState) {
                    Settings.Secure.putInt(getContentResolver(),
                            NOTIFICATION_HISTORY_ENABLED, newState);
                    mUiEventLogger.log(isChecked ? NotificationHistoryEvent.NOTIFICATION_HISTORY_ON
                            : NotificationHistoryEvent.NOTIFICATION_HISTORY_OFF);
                    Log.d(TAG, "onSwitchChange history to " + isChecked);
                }
                // Reset UI visibility to ensure it matches real state.
                mHistoryOn.setVisibility(View.GONE);
                if (isChecked) {
                    mHistoryEmpty.setVisibility(View.VISIBLE);
                    mHistoryOff.setVisibility(View.GONE);
                } else {
                    mHistoryOff.setVisibility(View.VISIBLE);
                    mHistoryEmpty.setVisibility(View.GONE);
                }
                mTodayView.removeAllViews();
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
                dismissed = mNm.getHistoricalNotificationsWithAttribution(
                        NotificationHistoryActivity.this.getPackageName(),
                        NotificationHistoryActivity.this.getAttributionTag(), 6, false);
            } catch (SecurityException | RemoteException e) {
                Log.d(TAG, "OnPaused called while trying to retrieve notifications");
            }

            mSnoozedRv = mSnoozeView.findViewById(R.id.notification_list);
            LinearLayoutManager lm = new LinearLayoutManager(NotificationHistoryActivity.this);
            mSnoozedRv.setLayoutManager(lm);
            mSnoozedRv.setAdapter(
                    new NotificationSbnAdapter(NotificationHistoryActivity.this, mPm, mUm,
                            true, mUiEventLogger));
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
                    new NotificationSbnAdapter(NotificationHistoryActivity.this, mPm, mUm,
                            false , mUiEventLogger));
            mDismissedRv.setNestedScrollingEnabled(false);

            if (dismissed == null || dismissed.length == 0) {
                mDismissView.setVisibility(View.GONE);
            } else {
                mDismissView.setVisibility(View.VISIBLE);
                ((NotificationSbnAdapter) mDismissedRv.getAdapter()).onRebuildComplete(
                    new ArrayList<>(Arrays.asList(dismissed)));
            }

            mCountdownLatch.countDown();
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
