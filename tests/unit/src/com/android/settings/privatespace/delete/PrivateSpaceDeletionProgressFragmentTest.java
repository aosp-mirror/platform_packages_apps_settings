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

package com.android.settings.privatespace.delete;

import static com.android.settings.privatespace.PrivateSpaceMaintainer.ErrorDeletingPrivateSpace.DELETE_PS_ERROR_INTERNAL;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Flags;
import android.platform.test.flag.junit.SetFlagsRule;

import androidx.test.annotation.UiThreadTest;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.privatespace.PrivateSpaceMaintainer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class PrivateSpaceDeletionProgressFragmentTest {
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    private Context mContext;
    private PrivateSpaceDeletionProgressFragment mFragment;
    private PrivateSpaceMaintainer mPrivateSpaceMaintainer;
    @Mock private PrivateSpaceMaintainer mPrivateSpaceMaintainerMock;

    @UiThreadTest
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        mFragment = new PrivateSpaceDeletionProgressFragment();
        PrivateSpaceDeletionProgressFragment.Injector injector =
                new PrivateSpaceDeletionProgressFragment.Injector() {
                    @Override
                    public PrivateSpaceMaintainer injectPrivateSpaceMaintainer(Context context) {
                        return mPrivateSpaceMaintainer;
                    }
                };
        mPrivateSpaceMaintainer = PrivateSpaceMaintainer.getInstance(mContext);
        mFragment.setPrivateSpaceMaintainer(injector);
    }

    @After
    public void tearDown() {
        mPrivateSpaceMaintainer.deletePrivateSpace();
    }

    @Test
    @UiThreadTest
    public void verifyMetricsConstant() {
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);
        assertThat(mFragment.getMetricsCategory()).isEqualTo(SettingsEnums.PRIVATE_SPACE_SETTINGS);
    }

    /** Tests that deletePrivateSpace() deletes the private space. */
    @Test
    @UiThreadTest
    public void deletePrivateSpace_deletesPS() {
        PrivateSpaceDeletionProgressFragment spyFragment = spy(mFragment);
        doNothing().when(spyFragment).showSuccessfulDeletionToast();
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);

        mPrivateSpaceMaintainer.createPrivateSpace();
        spyFragment.deletePrivateSpace();
        assertThat(mPrivateSpaceMaintainer.doesPrivateSpaceExist()).isFalse();
    }

    /** Tests that on deletion of the private space relevant toast is shown. */
    @Test
    @UiThreadTest
    public void deletePrivateSpace_onDeletion_showsDeletedToast() {
        PrivateSpaceDeletionProgressFragment spyFragment = spy(mFragment);
        doNothing().when(spyFragment).showSuccessfulDeletionToast();
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);

        mPrivateSpaceMaintainer.createPrivateSpace();
        spyFragment.deletePrivateSpace();
        verify(spyFragment).showSuccessfulDeletionToast();
    }

    /** Tests that on failing to delete the private space relevant toast is shown. */
    @Test
    @UiThreadTest
    public void deletePrivateSpace_onDeletionError_showsDeletionFailedToast() {
        PrivateSpaceDeletionProgressFragment spyFragment =
                spy(new PrivateSpaceDeletionProgressFragment());
        PrivateSpaceDeletionProgressFragment.Injector injector =
                new PrivateSpaceDeletionProgressFragment.Injector() {
                    @Override
                    PrivateSpaceMaintainer injectPrivateSpaceMaintainer(Context context) {
                        return mPrivateSpaceMaintainerMock;
                    }
                };
        spyFragment.setPrivateSpaceMaintainer(injector);
        doReturn(DELETE_PS_ERROR_INTERNAL).when(mPrivateSpaceMaintainerMock).deletePrivateSpace();
        doNothing().when(spyFragment).showDeletionInternalErrorToast();
        mSetFlagsRule.enableFlags(Flags.FLAG_ALLOW_PRIVATE_PROFILE);

        spyFragment.deletePrivateSpace();

        verify(spyFragment).showDeletionInternalErrorToast();
    }
}
