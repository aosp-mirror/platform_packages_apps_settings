/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.applications;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.usb.IUsbManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.applications.manageapplications.ManageApplications;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.applications.ApplicationsState.AppEntry;

import java.util.ArrayList;

public abstract class AppInfoBase extends SettingsPreferenceFragment
        implements ApplicationsState.Callbacks {

    public static final String ARG_PACKAGE_NAME = "package";
    public static final String ARG_PACKAGE_UID = "uid";

    private static final String TAG = "AppInfoBase";

    protected EnforcedAdmin mAppsControlDisallowedAdmin;
    protected boolean mAppsControlDisallowedBySystem;

    protected ApplicationFeatureProvider mApplicationFeatureProvider;
    protected ApplicationsState mState;
    protected ApplicationsState.Session mSession;
    protected ApplicationsState.AppEntry mAppEntry;
    protected PackageInfo mPackageInfo;
    protected int mUserId;
    protected String mPackageName;

    protected IUsbManager mUsbManager;
    protected DevicePolicyManager mDpm;
    protected UserManager mUserManager;
    protected PackageManager mPm;

    // Dialog identifiers used in showDialog
    protected static final int DLG_BASE = 0;

    protected boolean mFinishing;
    protected boolean mListeningToPackageRemove;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFinishing = false;
        final Activity activity = getActivity();
        mApplicationFeatureProvider = FeatureFactory.getFactory(activity)
                .getApplicationFeatureProvider(activity);
        mState = ApplicationsState.getInstance(activity.getApplication());
        mSession = mState.newSession(this, getSettingsLifecycle());
        mDpm = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mUserManager = (UserManager) activity.getSystemService(Context.USER_SERVICE);
        mPm = activity.getPackageManager();
        IBinder b = ServiceManager.getService(Context.USB_SERVICE);
        mUsbManager = IUsbManager.Stub.asInterface(b);

        retrieveAppEntry();
        startListeningToPackageRemove();
    }

    @Override
    public void onResume() {
        super.onResume();
        mAppsControlDisallowedAdmin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                getActivity(), UserManager.DISALLOW_APPS_CONTROL, mUserId);
        mAppsControlDisallowedBySystem = RestrictedLockUtilsInternal.hasBaseUserRestriction(
                getActivity(), UserManager.DISALLOW_APPS_CONTROL, mUserId);

        if (!refreshUi()) {
            setIntentAndFinish(true /* appChanged */);
        }
    }


    @Override
    public void onDestroy() {
        stopListeningToPackageRemove();
        super.onDestroy();
    }

    protected String retrieveAppEntry() {
        final Bundle args = getArguments();
        mPackageName = (args != null) ? args.getString(ARG_PACKAGE_NAME) : null;
        Intent intent = (args == null) ?
                getIntent() : (Intent) args.getParcelable("intent");
        if (mPackageName == null) {
            if (intent != null && intent.getData() != null) {
                mPackageName = intent.getData().getSchemeSpecificPart();
            }
        }
        if (intent != null && intent.hasExtra(Intent.EXTRA_USER_HANDLE)) {
            mUserId = ((UserHandle) intent.getParcelableExtra(
                    Intent.EXTRA_USER_HANDLE)).getIdentifier();
        } else {
            mUserId = UserHandle.myUserId();
        }
        mAppEntry = mState.getEntry(mPackageName, mUserId);
        if (mAppEntry != null) {
            // Get application info again to refresh changed properties of application
            try {
                mPackageInfo = mPm.getPackageInfoAsUser(mAppEntry.info.packageName,
                        PackageManager.MATCH_DISABLED_COMPONENTS |
                                PackageManager.GET_SIGNING_CERTIFICATES |
                                PackageManager.GET_PERMISSIONS, mUserId);
            } catch (NameNotFoundException e) {
                Log.e(TAG, "Exception when retrieving package:" + mAppEntry.info.packageName, e);
            }
        } else {
            Log.w(TAG, "Missing AppEntry; maybe reinstalling?");
            mPackageInfo = null;
        }

        return mPackageName;
    }

    protected void setIntentAndFinish(boolean appChanged) {
        Log.i(TAG, "appChanged=" + appChanged);
        Intent intent = new Intent();
        intent.putExtra(ManageApplications.APP_CHG, appChanged);
        SettingsActivity sa = (SettingsActivity) getActivity();
        sa.finishPreferencePanel(Activity.RESULT_OK, intent);
        mFinishing = true;
    }

    protected void showDialogInner(int id, int moveErrorCode) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id, moveErrorCode);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    protected abstract boolean refreshUi();

    protected abstract AlertDialog createDialog(int id, int errorCode);

    @Override
    public void onRunningStateChanged(boolean running) {
        refreshUi();
    }

    @Override
    public void onRebuildComplete(ArrayList<AppEntry> apps) {
        // No op.
    }

    @Override
    public void onPackageIconChanged() {
        // No op.
    }

    @Override
    public void onPackageSizeChanged(String packageName) {
        // No op.
    }

    @Override
    public void onAllSizesComputed() {
        // No op.
    }

    @Override
    public void onLauncherInfoChanged() {
        // No op.
    }

    @Override
    public void onLoadEntriesCompleted() {
        // No op.
    }

    @Override
    public void onPackageListChanged() {
        if (!refreshUi()) {
            setIntentAndFinish(true /* appChanged */);
        }
    }

    public static void startAppInfoFragment(Class<?> fragment, String title,
            String pkg, int uid, Fragment source, int request, int sourceMetricsCategory) {
        final Bundle args = new Bundle();
        args.putString(AppInfoBase.ARG_PACKAGE_NAME, pkg);
        args.putInt(AppInfoBase.ARG_PACKAGE_UID, uid);

        new SubSettingLauncher(source.getContext())
                .setDestination(fragment.getName())
                .setSourceMetricsCategory(sourceMetricsCategory)
                .setTitleText(title)
                .setArguments(args)
                .setUserHandle(new UserHandle(UserHandle.getUserId(uid)))
                .setResultListener(source, request)
                .launch();
    }

    public static class MyAlertDialogFragment extends InstrumentedDialogFragment {

        private static final String ARG_ID = "id";

        @Override
        public int getMetricsCategory() {
            return SettingsEnums.DIALOG_APP_INFO_ACTION;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt(ARG_ID);
            int errorCode = getArguments().getInt("moveError");
            Dialog dialog = ((AppInfoBase) getTargetFragment()).createDialog(id, errorCode);
            if (dialog == null) {
                throw new IllegalArgumentException("unknown id " + id);
            }
            return dialog;
        }

        public static MyAlertDialogFragment newInstance(int id, int errorCode) {
            MyAlertDialogFragment dialogFragment = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_ID, id);
            args.putInt("moveError", errorCode);
            dialogFragment.setArguments(args);
            return dialogFragment;
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
            if (!mFinishing && (mAppEntry == null || mAppEntry.info == null
                    || TextUtils.equals(mAppEntry.info.packageName, packageName))) {
                onPackageRemoved();
            }
        }
    };

}
