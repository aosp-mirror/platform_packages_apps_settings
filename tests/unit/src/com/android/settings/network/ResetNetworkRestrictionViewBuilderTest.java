/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.os.UserManager;
import android.view.View;

import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import androidx.appcompat.app.AlertDialog;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class ResetNetworkRestrictionViewBuilderTest {

    @Mock
    private Activity mActivity;
    @Mock
    private UserManager mUserManager;
    @Mock
    private View mView;
    @Mock
    private AlertDialog.Builder mAlertDialogBuilder;

    private ResetNetworkRestrictionViewBuilder mBuilder;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(mUserManager).when(mActivity).getSystemService(Context.USER_SERVICE);
    }

    @Test
    public void build_getNull_whenNoRestriction() {
        mBuilder = new ResetNetworkRestrictionViewBuilder(mActivity) {
            @Override
            protected boolean hasUserRestriction() { return false; }
            @Override
            protected EnforcedAdmin getEnforceAdminByRestriction() { return null; }
        };

        assertThat(mBuilder.build()).isNull();
    }

    @Test
    public void build_getView_whenUserRestriction() {
        mBuilder = new ResetNetworkRestrictionViewBuilder(mActivity) {
            @Override
            protected boolean hasUserRestriction() { return true; }
            @Override
            protected View operationNotAllow() { return mView; }
        };

        assertThat(mBuilder.build()).isNotNull();
    }

    @Test
    public void build_getView_whenEnforceAdminRestriction() {
        doReturn(mAlertDialogBuilder).when(mAlertDialogBuilder).setOnDismissListener(any());

        String restriction = ResetNetworkRestrictionViewBuilder.mRestriction;
        EnforcedAdmin admin = RestrictedLockUtils.EnforcedAdmin
                .createDefaultEnforcedAdminWithRestriction(restriction);

        mBuilder = new ResetNetworkRestrictionViewBuilder(mActivity) {
            @Override
            protected boolean hasUserRestriction() { return false; }
            @Override
            protected EnforcedAdmin getEnforceAdminByRestriction() { return admin; }
            @Override
            protected AlertDialog.Builder createRestrictDialogBuilder(EnforcedAdmin admin) {
                return mAlertDialogBuilder;
            }
            @Override
            protected View createEmptyView() { return mView; }
        };

        assertThat(mBuilder.build()).isNotNull();
    }
}
