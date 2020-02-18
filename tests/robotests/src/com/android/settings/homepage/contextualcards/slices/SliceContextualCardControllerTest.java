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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.settings.SettingsEnums;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;

import com.android.settings.R;
import com.android.settings.homepage.contextualcards.CardContentProvider;
import com.android.settings.homepage.contextualcards.CardDatabaseHelper;
import com.android.settings.homepage.contextualcards.ContextualCard;
import com.android.settings.homepage.contextualcards.ContextualCardFeatureProvider;
import com.android.settings.homepage.contextualcards.ContextualCardFeatureProviderImpl;
import com.android.settings.homepage.contextualcards.ContextualCardFeedbackDialog;
import com.android.settings.homepage.contextualcards.ContextualCardsFragment;
import com.android.settings.testutils.FakeFeatureFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowContentResolver;
import org.robolectric.shadows.androidx.fragment.FragmentController;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
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
        final ContextualCardFeatureProvider cardFeatureProvider =
                new ContextualCardFeatureProviderImpl(mContext);
        mFeatureFactory.mContextualCardFeatureProvider = cardFeatureProvider;
    }

    @After
    public void tearDown() {
        CardDatabaseHelper.getInstance(mContext).close();
    }

    @Test
    public void onDismissed_cardShouldBeMarkedAsDismissedWithTimestamp() {
        final Uri providerUri = CardContentProvider.REFRESH_CARD_URI;
        mResolver.insert(providerUri, generateOneRow());
        doNothing().when(mController).showFeedbackDialog(any(ContextualCard.class));

        final ContextualCard card = getTestSliceCard();
        mController.onDismissed(card);

        final String[] columns = {CardDatabaseHelper.CardColumns.DISMISSED_TIMESTAMP};
        final String selection = CardDatabaseHelper.CardColumns.NAME + "=?";
        final String[] selectionArgs = {TEST_CARD_NAME};
        final Cursor cr = mResolver.query(providerUri, columns, selection, selectionArgs, null);
        cr.moveToFirst();
        final long dismissedTimestamp = cr.getLong(0);
        cr.close();

        assertThat(dismissedTimestamp).isNotEqualTo(0L);
        verify(mFeatureFactory.metricsFeatureProvider).action(any(),
                eq(SettingsEnums.ACTION_CONTEXTUAL_CARD_DISMISS), any(String.class));
    }

    @Test
    public void onDismissed_feedbackDisabled_shouldNotShowFeedbackDialog() {
        mResolver.insert(CardContentProvider.REFRESH_CARD_URI, generateOneRow());
        final ContextualCardsFragment fragment =
                FragmentController.of(new ContextualCardsFragment()).create().get();
        final ShadowActivity shadowActivity = Shadows.shadowOf(fragment.getActivity());
        doReturn(false).when(mController).isFeedbackEnabled(anyString());

        mController.onDismissed(getTestSliceCard());

        assertThat(shadowActivity.getNextStartedActivity()).isNull();
    }

    @Test
    public void onDismissed_feedbackEnabled_shouldShowFeedbackDialog() {
        mResolver.insert(CardContentProvider.REFRESH_CARD_URI, generateOneRow());
        final ContextualCardsFragment fragment =
                FragmentController.of(new ContextualCardsFragment()).create().get();
        final ShadowActivity shadowActivity = Shadows.shadowOf(fragment.getActivity());
        doReturn(true).when(mController).isFeedbackEnabled(anyString());

        mController.onDismissed(getTestSliceCard());

        assertThat(shadowActivity.getNextStartedActivity().getComponent().getClassName())
                .isEqualTo(ContextualCardFeedbackDialog.class.getName());
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isFeedbackEnabled_hasFeedbackEmail_debug_returnTrue() {
        ReflectionHelpers.setStaticField(Build.class, "IS_DEBUGGABLE", true);
        final String email = mContext.getString(R.string.config_contextual_card_feedback_email);

        assertThat(mController.isFeedbackEnabled(email)).isTrue();
    }

    @Test
    @Config(qualifiers = "mcc999")
    public void isFeedbackEnabled_hasFeedbackEmail_user_returnFalse() {
        ReflectionHelpers.setStaticField(Build.class, "IS_DEBUGGABLE", false);
        final String email = mContext.getString(R.string.config_contextual_card_feedback_email);

        assertThat(mController.isFeedbackEnabled(email)).isFalse();
    }

    @Test
    public void isFeedbackEnabled_noFeedbackEmail_debug_returnFalse() {
        ReflectionHelpers.setStaticField(Build.class, "IS_DEBUGGABLE", true);
        final String email = mContext.getString(R.string.config_contextual_card_feedback_email);

        assertThat(mController.isFeedbackEnabled(email)).isFalse();
    }

    @Test
    public void isFeedbackEnabled_noFeedbackEmail_user_returnFalse() {
        ReflectionHelpers.setStaticField(Build.class, "IS_DEBUGGABLE", false);
        final String email = mContext.getString(R.string.config_contextual_card_feedback_email);

        assertThat(mController.isFeedbackEnabled(email)).isFalse();
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
        values.put(CardDatabaseHelper.CardColumns.DISMISSED_TIMESTAMP, 0L);

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
