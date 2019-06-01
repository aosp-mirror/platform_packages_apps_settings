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

package com.android.settings.notification;

import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_NONE;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.role.RoleManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.Utils;
import com.android.settings.applications.AppInfoBase;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settingslib.RestrictedLockUtilsInternal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

abstract public class NotificationSettingsBase extends DashboardFragment {
    private static final String TAG = "NotifiSettingsBase";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    public static final String ARG_FROM_SETTINGS = "fromSettings";

    protected PackageManager mPm;
    protected NotificationBackend mBackend = new NotificationBackend();
    protected NotificationManager mNm;
    protected RoleManager mRm;
    protected Context mContext;

    protected int mUid;
    protected int mUserId;
    protected String mPkg;
    protected PackageInfo mPkgInfo;
    protected EnforcedAdmin mSuspendedAppsAdmin;
    protected NotificationChannelGroup mChannelGroup;
    protected NotificationChannel mChannel;
    protected NotificationBackend.AppRow mAppRow;

    protected boolean mShowLegacyChannelConfig = false;
    protected boolean mListeningToPackageRemove;

    protected List<NotificationPreferenceController> mControllers = new ArrayList<>();
    protected List<Preference> mDynamicPreferences = new ArrayList<>();
    protected ImportanceListener mImportanceListener = new ImportanceListener();

    protected Intent mIntent;
    protected Bundle mArgs;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = getActivity();
        mIntent = getActivity().getIntent();
        mArgs = getArguments();

        mPm = getPackageManager();
        mNm = NotificationManager.from(mContext);
        mRm = mContext.getSystemService(RoleManager.class);

        mPkg = mArgs != null && mArgs.containsKey(AppInfoBase.ARG_PACKAGE_NAME)
                ? mArgs.getString(AppInfoBase.ARG_PACKAGE_NAME)
                : mIntent.getStringExtra(Settings.EXTRA_APP_PACKAGE);
        mUid = mArgs != null && mArgs.containsKey(AppInfoBase.ARG_PACKAGE_UID)
                ? mArgs.getInt(AppInfoBase.ARG_PACKAGE_UID)
                : mIntent.getIntExtra(Settings.EXTRA_APP_UID, -1);

        if (mUid < 0) {
            try {
                mUid = mPm.getPackageUid(mPkg, 0);
            } catch (NameNotFoundException e) {
            }
        }

        mPkgInfo = findPackageInfo(mPkg, mUid);

        if (mPkgInfo != null) {
            mUserId = UserHandle.getUserId(mUid);
            mSuspendedAppsAdmin = RestrictedLockUtilsInternal.checkIfApplicationIsSuspended(
                    mContext, mPkg, mUserId);


            loadChannel();
            loadAppRow();
            loadChannelGroup();
            collectConfigActivities();

            getSettingsLifecycle().addObserver(use(HeaderPreferenceController.class));

            for (NotificationPreferenceController controller : mControllers) {
                controller.onResume(mAppRow, mChannel, mChannelGroup, mSuspendedAppsAdmin);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mIntent == null && mArgs == null) {
            Log.w(TAG, "No intent");
            toastAndFinish();
            return;
        }

        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null) {
            Log.w(TAG, "Missing package or uid or packageinfo");
            toastAndFinish();
            return;
        }

        startListeningToPackageRemove();
    }

    @Override
    public void onDestroy() {
        stopListeningToPackageRemove();
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mUid < 0 || TextUtils.isEmpty(mPkg) || mPkgInfo == null || mAppRow == null) {
            Log.w(TAG, "Missing package or uid or packageinfo");
            finish();
            return;
        }
        // Reload app, channel, etc onResume in case they've changed. A little wasteful if we've
        // just done onAttach but better than making every preference controller reload all
        // the data
        loadAppRow();
        if (mAppRow == null) {
            Log.w(TAG, "Can't load package");
            finish();
            return;
        }
        loadChannel();
        loadChannelGroup();
        collectConfigActivities();
    }

    private void loadChannel() {
        Intent intent = getActivity().getIntent();
        String channelId = intent != null ? intent.getStringExtra(Settings.EXTRA_CHANNEL_ID) : null;
        if (channelId == null && intent != null) {
            Bundle args = intent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
            channelId = args != null ? args.getString(Settings.EXTRA_CHANNEL_ID) : null;
        }
        mChannel = mBackend.getChannel(mPkg, mUid, channelId);
    }

    private void loadAppRow() {
        mAppRow = mBackend.loadAppRow(mContext, mPm, mRm, mPkgInfo);
    }

    private void loadChannelGroup() {
        mShowLegacyChannelConfig = mBackend.onlyHasDefaultChannel(mAppRow.pkg, mAppRow.uid)
                || (mChannel != null
                && NotificationChannel.DEFAULT_CHANNEL_ID.equals(mChannel.getId()));

        if (mShowLegacyChannelConfig) {
            mChannel = mBackend.getChannel(
                    mAppRow.pkg, mAppRow.uid, NotificationChannel.DEFAULT_CHANNEL_ID);
        }
        if (mChannel != null && !TextUtils.isEmpty(mChannel.getGroup())) {
            NotificationChannelGroup group = mBackend.getGroup(mPkg, mUid, mChannel.getGroup());
            if (group != null) {
                mChannelGroup = group;
            }
        }
    }

    protected void toastAndFinish() {
        Toast.makeText(mContext, R.string.app_not_found_dlg_text, Toast.LENGTH_SHORT).show();
        getActivity().finish();
    }

    protected void collectConfigActivities() {
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES)
                .setPackage(mAppRow.pkg);
        final List<ResolveInfo> resolveInfos = mPm.queryIntentActivities(
                intent,
                0 //PackageManager.MATCH_DEFAULT_ONLY
        );
        if (DEBUG) {
            Log.d(TAG, "Found " + resolveInfos.size() + " preference activities"
                    + (resolveInfos.size() == 0 ? " ;_;" : ""));
        }
        for (ResolveInfo ri : resolveInfos) {
            final ActivityInfo activityInfo = ri.activityInfo;
            if (mAppRow.settingsIntent != null) {
                if (DEBUG) {
                    Log.d(TAG, "Ignoring duplicate notification preference activity ("
                            + activityInfo.name + ") for package "
                            + activityInfo.packageName);
                }
                continue;
            }
            // TODO(78660939): This should actually start a new task
            mAppRow.settingsIntent = intent
                    .setPackage(null)
                    .setClassName(activityInfo.packageName, activityInfo.name);
            if (mChannel != null) {
                mAppRow.settingsIntent.putExtra(Notification.EXTRA_CHANNEL_ID, mChannel.getId());
            }
            if (mChannelGroup != null) {
                mAppRow.settingsIntent.putExtra(
                        Notification.EXTRA_CHANNEL_GROUP_ID, mChannelGroup.getId());
            }
        }
    }

    private PackageInfo findPackageInfo(String pkg, int uid) {
        if (pkg == null || uid < 0) {
            return null;
        }
        final String[] packages = mPm.getPackagesForUid(uid);
        if (packages != null && pkg != null) {
            final int N = packages.length;
            for (int i = 0; i < N; i++) {
                final String p = packages[i];
                if (pkg.equals(p)) {
                    try {
                        return mPm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES);
                    } catch (NameNotFoundException e) {
                        Log.w(TAG, "Failed to load package " + pkg, e);
                    }
                }
            }
        }
        return null;
    }

    private Drawable getAlertingIcon() {
        Drawable icon = getContext().getDrawable(R.drawable.ic_notifications_alert);
        icon.setTintList(Utils.getColorAccent(getContext()));
        return icon;
    }

    protected Preference populateSingleChannelPrefs(PreferenceGroup parent,
            final NotificationChannel channel, final boolean groupBlocked) {
        MasterSwitchPreference channelPref = new MasterSwitchPreference(getPrefContext());
        channelPref.setSwitchEnabled(mSuspendedAppsAdmin == null
                && isChannelBlockable(channel)
                && isChannelConfigurable(channel)
                && !groupBlocked);
        channelPref.setIcon(null);
        if (channel.getImportance() > IMPORTANCE_LOW) {
            channelPref.setIcon(getAlertingIcon());
        }
        channelPref.setIconSize(MasterSwitchPreference.ICON_SIZE_SMALL);
        channelPref.setKey(channel.getId());
        channelPref.setTitle(channel.getName());
        channelPref.setSummary(NotificationBackend.getSentSummary(
                mContext, mAppRow.sentByChannel.get(channel.getId()), false));
        channelPref.setChecked(channel.getImportance() != IMPORTANCE_NONE);
        Bundle channelArgs = new Bundle();
        channelArgs.putInt(AppInfoBase.ARG_PACKAGE_UID, mUid);
        channelArgs.putString(AppInfoBase.ARG_PACKAGE_NAME, mPkg);
        channelArgs.putString(Settings.EXTRA_CHANNEL_ID, channel.getId());
        channelArgs.putBoolean(ARG_FROM_SETTINGS, true);
        channelPref.setIntent(new SubSettingLauncher(getActivity())
                .setDestination(ChannelNotificationSettings.class.getName())
                .setArguments(channelArgs)
                .setTitleRes(R.string.notification_channel_title)
                .setSourceMetricsCategory(getMetricsCategory())
                .toIntent());

        channelPref.setOnPreferenceChangeListener(
                (preference, o) -> {
                    boolean value = (Boolean) o;
                    int importance = value ? IMPORTANCE_LOW : IMPORTANCE_NONE;
                    channel.setImportance(importance);
                    channel.lockFields(
                            NotificationChannel.USER_LOCKED_IMPORTANCE);
                    MasterSwitchPreference channelPref1 = (MasterSwitchPreference) preference;
                    channelPref1.setIcon(null);
                    if (channel.getImportance() > IMPORTANCE_LOW) {
                        channelPref1.setIcon(getAlertingIcon());
                    }
                    toggleBehaviorIconState(channelPref1.getIcon(),
                            importance != IMPORTANCE_NONE);
                    mBackend.updateChannel(mPkg, mUid, channel);

                    return true;
                });
        if (parent.findPreference(channelPref.getKey()) == null) {
            parent.addPreference(channelPref);
        }
        return channelPref;
    }

    private void toggleBehaviorIconState(Drawable icon, boolean enabled) {
        if (icon == null) return;

        LayerDrawable layerDrawable = (LayerDrawable) icon;
        GradientDrawable background =
                (GradientDrawable) layerDrawable.findDrawableByLayerId(R.id.back);

        if (background == null) return;

        if (enabled) {
            background.clearColorFilter();
        } else {
            background.setColorFilter(new BlendModeColorFilter(
                    mContext.getColor(R.color.material_grey_300),
                    BlendMode.SRC_IN));
        }
    }

    protected boolean isChannelConfigurable(NotificationChannel channel) {
        if (channel != null && mAppRow != null) {
            return !channel.isImportanceLockedByOEM();
        }
        return false;
    }

    protected boolean isChannelBlockable(NotificationChannel channel) {
        if (channel != null && mAppRow != null) {
            if (!mAppRow.systemApp) {
                return true;
            }

            if (channel.isImportanceLockedByCriticalDeviceFunction()) {
                return false;
            }

            if (channel.isImportanceLockedByOEM()) {
                return false;
            }

            return channel.isBlockableSystem()
                    || channel.getImportance() == NotificationManager.IMPORTANCE_NONE;
        }
        return false;
    }

    protected boolean isChannelGroupBlockable(NotificationChannelGroup group) {
        if (group != null && mAppRow != null) {
            if (!mAppRow.systemApp) {
                return true;
            }

            return group.isBlocked();
        }
        return false;
    }

    protected void setVisible(Preference p, boolean visible) {
        setVisible(getPreferenceScreen(), p, visible);
    }

    protected void setVisible(PreferenceGroup parent, Preference p, boolean visible) {
        final boolean isVisible = parent.findPreference(p.getKey()) != null;
        if (isVisible == visible) return;
        if (visible) {
            parent.addPreference(p);
        } else {
            parent.removePreference(p);
        }
    }

    protected void startListeningToPackageRemove() {
        if (mListeningToPackageRemove) {
            return;
        }
        mListeningToPackageRemove = true;
        final IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        getContext().registerReceiver(mPackageRemovedReceiver, filter);
    }

    protected void stopListeningToPackageRemove() {
        if (!mListeningToPackageRemove) {
            return;
        }
        mListeningToPackageRemove = false;
        getContext().unregisterReceiver(mPackageRemovedReceiver);
    }

    protected void onPackageRemoved() {
        getActivity().finishAndRemoveTask();
    }

    protected final BroadcastReceiver mPackageRemovedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String packageName = intent.getData().getSchemeSpecificPart();
            if (mPkgInfo == null || TextUtils.equals(mPkgInfo.packageName, packageName)) {
                if (DEBUG) {
                    Log.d(TAG, "Package (" + packageName + ") removed. Removing"
                            + "NotificationSettingsBase.");
                }
                onPackageRemoved();
            }
        }
    };

    protected Comparator<NotificationChannel> mChannelComparator =
            (left, right) -> {
                if (left.isDeleted() != right.isDeleted()) {
                    return Boolean.compare(left.isDeleted(), right.isDeleted());
                } else if (left.getId().equals(NotificationChannel.DEFAULT_CHANNEL_ID)) {
                    // Uncategorized/miscellaneous legacy channel goes last
                    return 1;
                } else if (right.getId().equals(NotificationChannel.DEFAULT_CHANNEL_ID)) {
                    return -1;
                }

                return left.getId().compareTo(right.getId());
            };

    protected class ImportanceListener {
        protected void onImportanceChanged() {
            final PreferenceScreen screen = getPreferenceScreen();
            for (NotificationPreferenceController controller : mControllers) {
                controller.displayPreference(screen);
            }
            updatePreferenceStates();

            boolean hideDynamicFields = false;
            if (mAppRow == null || mAppRow.banned) {
                hideDynamicFields = true;
            } else {
                if (mChannel != null) {
                    hideDynamicFields = mChannel.getImportance() == IMPORTANCE_NONE;
                } else if (mChannelGroup != null) {
                    hideDynamicFields = mChannelGroup.isBlocked();
                }
            }
            for (Preference preference : mDynamicPreferences) {
                setVisible(getPreferenceScreen(), preference, !hideDynamicFields);
            }
        }
    }
}
