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

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Unittest for DefaultRingtonePreference. */
@RunWith(AndroidJUnit4.class)
public class DefaultRingtonePreferenceTest {

    private DefaultRingtonePreference mDefaultRingtonePreference;

    @Mock
    private ContentResolver mContentResolver;
    @Mock
    private Uri mRingtoneUri;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Context context = spy(ApplicationProvider.getApplicationContext());
        doReturn(mContentResolver).when(context).getContentResolver();

        mDefaultRingtonePreference = spy(new DefaultRingtonePreference(context, null /* attrs */));
        doReturn(context).when(mDefaultRingtonePreference).getContext();
        when(mDefaultRingtonePreference.getRingtoneType())
                .thenReturn(RingtoneManager.TYPE_RINGTONE);
        mDefaultRingtonePreference.setUserId(1);
        mDefaultRingtonePreference.mUserContext = context;
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
}
