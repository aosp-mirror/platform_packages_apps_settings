/*
 * Copyright (C) 2018 The Android Open Source Project
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
 *
 */

package com.android.settings.slices;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.SettingsSlicesContract;

import com.android.settingslib.SliceBroadcastRelay;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public class SliceRelayReceiverTest {

  private Context mContext;
  private SliceRelayReceiver mSliceRelayReceiver;

  @Before
  public void setUp() {
      mContext = spy(RuntimeEnvironment.application);
      mSliceRelayReceiver = new SliceRelayReceiver();
  }


  @Test
  public void onReceive_extraUri_notifiesChangeOnUri() {
      // Monitor the ContentResolver
      final ContentResolver resolver = spy(mContext.getContentResolver());
      doReturn(resolver).when(mContext).getContentResolver();

      final Uri uri = new Uri.Builder()
          .scheme(ContentResolver.SCHEME_CONTENT)
          .authority(SettingsSlicesContract.AUTHORITY)
          .appendPath("path")
          .build();

      final Intent intent = new Intent();
    intent.putExtra(SliceBroadcastRelay.EXTRA_URI, uri.toString());

    mSliceRelayReceiver.onReceive(mContext, intent);

    verify(resolver).notifyChange(eq(uri), any());
  }
}
