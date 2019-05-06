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

package com.android.settings.applications;

import static com.android.settings.applications.AppPermissionsPreferenceController.NUM_PERMISSIONS_TO_SHOW;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

import androidx.preference.Preference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class AppPermissionsPreferenceControllerTest {

    private Context mContext;
    private AppPermissionsPreferenceController mController;
    private Preference mPreference;

    @Before
    public void setUp() throws NameNotFoundException {
        mContext = RuntimeEnvironment.application;
        mPreference = spy(new Preference(mContext));
        mController = spy(new AppPermissionsPreferenceController(mContext, "pref_key"));
    }

    @Test
    public void isAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_shouldResetNumPackageChecked() {
        doNothing().when(mController).queryPermissionSummary();
        mController.mNumPackageChecked = 3;

        mController.updateState(mPreference);

        assertThat(mController.mNumPackageChecked).isEqualTo(0);
    }

    @Test
    public void updateSummary_noGrantedPermission_shouldSetNullSummary() {
        doNothing().when(mController).queryPermissionSummary();
        mController.updateState(mPreference);
        mController.mNumPackageChecked = 2;

        mController.updateSummary(new ArrayList<>());

        assertThat(mPreference.getSummary()).isNull();
    }

    @Test
    public void updateSummary_hasPermissionGroups_shouldSetPermissionAsSummary() {
        doNothing().when(mController).queryPermissionSummary();
        mController.updateState(mPreference);
        final String permission = "location";
        final ArrayList<CharSequence> labels = new ArrayList<>();
        labels.add(permission);
        final String summary = "Apps using " + permission;
        mController.mNumPackageChecked = 2;

        mController.updateSummary(labels);

        assertThat(mPreference.getSummary()).isEqualTo(summary);
    }

    @Test
    public void updateSummary_notReachCallbackCount_shouldNotSetSummary() {
        doNothing().when(mController).queryPermissionSummary();
        mController.updateState(mPreference);
        final String permission = "location";
        final ArrayList<CharSequence> labels = new ArrayList<>();
        labels.add(permission);

        mController.updateSummary(labels);

        verify(mPreference, never()).setSummary(anyString());
    }

    @Test
    public void updateSummary_hasFiveItems_shouldShowCertainNumItems() {
        doNothing().when(mController).queryPermissionSummary();
        mController.updateState(mPreference);
        mController.mNumPackageChecked = 2;

        mController.updateSummary(getPermissionGroupsSet());

        final CharSequence summary = mPreference.getSummary();
        final int items = summary.toString().split(",").length;
        assertThat(items).isEqualTo(NUM_PERMISSIONS_TO_SHOW);
    }

    private List<CharSequence> getPermissionGroupsSet() {
        final List<CharSequence> labels = new ArrayList<>();
        labels.add("Phone");
        labels.add("SMS");
        labels.add("Microphone");
        labels.add("Contacts");
        labels.add("Camera");
        labels.add("Location");

        return labels;
    }
}
