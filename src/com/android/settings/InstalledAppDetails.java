

/**
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.android.settings;

import com.android.settings.R;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.text.format.Formatter;
import android.util.Config;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import android.content.ComponentName;
import android.view.View;
import android.widget.AppSecurityPermissions;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Activity to display application information from Settings. This activity presents
 * extended information associated with a package like code, data, total size, permissions
 * used by the application and also the set of default launchable activities.
 * For system applications, an option to clear user data is displayed only if data size is > 0.
 * System applications that do not want clear user data do not have this option.
 * For non-system applications, there is no option to clear data. Instead there is an option to
 * uninstall the application.
 */
public class InstalledAppDetails extends Activity implements View.OnClickListener {
    private static final String TAG="InstalledAppDetails";
    private static final int _UNKNOWN_APP=R.string.unknown;
    private ApplicationInfo mAppInfo;
    private Button mUninstallButton;
    private boolean mUpdatedSysApp = false;
    private Button mActivitiesButton;
    private boolean localLOGV = false;
    private TextView mAppVersion;
    private TextView mTotalSize;
    private TextView mAppSize;
    private TextView mDataSize;
    private PkgSizeObserver mSizeObserver;
    private ClearUserDataObserver mClearDataObserver;
    // Views related to cache info
    private TextView mCacheSize;
    private Button mClearCacheButton;
    private ClearCacheObserver mClearCacheObserver;
    private Button mForceStopButton;
    private Button mClearDataButton;
    private Button mMoveAppButton;
    
    PackageStats mSizeInfo;
    private PackageManager mPm;
    
    //internal constants used in Handler
    private static final int OP_SUCCESSFUL = 1;
    private static final int OP_FAILED = 2;
    private static final int CLEAR_USER_DATA = 1;
    private static final int GET_PKG_SIZE = 2;
    private static final int CLEAR_CACHE = 3;
    private static final String ATTR_PACKAGE_STATS="PackageStats";
    
    // invalid size value used initially and also when size retrieval through PackageManager
    // fails for whatever reason
    private static final int SIZE_INVALID = -1;
    
    // Resource strings
    private CharSequence mInvalidSizeStr;
    private CharSequence mComputingStr;
    
    // Dialog identifiers used in showDialog
    private static final int DLG_BASE = 0;
    private static final int DLG_CLEAR_DATA = DLG_BASE + 1;
    private static final int DLG_FACTORY_RESET = DLG_BASE + 2;
    private static final int DLG_APP_NOT_FOUND = DLG_BASE + 3;
    private static final int DLG_CANNOT_CLEAR_DATA = DLG_BASE + 4;
    private static final int DLG_FORCE_STOP = DLG_BASE + 5;
    
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CLEAR_USER_DATA:
                    processClearMsg(msg);
                    break;
                case GET_PKG_SIZE:
                    refreshSizeInfo(msg);
                    break;
                case CLEAR_CACHE:
                    // Refresh size info
                    mPm.getPackageSizeInfo(mAppInfo.packageName, mSizeObserver);
                    break;
                default:
                    break;
            }
        }
    };
    
    class ClearUserDataObserver extends IPackageDataObserver.Stub {
       public void onRemoveCompleted(final String packageName, final boolean succeeded) {
           final Message msg = mHandler.obtainMessage(CLEAR_USER_DATA);
           msg.arg1 = succeeded?OP_SUCCESSFUL:OP_FAILED;
           mHandler.sendMessage(msg);
        }
    }
    
    class PkgSizeObserver extends IPackageStatsObserver.Stub {
        public void onGetStatsCompleted(PackageStats pStats, boolean succeeded) {
             Message msg = mHandler.obtainMessage(GET_PKG_SIZE);
             Bundle data = new Bundle();
             data.putParcelable(ATTR_PACKAGE_STATS, pStats);
             msg.setData(data);
             mHandler.sendMessage(msg);
            
         }
     }

    class ClearCacheObserver extends IPackageDataObserver.Stub {
        public void onRemoveCompleted(final String packageName, final boolean succeeded) {
            final Message msg = mHandler.obtainMessage(CLEAR_CACHE);
            msg.arg1 = succeeded?OP_SUCCESSFUL:OP_FAILED;
            mHandler.sendMessage(msg);
         }
     }
    
    private String getSizeStr(long size) {
        if (size == SIZE_INVALID) {
            return mInvalidSizeStr.toString();
        }
        return Formatter.formatFileSize(this, size);
    }
    
    private void initDataButtons() {
        if (mAppInfo.manageSpaceActivityName != null) {
            mClearDataButton.setText(R.string.manage_space_text);
        } else {
            mClearDataButton.setText(R.string.clear_user_data_text);
        }
        mClearDataButton.setOnClickListener(this);
        String pkgName = mAppInfo.packageName;
        boolean dataOnly = false;
        ApplicationInfo info1 = null;
        ApplicationInfo info2 = null;

        try {
            info1 = mPm.getApplicationInfo(pkgName, 0);
        } catch (NameNotFoundException e) {
        }
        dataOnly = (info1 == null) && (mAppInfo != null);
        if (dataOnly) {
            mMoveAppButton.setText(R.string.move_app);
        } else if ((mAppInfo.flags & ApplicationInfo.FLAG_ON_SDCARD) != 0) {
            mMoveAppButton.setText(R.string.move_app_to_internal);
        } else {
            mMoveAppButton.setText(R.string.move_app_to_sdcard);
        }
    }

    private void initControlButtons() {
        mUpdatedSysApp = (mAppInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
        boolean enabled = true;
        if (mUpdatedSysApp) {
            mUninstallButton.setText(R.string.app_factory_reset);
        } else if ((mAppInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0){
            mUninstallButton.setText(R.string.uninstall_text);
        } else {
            // Disable uninstall for system apps
            enabled = false;
        }
        mUninstallButton.setEnabled(enabled);
        if (enabled) {
            // Register listener
            mUninstallButton.setOnClickListener(this);
        }
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> rList = am.getRunningAppProcesses();
        boolean running = false;
        if (rList != null) {
            for (RunningAppProcessInfo info : rList) {
                if (info.pkgList != null) {
                    for (String rpkg : info.pkgList) {
                        if (rpkg.equals(mAppInfo.packageName)) {
                            running = true;
                            break;
                        }
                    }
                }
            }
        }
        mForceStopButton.setEnabled(running);
        if (running) {
            mForceStopButton.setOnClickListener(this);
        }
    }

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        // Get package manager
        mPm = getPackageManager();
        // Get application's name from intent
        Intent intent = getIntent();
        final String packageName = intent.getStringExtra(ManageApplications.APP_PKG_NAME);
        mComputingStr = getText(R.string.computing_size);
        // Try retrieving package stats again
        CharSequence totalSizeStr, appSizeStr, dataSizeStr;
        totalSizeStr = appSizeStr = dataSizeStr = mComputingStr;
        if(localLOGV) Log.i(TAG, "Have to compute package sizes");
        mSizeObserver = new PkgSizeObserver();
        try {
            mAppInfo = mPm.getApplicationInfo(packageName, 
                    PackageManager.GET_UNINSTALLED_PACKAGES);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Exception when retrieving package:"+packageName, e);
            showDialogInner(DLG_APP_NOT_FOUND);
            return;
        }
        setContentView(R.layout.installed_app_details);
        //TODO download str and download url
        // Set default values on sizes
        mTotalSize = (TextView)findViewById(R.id.total_size_text);
        mTotalSize.setText(totalSizeStr);
        mAppSize = (TextView)findViewById(R.id.application_size_text);
        mAppSize.setText(appSizeStr);
        mDataSize = (TextView)findViewById(R.id.data_size_text);
        mDataSize.setText(dataSizeStr);
        // Get Control button panel
        View btnPanel = findViewById(R.id.control_buttons_panel);
        mForceStopButton = (Button) btnPanel.findViewById(R.id.left_button);
        mForceStopButton.setText(R.string.force_stop);
        mUninstallButton = (Button)btnPanel.findViewById(R.id.right_button);
        initControlButtons();
        // Initialize clear data and move install location buttons
        View data_buttons_panel = findViewById(R.id.data_buttons_panel);
        mClearDataButton = (Button) data_buttons_panel.findViewById(R.id.left_button);
        mMoveAppButton = (Button) data_buttons_panel.findViewById(R.id.right_button);
        // Disable move for now
        mMoveAppButton.setEnabled(false);
         // Cache section
         mCacheSize = (TextView) findViewById(R.id.cache_size_text);
         mCacheSize.setText(mComputingStr);
         mClearCacheButton = (Button) findViewById(R.id.clear_cache_button);

         // Get list of preferred activities
         mActivitiesButton = (Button)findViewById(R.id.clear_activities_button);
         List<ComponentName> prefActList = new ArrayList<ComponentName>();
         // Intent list cannot be null. so pass empty list
         List<IntentFilter> intentList = new ArrayList<IntentFilter>();
         mPm.getPreferredActivities(intentList,  prefActList, packageName);
         if(localLOGV) Log.i(TAG, "Have "+prefActList.size()+" number of activities in prefered list");
         TextView autoLaunchView = (TextView)findViewById(R.id.auto_launch);
         if(prefActList.size() <= 0) {
             // Disable clear activities button
             autoLaunchView.setText(R.string.auto_launch_disable_text);
             mActivitiesButton.setEnabled(false);
         } else {
             autoLaunchView.setText(R.string.auto_launch_enable_text);
             mActivitiesButton.setOnClickListener(this);
         }
         
         // Security permissions section
         LinearLayout permsView = (LinearLayout) findViewById(R.id.permissions_section);
         AppSecurityPermissions asp = new AppSecurityPermissions(this, packageName);
         if(asp.getPermissionCount() > 0) {
             permsView.setVisibility(View.VISIBLE);
             // Make the security sections header visible
             LinearLayout securityList = (LinearLayout) permsView.findViewById(
                     R.id.security_settings_list);
             securityList.addView(asp.getPermissionsView());
         } else {
             permsView.setVisibility(View.GONE);
         }
    }
    
    private void refreshAppAttributes(PackageInfo pkgInfo) {
        setAppLabelAndIcon();
        initControlButtons();
        initDataButtons();
        // Version number of application
        setAppVersion(pkgInfo);
        // Refresh size info
        if (mAppInfo != null && mAppInfo.packageName != null) {
            mPm.getPackageSizeInfo(mAppInfo.packageName, mSizeObserver);
        }
    }
    
    // Utility method to set applicaiton label and icon.
    private void setAppLabelAndIcon() {
        ((ImageView)findViewById(R.id.app_icon)).setImageDrawable(mAppInfo.loadIcon(mPm));
        //set application name TODO version
        CharSequence appName = mAppInfo.loadLabel(mPm);
        if(appName == null) {
            appName = getString(_UNKNOWN_APP);
        }
        ((TextView)findViewById(R.id.app_name)).setText(appName);
    }
    
    // Utility method to set application version
    private void setAppVersion(PackageInfo pkgInfo) {
        // Version number of application
        mAppVersion = ((TextView)findViewById(R.id.app_version));
        if (pkgInfo != null) {
            mAppVersion.setVisibility(View.VISIBLE);
            mAppVersion.setText(getString(R.string.version_text,
                    String.valueOf(pkgInfo.versionName)));
        } else {
            mAppVersion.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        PackageInfo pkgInfo;
        // Get application info again to refresh changed properties of application
        try {
            mAppInfo = mPm.getApplicationInfo(mAppInfo.packageName, 
                    PackageManager.GET_UNINSTALLED_PACKAGES);
            pkgInfo = mPm.getPackageInfo(mAppInfo.packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Exception when retrieving package:" + mAppInfo.packageName, e);
            showDialogInner(DLG_APP_NOT_FOUND);
            return;
        }
        refreshAppAttributes(pkgInfo);
    }

    private void setIntentAndFinish(boolean finish, boolean appChanged) {
        if(localLOGV) Log.i(TAG, "appChanged="+appChanged);
        Intent intent = new Intent();
        intent.putExtra(ManageApplications.APP_CHG, appChanged);
        setResult(ManageApplications.RESULT_OK, intent);
        if(finish) {
            finish();
        }
    }
    
    /*
     * Private method to handle get size info notification from observer when
     * the async operation from PackageManager is complete. The current user data
     * info has to be refreshed in the manage applications screen as well as the current screen.
     */
    private void refreshSizeInfo(Message msg) {
        boolean changed = false;
        PackageStats newPs = msg.getData().getParcelable(ATTR_PACKAGE_STATS);
        long newTot = newPs.cacheSize+newPs.codeSize+newPs.dataSize;
        if(mSizeInfo == null) {
            mSizeInfo = newPs;
            String str = getSizeStr(newTot);
            mTotalSize.setText(str);
            mAppSize.setText(getSizeStr(newPs.codeSize));
            mDataSize.setText(getSizeStr(newPs.dataSize));
            mCacheSize.setText(getSizeStr(newPs.cacheSize));
        } else {
            long oldTot = mSizeInfo.cacheSize+mSizeInfo.codeSize+mSizeInfo.dataSize;
            if(newTot != oldTot) {
                String str = getSizeStr(newTot);
                mTotalSize.setText(str);
                changed = true;
            }
            if(newPs.codeSize != mSizeInfo.codeSize) {
                mAppSize.setText(getSizeStr(newPs.codeSize));
                changed = true;
            }
            if(newPs.dataSize != mSizeInfo.dataSize) {
                mDataSize.setText(getSizeStr(newPs.dataSize));
                changed = true;
            }
            if(newPs.cacheSize != mSizeInfo.cacheSize) {
                mCacheSize.setText(getSizeStr(newPs.cacheSize));
                changed = true;
            }
            if(changed) {
                mSizeInfo = newPs;
            }
        }
        // If data size is zero disable clear data button
        if (newPs.dataSize == 0) {
            mClearDataButton.setEnabled(false);
        }
        long data = mSizeInfo.dataSize;
        refreshCacheInfo(newPs.cacheSize);
    }
    
    private void refreshCacheInfo(long cacheSize) {
        // Set cache info
        mCacheSize.setText(getSizeStr(cacheSize));
        if (cacheSize <= 0) {
            mClearCacheButton.setEnabled(false);
        } else {
            mClearCacheButton.setOnClickListener(this);
        }
    }
    
    /*
     * Private method to handle clear message notification from observer when
     * the async operation from PackageManager is complete
     */
    private void processClearMsg(Message msg) {
        int result = msg.arg1;
        String packageName = mAppInfo.packageName;
        mClearDataButton.setText(R.string.clear_user_data_text);
        if(result == OP_SUCCESSFUL) {
            Log.i(TAG, "Cleared user data for package : "+packageName);
            mPm.getPackageSizeInfo(packageName, mSizeObserver);
        } else {
            mClearDataButton.setEnabled(true);
        }
    }
    
    /*
     * Private method to initiate clearing user data when the user clicks the clear data 
     * button for a system package
     */
    private  void initiateClearUserData() {
        mClearDataButton.setEnabled(false);
        // Invoke uninstall or clear user data based on sysPackage
        String packageName = mAppInfo.packageName;
        Log.i(TAG, "Clearing user data for package : " + packageName);
        if(mClearDataObserver == null) {
            mClearDataObserver = new ClearUserDataObserver();
        }
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        boolean res = am.clearApplicationUserData(packageName, mClearDataObserver);
        if(!res) {
            // Clearing data failed for some obscure reason. Just log error for now
            Log.i(TAG, "Couldnt clear application user data for package:"+packageName);
            showDialogInner(DLG_CANNOT_CLEAR_DATA);
        } else {
            mClearDataButton.setText(R.string.recompute_size);
        }
    }
    
    private void showDialogInner(int id) {
        //removeDialog(id);
        showDialog(id);
    }
    
    @Override
    public Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
        case DLG_CLEAR_DATA:
            return new AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_data_dlg_title))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setMessage(getString(R.string.clear_data_dlg_text))
            .setPositiveButton(R.string.dlg_ok,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // Clear user data here
                    initiateClearUserData();
                }
            })
            .setNegativeButton(R.string.dlg_cancel, null)
            .create();
        case DLG_FACTORY_RESET:
            return new AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_factory_reset_dlg_title))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setMessage(getString(R.string.app_factory_reset_dlg_text))
            .setPositiveButton(R.string.dlg_ok,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // Clear user data here
                    uninstallPkg(mAppInfo.packageName);
                }
            })
            .setNegativeButton(R.string.dlg_cancel, null)
            .create();
        case DLG_APP_NOT_FOUND:
            return new AlertDialog.Builder(this)
            .setTitle(getString(R.string.app_not_found_dlg_title))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setMessage(getString(R.string.app_not_found_dlg_title))
            .setNeutralButton(getString(R.string.dlg_ok),
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    //force to recompute changed value
                    setIntentAndFinish(true, true);
                }
            })
            .create();
        case DLG_CANNOT_CLEAR_DATA:
            return new AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_failed_dlg_title))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setMessage(getString(R.string.clear_failed_dlg_text))
            .setNeutralButton(R.string.dlg_ok,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mClearDataButton.setEnabled(false);
                    //force to recompute changed value
                    setIntentAndFinish(false, false);
                }
            })
            .create();
            case DLG_FORCE_STOP:
                return new AlertDialog.Builder(this)
                .setTitle(getString(R.string.force_stop_dlg_title))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(getString(R.string.force_stop_dlg_text))
                .setPositiveButton(R.string.dlg_ok,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // Force stop
                    forceStopPackage(mAppInfo.packageName);
                }
            })
            .setNegativeButton(R.string.dlg_cancel, null)
            .create();
        }
        return null;
    }

    private void uninstallPkg(String packageName) {
         // Create new intent to launch Uninstaller activity
        Uri packageURI = Uri.parse("package:"+packageName);
        Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
        startActivity(uninstallIntent);
        setIntentAndFinish(true, true);
    }

    private void forceStopPackage(String pkgName) {
        ActivityManager am = (ActivityManager)getSystemService(
                Context.ACTIVITY_SERVICE);
        am.forceStopPackage(pkgName);
    }

    /*
     * Method implementing functionality of buttons clicked
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    public void onClick(View v) {
        String packageName = mAppInfo.packageName;
        if(v == mUninstallButton) {
            if (mUpdatedSysApp) {
                showDialogInner(DLG_FACTORY_RESET);
            } else {
                uninstallPkg(packageName);
            }
        } else if(v == mActivitiesButton) {
            mPm.clearPackagePreferredActivities(packageName);
            mActivitiesButton.setEnabled(false);
        } else if(v == mClearDataButton) {
            if (mAppInfo.manageSpaceActivityName != null) {
                Intent intent = new Intent(Intent.ACTION_DEFAULT);
                intent.setClassName(mAppInfo.packageName, mAppInfo.manageSpaceActivityName);
                startActivityForResult(intent, -1);
            } else {
                showDialogInner(DLG_CLEAR_DATA);
            }
        } else if (v == mClearCacheButton) {
            // Lazy initialization of observer
            if (mClearCacheObserver == null) {
                mClearCacheObserver = new ClearCacheObserver();
            }
            mPm.deleteApplicationCacheFiles(packageName, mClearCacheObserver);
        } else if (v == mForceStopButton) {
            // TODO Once framework supports a pre-broadcast to
            // actually find out if an application will be restarted right
            // after its killed, there is no way we can determine if
            // a force stop will really kill the app. So just ignore
            // the dialog for now and force stop the package. Please note
            // that the button cannot be enabled or disabled since
            // we do not have this info for now.
            forceStopPackage(mAppInfo.packageName);
        }
    }
}

