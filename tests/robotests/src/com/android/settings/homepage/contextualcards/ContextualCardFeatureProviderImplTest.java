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
 */

package com.android.settings.homepage.contextualcards;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.UserHandle;

import com.android.settings.intelligence.ContextualCardProto.ContextualCardList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class ContextualCardFeatureProviderImplTest {

    private Context mContext;
    private ContextualCardFeatureProviderImpl mImpl;

    @Before
    public void setUp() {
        mContext = spy(RuntimeEnvironment.application);
        mImpl = new ContextualCardFeatureProviderImpl(mContext);
    }

    @Test
    public void sendBroadcast_emptyAction_notSendBroadcast() {
        final Intent intent = new Intent();
        mImpl.sendBroadcast(intent);

        verify(mContext, never()).sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void sendBroadcast_hasAction_sendBroadcast() {
        final Intent intent = new Intent();
        mImpl.sendBroadcast(intent);

        verify(mContext).sendBroadcastAsUser(intent, UserHandle.ALL);
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void logContextualCardDisplay_hasAction_sendBroadcast() {
        mImpl.logContextualCardDisplay(new ArrayList<>(), new ArrayList<>());

        verify(mContext).sendBroadcastAsUser(any(Intent.class), any());
    }

    @Test
    public void serialize_hasSizeTwo_returnSizeTwo() {
        final List<ContextualCard> cards = new ArrayList<>();
        cards.add(new ContextualCard.Builder()
                .setName("name1")
                .setSliceUri(Uri.parse("uri1"))
                .build());
        cards.add(new ContextualCard.Builder()
                .setName("name2")
                .setSliceUri(Uri.parse("uri2"))
                .build());


        final byte[] data = mImpl.serialize(cards);

        try {
            assertThat(ContextualCardList
                    .parseFrom(data).getCardCount()).isEqualTo(cards.size());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}