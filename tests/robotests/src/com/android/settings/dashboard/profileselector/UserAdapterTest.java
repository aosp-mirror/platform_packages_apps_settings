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

package com.android.settings.dashboard.profileselector;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.pm.UserInfo;
import android.content.pm.UserProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.platform.test.flag.junit.SetFlagsRule;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.android.internal.widget.RecyclerView;
import com.android.settings.R;

import com.google.android.collect.Lists;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class UserAdapterTest {
    @Rule
    public MockitoRule mRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final int mPersonalUserId = UserHandle.myUserId();
    private static final int WORK_USER_ID = 1;
    private static final int PRIVATE_USER_ID = 2;

    @Mock
    private UserManager mUserManager;

    @Mock
    private UserInfo mPersonalUserInfo;

    @Mock
    private UserInfo mWorkUserInfo;
    @Mock
    private UserInfo mPrivateUserInfo;

    @Mock
    private UserAdapter.OnClickListener mOnClickListener;

    @Spy
    private Context mContext = ApplicationProvider.getApplicationContext();

    private UserProperties mPersonalUserProperties =
            new UserProperties.Builder()
                    .setShowInQuietMode(UserProperties.SHOW_IN_QUIET_MODE_DEFAULT)
                    .build();
    private UserProperties mWorkUserProperties =
            new UserProperties.Builder()
                    .setShowInQuietMode(UserProperties.SHOW_IN_QUIET_MODE_PAUSED)
                    .build();
    private UserProperties mPrivateUserProperties =
            new UserProperties.Builder()
                    .setShowInQuietMode(UserProperties.SHOW_IN_QUIET_MODE_HIDDEN)
                    .build();

    @Before
    public void setUp() {
        when(mContext.getSystemService(UserManager.class)).thenReturn(mUserManager);
        when(mUserManager.getUserInfo(mPersonalUserId)).thenReturn(mPersonalUserInfo);
        when(mUserManager.getUserInfo(WORK_USER_ID)).thenReturn(mWorkUserInfo);
        when(mUserManager.getUserInfo(PRIVATE_USER_ID)).thenReturn(mPrivateUserInfo);
        when(mUserManager.getUserProperties(UserHandle.of(mPersonalUserId)))
                .thenReturn(mPersonalUserProperties);
        when(mUserManager.getUserProperties(UserHandle.of(WORK_USER_ID)))
                .thenReturn(mWorkUserProperties);
        when(mUserManager.getUserProperties(UserHandle.of(PRIVATE_USER_ID)))
                .thenReturn(mPrivateUserProperties);
    }

    @Test
    public void createUserSpinnerAdapter_singleProfile_returnsNull() {
        when(mUserManager.getUserProfiles()).thenReturn(
                Lists.newArrayList(UserHandle.of(mPersonalUserId)));

        UserAdapter userSpinnerAdapter =
                UserAdapter.createUserSpinnerAdapter(mUserManager, mContext);

        assertThat(userSpinnerAdapter).isNull();
    }

    @Test
    public void createUserSpinnerAdapter_twoProfiles_succeed() {
        when(mUserManager.getUserProfiles()).thenReturn(
                Lists.newArrayList(UserHandle.of(mPersonalUserId), UserHandle.of(WORK_USER_ID)));

        UserAdapter userSpinnerAdapter =
                UserAdapter.createUserSpinnerAdapter(mUserManager, mContext);

        assertThat(userSpinnerAdapter.getCount()).isEqualTo(2);
        assertThat(userSpinnerAdapter.getUserHandle(0).getIdentifier()).isEqualTo(mPersonalUserId);
        assertThat(userSpinnerAdapter.getUserHandle(1).getIdentifier()).isEqualTo(WORK_USER_ID);
    }

    @Test
    public void createUserSpinnerAdapter_withWorkAndPrivateProfiles_shouldSucceed() {
        mSetFlagsRule.enableFlags(
                android.os.Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES,
                android.multiuser.Flags.FLAG_HANDLE_INTERLEAVED_SETTINGS_FOR_PRIVATE_SPACE);
        when(mUserManager.getUserProfiles()).thenReturn(
                Lists.newArrayList(
                        UserHandle.of(mPersonalUserId),
                        UserHandle.of(WORK_USER_ID),
                        UserHandle.of(PRIVATE_USER_ID)));

        UserAdapter userSpinnerAdapter =
                UserAdapter.createUserSpinnerAdapter(mUserManager, mContext);

        assertThat(userSpinnerAdapter.getCount()).isEqualTo(3);
        assertThat(userSpinnerAdapter.getUserHandle(0).getIdentifier()).isEqualTo(mPersonalUserId);
        assertThat(userSpinnerAdapter.getUserHandle(1).getIdentifier()).isEqualTo(WORK_USER_ID);
        assertThat(userSpinnerAdapter.getUserHandle(2).getIdentifier()).isEqualTo(PRIVATE_USER_ID);
    }

    @Test
    public void createUserSpinnerAdapter_withWorkAndQuietPrivateProfile_shouldShowTwoProfiles() {
        mSetFlagsRule.enableFlags(
                android.os.Flags.FLAG_ALLOW_PRIVATE_PROFILE,
                android.multiuser.Flags.FLAG_ENABLE_PRIVATE_SPACE_FEATURES,
                android.multiuser.Flags.FLAG_HANDLE_INTERLEAVED_SETTINGS_FOR_PRIVATE_SPACE);
        when(mUserManager.getUserProfiles()).thenReturn(
                Lists.newArrayList(
                        UserHandle.of(mPersonalUserId),
                        UserHandle.of(WORK_USER_ID),
                        UserHandle.of(PRIVATE_USER_ID)));
        when(mUserManager.isQuietModeEnabled(UserHandle.of(PRIVATE_USER_ID))).thenReturn(true);

        UserAdapter userSpinnerAdapter =
                UserAdapter.createUserSpinnerAdapter(mUserManager, mContext);

        assertThat(userSpinnerAdapter.getCount()).isEqualTo(2);
        assertThat(userSpinnerAdapter.getUserHandle(0).getIdentifier()).isEqualTo(mPersonalUserId);
        assertThat(userSpinnerAdapter.getUserHandle(1).getIdentifier()).isEqualTo(WORK_USER_ID);
    }

    @Test
    public void createUserRecycleViewAdapter_canBindViewHolderCorrectly() {
        ArrayList<UserHandle> userHandles =
                Lists.newArrayList(UserHandle.of(mPersonalUserId), UserHandle.of(WORK_USER_ID));
        FrameLayout parent = new FrameLayout(mContext);

        RecyclerView.Adapter<UserAdapter.ViewHolder> adapter =
                UserAdapter.createUserRecycleViewAdapter(mContext, userHandles, mOnClickListener);
        UserAdapter.ViewHolder holder = adapter.createViewHolder(parent, 0);
        adapter.bindViewHolder(holder, 0);
        holder.itemView.findViewById(R.id.button).performClick();

        assertThat(adapter.getItemCount()).isEqualTo(2);
        TextView textView = holder.itemView.findViewById(android.R.id.title);
        assertThat(textView.getText().toString()).isEqualTo("Personal");
        verify(mOnClickListener).onClick(anyInt());
    }
}
