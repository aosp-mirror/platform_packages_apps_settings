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

package com.android.settings.widget;

import android.annotation.IdRes;
import android.annotation.UserIdInt;
import android.app.ActionBar;
import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.applications.appinfo.AppInfoDashboardFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.widget.ActionBarShadowController;
import com.android.settingslib.widget.LayoutPreference;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class EntityHeaderController {

    @IntDef({ActionType.ACTION_NONE,
            ActionType.ACTION_NOTIF_PREFERENCE,
            ActionType.ACTION_EDIT_PREFERENCE,})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActionType {
        int ACTION_NONE = 0;
        int ACTION_NOTIF_PREFERENCE = 1;
        int ACTION_EDIT_PREFERENCE = 2;
    }

    public static final String PREF_KEY_APP_HEADER = "pref_app_header";

    private static final String TAG = "AppDetailFeature";

    private final Context mAppContext;
    private final Activity mActivity;
    private final Fragment mFragment;
    private final int mMetricsCategory;
    private final View mHeader;
    private Lifecycle mLifecycle;
    private RecyclerView mRecyclerView;
    private Drawable mIcon;
    private String mIconContentDescription;
    private CharSequence mLabel;
    private CharSequence mSummary;
    // Required for hearing aid devices.
    private CharSequence mSecondSummary;
    private String mPackageName;
    private Intent mAppNotifPrefIntent;
    @UserIdInt
    private int mUid = UserHandle.USER_NULL;
    @ActionType
    private int mAction1;
    @ActionType
    private int mAction2;

    private boolean mHasAppInfoLink;

    private boolean mIsInstantApp;

    private View.OnClickListener mEditOnClickListener;

    /**
     * Creates a new instance of the controller.
     *
     * @param fragment The fragment that header will be placed in.
     * @param header   Optional: header view if it's already created.
     */
    public static EntityHeaderController newInstance(Activity activity, Fragment fragment,
            View header) {
        return new EntityHeaderController(activity, fragment, header);
    }

    private EntityHeaderController(Activity activity, Fragment fragment, View header) {
        mActivity = activity;
        mAppContext = activity.getApplicationContext();
        mFragment = fragment;
        mMetricsCategory = FeatureFactory.getFactory(mAppContext).getMetricsFeatureProvider()
                .getMetricsCategory(fragment);
        if (header != null) {
            mHeader = header;
        } else {
            mHeader = LayoutInflater.from(fragment.getContext())
                    .inflate(R.layout.settings_entity_header, null /* root */);
        }
    }

    public EntityHeaderController setRecyclerView(RecyclerView recyclerView, Lifecycle lifecycle) {
        mRecyclerView = recyclerView;
        mLifecycle = lifecycle;
        return this;
    }

    /**
     * Set the icon in the header. Callers should also consider calling setIconContentDescription
     * to provide a description of this icon for accessibility purposes.
     */
    public EntityHeaderController setIcon(Drawable icon) {
        if (icon != null) {
            mIcon = icon.getConstantState().newDrawable(mAppContext.getResources());
        }
        return this;
    }

    /**
     * Convenience method to set the header icon from an ApplicationsState.AppEntry. Callers should
     * also consider calling setIconContentDescription to provide a description of this icon for
     * accessibility purposes.
     */
    public EntityHeaderController setIcon(ApplicationsState.AppEntry appEntry) {
        mIcon = IconDrawableFactory.newInstance(mAppContext).getBadgedIcon(appEntry.info);
        return this;
    }

    public EntityHeaderController setIconContentDescription(String contentDescription) {
        mIconContentDescription = contentDescription;
        return this;
    }

    public EntityHeaderController setLabel(CharSequence label) {
        mLabel = label;
        return this;
    }

    public EntityHeaderController setLabel(ApplicationsState.AppEntry appEntry) {
        mLabel = appEntry.label;
        return this;
    }

    public EntityHeaderController setSummary(CharSequence summary) {
        mSummary = summary;
        return this;
    }

    public EntityHeaderController setSummary(PackageInfo packageInfo) {
        if (packageInfo != null) {
            mSummary = packageInfo.versionName;
        }
        return this;
    }

    public EntityHeaderController setSecondSummary(CharSequence summary) {
        mSecondSummary = summary;
        return this;
    }

    public EntityHeaderController setSecondSummary(PackageInfo packageInfo) {
        if (packageInfo != null) {
            mSummary = packageInfo.versionName;
        }
        return this;
    }

    public EntityHeaderController setHasAppInfoLink(boolean hasAppInfoLink) {
        mHasAppInfoLink = hasAppInfoLink;
        return this;
    }

    public EntityHeaderController setButtonActions(@ActionType int action1,
            @ActionType int action2) {
        mAction1 = action1;
        mAction2 = action2;
        return this;
    }

    public EntityHeaderController setPackageName(String packageName) {
        mPackageName = packageName;
        return this;
    }

    public EntityHeaderController setUid(int uid) {
        mUid = uid;
        return this;
    }

    public EntityHeaderController setAppNotifPrefIntent(Intent appNotifPrefIntent) {
        mAppNotifPrefIntent = appNotifPrefIntent;
        return this;
    }

    public EntityHeaderController setIsInstantApp(boolean isInstantApp) {
        this.mIsInstantApp = isInstantApp;
        return this;
    }

    public EntityHeaderController setEditListener(View.OnClickListener listener) {
        this.mEditOnClickListener = listener;
        return this;
    }

    /**
     * Done mutating entity header, rebinds everything and return a new {@link LayoutPreference}.
     */
    public LayoutPreference done(Activity activity, Context uiContext) {
        final LayoutPreference pref = new LayoutPreference(uiContext, done(activity));
        // Makes sure it's the first preference onscreen.
        pref.setOrder(-1000);
        pref.setSelectable(false);
        pref.setKey(PREF_KEY_APP_HEADER);
        pref.setAllowDividerBelow(true);
        return pref;
    }

    /**
     * Done mutating entity header, rebinds everything (optionally skip rebinding buttons).
     */
    public View done(Activity activity, boolean rebindActions) {
        styleActionBar(activity);
        ImageView iconView = mHeader.findViewById(R.id.entity_header_icon);
        if (iconView != null) {
            iconView.setImageDrawable(mIcon);
            iconView.setContentDescription(mIconContentDescription);
        }
        setText(R.id.entity_header_title, mLabel);
        setText(R.id.entity_header_summary, mSummary);
        setText(R.id.entity_header_second_summary, mSecondSummary);
        if (mIsInstantApp) {
            setText(R.id.install_type,
                    mHeader.getResources().getString(R.string.install_type_instant));
        }

        if (rebindActions) {
            bindHeaderButtons();
        }

        return mHeader;
    }

    /**
     * Only binds entity header with button actions.
     */
    public EntityHeaderController bindHeaderButtons() {
        final View entityHeaderContent = mHeader.findViewById(R.id.entity_header_content);
        final ImageButton button1 = mHeader.findViewById(android.R.id.button1);
        final ImageButton button2 = mHeader.findViewById(android.R.id.button2);
        bindAppInfoLink(entityHeaderContent);
        bindButton(button1, mAction1);
        bindButton(button2, mAction2);
        return this;
    }

    private void bindAppInfoLink(View entityHeaderContent) {
        if (!mHasAppInfoLink) {
            // Caller didn't ask for app link, skip.
            return;
        }
        if (entityHeaderContent == null
                || mPackageName == null
                || mPackageName.equals(Utils.OS_PKG)
                || mUid == UserHandle.USER_NULL) {
            Log.w(TAG, "Missing ingredients to build app info link, skip");
            return;
        }
        entityHeaderContent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppInfoBase.startAppInfoFragment(
                        AppInfoDashboardFragment.class, R.string.application_info_label,
                        mPackageName, mUid, mFragment, 0 /* request */,
                        mMetricsCategory);
            }
        });
        return;
    }

    /**
     * Styles the action bar (elevation, scrolling behaviors, color, etc).
     * <p/>
     * This method must be called after {@link Fragment#onCreate(Bundle)}.
     */
    public EntityHeaderController styleActionBar(Activity activity) {
        if (activity == null) {
            Log.w(TAG, "No activity, cannot style actionbar.");
            return this;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (actionBar == null) {
            Log.w(TAG, "No actionbar, cannot style actionbar.");
            return this;
        }
        actionBar.setBackgroundDrawable(
                new ColorDrawable(
                        Utils.getColorAttrDefaultColor(activity, android.R.attr.colorPrimaryDark)));
        actionBar.setElevation(0);
        if (mRecyclerView != null && mLifecycle != null) {
            ActionBarShadowController.attachToView(mActivity, mLifecycle, mRecyclerView);
        }

        return this;
    }

    /**
     * Done mutating entity header, rebinds everything.
     */
    @VisibleForTesting
    View done(Activity activity) {
        return done(activity, true /* rebindActions */);
    }

    private void bindButton(ImageButton button, @ActionType int action) {
        if (button == null) {
            return;
        }
        switch (action) {
            case ActionType.ACTION_EDIT_PREFERENCE: {
                if (mEditOnClickListener == null) {
                    button.setVisibility(View.GONE);
                } else {
                    button.setImageResource(com.android.internal.R.drawable.ic_mode_edit);
                    button.setVisibility(View.VISIBLE);
                    button.setOnClickListener(mEditOnClickListener);
                }
                return;
            }
            case ActionType.ACTION_NOTIF_PREFERENCE: {
                if (mAppNotifPrefIntent == null) {
                    button.setVisibility(View.GONE);
                } else {
                    button.setOnClickListener(v -> {
                        FeatureFactory.getFactory(mAppContext).getMetricsFeatureProvider()
                                .action(SettingsEnums.PAGE_UNKNOWN,
                                        SettingsEnums.ACTION_OPEN_APP_NOTIFICATION_SETTING,
                                        mMetricsCategory,
                                        null, 0);
                        mFragment.startActivity(mAppNotifPrefIntent);
                    });
                    button.setVisibility(View.VISIBLE);
                }
                return;
            }
            case ActionType.ACTION_NONE: {
                button.setVisibility(View.GONE);
                return;
            }
        }
    }


    private void setText(@IdRes int id, CharSequence text) {
        TextView textView = mHeader.findViewById(id);
        if (textView != null) {
            textView.setText(text);
            textView.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
        }
    }
}
