

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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
public class InstalledAppDetails extends Activity implements View.OnClickListener, DialogInterface.OnClickListener  {
    private static final String TAG="InstalledAppDetails";
    private static final int _UNKNOWN_APP=R.string.unknown;
    private ApplicationInfo mAppInfo;
    private Button mAppButton;
    private Button mActivitiesButton;
    private boolean mCanUninstall;
    private boolean localLOGV=Config.LOGV || false;
    private TextView mAppSnippetSize;
    private TextView mTotalSize;
    private TextView mAppSize;
    private TextView mDataSize;
    private PkgSizeObserver mSizeObserver;
    private ClearUserDataObserver mClearDataObserver;
    // Views related to cache info
    private View mCachePanel;
    private TextView mCacheSize;
    private Button mClearCacheButton;
    private ClearCacheObserver mClearCacheObserver;
    private Button mForceStopButton;
    
    PackageStats mSizeInfo;
    private Button mManageSpaceButton;
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
    private CharSequence mAppButtonText;
    
    // Possible btn states
    private enum AppButtonStates {
        CLEAR_DATA,
        UNINSTALL,
        NONE
    } 
    private AppButtonStates mAppButtonState;
    
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
    
    private boolean isUninstallable() {
        if (((mAppInfo.flags&ApplicationInfo.FLAG_SYSTEM) != 0) && 
                ((mAppInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0)) {
            return false;
        }
        return true;
    }
    
    class ClearUserDataObserver extends IPackageDataObserver.Stub {
       public void onRemoveCompleted(final String packageName, final boolean succeeded) {
           final Message msg = mHandler.obtainMessage(CLEAR_USER_DATA);
           msg.arg1 = succeeded?OP_SUCCESSFUL:OP_FAILED;
           mHandler.sendMessage(msg);
        }
    }
    
    class PkgSizeObserver extends IPackageStatsObserver.Stub {
        public int idx;
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
    
    private void setAppBtnState() {
        boolean visible = false;
        if(mCanUninstall) {
            //app can clear user data
            if((mAppInfo.flags & ApplicationInfo.FLAG_ALLOW_CLEAR_USER_DATA) 
                    == ApplicationInfo.FLAG_ALLOW_CLEAR_USER_DATA) {
                mAppButtonText = getText(R.string.clear_user_data_text);
               mAppButtonState = AppButtonStates.CLEAR_DATA;
               visible = true;
            } else {
                //hide button if diableClearUserData is set
                visible = false;
                mAppButtonState = AppButtonStates.NONE;
            }
        } else {
            visible = true;
            mAppButtonState = AppButtonStates.UNINSTALL;
            mAppButtonText = getText(R.string.uninstall_text);
        }
        if(visible) {
            mAppButton.setText(mAppButtonText);
            mAppButton.setVisibility(View.VISIBLE);
        } else {
            mAppButton.setVisibility(View.GONE);
        }
    }
    
    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        //get package manager
        mPm = getPackageManager();
        //get application's name from intent
        Intent intent = getIntent();
        final String packageName = intent.getStringExtra(ManageApplications.APP_PKG_NAME);
        mComputingStr = getText(R.string.computing_size);
        // Try retrieving package stats again
        CharSequence totalSizeStr, appSizeStr, dataSizeStr;
        totalSizeStr = appSizeStr = dataSizeStr = mComputingStr;
        if(localLOGV) Log.i(TAG, "Have to compute package sizes");
        mSizeObserver = new PkgSizeObserver();
        mPm.getPackageSizeInfo(packageName, mSizeObserver);

        try {
            mAppInfo = mPm.getApplicationInfo(packageName, 
                    PackageManager.GET_UNINSTALLED_PACKAGES);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Exception when retrieving package:"+packageName, e);
            displayErrorDialog(R.string.app_not_found_dlg_text, true, true);
        }
        setContentView(R.layout.installed_app_details);       
        ((ImageView)findViewById(R.id.app_icon)).setImageDrawable(mAppInfo.loadIcon(mPm));
        //set application name TODO version
        CharSequence appName = mAppInfo.loadLabel(mPm);
        if(appName == null) {
            appName = getString(_UNKNOWN_APP);
        }
        ((TextView)findViewById(R.id.app_name)).setText(appName);
        mAppSnippetSize = ((TextView)findViewById(R.id.app_size));
        mAppSnippetSize.setText(totalSizeStr);
        //TODO download str and download url
        //set values on views
        mTotalSize = (TextView)findViewById(R.id.total_size_text);
        mTotalSize.setText(totalSizeStr);
        mAppSize = (TextView)findViewById(R.id.application_size_text);
        mAppSize.setText(appSizeStr);
        mDataSize = (TextView)findViewById(R.id.data_size_text);
        mDataSize.setText(dataSizeStr);
         
         mAppButton = ((Button)findViewById(R.id.uninstall_button));
        //determine if app is a system app
         mCanUninstall = !isUninstallable();
         if(localLOGV) Log.i(TAG, "Is systemPackage "+mCanUninstall);
         setAppBtnState();
         mManageSpaceButton = (Button)findViewById(R.id.manage_space_button);
         if(mAppInfo.manageSpaceActivityName != null) {
             mManageSpaceButton.setVisibility(View.VISIBLE);
             mManageSpaceButton.setOnClickListener(this);
         }
         
         // Cache section
         mCachePanel = findViewById(R.id.cache_panel);
         mCacheSize = (TextView) findViewById(R.id.cache_size_text);
         mCacheSize.setText(mComputingStr);
         mClearCacheButton = (Button) findViewById(R.id.clear_cache_button);
         mForceStopButton = (Button) findViewById(R.id.force_stop_button);
         mForceStopButton.setOnClickListener(this);
         
         //clear activities
         mActivitiesButton = (Button)findViewById(R.id.clear_activities_button);
         List<ComponentName> prefActList = new ArrayList<ComponentName>();
         //intent list cannot be null. so pass empty list
         List<IntentFilter> intentList = new ArrayList<IntentFilter>();
         mPm.getPreferredActivities(intentList,  prefActList, packageName);
         if(localLOGV) Log.i(TAG, "Have "+prefActList.size()+" number of activities in prefered list");
         TextView autoLaunchView = (TextView)findViewById(R.id.auto_launch);
         if(prefActList.size() <= 0) {
             //disable clear activities button
             autoLaunchView.setText(R.string.auto_launch_disable_text);
             mActivitiesButton.setEnabled(false);
         } else {
             autoLaunchView.setText(R.string.auto_launch_enable_text);
             mActivitiesButton.setOnClickListener(this);
         }
         
         // security permissions section
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
    
    private void displayErrorDialog(int msgId, final boolean finish, final boolean changed) {
        //display confirmation dialog
        new AlertDialog.Builder(this)
        .setTitle(getString(R.string.app_not_found_dlg_title))
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setMessage(getString(msgId))
        .setNeutralButton(getString(R.string.dlg_ok), 
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //force to recompute changed value
                        setIntentAndFinish(finish, changed);
                    }
                }
        )
        .show();
    }
    
    private void setIntentAndFinish(boolean finish, boolean appChanged) {
        if(localLOGV) Log.i(TAG, "appChanged="+appChanged);
        Intent intent = new Intent();
        intent.putExtra(ManageApplications.APP_CHG, appChanged);
        setResult(ManageApplications.RESULT_OK, intent);
        mAppButton.setEnabled(false);
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
            mAppSnippetSize.setText(str);
            mAppSize.setText(getSizeStr(newPs.codeSize));
            mDataSize.setText(getSizeStr(newPs.dataSize));
            mCacheSize.setText(getSizeStr(newPs.cacheSize));
        } else {
            long oldTot = mSizeInfo.cacheSize+mSizeInfo.codeSize+mSizeInfo.dataSize;
            if(newTot != oldTot) {
                String str = getSizeStr(newTot);
                mTotalSize.setText(str);
                mAppSnippetSize.setText(str);
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
        
        long data = mSizeInfo.dataSize;
        // Disable button if data is 0
        if(mAppButtonState != AppButtonStates.NONE){
            mAppButton.setText(mAppButtonText);
            if((mAppButtonState == AppButtonStates.CLEAR_DATA) && (data == 0)) {
                mAppButton.setEnabled(false);
            } else {
                mAppButton.setEnabled(true);
                mAppButton.setOnClickListener(this);
            }            
        }
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
        if(result == OP_SUCCESSFUL) {
            Log.i(TAG, "Cleared user data for system package:"+packageName);
            mPm.getPackageSizeInfo(packageName, mSizeObserver);
        } else {
            mAppButton.setText(R.string.clear_user_data_text);
            mAppButton.setEnabled(true);
        }
    }
    
    /*
     * Private method to initiate clearing user data when the user clicks the clear data 
     * button for a system package
     */
    private  void initiateClearUserDataForSysPkg() {
        mAppButton.setEnabled(false);
        //invoke uninstall or clear user data based on sysPackage
        String packageName = mAppInfo.packageName;
        Log.i(TAG, "Clearing user data for system package");
        if(mClearDataObserver == null) {
            mClearDataObserver = new ClearUserDataObserver();
        }
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        boolean res = am.clearApplicationUserData(packageName, mClearDataObserver);
        if(!res) {
            //doesnt initiate clear. some error. should not happen but just log error for now
            Log.i(TAG, "Couldnt clear application user data for package:"+packageName);
            displayErrorDialog(R.string.clear_data_failed, false, false);
        } else {
                mAppButton.setText(R.string.recompute_size);
        }
    }
    
    /*
     * Method implementing functionality of buttons clicked
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    public void onClick(View v) {
        String packageName = mAppInfo.packageName;
        if(v == mAppButton) {
            if(mCanUninstall) {
                //display confirmation dialog
                new AlertDialog.Builder(this)
                .setTitle(getString(R.string.clear_data_dlg_title))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(getString(R.string.clear_data_dlg_text))
                .setPositiveButton(R.string.dlg_ok, this)
                .setNegativeButton(R.string.dlg_cancel, this)
                .show();
            } else {
                //create new intent to launch Uninstaller activity
                Uri packageURI = Uri.parse("package:"+packageName);
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                startActivity(uninstallIntent);
                setIntentAndFinish(true, true);
            }
        } else if(v == mActivitiesButton) {
            mPm.clearPackagePreferredActivities(packageName);
            mActivitiesButton.setEnabled(false);
        } else if(v == mManageSpaceButton) {
            Intent intent = new Intent(Intent.ACTION_DEFAULT);
            intent.setClassName(mAppInfo.packageName, mAppInfo.manageSpaceActivityName);
            startActivityForResult(intent, -1);
        } else if (v == mClearCacheButton) {
            // Lazy initialization of observer
            if (mClearCacheObserver == null) {
                mClearCacheObserver = new ClearCacheObserver();
            }
            mPm.deleteApplicationCacheFiles(packageName, mClearCacheObserver);
        } else if (v == mForceStopButton) {
            ActivityManager am = (ActivityManager)getSystemService(
                    Context.ACTIVITY_SERVICE);
            am.restartPackage(packageName);
        }
    }

    public void onClick(DialogInterface dialog, int which) {
        if(which == AlertDialog.BUTTON_POSITIVE) {
            //invoke uninstall or clear user data based on sysPackage
            initiateClearUserDataForSysPkg();
        } else {
            //cancel do nothing just retain existing screen
        }
    }
}

