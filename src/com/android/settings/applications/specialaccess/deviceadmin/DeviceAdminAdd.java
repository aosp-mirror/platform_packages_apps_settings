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

package com.android.settings.applications.specialaccess.deviceadmin;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.text.method.ScrollingMovementMethod;
import android.util.EventLog;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AppSecurityPermissions;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.android.settings.EventLogTags;
import com.android.settings.R;
import com.android.settings.fuelgauge.BatteryUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.users.UserDialogs;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DeviceAdminAdd extends Activity {
    static final String TAG = "DeviceAdminAdd";

    static final int DIALOG_WARNING = 1;

    private static final int MAX_ADD_MSG_LINES_PORTRAIT = 5;
    private static final int MAX_ADD_MSG_LINES_LANDSCAPE = 2;
    private static final int MAX_ADD_MSG_LINES = 15;

    /**
     * Optional key to map to the package name of the Device Admin.
     * Currently only used when uninstalling an active device admin.
     */
    public static final String EXTRA_DEVICE_ADMIN_PACKAGE_NAME =
            "android.app.extra.DEVICE_ADMIN_PACKAGE_NAME";

    public static final String EXTRA_CALLED_FROM_SUPPORT_DIALOG =
            "android.app.extra.CALLED_FROM_SUPPORT_DIALOG";

    private final IBinder mToken = new Binder();
    Handler mHandler;

    DevicePolicyManager mDPM;
    AppOpsManager mAppOps;
    DeviceAdminInfo mDeviceAdmin;
    String mAddMsgText;
    String mProfileOwnerName;

    ImageView mAdminIcon;
    TextView mAdminName;
    TextView mAdminDescription;
    TextView mAddMsg;
    TextView mProfileOwnerWarning;
    ImageView mAddMsgExpander;
    boolean mAddMsgEllipsized = true;
    TextView mAdminWarning;
    TextView mSupportMessage;
    ViewGroup mAdminPolicies;
    Button mActionButton;
    Button mUninstallButton;
    Button mCancelButton;

    boolean mUninstalling = false;
    boolean mAdding;
    boolean mRefreshing;
    boolean mWaitingForRemoveMsg;
    boolean mAddingProfileOwner;
    boolean mAdminPoliciesInitialized;

    boolean mIsCalledFromSupportDialog = false;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mHandler = new Handler(getMainLooper());

        mDPM = (DevicePolicyManager)getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAppOps = (AppOpsManager)getSystemService(Context.APP_OPS_SERVICE);
        PackageManager packageManager = getPackageManager();

        if ((getIntent().getFlags()&Intent.FLAG_ACTIVITY_NEW_TASK) != 0) {
            Log.w(TAG, "Cannot start ADD_DEVICE_ADMIN as a new task");
            finish();
            return;
        }

        mIsCalledFromSupportDialog = getIntent().getBooleanExtra(
                EXTRA_CALLED_FROM_SUPPORT_DIALOG, false);

        String action = getIntent().getAction();
        ComponentName who = (ComponentName)getIntent().getParcelableExtra(
                DevicePolicyManager.EXTRA_DEVICE_ADMIN);
        if (who == null) {
            String packageName = getIntent().getStringExtra(EXTRA_DEVICE_ADMIN_PACKAGE_NAME);
            Optional<ComponentName> installedAdmin = findAdminWithPackageName(packageName);
            if (!installedAdmin.isPresent()) {
                Log.w(TAG, "No component specified in " + action);
                finish();
                return;
            }
            who = installedAdmin.get();
            mUninstalling = true;
        }

        if (action != null && action.equals(DevicePolicyManager.ACTION_SET_PROFILE_OWNER)) {
            setResult(RESULT_CANCELED);
            setFinishOnTouchOutside(true);
            mAddingProfileOwner = true;
            mProfileOwnerName =
                    getIntent().getStringExtra(DevicePolicyManager.EXTRA_PROFILE_OWNER_NAME);
            String callingPackage = getCallingPackage();
            if (callingPackage == null || !callingPackage.equals(who.getPackageName())) {
                Log.e(TAG, "Unknown or incorrect caller");
                finish();
                return;
            }
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(callingPackage, 0);
                if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                    Log.e(TAG, "Cannot set a non-system app as a profile owner");
                    finish();
                    return;
                }
            } catch (NameNotFoundException nnfe) {
                Log.e(TAG, "Cannot find the package " + callingPackage);
                finish();
                return;
            }
        }

        ActivityInfo ai;
        try {
            ai = packageManager.getReceiverInfo(who, PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Unable to retrieve device policy " + who, e);
            finish();
            return;
        }

        // When activating, make sure the given component name is actually a valid device admin.
        // No need to check this when deactivating, because it is safe to deactivate an active
        // invalid device admin.
        if (!mDPM.isAdminActive(who)) {
            List<ResolveInfo> avail = packageManager.queryBroadcastReceivers(
                    new Intent(DeviceAdminReceiver.ACTION_DEVICE_ADMIN_ENABLED),
                    PackageManager.GET_DISABLED_UNTIL_USED_COMPONENTS);
            int count = avail == null ? 0 : avail.size();
            boolean found = false;
            for (int i=0; i<count; i++) {
                ResolveInfo ri = avail.get(i);
                if (ai.packageName.equals(ri.activityInfo.packageName)
                        && ai.name.equals(ri.activityInfo.name)) {
                    try {
                        // We didn't retrieve the meta data for all possible matches, so
                        // need to use the activity info of this specific one that was retrieved.
                        ri.activityInfo = ai;
                        DeviceAdminInfo dpi = new DeviceAdminInfo(this, ri);
                        found = true;
                    } catch (XmlPullParserException e) {
                        Log.w(TAG, "Bad " + ri.activityInfo, e);
                    } catch (IOException e) {
                        Log.w(TAG, "Bad " + ri.activityInfo, e);
                    }
                    break;
                }
            }
            if (!found) {
                Log.w(TAG, "Request to add invalid device admin: " + who);
                finish();
                return;
            }
        }

        ResolveInfo ri = new ResolveInfo();
        ri.activityInfo = ai;
        try {
            mDeviceAdmin = new DeviceAdminInfo(this, ri);
        } catch (XmlPullParserException e) {
            Log.w(TAG, "Unable to retrieve device policy " + who, e);
            finish();
            return;
        } catch (IOException e) {
            Log.w(TAG, "Unable to retrieve device policy " + who, e);
            finish();
            return;
        }

        // This admin already exists, an we have two options at this point.  If new policy
        // bits are set, show the user the new list.  If nothing has changed, simply return
        // "OK" immediately.
        if (DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN.equals(getIntent().getAction())) {
            mRefreshing = false;
            if (mDPM.isAdminActive(who)) {
                if (mDPM.isRemovingAdmin(who, android.os.Process.myUserHandle().getIdentifier())) {
                    Log.w(TAG, "Requested admin is already being removed: " + who);
                    finish();
                    return;
                }

                ArrayList<DeviceAdminInfo.PolicyInfo> newPolicies = mDeviceAdmin.getUsedPolicies();
                for (int i = 0; i < newPolicies.size(); i++) {
                    DeviceAdminInfo.PolicyInfo pi = newPolicies.get(i);
                    if (!mDPM.hasGrantedPolicy(who, pi.ident)) {
                        mRefreshing = true;
                        break;
                    }
                }
                if (!mRefreshing) {
                    // Nothing changed (or policies were removed) - return immediately
                    setResult(Activity.RESULT_OK);
                    finish();
                    return;
                }
            }
        }

        final CharSequence addMsgCharSequence = getIntent().getCharSequenceExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION);
        if (addMsgCharSequence != null) {
            mAddMsgText = addMsgCharSequence.toString();
        }

        if (mAddingProfileOwner) {
            // If we're trying to add a profile owner and user setup hasn't completed yet, no
            // need to prompt for permission. Just add and finish
            if (!mDPM.hasUserSetupCompleted()) {
                addAndFinish();
                return;
            }

            // othewise, only the defined default supervision profile owner can be set after user
            // setup.
            final String supervisor = getString(
                    com.android.internal.R.string.config_defaultSupervisionProfileOwnerComponent);
            if (supervisor == null) {
                Log.w(TAG, "Unable to set profile owner post-setup, no default supervisor"
                        + "profile owner defined");
                finish();
                return;
            }

            final ComponentName supervisorComponent = ComponentName.unflattenFromString(
                    supervisor);
            if (who.compareTo(supervisorComponent) != 0) {
                Log.w(TAG, "Unable to set non-default profile owner post-setup " + who);
                finish();
                return;
            }

            // Build and show the simplified dialog
            final Dialog dialog = new AlertDialog.Builder(this)
                    .setTitle(getText(R.string.profile_owner_add_title_simplified))
                    .setView(R.layout.profile_owner_add)
                    .setPositiveButton(R.string.allow, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            addAndFinish();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        public void onDismiss(DialogInterface dialogInterface) {
                            finish();
                        }
                    })
                    .create();
            dialog.show();

            mActionButton = ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE);
            mActionButton.setFilterTouchesWhenObscured(true);
            mAddMsg = dialog.findViewById(R.id.add_msg_simplified);
            mAddMsg.setMovementMethod(new ScrollingMovementMethod());
            mAddMsg.setText(mAddMsgText);
            mAdminWarning = dialog.findViewById(R.id.admin_warning_simplified);
            mAdminWarning.setText(getString(R.string.device_admin_warning_simplified,
                    mProfileOwnerName));
            return;
        }
        setContentView(R.layout.device_admin_add);

        mAdminIcon = (ImageView)findViewById(R.id.admin_icon);
        mAdminName = (TextView)findViewById(R.id.admin_name);
        mAdminDescription = (TextView)findViewById(R.id.admin_description);
        mProfileOwnerWarning = (TextView) findViewById(R.id.profile_owner_warning);

        mAddMsg = (TextView)findViewById(R.id.add_msg);
        mAddMsgExpander = (ImageView) findViewById(R.id.add_msg_expander);
        final View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMessageEllipsis(mAddMsg);
            }
        };
        mAddMsgExpander.setOnClickListener(onClickListener);

        // Determine whether the message can be collapsed - getLineCount() gives the correct
        // number of lines only after a layout pass.
        mAddMsg.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        final int maxLines = getEllipsizedLines();
                        // hide the icon if number of visible lines does not exceed maxLines
                        boolean hideMsgExpander = mAddMsg.getLineCount() <= maxLines;
                        mAddMsgExpander.setVisibility(hideMsgExpander ? View.GONE : View.VISIBLE);
                        if (hideMsgExpander) {
                            ((View)mAddMsgExpander.getParent()).invalidate();
                        }
                        mAddMsg.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });

        // toggleMessageEllipsis also handles initial layout:
        toggleMessageEllipsis(mAddMsg);

        mAdminWarning = (TextView) findViewById(R.id.admin_warning);
        mAdminPolicies = (ViewGroup) findViewById(R.id.admin_policies);
        mSupportMessage = (TextView) findViewById(R.id.admin_support_message);

        mCancelButton = (Button) findViewById(R.id.cancel_button);
        mCancelButton.setFilterTouchesWhenObscured(true);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EventLog.writeEvent(EventLogTags.EXP_DET_DEVICE_ADMIN_DECLINED_BY_USER,
                    mDeviceAdmin.getActivityInfo().applicationInfo.uid);
                finish();
            }
        });

        mUninstallButton = (Button) findViewById(R.id.uninstall_button);
        mUninstallButton.setFilterTouchesWhenObscured(true);
        mUninstallButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EventLog.writeEvent(EventLogTags.EXP_DET_DEVICE_ADMIN_UNINSTALLED_BY_USER,
                        mDeviceAdmin.getActivityInfo().applicationInfo.uid);
                mDPM.uninstallPackageWithActiveAdmins(mDeviceAdmin.getPackageName());
                finish();
            }
        });

        mActionButton = (Button) findViewById(R.id.action_button);

        final View restrictedAction = findViewById(R.id.restricted_action);
        restrictedAction.setFilterTouchesWhenObscured(true);
        restrictedAction.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!mActionButton.isEnabled()) {
                    showPolicyTransparencyDialogIfRequired();
                    return;
                }
                if (mAdding) {
                    addAndFinish();
                } else if (isManagedProfile(mDeviceAdmin)
                        && mDeviceAdmin.getComponent().equals(mDPM.getProfileOwner())) {
                    final int userId = UserHandle.myUserId();
                    UserDialogs.createRemoveDialog(DeviceAdminAdd.this, userId,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    UserManager um = UserManager.get(DeviceAdminAdd.this);
                                    um.removeUser(userId);
                                    finish();
                                }
                            }
                    ).show();
                } else if (mUninstalling) {
                    mDPM.uninstallPackageWithActiveAdmins(mDeviceAdmin.getPackageName());
                    finish();
                } else if (!mWaitingForRemoveMsg) {
                    try {
                        // Don't allow the admin to put a dialog up in front
                        // of us while we interact with the user.
                        ActivityManager.getService().stopAppSwitches();
                    } catch (RemoteException e) {
                    }
                    mWaitingForRemoveMsg = true;
                    mDPM.getRemoveWarning(mDeviceAdmin.getComponent(),
                            new RemoteCallback(new RemoteCallback.OnResultListener() {
                                @Override
                                public void onResult(Bundle result) {
                                    CharSequence msg = result != null
                                            ? result.getCharSequence(
                                            DeviceAdminReceiver.EXTRA_DISABLE_WARNING)
                                            : null;
                                    continueRemoveAction(msg);
                                }
                            }, mHandler));
                    // Don't want to wait too long.
                    getWindow().getDecorView().getHandler().postDelayed(new Runnable() {
                        @Override public void run() {
                            continueRemoveAction(null);
                        }
                    }, 2*1000);
                }
            }
        });
    }

    /**
     * Shows a dialog to explain why the button is disabled if required.
     */
    private void showPolicyTransparencyDialogIfRequired() {
        if (isManagedProfile(mDeviceAdmin)
                && mDeviceAdmin.getComponent().equals(mDPM.getProfileOwner())) {
            EnforcedAdmin enforcedAdmin;
            ComponentName adminComponent = mDPM.getProfileOwnerAsUser(getUserId());
            if (adminComponent != null && mDPM.isOrganizationOwnedDeviceWithManagedProfile()) {
                enforcedAdmin = new EnforcedAdmin(adminComponent,
                        UserManager.DISALLOW_REMOVE_MANAGED_PROFILE, UserHandle.of(getUserId()));
            } else {
                // Todo (b/151061366): Investigate this case to check if it is still viable.
                if (hasBaseCantRemoveProfileRestriction()) {
                    // If DISALLOW_REMOVE_MANAGED_PROFILE is set by the system, there's no
                    // point showing a dialog saying it's disabled by an admin.
                    return;
                }
                enforcedAdmin = getAdminEnforcingCantRemoveProfile();
            }
            if (enforcedAdmin != null) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(
                        DeviceAdminAdd.this,
                        enforcedAdmin);
            }
        }
    }

    void addAndFinish() {
        try {
            logSpecialPermissionChange(true, mDeviceAdmin.getComponent().getPackageName());
            mDPM.setActiveAdmin(mDeviceAdmin.getComponent(), mRefreshing);
            EventLog.writeEvent(EventLogTags.EXP_DET_DEVICE_ADMIN_ACTIVATED_BY_USER,
                mDeviceAdmin.getActivityInfo().applicationInfo.uid);

            unrestrictAppIfPossible(BatteryUtils.getInstance(this));

            setResult(Activity.RESULT_OK);
        } catch (RuntimeException e) {
            // Something bad happened...  could be that it was
            // already set, though.
            Log.w(TAG, "Exception trying to activate admin "
                    + mDeviceAdmin.getComponent(), e);
            if (mDPM.isAdminActive(mDeviceAdmin.getComponent())) {
                setResult(Activity.RESULT_OK);
            }
        }
        if (mAddingProfileOwner) {
            try {
                mDPM.setProfileOwner(mDeviceAdmin.getComponent(),
                        mProfileOwnerName, UserHandle.myUserId());
            } catch (RuntimeException re) {
                setResult(Activity.RESULT_CANCELED);
            }
        }
        finish();
    }

    void unrestrictAppIfPossible(BatteryUtils batteryUtils) {
        // Unrestrict admin app if it is already been restricted
        final String packageName = mDeviceAdmin.getComponent().getPackageName();
        batteryUtils.clearForceAppStandby(packageName);
    }

    void continueRemoveAction(CharSequence msg) {
        if (!mWaitingForRemoveMsg) {
            return;
        }
        mWaitingForRemoveMsg = false;
        if (msg == null) {
            try {
                ActivityManager.getService().resumeAppSwitches();
            } catch (RemoteException e) {
            }
            logSpecialPermissionChange(false, mDeviceAdmin.getComponent().getPackageName());
            mDPM.removeActiveAdmin(mDeviceAdmin.getComponent());
            finish();
        } else {
            try {
                // Continue preventing anything from coming in front.
                ActivityManager.getService().stopAppSwitches();
            } catch (RemoteException e) {
            }
            Bundle args = new Bundle();
            args.putCharSequence(
                    DeviceAdminReceiver.EXTRA_DISABLE_WARNING, msg);
            showDialog(DIALOG_WARNING, args);
        }
    }

    void logSpecialPermissionChange(boolean allow, String packageName) {
        int logCategory = allow ? SettingsEnums.APP_SPECIAL_PERMISSION_ADMIN_ALLOW :
                SettingsEnums.APP_SPECIAL_PERMISSION_ADMIN_DENY;
        FeatureFactory.getFactory(this).getMetricsFeatureProvider().action(
                SettingsEnums.PAGE_UNKNOWN,
                logCategory,
                SettingsEnums.PAGE_UNKNOWN,
                packageName,
                0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mActionButton.setEnabled(true);
        if (!mAddingProfileOwner) {
            updateInterface();
        }
        // As long as we are running, don't let anyone overlay stuff on top of the screen.
        mAppOps.setUserRestriction(AppOpsManager.OP_SYSTEM_ALERT_WINDOW, true, mToken);
        mAppOps.setUserRestriction(AppOpsManager.OP_TOAST_WINDOW, true, mToken);

    }

    @Override
    protected void onPause() {
        super.onPause();
        // This just greys out the button. The actual listener is attached to R.id.restricted_action
        mActionButton.setEnabled(false);
        mAppOps.setUserRestriction(AppOpsManager.OP_SYSTEM_ALERT_WINDOW, false, mToken);
        mAppOps.setUserRestriction(AppOpsManager.OP_TOAST_WINDOW, false, mToken);
        try {
            ActivityManager.getService().resumeAppSwitches();
        } catch (RemoteException e) {
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // In case this is triggered from support dialog, finish this activity once the user leaves
        // so that this won't appear as a background next time support dialog is triggered. This
        // is because the support dialog activity and this belong to the same task and we can't
        // start this in new activity since we need to know the calling package in this activity.
        if (mIsCalledFromSupportDialog) {
            finish();
        }
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_WARNING: {
                CharSequence msg = args.getCharSequence(DeviceAdminReceiver.EXTRA_DISABLE_WARNING);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(msg);
                builder.setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            ActivityManager.getService().resumeAppSwitches();
                        } catch (RemoteException e) {
                        }
                        mDPM.removeActiveAdmin(mDeviceAdmin.getComponent());
                        finish();
                    }
                });
                builder.setNegativeButton(R.string.dlg_cancel, null);
                return builder.create();
            }
            default:
                return super.onCreateDialog(id, args);

        }
    }

    void updateInterface() {
        findViewById(R.id.restricted_icon).setVisibility(View.GONE);
        mAdminIcon.setImageDrawable(mDeviceAdmin.loadIcon(getPackageManager()));
        mAdminName.setText(mDeviceAdmin.loadLabel(getPackageManager()));
        try {
            mAdminDescription.setText(
                    mDeviceAdmin.loadDescription(getPackageManager()));
            mAdminDescription.setVisibility(View.VISIBLE);
        } catch (Resources.NotFoundException e) {
            mAdminDescription.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(mAddMsgText)) {
            mAddMsg.setText(mAddMsgText);
            mAddMsg.setVisibility(View.VISIBLE);
        } else {
            mAddMsg.setVisibility(View.GONE);
            mAddMsgExpander.setVisibility(View.GONE);
        }
        if (!mRefreshing && !mAddingProfileOwner
                && mDPM.isAdminActive(mDeviceAdmin.getComponent())) {
            mAdding = false;
            final boolean isProfileOwner =
                    mDeviceAdmin.getComponent().equals(mDPM.getProfileOwner());
            final boolean isManagedProfile = isManagedProfile(mDeviceAdmin);
            if (isProfileOwner && isManagedProfile) {
                // Profile owner in a managed profile, user can remove profile to disable admin.
                mAdminWarning.setText(R.string.admin_profile_owner_message);
                mActionButton.setText(R.string.remove_managed_profile_label);

                final EnforcedAdmin admin = getAdminEnforcingCantRemoveProfile();
                final boolean hasBaseRestriction = hasBaseCantRemoveProfileRestriction();
                if ((hasBaseRestriction && mDPM.isOrganizationOwnedDeviceWithManagedProfile())
                        || (admin != null && !hasBaseRestriction)) {
                    findViewById(R.id.restricted_icon).setVisibility(View.VISIBLE);
                }
                mActionButton.setEnabled(admin == null && !hasBaseRestriction);
            } else if (isProfileOwner || mDeviceAdmin.getComponent().equals(
                            mDPM.getDeviceOwnerComponentOnCallingUser())) {
                // Profile owner in a user or device owner, user can't disable admin.
                if (isProfileOwner) {
                    // Show profile owner in a user description.
                    mAdminWarning.setText(R.string.admin_profile_owner_user_message);
                } else {
                    // Show device owner description.
                    mAdminWarning.setText(R.string.admin_device_owner_message);
                }
                mActionButton.setText(R.string.remove_device_admin);
                mActionButton.setEnabled(false);
            } else {
                addDeviceAdminPolicies(false /* showDescription */);
                mAdminWarning.setText(getString(R.string.device_admin_status,
                        mDeviceAdmin.getActivityInfo().applicationInfo.loadLabel(
                        getPackageManager())));
                setTitle(R.string.active_device_admin_msg);
                if (mUninstalling) {
                    mActionButton.setText(R.string.remove_and_uninstall_device_admin);
                } else {
                    mActionButton.setText(R.string.remove_device_admin);
                }
            }
            CharSequence supportMessage = mDPM.getLongSupportMessageForUser(
                    mDeviceAdmin.getComponent(), UserHandle.myUserId());
            if (!TextUtils.isEmpty(supportMessage)) {
                mSupportMessage.setText(supportMessage);
                mSupportMessage.setVisibility(View.VISIBLE);
            } else {
                mSupportMessage.setVisibility(View.GONE);
            }
        } else {
            addDeviceAdminPolicies(true /* showDescription */);
            mAdminWarning.setText(getString(R.string.device_admin_warning,
                    mDeviceAdmin.getActivityInfo().applicationInfo.loadLabel(getPackageManager())));
            setTitle(getText(R.string.add_device_admin_msg));
            mActionButton.setText(getText(R.string.add_device_admin));
            if (isAdminUninstallable()) {
                mUninstallButton.setVisibility(View.VISIBLE);
            }
            mSupportMessage.setVisibility(View.GONE);
            mAdding = true;
        }
    }

    private EnforcedAdmin getAdminEnforcingCantRemoveProfile() {
        // Removing a managed profile is disallowed if DISALLOW_REMOVE_MANAGED_PROFILE
        // is set in the parent rather than the user itself.
        return RestrictedLockUtilsInternal.checkIfRestrictionEnforced(this,
                UserManager.DISALLOW_REMOVE_MANAGED_PROFILE, getParentUserId());
    }

    private boolean hasBaseCantRemoveProfileRestriction() {
        return RestrictedLockUtilsInternal.hasBaseUserRestriction(this,
                UserManager.DISALLOW_REMOVE_MANAGED_PROFILE, getParentUserId());
    }

    private int getParentUserId() {
        return UserManager.get(this).getProfileParent(UserHandle.myUserId()).id;
    }

    private void addDeviceAdminPolicies(boolean showDescription) {
        if (!mAdminPoliciesInitialized) {
            boolean isAdminUser = UserManager.get(this).isAdminUser();
            for (DeviceAdminInfo.PolicyInfo pi : mDeviceAdmin.getUsedPolicies()) {
                int descriptionId = isAdminUser ? pi.description : pi.descriptionForSecondaryUsers;
                int labelId = isAdminUser ? pi.label : pi.labelForSecondaryUsers;
                View view = AppSecurityPermissions.getPermissionItemView(this, getText(labelId),
                        showDescription ? getText(descriptionId) : "", true);
                mAdminPolicies.addView(view);
            }
            mAdminPoliciesInitialized = true;
        }
    }

    void toggleMessageEllipsis(View v) {
        TextView tv = (TextView) v;

        mAddMsgEllipsized = ! mAddMsgEllipsized;
        tv.setEllipsize(mAddMsgEllipsized ? TruncateAt.END : null);
        tv.setMaxLines(mAddMsgEllipsized ? getEllipsizedLines() : MAX_ADD_MSG_LINES);

        mAddMsgExpander.setImageResource(mAddMsgEllipsized ?
            com.android.internal.R.drawable.expander_ic_minimized :
            com.android.internal.R.drawable.expander_ic_maximized);
    }

    int getEllipsizedLines() {
        Display d = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();

        return d.getHeight() > d.getWidth() ?
            MAX_ADD_MSG_LINES_PORTRAIT : MAX_ADD_MSG_LINES_LANDSCAPE;
    }

    /**
     * @return true if adminInfo is running in a managed profile.
     */
    private boolean isManagedProfile(DeviceAdminInfo adminInfo) {
        UserManager um = UserManager.get(this);
        UserInfo info = um.getUserInfo(
                UserHandle.getUserId(adminInfo.getActivityInfo().applicationInfo.uid));
        return info != null ? info.isManagedProfile() : false;
    }

    /**
     * @return an {@link Optional} containing the admin with a given package name, if it exists,
     *         or {@link Optional#empty()} otherwise.
     */
    private Optional<ComponentName> findAdminWithPackageName(String packageName) {
        List<ComponentName> admins = mDPM.getActiveAdmins();
        if (admins == null) {
            return Optional.empty();
        }
        return admins.stream().filter(i -> i.getPackageName().equals(packageName)).findAny();
    }

    private boolean isAdminUninstallable() {
        // System apps can't be uninstalled.
        return !mDeviceAdmin.getActivityInfo().applicationInfo.isSystemApp();
    }
}
