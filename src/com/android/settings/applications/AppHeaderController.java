/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.applications;

import android.annotation.IdRes;
import android.annotation.UserIdInt;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.support.annotation.IntDef;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.settings.AppHeader;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.instantapps.InstantAppDetails;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.ApplicationsState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class AppHeaderController {

    @IntDef({ActionType.ACTION_NONE,
            ActionType.ACTION_APP_INFO,
            ActionType.ACTION_APP_PREFERENCE,
            ActionType.ACTION_STORE_DEEP_LINK,
            ActionType.ACTION_NOTIF_PREFERENCE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActionType {
        int ACTION_NONE = 0;
        int ACTION_APP_INFO = 1;
        int ACTION_STORE_DEEP_LINK = 2;
        int ACTION_APP_PREFERENCE = 3;
        int ACTION_NOTIF_PREFERENCE = 4;
    }

    public static final String PREF_KEY_APP_HEADER = "pref_app_header";

    private static final String TAG = "AppDetailFeature";

    private final Context mContext;
    private final Fragment mFragment;
    private final int mMetricsCategory;
    private final View mAppHeader;

    private Drawable mIcon;
    private CharSequence mLabel;
    private CharSequence mSummary;
    private String mPackageName;
    private Intent mAppNotifPrefIntent;
    @UserIdInt
    private int mUid = UserHandle.USER_NULL;
    @ActionType
    private int mLeftAction;
    @ActionType
    private int mRightAction;

    private InstantAppDetails mInstantAppDetails;

    public AppHeaderController(Context context, Fragment fragment, View appHeader) {
        mContext = context;
        mFragment = fragment;
        mMetricsCategory = FeatureFactory.getFactory(context).getMetricsFeatureProvider()
                .getMetricsCategory(fragment);
        if (appHeader != null) {
            mAppHeader = appHeader;
        } else {
            mAppHeader = LayoutInflater.from(mContext)
                    .inflate(R.layout.app_details, null /* root */);
        }
    }

    public AppHeaderController setIcon(Drawable icon) {
        if (icon != null) {
            mIcon = icon.getConstantState().newDrawable(mContext.getResources());
        }
        return this;
    }

    public AppHeaderController setIcon(ApplicationsState.AppEntry appEntry) {
        if (appEntry.icon != null) {
            mIcon = appEntry.icon.getConstantState().newDrawable(mContext.getResources());
        }
        return this;
    }

    public AppHeaderController setLabel(CharSequence label) {
        mLabel = label;
        return this;
    }

    public AppHeaderController setLabel(ApplicationsState.AppEntry appEntry) {
        mLabel = appEntry.label;
        return this;
    }

    public AppHeaderController setSummary(CharSequence summary) {
        mSummary = summary;
        return this;
    }

    public AppHeaderController setSummary(PackageInfo packageInfo) {
        if (packageInfo != null) {
            mSummary = packageInfo.versionName;
        }
        return this;
    }

    public AppHeaderController setButtonActions(@ActionType int leftAction,
            @ActionType int rightAction) {
        mLeftAction = leftAction;
        mRightAction = rightAction;
        return this;
    }

    public AppHeaderController setPackageName(String packageName) {
        mPackageName = packageName;
        return this;
    }

    public AppHeaderController setUid(int uid) {
        mUid = uid;
        return this;
    }

    public AppHeaderController setAppNotifPrefIntent(Intent appNotifPrefIntent) {
        mAppNotifPrefIntent = appNotifPrefIntent;
        return this;
    }

    public AppHeaderController setInstantAppDetails(InstantAppDetails instantAppDetails) {
        mInstantAppDetails = instantAppDetails;
        return this;
    }

    /**
     * Binds app header view and data from {@code PackageInfo} and {@code AppEntry}.
     */
    public void bindAppHeader(PackageInfo packageInfo, ApplicationsState.AppEntry appEntry) {
        final String versionName = packageInfo == null ? null : packageInfo.versionName;
        final Resources res = mAppHeader.getResources();

        // Set Icon
        final ImageView iconView = (ImageView) mAppHeader.findViewById(R.id.app_detail_icon);
        if (appEntry.icon != null) {
            iconView.setImageDrawable(appEntry.icon.getConstantState().newDrawable(res));
        }

        // Set application name.
        final TextView labelView = (TextView) mAppHeader.findViewById(R.id.app_detail_title);
        labelView.setText(appEntry.label);

        // Version number of application
        final TextView appVersion = (TextView) mAppHeader.findViewById(R.id.app_detail_summary);

        if (!TextUtils.isEmpty(versionName)) {
            appVersion.setSelected(true);
            appVersion.setVisibility(View.VISIBLE);
            appVersion.setText(res.getString(R.string.version_text, String.valueOf(versionName)));
        } else {
            appVersion.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Done mutating appheader, rebinds everything and return a new {@link LayoutPreference}.
     */
    public LayoutPreference done(Context context) {
        final LayoutPreference pref = new LayoutPreference(context, done());
        // Makes sure it's the first preference onscreen.
        pref.setOrder(-1000);
        pref.setKey(PREF_KEY_APP_HEADER);
        return pref;
    }

    /**
     * Done mutating appheader, rebinds everything.
     */
    public View done() {
        return done(true);
    }

    /**
     * Done mutating appheader, rebinds everything (optionally skip rebinding buttons).
     */
    public View done(boolean rebindActions) {
        ImageView iconView = (ImageView) mAppHeader.findViewById(R.id.app_detail_icon);
        if (iconView != null) {
            iconView.setImageDrawable(mIcon);
        }
        setText(R.id.app_detail_title, mLabel);
        setText(R.id.app_detail_summary, mSummary);
        if (rebindActions) {
            bindAppHeaderButtons();
        }

        if (mInstantAppDetails != null) {
            setText(R.id.instant_app_developer_title, mInstantAppDetails.developerTitle);
            View maturity = mAppHeader.findViewById(R.id.instant_app_maturity);

            if (maturity != null) {
                String maturityText = mInstantAppDetails.maturityRatingString;
                Drawable maturityIcon = mInstantAppDetails.maturityRatingIcon;
                if (!TextUtils.isEmpty(maturityText) || maturityIcon != null) {
                    maturity.setVisibility(View.VISIBLE);
                }
                setText(R.id.instant_app_maturity_text, maturityText);
                if (maturityIcon != null) {
                    ImageView maturityIconView = (ImageView) mAppHeader.findViewById(
                            R.id.instant_app_maturity_icon);
                    if (maturityIconView != null) {
                        maturityIconView.setImageDrawable(maturityIcon);
                    }
                }
            }
            setText(R.id.instant_app_monetization, mInstantAppDetails.monetizationNotice);
        }

        return mAppHeader;
    }

    /**
     * Only binds app header with button actions.
     */
    public void bindAppHeaderButtons() {
        ImageButton leftButton = (ImageButton) mAppHeader.findViewById(R.id.left_button);
        ImageButton rightButton = (ImageButton) mAppHeader.findViewById(R.id.right_button);

        bindButton(leftButton, mLeftAction);
        bindButton(rightButton, mRightAction);
    }

    private void bindButton(ImageButton button, @ActionType int action) {
        if (button == null) {
            return;
        }
        switch (action) {
            case ActionType.ACTION_APP_INFO: {
                if (mPackageName == null || mPackageName.equals(Utils.OS_PKG)
                        || mUid == UserHandle.USER_NULL
                        || !AppHeader.includeAppInfo(mFragment)) {
                    button.setVisibility(View.GONE);
                } else {
                    button.setContentDescription(
                            mContext.getString(R.string.application_info_label));
                    button.setImageResource(com.android.settings.R.drawable.ic_info);
                    button.setOnClickListener(v -> AppInfoBase.startAppInfoFragment(
                            InstalledAppDetails.class, R.string.application_info_label,
                            mPackageName, mUid, mFragment, 0 /* request */, mMetricsCategory));
                    button.setVisibility(View.VISIBLE);
                }
                return;
            }
            case ActionType.ACTION_STORE_DEEP_LINK: {
                final Intent intent = new Intent(Intent.ACTION_SHOW_APP_INFO)
                        .setPackage(getInstallerPackageName(mContext, mPackageName));
                final Intent result = resolveIntent(intent);
                if (result == null) {
                    button.setVisibility(View.GONE);
                } else {
                    result.putExtra(Intent.EXTRA_PACKAGE_NAME, mPackageName);
                    button.setImageResource(R.drawable.ic_sim_sd);
                    button.setOnClickListener(v -> mFragment.startActivity(intent));
                    button.setVisibility(View.VISIBLE);
                }
                return;
            }
            case ActionType.ACTION_NOTIF_PREFERENCE: {
                if (mAppNotifPrefIntent == null) {
                    button.setVisibility(View.GONE);
                } else {
                    button.setOnClickListener(v -> mFragment.startActivity(mAppNotifPrefIntent));
                    button.setVisibility(View.VISIBLE);
                }
                return;
            }
            case ActionType.ACTION_APP_PREFERENCE: {
                final Intent intent = resolveIntent(
                        new Intent(Intent.ACTION_APPLICATION_PREFERENCES).setPackage(mPackageName));
                if (intent == null) {
                    button.setVisibility(View.GONE);
                    return;
                }
                button.setOnClickListener(v -> mFragment.startActivity(intent));
                button.setVisibility(View.VISIBLE);
                return;
            }
            case ActionType.ACTION_NONE: {
                button.setVisibility(View.GONE);
                return;
            }
        }
    }

    private String getInstallerPackageName(Context context, String packageName) {
        try {
            return context.getPackageManager().getInstallerPackageName(packageName);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Exception while retrieving the package installer of " + packageName, e);
            return null;
        }
    }

    private Intent resolveIntent(Intent i) {
        ResolveInfo result = mContext.getPackageManager().resolveActivity(i, 0);
        if (result != null) {
            return new Intent(i.getAction())
                    .setClassName(result.activityInfo.packageName, result.activityInfo.name);
        }
        return null;
    }

    private void setText(@IdRes int id, CharSequence text) {
        TextView textView = (TextView) mAppHeader.findViewById(id);
        if (textView != null) {
            textView.setText(text);
            textView.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
        }
    }
}