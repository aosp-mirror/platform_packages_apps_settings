/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.settings.emergency;

import static android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS;
import static android.content.pm.PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS;
import static android.content.pm.PackageManager.MATCH_SYSTEM_ONLY;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;

import androidx.annotation.VisibleForTesting;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.widget.LayoutPreference;

import java.util.List;

/**
 * Preference controller for More settings button
 */
public class MoreSettingsPreferenceController extends BasePreferenceController implements
        View.OnClickListener {

    private static final String EXTRA_KEY_ATTRIBUTION = "attribution";
    private static final String TAG = "MoreSettingsPrefCtrl";
    @VisibleForTesting
    Intent mIntent;
    private LayoutPreference mPreference;

    public MoreSettingsPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        final String packageName = mContext.getResources().getString(
                R.string.config_emergency_package_name);

        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        mIntent = new Intent(Intent.ACTION_MAIN)
                .setPackage(packageName);

        final List<ResolveInfo> info = mContext.getPackageManager()
                .queryIntentActivities(mIntent, MATCH_SYSTEM_ONLY);

        if (info != null && !info.isEmpty()) {
            mIntent.setClassName(packageName, info.get(0).activityInfo.name);
        } else {
            mIntent = null;
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(getPreferenceKey());
        final Button button = mPreference.findViewById(R.id.button);
        final Drawable icon = getIcon();
        button.setText(getButtonText());
        if (icon != null) {
            button.setCompoundDrawablesWithIntrinsicBounds(
                    /* left= */ icon,
                    /* top= */null,
                    /* right= */ null,
                    /* bottom= */ null);
            button.setVisibility(View.VISIBLE);
        }

        button.setOnClickListener(this);
    }

    @Override
    public int getAvailabilityStatus() {
        if (mIntent == null) {
            return UNSUPPORTED_ON_DEVICE;
        } else {
            return AVAILABLE;
        }
    }

    @Override
    public void onClick(View v) {
        FeatureFactory.getFeatureFactory().getMetricsFeatureProvider()
                .logClickedPreference(mPreference, getMetricsCategory());
        final Intent intent = new Intent(mIntent)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Bundle bundle = new Bundle();
        bundle.putString(EXTRA_KEY_ATTRIBUTION, mContext.getPackageName());
        mContext.startActivity(intent, bundle);
    }

    private Drawable getIcon() {
        final String packageName = mContext.getResources().getString(
                R.string.config_emergency_package_name);
        try {
            final PackageManager pm = mContext.getPackageManager();
            final ApplicationInfo appInfo = pm.getApplicationInfo(
                    packageName, MATCH_DISABLED_COMPONENTS
                            | MATCH_DISABLED_UNTIL_USED_COMPONENTS);
            return getScaledDrawable(mContext, Utils.getBadgedIcon(mContext, appInfo), 24, 24);
        } catch (Exception e) {
            Log.d(TAG, "Failed to get open app button icon", e);
            return null;
        }
    }

    private CharSequence getButtonText() {
        final String packageName = mContext.getResources().getString(
                R.string.config_emergency_package_name);
        try {
            final PackageManager pm = mContext.getPackageManager();
            final ApplicationInfo appInfo = pm.getApplicationInfo(
                    packageName, MATCH_DISABLED_COMPONENTS
                            | MATCH_DISABLED_UNTIL_USED_COMPONENTS);
            return mContext.getString(R.string.open_app_button, appInfo.loadLabel(pm));
        } catch (Exception e) {
            Log.d(TAG, "Failed to get open app button text, falling back.");
            return "";
        }
    }

    private static Drawable getScaledDrawable(Context context, Drawable icon, int width,
            int height) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int widthInDp =
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, width, displayMetrics);
        int heightInDp =
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, height,
                        displayMetrics);

        return new BitmapDrawable(context.getResources(),
                convertToBitmap(icon, widthInDp, heightInDp));
    }

    private static Bitmap convertToBitmap(Drawable icon, int width, int height) {
        if (icon == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        icon.setBounds(0, 0, width, height);
        icon.draw(canvas);
        return bitmap;
    }
}
