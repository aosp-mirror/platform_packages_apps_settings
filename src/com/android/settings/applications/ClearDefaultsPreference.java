/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.usb.IUsbManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;

public class ClearDefaultsPreference extends Preference {

    protected static final String TAG = ClearDefaultsPreference.class.getSimpleName();
    protected ApplicationsState.AppEntry mAppEntry;

    private Button mActivitiesButton;

    private AppWidgetManager mAppWidgetManager;
    private IUsbManager mUsbManager;
    private PackageManager mPm;
    private String mPackageName;

    private final boolean mAppsControlDisallowedBySystem;
    private final RestrictedLockUtils.EnforcedAdmin mAppsControlDisallowedAdmin;

    public ClearDefaultsPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setLayoutResource(R.layout.app_preferred_settings);

        mAppWidgetManager = AppWidgetManager.getInstance(context);
        mPm = context.getPackageManager();
        IBinder b = ServiceManager.getService(Context.USB_SERVICE);
        mUsbManager = IUsbManager.Stub.asInterface(b);

        mAppsControlDisallowedBySystem = RestrictedLockUtilsInternal.hasBaseUserRestriction(
                getContext(), UserManager.DISALLOW_APPS_CONTROL, UserHandle.myUserId());
        mAppsControlDisallowedAdmin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                getContext(), UserManager.DISALLOW_APPS_CONTROL, UserHandle.myUserId());


    }

    public ClearDefaultsPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ClearDefaultsPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context,
                androidx.preference.R.attr.preferenceStyle,
                android.R.attr.preferenceStyle));
    }

    public ClearDefaultsPreference(Context context) {
        this(context, null);
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }

    public void setAppEntry(ApplicationsState.AppEntry entry) {
        mAppEntry = entry;
    }

    @Override
    public void onBindViewHolder(final PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        mActivitiesButton = (Button) view.findViewById(R.id.clear_activities_button);
        mActivitiesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAppsControlDisallowedAdmin != null && !mAppsControlDisallowedBySystem) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(
                            getContext(), mAppsControlDisallowedAdmin);
                    return;
                }

                if (mUsbManager != null) {
                    final int userId = UserHandle.myUserId();
                    mPm.clearPackagePreferredActivities(mPackageName);
                    if (isDefaultBrowser(mPackageName)) {
                        mPm.setDefaultBrowserPackageNameAsUser(null, userId);
                    }
                    try {
                        mUsbManager.clearDefaults(mPackageName, userId);
                    } catch (RemoteException e) {
                        Log.e(TAG, "mUsbManager.clearDefaults", e);
                    }
                    mAppWidgetManager.setBindAppWidgetPermission(mPackageName, false);
                    TextView autoLaunchView = (TextView) view.findViewById(R.id.auto_launch);
                    resetLaunchDefaultsUi(autoLaunchView);
                }
            }
        });

        updateUI(view);
    }

    public boolean updateUI(PreferenceViewHolder view) {
        boolean hasBindAppWidgetPermission =
                mAppWidgetManager.hasBindAppWidgetPermission(mAppEntry.info.packageName);

        TextView autoLaunchView = (TextView) view.findViewById(R.id.auto_launch);
        boolean autoLaunchEnabled = AppUtils.hasPreferredActivities(mPm, mPackageName)
                || isDefaultBrowser(mPackageName)
                || AppUtils.hasUsbDefaults(mUsbManager, mPackageName);
        if (!autoLaunchEnabled && !hasBindAppWidgetPermission) {
            resetLaunchDefaultsUi(autoLaunchView);
        } else {
            boolean useBullets = hasBindAppWidgetPermission && autoLaunchEnabled;

            if (hasBindAppWidgetPermission) {
                autoLaunchView.setText(R.string.auto_launch_label_generic);
            } else {
                autoLaunchView.setText(R.string.auto_launch_label);
            }

            Context context = getContext();
            CharSequence text = null;
            int bulletIndent = context.getResources().getDimensionPixelSize(
                    R.dimen.installed_app_details_bullet_offset);
            if (autoLaunchEnabled) {
                CharSequence autoLaunchEnableText = context.getText(
                        R.string.auto_launch_enable_text);
                SpannableString s = new SpannableString(autoLaunchEnableText);
                if (useBullets) {
                    s.setSpan(new BulletSpan(bulletIndent), 0, autoLaunchEnableText.length(), 0);
                }
                text = (text == null) ?
                        TextUtils.concat(s, "\n") : TextUtils.concat(text, "\n", s, "\n");
            }
            if (hasBindAppWidgetPermission) {
                CharSequence alwaysAllowBindAppWidgetsText =
                        context.getText(R.string.always_allow_bind_appwidgets_text);
                SpannableString s = new SpannableString(alwaysAllowBindAppWidgetsText);
                if (useBullets) {
                    s.setSpan(new BulletSpan(bulletIndent),
                            0, alwaysAllowBindAppWidgetsText.length(), 0);
                }
                text = (text == null) ?
                        TextUtils.concat(s, "\n") : TextUtils.concat(text, "\n", s, "\n");
            }
            autoLaunchView.setText(text);
            mActivitiesButton.setEnabled(true);
        }
        return true;
    }

    private boolean isDefaultBrowser(String packageName) {
        final String defaultBrowser = mPm.getDefaultBrowserPackageNameAsUser(UserHandle.myUserId());
        return packageName.equals(defaultBrowser);
    }

    private void resetLaunchDefaultsUi(TextView autoLaunchView) {
        autoLaunchView.setText(R.string.auto_launch_disable_text);
        // Disable clear activities button
        mActivitiesButton.setEnabled(false);
    }
}
