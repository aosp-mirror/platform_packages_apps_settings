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
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static androidx.core.view.accessibility.AccessibilityEventCompat.TYPE_VIEW_ACCESSIBILITY_FOCUSED;

import android.annotation.AttrRes;
import android.annotation.ColorInt;
import android.annotation.DrawableRes;
import android.app.ActionBar;
import android.app.ActivityManager;
import android.app.INotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
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
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.logging.UiEvent;
import com.android.internal.logging.UiEventLogger;
import com.android.internal.logging.UiEventLoggerImpl;
import com.android.internal.widget.NotificationExpandButton;
import com.android.settings.R;
import com.android.settings.notification.NotificationBackend;
import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;
import com.android.settingslib.utils.StringUtil;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.MainSwitchBar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class NotificationHistoryActivity extends CollapsingToolbarBaseActivity {

    private static String TAG = "NotifHistory";
    // MAX_RECENT_DISMISS_ITEM_COUNT needs to be less or equals than
    // R.integer.config_notificationServiceArchiveSize, which is the Number of notifications kept
    // in the notification service historical archive
    private static final int MAX_RECENT_DISMISS_ITEM_COUNT = 50;
    private static final int HISTORY_HOURS = 24;

    private ViewGroup mHistoryOn;
    private ViewGroup mHistoryOff;
    private ViewGroup mHistoryEmpty;
    private ViewGroup mTodayView;
    private ViewGroup mSnoozeView;
    private ViewGroup mDismissView;
    private MainSwitchBar mSwitchBar;

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
                notifications.isEmpty() ? GONE : VISIBLE);
        mCountdownLatch.countDown();
        View recyclerView = mTodayView.findViewById(R.id.apps);
        recyclerView.setClipToOutline(true);
        // for each package, new header and recycler view
        for (int i = 0, notificationsSize = notifications.size(); i < notificationsSize; i++) {
            NotificationHistoryPackage nhp = notifications.get(i);
            View viewForPackage = LayoutInflater.from(this)
                    .inflate(R.layout.notification_history_app_layout, null);

            int cornerType = ROUND_CORNER_CENTER;
            if (i == (notificationsSize - 1)) {
                cornerType |= ROUND_CORNER_BOTTOM;
            }
            if (i == 0) {
                cornerType |= ROUND_CORNER_TOP;
            }
            int backgroundRes = NotificationHistoryActivity.getRoundCornerDrawableRes(cornerType);
            viewForPackage.setBackgroundResource(backgroundRes);

            final View container = viewForPackage.findViewById(R.id.notification_list_wrapper);
            container.setVisibility(GONE);
            View header = viewForPackage.findViewById(R.id.app_header);
            NotificationExpandButton expand = viewForPackage.findViewById(
                    com.android.internal.R.id.expand_button);
            int textColor = obtainThemeColor(android.R.attr.textColorPrimary);
            int backgroundColor = obtainThemeColor(android.R.attr.colorBackgroundFloating);
            int pillColor = ColorUtils.blendARGB(textColor, backgroundColor, 0.9f);
            expand.setDefaultPillColor(pillColor);
            expand.setDefaultTextColor(textColor);
            expand.setExpanded(false);
            header.setStateDescription(container.getVisibility() == VISIBLE
                    ? getString(R.string.condition_expand_hide)
                    : getString(R.string.condition_expand_show));
            int finalI = i;
            header.setOnClickListener(v -> {
                container.setVisibility(container.getVisibility() == VISIBLE
                        ? GONE : VISIBLE);
                expand.setExpanded(container.getVisibility() == VISIBLE);
                header.setStateDescription(container.getVisibility() == VISIBLE
                        ? getString(R.string.condition_expand_hide)
                        : getString(R.string.condition_expand_show));
                header.sendAccessibilityEvent(TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                mUiEventLogger.logWithPosition((container.getVisibility() == VISIBLE)
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
            count.setText(StringUtil.getIcuPluralsString(this, nhp.notifications.size(),
                    R.string.notification_history_count));

            final NotificationHistoryRecyclerView rv =
                    viewForPackage.findViewById(R.id.notification_list);
            rv.setAdapter(new NotificationHistoryAdapter(mNm, rv,
                    newCount -> {
                        count.setText(StringUtil.getIcuPluralsString(this, newCount,
                                R.string.notification_history_count));
                        if (newCount == 0) {
                            viewForPackage.setVisibility(GONE);
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
        mSwitchBar = findViewById(R.id.main_switch_bar);
        ((TextView) findViewById(R.id.today_header)).setText(
                getString(R.string.notification_history_today, HISTORY_HOURS));

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
                        && findViewById(R.id.today_list).getVisibility() == GONE
                        && mSnoozeView.getVisibility() == GONE
                        && mDismissView.getVisibility() == GONE) {
                    mHistoryOn.setVisibility(GONE);
                    mHistoryEmpty.setVisibility(VISIBLE);
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

    public static final int ROUND_CORNER_CENTER = 1;
    public static final int ROUND_CORNER_TOP = 1 << 1;
    public static final int ROUND_CORNER_BOTTOM = 1 << 2;

    public static @DrawableRes int getRoundCornerDrawableRes(int cornerType) {

        if ((cornerType & ROUND_CORNER_CENTER) == 0) {
            return 0;
        }

        if (((cornerType & ROUND_CORNER_TOP) != 0) && ((cornerType & ROUND_CORNER_BOTTOM) == 0)) {
            // the first
            return com.android.settingslib.widget.theme.R.drawable.settingslib_round_background_top;
        } else if (((cornerType & ROUND_CORNER_BOTTOM) != 0)
                && ((cornerType & ROUND_CORNER_TOP) == 0)) {
            // the last
            return com.android.settingslib.widget.theme.R.drawable.settingslib_round_background_bottom;
        } else if (((cornerType & ROUND_CORNER_TOP) != 0)
                && ((cornerType & ROUND_CORNER_BOTTOM) != 0)) {
            // the only one preference
            return com.android.settingslib.widget.theme.R.drawable.settingslib_round_background;
        } else {
            // in the center
            return com.android.settingslib.widget.theme.R.drawable.settingslib_round_background_center;
        }
    }

    private @ColorInt int obtainThemeColor(@AttrRes int attrRes) {
        Resources.Theme theme = new ContextThemeWrapper(this,
                android.R.style.Theme_DeviceDefault_DayNight).getTheme();
        try (TypedArray ta = theme.obtainStyledAttributes(new int[]{attrRes})) {
            return ta == null ? 0 : ta.getColor(0, 0);
        }
    }

    private void bindSwitch() {
        if (mSwitchBar != null) {
            mSwitchBar.show();
            mSwitchBar.setTitle(getString(R.string.notification_history_toggle));
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
            mHistoryOff.setVisibility(GONE);
            mHistoryOn.setVisibility(VISIBLE);
        } else {
            mHistoryOn.setVisibility(GONE);
            mHistoryOff.setVisibility(VISIBLE);
            mTodayView.removeAllViews();
        }
        mHistoryEmpty.setVisibility(GONE);
    }

    private final OnCheckedChangeListener mOnSwitchClickListener =
            (switchView, isChecked) -> {
                int oldState = 0;
                try {
                    oldState = Settings.Secure.getInt(getContentResolver(),
                            NOTIFICATION_HISTORY_ENABLED);
                } catch (Settings.SettingNotFoundException ignored) {
                }
                final int newState = isChecked ? 1 : 0;
                if (oldState != newState) {
                    Settings.Secure.putInt(
                            getContentResolver(), NOTIFICATION_HISTORY_ENABLED, newState);
                    mUiEventLogger.log(isChecked ? NotificationHistoryEvent.NOTIFICATION_HISTORY_ON
                            : NotificationHistoryEvent.NOTIFICATION_HISTORY_OFF);
                    Log.d(TAG, "onSwitchChange history to " + isChecked);
                }
                // Reset UI visibility to ensure it matches real state.
                mHistoryOn.setVisibility(GONE);
                if (isChecked) {
                    mHistoryEmpty.setVisibility(VISIBLE);
                    mHistoryOff.setVisibility(GONE);
                } else {
                    mHistoryOff.setVisibility(VISIBLE);
                    mHistoryEmpty.setVisibility(GONE);
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
                        NotificationHistoryActivity.this.getAttributionTag(),
                        MAX_RECENT_DISMISS_ITEM_COUNT, false);
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
                mSnoozeView.setVisibility(GONE);
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
                            false, mUiEventLogger));
            mDismissedRv.setNestedScrollingEnabled(false);

            if (dismissed == null || dismissed.length == 0) {
                mDismissView.setVisibility(GONE);
            } else {
                mDismissView.setVisibility(VISIBLE);
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
                mSnoozeView.setVisibility(VISIBLE);
            } else {
                ((NotificationSbnAdapter) mDismissedRv.getAdapter()).addSbn(sbn);
                mDismissView.setVisibility(VISIBLE);
            }
        }
    };
}
