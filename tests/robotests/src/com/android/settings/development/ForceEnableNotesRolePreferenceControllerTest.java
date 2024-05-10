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

package com.android.settings.development;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.pm.UserInfo;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserManager;

import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.util.Arrays;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ForceEnableNotesRolePreferenceControllerTest.ShadowOverlayManagerStub.class)
public class ForceEnableNotesRolePreferenceControllerTest {
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private SwitchPreference mPreference;
    @Mock
    private UserManager mUserManager;
    @Mock
    private static IOverlayManager sOverlayManager;

    private ForceEnableNotesRolePreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new ForceEnableNotesRolePreferenceController(
                RuntimeEnvironment.application) {
            private boolean mEnabled;

            protected boolean isEnabled() {
                return mEnabled;
            }

            protected void setEnabled(boolean enabled) {
                mEnabled = enabled;
            }
        };
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
        mController.displayPreference(mScreen);
    }

    @Test
    public void setEnabled_updatesForAllFullAndProfileUsers() throws RemoteException {
        Context context = spy(RuntimeEnvironment.application.getApplicationContext());
        UserInfo user1 = new UserInfo(1, "Name", "Path", 0x0ff0ff,
                UserManager.USER_TYPE_FULL_SYSTEM);
        UserInfo user2 = new UserInfo(2, "Name", "Path", 0x0ff0ff,
                UserManager.USER_TYPE_PROFILE_MANAGED);
        UserInfo user3 = new UserInfo(3, "Name", "Path", 0x0ff0ff, "Some other type");
        when(context.getSystemService(UserManager.class)).thenReturn(mUserManager);

        when(mUserManager.getUsers()).thenReturn(Arrays.asList(user1, user2, user3));

        mController = new ForceEnableNotesRolePreferenceController(context);
        mController.setEnabled(true);

        verify(sOverlayManager).setEnabled(
                ForceEnableNotesRolePreferenceController.OVERLAY_PACKAGE_NAME, true, 1);
        verify(sOverlayManager).setEnabled(
                ForceEnableNotesRolePreferenceController.OVERLAY_PACKAGE_NAME, true, 2);
    }

    @Test
    public void updateState_enabled_preferenceShouldBeChecked() {
        mController.setEnabled(true);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(true);
    }

    @Test
    public void updateState_disabled_preferenceShouldBeUnchecked() {
        mController.setEnabled(false);
        mController.updateState(mPreference);

        verify(mPreference).setChecked(false);
    }

    @Test
    public void onPreferenceChange_checked_shouldBeEnabled() {
        mController.onPreferenceChange(mPreference, true);

        assertTrue(mController.isEnabled());
    }

    @Test
    public void onPreferenceChange_unchecked_shouldNotBeEnabled() {
        mController.onPreferenceChange(mPreference, false);

        assertFalse(mController.isEnabled());
    }

    @Test
    public void onDeveloperOptionsSwitchDisabled_preferenceShouldNotBeEnabled() {
        mController.onDeveloperOptionsSwitchDisabled();

        assertFalse(mController.isEnabled());
        verify(mPreference).setEnabled(false);
        verify(mPreference).setChecked(false);
    }

    @Implements(IOverlayManager.Stub.class)
    public static class ShadowOverlayManagerStub {
        @Implementation
        public static IOverlayManager asInterface(IBinder iBinder) {
            return sOverlayManager;
        }
    }
}
