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

package com.android.settings.homepage.contextualcards.slices;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.android.settings.homepage.contextualcards.CardContentProvider;
import com.android.settings.homepage.contextualcards.CardDatabaseHelper;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ContextualCardFeedbackDialog;
import com.android.settings.homepage.contextualcards.ContextualCardsFragment;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.androidx.fragment.FragmentController;

@RunWith(SettingsRobolectricTestRunner.class)
public class SliceContextualCardControllerTest {

    private static final String TEST_SLICE_URI = "content://test/test";
    private static final String TEST_CARD_NAME = "test_card_name";

    private Context mContext;
    private CardContentProvider mProvider;
    private ContentResolver mResolver;
    private SliceContextualCardController mController;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mProvider = Robolectric.setupContentProvider(CardContentProvider.class);
        ShadowContentResolver.registerProviderInternal(CardContentProvider.CARD_AUTHORITY,
                mProvider);
        mResolver = mContext.getContentResolver();
        mController = spy(new SliceContextualCardController(mContext));
        mFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    @Test
    public void onDismissed_cardShouldBeMarkedAsDismissed() {
        final Uri providerUri = CardContentProvider.URI;
        mResolver.insert(providerUri, generateOneRow());
        doNothing().when(mController).showFeedbackDialog(any(ContextualCard.class));

        final ContextualCard card = getTestSliceCard();
        mController.onDismissed(card);

        final String[] columns = {CardDatabaseHelper.CardColumns.CARD_DISMISSED};
        final String selection = CardDatabaseHelper.CardColumns.NAME + "=?";
        final String[] selectionArgs = {TEST_CARD_NAME};
        final Cursor cr = mResolver.query(providerUri, columns, selection, selectionArgs, null);
        cr.moveToFirst();
        final int qryDismissed = cr.getInt(0);
        cr.close();

        assertThat(qryDismissed).isEqualTo(1);
        verify(mFeatureFactory.mContextualCardFeatureProvider).logContextualCardDismiss(
                mContext, card);
    }

    @Test
    public void onDismissed_noFeedbackEmail_shouldNotShowFeedbackDialog() {
        mResolver.insert(CardContentProvider.URI, generateOneRow());
        final ContextualCardsFragment fragment =
                FragmentController.of(new ContextualCardsFragment()).create().get();
        final ShadowActivity shadowActivity = Shadows.shadowOf(fragment.getActivity());

        mController.onDismissed(getTestSliceCard());

        assertThat(shadowActivity.getNextStartedActivity()).isNull();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void onDismissed_hasFeedbackEmail_shouldShowFeedbackDialog() {
        mResolver.insert(CardContentProvider.URI, generateOneRow());
        final ContextualCardsFragment fragment =
                FragmentController.of(new ContextualCardsFragment()).create().get();
        final ShadowActivity shadowActivity = Shadows.shadowOf(fragment.getActivity());

        mController.onDismissed(getTestSliceCard());

        assertThat(shadowActivity.getNextStartedActivity().getComponent().getClassName())
                .isEqualTo(ContextualCardFeedbackDialog.class.getName());
    }

    private ContentValues generateOneRow() {
        final ContentValues values = new ContentValues();
        values.put(CardDatabaseHelper.CardColumns.NAME, TEST_CARD_NAME);
        values.put(CardDatabaseHelper.CardColumns.TYPE, 1);
        values.put(CardDatabaseHelper.CardColumns.SCORE, 0.9);
        values.put(CardDatabaseHelper.CardColumns.SLICE_URI, TEST_SLICE_URI);
        values.put(CardDatabaseHelper.CardColumns.CATEGORY, 2);
        values.put(CardDatabaseHelper.CardColumns.PACKAGE_NAME, "com.android.settings");
        values.put(CardDatabaseHelper.CardColumns.APP_VERSION, 10001);
        values.put(CardDatabaseHelper.CardColumns.CARD_DISMISSED, 0);

        return values;
    }

    private ContextualCard getTestSliceCard() {
        return new ContextualCard.Builder()
                .setName(TEST_CARD_NAME)
                .setCardType(ContextualCard.CardType.SLICE)
                .setSliceUri(Uri.parse(TEST_SLICE_URI))
                .build();
    }
}
