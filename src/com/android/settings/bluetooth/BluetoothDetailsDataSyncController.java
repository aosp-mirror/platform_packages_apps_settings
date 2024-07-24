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

package com.android.settings.bluetooth;

import android.companion.AssociationInfo;
import android.companion.CompanionDeviceManager;
import android.companion.datatransfer.PermissionSyncRequest;
import android.content.Context;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.bluetooth.CachedBluetoothDevice;
import com.android.settingslib.core.lifecycle.Lifecycle;

import com.google.common.base.Objects;

import java.util.Comparator;

/**
 * The controller of the CDM data sync in the bluetooth detail settings.
 */
public class BluetoothDetailsDataSyncController extends BluetoothDetailsController
        implements Preference.OnPreferenceClickListener {

    private static final int DUMMY_ASSOCIATION_ID = -1;
    private static final String TAG = "BTDataSyncController";
    private static final String KEY_DATA_SYNC_GROUP = "data_sync_group";
    private static final String KEY_PERM_SYNC = "perm_sync";

    @VisibleForTesting
    PreferenceCategory mPreferenceCategory;
    @VisibleForTesting
    int mAssociationId = DUMMY_ASSOCIATION_ID;

    private CachedBluetoothDevice mCachedDevice;
    private CompanionDeviceManager mCompanionDeviceManager;

    public BluetoothDetailsDataSyncController(Context context,
            PreferenceFragmentCompat fragment,
            CachedBluetoothDevice device,
            Lifecycle lifecycle) {
        super(context, fragment, device, lifecycle);
        mCachedDevice = device;
        mCompanionDeviceManager = context.getSystemService(CompanionDeviceManager.class);

        mCompanionDeviceManager.getAllAssociations().stream().filter(
                a -> a.getDeviceMacAddress() != null).filter(
                a -> Objects.equal(mCachedDevice.getAddress(),
                        a.getDeviceMacAddress().toString().toUpperCase())).max(
                Comparator.comparingLong(AssociationInfo::getTimeApprovedMs)).ifPresent(
                a -> mAssociationId = a.getId());
    }

    @Override
    public boolean isAvailable() {
        if (mAssociationId == DUMMY_ASSOCIATION_ID) {
            return false;
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        TwoStatePreference switchPreference = (TwoStatePreference) preference;
        String key = switchPreference.getKey();
        if (key.equals(KEY_PERM_SYNC)) {
            if (switchPreference.isChecked()) {
                mCompanionDeviceManager.enablePermissionsSync(mAssociationId);
            } else {
                mCompanionDeviceManager.disablePermissionsSync(mAssociationId);
            }
        }
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DATA_SYNC_GROUP;
    }

    @Override
    protected void init(PreferenceScreen screen) {
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        refresh();
    }

    @Override
    protected void refresh() {
        TwoStatePreference permSyncPref = mPreferenceCategory.findPreference(KEY_PERM_SYNC);
        if (permSyncPref == null) {
            permSyncPref = createPermSyncPreference(mPreferenceCategory.getContext());
            mPreferenceCategory.addPreference(permSyncPref);
        }

        if (mAssociationId == DUMMY_ASSOCIATION_ID) {
            permSyncPref.setVisible(false);
            return;
        }

        boolean visible = false;
        boolean checked = false;
        PermissionSyncRequest request = mCompanionDeviceManager.getPermissionSyncRequest(
                mAssociationId);
        if (request != null) {
            visible = true;
            if (request.isUserConsented()) {
                checked = true;
            }
        }
        permSyncPref.setVisible(visible);
        permSyncPref.setChecked(checked);
    }

    @VisibleForTesting
    TwoStatePreference createPermSyncPreference(Context context) {
        TwoStatePreference pref = new SwitchPreferenceCompat(context);
        pref.setKey(KEY_PERM_SYNC);
        pref.setTitle(context.getString(R.string.bluetooth_details_permissions_sync_title));
        pref.setSummary(context.getString(R.string.bluetooth_details_permissions_sync_summary));
        pref.setOnPreferenceClickListener(this);
        return pref;
    }
}
