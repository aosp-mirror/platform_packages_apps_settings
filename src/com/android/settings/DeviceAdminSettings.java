/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppGlobals;
import android.app.ListFragment;
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
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DeviceAdminSettings extends ListFragment {
    static final String TAG = "DeviceAdminSettings";

    private DevicePolicyManager mDPM;
    private UserManager mUm;


    private static class DeviceAdminListItem implements Comparable<DeviceAdminListItem> {
        public DeviceAdminInfo info;

        // These aren't updated so they keep a stable sort order if user activates / de-activates
        // an admin.
        public String name;
        public boolean active;

        public int compareTo(DeviceAdminListItem other)  {
            // Sort active admins first, then by name.
            if (this.active != other.active) {
                return this.active ? -1 : 1;
            }
            return this.name.compareTo(other.name);
        }
    }
    /**
     * Internal collection of device admin info objects for all profiles associated with the current
     * user.
     */
    private final ArrayList<DeviceAdminListItem>
            mAdmins = new ArrayList<DeviceAdminListItem>();

    private String mDeviceOwnerPkg;
    private SparseArray<ComponentName> mProfileOwnerComponents = new SparseArray<ComponentName>();

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Refresh the list, if state change has been received. It could be that checkboxes
            // need to be updated
            if (DevicePolicyManager.ACTION_DEVICE_POLICY_MANAGER_STATE_CHANGED.equals(
                    intent.getAction())) {
                updateList();
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mDPM = (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        mUm = (UserManager) getActivity().getSystemService(Context.USER_SERVICE);
        return inflater.inflate(R.layout.device_admin_settings, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        Utils.forceCustomPadding(getListView(), true /* additive padding */);
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DevicePolicyManager.ACTION_DEVICE_POLICY_MANAGER_STATE_CHANGED);
        getActivity().registerReceiverAsUser(
                mBroadcastReceiver, UserHandle.ALL, filter, null, null);

        final ComponentName deviceOwnerComponent = mDPM.getDeviceOwnerComponentOnAnyUser();
        mDeviceOwnerPkg =
                deviceOwnerComponent != null ? deviceOwnerComponent.getPackageName() : null;
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
    public void onPause() {
        getActivity().unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    /**
     * Update the internal collection of available admins for all profiles associated with the
     * current user.
     */
    void updateList() {
        mAdmins.clear();

        final List<UserHandle> profiles = mUm.getUserProfiles();
        final int profilesSize = profiles.size();
        for (int i = 0; i < profilesSize; ++i) {
            final int profileId = profiles.get(i).getIdentifier();
            updateAvailableAdminsForProfile(profileId);
        }
        Collections.sort(mAdmins);

        getListView().setAdapter(new PolicyListAdapter());
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Object o = l.getAdapter().getItem(position);
        DeviceAdminInfo dpi = (DeviceAdminInfo) o;
        final UserHandle user = new UserHandle(getUserId(dpi));
        final Activity activity = getActivity();
        Intent intent = new Intent(activity, DeviceAdminAdd.class);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, dpi.getComponent());
        activity.startActivityAsUser(intent, user);
    }

    static class ViewHolder {
        ImageView icon;
        TextView name;
        CheckBox checkbox;
        TextView description;
    }

    class PolicyListAdapter extends BaseAdapter {
        final LayoutInflater mInflater;

        PolicyListAdapter() {
            mInflater = (LayoutInflater)
                    getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public int getCount() {
            return mAdmins.size();
        }

        /**
         * The item for the given position in the list.
         *
         * @return DeviceAdminInfo object for actual device admins.
         */
        @Override
        public Object getItem(int position) {
            return ((DeviceAdminListItem) (mAdmins.get(position))).info;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        /**
         * See {@link #getItemViewType} for the view types.
         */
        @Override
        public int getViewTypeCount() {
            return 1;
        }

        /**
         * Returns 0 for all types.
         */
        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public boolean isEnabled(int position) {
            Object o = getItem(position);
            return isEnabled(o);
        }

        private boolean isEnabled(Object o) {
            DeviceAdminInfo info = (DeviceAdminInfo) o;
            // Disable item if admin is being removed
            if (isRemovingAdmin(info)) {
                return false;
            }
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Object o = getItem(position);
            if (convertView == null) {
                convertView = newDeviceAdminView(parent);
            }
            bindView(convertView, (DeviceAdminInfo) o);
            return convertView;
        }

        private View newDeviceAdminView(ViewGroup parent) {
            View v = mInflater.inflate(R.layout.device_admin_item, parent, false);
            ViewHolder h = new ViewHolder();
            h.icon = (ImageView) v.findViewById(R.id.icon);
            h.name = (TextView) v.findViewById(R.id.name);
            h.checkbox = (CheckBox) v.findViewById(R.id.checkbox);
            h.description = (TextView) v.findViewById(R.id.description);
            v.setTag(h);
            return v;
        }

        private void bindView(View view, DeviceAdminInfo item) {
            final Activity activity = getActivity();
            ViewHolder vh = (ViewHolder) view.getTag();
            Drawable activityIcon = item.loadIcon(activity.getPackageManager());
            Drawable badgedIcon = activity.getPackageManager().getUserBadgedIcon(
                    activityIcon, new UserHandle(getUserId(item)));
            vh.icon.setImageDrawable(badgedIcon);
            vh.name.setText(item.loadLabel(activity.getPackageManager()));
            vh.checkbox.setChecked(isActiveAdmin(item));
            final boolean enabled = isEnabled(item);
            try {
                vh.description.setText(item.loadDescription(activity.getPackageManager()));
            } catch (Resources.NotFoundException e) {
            }
            vh.checkbox.setEnabled(enabled);
            vh.name.setEnabled(enabled);
            vh.description.setEnabled(enabled);
            vh.icon.setEnabled(enabled);
        }
    }

    private boolean isDeviceOwner(DeviceAdminInfo item) {
        return getUserId(item) == UserHandle.myUserId()
                && item.getPackageName().equals(mDeviceOwnerPkg);
    }

    private boolean isProfileOwner(DeviceAdminInfo item) {
        ComponentName profileOwner = mProfileOwnerComponents.get(getUserId(item));
        return item.getComponent().equals(profileOwner);
    }

    private boolean isActiveAdmin(DeviceAdminInfo item) {
        return mDPM.isAdminActiveAsUser(item.getComponent(), getUserId(item));
    }

    private boolean isRemovingAdmin(DeviceAdminInfo item) {
        return mDPM.isRemovingAdmin(item.getComponent(), getUserId(item));
    }

    /**
     * Add device admins to the internal collection that belong to a profile.
     *
     * @param profileId the profile identifier.
     */
    private void updateAvailableAdminsForProfile(final int profileId) {
        // We are adding the union of two sets 'A' and 'B' of device admins to mAvailableAdmins.
        // Set 'A' is the set of active admins for the profile whereas set 'B' is the set of
        // listeners to DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED for the profile.

        // Add all of set 'A' to mAvailableAdmins.
        List<ComponentName> activeAdminsListForProfile = mDPM.getActiveAdminsAsUser(profileId);
        addActiveAdminsForProfile(activeAdminsListForProfile, profileId);

        // Collect set 'B' and add B-A to mAvailableAdmins.
        addDeviceAdminBroadcastReceiversForProfile(activeAdminsListForProfile, profileId);
    }

    /**
     * Add a profile's device admins that are receivers of
     * {@code DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED} to the internal collection if they
     * haven't been added yet.
     *
     * @param alreadyAddedComponents the set of active admin component names. Receivers of
     *            {@code DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED} whose component is in this
     *            set are not added to the internal collection again.
     * @param profileId the identifier of the profile
     */
    private void addDeviceAdminBroadcastReceiversForProfile(
            Collection<ComponentName> alreadyAddedComponents, final int profileId) {
        final PackageManager pm = getActivity().getPackageManager();
        List<ResolveInfo> enabledForProfile = pm.queryBroadcastReceiversAsUser(
                new Intent(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED),
                PackageManager.GET_META_DATA | PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS,
                profileId);
        if (enabledForProfile == null) {
            enabledForProfile = Collections.emptyList();
        }
        final int n = enabledForProfile.size();
        for (int i = 0; i < n; ++i) {
            ResolveInfo resolveInfo = enabledForProfile.get(i);
            ComponentName riComponentName =
                    new ComponentName(resolveInfo.activityInfo.packageName,
                            resolveInfo.activityInfo.name);
            if (alreadyAddedComponents == null
                    || !alreadyAddedComponents.contains(riComponentName)) {
                DeviceAdminInfo deviceAdminInfo =  createDeviceAdminInfo(resolveInfo.activityInfo);
                // add only visible ones (note: active admins are added regardless of visibility)
                if (deviceAdminInfo != null && deviceAdminInfo.isVisible()) {
                    if (!deviceAdminInfo.getActivityInfo().applicationInfo.isInternal()) {
                        continue;
                    }
                    DeviceAdminListItem item = new DeviceAdminListItem();
                    item.info = deviceAdminInfo;
                    item.name = deviceAdminInfo.loadLabel(pm).toString();
                    // Active ones already added.
                    item.active = false;
                    mAdmins.add(item);
                }
            }
        }
    }

    /**
     * Add a {@link DeviceAdminInfo} object to the internal collection of available admins for all
     * active admin components associated with a profile.
     *
     * @param profileId a profile identifier.
     */
    private void addActiveAdminsForProfile(final List<ComponentName> activeAdmins,
            final int profileId) {
        if (activeAdmins != null) {
            final PackageManager packageManager = getActivity().getPackageManager();
            final IPackageManager iPackageManager = AppGlobals.getPackageManager();
            final int n = activeAdmins.size();
            for (int i = 0; i < n; ++i) {
                final ComponentName activeAdmin = activeAdmins.get(i);
                final ActivityInfo ai;
                try {
                    ai = iPackageManager.getReceiverInfo(activeAdmin,
                            PackageManager.GET_META_DATA |
                            PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS |
                            PackageManager.MATCH_ENCRYPTION_AWARE_AND_UNAWARE, profileId);
                } catch (RemoteException e) {
                    Log.w(TAG, "Unable to load component: " + activeAdmin);
                    continue;
                }
                final DeviceAdminInfo deviceAdminInfo = createDeviceAdminInfo(ai);
                if (deviceAdminInfo == null) {
                    continue;
                }
                // Don't do the applicationInfo.isInternal() check here; if an active
                // admin is already on SD card, just show it.
                final DeviceAdminListItem item = new DeviceAdminListItem();
                item.info = deviceAdminInfo;
                item.name = deviceAdminInfo.loadLabel(packageManager).toString();
                item.active = true;
                mAdmins.add(item);
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
    private DeviceAdminInfo createDeviceAdminInfo(ActivityInfo ai) {
        try {
            return new DeviceAdminInfo(getActivity(), ai);
        } catch (XmlPullParserException|IOException e) {
            Log.w(TAG, "Skipping " + ai, e);
        }
        return null;
    }

    /**
     * Extracts the user id from a device admin info object.
     * @param adminInfo the device administrator info.
     * @return identifier of the user associated with the device admin.
     */
    private int getUserId(DeviceAdminInfo adminInfo) {
        return UserHandle.getUserId(adminInfo.getActivityInfo().applicationInfo.uid);
    }
}
