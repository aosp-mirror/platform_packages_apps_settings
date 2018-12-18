package com.android.settings.applications.specialaccess.financialapps;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import static android.Manifest.permission.SMS_FINANCIAL_TRANSACTIONS;
import static android.Manifest.permission.READ_SMS;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadow.api.Shadow;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
public class FinancialAppsControllerTest {
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private AppOpsManager mAppOpsManager;
    @Mock
    private PreferenceScreen mRoot;
    @Mock
    private Preference mPreference;

    private Context mContext;
    private PackageInfo mPackageInfoNoPermissionRequested;
    private PackageInfo mPackageInfoPermissionRequestedQPlus;
    private PackageInfo mPackageInfoPermissionRequestedPreQ;
    private FinancialAppsController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);
        when(mContext.getSystemService(AppOpsManager.class)).thenReturn(mAppOpsManager);

        initializePackageInfos();

        mController = new FinancialAppsController(mContext, "key");
        mController.displayPreference(mRoot);
    }

    private void initializePackageInfos() {
      mPackageInfoNoPermissionRequested = new PackageInfo();
      mPackageInfoNoPermissionRequested.applicationInfo = new ApplicationInfo();

      mPackageInfoPermissionRequestedQPlus = new PackageInfo();
      mPackageInfoPermissionRequestedQPlus.applicationInfo = new ApplicationInfo();
      // TODO(b/121161546): update after robolectric test support Q
      //mPackageInfoPermissionRequestedQPlus.applicationInfo.targetSdkVersion =
      //        Build.VERSION_CODES.Q;
      mPackageInfoPermissionRequestedQPlus.applicationInfo.uid = 2001;
      mPackageInfoPermissionRequestedQPlus.applicationInfo.nonLocalizedLabel = "QPLUS Package";
      mPackageInfoPermissionRequestedQPlus.packageName = "QPLUS";
      mPackageInfoPermissionRequestedQPlus.requestedPermissions =
              new String[] {SMS_FINANCIAL_TRANSACTIONS};

      mPackageInfoPermissionRequestedPreQ = new PackageInfo();
      mPackageInfoPermissionRequestedPreQ.applicationInfo = new ApplicationInfo();
      mPackageInfoPermissionRequestedPreQ.applicationInfo.targetSdkVersion = Build.VERSION_CODES.M;
      mPackageInfoPermissionRequestedPreQ.applicationInfo.uid = 2002;
      mPackageInfoPermissionRequestedPreQ.applicationInfo.nonLocalizedLabel = "PREQ Package";
      mPackageInfoPermissionRequestedPreQ.packageName = "PREQ";
      mPackageInfoPermissionRequestedPreQ.requestedPermissions = new String[] {READ_SMS};
    }

    @Test
    public void isAvailable_true() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void noPreferenceAddedWhenNoPackageRequestPermission() {
        when(mPackageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS))
            .thenReturn(new ArrayList<PackageInfo>(
                    Arrays.asList(mPackageInfoNoPermissionRequested)));
        mController.updateState(null);
        assertThat(mController.mRoot.getPreferenceCount()).isEqualTo(0);
    }

    //TODO(b/121161546): Add these tests after robolectric test support Q
    /*
    @Test
    public void preferenceAddedWhenPreQPackageRequestPermission() {
        when(mPackageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS))
            .thenReturn(new ArrayList<PackageInfo>(
                    Arrays.asList(mPackageInfoPermissionRequestedPreQ)));
        mController.updateState(null);
        assertThat(mController.mRoot.getPreferenceCount()).isEqualTo(1);
        SwitchPreference pref = (SwitchPreference) mController.mRoot.getPreference(0);
        assertThat(pref).isNotNull();
    }

    @Test
    public void preferenceAddedWhenQPlusPackageRequestPermission() {
        when(mPackageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS))
            .thenReturn(new ArrayList<PackageInfo>(
                    Arrays.asList(mPackageInfoPermissionRequestedQPlus)));
        mController.updateState(null);
        assertThat(mController.mRoot.getPreferenceCount()).isEqualTo(1);
        SwitchPreference pref = (SwitchPreference) mController.mRoot.getPreference(0);
        assertThat(pref).isNotNull();
        }*/
}
