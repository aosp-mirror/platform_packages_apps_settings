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

package com.android.settings;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentInterface;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.UserProperties;
import android.net.Uri;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

/** Unittest for DefaultRingtonePreference. */
@RunWith(AndroidJUnit4.class)
public class DefaultRingtonePreferenceTest {

    private static final int OWNER_USER_ID = 1;
    private static final int OTHER_USER_ID = 10;
    private static final int INVALID_RINGTONE_TYPE = 0;
    private DefaultRingtonePreference mDefaultRingtonePreference;

    @Mock
    private ContentResolver mContentResolver;
    @Mock
    private UserManager mUserManager;
    private Uri mRingtoneUri;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Context context = spy(ApplicationProvider.getApplicationContext());
        mContentResolver = ContentResolver.wrap(Mockito.mock(ContentInterface.class));
        when(context.getContentResolver()).thenReturn(mContentResolver);

        mDefaultRingtonePreference = spy(new DefaultRingtonePreference(context, null /* attrs */));
        doReturn(context).when(mDefaultRingtonePreference).getContext();

        // Use INVALID_RINGTONE_TYPE to return early in RingtoneManager.setActualDefaultRingtoneUri
        when(mDefaultRingtonePreference.getRingtoneType())
                .thenReturn(INVALID_RINGTONE_TYPE);

        mDefaultRingtonePreference.setUserId(OWNER_USER_ID);
        mDefaultRingtonePreference.mUserContext = context;
        when(mDefaultRingtonePreference.isDefaultRingtone(any(Uri.class))).thenReturn(false);

        when(context.getSystemServiceName(UserManager.class)).thenReturn(Context.USER_SERVICE);
        when(context.getSystemService(Context.USER_SERVICE)).thenReturn(mUserManager);

        UserProperties userProperties = new UserProperties.Builder().setMediaSharedWithParent(false)
                .build();
        when(mUserManager.getUserProperties(UserHandle.of(OTHER_USER_ID))).thenReturn(
                userProperties);

        mRingtoneUri = Uri.parse("content://none");
    }

    @Test
    public void onSaveRingtone_nullMimeType_shouldNotSetRingtone() {
        when(mContentResolver.getType(mRingtoneUri)).thenReturn(null);

        mDefaultRingtonePreference.onSaveRingtone(mRingtoneUri);

        verify(mDefaultRingtonePreference, never()).setActualDefaultRingtoneUri(mRingtoneUri);
    }

    @Test
    public void onSaveRingtone_notAudioMimeType_shouldNotSetRingtone() {
        when(mContentResolver.getType(mRingtoneUri)).thenReturn("text/plain");

        mDefaultRingtonePreference.onSaveRingtone(mRingtoneUri);

        verify(mDefaultRingtonePreference, never()).setActualDefaultRingtoneUri(mRingtoneUri);
    }

    @Test
    public void onSaveRingtone_notManagedProfile_shouldNotSetRingtone() {
        mRingtoneUri = Uri.parse("content://" + OTHER_USER_ID + "@ringtone");
        when(mContentResolver.getType(mRingtoneUri)).thenReturn("audio/*");
        when(mUserManager.isSameProfileGroup(OWNER_USER_ID, OTHER_USER_ID)).thenReturn(true);
        when(mUserManager.getProfileParent(UserHandle.of(OTHER_USER_ID))).thenReturn(
                UserHandle.of(OWNER_USER_ID));
        when(mUserManager.isManagedProfile(OTHER_USER_ID)).thenReturn(false);

        mDefaultRingtonePreference.onSaveRingtone(mRingtoneUri);

        verify(mDefaultRingtonePreference, never()).setActualDefaultRingtoneUri(mRingtoneUri);
    }

    @Test
    public void onSaveRingtone_notSameUser_shouldNotSetRingtone() {
        mRingtoneUri = Uri.parse("content://" + OTHER_USER_ID + "@ringtone");
        when(mContentResolver.getType(mRingtoneUri)).thenReturn("audio/*");
        when(mUserManager.isSameProfileGroup(OWNER_USER_ID, OTHER_USER_ID)).thenReturn(false);

        mDefaultRingtonePreference.onSaveRingtone(mRingtoneUri);

        verify(mDefaultRingtonePreference, never()).setActualDefaultRingtoneUri(mRingtoneUri);
    }

    @Test
    public void onSaveRingtone_isManagedProfile_shouldSetRingtone() {
        mRingtoneUri = Uri.parse("content://" + OTHER_USER_ID + "@ringtone");
        when(mContentResolver.getType(mRingtoneUri)).thenReturn("audio/*");
        when(mUserManager.isSameProfileGroup(OWNER_USER_ID, OTHER_USER_ID)).thenReturn(true);
        when(mUserManager.getProfileParent(UserHandle.of(OTHER_USER_ID))).thenReturn(
                UserHandle.of(OWNER_USER_ID));
        when(mUserManager.isManagedProfile(OTHER_USER_ID)).thenReturn(true);

        mDefaultRingtonePreference.onSaveRingtone(mRingtoneUri);

        verify(mDefaultRingtonePreference).setActualDefaultRingtoneUri(mRingtoneUri);
    }

    @Test
    public void onSaveRingtone_defaultUri_shouldSetRingtone() {
        mRingtoneUri = Uri.parse("default_ringtone");
        when(mDefaultRingtonePreference.isDefaultRingtone(any(Uri.class))).thenReturn(true);

        mDefaultRingtonePreference.onSaveRingtone(mRingtoneUri);

        verify(mDefaultRingtonePreference).setActualDefaultRingtoneUri(mRingtoneUri);
    }
}
