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

package com.android.settings.utils;

import android.annotation.Nullable;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.IconDrawableFactory;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.widget.AppSwitchPreference;
import com.android.settings.widget.EmptyTextSettings;
import com.android.settingslib.applications.ServiceListing;

import java.util.List;

public abstract class ManagedServiceSettings extends EmptyTextSettings {
    private static final String TAG = "ManagedServiceSettings";
    private final Config mConfig;

    protected Context mContext;
    private PackageManager mPm;
    private DevicePolicyManager mDpm;
    private ServiceListing mServiceListing;
    private IconDrawableFactory mIconDrawableFactory;

    abstract protected Config getConfig();

    public ManagedServiceSettings() {
        mConfig = getConfig();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mContext = getActivity();
        mPm = mContext.getPackageManager();
        mDpm = (DevicePolicyManager) mContext.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mIconDrawableFactory = IconDrawableFactory.newInstance(mContext);
        mServiceListing = new ServiceListing.Builder(mContext)
                .setPermission(mConfig.permission)
                .setIntentAction(mConfig.intentAction)
                .setNoun(mConfig.noun)
                .setSetting(mConfig.setting)
                .setTag(mConfig.tag)
                .build();
        mServiceListing.addCallback(this::updateList);
        setPreferenceScreen(getPreferenceManager().createPreferenceScreen(mContext));
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setEmptyText(mConfig.emptyText);
    }

    @Override
    public void onResume() {
        super.onResume();
        mServiceListing.reload();
        mServiceListing.setListening(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        mServiceListing.setListening(false);
    }

    private void updateList(List<ServiceInfo> services) {
        UserManager um = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        final int managedProfileId = Utils.getManagedProfileId(um, UserHandle.myUserId());

        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        services.sort(new PackageItemInfo.DisplayNameComparator(mPm));
        for (ServiceInfo service : services) {
            final ComponentName cn = new ComponentName(service.packageName, service.name);
            CharSequence title = null;
            try {
                title = mPm.getApplicationInfoAsUser(
                        service.packageName, 0, UserHandle.myUserId()).loadLabel(mPm);
            } catch (PackageManager.NameNotFoundException e) {
                // unlikely, as we are iterating over live services.
                Log.e(TAG, "can't find package name", e);
            }
            final CharSequence finalTitle = title;
            final String summary = service.loadLabel(mPm).toString();
            final SwitchPreference pref = new AppSwitchPreference(getPrefContext());
            pref.setPersistent(false);
            pref.setIcon(mIconDrawableFactory.getBadgedIcon(service, service.applicationInfo,
                    UserHandle.getUserId(service.applicationInfo.uid)));
            if (title != null && !title.equals(summary)) {
                pref.setTitle(title);
                pref.setSummary(summary);
            } else {
                pref.setTitle(summary);
            }
            pref.setKey(cn.flattenToString());
            pref.setChecked(isServiceEnabled(cn));
            if (managedProfileId != UserHandle.USER_NULL
                    && !mDpm.isNotificationListenerServicePermitted(
                            service.packageName, managedProfileId)) {
                pref.setSummary(R.string.work_profile_notification_access_blocked_summary);
            }
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                final boolean enable = (boolean) newValue;
                if (finalTitle != null) {
                    return setEnabled(cn, finalTitle.toString(), enable);
                } else {
                    return setEnabled(cn, null, enable);
                }
            });
            pref.setKey(cn.flattenToString());
            screen.addPreference(pref);
        }
        highlightPreferenceIfNeeded();
    }

    private int getCurrentUser(int managedProfileId) {
        if (managedProfileId != UserHandle.USER_NULL) {
            return managedProfileId;
        }
        return UserHandle.myUserId();
    }

    protected boolean isServiceEnabled(ComponentName cn) {
        return mServiceListing.isEnabled(cn);
    }

    protected boolean setEnabled(ComponentName service, String title, boolean enable) {
        if (!enable) {
            // the simple version: disabling
            mServiceListing.setEnabled(service, false);
            return true;
        } else {
            if (mServiceListing.isEnabled(service)) {
                return true; // already enabled
            }
            // show a scary dialog
            new ScaryWarningDialogFragment()
                    .setServiceInfo(service, title, this)
                    .show(getFragmentManager(), "dialog");
            return false;
        }
    }

    protected void enable(ComponentName service) {
        mServiceListing.setEnabled(service, true);
    }

    public static class ScaryWarningDialogFragment extends InstrumentedDialogFragment {
        private static final String KEY_COMPONENT = "c";
        private static final String KEY_LABEL = "l";

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_SERVICE_ACCESS_WARNING;
        }

        public ScaryWarningDialogFragment setServiceInfo(ComponentName cn, String label,
                Fragment target) {
            Bundle args = new Bundle();
            args.putString(KEY_COMPONENT, cn.flattenToString());
            args.putString(KEY_LABEL, label);
            setArguments(args);
            setTargetFragment(target, 0);
            return this;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();
            final String label = args.getString(KEY_LABEL);
            final ComponentName cn = ComponentName.unflattenFromString(args
                    .getString(KEY_COMPONENT));
            ManagedServiceSettings parent = (ManagedServiceSettings) getTargetFragment();

            final String title = getResources().getString(parent.mConfig.warningDialogTitle, label);
            final String summary = getResources().getString(parent.mConfig.warningDialogSummary,
                    label);
            return new AlertDialog.Builder(getContext())
                    .setMessage(summary)
                    .setTitle(title)
                    .setCancelable(true)
                    .setPositiveButton(R.string.allow,
                            (dialog, id) -> parent.enable(cn))
                    .setNegativeButton(R.string.deny,
                            (dialog, id) -> {
                                // pass
                            })
                    .create();
        }
    }

    public static class Config {
        public final String tag;
        public final String setting;
        public final String intentAction;
        public final String permission;
        public final String noun;
        public final int warningDialogTitle;
        public final int warningDialogSummary;
        public final int emptyText;
        public final String configIntentAction;

        private Config(String tag, String setting, String intentAction, String configIntentAction,
                String permission, String noun, int warningDialogTitle, int warningDialogSummary,
                int emptyText) {
            this.tag = tag;
            this.setting = setting;
            this.intentAction = intentAction;
            this.permission = permission;
            this.noun = noun;
            this.warningDialogTitle = warningDialogTitle;
            this.warningDialogSummary = warningDialogSummary;
            this.emptyText = emptyText;
            this.configIntentAction = configIntentAction;
        }

        public static class Builder{
            private String mTag;
            private String mSetting;
            private String mIntentAction;
            private String mPermission;
            private String mNoun;
            private int mWarningDialogTitle;
            private int mWarningDialogSummary;
            private int mEmptyText;
            private String mConfigIntentAction;

            public Builder setTag(String tag) {
                mTag = tag;
                return this;
            }

            public Builder setSetting(String setting) {
                mSetting = setting;
                return this;
            }

            public Builder setIntentAction(String intentAction) {
                mIntentAction = intentAction;
                return this;
            }

            public Builder setConfigurationIntentAction(String action) {
                mConfigIntentAction = action;
                return this;
            }

            public Builder setPermission(String permission) {
                mPermission = permission;
                return this;
            }

            public Builder setNoun(String noun) {
                mNoun = noun;
                return this;
            }

            public Builder setWarningDialogTitle(int warningDialogTitle) {
                mWarningDialogTitle = warningDialogTitle;
                return this;
            }

            public Builder setWarningDialogSummary(int warningDialogSummary) {
                mWarningDialogSummary = warningDialogSummary;
                return this;
            }

            public Builder setEmptyText(int emptyText) {
                mEmptyText = emptyText;
                return this;
            }

            public Config build() {
                return new Config(mTag, mSetting, mIntentAction, mConfigIntentAction, mPermission,
                        mNoun, mWarningDialogTitle, mWarningDialogSummary, mEmptyText);
            }
        }
    }

}
