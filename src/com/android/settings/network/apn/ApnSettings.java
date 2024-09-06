/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings.network.apn;

import static com.android.settings.network.apn.ApnEditPageProviderKt.INSERT_URL;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Telephony;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.data.ApnSetting;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.flags.Flags;
import com.android.settings.network.telephony.SubscriptionRepository;
import com.android.settings.spa.SpaActivity;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import kotlin.Unit;

import java.util.ArrayList;

/** Handle each different apn setting. */
public class ApnSettings extends RestrictedSettingsFragment
        implements Preference.OnPreferenceChangeListener {
    static final String TAG = "ApnSettings";

    public static final String APN_ID = "apn_id";
    public static final String APN_LIST = "apn_list";
    public static final String SUB_ID = "sub_id";
    public static final String MVNO_TYPE = "mvno_type";
    public static final String MVNO_MATCH_DATA = "mvno_match_data";

    private static final String[] CARRIERS_PROJECTION = new String[] {
            Telephony.Carriers._ID,
            Telephony.Carriers.NAME,
            Telephony.Carriers.APN,
            Telephony.Carriers.TYPE,
            Telephony.Carriers.MVNO_TYPE,
            Telephony.Carriers.MVNO_MATCH_DATA,
            Telephony.Carriers.EDITED_STATUS,
    };

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int TYPES_INDEX = 3;
    private static final int MVNO_TYPE_INDEX = 4;
    private static final int MVNO_MATCH_DATA_INDEX = 5;
    private static final int EDITED_INDEX = 6;

    private static final int MENU_NEW = Menu.FIRST;
    private static final int MENU_RESTORE = Menu.FIRST + 1;

    private static final int DIALOG_RESTORE_DEFAULTAPN = 1001;

    private boolean mRestoreDefaultApnMode;

    private UserManager mUserManager;
    private int mSubId;
    private PreferredApnRepository mPreferredApnRepository;
    @Nullable
    private String mPreferredApnKey;
    private String mMvnoType;
    private String mMvnoMatchData;

    private boolean mUnavailable;

    private boolean mHideImsApn;
    private boolean mAllowAddingApns;
    private boolean mHidePresetApnDetails;

    public ApnSettings() {
        super(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS);
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.APN;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Activity activity = getActivity();
        mSubId = activity.getIntent().getIntExtra(SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        mPreferredApnRepository = new PreferredApnRepository(activity, mSubId);

        setIfOnlyAvailableForAdmins(true);

        final CarrierConfigManager configManager = (CarrierConfigManager)
                getSystemService(Context.CARRIER_CONFIG_SERVICE);
        final PersistableBundle b = configManager.getConfigForSubId(mSubId);
        mHideImsApn = b.getBoolean(CarrierConfigManager.KEY_HIDE_IMS_APN_BOOL);
        mAllowAddingApns = b.getBoolean(CarrierConfigManager.KEY_ALLOW_ADDING_APNS_BOOL);
        if (mAllowAddingApns) {
            final String[] readOnlyApnTypes = b.getStringArray(
                    CarrierConfigManager.KEY_READ_ONLY_APN_TYPES_STRING_ARRAY);
            // if no apn type can be edited, do not allow adding APNs
            if (ApnEditor.hasAllApns(readOnlyApnTypes)) {
                Log.d(TAG, "not allowing adding APN because all APN types are read only");
                mAllowAddingApns = false;
            }
        }
        mHidePresetApnDetails = b.getBoolean(CarrierConfigManager.KEY_HIDE_PRESET_APN_DETAILS_BOOL);
        mUserManager = UserManager.get(activity);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getEmptyTextView().setText(com.android.settingslib.R.string.apn_settings_not_available);
        mUnavailable = isUiRestricted();
        setHasOptionsMenu(!mUnavailable);
        if (mUnavailable) {
            addPreferencesFromResource(R.xml.placeholder_prefs);
            return;
        }

        addPreferencesFromResource(R.xml.apn_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LifecycleOwner viewLifecycleOwner = getViewLifecycleOwner();
        new SubscriptionRepository(requireContext())
                .collectSubscriptionEnabled(mSubId, viewLifecycleOwner, (isEnabled) -> {
                    if (!isEnabled) {
                        Log.d(TAG, "Due to subscription not enabled, closes APN settings page");
                        finish();
                    }
                    return Unit.INSTANCE;
                });

        mPreferredApnRepository.collectPreferredApn(viewLifecycleOwner, (preferredApn) -> {
            mPreferredApnKey = preferredApn;
            final PreferenceGroup apnPreferenceList = findPreference(APN_LIST);
            for (int i = 0; i < apnPreferenceList.getPreferenceCount(); i++) {
                ApnPreference apnPreference = (ApnPreference) apnPreferenceList.getPreference(i);
                apnPreference.setIsChecked(apnPreference.getKey().equals(preferredApn));
            }
            return Unit.INSTANCE;
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mUnavailable) {
            return;
        }

        if (!mRestoreDefaultApnMode) {
            fillList();
        }
    }

    @Override
    public EnforcedAdmin getRestrictionEnforcedAdmin() {
        final UserHandle user = UserHandle.of(mUserManager.getProcessUserId());
        if (mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS, user)
                && !mUserManager.hasBaseUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,
                        user)) {
            return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
        }
        return null;
    }

    private void fillList() {
        final Uri simApnUri = Uri.withAppendedPath(Telephony.Carriers.SIM_APN_URI,
                String.valueOf(mSubId));
        final StringBuilder where =
                new StringBuilder("NOT (type='ia' AND (apn=\"\" OR apn IS NULL)) AND "
                + "user_visible!=0");
        // Remove Emergency type, users should not mess with that
        where.append(" AND NOT (type='emergency')");

        if (mHideImsApn) {
            where.append(" AND NOT (type='ims')");
        }

        final Cursor cursor = getContentResolver().query(simApnUri,
                CARRIERS_PROJECTION, where.toString(), null,
                Telephony.Carriers.DEFAULT_SORT_ORDER);

        if (cursor != null) {
            final PreferenceGroup apnPrefList = findPreference(APN_LIST);
            apnPrefList.removeAll();

            final ArrayList<ApnPreference> apnList = new ArrayList<ApnPreference>();
            final ArrayList<ApnPreference> mmsApnList = new ArrayList<ApnPreference>();

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                final String name = cursor.getString(NAME_INDEX);
                final String apn = cursor.getString(APN_INDEX);
                final String key = cursor.getString(ID_INDEX);
                final String type = cursor.getString(TYPES_INDEX);
                final int edited = cursor.getInt(EDITED_INDEX);
                mMvnoType = cursor.getString(MVNO_TYPE_INDEX);
                mMvnoMatchData = cursor.getString(MVNO_MATCH_DATA_INDEX);

                final ApnPreference pref = new ApnPreference(getPrefContext());

                pref.setKey(key);
                pref.setTitle(name);
                pref.setPersistent(false);
                pref.setOnPreferenceChangeListener(this);
                pref.setSubId(mSubId);
                if (mHidePresetApnDetails && edited == Telephony.Carriers.UNEDITED) {
                    pref.setHideDetails();
                } else {
                    pref.setSummary(apn);
                }

                final boolean defaultSelectable =
                        ((type == null) || type.contains(ApnSetting.TYPE_DEFAULT_STRING));
                pref.setDefaultSelectable(defaultSelectable);
                if (defaultSelectable) {
                    pref.setIsChecked(key.equals(mPreferredApnKey));
                    apnList.add(pref);
                } else {
                    mmsApnList.add(pref);
                }
                cursor.moveToNext();
            }
            cursor.close();

            for (Preference preference : apnList) {
                apnPrefList.addPreference(preference);
            }
            for (Preference preference : mmsApnList) {
                apnPrefList.addPreference(preference);
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!mUnavailable) {
            if (mAllowAddingApns) {
                menu.add(0, MENU_NEW, 0,
                        getResources().getString(R.string.menu_new))
                        .setIcon(R.drawable.ic_add_24dp)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            menu.add(0, MENU_RESTORE, 0,
                    getResources().getString(R.string.menu_restore))
                    .setIcon(android.R.drawable.ic_menu_upload);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_NEW:
                addNewApn();
                return true;
            case MENU_RESTORE:
                restoreDefaultApn();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addNewApn() {
        if (Flags.newApnPageEnabled()) {
            String route = ApnEditPageProvider.INSTANCE.getRoute(
                    INSERT_URL, Telephony.Carriers.CONTENT_URI, mSubId);
            SpaActivity.startSpaActivity(getContext(), route);
        } else {
            final Intent intent = new Intent(Intent.ACTION_INSERT, Telephony.Carriers.CONTENT_URI);
            intent.putExtra(SUB_ID, mSubId);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (!TextUtils.isEmpty(mMvnoType) && !TextUtils.isEmpty(mMvnoMatchData)) {
                intent.putExtra(MVNO_TYPE, mMvnoType);
                intent.putExtra(MVNO_MATCH_DATA, mMvnoMatchData);
            }
            startActivity(intent);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(): Preference - " + preference
                + ", newValue - " + newValue + ", newValue type - "
                + newValue.getClass());
        if (newValue instanceof String) {
            mPreferredApnRepository.setPreferredApn((String) newValue);
        }

        return true;
    }

    private void restoreDefaultApn() {
        showDialog(DIALOG_RESTORE_DEFAULTAPN);
        mRestoreDefaultApnMode = true;

        mPreferredApnRepository.restorePreferredApn(getViewLifecycleOwner(), () -> {
            onPreferredApnRestored();
            return Unit.INSTANCE;
        });
    }

    private void onPreferredApnRestored() {
        final Activity activity = getActivity();
        if (activity == null) {
            mRestoreDefaultApnMode = false;
            return;
        }
        fillList();
        getPreferenceScreen().setEnabled(true);
        mRestoreDefaultApnMode = false;
        removeDialog(DIALOG_RESTORE_DEFAULTAPN);
        Toast.makeText(
                activity,
                getResources().getString(R.string.restore_default_apn_completed),
                Toast.LENGTH_LONG).show();
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_RESTORE_DEFAULTAPN) {
            final ProgressDialog dialog = new ProgressDialog(getActivity()) {
                public boolean onTouchEvent(MotionEvent event) {
                    return true;
                }
            };
            dialog.setMessage(getResources().getString(R.string.restore_default_apn));
            dialog.setCancelable(false);
            return dialog;
        }
        return null;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (dialogId == DIALOG_RESTORE_DEFAULTAPN) {
            return SettingsEnums.DIALOG_APN_RESTORE_DEFAULT;
        }
        return 0;
    }
}
