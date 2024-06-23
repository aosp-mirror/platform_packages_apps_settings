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

package com.android.settings.privatespace;

import static android.provider.Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS;
import static android.provider.Settings.Secure.PRIVATE_SPACE_AUTO_LOCK;

import static com.android.settings.privatespace.PrivateSpaceMaintainer.HIDE_PRIVATE_SPACE_ENTRY_POINT_DISABLED_VAL;
import static com.android.settings.privatespace.PrivateSpaceMaintainer.HIDE_PRIVATE_SPACE_ENTRY_POINT_ENABLED_VAL;
import static com.android.settings.privatespace.PrivateSpaceMaintainer.PRIVATE_SPACE_AUTO_LOCK_DEFAULT_VAL;

import static com.google.common.truth.Truth.assertThat;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Flags;
import android.os.RemoteException;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.privatespace.PrivateSpaceMaintainer.ErrorDeletingPrivateSpace;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class PrivateSpaceMaintainerTest {
    private static final String TAG = "PSMaintainerTest";
    private Context mContext;
    private ContentResolver mContentResolver;
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    /** Required setup before a test. */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mContentResolver = mContext.getContentResolver();
    }

    @After
    public void tearDown() {
        PrivateSpaceMaintainer privateSpaceMaintainer =
                PrivateSpaceMaintainer.getInstance(mContext);
        privateSpaceMaintainer.deletePrivateSpace();
    }

    /** Tests that {@link PrivateSpaceMaintainer#deletePrivateSpace()} deletes PS when PS exists. */
    @Test
    public void deletePrivateSpace_psExists_deletesPS() {
        PrivateSpaceMaintainer privateSpaceMaintainer =
                PrivateSpaceMaintainer.getInstance(mContext);
        privateSpaceMaintainer.createPrivateSpace();
        ErrorDeletingPrivateSpace errorDeletingPrivateSpace =
                privateSpaceMaintainer.deletePrivateSpace();
        assertThat(errorDeletingPrivateSpace)
                .isEqualTo(ErrorDeletingPrivateSpace.DELETE_PS_ERROR_NONE);
        assertThat(privateSpaceMaintainer.doesPrivateSpaceExist()).isFalse();
    }

    /**
     * Tests that {@link PrivateSpaceMaintainer#deletePrivateSpace()} returns error when PS does not
     * exist.
     */
    @Test
    public void deletePrivateSpace_psDoesNotExist_returnsNoPSError() {
        PrivateSpaceMaintainer privateSpaceMaintainer =
                PrivateSpaceMaintainer.getInstance(mContext);
        ErrorDeletingPrivateSpace errorDeletingPrivateSpace =
                privateSpaceMaintainer.deletePrivateSpace();
        assertThat(errorDeletingPrivateSpace)
                .isEqualTo(ErrorDeletingPrivateSpace.DELETE_PS_ERROR_NO_PRIVATE_SPACE);
        assertThat(privateSpaceMaintainer.doesPrivateSpaceExist()).isFalse();
    }

    /** Tests that {@link PrivateSpaceMaintainer#createPrivateSpace()} when PS exists creates PS. */
    @Test
    public void createPrivateSpace_psDoesNotExist_createsPS() {
        PrivateSpaceMaintainer privateSpaceMaintainer =
                PrivateSpaceMaintainer.getInstance(mContext);
        privateSpaceMaintainer.deletePrivateSpace();
        assertThat(privateSpaceMaintainer.createPrivateSpace()).isTrue();
        assertThat(privateSpaceMaintainer.doesPrivateSpaceExist()).isTrue();
    }

    /**
     * Tests that {@link PrivateSpaceMaintainer#createPrivateSpace()} when PS exists still returns
     * true.
     */
    @Test
    public void createPrivateSpace_psExists_returnsFalse() {
        PrivateSpaceMaintainer privateSpaceMaintainer =
                PrivateSpaceMaintainer.getInstance(mContext);
        privateSpaceMaintainer.deletePrivateSpace();
        assertThat(privateSpaceMaintainer.createPrivateSpace()).isTrue();
        assertThat(privateSpaceMaintainer.doesPrivateSpaceExist()).isTrue();
        assertThat(privateSpaceMaintainer.createPrivateSpace()).isTrue();
    }

    /**
     * Tests that {@link PrivateSpaceMaintainer#createPrivateSpace()} when no PS exists resets hide
     * PS Settings.
     */
    @Test
    public void createPrivateSpace_psDoesNotExist_resetsHidePSSettings() {
        PrivateSpaceMaintainer privateSpaceMaintainer =
                PrivateSpaceMaintainer.getInstance(mContext);
        Settings.Secure.putInt(
                mContentResolver,
                Settings.Secure.HIDE_PRIVATESPACE_ENTRY_POINT,
                HIDE_PRIVATE_SPACE_ENTRY_POINT_ENABLED_VAL);

        privateSpaceMaintainer.deletePrivateSpace();
        privateSpaceMaintainer.createPrivateSpace();
        assertThat(privateSpaceMaintainer.getHidePrivateSpaceEntryPointSetting())
                .isEqualTo(HIDE_PRIVATE_SPACE_ENTRY_POINT_DISABLED_VAL);
    }

    /**
     * Tests that {@link PrivateSpaceMaintainer#createPrivateSpace()} sets the PS sensitive
     * notifications to hidden by default.
     */
    @Test
    public void createPrivateSpace_psDoesNotExist_setsDefaultPsSensitiveNotificationsValue() {
        mSetFlagsRule.enableFlags(
                Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_ENABLE_PS_SENSITIVE_NOTIFICATIONS_TOGGLE);
        PrivateSpaceMaintainer privateSpaceMaintainer =
                PrivateSpaceMaintainer.getInstance(mContext);
        privateSpaceMaintainer.deletePrivateSpace();
        privateSpaceMaintainer.createPrivateSpace();
        assertThat(privateSpaceMaintainer.doesPrivateSpaceExist()).isTrue();
        assertThat(getPsSensitiveNotificationsValue(privateSpaceMaintainer))
                .isEqualTo(HidePrivateSpaceSensitiveNotificationsController.DISABLED);
    }

    /**
     * Tests that {@link PrivateSpaceMaintainer#createPrivateSpace()} when PS exist does not reset
     * hide PS Settings.
     */
    @Test
    public void createPrivateSpace_psExists_doesNotResetHidePSSettings() {
        PrivateSpaceMaintainer privateSpaceMaintainer =
                PrivateSpaceMaintainer.getInstance(mContext);
        privateSpaceMaintainer.createPrivateSpace();
        Settings.Secure.putInt(
                mContentResolver,
                Settings.Secure.HIDE_PRIVATESPACE_ENTRY_POINT,
                HIDE_PRIVATE_SPACE_ENTRY_POINT_ENABLED_VAL);

        privateSpaceMaintainer.createPrivateSpace();
        assertThat(privateSpaceMaintainer.getHidePrivateSpaceEntryPointSetting())
                .isEqualTo(HIDE_PRIVATE_SPACE_ENTRY_POINT_ENABLED_VAL);
    }

    /**
     * Tests that {@link PrivateSpaceMaintainer#lockPrivateSpace()} when PS exists and is running
     * locks the private profile.
     */
    @Test
    public void lockPrivateSpace_psExistsAndPrivateProfileRunning_locksCreatedPrivateSpace() {
        PrivateSpaceMaintainer privateSpaceMaintainer =
                PrivateSpaceMaintainer.getInstance(mContext);
        privateSpaceMaintainer.createPrivateSpace();
        assertThat(privateSpaceMaintainer.doesPrivateSpaceExist()).isTrue();
        assertThat(privateSpaceMaintainer.isPrivateProfileRunning()).isTrue();
        assertThat(privateSpaceMaintainer.isPrivateSpaceLocked()).isFalse();
        assertThat(privateSpaceMaintainer.lockPrivateSpace()).isTrue();
        assertThat(privateSpaceMaintainer.isPrivateSpaceLocked()).isTrue();
    }

    /**
     * Tests that {@link PrivateSpaceMaintainer#lockPrivateSpace()} when PS exist and private
     * profile not running returns false.
     */
    @Test
    public void lockPrivateSpace_psExistsAndPrivateProfileNotRunning_returnsFalse() {
        PrivateSpaceMaintainer privateSpaceMaintainer =
                PrivateSpaceMaintainer.getInstance(mContext);
        privateSpaceMaintainer.createPrivateSpace();
        assertThat(privateSpaceMaintainer.doesPrivateSpaceExist()).isTrue();
        assertThat(privateSpaceMaintainer.isPrivateProfileRunning()).isTrue();
        IActivityManager am = ActivityManager.getService();
        try {
            am.stopProfile(privateSpaceMaintainer.getPrivateProfileHandle().getIdentifier());
        } catch (RemoteException e) {
            Assert.fail("Stop profile failed with exception " + e.getMessage());
        }
        assertThat(privateSpaceMaintainer.isPrivateProfileRunning()).isFalse();
        assertThat(privateSpaceMaintainer.lockPrivateSpace()).isFalse();
    }

    /**
     * Tests that {@link PrivateSpaceMaintainer#lockPrivateSpace()} when no PS exists returns false.
     */
    @Test
    public void lockPrivateSpace_psDoesNotExist_returnsFalse() {
        PrivateSpaceMaintainer privateSpaceMaintainer =
                PrivateSpaceMaintainer.getInstance(mContext);
        assertThat(privateSpaceMaintainer.doesPrivateSpaceExist()).isFalse();
        assertThat(privateSpaceMaintainer.lockPrivateSpace()).isFalse();
    }

    /**
     * Tests that {@link PrivateSpaceMaintainer#createPrivateSpace()} when no PS exists sets
     * USER_SETUP_COMPLETE setting.
     */
    @Test
    public void createPrivateSpace_psDoesNotExist_setsUserSetupComplete() {
        PrivateSpaceMaintainer privateSpaceMaintainer =
                PrivateSpaceMaintainer.getInstance(mContext);
        privateSpaceMaintainer.createPrivateSpace();
        assertThat(getSecureUserSetupComplete()).isEqualTo(1);
    }

    /**
     * Tests that {@link PrivateSpaceMaintainer#createPrivateSpace()} when PS exists does not change
     * USER_SETUP_COMPLETE setting.
     */
    @Test
    public void createPrivateSpace_pSExists_doesNotChangeUserSetupSetting() {
        PrivateSpaceMaintainer privateSpaceMaintainer =
                PrivateSpaceMaintainer.getInstance(mContext);
        privateSpaceMaintainer.createPrivateSpace();
        assertThat(getSecureUserSetupComplete()).isEqualTo(1);
        privateSpaceMaintainer.createPrivateSpace();
        assertThat(getSecureUserSetupComplete()).isEqualTo(1);
    }

    /**
     * Tests that {@link PrivateSpaceMaintainer#createPrivateSpace()} when no PS exists resets PS
     * auto lock Settings.
     */
    @Test
    public void createPrivateSpace_psDoesNotExist_resetsPSAutoLockSettings() {
        mSetFlagsRule.enableFlags(
                Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_SUPPORT_AUTOLOCK_FOR_PRIVATE_SPACE);
        final int autoLockOption = 2;
        PrivateSpaceMaintainer privateSpaceMaintainer =
                PrivateSpaceMaintainer.getInstance(mContext);
        Settings.Secure.putInt(
                mContentResolver, Settings.Secure.PRIVATE_SPACE_AUTO_LOCK, autoLockOption);

        privateSpaceMaintainer.deletePrivateSpace();
        privateSpaceMaintainer.createPrivateSpace();
        assertThat(privateSpaceMaintainer.getPrivateSpaceAutoLockSetting())
                .isEqualTo(PRIVATE_SPACE_AUTO_LOCK_DEFAULT_VAL);
        assertThat(Settings.Secure.getInt(mContentResolver, PRIVATE_SPACE_AUTO_LOCK, -1))
                .isEqualTo(PRIVATE_SPACE_AUTO_LOCK_DEFAULT_VAL);
    }

    /**
     * Tests that {@link PrivateSpaceMaintainer#createPrivateSpace()} when PS exist does not reset
     * PS auto lock setting.
     */
    @Test
    public void createPrivateSpace_psExists_doesNotResetPSAutoLockSettings() {
        mSetFlagsRule.enableFlags(
                Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_SUPPORT_AUTOLOCK_FOR_PRIVATE_SPACE);
        final int privateSpaceAutLockValue = 1;
        PrivateSpaceMaintainer privateSpaceMaintainer =
                PrivateSpaceMaintainer.getInstance(mContext);
        privateSpaceMaintainer.createPrivateSpace();
        Settings.Secure.putInt(
                mContentResolver,
                Settings.Secure.PRIVATE_SPACE_AUTO_LOCK,
                privateSpaceAutLockValue);

        privateSpaceMaintainer.createPrivateSpace();
        assertThat(privateSpaceMaintainer.getPrivateSpaceAutoLockSetting())
                .isEqualTo(privateSpaceAutLockValue);
    }

    private int getSecureUserSetupComplete() {
        PrivateSpaceMaintainer privateSpaceMaintainer =
                PrivateSpaceMaintainer.getInstance(mContext);
        return Settings.Secure.getIntForUser(
                mContentResolver,
                Settings.Secure.USER_SETUP_COMPLETE,
                0,
                privateSpaceMaintainer.getPrivateProfileHandle().getIdentifier());
    }

    private int getPsSensitiveNotificationsValue(PrivateSpaceMaintainer privateSpaceMaintainer) {
        return Settings.Secure.getIntForUser(mContentResolver,
                LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS,
                HidePrivateSpaceSensitiveNotificationsController.ENABLED,
                privateSpaceMaintainer.getPrivateProfileHandle().getIdentifier());
    }
}
