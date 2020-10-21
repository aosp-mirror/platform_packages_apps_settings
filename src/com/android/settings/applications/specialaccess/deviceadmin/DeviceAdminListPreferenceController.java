/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.applications.specialaccess.deviceadmin;

import static android.app.admin.DevicePolicyManager.ACTION_DEVICE_POLICY_MANAGER_STATE_CHANGED;

import android.app.AppGlobals;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.widget.FilterTouchesSwitchPreference;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.FooterPreferenceMixinCompat;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DeviceAdminListPreferenceController extends BasePreferenceController
        implements LifecycleObserver, OnStart, OnStop {

    private static final IntentFilter FILTER = new IntentFilter();
    private static final String TAG = "DeviceAdminListPrefCtrl";

    private final DevicePolicyManager mDPM;
    private final UserManager mUm;
    private final PackageManager mPackageManager;
    private final IPackageManager mIPackageManager;
    /**
     * Internal collection of device admin info objects for all profiles associated with the current
     * user.
     */
    private final ArrayList<DeviceAdminListItem> mAdmins = new ArrayList<>();
    private final SparseArray<ComponentName> mProfileOwnerComponents = new SparseArray<>();

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Refresh the list, if state change has been received. It could be that checkboxes
            // need to be updated
            if (TextUtils.equals(ACTION_DEVICE_POLICY_MANAGER_STATE_CHANGED, intent.getAction())) {
                updateList();
            }
        }
    };

    private PreferenceGroup mPreferenceGroup;
    private FooterPreferenceMixinCompat mFooterPreferenceMixin;

    static {
        FILTER.addAction(ACTION_DEVICE_POLICY_MANAGER_STATE_CHANGED);
    }

    public DeviceAdminListPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mDPM = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mUm = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mPackageManager = mContext.getPackageManager();
        mIPackageManager = AppGlobals.getPackageManager();
    }

    public DeviceAdminListPreferenceController setFooterPreferenceMixin(
            FooterPreferenceMixinCompat mixin) {
        mFooterPreferenceMixin = mixin;
        return this;
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreferenceGroup = screen.findPreference(getPreferenceKey());
    }

    @Override
    public void onStart() {
        mContext.registerReceiverAsUser(
                mBroadcastReceiver, UserHandle.ALL, FILTER,
                null /* broadcastPermission */, null /* scheduler */);
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        mProfileOwnerComponents.clear();
        final List<UserHandle> profiles = mUm.getUserProfiles();
        final int profilesSize = profiles.size();
        for (int i = 0; i < profilesSize; ++i) {
            final int profileId = profiles.get(i).getIdentifier();
            mProfileOwnerComponents.put(profileId, mDPM.getProfileOwnerAsUser(profileId));
        }
        updateList();
    }

    @Override
    public void onStop() {
        mContext.unregisterReceiver(mBroadcastReceiver);
    }

    @VisibleForTesting
    void updateList() {
        refreshData();
        refreshUI();
    }

    private void refreshData() {
        mAdmins.clear();
        final List<UserHandle> profiles = mUm.getUserProfiles();
        for (UserHandle profile : profiles) {
            final int profileId = profile.getIdentifier();
            updateAvailableAdminsForProfile(profileId);
        }
        Collections.sort(mAdmins);
    }

    private void refreshUI() {
        if (mPreferenceGroup == null) {
            return;
        }
        if (mFooterPreferenceMixin != null) {
            final FooterPreference footer = mFooterPreferenceMixin.createFooterPreference();
            footer.setTitle(R.string.no_device_admins);
            footer.setVisible(mAdmins.isEmpty());
        }
        final Map<String, FilterTouchesSwitchPreference> preferenceCache = new ArrayMap<>();
        final Context prefContext = mPreferenceGroup.getContext();
        final int childrenCount = mPreferenceGroup.getPreferenceCount();
        for (int i = 0; i < childrenCount; i++) {
            final Preference pref = mPreferenceGroup.getPreference(i);
            if (!(pref instanceof FilterTouchesSwitchPreference)) {
                continue;
            }
            final FilterTouchesSwitchPreference appSwitch = (FilterTouchesSwitchPreference) pref;
            preferenceCache.put(appSwitch.getKey(), appSwitch);
        }
        for (DeviceAdminListItem item : mAdmins) {
            final String key = item.getKey();
            FilterTouchesSwitchPreference pref = preferenceCache.remove(key);
            if (pref == null) {
                pref = new FilterTouchesSwitchPreference(prefContext);
                mPreferenceGroup.addPreference(pref);
            }
            bindPreference(item, pref);
        }
        for (FilterTouchesSwitchPreference unusedCacheItem : preferenceCache.values()) {
            mPreferenceGroup.removePreference(unusedCacheItem);
        }
    }

    private void bindPreference(DeviceAdminListItem item, FilterTouchesSwitchPreference pref) {
        pref.setKey(item.getKey());
        pref.setTitle(item.getName());
        pref.setIcon(item.getIcon());
        pref.setChecked(item.isActive());
        pref.setSummary(item.getDescription());
        pref.setEnabled(item.isEnabled());
        pref.setOnPreferenceClickListener(preference -> {
            final UserHandle user = item.getUser();
            mContext.startActivityAsUser(item.getLaunchIntent(mContext), user);
            return true;
        });
        pref.setOnPreferenceChangeListener((preference, newValue) -> false);
    }

    /**
     * Add device admins to the internal collection that belong to a profile.
     *
     * @param profileId the profile identifier.
     */
    private void updateAvailableAdminsForProfile(final int profileId) {
        // We are adding the union of two sets 'A' and 'B' of device admins to mAvailableAdmins.
        // - Set 'A' is the set of active admins for the profile
        // - set 'B' is the set of listeners to DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED for
        //   the profile.

        // Add all of set 'A' to mAvailableAdmins.
        final List<ComponentName> activeAdminsForProfile = mDPM.getActiveAdminsAsUser(profileId);
        addActiveAdminsForProfile(activeAdminsForProfile, profileId);

        // Collect set 'B' and add B-A to mAvailableAdmins.
        addDeviceAdminBroadcastReceiversForProfile(activeAdminsForProfile, profileId);
    }

    /**
     * Add a {@link DeviceAdminInfo} object to the internal collection of available admins for all
     * active admin components associated with a profile.
     */
    private void addActiveAdminsForProfile(List<ComponentName> activeAdmins, int profileId) {
        if (activeAdmins == null) {
            return;
        }

        for (ComponentName activeAdmin : activeAdmins) {
            final ActivityInfo ai;
            try {
                ai = mIPackageManager.getReceiverInfo(activeAdmin,
                        PackageManager.GET_META_DATA |
                                PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS |
                                PackageManager.MATCH_DIRECT_BOOT_UNAWARE |
                                PackageManager.MATCH_DIRECT_BOOT_AWARE, profileId);
            } catch (RemoteException e) {
                Log.w(TAG, "Unable to load component: " + activeAdmin);
                continue;
            }
            final DeviceAdminInfo deviceAdminInfo = createDeviceAdminInfo(mContext, ai);
            if (deviceAdminInfo == null) {
                continue;
            }
            mAdmins.add(new DeviceAdminListItem(mContext, deviceAdminInfo));
        }
    }

    /**
     * Add a profile's device admins that are receivers of
     * {@code DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED} to the internal collection if they
     * haven't been added yet.
     *
     * @param alreadyAddedComponents the set of active admin component names. Receivers of
     *                               {@code DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED}
     *                               whose component is in this
     *                               set are not added to the internal collection again.
     * @param profileId              the identifier of the profile
     */
    private void addDeviceAdminBroadcastReceiversForProfile(
            Collection<ComponentName> alreadyAddedComponents, int profileId) {
        final List<ResolveInfo> enabledForProfile = mPackageManager.queryBroadcastReceiversAsUser(
                new Intent(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED),
                PackageManager.GET_META_DATA | PackageManager.MATCH_DISABLED_UNTIL_USED_COMPONENTS,
                profileId);
        if (enabledForProfile == null) {
            return;
        }
        for (ResolveInfo resolveInfo : enabledForProfile) {
            final ComponentName riComponentName =
                    new ComponentName(resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name);
            if (alreadyAddedComponents != null
                    && alreadyAddedComponents.contains(riComponentName)) {
                continue;
            }
            DeviceAdminInfo deviceAdminInfo = createDeviceAdminInfo(
                    mContext, resolveInfo.activityInfo);
            // add only visible ones (note: active admins are added regardless of visibility)
            if (deviceAdminInfo != null && deviceAdminInfo.isVisible()) {
                if (!deviceAdminInfo.getActivityInfo().applicationInfo.isInternal()) {
                    continue;
                }
                mAdmins.add(new DeviceAdminListItem(mContext, deviceAdminInfo));
            }
        }
    }

    /**
     * Creates a device admin info object for the resolved intent that points to the component of
     * the device admin.
     *
     * @param ai ActivityInfo for the admin component.
     * @return new {@link DeviceAdminInfo} object or null if there was an error.
     */
    private static DeviceAdminInfo createDeviceAdminInfo(Context context, ActivityInfo ai) {
        try {
            return new DeviceAdminInfo(context, ai);
        } catch (XmlPullParserException | IOException e) {
            Log.w(TAG, "Skipping " + ai, e);
        }
        return null;
    }
}
