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
import android.service.notification.NotificationListenerFilter;

import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.notification.NotificationBackend;

import java.util.HashSet;
import java.util.Set;

public class TypeFilterPreferenceController extends BasePreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener {

    private static final String TAG = "TypeFilterPrefCntlr";

    private ComponentName mCn;
    private int mUserId;
    private NotificationBackend mNm;
    private NotificationListenerFilter mNlf;

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

    @Override
    public int getAvailabilityStatus() {
        if (mNm.isNotificationListenerAccessGranted(mCn)) {
            return AVAILABLE;
        } else {
            return DISABLED_DEPENDENT_SETTING;
        }
    }

    @Override
    public void updateState(Preference pref) {
        mNlf = mNm.getListenerFilter(mCn, mUserId);
        Set<String> values = new HashSet<>();
        Set<String> entries = new HashSet<>();

        if (hasFlag(mNlf.getTypes(), FLAG_FILTER_TYPE_ONGOING)) {
            values.add(String.valueOf(FLAG_FILTER_TYPE_ONGOING));
            entries.add(mContext.getString(R.string.notif_type_ongoing));
        }
        if (hasFlag(mNlf.getTypes(), FLAG_FILTER_TYPE_CONVERSATIONS)) {
            values.add(String.valueOf(FLAG_FILTER_TYPE_CONVERSATIONS));
            entries.add(mContext.getString(R.string.notif_type_conversation));
        }
        if (hasFlag(mNlf.getTypes(), FLAG_FILTER_TYPE_ALERTING)) {
            values.add(String.valueOf(FLAG_FILTER_TYPE_ALERTING));
            entries.add(mContext.getString(R.string.notif_type_alerting));
        }
        if (hasFlag(mNlf.getTypes(), FLAG_FILTER_TYPE_SILENT)) {
            values.add(String.valueOf(FLAG_FILTER_TYPE_SILENT));
            entries.add(mContext.getString(R.string.notif_type_silent));
        }

        final MultiSelectListPreference preference = (MultiSelectListPreference) pref;
        preference.setValues(values);
        super.updateState(preference);
        pref.setEnabled(getAvailabilityStatus() == AVAILABLE);
    }

    private boolean hasFlag(int value, int flag) {
        return (value & flag) != 0;
    }

    public CharSequence getSummary() {
        Set<String> entries = new HashSet<>();
        if (hasFlag(mNlf.getTypes(), FLAG_FILTER_TYPE_ONGOING)) {
            entries.add(mContext.getString(R.string.notif_type_ongoing));
        }
        if (hasFlag(mNlf.getTypes(), FLAG_FILTER_TYPE_CONVERSATIONS)) {
            entries.add(mContext.getString(R.string.notif_type_conversation));
        }
        if (hasFlag(mNlf.getTypes(), FLAG_FILTER_TYPE_ALERTING)) {
            entries.add(mContext.getString(R.string.notif_type_alerting));
        }
        if (hasFlag(mNlf.getTypes(), FLAG_FILTER_TYPE_SILENT)) {
            entries.add(mContext.getString(R.string.notif_type_silent));
        }
        return String.join(System.lineSeparator(), entries);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        // retrieve latest in case the package filter has changed
        mNlf = mNm.getListenerFilter(mCn, mUserId);

        Set<String> set = (Set<String>) newValue;

        int newFilter = 0;
        for (String filterType : set) {
            newFilter |= Integer.parseInt(filterType);
        }
        mNlf.setTypes(newFilter);
        preference.setSummary(getSummary());
        mNm.setListenerFilter(mCn, mUserId, mNlf);
        return true;
    }

}