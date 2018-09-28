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

package com.android.settings.homepage.contextualcards;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.android.settings.testutils.SettingsRobolectricTestRunner;

import com.google.android.settings.intelligence.libs.contextualcards.ContextualCard;
import com.google.android.settings.intelligence.libs.contextualcards.ContextualCardProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;

@RunWith(SettingsRobolectricTestRunner.class)
public class SettingsContextualCardProviderTest {

    private Context mContext;
    private ContentResolver mResolver;
    private Uri mUri;

    @Before
    public void setUp() {
        mContext = RuntimeEnvironment.application;
        mResolver = mContext.getContentResolver();
        mUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(SettingsContextualCardProvider.CARD_AUTHORITY)
                .build();
    }

    @Test
    public void contentProviderCall_returnCorrectSize() {
        final Bundle returnValue =
                mResolver.call(mUri, ContextualCardProvider.METHOD_GET_CARD_LIST, "", null);
        final ArrayList<ContextualCard> cards =
                returnValue.getParcelableArrayList(ContextualCardProvider.BUNDLE_CARD_LIST);
        assertThat(cards.size()).isEqualTo(2);
    }
}