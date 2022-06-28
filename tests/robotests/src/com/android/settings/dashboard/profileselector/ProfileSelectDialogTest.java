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

package com.android.settings.dashboard.profileselector;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settingslib.drawer.ActivityTile;
import com.android.settingslib.drawer.CategoryKey;
import com.android.settingslib.drawer.Tile;

import com.google.android.collect.Lists;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ProfileSelectDialogTest {

    private static final UserHandle NORMAL_USER = new UserHandle(1111);
    private static final UserHandle REMOVED_USER = new UserHandle(2222);

    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();

    @Mock
    private UserManager mUserManager;

    private ActivityInfo mActivityInfo;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(mContext.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);
        final UserInfo userInfo = new UserInfo(
                NORMAL_USER.getIdentifier(), "test_user", UserInfo.FLAG_RESTRICTED);
        when(mUserManager.getUserInfo(NORMAL_USER.getIdentifier())).thenReturn(userInfo);
        mActivityInfo = new ActivityInfo();
        mActivityInfo.packageName = "pkg";
        mActivityInfo.name = "cls";
    }

    @Test
    public void updateUserHandlesIfNeeded_Normal() {
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        tile.userHandle.add(NORMAL_USER);

        ProfileSelectDialog.updateUserHandlesIfNeeded(mContext, tile);

        assertThat(tile.userHandle).hasSize(1);
        assertThat(tile.userHandle.get(0).getIdentifier()).isEqualTo(NORMAL_USER.getIdentifier());
        verify(mUserManager, never()).getUserInfo(NORMAL_USER.getIdentifier());
    }

    @Test
    public void updateUserHandlesIfNeeded_Remove() {
        final Tile tile = new ActivityTile(mActivityInfo, CategoryKey.CATEGORY_HOMEPAGE);
        tile.userHandle.add(REMOVED_USER);
        tile.userHandle.add(NORMAL_USER);
        tile.userHandle.add(REMOVED_USER);

        ProfileSelectDialog.updateUserHandlesIfNeeded(mContext, tile);

        assertThat(tile.userHandle).hasSize(1);
        assertThat(tile.userHandle.get(0).getIdentifier()).isEqualTo(NORMAL_USER.getIdentifier());
        verify(mUserManager, times(1)).getUserInfo(NORMAL_USER.getIdentifier());
        verify(mUserManager, times(2)).getUserInfo(REMOVED_USER.getIdentifier());
    }

    @Test
    public void createDialog_showsCorrectTitle() {
        mContext.setTheme(R.style.Theme_AppCompat);

        Dialog dialog = ProfileSelectDialog.createDialog(mContext, Lists.newArrayList(NORMAL_USER),
                (position) -> {
                });
        dialog.show();

        TextView titleView = dialog.findViewById(R.id.topPanel).findViewById(android.R.id.title);
        assertThat(titleView.getText().toString()).isEqualTo(
                mContext.getText(com.android.settingslib.R.string.choose_profile).toString());
    }
}
