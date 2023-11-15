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

import static com.android.settings.Utils.SETTINGS_PACKAGE_NAME;
import static com.android.settings.fuelgauge.datasaver.DynamicDenylistManager.PREF_KEY_MANUAL_DENYLIST_SYNCED;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.NetworkPolicyManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class DynamicDenylistManagerTest {

    private static final int[] EMPTY_ARRAY = new int[]{};
    private static final String FAKE_UID_1 = "1001";
    private static final String FAKE_UID_2 = "1002";
    private static final int FAKE_UID_1_INT = Integer.parseInt(FAKE_UID_1);
    private static final int FAKE_UID_2_INT = Integer.parseInt(FAKE_UID_2);

    private SharedPreferences mManualDenyListPref;
    private SharedPreferences mDynamicDenyListPref;
    private DynamicDenylistManager mDynamicDenylistManager;

    @Mock
    private NetworkPolicyManager mNetworkPolicyManager;
    @Mock
    private PackageManager mPackageManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() {
        mDynamicDenylistManager.clearSharedPreferences();
    }

    @Test
    public void init_withoutExistedRejectPolicy_createWithExpectedValue() {
        initDynamicDenylistManager(EMPTY_ARRAY);

        assertThat(mManualDenyListPref.getAll()).hasSize(1);
        assertTrue(mManualDenyListPref.contains(PREF_KEY_MANUAL_DENYLIST_SYNCED));
    }

    @Test
    public void init_withExistedRejectPolicy_createWithExpectedValue() {
        initDynamicDenylistManager(new int[]{FAKE_UID_1_INT, FAKE_UID_2_INT});

        assertThat(mManualDenyListPref.getAll()).hasSize(3);
        assertTrue(mManualDenyListPref.contains(PREF_KEY_MANUAL_DENYLIST_SYNCED));
        assertTrue(mManualDenyListPref.contains(FAKE_UID_1));
        assertTrue(mManualDenyListPref.contains(FAKE_UID_2));
    }

    @Test
    public void getManualDenylistPref_initiated_containsExpectedValue() {
        initDynamicDenylistManager(EMPTY_ARRAY);

        setupPreference(mManualDenyListPref, FAKE_UID_1);

        assertTrue(mManualDenyListPref.contains(FAKE_UID_1));
    }

    @Test
    public void getDynamicDenylistPref_initiated_containsExpectedValue() {
        initDynamicDenylistManager(EMPTY_ARRAY);

        setupPreference(mDynamicDenyListPref, FAKE_UID_1);

        assertTrue(mDynamicDenyListPref.contains(FAKE_UID_1));
    }

    @Test
    public void updateManualDenylist_policyReject_addsUid() {
        initDynamicDenylistManager(EMPTY_ARRAY);

        mDynamicDenylistManager.updateDenylistPref(FAKE_UID_1_INT,
                POLICY_REJECT_METERED_BACKGROUND);

        assertTrue(mManualDenyListPref.contains(FAKE_UID_1));
    }

    @Test
    public void updateManualDenylist_policyNone_removesUid() {
        initDynamicDenylistManager(EMPTY_ARRAY);
        setupPreference(mManualDenyListPref, FAKE_UID_1);
        assertTrue(mManualDenyListPref.contains(FAKE_UID_1));

        mDynamicDenylistManager.updateDenylistPref(FAKE_UID_1_INT, POLICY_NONE);

        assertFalse(mManualDenyListPref.contains(FAKE_UID_1));
    }

    @Test
    public void updateManualDenylist_samePolicy_doNothing() {
        initDynamicDenylistManager(EMPTY_ARRAY);
        setupPreference(mManualDenyListPref, FAKE_UID_1);
        assertTrue(mManualDenyListPref.contains(FAKE_UID_1));
        assertThat(mManualDenyListPref.getAll()).hasSize(2);

        mDynamicDenylistManager.updateDenylistPref(FAKE_UID_1_INT,
                POLICY_REJECT_METERED_BACKGROUND);

        assertThat(mManualDenyListPref.getAll()).hasSize(2);
    }

    @Test
    public void setUidPolicyLocked_invokeSetUidPolicy() {
        initDynamicDenylistManager(EMPTY_ARRAY);

        mDynamicDenylistManager.setUidPolicyLocked(FAKE_UID_1_INT,
                POLICY_REJECT_METERED_BACKGROUND);

        assertTrue(mManualDenyListPref.contains(FAKE_UID_1));
        verify(mNetworkPolicyManager).setUidPolicy(eq(FAKE_UID_1_INT),
                eq(POLICY_REJECT_METERED_BACKGROUND));
    }

    @Test
    public void setDenylist_emptyListAndNoData_doNothing() {
        initDynamicDenylistManager(EMPTY_ARRAY);

        mDynamicDenylistManager.setDenylist(Collections.emptyList());

        verify(mNetworkPolicyManager, never()).setUidPolicy(anyInt(), anyInt());
    }

    @Test
    public void setDenylist_uidDeniedAlready_doNothing()
            throws PackageManager.NameNotFoundException {
        when(mPackageManager.getPackageUid(anyString(), eq(0))).thenReturn(FAKE_UID_1_INT);
        initDynamicDenylistManager(new int[]{FAKE_UID_1_INT});

        mDynamicDenylistManager.setDenylist(List.of(FAKE_UID_1));

        verify(mNetworkPolicyManager, never()).setUidPolicy(anyInt(), anyInt());
    }

    @Test
    public void setDenylist_sameList_doNothing() throws PackageManager.NameNotFoundException {
        when(mPackageManager.getPackageUid(eq(FAKE_UID_1), eq(0))).thenReturn(FAKE_UID_1_INT);
        when(mPackageManager.getPackageUid(eq(FAKE_UID_2), eq(0))).thenReturn(FAKE_UID_2_INT);
        initDynamicDenylistManager(EMPTY_ARRAY);
        setupPreference(mDynamicDenyListPref, FAKE_UID_2, FAKE_UID_1);

        mDynamicDenylistManager.setDenylist(List.of(FAKE_UID_1, FAKE_UID_2));

        verify(mNetworkPolicyManager, never()).setUidPolicy(anyInt(), anyInt());
    }

    @Test
    public void setDenylist_newListWithOldData_modifyPolicyNoneAndReject()
            throws PackageManager.NameNotFoundException {
        when(mPackageManager.getPackageUid(anyString(), eq(0))).thenReturn(
                Integer.parseInt(FAKE_UID_1));
        initDynamicDenylistManager(EMPTY_ARRAY);
        setupPreference(mDynamicDenyListPref, FAKE_UID_2);

        mDynamicDenylistManager.setDenylist(List.of(FAKE_UID_1));

        verify(mNetworkPolicyManager).setUidPolicy(FAKE_UID_2_INT, POLICY_NONE);
        verify(mNetworkPolicyManager).setUidPolicy(FAKE_UID_1_INT,
                POLICY_REJECT_METERED_BACKGROUND);
        assertThat(mDynamicDenyListPref.getAll()).hasSize(1);
        assertTrue(mDynamicDenyListPref.contains(FAKE_UID_1));
    }

    @Test
    public void setDenylist_newListWithoutOldData_modifyPolicyReject()
            throws PackageManager.NameNotFoundException {
        when(mPackageManager.getPackageUid(anyString(), eq(0))).thenReturn(
                Integer.parseInt(FAKE_UID_1));
        initDynamicDenylistManager(EMPTY_ARRAY);

        mDynamicDenylistManager.setDenylist(List.of(FAKE_UID_1));

        verify(mNetworkPolicyManager, never()).setUidPolicy(anyInt(), eq(POLICY_NONE));
        verify(mNetworkPolicyManager).setUidPolicy(FAKE_UID_1_INT,
                POLICY_REJECT_METERED_BACKGROUND);
        assertThat(mDynamicDenyListPref.getAll()).hasSize(1);
        assertTrue(mDynamicDenyListPref.contains(FAKE_UID_1));
    }

    @Test
    public void setDenylist_emptyListWithOldData_modifyPolicyNone() {
        initDynamicDenylistManager(EMPTY_ARRAY);
        setupPreference(mDynamicDenyListPref, FAKE_UID_2);

        mDynamicDenylistManager.setDenylist(Collections.emptyList());

        verify(mNetworkPolicyManager).setUidPolicy(FAKE_UID_2_INT, POLICY_NONE);
        verify(mNetworkPolicyManager, never()).setUidPolicy(anyInt(),
                eq(POLICY_REJECT_METERED_BACKGROUND));
        assertThat(mDynamicDenyListPref.getAll()).isEmpty();
    }

    @Test
    public void isInManualDenylist_returnsFalse() {
        initDynamicDenylistManager(EMPTY_ARRAY);

        assertFalse(mDynamicDenylistManager.isInManualDenylist(FAKE_UID_1_INT));
    }

    @Test
    public void isInManualDenylist_incorrectUid_returnsFalse() {
        initDynamicDenylistManager(EMPTY_ARRAY);

        mManualDenyListPref.edit().putInt(FAKE_UID_2, POLICY_REJECT_METERED_BACKGROUND).apply();

        assertFalse(mDynamicDenylistManager.isInManualDenylist(FAKE_UID_1_INT));
    }

    @Test
    public void isInManualDenylist_initiated_returnsTrue() {
        initDynamicDenylistManager(EMPTY_ARRAY);

        mManualDenyListPref.edit().putInt(FAKE_UID_1, POLICY_REJECT_METERED_BACKGROUND).apply();

        assertTrue(mDynamicDenylistManager.isInManualDenylist(FAKE_UID_1_INT));
    }

    @Test
    public void resetDenylistIfNeeded_nullPackageName_doNothing() {
        initDynamicDenylistManager(new int[0], new int[]{FAKE_UID_1_INT, FAKE_UID_2_INT});

        mDynamicDenylistManager.resetDenylistIfNeeded(null, false);

        assertThat(mManualDenyListPref.getAll()).hasSize(1);
        verify(mNetworkPolicyManager, never()).setUidPolicy(anyInt(), eq(POLICY_NONE));
    }

    @Test
    public void resetDenylistIfNeeded_invalidPackageName_doNothing() {
        initDynamicDenylistManager(new int[0], new int[]{FAKE_UID_1_INT, FAKE_UID_2_INT});

        mDynamicDenylistManager.resetDenylistIfNeeded("invalid_package_name", false);

        assertThat(mManualDenyListPref.getAll()).hasSize(1);
        verify(mNetworkPolicyManager, never()).setUidPolicy(anyInt(), eq(POLICY_NONE));
    }

    @Test
    public void resetDenylistIfNeeded_denylistUnchanged_doNothingWithPolicy() {
        initDynamicDenylistManager(new int[]{FAKE_UID_1_INT, FAKE_UID_2_INT});

        mDynamicDenylistManager.resetDenylistIfNeeded(SETTINGS_PACKAGE_NAME, false);

        verify(mNetworkPolicyManager, never()).setUidPolicy(anyInt(), eq(POLICY_NONE));
    }

    @Test
    public void resetDenylistIfNeeded_denylistChanged_resetAndClear() {
        initDynamicDenylistManager(new int[0], new int[]{FAKE_UID_1_INT, FAKE_UID_2_INT});

        mDynamicDenylistManager.resetDenylistIfNeeded(SETTINGS_PACKAGE_NAME, false);

        assertThat(mManualDenyListPref.getAll()).isEmpty();
        verify(mNetworkPolicyManager, times(2)).setUidPolicy(anyInt(), eq(POLICY_NONE));
    }

    @Test
    public void resetDenylistIfNeeded_forceResetWithNullPackageName_resetAndClear() {
        initDynamicDenylistManager(new int[0], new int[]{FAKE_UID_2_INT});

        mDynamicDenylistManager.resetDenylistIfNeeded(null, true);

        assertThat(mManualDenyListPref.getAll()).isEmpty();
        verify(mNetworkPolicyManager).setUidPolicy(eq(FAKE_UID_2_INT), eq(POLICY_NONE));
    }

    @Test// 4
    public void resetDenylistIfNeeded_forceResetWithInvalidPackageName_resetAndClear() {
        initDynamicDenylistManager(new int[0], new int[]{FAKE_UID_1_INT, FAKE_UID_2_INT});

        mDynamicDenylistManager.resetDenylistIfNeeded("invalid_package_name", true);

        assertThat(mManualDenyListPref.getAll()).isEmpty();
        verify(mNetworkPolicyManager, times(2)).setUidPolicy(anyInt(), eq(POLICY_NONE));
    }

    @Test
    public void resetDenylistIfNeeded_forceResetButDenylistUnchanged_doNothingWithPolicy() {
        initDynamicDenylistManager(new int[]{FAKE_UID_1_INT});

        mDynamicDenylistManager.resetDenylistIfNeeded(SETTINGS_PACKAGE_NAME, true);

        assertThat(mManualDenyListPref.getAll()).isEmpty();
        verify(mNetworkPolicyManager, never()).setUidPolicy(anyInt(), eq(POLICY_NONE));
    }

    @Test
    public void resetDenylistIfNeeded_forceResetWithDenylistChanged_resetAndClear() {
        initDynamicDenylistManager(new int[0], new int[]{FAKE_UID_1_INT, FAKE_UID_2_INT});

        mDynamicDenylistManager.resetDenylistIfNeeded(SETTINGS_PACKAGE_NAME, true);

        assertThat(mManualDenyListPref.getAll()).isEmpty();
        verify(mNetworkPolicyManager, times(2)).setUidPolicy(anyInt(), eq(POLICY_NONE));
    }

    @Test
    public void clearSharedPreferences_manualDenyListPrefIsEmpty() {
        initDynamicDenylistManager(EMPTY_ARRAY);
        mManualDenyListPref.edit().putInt(FAKE_UID_1, POLICY_REJECT_METERED_BACKGROUND).apply();
        assertThat(mManualDenyListPref.getAll()).hasSize(2);
        assertTrue(mManualDenyListPref.contains(FAKE_UID_1));
        assertTrue(mManualDenyListPref.contains(PREF_KEY_MANUAL_DENYLIST_SYNCED));

        mDynamicDenylistManager.clearSharedPreferences();

        assertThat(mManualDenyListPref.getAll()).isEmpty();
    }

    @Test
    public void clearSharedPreferences_dynamicDenyListPrefIsEmpty() {
        initDynamicDenylistManager(EMPTY_ARRAY);
        mDynamicDenyListPref.edit().putInt(FAKE_UID_1, POLICY_REJECT_METERED_BACKGROUND).apply();
        assertThat(mDynamicDenyListPref.getAll()).hasSize(1);
        assertTrue(mDynamicDenyListPref.contains(FAKE_UID_1));

        mDynamicDenylistManager.clearSharedPreferences();

        assertThat(mDynamicDenyListPref.getAll()).isEmpty();
    }

    private void initDynamicDenylistManager(int[] preload) {
        initDynamicDenylistManager(preload, preload);
    }
    private void initDynamicDenylistManager(int[] preload1, int[] preload2) {
        final Context context = spy(RuntimeEnvironment.application.getApplicationContext());
        when(context.getApplicationContext()).thenReturn(context);
        when(context.getPackageManager()).thenReturn(mPackageManager);
        when(mNetworkPolicyManager.getUidsWithPolicy(anyInt()))
                .thenReturn(preload1).thenReturn(preload2);
        mDynamicDenylistManager = new DynamicDenylistManager(context, mNetworkPolicyManager);
        mManualDenyListPref = mDynamicDenylistManager.getManualDenylistPref();
        mDynamicDenyListPref = mDynamicDenylistManager.getDynamicDenylistPref();
    }

    private void setupPreference(SharedPreferences sharedPreferences, String... uids) {
        for (String uid : uids) {
            sharedPreferences.edit().putInt(uid, POLICY_REJECT_METERED_BACKGROUND).apply();
        }
    }
}
