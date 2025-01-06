/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.virtual;

import static com.android.settingslib.drawer.TileUtils.IA_SETTINGS_ACTION;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.companion.AssociationInfo;
import android.companion.CompanionDeviceManager;
import android.companion.virtual.VirtualDevice;
import android.companion.virtual.VirtualDeviceManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.util.ArraySet;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.core.content.pm.ApplicationInfoBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowPackageManager;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class VirtualDeviceUpdaterTest {

    private static final String PERSISTENT_DEVICE_ID = "companion:42";
    private static final int ASSOCIATION_ID = 42;
    private static final int DEVICE_ID = 7;
    private static final String PACKAGE_NAME = "test.package.name";

    @Mock
    private VirtualDeviceManager mVirtualDeviceManager;
    @Mock
    private CompanionDeviceManager mCompanionDeviceManager;
    @Mock
    private VirtualDeviceUpdater.DeviceListener mDeviceListener;
    @Mock
    private AssociationInfo mAssociationInfo;
    @Mock
    private VirtualDevice mVirtualDevice;
    private ShadowPackageManager mPackageManager;

    private VirtualDeviceUpdater mVirtualDeviceUpdater;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Context context = spy(ApplicationProvider.getApplicationContext());
        when(context.getSystemService(VirtualDeviceManager.class))
                .thenReturn(mVirtualDeviceManager);
        when(context.getSystemService(CompanionDeviceManager.class))
                .thenReturn(mCompanionDeviceManager);
        mPackageManager = Shadows.shadowOf(context.getPackageManager());
        mVirtualDeviceUpdater = new VirtualDeviceUpdater(context, mDeviceListener);
    }

    @Test
    public void loadDevices_noDevices() {
        mVirtualDeviceUpdater.loadDevices();
        verifyNoMoreInteractions(mDeviceListener);
        assertThat(mVirtualDeviceUpdater.mDevices).isEmpty();
    }

    @Test
    public void loadDevices_noAssociationInfo() {
        when(mVirtualDeviceManager.getAllPersistentDeviceIds())
                .thenReturn(new ArraySet<>(new String[]{PERSISTENT_DEVICE_ID}));

        mVirtualDeviceUpdater.loadDevices();
        verifyNoMoreInteractions(mDeviceListener);
        assertThat(mVirtualDeviceUpdater.mDevices).isEmpty();
    }

    @Test
    public void loadDevices_invalidAssociationId() {
        when(mVirtualDeviceManager.getAllPersistentDeviceIds())
                .thenReturn(new ArraySet<>(new String[]{"NotACompanionPersistentId"}));

        mVirtualDeviceUpdater.loadDevices();
        verifyNoMoreInteractions(mDeviceListener);
        assertThat(mVirtualDeviceUpdater.mDevices).isEmpty();
    }

    @Test
    public void loadDevices_noMatchingAssociationId() {
        when(mVirtualDeviceManager.getAllPersistentDeviceIds())
                .thenReturn(new ArraySet<>(new String[]{PERSISTENT_DEVICE_ID}));
        when(mCompanionDeviceManager.getAllAssociations(UserHandle.USER_ALL))
                .thenReturn(List.of(mAssociationInfo));
        when(mAssociationInfo.getId()).thenReturn(ASSOCIATION_ID + 1);

        mVirtualDeviceUpdater.loadDevices();
        verifyNoMoreInteractions(mDeviceListener);
        assertThat(mVirtualDeviceUpdater.mDevices).isEmpty();
    }

    @Test
    public void loadDevices_excludePackageFromSettings() {
        when(mVirtualDeviceManager.getAllPersistentDeviceIds())
                .thenReturn(new ArraySet<>(new String[]{PERSISTENT_DEVICE_ID}));
        when(mCompanionDeviceManager.getAllAssociations(UserHandle.USER_ALL))
                .thenReturn(List.of(mAssociationInfo));
        when(mAssociationInfo.getId()).thenReturn(ASSOCIATION_ID);
        when(mAssociationInfo.getPackageName()).thenReturn(PACKAGE_NAME);
        final ApplicationInfo appInfo =
                ApplicationInfoBuilder.newBuilder().setPackageName(PACKAGE_NAME).build();
        final ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = PACKAGE_NAME;
        activityInfo.name = PACKAGE_NAME;
        activityInfo.applicationInfo = appInfo;
        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.activityInfo = activityInfo;
        final Intent intent = new Intent(IA_SETTINGS_ACTION);
        intent.setPackage(PACKAGE_NAME);
        mPackageManager.addResolveInfoForIntent(intent, resolveInfo);

        mVirtualDeviceUpdater.loadDevices();
        verifyNoMoreInteractions(mDeviceListener);
        assertThat(mVirtualDeviceUpdater.mDevices).isEmpty();
    }

    @Test
    public void loadDevices_newDevice_inactive() {
        when(mVirtualDeviceManager.getAllPersistentDeviceIds())
                .thenReturn(new ArraySet<>(new String[]{PERSISTENT_DEVICE_ID}));
        when(mCompanionDeviceManager.getAllAssociations(UserHandle.USER_ALL))
                .thenReturn(List.of(mAssociationInfo));
        when(mAssociationInfo.getId()).thenReturn(ASSOCIATION_ID);
        when(mAssociationInfo.getPackageName()).thenReturn(PACKAGE_NAME);

        mVirtualDeviceUpdater.loadDevices();
        VirtualDeviceWrapper device = new VirtualDeviceWrapper(
                mAssociationInfo, PERSISTENT_DEVICE_ID, Context.DEVICE_ID_INVALID);
        verify(mDeviceListener).onDeviceAdded(device);
        verifyNoMoreInteractions(mDeviceListener);
        assertThat(mVirtualDeviceUpdater.mDevices).containsExactly(PERSISTENT_DEVICE_ID, device);
    }

    @Test
    public void loadDevices_newDevice_active() {
        when(mVirtualDeviceManager.getAllPersistentDeviceIds())
                .thenReturn(new ArraySet<>(new String[]{PERSISTENT_DEVICE_ID}));
        when(mVirtualDeviceManager.getVirtualDevices())
                .thenReturn(List.of(mVirtualDevice));
        when(mCompanionDeviceManager.getAllAssociations(UserHandle.USER_ALL))
                .thenReturn(List.of(mAssociationInfo));
        when(mAssociationInfo.getId()).thenReturn(ASSOCIATION_ID);
        when(mAssociationInfo.getPackageName()).thenReturn(PACKAGE_NAME);
        when(mVirtualDevice.getDeviceId()).thenReturn(DEVICE_ID);
        when(mVirtualDevice.getPersistentDeviceId()).thenReturn(PERSISTENT_DEVICE_ID);

        mVirtualDeviceUpdater.loadDevices();
        VirtualDeviceWrapper device = new VirtualDeviceWrapper(
                mAssociationInfo, PERSISTENT_DEVICE_ID, DEVICE_ID);
        verify(mDeviceListener).onDeviceAdded(device);
        verifyNoMoreInteractions(mDeviceListener);
        assertThat(mVirtualDeviceUpdater.mDevices).containsExactly(PERSISTENT_DEVICE_ID, device);
    }

    @Test
    public void loadDevices_removeDevice() {
        VirtualDeviceWrapper device = new VirtualDeviceWrapper(
                mAssociationInfo, PERSISTENT_DEVICE_ID, DEVICE_ID);
        mVirtualDeviceUpdater.mDevices.put(PERSISTENT_DEVICE_ID, device);

        mVirtualDeviceUpdater.loadDevices();
        verify(mDeviceListener).onDeviceRemoved(device);
        verifyNoMoreInteractions(mDeviceListener);
        assertThat(mVirtualDeviceUpdater.mDevices).isEmpty();
    }

    @Test
    public void loadDevices_noChanges_activeDevice() {
        VirtualDeviceWrapper device = new VirtualDeviceWrapper(
                mAssociationInfo, PERSISTENT_DEVICE_ID, DEVICE_ID);
        mVirtualDeviceUpdater.mDevices.put(PERSISTENT_DEVICE_ID, device);

        when(mVirtualDeviceManager.getAllPersistentDeviceIds())
                .thenReturn(new ArraySet<>(new String[]{PERSISTENT_DEVICE_ID}));
        when(mVirtualDeviceManager.getVirtualDevices())
                .thenReturn(List.of(mVirtualDevice));
        when(mCompanionDeviceManager.getAllAssociations(UserHandle.USER_ALL))
                .thenReturn(List.of(mAssociationInfo));
        when(mAssociationInfo.getId()).thenReturn(ASSOCIATION_ID);
        when(mAssociationInfo.getPackageName()).thenReturn(PACKAGE_NAME);
        when(mVirtualDevice.getDeviceId()).thenReturn(DEVICE_ID);
        when(mVirtualDevice.getPersistentDeviceId()).thenReturn(PERSISTENT_DEVICE_ID);

        mVirtualDeviceUpdater.loadDevices();
        verifyNoMoreInteractions(mDeviceListener);
        assertThat(mVirtualDeviceUpdater.mDevices).containsExactly(PERSISTENT_DEVICE_ID, device);
    }

    @Test
    public void loadDevices_noChanges_inactiveDevice() {
        VirtualDeviceWrapper device = new VirtualDeviceWrapper(
                mAssociationInfo, PERSISTENT_DEVICE_ID, Context.DEVICE_ID_INVALID);
        mVirtualDeviceUpdater.mDevices.put(PERSISTENT_DEVICE_ID, device);

        when(mVirtualDeviceManager.getAllPersistentDeviceIds())
                .thenReturn(new ArraySet<>(new String[]{PERSISTENT_DEVICE_ID}));
        when(mCompanionDeviceManager.getAllAssociations(UserHandle.USER_ALL))
                .thenReturn(List.of(mAssociationInfo));
        when(mAssociationInfo.getId()).thenReturn(ASSOCIATION_ID);
        when(mAssociationInfo.getPackageName()).thenReturn(PACKAGE_NAME);

        mVirtualDeviceUpdater.loadDevices();
        verifyNoMoreInteractions(mDeviceListener);
        assertThat(mVirtualDeviceUpdater.mDevices).containsExactly(PERSISTENT_DEVICE_ID, device);
    }

    @Test
    public void loadDevices_deviceChange_activeToInactive() {
        VirtualDeviceWrapper device = new VirtualDeviceWrapper(
                mAssociationInfo, PERSISTENT_DEVICE_ID, DEVICE_ID);
        mVirtualDeviceUpdater.mDevices.put(PERSISTENT_DEVICE_ID, device);

        when(mVirtualDeviceManager.getAllPersistentDeviceIds())
                .thenReturn(new ArraySet<>(new String[]{PERSISTENT_DEVICE_ID}));
        when(mCompanionDeviceManager.getAllAssociations(UserHandle.USER_ALL))
                .thenReturn(List.of(mAssociationInfo));
        when(mAssociationInfo.getId()).thenReturn(ASSOCIATION_ID);
        when(mAssociationInfo.getPackageName()).thenReturn(PACKAGE_NAME);

        mVirtualDeviceUpdater.loadDevices();

        device.setDeviceId(Context.DEVICE_ID_INVALID);
        verify(mDeviceListener).onDeviceChanged(device);
        verifyNoMoreInteractions(mDeviceListener);
        assertThat(mVirtualDeviceUpdater.mDevices).containsExactly(PERSISTENT_DEVICE_ID, device);
    }

    @Test
    public void loadDevices_deviceChange_inactiveToActive() {
        VirtualDeviceWrapper device = new VirtualDeviceWrapper(
                mAssociationInfo, PERSISTENT_DEVICE_ID, Context.DEVICE_ID_INVALID);
        mVirtualDeviceUpdater.mDevices.put(PERSISTENT_DEVICE_ID, device);

        when(mVirtualDeviceManager.getAllPersistentDeviceIds())
                .thenReturn(new ArraySet<>(new String[]{PERSISTENT_DEVICE_ID}));
        when(mVirtualDeviceManager.getVirtualDevices())
                .thenReturn(List.of(mVirtualDevice));
        when(mCompanionDeviceManager.getAllAssociations(UserHandle.USER_ALL))
                .thenReturn(List.of(mAssociationInfo));
        when(mAssociationInfo.getId()).thenReturn(ASSOCIATION_ID);
        when(mAssociationInfo.getPackageName()).thenReturn(PACKAGE_NAME);
        when(mVirtualDevice.getDeviceId()).thenReturn(DEVICE_ID);
        when(mVirtualDevice.getPersistentDeviceId()).thenReturn(PERSISTENT_DEVICE_ID);


        mVirtualDeviceUpdater.loadDevices();

        device.setDeviceId(DEVICE_ID);
        verify(mDeviceListener).onDeviceChanged(device);
        verifyNoMoreInteractions(mDeviceListener);
        assertThat(mVirtualDeviceUpdater.mDevices).containsExactly(PERSISTENT_DEVICE_ID, device);
    }
}
