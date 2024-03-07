/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.localepicker;

import static com.android.settings.flags.Flags.localeNotificationEnabled;

import android.app.FragmentTransaction;
import android.app.LocaleManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.LocaleList;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;

import androidx.core.app.NotificationCompat;
import androidx.core.view.ViewCompat;

import com.android.internal.app.LocalePickerWithRegion;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.applications.AppLocaleUtil;
import com.android.settings.applications.appinfo.AppLocaleDetails;
import com.android.settings.core.SettingsBaseActivity;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

public class AppLocalePickerActivity extends SettingsBaseActivity
        implements LocalePickerWithRegion.LocaleSelectedListener, MenuItem.OnActionExpandListener {
    private static final String TAG = AppLocalePickerActivity.class.getSimpleName();
    private static final String CHANNEL_ID_SUGGESTION = "suggestion";
    private static final String CHANNEL_ID_SUGGESTION_TO_USER = "Locale suggestion";
    private static final int SIM_LOCALE = 1 << 0;
    private static final int SYSTEM_LOCALE = 1 << 1;
    private static final int APP_LOCALE = 1 << 2;
    private static final int IME_LOCALE = 1 << 3;
    static final String EXTRA_APP_LOCALE = "app_locale";
    static final String EXTRA_NOTIFICATION_ID = "notification_id";

    private String mPackageName;
    private LocalePickerWithRegion mLocalePickerWithRegion;
    private AppLocaleDetails mAppLocaleDetails;
    private View mAppLocaleDetailContainer;
    private NotificationController mNotificationController;
    private MetricsFeatureProvider mMetricsFeatureProvider;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri data = getIntent().getData();
        if (data == null) {
            Log.d(TAG, "There is no uri data.");
            finish();
            return;
        }
        mPackageName = data.getSchemeSpecificPart();
        if (TextUtils.isEmpty(mPackageName)) {
            Log.d(TAG, "There is no package name.");
            finish();
            return;
        }

        if (!canDisplayLocaleUi()) {
            Log.w(TAG, "Not allow to display Locale Settings UI.");
            finish();
            return;
        }

        setTitle(R.string.app_locale_picker_title);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mNotificationController = NotificationController.getInstance(this);

        mLocalePickerWithRegion = LocalePickerWithRegion.createLanguagePicker(
                this,
                this,
                false /* translate only */,
                null,
                mPackageName,
                this);
        mAppLocaleDetails = AppLocaleDetails.newInstance(mPackageName, getUserId());
        mAppLocaleDetailContainer = launchAppLocaleDetailsPage();
        // Launch Locale picker part.
        launchLocalePickerPage();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocaleSelected(LocaleStore.LocaleInfo localeInfo) {
        if (localeInfo == null || localeInfo.getLocale() == null || localeInfo.isSystemLocale()) {
            setAppDefaultLocale("");
        } else {
            logLocaleSource(localeInfo);
            setAppDefaultLocale(localeInfo.getLocale().toLanguageTag());
            broadcastAppLocaleChange(localeInfo);
        }
        finish();
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        mAppBarLayout.setExpanded(false /*expanded*/, false /*animate*/);
        ViewCompat.setNestedScrollingEnabled(mAppLocaleDetails.getListView(), true);
        ViewCompat.setNestedScrollingEnabled(mLocalePickerWithRegion.getListView(), true);
        return true;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        mAppBarLayout.setExpanded(false /*expanded*/, false /*animate*/);
        ViewCompat.setNestedScrollingEnabled(mAppLocaleDetails.getListView(), false);
        ViewCompat.setNestedScrollingEnabled(mLocalePickerWithRegion.getListView(), false);
        return true;
    }

    /** Sets the app's locale to the supplied language tag */
    private void setAppDefaultLocale(String languageTag) {
        Log.d(TAG, "setAppDefaultLocale: " + languageTag);
        LocaleManager localeManager = getSystemService(LocaleManager.class);
        if (localeManager == null) {
            Log.w(TAG, "LocaleManager is null, cannot set default app locale");
            return;
        }
        localeManager.setApplicationLocales(mPackageName, LocaleList.forLanguageTags(languageTag));
    }

    private void broadcastAppLocaleChange(LocaleStore.LocaleInfo localeInfo) {
        if (!localeNotificationEnabled()) {
            return;
        }
        String localeTag = localeInfo.getLocale().toLanguageTag();
        if (LocaleUtils.isInSystemLocale(localeTag) || localeInfo.isAppCurrentLocale()) {
            return;
        }
        try {
            int uid = getPackageManager().getApplicationInfo(mPackageName,
                    PackageManager.GET_META_DATA).uid;
            boolean launchNotification = mNotificationController.shouldTriggerNotification(
                    uid, localeTag);
            if (launchNotification) {
                triggerNotification(
                        mNotificationController.getNotificationId(localeTag),
                        getString(R.string.title_system_locale_addition,
                                localeInfo.getFullNameNative()),
                        getString(R.string.desc_system_locale_addition),
                        localeTag);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to find info for package: " + mPackageName);
        }
    }

    private void triggerNotification(
            int notificationId,
            String title,
            String description,
            String localeTag) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        final boolean channelExist =
                notificationManager.getNotificationChannel(CHANNEL_ID_SUGGESTION) != null;

        // Create an alert channel if it does not exist
        if (!channelExist) {
            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID_SUGGESTION,
                            CHANNEL_ID_SUGGESTION_TO_USER,
                            NotificationManager.IMPORTANCE_DEFAULT);
            channel.setSound(/* sound */ null, /* audioAttributes */ null); // silent notification
            notificationManager.createNotificationChannel(channel);
        }
        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID_SUGGESTION)
                        .setSmallIcon(R.drawable.ic_settings_language)
                        .setAutoCancel(true)
                        .setContentTitle(title)
                        .setContentText(description)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentIntent(
                                createPendingIntent(localeTag, notificationId, false))
                        .setDeleteIntent(
                                createPendingIntent(localeTag, notificationId, true));
        notificationManager.notify(notificationId, builder.build());
    }

    private PendingIntent createPendingIntent(String locale, int notificationId,
            boolean isDeleteIntent) {
        Intent intent = isDeleteIntent
                ? new Intent(this, NotificationCancelReceiver.class)
                : new Intent(this, NotificationActionActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        intent.putExtra(EXTRA_APP_LOCALE, locale)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        int flag = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        int elapsedTime = (int) SystemClock.elapsedRealtimeNanos();

        return isDeleteIntent
                ? PendingIntent.getBroadcast(this, elapsedTime, intent, flag)
                : PendingIntent.getActivity(this, elapsedTime, intent, flag);
    }

    private View launchAppLocaleDetailsPage() {
        FrameLayout appLocaleDetailsContainer = new FrameLayout(this);
        appLocaleDetailsContainer.setId(R.id.layout_app_locale_details);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.layout_app_locale_details, mAppLocaleDetails)
                .commit();
        return appLocaleDetailsContainer;
    }

    private void launchLocalePickerPage() {
        // LocalePickerWithRegion use android.app.ListFragment. Thus, it can not use
        // getSupportFragmentManager() to add this into container.
        android.app.FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.registerFragmentLifecycleCallbacks(
                new android.app.FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentViewCreated(
                            android.app.FragmentManager fm,
                            android.app.Fragment f, View v, Bundle s) {
                        super.onFragmentViewCreated(fm, f, v, s);
                        ListView listView = (ListView) v.findViewById(android.R.id.list);
                        if (listView != null) {
                            listView.addHeaderView(mAppLocaleDetailContainer);
                        }
                    }
                }, true);
        fragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(R.id.content_frame, mLocalePickerWithRegion)
                .commit();
    }

    private boolean canDisplayLocaleUi() {
        try {
            PackageManager packageManager = getPackageManager();
            return AppLocaleUtil.canDisplayLocaleUi(this,
                    packageManager.getApplicationInfo(mPackageName, 0),
                    packageManager.queryIntentActivities(AppLocaleUtil.LAUNCHER_ENTRY_INTENT,
                            PackageManager.GET_META_DATA));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to find info for package: " + mPackageName);
        }

        return false;
    }

    private void logLocaleSource(LocaleStore.LocaleInfo localeInfo) {
        if (!localeInfo.isSuggested() || localeInfo.isAppCurrentLocale()) {
            return;
        }
        int localeSource = 0;
        if (hasSuggestionType(localeInfo,
                LocaleStore.LocaleInfo.SUGGESTION_TYPE_SYSTEM_AVAILABLE_LANGUAGE)) {
            localeSource |= SYSTEM_LOCALE;
        }
        if (hasSuggestionType(localeInfo,
                LocaleStore.LocaleInfo.SUGGESTION_TYPE_OTHER_APP_LANGUAGE)) {
            localeSource |= APP_LOCALE;
        }
        if (hasSuggestionType(localeInfo, LocaleStore.LocaleInfo.SUGGESTION_TYPE_IME_LANGUAGE)) {
            localeSource |= IME_LOCALE;
        }
        if (hasSuggestionType(localeInfo, LocaleStore.LocaleInfo.SUGGESTION_TYPE_SIM)) {
            localeSource |= SIM_LOCALE;
        }
        mMetricsFeatureProvider.action(this,
                SettingsEnums.ACTION_CHANGE_APP_LANGUAGE_FROM_SUGGESTED, localeSource);
    }

    private static boolean hasSuggestionType(LocaleStore.LocaleInfo localeInfo,
            int suggestionType) {
        return localeInfo.isSuggestionOfType(suggestionType);
    }
}
