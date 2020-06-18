/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.settings.network;

import static android.net.TetheringConstants.EXTRA_ADD_TETHER_TYPE;
import static android.net.TetheringConstants.EXTRA_PROVISION_CALLBACK;
import static android.net.TetheringManager.TETHERING_WIFI;

import static com.android.settings.network.TetherProvisioningActivity.EXTRA_TETHER_SUBID;
import static com.android.settings.network.TetherProvisioningActivity.EXTRA_TETHER_UI_PROVISIONING_APP_NAME;
import static com.android.settings.network.TetherProvisioningActivity.PROVISION_REQUEST;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Activity;
import android.content.Intent;
import android.net.TetheringManager;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.provider.Settings;
import android.telephony.SubscriptionManager;

import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class TetherProvisioningActivityTest {
    private static class WrappedReceiver extends ResultReceiver {
        private final CompletableFuture<Integer> mFuture = new CompletableFuture<>();

        WrappedReceiver() {
            super(null /* handler */);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            mFuture.complete(resultCode);
        }

        public int get() throws Exception {
            return mFuture.get(10_000L, TimeUnit.MILLISECONDS);
        }
    }

    @Test
    public void testOnCreate_FinishWithNonActiveDataSubId() throws Exception {
        final WrappedReceiver receiver = new WrappedReceiver();
        try (ActivityScenario<TetherProvisioningActivity> scenario = ActivityScenario.launch(
                new Intent(Settings.ACTION_TETHER_PROVISIONING_UI)
                        .putExtra(EXTRA_ADD_TETHER_TYPE, TETHERING_WIFI)
                        .putExtra(EXTRA_PROVISION_CALLBACK, receiver)
                        .putExtra(TetherProvisioningActivity.EXTRA_TETHER_SUBID, 10000))) {
            assertEquals(TetheringManager.TETHER_ERROR_PROVISIONING_FAILED, receiver.get());
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void testOnCreate_FinishWithUnavailableProvisioningApp() throws Exception {
        final WrappedReceiver receiver = new WrappedReceiver();
        final int subId = SubscriptionManager.getActiveDataSubscriptionId();
        final String[] emptyProvisioningApp = { "", "" };
        try (ActivityScenario<TetherProvisioningActivity> scenario = ActivityScenario.launch(
                new Intent(Settings.ACTION_TETHER_PROVISIONING_UI)
                        .putExtra(EXTRA_ADD_TETHER_TYPE, TETHERING_WIFI)
                        .putExtra(EXTRA_PROVISION_CALLBACK, receiver)
                        .putExtra(EXTRA_TETHER_SUBID, subId)
                        .putExtra(EXTRA_TETHER_UI_PROVISIONING_APP_NAME, emptyProvisioningApp))) {
            assertEquals(TetheringManager.TETHER_ERROR_PROVISIONING_FAILED, receiver.get());
            assertEquals(Lifecycle.State.DESTROYED, scenario.getState());
        }
    }

    @Test
    public void testOnCreate_startActivityForResult() {
        final WrappedReceiver receiver = new WrappedReceiver();
        final int subId = SubscriptionManager.getActiveDataSubscriptionId();
        final String[] provisionApp = new String[] {
                "android.test.entitlement",
                "android.test.entitlement.InstrumentedEntitlementActivity"
        };
        try (ActivityScenario<TetherProvisioningActivity> scenario = ActivityScenario.launch(
                new Intent(Settings.ACTION_TETHER_PROVISIONING_UI)
                        .putExtra(EXTRA_ADD_TETHER_TYPE, TETHERING_WIFI)
                        .putExtra(EXTRA_PROVISION_CALLBACK, receiver)
                        .putExtra(EXTRA_TETHER_SUBID, subId)
                        .putExtra(EXTRA_TETHER_UI_PROVISIONING_APP_NAME, provisionApp))) {
            scenario.onActivity(activity -> {
                assertFalse(activity.isFinishing());
                activity.onActivityResult(PROVISION_REQUEST, Activity.RESULT_OK, null /* intent */);
                try {
                    assertEquals(TetheringManager.TETHER_ERROR_NO_ERROR, receiver.get());
                } catch (Exception e) {
                    // ActivityAction#perform() doesn't throw the exception. Just catch the
                    // exception and call fail() here.
                    fail("Can not get result after 10s.");
                }
                assertTrue(activity.isFinishing());
            });
        }
    }
}
