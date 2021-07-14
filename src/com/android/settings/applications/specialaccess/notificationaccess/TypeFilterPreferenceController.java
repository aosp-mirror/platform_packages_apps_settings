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

package com.android.settings.applications.specialaccess.notificationaccess;

import static android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_ALERTING;
import static android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_CONVERSATIONS;
import static android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_ONGOING;
import static android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_SILENT;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.service.notification.NotificationListenerFilter;
import android.service.notification.NotificationListenerService;
import android.text.TextUtils;

import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;

public abstract class TypeFilterPreferenceController extends BasePreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "TypeFilterPrefCntlr";
    private static final String FLAG_SEPARATOR = "\\|";

    private ComponentName mCn;
    private int mUserId;
    private NotificationBackend mNm;
    private NotificationListenerFilter mNlf;
    private ServiceInfo mSi;
    private int mTargetSdk;

    public TypeFilterPreferenceController(Context context, String key) {
        super(context, key);
    }

    public TypeFilterPreferenceController setCn(ComponentName cn) {
        mCn = cn;
        return this;
    }

    public TypeFilterPreferenceController setUserId(int userId) {
        mUserId = userId;
        return this;
    }

    public TypeFilterPreferenceController setNm(NotificationBackend nm) {
        mNm = nm;
        return this;
    }

    public TypeFilterPreferenceController setServiceInfo(ServiceInfo si) {
        mSi = si;
        return this;
    }

    public TypeFilterPreferenceController setTargetSdk(int targetSdk) {
        mTargetSdk = targetSdk;
        return this;
    }

    abstract protected int getType();

    @Override
    public int getAvailabilityStatus() {
        if (mNm.isNotificationListenerAccessGranted(mCn)) {
            if (mTargetSdk > Build.VERSION_CODES.S) {
                return AVAILABLE;
            }

            mNlf = mNm.getListenerFilter(mCn, mUserId);
            if (!mNlf.areAllTypesAllowed() || !mNlf.getDisallowedPackages().isEmpty()) {
                return AVAILABLE;
            }
        }
        return DISABLED_DEPENDENT_SETTING;
    }

    private boolean hasFlag(int value, int flag) {
        return (value & flag) != 0;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // retrieve latest in case the package filter has changed
        mNlf = mNm.getListenerFilter(mCn, mUserId);

        boolean enabled = (boolean) newValue;

        int newFilter = mNlf.getTypes();
        if (enabled) {
            newFilter |= getType();
        } else {
            newFilter &= ~getType();
        }
        mNlf.setTypes(newFilter);
        mNm.setListenerFilter(mCn, mUserId, mNlf);
        return true;
    }

    @Override
    public void updateState(Preference pref) {
        mNlf = mNm.getListenerFilter(mCn, mUserId);

        CheckBoxPreference check = (CheckBoxPreference) pref;
        check.setChecked(hasFlag(mNlf.getTypes(), getType()));

        boolean disableRequestedByApp = false;
        if (mSi != null) {
            if (mSi.metaData != null && mSi.metaData.containsKey(
                    NotificationListenerService.META_DATA_DISABLED_FILTER_TYPES)) {
                String typeList = mSi.metaData.get(
                        NotificationListenerService.META_DATA_DISABLED_FILTER_TYPES).toString();
                if (typeList != null) {
                    int types = 0;
                    String[] typeStrings = typeList.split(FLAG_SEPARATOR);
                    for (int i = 0; i < typeStrings.length; i++) {
                        final String typeString = typeStrings[i];
                        if (TextUtils.isEmpty(typeString)) {
                            continue;
                        }
                        if (typeString.equalsIgnoreCase("ONGOING")) {
                            types |= FLAG_FILTER_TYPE_ONGOING;
                        } else if (typeString.equalsIgnoreCase("CONVERSATIONS")) {
                            types |= FLAG_FILTER_TYPE_CONVERSATIONS;
                        } else if (typeString.equalsIgnoreCase("SILENT")) {
                            types |= FLAG_FILTER_TYPE_SILENT;
                        } else if (typeString.equalsIgnoreCase("ALERTING")) {
                            types |= FLAG_FILTER_TYPE_ALERTING;
                        } else {
                            try {
                                types |= Integer.parseInt(typeString);
                            } catch (NumberFormatException e) {
                                // skip
                            }
                        }
                    }
                    if (hasFlag(types, getType())) {
                        disableRequestedByApp = true;
                    }
                }
            }
        }
        // Apps can prevent a category from being turned on, but not turned off
        boolean disabledByApp = disableRequestedByApp && !check.isChecked();
        pref.setEnabled(getAvailabilityStatus() == AVAILABLE && !disabledByApp);
    }
}