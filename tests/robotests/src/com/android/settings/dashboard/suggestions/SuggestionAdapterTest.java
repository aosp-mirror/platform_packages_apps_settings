/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.dashboard.suggestions;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.service.settings.suggestions.Suggestion;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.dashboard.DashboardAdapter;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.testutils.shadow.ShadowCardView;
import com.android.settingslib.suggestions.SuggestionControllerMixin;
import com.android.settingslib.utils.IconCache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(shadows = ShadowCardView.class)
public class SuggestionAdapterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SettingsActivity mActivity;
    @Mock
    private SuggestionControllerMixin mSuggestionControllerMixin;
    @Mock
    private Resources mResources;
    @Mock
    private WindowManager mWindowManager;

    private FakeFeatureFactory mFeatureFactory;
    private Context mContext;
    private SuggestionAdapter mSuggestionAdapter;
    private DashboardAdapter.DashboardItemHolder mSuggestionHolder;
    private List<Suggestion> mOneSuggestion;
    private List<Suggestion> mTwoSuggestions;
    private SuggestionAdapter.CardConfig mConfig;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = spy(RuntimeEnvironment.application);
        when(mActivity.getSystemService(Context.WINDOW_SERVICE)).thenReturn(mWindowManager);
        when(mActivity.getResources()).thenReturn(mResources);
        when(mResources.getDimensionPixelOffset(R.dimen.suggestion_card_inner_margin))
            .thenReturn(10);
        when(mResources.getDimensionPixelOffset(R.dimen.suggestion_card_outer_margin))
            .thenReturn(20);
        mConfig = spy(SuggestionAdapter.CardConfig.get(mActivity));

        mFeatureFactory = FakeFeatureFactory.setupForTest();

        final Suggestion suggestion1 = new Suggestion.Builder("id1")
                .setTitle("Test suggestion 1")
                .build();
        final Suggestion suggestion2 = new Suggestion.Builder("id2")
                .setTitle("Test suggestion 2")
                .build();
        mOneSuggestion = new ArrayList<>();
        mOneSuggestion.add(suggestion1);
        mTwoSuggestions = new ArrayList<>();
        mTwoSuggestions.add(suggestion1);
        mTwoSuggestions.add(suggestion2);
    }

    @Test
    public void getItemCount_shouldReturnListSize() {
        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
            null /* savedInstanceState */, null /* callback */, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(mOneSuggestion);
        assertThat(mSuggestionAdapter.getItemCount()).isEqualTo(1);

        mSuggestionAdapter.setSuggestions(mTwoSuggestions);
        assertThat(mSuggestionAdapter.getItemCount()).isEqualTo(2);
    }

    @Test
    public void getItemViewType_shouldReturnSuggestionTile() {
        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
            null /* savedInstanceState */, null /* callback */, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(mOneSuggestion);
        assertThat(mSuggestionAdapter.getItemViewType(0))
            .isEqualTo(R.layout.suggestion_tile);
    }

    @Test
    public void getItemType_hasButton_shouldReturnSuggestionWithButton() {
        final List<Suggestion> suggestions = new ArrayList<>();
        suggestions.add(new Suggestion.Builder("id")
                .setFlags(Suggestion.FLAG_HAS_BUTTON)
                .setTitle("123")
                .setSummary("456")
                .build());
        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
            null /* savedInstanceState */, null /* callback */, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(suggestions);

        assertThat(mSuggestionAdapter.getItemViewType(0))
            .isEqualTo(R.layout.suggestion_tile_with_button);
    }

    @Test
    public void onBindViewHolder_shouldLog() {
        final View view = spy(LayoutInflater.from(mContext).inflate(
            R.layout.suggestion_tile, new LinearLayout(mContext), true));
        mSuggestionHolder = new DashboardAdapter.DashboardItemHolder(view);
        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
            null /* savedInstanceState */, null /* callback */, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(mOneSuggestion);
        doReturn("sans").when(mContext).getString(anyInt());

        // Bind twice
        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);
        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);

        // Log once
        verify(mFeatureFactory.metricsFeatureProvider).action(
                mContext, MetricsProto.MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                mOneSuggestion.get(0).getId());
    }

    @Test
    public void onBindViewHolder_itemViewShouldHandleClick()
            throws PendingIntent.CanceledException {
        final List<Suggestion> suggestions = makeSuggestions("pkg1");
        setupSuggestions(mActivity, suggestions);

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);
        mSuggestionHolder.itemView.performClick();

        verify(mSuggestionControllerMixin).launchSuggestion(suggestions.get(0));
        verify(suggestions.get(0).getPendingIntent()).send();
    }

    @Test
    public void onBindViewHolder_hasButton_buttonShouldHandleClick()
        throws PendingIntent.CanceledException {
        final List<Suggestion> suggestions = new ArrayList<>();
        final PendingIntent pendingIntent = mock(PendingIntent.class);
        suggestions.add(new Suggestion.Builder("id")
            .setFlags(Suggestion.FLAG_HAS_BUTTON)
            .setTitle("123")
            .setSummary("456")
            .setPendingIntent(pendingIntent)
            .build());
        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
            null /* savedInstanceState */, null /* callback */, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(suggestions);
        mSuggestionHolder = mSuggestionAdapter.onCreateViewHolder(
            new FrameLayout(RuntimeEnvironment.application),
            mSuggestionAdapter.getItemViewType(0));
        doReturn("sans").when(mContext).getString(anyInt());

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);
        mSuggestionHolder.itemView.findViewById(android.R.id.primary).performClick();

        verify(mSuggestionControllerMixin).launchSuggestion(suggestions.get(0));
        verify(pendingIntent).send();
    }

    @Test
    public void getSuggestions_shouldReturnSuggestionWhenMatch() {
        final List<Suggestion> suggestions = makeSuggestions("pkg1");
        setupSuggestions(mActivity, suggestions);

        assertThat(mSuggestionAdapter.getSuggestion(0)).isNotNull();
    }

    @Test
    public void onBindViewHolder_closeButtonShouldHandleClick()
        throws PendingIntent.CanceledException {
        final List<Suggestion> suggestions = makeSuggestions("pkg1");
        final SuggestionAdapter.Callback callback = mock(SuggestionAdapter.Callback.class);
        mSuggestionAdapter = new SuggestionAdapter(mActivity, mSuggestionControllerMixin,
            null /* savedInstanceState */, callback, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(suggestions);
        mSuggestionHolder = mSuggestionAdapter.onCreateViewHolder(
            new FrameLayout(RuntimeEnvironment.application),
            mSuggestionAdapter.getItemViewType(0));

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);
        mSuggestionHolder.itemView.findViewById(R.id.close_button).performClick();

        final Suggestion suggestion = suggestions.get(0);
        verify(mFeatureFactory.suggestionsFeatureProvider).dismissSuggestion(
            mActivity, mSuggestionControllerMixin, suggestion);
        verify(callback).onSuggestionClosed(suggestion);
    }

    @Test
    public void onBindViewHolder_iconNotTintable_shouldNotTintIcon()
            throws PendingIntent.CanceledException {
        final Icon icon = mock(Icon.class);
        final Suggestion suggestion = new Suggestion.Builder("pkg1")
            .setPendingIntent(mock(PendingIntent.class))
            .setIcon(icon)
            .build();
        final List<Suggestion> suggestions = new ArrayList<>();
        suggestions.add(suggestion);
        mSuggestionAdapter = new SuggestionAdapter(mActivity, mSuggestionControllerMixin,
            null /* savedInstanceState */, null /* callback */, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(suggestions);
        mSuggestionHolder = mSuggestionAdapter.onCreateViewHolder(
            new FrameLayout(RuntimeEnvironment.application),
            mSuggestionAdapter.getItemViewType(0));
        IconCache cache = mock(IconCache.class);
        final Drawable drawable = mock(Drawable.class);
        when(cache.getIcon(icon)).thenReturn(drawable);
        ReflectionHelpers.setField(mSuggestionAdapter, "mCache", cache);

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);

        verify(drawable, never()).setTint(anyInt());
    }

    @Test
    public void onBindViewHolder_iconTintable_shouldTintIcon()
            throws PendingIntent.CanceledException {
        final Icon icon = mock(Icon.class);
        final int FLAG_ICON_TINTABLE = 1 << 1;
        final Suggestion suggestion = new Suggestion.Builder("pkg1")
            .setPendingIntent(mock(PendingIntent.class))
            .setIcon(icon)
            .setFlags(FLAG_ICON_TINTABLE)
            .build();
        final List<Suggestion> suggestions = new ArrayList<>();
        suggestions.add(suggestion);
        mSuggestionAdapter = new SuggestionAdapter(mActivity, mSuggestionControllerMixin,
            null /* savedInstanceState */, null /* callback */, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(suggestions);
        mSuggestionHolder = mSuggestionAdapter.onCreateViewHolder(
            new FrameLayout(RuntimeEnvironment.application),
            mSuggestionAdapter.getItemViewType(0));
        IconCache cache = mock(IconCache.class);
        final Drawable drawable = mock(Drawable.class);
        when(cache.getIcon(icon)).thenReturn(drawable);
        ReflectionHelpers.setField(mSuggestionAdapter, "mCache", cache);
        TypedArray typedArray = mock(TypedArray.class);
        final int colorAccent = 1234;
        when(mActivity.obtainStyledAttributes(any())).thenReturn(typedArray);
        when(typedArray.getColor(anyInt(), anyInt())).thenReturn(colorAccent);

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);

        verify(drawable).setTint(colorAccent);
    }

    @Test
    public void onBindViewHolder_closeButtonShouldHaveContentDescription()
        throws PendingIntent.CanceledException {
        final List<Suggestion> suggestions = makeSuggestions("pkg1");
        setupSuggestions(mActivity, suggestions);

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);

        assertThat(
            mSuggestionHolder.itemView.findViewById(R.id.close_button).getContentDescription())
            .isNotNull();
    }

    @Test
    public void setCardLayout_twoCards_shouldSetCardWidthToHalfScreenMinusPadding() {
        final List<Suggestion> suggestions = makeSuggestions("pkg1");
        setupSuggestions(mContext, suggestions);
        doReturn(200).when(mConfig).getScreenWidth();

        mConfig.setCardLayout(mSuggestionHolder, 0);

        /*
         * card width = (screen width - left margin - inner margin - right margin) / 2
         *            = (200 - 20 - 10 - 20) / 2
         *            = 75
         */
        assertThat(mSuggestionHolder.itemView.getLayoutParams().width).isEqualTo(75);
    }

    private void setupSuggestions(Context context, List<Suggestion> suggestions) {
        mSuggestionAdapter = new SuggestionAdapter(context, mSuggestionControllerMixin,
            null /* savedInstanceState */, null /* callback */, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(suggestions);
        mSuggestionHolder = mSuggestionAdapter.onCreateViewHolder(
                new FrameLayout(RuntimeEnvironment.application),
                mSuggestionAdapter.getItemViewType(0));
    }

    private List<Suggestion> makeSuggestions(String... pkgNames) {
        final List<Suggestion> suggestions = new ArrayList<>();
        for (String pkgName : pkgNames) {
            final Suggestion suggestion = new Suggestion.Builder(pkgName)
                    .setPendingIntent(mock(PendingIntent.class))
                    .build();
            suggestions.add(suggestion);
        }
        return suggestions;
    }
}
