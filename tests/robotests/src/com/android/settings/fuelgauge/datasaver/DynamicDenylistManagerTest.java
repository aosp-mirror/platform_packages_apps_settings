/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.fuelgauge.datasaver;

import static android.net.NetworkPolicyManager.POLICY_NONE;
import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class DynamicDenylistManagerTest {

    private static final String FAKE_UID_1 = "package_uid_1";
    private static final String FAKE_UID_2 = "package_uid_2";

    private SharedPreferences mManualDenyListPref;
    private SharedPreferences mDynamicDenyListPref;
    private DynamicDenylistManager mDynamicDenylistManager;
    private Context mContext;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application.getApplicationContext();
        mDynamicDenylistManager = new DynamicDenylistManager(mContext);
        mManualDenyListPref = mDynamicDenylistManager.getManualDenylistPref();
        mDynamicDenyListPref = mDynamicDenylistManager.getDynamicDenylistPref();
    }

    @After
    public void tearDown() {
        mDynamicDenylistManager.clearManualDenylistPref();
        mDynamicDenylistManager.clearDynamicDenylistPref();
    }

    @Test
    public void getManualDenylistPref_isEmpty() {
        assertThat(mManualDenyListPref.getAll()).isEmpty();
    }

    @Test
    public void getDynamicDenylistPref_isEmpty() {
        assertThat(mDynamicDenyListPref.getAll()).isEmpty();
    }

    @Test
    public void getManualDenylistPref_initiated_containsExpectedValue() {
        mManualDenyListPref.edit().putInt(FAKE_UID_1, POLICY_REJECT_METERED_BACKGROUND).apply();

        assertThat(mManualDenyListPref.getAll().size()).isEqualTo(1);
        assertTrue(mManualDenyListPref.contains(FAKE_UID_1));
    }

    @Test
    public void getDynamicDenylistPref_initiated_containsExpectedValue() {
        mDynamicDenyListPref.edit().putInt(FAKE_UID_1, POLICY_REJECT_METERED_BACKGROUND).apply();

        assertThat(mDynamicDenyListPref.getAll()).hasSize(1);
        assertTrue(mDynamicDenyListPref.contains(FAKE_UID_1));
    }

    @Test
    public void updateManualDenylist_policyReject_addsUid() {
        mDynamicDenylistManager.updateManualDenylist(FAKE_UID_1, POLICY_REJECT_METERED_BACKGROUND);

        assertThat(mManualDenyListPref.getAll()).hasSize(1);
        assertTrue(mManualDenyListPref.contains(FAKE_UID_1));
    }

    @Test
    public void updateManualDenylist_policyNone_removesUid() {
        mManualDenyListPref.edit().putInt(FAKE_UID_1, POLICY_REJECT_METERED_BACKGROUND).apply();
        assertTrue(mManualDenyListPref.contains(FAKE_UID_1));

        mDynamicDenylistManager.updateManualDenylist(FAKE_UID_1, POLICY_NONE);

        assertThat(mManualDenyListPref.getAll()).isEmpty();
    }

    @Test
    public void updateManualDenylist_samePolicy_doNothing() {
        mManualDenyListPref.edit().putInt(FAKE_UID_1, POLICY_REJECT_METERED_BACKGROUND).apply();
        assertTrue(mManualDenyListPref.contains(FAKE_UID_1));

        mDynamicDenylistManager.updateManualDenylist(FAKE_UID_1, POLICY_REJECT_METERED_BACKGROUND);

        assertThat(mManualDenyListPref.getAll()).hasSize(1);
    }

    @Test
    public void isManualDenylist_returnsFalse() {
        assertFalse(mDynamicDenylistManager.isInManualDenylist(FAKE_UID_1));
    }

    @Test
    public void isManualDenylist_incorrectUid_returnsFalse() {
        mManualDenyListPref.edit().putInt(FAKE_UID_2, POLICY_REJECT_METERED_BACKGROUND).apply();

        assertFalse(mDynamicDenylistManager.isInManualDenylist(FAKE_UID_1));
    }

    @Test
    public void isManualDenylist_initiated_returnsTrue() {
        mManualDenyListPref.edit().putInt(FAKE_UID_1, POLICY_REJECT_METERED_BACKGROUND).apply();

        assertTrue(mDynamicDenylistManager.isInManualDenylist(FAKE_UID_1));
    }

    @Test
    public void clearManualDenylistPref_isEmpty() {
        mManualDenyListPref.edit().putInt(FAKE_UID_1, POLICY_REJECT_METERED_BACKGROUND).apply();
        assertThat(mManualDenyListPref.getAll()).hasSize(1);
        assertTrue(mManualDenyListPref.contains(FAKE_UID_1));

        mDynamicDenylistManager.clearManualDenylistPref();

        assertThat(mManualDenyListPref.getAll()).isEmpty();
    }

    @Test
    public void clearDynamicDenylistPref_isEmpty() {
        mDynamicDenyListPref.edit().putInt(FAKE_UID_1, POLICY_REJECT_METERED_BACKGROUND).apply();
        assertThat(mDynamicDenyListPref.getAll()).hasSize(1);
        assertTrue(mDynamicDenyListPref.contains(FAKE_UID_1));

        mDynamicDenylistManager.clearDynamicDenylistPref();

        assertThat(mDynamicDenyListPref.getAll()).isEmpty();
    }
}
