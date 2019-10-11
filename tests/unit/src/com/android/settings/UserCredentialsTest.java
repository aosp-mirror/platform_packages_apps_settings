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

package com.android.settings;

import static com.android.settings.UserCredentialsSettings.Credential;

import android.os.Parcel;
import android.os.Process;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * User credentials settings fragment tests
 *
 * To run the test, use command:
 * adb shell am instrument -e class com.android.settings.UserCredentialsTest
 * -w com.android.settings.tests.unit/androidx.test.runner.AndroidJUnitRunner
 *
 */
public class UserCredentialsTest extends InstrumentationTestCase {
    private static final String TAG = "UserCredentialsTests";

    @SmallTest
    public void testCredentialIsParcelable() {
        final String alias = "credential-test-alias";
        Credential c = new Credential(alias, Process.SYSTEM_UID);

        c.storedTypes.add(Credential.Type.CA_CERTIFICATE);
        c.storedTypes.add(Credential.Type.USER_KEY);

        Parcel p = Parcel.obtain();
        c.writeToParcel(p, /* flags */ 0);
        p.setDataPosition(0);

        Credential r = Credential.CREATOR.createFromParcel(p);
        assertEquals(c.alias, r.alias);
        assertEquals(c.uid, r.uid);
        assertEquals(c.storedTypes, r.storedTypes);
    }
}
