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
package com.android.settings.fuelgauge.anomaly.action;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.android.settings.fuelgauge.anomaly.Anomaly;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class LocationCheckActionTest {
    private static final String PACKAGE_NAME = "com.android.chrome";

    private Context mContext;
    private LocationCheckAction mLocationCheckAction;
    private Anomaly mAnomaly;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        mLocationCheckAction = new LocationCheckAction(mContext);

        mAnomaly = new Anomaly.Builder()
                .setUid(getPackageUid(mContext, PACKAGE_NAME))
                .setPackageName(PACKAGE_NAME)
                .build();
    }

    @Test
    public void testRevokeAndCheck() {
        mLocationCheckAction.handlePositiveAction(mAnomaly, 0 /* metric key */);

        assertThat(mLocationCheckAction.isActionActive(mAnomaly)).isFalse();
    }

    private int getPackageUid(Context context, String packageName) {
        try {
            return context.getPackageManager().getPackageUid(packageName,
                    PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }
}




