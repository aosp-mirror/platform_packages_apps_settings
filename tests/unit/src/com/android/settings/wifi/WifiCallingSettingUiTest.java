/**
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.wifi;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withResourceName;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anything;
import static org.junit.Assert.assertEquals;
import static org.junit.matchers.JUnitMatchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.support.test.uiautomator.UiDevice;
import android.telephony.SubscriptionInfo;
import android.telephony.ims.ImsMmTelManager;

import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.ViewInteraction;
import androidx.test.runner.AndroidJUnit4;

import com.android.ims.ImsManager;
import com.android.internal.telephony.SubscriptionController;
import com.android.settings.testutils.MockedServiceManager;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class WifiCallingSettingUiTest {
    private static final String SUBSCRIPTION0_NAME = "SUB0";
    private static final String SUBSCRIPTION1_NAME = "SUB1";
    private static final String WFC_MODE_TITLE = "Calling preference";
    private static final String WFC_MODE_WIFI_ONLY = "Wi-Fi only";
    private static final String WFC_MODE_WIFI_PREFERRED = "Wi-Fi preferred";
    private static final String WFC_MODE_CELLULAR_PREFERRED = "Mobile preferred";

    private Instrumentation mInstrumentation;
    private Context mContext;
    private UiDevice mDevice;
    @Mock
    SubscriptionController mSubscriptionController;
    MockedServiceManager mMockedServiceManager;
    protected HashMap<Integer, ImsManager> mImsManagerInstances = new HashMap<>();
    List<SubscriptionInfo> mSils = new ArrayList();
    @Mock
    SubscriptionInfo mSubscriptionInfo0;
    @Mock
    SubscriptionInfo mSubscriptionInfo1;
    @Mock
    ImsManager mImsManager0;
    @Mock
    ImsManager mImsManager1;
    @Mock
    ImsMmTelManager mImsMmTelManager0;
    @Mock
    ImsMmTelManager mImsMmTelManager1;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mContext = mInstrumentation.getTargetContext();
        mDevice = UiDevice.getInstance(mInstrumentation);

        mMockedServiceManager = new MockedServiceManager();
        mMockedServiceManager.replaceService("isub", mSubscriptionController);

        mMockedServiceManager.replaceInstance(
                ImsManager.class, "sImsManagerInstances", null, mImsManagerInstances);
        mMockedServiceManager.replaceInstance(
                SubscriptionController.class, "sInstance", null, mSubscriptionController);
        doReturn(mSubscriptionController)
                .when(mSubscriptionController).queryLocalInterface(anyString());
        mImsManagerInstances.put(0, mImsManager0);
        mImsManagerInstances.put(1, mImsManager1);
        doReturn(mSils).when(mSubscriptionController).getActiveSubscriptionInfoList(anyString(),
                nullable(String.class));
        doReturn(0).when(mSubscriptionController).getPhoneId(0);
        doReturn(1).when(mSubscriptionController).getPhoneId(1);
        doReturn(0).when(mSubscriptionInfo0).getSubscriptionId();
        doReturn(1).when(mSubscriptionInfo1).getSubscriptionId();
        doReturn(0).when(mSubscriptionInfo0).getSimSlotIndex();
        doReturn(1).when(mSubscriptionInfo1).getSimSlotIndex();
        doReturn(SUBSCRIPTION0_NAME).when(mSubscriptionInfo0).getDisplayName();
        doReturn(SUBSCRIPTION1_NAME).when(mSubscriptionInfo1).getDisplayName();

        doReturn(true).when(mImsManager0).isWfcEnabledByPlatform();
        doReturn(true).when(mImsManager0).isNonTtyOrTtyOnVolteEnabled();
        doReturn(true).when(mImsManager1).isWfcEnabledByPlatform();
        doReturn(true).when(mImsManager1).isNonTtyOrTtyOnVolteEnabled();

        mDevice.wakeUp();
        mDevice.pressMenu();
    }

    @After
    public void tearDown() throws Exception {
        mMockedServiceManager.restoreAllServices();
    }

    @Test
    public void testSingleSimUi() throws InterruptedException {
        configureSingleSim();
        doReturn(true).when(mImsManager0).isWfcEnabledByUser();
        doReturn(ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED)
                .when(mImsMmTelManager0).getVoWiFiModeSetting();
        doReturn(ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED)
                .when(mImsMmTelManager0).getVoWiFiRoamingModeSetting();

        mInstrumentation.startActivitySync(createActivityIntent());

        checkSingleSimUi();

        try {
            mDevice.setOrientationLeft();
        } catch (Exception e) {
            Assert.fail("Exception " + e);
        }

        // Re-check after rotation. Fragment should be recreated properly.
        checkSingleSimUi();

        try {
            mDevice.setOrientationNatural();
        } catch (Exception e) {
            Assert.fail("Exception " + e);
        }

        // Re-check after rotation. Fragment should be resumed properly.
        checkSingleSimUi();
    }

    private void checkSingleSimUi() {
        assertEquals(false, checkExists(onView(withText(SUBSCRIPTION0_NAME))));
        assertEquals(false, checkExists(onView(withText(SUBSCRIPTION1_NAME))));
        assertEquals(true, checkExists(onView(withText(WFC_MODE_TITLE))));
        assertEquals(true, checkExists(onView(withText(WFC_MODE_WIFI_PREFERRED))));
        checkSwitchBarStatus(true, true);
        checkEmptyViewStatus(false);
    }

    @Test
    public void testNoValidSub() throws InterruptedException {
        configureDualSim();
        doReturn(false).when(mImsManager0).isWfcEnabledByPlatform();
        doReturn(false).when(mImsManager0).isNonTtyOrTtyOnVolteEnabled();
        doReturn(false).when(mImsManager1).isWfcEnabledByPlatform();
        doReturn(false).when(mImsManager1).isNonTtyOrTtyOnVolteEnabled();
        doReturn(false).when(mImsManager0).isWfcEnabledByUser();
        doReturn(ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED)
                .when(mImsMmTelManager0).getVoWiFiModeSetting();
        doReturn(ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED)
                .when(mImsMmTelManager0).getVoWiFiRoamingModeSetting();

        Activity activity = mInstrumentation.startActivitySync(createActivityIntent());

        assertEquals(false, checkExists(onView(withText(SUBSCRIPTION0_NAME))));
        assertEquals(false, checkExists(onView(withText(SUBSCRIPTION1_NAME))));
        assertEquals(false, checkExists(onView(withText(WFC_MODE_TITLE))));

        checkSwitchBarStatus(false, false);
        checkEmptyViewStatus(false);
    }

    @Test
    public void testWfcDisabled() throws InterruptedException {
        configureSingleSim();
        doReturn(false).when(mImsManager0).isWfcEnabledByUser();
        doReturn(ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED)
                .when(mImsMmTelManager0).getVoWiFiModeSetting();
        doReturn(ImsMmTelManager.WIFI_MODE_WIFI_PREFERRED)
                .when(mImsMmTelManager0).getVoWiFiRoamingModeSetting();

        Activity activity = mInstrumentation.startActivitySync(createActivityIntent());

        assertEquals(false, checkExists(onView(withText(SUBSCRIPTION0_NAME))));
        assertEquals(false, checkExists(onView(withText(SUBSCRIPTION1_NAME))));
        assertEquals(false, checkExists(onView(withText(WFC_MODE_TITLE))));

        checkSwitchBarStatus(true, false);
        checkEmptyViewStatus(true);
    }

    @Test
    public void testDualSimUi() throws InterruptedException {
        configureDualSim();
        doReturn(true).when(mImsManager0).isWfcEnabledByUser();
        doReturn(false).when(mImsManager1).isWfcEnabledByUser();
        doReturn(ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED)
                .when(mImsMmTelManager0).getVoWiFiModeSetting();
        doReturn(ImsMmTelManager.WIFI_MODE_CELLULAR_PREFERRED)
                .when(mImsMmTelManager0).getVoWiFiRoamingModeSetting();

        mInstrumentation.startActivitySync(createActivityIntent());

        assertEquals(true, checkExists(onView(withText(SUBSCRIPTION0_NAME))));
        assertEquals(true, checkExists(onView(withText(SUBSCRIPTION1_NAME))));
        assertEquals(true, checkExists(onView(withText(WFC_MODE_TITLE))));
        assertEquals(true, checkExists(onView(withText(WFC_MODE_CELLULAR_PREFERRED))));

        onView(withText(SUBSCRIPTION0_NAME)).check(matches(isSelected()));
        checkSwitchBarStatus(true, true);
        checkEmptyViewStatus(false);

        // Switch to SUB1.
        onView(withText(SUBSCRIPTION1_NAME)).perform(click());

        checkSwitchBarStatus(true, false);
        checkEmptyViewStatus(true);
        onView(withText(SUBSCRIPTION1_NAME)).check(matches(isSelected()));
    }

    private boolean checkExists(ViewInteraction v) {
        try {
            v.check(matches(isCompletelyDisplayed()));
            return true;
        } catch (NoMatchingViewException e) {
            return false;
        }
    }

    private Intent createActivityIntent() {
        Intent intent = new Intent(mContext,
                com.android.settings.Settings.WifiCallingSettingsActivity.class);
        intent.setPackage("com.android.settings");
        intent.setAction("android.intent.action.MAIN");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    private void configureSingleSim() {
        mSils.clear();
        mSils.add(mSubscriptionInfo0);
    }

    private void configureDualSim() {
        mSils.clear();
        mSils.add(mSubscriptionInfo0);
        mSils.add(mSubscriptionInfo1);
    }

    private void checkSwitchBarStatus(boolean shouldDisplay, boolean statusOn) {
        if (shouldDisplay) {
            try {
                onView(allOf(withResourceName("switch_text"), isCompletelyDisplayed()))
                        .check(matches(withText(containsString(statusOn ? "On" : "Off"))));
            } catch (Exception e) {
                Assert.fail("Exception " + e);
            }
        } else {
            onView(allOf(withResourceName("switch_text"), isCompletelyDisplayed()))
                    .check(doesNotExist());
        }
    }

    private void checkEmptyViewStatus(boolean shouldDisplay) {
        try {
            if (!shouldDisplay) {
                onView(allOf(withResourceName("empty"), isCompletelyDisplayed()))
                        .check(doesNotExist());
            } else {
                onView(allOf(withResourceName("empty"), isCompletelyDisplayed()))
                        .check(matches(anything()));
            }
        } catch (Exception e) {
            Assert.fail("Exception " + e);
        }
    }
}
