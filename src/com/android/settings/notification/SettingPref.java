/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.preference.Preference;
import android.preference.TwoStatePreference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings.Global;
import android.provider.Settings.System;

import com.android.settings.SettingsPreferenceFragment;

/** Helper to manage a two-state or dropdown preference bound to a global or system setting. */
public class SettingPref {
    public static final int TYPE_GLOBAL = 1;
    public static final int TYPE_SYSTEM = 2;

    protected final int mType;
    private final String mKey;
    protected final String mSetting;
    protected final int mDefault;
    private final int[] mValues;
    private final Uri mUri;

    protected TwoStatePreference mTwoState;
    protected DropDownPreference mDropDown;

    public SettingPref(int type, String key, String setting, int def, int... values) {
        mType = type;
        mKey = key;
        mSetting = setting;
        mDefault = def;
        mValues = values;
        mUri = getUriFor(mType, mSetting);
    }

    public boolean isApplicable(Context context) {
        return true;
    }

    protected String getCaption(Resources res, int value) {
        throw new UnsupportedOperationException();
    }

    public Preference init(SettingsPreferenceFragment settings) {
        final Context context = settings.getActivity();
        Preference p = settings.getPreferenceScreen().findPreference(mKey);
        if (p != null && !isApplicable(context)) {
            settings.getPreferenceScreen().removePreference(p);
            p = null;
        }
        if (p instanceof TwoStatePreference) {
            mTwoState = (TwoStatePreference) p;
        } else if (p instanceof DropDownPreference) {
            mDropDown = (DropDownPreference) p;
            for (int value : mValues) {
                mDropDown.addItem(getCaption(context.getResources(), value), value);
            }
        }
        update(context);
        if (mTwoState != null) {
            p.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    setSetting(context, (Boolean) newValue ? 1 : 0);
                    return true;
                }
            });
            return mTwoState;
        }
        if (mDropDown != null) {
            mDropDown.setCallback(new DropDownPreference.Callback() {
                @Override
                public boolean onItemSelected(int pos, Object value) {
                    return setSetting(context, (Integer) value);
                }
            });
            return mDropDown;
        }
        return null;
    }

    protected boolean setSetting(Context context, int value) {
        return putInt(mType, context.getContentResolver(), mSetting, value);
    }

    public Uri getUri() {
        return mUri;
    }

    public String getKey() {
        return mKey;
    }

    public void update(Context context) {
        final int val = getInt(mType, context.getContentResolver(), mSetting, mDefault);
        if (mTwoState != null) {
            mTwoState.setChecked(val != 0);
        } else if (mDropDown != null) {
            mDropDown.setSelectedValue(val);
        }
    }

    private static Uri getUriFor(int type, String setting) {
        switch(type) {
            case TYPE_GLOBAL:
                return Global.getUriFor(setting);
            case TYPE_SYSTEM:
                return System.getUriFor(setting);
        }
        throw new IllegalArgumentException();
    }

    protected static boolean putInt(int type, ContentResolver cr, String setting, int value) {
        switch(type) {
            case TYPE_GLOBAL:
                return Global.putInt(cr, setting, value);
            case TYPE_SYSTEM:
                return System.putInt(cr, setting, value);
        }
        throw new IllegalArgumentException();
    }

    protected static int getInt(int type, ContentResolver cr, String setting, int def) {
        switch(type) {
            case TYPE_GLOBAL:
                return Global.getInt(cr, setting, def);
            case TYPE_SYSTEM:
                return System.getInt(cr, setting, def);
        }
        throw new IllegalArgumentException();
    }
}