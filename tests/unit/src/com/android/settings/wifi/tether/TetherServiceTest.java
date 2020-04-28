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

package com.android.settings.wifi.tether;

import static android.net.ConnectivityManager.EXTRA_ADD_TETHER_TYPE;
import static android.net.ConnectivityManager.EXTRA_PROVISION_CALLBACK;
import static android.net.ConnectivityManager.EXTRA_REM_TETHER_TYPE;
import static android.net.ConnectivityManager.EXTRA_RUN_PROVISION;
import static android.net.ConnectivityManager.EXTRA_SET_ALARM;
import static android.net.ConnectivityManager.TETHERING_BLUETOOTH;
import static android.net.ConnectivityManager.TETHERING_INVALID;
import static android.net.ConnectivityManager.TETHERING_USB;
import static android.net.ConnectivityManager.TETHERING_WIFI;
import static android.net.ConnectivityManager.TETHER_ERROR_NO_ERROR;
import static android.net.ConnectivityManager.TETHER_ERROR_PROVISION_FAILED;
import static android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.test.ServiceTestCase;
import android.util.Log;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TetherServiceTest extends ServiceTestCase<TetherService> {

    private static final String TAG = "TetherServiceTest";
    private static final String FAKE_PACKAGE_NAME = "com.some.package.name";
    private static final String ENTITLEMENT_PACKAGE_NAME = "com.some.entitlement.name";
    private static final String TEST_RESPONSE_ACTION = "testProvisioningResponseAction";
    private static final String TEST_NO_UI_ACTION = "testNoUiProvisioningRequestAction";
    private static final int BOGUS_RECEIVER_RESULT = -5;
    private static final int TEST_CHECK_PERIOD = 100;
    private static final int MS_PER_HOUR = 60 * 60 * 1000;
    private static final int SHORT_TIMEOUT = 100;
    private static final int PROVISION_TIMEOUT = 1000;

    private TetherService mService;
    private MockResources mResources;
    private MockTetherServiceWrapper mWrapper;
    int mLastReceiverResultCode = BOGUS_RECEIVER_RESULT;
    private int mLastTetherRequestType = TETHERING_INVALID;
    private int mProvisionResponse = BOGUS_RECEIVER_RESULT;
    private ProvisionReceiver mProvisionReceiver;
    private Receiver mResultReceiver;

    @Mock private AlarmManager mAlarmManager;
    @Mock private ConnectivityManager mConnectivityManager;
    @Mock private PackageManager mPackageManager;
    @Mock private WifiManager mWifiManager;
    @Mock private SharedPreferences mPrefs;
    @Mock private Editor mPrefEditor;
    @Captor private ArgumentCaptor<PendingIntent> mPiCaptor;
    @Captor private ArgumentCaptor<String> mStoredTypes;

    public TetherServiceTest() {
        super(TetherService.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        mResources = new MockResources();
        mContext = new TestContextWrapper(getContext());
        setContext(mContext);

        mResultReceiver = new Receiver(this);
        mLastReceiverResultCode = BOGUS_RECEIVER_RESULT;
        mProvisionResponse = Activity.RESULT_OK;
        mProvisionReceiver = new ProvisionReceiver();
        IntentFilter filter = new IntentFilter(TEST_NO_UI_ACTION);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        mContext.registerReceiver(mProvisionReceiver, filter);

        final String CURRENT_TYPES = "currentTethers";
        when(mPrefs.getString(CURRENT_TYPES, "")).thenReturn("");
        when(mPrefs.edit()).thenReturn(mPrefEditor);
        when(mPrefEditor.putString(eq(CURRENT_TYPES), mStoredTypes.capture())).thenReturn(
                mPrefEditor);
        mWrapper = new MockTetherServiceWrapper(mContext);

        ResolveInfo systemAppResolveInfo = new ResolveInfo();
        ActivityInfo systemActivityInfo = new ActivityInfo();
        systemActivityInfo.packageName = ENTITLEMENT_PACKAGE_NAME;
        ApplicationInfo systemAppInfo = new ApplicationInfo();
        systemAppInfo.flags |= ApplicationInfo.FLAG_SYSTEM;
        systemActivityInfo.applicationInfo = systemAppInfo;
        systemAppResolveInfo.activityInfo = systemActivityInfo;

        ResolveInfo nonSystemResolveInfo = new ResolveInfo();
        ActivityInfo nonSystemActivityInfo = new ActivityInfo();
        nonSystemActivityInfo.packageName = FAKE_PACKAGE_NAME;
        nonSystemActivityInfo.applicationInfo = new ApplicationInfo();
        nonSystemResolveInfo.activityInfo = nonSystemActivityInfo;

        List<ResolveInfo> resolvers = new ArrayList();
        resolvers.add(nonSystemResolveInfo);
        resolvers.add(systemAppResolveInfo);
        when(mPackageManager.queryBroadcastReceivers(
                any(Intent.class), eq(PackageManager.MATCH_ALL))).thenReturn(resolvers);
        setupService();
        getService().setTetherServiceWrapper(mWrapper);
    }

    @Override
    protected void tearDown() throws Exception {
        mContext.unregisterReceiver(mProvisionReceiver);
        super.tearDown();
    }

    private void cancelAllProvisioning() {
        int[] types = new int[]{TETHERING_BLUETOOTH, TETHERING_WIFI, TETHERING_USB};
        for (int type : types) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_REM_TETHER_TYPE, type);
            startService(intent);
        }
    }

    public void testStartForProvision() {
        runProvisioningForType(TETHERING_WIFI);

        assertTrue(waitForProvisionRequest(TETHERING_WIFI));
        assertTrue(waitForProvisionResponse(TETHER_ERROR_NO_ERROR));
    }

    public void testStartKeepsProvisionAppActive() {
        runProvisioningForType(TETHERING_WIFI);

        assertTrue(waitForProvisionRequest(TETHERING_WIFI));
        assertTrue(waitForProvisionResponse(TETHER_ERROR_NO_ERROR));
        assertFalse(mWrapper.isAppInactive(ENTITLEMENT_PACKAGE_NAME));
        // Non-system handler of the intent action should stay idle.
        assertTrue(mWrapper.isAppInactive(FAKE_PACKAGE_NAME));
    }

    public void testScheduleRechecks() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_ADD_TETHER_TYPE, TETHERING_WIFI);
        intent.putExtra(EXTRA_SET_ALARM, true);
        startService(intent);

        long period = TEST_CHECK_PERIOD * MS_PER_HOUR;
        verify(mAlarmManager).setRepeating(eq(AlarmManager.ELAPSED_REALTIME), anyLong(),
                eq(period), mPiCaptor.capture());
        PendingIntent pi = mPiCaptor.getValue();
        assertEquals(TetherService.class.getName(), pi.getIntent().getComponent().getClassName());
    }

    public void testStartMultiple() {
        runProvisioningForType(TETHERING_WIFI);

        assertTrue(waitForProvisionRequest(TETHERING_WIFI));
        assertTrue(waitForProvisionResponse(TETHER_ERROR_NO_ERROR));

        runProvisioningForType(TETHERING_USB);

        assertTrue(waitForProvisionRequest(TETHERING_USB));
        assertTrue(waitForProvisionResponse(TETHER_ERROR_NO_ERROR));

        runProvisioningForType(TETHERING_BLUETOOTH);

        assertTrue(waitForProvisionRequest(TETHERING_BLUETOOTH));
        assertTrue(waitForProvisionResponse(TETHER_ERROR_NO_ERROR));
    }

    public void testPersistTypes() {
        runProvisioningForType(TETHERING_WIFI);

        waitForProvisionRequest(TETHERING_WIFI);
        waitForProvisionResponse(TETHER_ERROR_NO_ERROR);

        runProvisioningForType(TETHERING_BLUETOOTH);

        waitForProvisionRequest(TETHERING_BLUETOOTH);
        waitForProvisionResponse(TETHER_ERROR_NO_ERROR);

        shutdownService();
        assertEquals(TETHERING_WIFI + "," + TETHERING_BLUETOOTH, mStoredTypes.getValue());
    }

    public void testFailureStopsTethering_Wifi() {
        mProvisionResponse = Activity.RESULT_CANCELED;

        runProvisioningForType(TETHERING_WIFI);

        assertTrue(waitForProvisionRequest(TETHERING_WIFI));
        assertTrue(waitForProvisionResponse(TETHER_ERROR_PROVISION_FAILED));

        verify(mConnectivityManager).stopTethering(ConnectivityManager.TETHERING_WIFI);
    }

    public void testFailureStopsTethering_Usb() {
        mProvisionResponse = Activity.RESULT_CANCELED;

        runProvisioningForType(TETHERING_USB);

        assertTrue(waitForProvisionRequest(TETHERING_USB));
        assertTrue(waitForProvisionResponse(TETHER_ERROR_PROVISION_FAILED));

        verify(mConnectivityManager).setUsbTethering(eq(false));
    }

    public void testCancelAlarm() {
        runProvisioningForType(TETHERING_WIFI);

        assertTrue(waitForProvisionRequest(TETHERING_WIFI));
        assertTrue(waitForProvisionResponse(TETHER_ERROR_NO_ERROR));

        Intent intent = new Intent();
        intent.putExtra(EXTRA_REM_TETHER_TYPE, TETHERING_WIFI);
        startService(intent);

        verify(mAlarmManager).cancel(mPiCaptor.capture());
        PendingIntent pi = mPiCaptor.getValue();
        assertEquals(TetherService.class.getName(), pi.getIntent().getComponent().getClassName());
    }

    public void testIgnoreOutdatedRequest() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_ADD_TETHER_TYPE, TETHERING_WIFI);
        intent.putExtra(EXTRA_RUN_PROVISION, true);
        intent.putExtra(EXTRA_PROVISION_CALLBACK, mResultReceiver);
        intent.putExtra(TetherService.EXTRA_SUBID, 1 /* Tested subId number */);
        startService(intent);

        SystemClock.sleep(PROVISION_TIMEOUT);
        assertEquals(TETHERING_INVALID, mLastTetherRequestType);
        assertTrue(mWrapper.isAppInactive(ENTITLEMENT_PACKAGE_NAME));
        assertTrue(mWrapper.isAppInactive(FAKE_PACKAGE_NAME));
    }

    private void runProvisioningForType(int type) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_ADD_TETHER_TYPE, type);
        intent.putExtra(EXTRA_RUN_PROVISION, true);
        intent.putExtra(EXTRA_PROVISION_CALLBACK, mResultReceiver);
        intent.putExtra(TetherService.EXTRA_SUBID, INVALID_SUBSCRIPTION_ID);
        startService(intent);
    }

    private boolean waitForAppInactive(UsageStatsManager usageStatsManager, String packageName) {
        long startTime = SystemClock.uptimeMillis();
        while (true) {
            if (usageStatsManager.isAppInactive(packageName)) {
                return true;
            }
            if ((SystemClock.uptimeMillis() - startTime) > PROVISION_TIMEOUT) {
                return false;
            }
            SystemClock.sleep(SHORT_TIMEOUT);
        }
    }

    private boolean waitForProvisionRequest(int expectedType) {
        long startTime = SystemClock.uptimeMillis();
        while (true) {
            if (mLastTetherRequestType == expectedType) {
                mLastTetherRequestType = TETHERING_INVALID;
                return true;
            }
            if ((SystemClock.uptimeMillis() - startTime) > PROVISION_TIMEOUT) {
                Log.v(TAG, String.format(
                        "waitForProvisionRequest timeout: expected=%d, actual=%d",
                        expectedType, mLastTetherRequestType));
                return false;
            }
            SystemClock.sleep(SHORT_TIMEOUT);
        }
    }

    private boolean waitForProvisionResponse(int expectedValue) {
        long startTime = SystemClock.uptimeMillis();
        while (true) {
            if (mLastReceiverResultCode == expectedValue) {
                mLastReceiverResultCode = BOGUS_RECEIVER_RESULT;
                return true;
            }
            if ((SystemClock.uptimeMillis() - startTime) > PROVISION_TIMEOUT) {
                Log.v(TAG, String.format(
                        "waitForProvisionResponse timeout: expected=%d, actual=%d",
                        expectedValue, mLastReceiverResultCode));
                return false;
            }
            SystemClock.sleep(SHORT_TIMEOUT);
        }
    }

    private static class MockResources extends android.test.mock.MockResources {
        @Override
        public int getInteger(int id) {
            switch(id) {
                case com.android.internal.R.integer.config_mobile_hotspot_provision_check_period:
                    return TEST_CHECK_PERIOD;
                default:
                    return 0;
            }
        }

        @Override
        public String getString(int id) {
            switch(id) {
                case com.android.internal.R.string.config_mobile_hotspot_provision_response:
                    return TEST_RESPONSE_ACTION;
                case com.android.internal.R.string.config_mobile_hotspot_provision_app_no_ui:
                    return TEST_NO_UI_ACTION;
                default:
                    return null;
            }
        }
    }

    private class TestContextWrapper extends ContextWrapper {

        public TestContextWrapper(Context base) {
            super(base);
        }

        @Override
        public Resources getResources() {
            return mResources;
        }

        @Override
        public SharedPreferences getSharedPreferences(String name, int mode) {
            // Stub out prefs to control the persisted tether type list.
            if (name == "tetherPrefs") {
                return mPrefs;
            }
            return super.getSharedPreferences(name, mode);
        }

        @Override
        public PackageManager getPackageManager() {
            return mPackageManager;
        }

        @Override
        public Object getSystemService(String name) {
            if (ALARM_SERVICE.equals(name)) {
                return mAlarmManager;
            } else if (CONNECTIVITY_SERVICE.equals(name)) {
                return mConnectivityManager;
            } else if (WIFI_SERVICE.equals(name)) {
                return mWifiManager;
            }

            return super.getSystemService(name);
        }
    }

    private static final class Receiver extends ResultReceiver {
        final WeakReference<TetherServiceTest> mTest;

        Receiver(TetherServiceTest test) {
            super(null);
            mTest = new WeakReference<TetherServiceTest>(test);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            TetherServiceTest test = mTest.get();
            if (test != null) {
                test.mLastReceiverResultCode = resultCode;
            }
        }
    };

    /**
     * Stubs out the provisioning app receiver.
     */
    private class ProvisionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mLastTetherRequestType = intent.getIntExtra("TETHER_TYPE", TETHERING_INVALID);
            sendResponse(mProvisionResponse, context);
        }

        private void sendResponse(int response, Context context) {
            Intent responseIntent = new Intent(TEST_RESPONSE_ACTION);
            responseIntent.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
            responseIntent.putExtra(TetherService.EXTRA_RESULT, response);
            context.sendBroadcast(
                    responseIntent, android.Manifest.permission.CONNECTIVITY_INTERNAL);
        }
    }

    private static class MockTetherServiceWrapper
            extends TetherService.TetherServiceWrapper {
        private final Set<String> mActivePackages;

        MockTetherServiceWrapper(Context context) {
            super(context);
            mActivePackages = new HashSet<>();
        }

        @Override
        void setAppInactive(String packageName, boolean isInactive) {
            if (!isInactive) {
                mActivePackages.add(packageName);
            } else {
                mActivePackages.remove(packageName);
            }
        }

        boolean isAppInactive(String packageName) {
            return !mActivePackages.contains(packageName);
        }

        @Override
        int getActiveDataSubscriptionId() {
            return INVALID_SUBSCRIPTION_ID;
        }
    }
}
