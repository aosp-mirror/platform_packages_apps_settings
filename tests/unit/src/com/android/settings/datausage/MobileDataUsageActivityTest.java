/*
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

package com.android.settings;

import static junit.framework.Assert.assertEquals;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.NetworkTemplate;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MobileDataUsageActivityTest {
    private static final String TAG = "MobileDataUsageTest";
    @Test
    public void test_mobileDataUsageIntent() {
        final Context context = InstrumentationRegistry.getTargetContext();
        final PackageManager packageManager = context.getPackageManager();
        final int subId = SubscriptionManager.getDefaultSubscriptionId();
        final NetworkTemplate template = getNetworkTemplate(context, subId);

        Intent intent = new Intent(android.provider.Settings.ACTION_MOBILE_DATA_USAGE);
        intent.putExtra(android.provider.Settings.EXTRA_NETWORK_TEMPLATE, template);
        intent.putExtra(android.provider.Settings.EXTRA_SUB_ID, subId);

        assertEquals(packageManager.queryIntentActivities(intent, 0).size(), 1);

        context.startActivity(intent);
        // Should exit gracefully without crashing.
    }

    private NetworkTemplate getNetworkTemplate(Context context, int subId) {
        TelephonyManager tm = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        NetworkTemplate mobileAll = NetworkTemplate.buildTemplateMobileAll(
                tm.getSubscriberId(subId));
        return NetworkTemplate.normalize(mobileAll,
                tm.getMergedSubscriberIds());
    }
}
