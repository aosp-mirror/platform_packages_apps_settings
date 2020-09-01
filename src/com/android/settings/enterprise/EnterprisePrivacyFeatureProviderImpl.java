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

package com.android.settings.enterprise;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.view.View;

import com.android.settings.R;
import com.android.settings.vpn2.VpnUtils;

import java.util.Date;
import java.util.List;

public class EnterprisePrivacyFeatureProviderImpl implements EnterprisePrivacyFeatureProvider {

    private final Context mContext;
    private final DevicePolicyManager mDpm;
    private final PackageManager mPm;
    private final UserManager mUm;
    private final ConnectivityManager mCm;
    private final Resources mResources;

    private static final int MY_USER_ID = UserHandle.myUserId();

    public EnterprisePrivacyFeatureProviderImpl(Context context, DevicePolicyManager dpm,
            PackageManager pm, UserManager um, ConnectivityManager cm,
            Resources resources) {
        mContext = context.getApplicationContext();
        mDpm = dpm;
        mPm = pm;
        mUm = um;
        mCm = cm;
        mResources = resources;
    }

    @Override
    public boolean hasDeviceOwner() {
        return getDeviceOwnerComponent() != null;
    }

    @Override
    public boolean isInCompMode() {
        return hasDeviceOwner() && getManagedProfileUserId() != UserHandle.USER_NULL;
    }

    @Override
    public String getDeviceOwnerOrganizationName() {
        final CharSequence organizationName = mDpm.getDeviceOwnerOrganizationName();
        if (organizationName == null) {
            return null;
        } else {
            return organizationName.toString();
        }
    }

    @Override
    public CharSequence getDeviceOwnerDisclosure() {
        if (!hasDeviceOwner()) {
            return null;
        }

        final SpannableStringBuilder disclosure = new SpannableStringBuilder();
        final CharSequence organizationName = mDpm.getDeviceOwnerOrganizationName();
        if (organizationName != null) {
            disclosure.append(mResources.getString(R.string.do_disclosure_with_name,
                    organizationName));
        } else {
            disclosure.append(mResources.getString(R.string.do_disclosure_generic));
        }
        disclosure.append(mResources.getString(R.string.do_disclosure_learn_more_separator));
        disclosure.append(mResources.getString(R.string.learn_more),
                new EnterprisePrivacySpan(mContext), 0);
        return disclosure;
    }

    @Override
    public Date getLastSecurityLogRetrievalTime() {
        final long timestamp = mDpm.getLastSecurityLogRetrievalTime();
        return timestamp < 0 ? null : new Date(timestamp);
    }

    @Override
    public Date getLastBugReportRequestTime() {
        final long timestamp = mDpm.getLastBugReportRequestTime();
        return timestamp < 0 ? null : new Date(timestamp);
    }

    @Override
    public Date getLastNetworkLogRetrievalTime() {
        final long timestamp = mDpm.getLastNetworkLogRetrievalTime();
        return timestamp < 0 ? null : new Date(timestamp);
    }

    @Override
    public boolean isSecurityLoggingEnabled() {
        return mDpm.isSecurityLoggingEnabled(null);
    }

    @Override
    public boolean isNetworkLoggingEnabled() {
        return mDpm.isNetworkLoggingEnabled(null);
    }

    @Override
    public boolean isAlwaysOnVpnSetInCurrentUser() {
        return VpnUtils.isAlwaysOnVpnSet(mCm, MY_USER_ID);
    }

    @Override
    public boolean isAlwaysOnVpnSetInManagedProfile() {
        final int managedProfileUserId = getManagedProfileUserId();
        return managedProfileUserId != UserHandle.USER_NULL &&
                VpnUtils.isAlwaysOnVpnSet(mCm, managedProfileUserId);
    }

    @Override
    public boolean isGlobalHttpProxySet() {
        return mCm.getGlobalProxy() != null;
    }

    @Override
    public int getMaximumFailedPasswordsBeforeWipeInCurrentUser() {
        ComponentName owner = mDpm.getDeviceOwnerComponentOnCallingUser();
        if (owner == null) {
            owner = mDpm.getProfileOwnerAsUser(MY_USER_ID);
        }
        if (owner == null) {
            return 0;
        }
        return mDpm.getMaximumFailedPasswordsForWipe(owner, MY_USER_ID);
    }

    @Override
    public int getMaximumFailedPasswordsBeforeWipeInManagedProfile() {
        final int userId = getManagedProfileUserId();
        if (userId == UserHandle.USER_NULL) {
            return 0;
        }
        final ComponentName profileOwner = mDpm.getProfileOwnerAsUser(userId);
        if (profileOwner == null) {
            return 0;
        }
        return mDpm.getMaximumFailedPasswordsForWipe(profileOwner, userId);
    }

    @Override
    public String getImeLabelIfOwnerSet() {
        if (!mDpm.isCurrentInputMethodSetByOwner()) {
            return null;
        }
        final String packageName = Settings.Secure.getStringForUser(mContext.getContentResolver(),
                Settings.Secure.DEFAULT_INPUT_METHOD, MY_USER_ID);
        if (packageName == null) {
            return null;
        }
        try {
            return mPm.getApplicationInfoAsUser(packageName, 0 /* flags */, MY_USER_ID)
                    .loadLabel(mPm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    @Override
    public int getNumberOfOwnerInstalledCaCertsForCurrentUser() {
        final List<String> certs = mDpm.getOwnerInstalledCaCerts(new UserHandle(MY_USER_ID));
        if (certs == null) {
            return 0;
        }
        return certs.size();
    }

    @Override
    public int getNumberOfOwnerInstalledCaCertsForManagedProfile() {
        final int userId = getManagedProfileUserId();
        if (userId == UserHandle.USER_NULL) {
            return 0;
        }
        final List<String> certs = mDpm.getOwnerInstalledCaCerts(new UserHandle(userId));
        if (certs == null) {
            return 0;
        }
        return certs.size();
    }

    @Override
    public int getNumberOfActiveDeviceAdminsForCurrentUserAndManagedProfile() {
        int activeAdmins = 0;
        for (final UserInfo userInfo : mUm.getProfiles(MY_USER_ID)) {
            final List<ComponentName> activeAdminsForUser
                    = mDpm.getActiveAdminsAsUser(userInfo.id);
            if (activeAdminsForUser != null) {
                activeAdmins += activeAdminsForUser.size();
            }
        }
        return activeAdmins;
    }

    @Override
    public boolean hasWorkPolicyInfo() {
        return (getWorkPolicyInfoIntentDO() != null) || (getWorkPolicyInfoIntentPO() != null);
    }

    @Override
    public boolean showWorkPolicyInfo() {
        Intent intent = getWorkPolicyInfoIntentDO();
        if (intent != null) {
            mContext.startActivity(intent);
            return true;
        }

        intent = getWorkPolicyInfoIntentPO();
        final UserInfo userInfo = getManagedProfileUserInfo();
        if (intent != null && userInfo != null) {
            mContext.startActivityAsUser(intent, userInfo.getUserHandle());
            return true;
        }

        return false;
    }

    private ComponentName getDeviceOwnerComponent() {
        if (!mPm.hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN)) {
            return null;
        }
        return mDpm.getDeviceOwnerComponentOnAnyUser();
    }

    private UserInfo getManagedProfileUserInfo() {
        for (final UserInfo userInfo : mUm.getProfiles(MY_USER_ID)) {
            if (userInfo.isManagedProfile()) {
                return userInfo;
            }
        }
        return null;
    }

    private int getManagedProfileUserId() {
        final UserInfo userInfo = getManagedProfileUserInfo();
        if (userInfo != null) {
            return userInfo.id;
        }
        return UserHandle.USER_NULL;
    }

    private Intent getWorkPolicyInfoIntentDO() {
        final ComponentName ownerComponent = getDeviceOwnerComponent();
        if (ownerComponent == null) {
            return null;
        }

        // Only search for the required action in the Device Owner's package
        final Intent intent =
                new Intent(Settings.ACTION_SHOW_WORK_POLICY_INFO)
                        .setPackage(ownerComponent.getPackageName())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final List<ResolveInfo> activities = mPm.queryIntentActivities(intent, 0);
        if (activities.size() != 0) {
            return intent;
        }

        return null;
    }

    private Intent getWorkPolicyInfoIntentPO() {
        final int userId = getManagedProfileUserId();
        if (userId == UserHandle.USER_NULL) {
            return null;
        }

        final ComponentName ownerComponent = mDpm.getProfileOwnerAsUser(userId);
        if (ownerComponent == null) {
            return null;
        }

        // Only search for the required action in the Profile Owner's package
        final Intent intent =
                new Intent(Settings.ACTION_SHOW_WORK_POLICY_INFO)
                        .setPackage(ownerComponent.getPackageName())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        final List<ResolveInfo> activities = mPm.queryIntentActivitiesAsUser(intent, 0, userId);
        if (activities.size() != 0) {
            return intent;
        }

        return null;
    }

    protected static class EnterprisePrivacySpan extends ClickableSpan {
        private final Context mContext;

        public EnterprisePrivacySpan(Context context) {
            mContext = context;
        }

        @Override
        public void onClick(View widget) {
            mContext.startActivity(new Intent(Settings.ACTION_ENTERPRISE_PRIVACY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof EnterprisePrivacySpan
                    && ((EnterprisePrivacySpan) object).mContext == mContext;
        }
    }
}
