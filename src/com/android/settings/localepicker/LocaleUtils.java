/*
 * Copyright (C) 2023 The Android Open Source Project
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
import static com.android.settings.localepicker.LocaleListEditor.EXTRA_RESULT_LOCALE;
import static com.android.settings.localepicker.RegionAndNumberingSystemPickerFragment.EXTRA_IS_NUMBERING_SYSTEM;
import static com.android.settings.localepicker.RegionAndNumberingSystemPickerFragment.EXTRA_TARGET_LOCALE;

import android.app.Dialog;
import android.app.LocaleManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.LocaleList;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocaleStore;
import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * A locale utility class.
 */
public class LocaleUtils {
    private static final String TAG = "LocaleUtils";
    private static final String CHANNEL_ID_SUGGESTION = "suggestion";
    private static final String CHANNEL_ID_SUGGESTION_TO_USER = "Locale suggestion";
    private static final String EXTRA_APP_LOCALE = "app_locale";
    private static final String EXTRA_NOTIFICATION_ID = "notification_id";
    private static final int SIM_LOCALE = 1 << 0;
    private static final int SYSTEM_LOCALE = 1 << 1;
    private static final int APP_LOCALE = 1 << 2;
    private static final int IME_LOCALE = 1 << 3;

    /**
     * Checks if the languageTag is in the system locale. Since in the current design, the system
     * language list would not show two locales with the same language and region but different
     * numbering system. So, the u extension has to be stripped out in the process of comparison.
     *
     * @param languageTag A language tag
     * @return true if the locale is in the system locale. Otherwise, false.
     */
    public static boolean isInSystemLocale(@NonNull String languageTag) {
        LocaleList systemLocales = LocaleList.getDefault();
        Locale localeWithoutUextension =
                new Locale.Builder()
                        .setLocale(Locale.forLanguageTag(languageTag))
                        .clearExtensions()
                        .build();
        for (int i = 0; i < systemLocales.size(); i++) {
            Locale sysLocaleWithoutUextension =
                    new Locale.Builder().setLocale(systemLocales.get(i)).clearExtensions().build();
            if (localeWithoutUextension.equals(sysLocaleWithoutUextension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Logs the locale, sets the default locale for the app then broadcasts it.
     *
     * @param context    Context
     * @param localeInfo locale info
     */
    public static void onLocaleSelected(@NonNull Context context,
            @NonNull LocaleStore.LocaleInfo localeInfo,
            @NonNull String packageName) {
        if (localeInfo.getLocale() == null || localeInfo.isSystemLocale()) {
            setAppDefaultLocale(context, "", packageName);
        } else {
            logLocaleSource(context, localeInfo);
            setAppDefaultLocale(context, localeInfo.getLocale().toLanguageTag(),
                    packageName);
            broadcastAppLocaleChange(context, localeInfo, packageName);
        }
    }

    private static void logLocaleSource(Context context, LocaleStore.LocaleInfo localeInfo) {
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
        MetricsFeatureProvider metricsFeatureProvider =
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        metricsFeatureProvider.action(context,
                SettingsEnums.ACTION_CHANGE_APP_LANGUAGE_FROM_SUGGESTED, localeSource);
    }

    private static boolean hasSuggestionType(LocaleStore.LocaleInfo localeInfo,
            int suggestionType) {
        return localeInfo.isSuggestionOfType(suggestionType);
    }

    private static void setAppDefaultLocale(Context context, String languageTag,
            String packageName) {
        LocaleManager localeManager = context.getSystemService(LocaleManager.class);
        if (localeManager == null) {
            Log.w(TAG, "LocaleManager is null, cannot set default app locale");
            return;
        }
        localeManager.setApplicationLocales(packageName,
                LocaleList.forLanguageTags(languageTag));
    }

    private static void broadcastAppLocaleChange(Context context, LocaleStore.LocaleInfo localeInfo,
            String packageName) {
        if (!localeNotificationEnabled()) {
            Log.w(TAG, "Locale notification is not enabled");
            return;
        }
        if (localeInfo.isAppCurrentLocale()) {
            return;
        }
        try {
            NotificationController notificationController = NotificationController.getInstance(
                    context);
            String localeTag = localeInfo.getLocale().toLanguageTag();
            int uid = context.getPackageManager().getApplicationInfo(packageName,
                    PackageManager.GET_META_DATA).uid;
            boolean launchNotification = notificationController.shouldTriggerNotification(
                    uid, localeTag);
            if (launchNotification) {
                triggerNotification(
                        context,
                        notificationController.getNotificationId(localeTag),
                        context.getString(R.string.title_system_locale_addition,
                                localeInfo.getFullNameNative()),
                        context.getString(R.string.desc_system_locale_addition),
                        localeTag);
                MetricsFeatureProvider metricsFeatureProvider =
                        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
                metricsFeatureProvider.action(context,
                        SettingsEnums.ACTION_NOTIFICATION_FOR_SYSTEM_LOCALE);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Unable to find info for package: " + packageName);
        }
    }

    private static void triggerNotification(
            Context context,
            int notificationId,
            String title,
            String description,
            String localeTag) {
        NotificationManager notificationManager = context.getSystemService(
                NotificationManager.class);
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
                new NotificationCompat.Builder(context, CHANNEL_ID_SUGGESTION)
                        .setSmallIcon(R.drawable.ic_settings_language)
                        .setAutoCancel(true)
                        .setContentTitle(title)
                        .setContentText(description)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentIntent(
                                createPendingIntent(context, localeTag, notificationId, false))
                        .setDeleteIntent(
                                createPendingIntent(context, localeTag, notificationId, true));
        notificationManager.notify(notificationId, builder.build());
    }

    private static PendingIntent createPendingIntent(Context context, String locale,
            int notificationId,
            boolean isDeleteIntent) {
        Intent intent = isDeleteIntent
                ? new Intent(context, NotificationCancelReceiver.class)
                : new Intent(context, NotificationActionActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        intent.putExtra(EXTRA_APP_LOCALE, locale)
                .putExtra(EXTRA_NOTIFICATION_ID, notificationId);
        int flag = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        int elapsedTime = (int) SystemClock.elapsedRealtimeNanos();

        return isDeleteIntent
                ? PendingIntent.getBroadcast(context, elapsedTime, intent, flag)
                : PendingIntent.getActivity(context, elapsedTime, intent, flag);
    }

    /**
     * Sort the locale's list.
     *
     * @param localeInfos   list of locale Infos
     * @param isCountryMode Whether the locale page is in country mode or not.
     * @return localeInfos list of locale Infos
     */
    public static @NonNull List<LocaleStore.LocaleInfo> getSortedLocaleList(
            @NonNull List<LocaleStore.LocaleInfo> localeInfos, boolean isCountryMode) {
        final Locale sortingLocale = Locale.getDefault();
        final LocaleHelper.LocaleInfoComparator comp = new LocaleHelper.LocaleInfoComparator(
                sortingLocale, isCountryMode);
        Collections.sort(localeInfos, comp);
        return localeInfos;
    }

    /**
     * Sort the locale's list by keywords in search.
     *
     * @param searchList    locale Infos in search bar
     * @param localeList    list of locale Infos
     * @param isCountryMode Whether the locale page is in country mode or not.
     * @return localeInfos list of locale Infos
     */
    public static @NonNull List<LocaleStore.LocaleInfo> getSortedLocaleFromSearchList(
            @NonNull List<LocaleStore.LocaleInfo> searchList,
            @NonNull List<LocaleStore.LocaleInfo> localeList,
            boolean isCountryMode) {
        List<LocaleStore.LocaleInfo> searchItem = localeList.stream()
                .filter(suggested -> searchList.stream()
                        .anyMatch(option -> option.getLocale() != null
                                && option.getLocale().getLanguage().equals(
                                suggested.getLocale().getLanguage())))
                .distinct()
                .collect(Collectors.toList());
        return getSortedLocaleList(searchItem, isCountryMode);
    }
}
