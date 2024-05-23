/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.notification.modes;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import static java.util.Objects.requireNonNull;

import android.annotation.Nullable;
import android.app.AutomaticZenRule;
import android.content.Context;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.service.notification.SystemZenRules;
import android.text.TextUtils;
import android.util.Log;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.content.res.AppCompatResources;

import com.android.settings.R;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class IconLoader {

    private static final String TAG = "ZenIconLoader";

    private static final Drawable MISSING = new ColorDrawable();

    @Nullable // Until first usage
    private static IconLoader sInstance;

    private final Context mContext;
    private final LruCache<String, Drawable> mCache;
    private final ListeningExecutorService mBackgroundExecutor;

    static IconLoader getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new IconLoader(context);
        }
        return sInstance;
    }

    private IconLoader(Context context) {
        this(context, Executors.newFixedThreadPool(4));
    }

    @VisibleForTesting
    IconLoader(Context context, ExecutorService backgroundExecutor) {
        mContext = context.getApplicationContext();
        mCache = new LruCache<>(50);
        mBackgroundExecutor =
                MoreExecutors.listeningDecorator(backgroundExecutor);
    }

    Context getContext() {
        return mContext;
    }

    @NonNull
    ListenableFuture<Drawable> getIcon(@NonNull AutomaticZenRule rule) {
        if (rule.getIconResId() == 0) {
            return Futures.immediateFuture(getFallbackIcon(rule.getType()));
        }

        return FluentFuture.from(loadIcon(rule.getPackageName(), rule.getIconResId()))
                .transform(icon ->
                        icon != null ? icon : getFallbackIcon(rule.getType()),
                        MoreExecutors.directExecutor());
    }

    @NonNull
    private ListenableFuture</* @Nullable */ Drawable> loadIcon(String pkg, int iconResId) {
        String cacheKey = pkg + ":" + iconResId;
        synchronized (mCache) {
            Drawable cachedValue = mCache.get(cacheKey);
            if (cachedValue != null) {
                return immediateFuture(cachedValue != MISSING ? cachedValue : null);
            }
        }

        return FluentFuture.from(mBackgroundExecutor.submit(() -> {
            if (TextUtils.isEmpty(pkg) || SystemZenRules.PACKAGE_ANDROID.equals(pkg)) {
                return mContext.getDrawable(iconResId);
            } else {
                Context appContext = mContext.createPackageContext(pkg, 0);
                Drawable appDrawable = AppCompatResources.getDrawable(appContext, iconResId);
                return getMonochromeIconIfPresent(appDrawable);
            }
        })).catching(Exception.class, ex -> {
            // If we cannot resolve the icon, then store MISSING in the cache below, so
            // we don't try again.
            Log.e(TAG, "Error while loading icon " + cacheKey, ex);
            return null;
        }, MoreExecutors.directExecutor()).transform(drawable -> {
            synchronized (mCache) {
                mCache.put(cacheKey, drawable != null ? drawable : MISSING);
            }
            return drawable;
        }, MoreExecutors.directExecutor());
    }

    private Drawable getFallbackIcon(int ruleType) {
        int iconResIdFromType = switch (ruleType) {
            // TODO: b/333528437 - continue replacing with proper default icons
            case AutomaticZenRule.TYPE_UNKNOWN -> R.drawable.ic_do_not_disturb_on_24dp;
            case AutomaticZenRule.TYPE_OTHER -> R.drawable.ic_do_not_disturb_on_24dp;
            case AutomaticZenRule.TYPE_SCHEDULE_TIME -> R.drawable.ic_modes_time;
            case AutomaticZenRule.TYPE_SCHEDULE_CALENDAR -> R.drawable.ic_modes_event;
            case AutomaticZenRule.TYPE_BEDTIME -> R.drawable.ic_do_not_disturb_on_24dp;
            case AutomaticZenRule.TYPE_DRIVING -> R.drawable.ic_do_not_disturb_on_24dp;
            case AutomaticZenRule.TYPE_IMMERSIVE -> R.drawable.ic_do_not_disturb_on_24dp;
            case AutomaticZenRule.TYPE_THEATER -> R.drawable.ic_do_not_disturb_on_24dp;
            case AutomaticZenRule.TYPE_MANAGED -> R.drawable.ic_do_not_disturb_on_24dp;
            default -> R.drawable.ic_do_not_disturb_on_24dp;
        };
        return requireNonNull(mContext.getDrawable(iconResIdFromType));
    }

    private static Drawable getMonochromeIconIfPresent(Drawable icon) {
        // For created rules, the app should've provided a monochrome Drawable. However, implicit
        // rules have the app's icon, which is not -- but might have a monochrome layer. Thus
        // we choose it, if present.
        if (icon instanceof AdaptiveIconDrawable adaptiveIcon) {
            if (adaptiveIcon.getMonochrome() != null) {
                // Wrap with negative inset => scale icon (inspired from BaseIconFactory)
                return new InsetDrawable(adaptiveIcon.getMonochrome(),
                        -2.0f * AdaptiveIconDrawable.getExtraInsetFraction());
            }
        }
        return icon;
    }
}
