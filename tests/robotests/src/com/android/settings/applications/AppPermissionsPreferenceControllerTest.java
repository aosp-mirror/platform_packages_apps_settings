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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.support.v7.preference.Preference;

import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class AppPermissionsPreferenceControllerTest {

    private static final String PERM_LOCATION = "android.permission-group.LOCATION";
    private static final String PERM_MICROPHONE = "android.permission-group.MICROPHONE";
    private static final String PERM_CAMERA = "android.permission-group.CAMERA";
    private static final String PERM_SMS = "android.permission-group.SMS";
    private static final String PERM_CONTACTS = "android.permission-group.CONTACTS";
    private static final String PERM_PHONE = "android.permission-group.PHONE";
    private static final String LABEL_LOCATION = "Location";
    private static final String LABEL_MICROPHONE = "Microphone";
    private static final String LABEL_CAMERA = "Camera";
    private static final String LABEL_SMS = "Sms";
    private static final String LABEL_CONTACTS = "Contacts";
    private static final String LABEL_PHONE = "Phone";

    @Mock
    private Preference mPreference;
    @Mock
    private PackageManager mPackageManager;
    @Mock
    private PermissionGroupInfo mGroupLocation;
    @Mock
    private PermissionGroupInfo mGroupMic;
    @Mock
    private PermissionGroupInfo mGroupCamera;
    @Mock
    private PermissionGroupInfo mGroupSms;
    @Mock
    private PermissionGroupInfo mGroupContacts;
    @Mock
    private PermissionGroupInfo mGroupPhone;

    private Context mContext;
    private AppPermissionsPreferenceController mController;
    private PermissionInfo mPermLocation;
    private PermissionInfo mPermMic;
    private PermissionInfo mPermCamera;
    private PermissionInfo mPermSms;
    private PermissionInfo mPermContacts;
    private PermissionInfo mPermPhone;

    @Before
    public void setUp() throws NameNotFoundException {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mContext.getPackageManager()).thenReturn(mPackageManager);

        // create permission groups
        when(mPackageManager.getPermissionGroupInfo(eq(PERM_LOCATION), anyInt()))
            .thenReturn(mGroupLocation);
        when(mGroupLocation.loadLabel(mPackageManager)).thenReturn(LABEL_LOCATION);
        when(mPackageManager.getPermissionGroupInfo(eq(PERM_MICROPHONE), anyInt()))
            .thenReturn(mGroupMic);
        when(mGroupMic.loadLabel(mPackageManager)).thenReturn(LABEL_MICROPHONE);
        when(mPackageManager.getPermissionGroupInfo(eq(PERM_CAMERA), anyInt()))
            .thenReturn(mGroupCamera);
        when(mGroupCamera.loadLabel(mPackageManager)).thenReturn(LABEL_CAMERA);
        when(mPackageManager.getPermissionGroupInfo(eq(PERM_SMS), anyInt())).thenReturn(mGroupSms);
        when(mGroupSms.loadLabel(mPackageManager)).thenReturn(LABEL_SMS);
        when(mPackageManager.getPermissionGroupInfo(eq(PERM_CONTACTS), anyInt()))
            .thenReturn(mGroupContacts);
        when(mGroupContacts.loadLabel(mPackageManager)).thenReturn(LABEL_CONTACTS);
        when(mPackageManager.getPermissionGroupInfo(eq(PERM_PHONE), anyInt()))
            .thenReturn(mGroupPhone);
        when(mGroupPhone.loadLabel(mPackageManager)).thenReturn(LABEL_PHONE);

        // create permissions
        mPermLocation = new PermissionInfo();
        mPermLocation.name = "Permission1";
        mPermLocation.group = PERM_LOCATION;
        mPermMic = new PermissionInfo();
        mPermMic.name = "Permission2";
        mPermMic.group = PERM_MICROPHONE;
        mPermCamera = new PermissionInfo();
        mPermCamera.name = "Permission3";
        mPermCamera.group = PERM_CAMERA;
        mPermSms = new PermissionInfo();
        mPermSms.name = "Permission4";
        mPermSms.group = PERM_SMS;
        mPermContacts = new PermissionInfo();
        mPermContacts.name = "Permission4";
        mPermContacts.group = PERM_CONTACTS;
        mPermPhone = new PermissionInfo();
        mPermPhone.name = "Permission4";
        mPermPhone.group = PERM_PHONE;
        final List<PermissionInfo> permissions = new ArrayList<>();
        permissions.add(mPermLocation);
        permissions.add(mPermMic);
        permissions.add(mPermCamera);
        permissions.add(mPermSms);
        permissions.add(mPermContacts);
        permissions.add(mPermPhone);
        when(mPackageManager.queryPermissionsByGroup(anyString(), anyInt()))
            .thenReturn(permissions);

        mController = spy(new AppPermissionsPreferenceController(mContext));
    }

    @Test
    public void isAvailable_shouldAlwaysReturnTrue() {
        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void updateState_noGrantedPermissions_shouldNotSetSummary() {
        final List<PackageInfo> installedPackages = new ArrayList<>();
        final PackageInfo info = new PackageInfo();
        installedPackages.add(info);
        when(mPackageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS))
            .thenReturn(installedPackages);

        mController.updateState(mPreference);

        verify(mPreference, never()).setSummary(anyString());
    }

    @Test
    public void updateState_hasPermissions_shouldSetSummary() {
        final List<PackageInfo> installedPackages = new ArrayList<>();
        final PackageInfo info = new PackageInfo();
        installedPackages.add(info);
        when(mPackageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS))
            .thenReturn(installedPackages);
        PermissionInfo[] permissions = new PermissionInfo[4];
        info.permissions = permissions;

        permissions[0] = mPermLocation;
        permissions[1] = mPermMic;
        permissions[2] = mPermCamera;
        permissions[3] = mPermSms;
        mController.updateState(mPreference);
        verify(mPreference).setSummary("Apps using Location, Microphone, Camera");

        permissions[0] = mPermPhone;
        permissions[1] = mPermMic;
        permissions[2] = mPermCamera;
        permissions[3] = mPermSms;
        mController.updateState(mPreference);
        verify(mPreference).setSummary("Apps using Microphone, Camera, Sms");

        permissions[0] = mPermPhone;
        permissions[1] = mPermMic;
        permissions[2] = mPermContacts;
        permissions[3] = mPermSms;
        mController.updateState(mPreference);
        verify(mPreference).setSummary("Apps using Microphone, Sms, Contacts");

        permissions = new PermissionInfo[2];
        info.permissions = permissions;
        permissions[0] = mPermLocation;
        permissions[1] = mPermCamera;
        mController.updateState(mPreference);
        verify(mPreference).setSummary("Apps using Location, Camera");

        permissions = new PermissionInfo[1];
        info.permissions = permissions;
        permissions[0] = mPermCamera;
        mController.updateState(mPreference);
        verify(mPreference).setSummary("Apps using Camera");
    }
}
