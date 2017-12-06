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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.settings.suggestions.Suggestion;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.DashboardAdapter;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.drawer.Tile;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SuggestionAdapterTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SettingsActivity mActivity;
    @Mock
    private SuggestionControllerMixin mSuggestionControllerMixin;
    private FakeFeatureFactory mFeatureFactory;
    private Context mContext;
    private SuggestionAdapter mSuggestionAdapter;
    private DashboardAdapter.DashboardItemHolder mSuggestionHolder;
    private List<Tile> mOneSuggestion;
    private List<Tile> mTwoSuggestions;
    private List<Suggestion> mOneSuggestionV2;
    private List<Suggestion> mTwoSuggestionsV2;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();

        final Tile suggestion1 = new Tile();
        final Tile suggestion2 = new Tile();
        final Suggestion suggestion1V2 = new Suggestion.Builder("id1")
                .setTitle("Test suggestion 1")
                .build();
        final Suggestion suggestion2V2 = new Suggestion.Builder("id2")
                .setTitle("Test suggestion 2")
                .build();
        suggestion1.title = "Test Suggestion 1";
        suggestion1.icon = mock(Icon.class);
        suggestion2.title = "Test Suggestion 2";
        suggestion2.icon = mock(Icon.class);
        mOneSuggestion = new ArrayList<>();
        mOneSuggestion.add(suggestion1);
        mTwoSuggestions = new ArrayList<>();
        mTwoSuggestions.add(suggestion1);
        mTwoSuggestions.add(suggestion2);
        mOneSuggestionV2 = new ArrayList<>();
        mOneSuggestionV2.add(suggestion1V2);
        mTwoSuggestionsV2 = new ArrayList<>();
        mTwoSuggestionsV2.add(suggestion1V2);
        mTwoSuggestionsV2.add(suggestion2V2);
    }

    @Test
    public void getItemCount_shouldReturnListSize() {
        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
                mOneSuggestion, null /* suggestionV2 */, new ArrayList<>());
        assertThat(mSuggestionAdapter.getItemCount()).isEqualTo(1);

        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
                mTwoSuggestions, null /* suggestionV2 */, new ArrayList<>());
        assertThat(mSuggestionAdapter.getItemCount()).isEqualTo(2);
    }

    @Test
    public void getItemCount_v2_shouldReturnListSize() {
        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
                null /* suggestions */, mOneSuggestionV2, new ArrayList<>());
        assertThat(mSuggestionAdapter.getItemCount()).isEqualTo(1);

        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
                null /* suggestions */, mTwoSuggestionsV2, new ArrayList<>());
        assertThat(mSuggestionAdapter.getItemCount()).isEqualTo(2);
    }

    @Test
    public void getItemViewType_shouldReturnSuggestionTile() {
        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
                mOneSuggestion, null /* suggestionV2 */, new ArrayList<>());
        assertThat(mSuggestionAdapter.getItemViewType(0))
                .isEqualTo(R.layout.suggestion_tile);
    }

    @Test
    public void getItemViewType_v2_shouldReturnSuggestionTile() {
        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
                null /* suggestions */, mOneSuggestionV2, new ArrayList<>());
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
                null /* suggestions */, suggestions, new ArrayList<>());

        assertThat(mSuggestionAdapter.getItemViewType(0))
                .isEqualTo(R.layout.suggestion_tile_with_button);
    }

    @Test
    public void onBindViewHolder_shouldSetListener() {
        final View view = spy(LayoutInflater.from(mContext).inflate(
                R.layout.suggestion_tile, new LinearLayout(mContext), true));
        mSuggestionHolder = new DashboardAdapter.DashboardItemHolder(view);
        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
                mOneSuggestion, null /* suggestionV2 */, new ArrayList<>());

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);

        verify(view).setOnClickListener(any(View.OnClickListener.class));
    }

    @Test
    public void onBindViewHolder_shouldLog() {
        final View view = spy(LayoutInflater.from(mContext).inflate(
                R.layout.suggestion_tile, new LinearLayout(mContext), true));
        mSuggestionHolder = new DashboardAdapter.DashboardItemHolder(view);
        mSuggestionAdapter = new SuggestionAdapter(mContext, mSuggestionControllerMixin,
                null /* suggestionV1*/, mOneSuggestionV2, new ArrayList<>());

        // Bind twice
        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);
        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);

        // Log once
        verify(mFeatureFactory.metricsFeatureProvider).action(
                mContext, MetricsProto.MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                mOneSuggestionV2.get(0).getId());
    }

    @Test
    public void onBindViewHolder_shouldInflateRemoteView() {
        List<Tile> packages = makeSuggestions("pkg1");
        RemoteViews remoteViews = mock(RemoteViews.class);
        TextView textView = new TextView(RuntimeEnvironment.application);
        doReturn(textView).when(remoteViews).apply(any(Context.class), any(ViewGroup.class));
        packages.get(0).remoteViews = remoteViews;
        setupSuggestions(mActivity, packages, null);

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);

        assertThat(textView.getParent()).isSameAs(mSuggestionHolder.itemView);
        mSuggestionHolder.itemView.performClick();

        verify(mActivity).startSuggestion(any(Intent.class));
    }

    @Test
    public void onBindViewHolder_primaryViewShouldHandleClick() {
        Context context =
                new ContextThemeWrapper(RuntimeEnvironment.application, R.style.Theme_Settings);

        List<Tile> packages = makeSuggestions("pkg1");
        RemoteViews remoteViews = mock(RemoteViews.class);
        FrameLayout layout = new FrameLayout(context);
        Button primary = new Button(context);
        primary.setId(android.R.id.primary);
        layout.addView(primary);
        doReturn(layout).when(remoteViews).apply(any(Context.class), any(ViewGroup.class));
        packages.get(0).remoteViews = remoteViews;
        setupSuggestions(mActivity, packages, null /* suggestionV2 */);

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);
        mSuggestionHolder.itemView.performClick();

        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
        verify(mActivity, never()).startSuggestion(any(Intent.class));

        primary.performClick();

        verify(mActivity).startSuggestion(any(Intent.class));
    }

    @Test
    public void onBindViewHolder_v2_itemViewShouldHandleClick()
            throws PendingIntent.CanceledException {
        final List<Suggestion> suggestions = makeSuggestionsV2("pkg1");
        setupSuggestions(mActivity, null /* suggestionV1 */, suggestions);

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);
        mSuggestionHolder.itemView.performClick();

        verify(mSuggestionControllerMixin).launchSuggestion(suggestions.get(0));
        verify(suggestions.get(0).getPendingIntent()).send();
    }

    @Test
    public void onBindViewHolder_viewsShouldClearOnRebind() {
        Context context =
                new ContextThemeWrapper(RuntimeEnvironment.application, R.style.Theme_Settings);

        List<Tile> packages = makeSuggestions("pkg1");
        RemoteViews remoteViews = mock(RemoteViews.class);
        FrameLayout layout = new FrameLayout(context);
        Button primary = new Button(context);
        primary.setId(android.R.id.primary);
        layout.addView(primary);
        doReturn(layout).when(remoteViews).apply(any(Context.class), any(ViewGroup.class));
        packages.get(0).remoteViews = remoteViews;
        setupSuggestions(mActivity, packages, null /* suggestionV2 */);

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);
        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);

        ViewGroup itemView = (ViewGroup) mSuggestionHolder.itemView;
        assertThat(itemView.getChildCount()).isEqualTo(1);
    }

    @Test
    public void getSuggestionsV2_shouldReturnSuggestionWhenMatch() {
        final List<Suggestion> suggestionsV2 = makeSuggestionsV2("pkg1");
        setupSuggestions(mActivity, null /* suggestionV1 */, suggestionsV2);

        assertThat(mSuggestionAdapter.getSuggestion(0)).isNull();
        assertThat(mSuggestionAdapter.getSuggestionsV2(0)).isNotNull();

        List<Tile> suggestionsV1 = makeSuggestions("pkg1");
        setupSuggestions(mActivity, suggestionsV1, null /* suggestionV2 */);

        assertThat(mSuggestionAdapter.getSuggestionsV2(0)).isNull();
        assertThat(mSuggestionAdapter.getSuggestion(0)).isNotNull();

    }

    private void setupSuggestions(Context context, List<Tile> suggestions,
            List<Suggestion> suggestionsV2) {
        mSuggestionAdapter = new SuggestionAdapter(context, mSuggestionControllerMixin,
                suggestions, suggestionsV2, new ArrayList<>());
        mSuggestionHolder = mSuggestionAdapter.onCreateViewHolder(
                new FrameLayout(RuntimeEnvironment.application),
                mSuggestionAdapter.getItemViewType(0));
    }

    private List<Tile> makeSuggestions(String... pkgNames) {
        final List<Tile> suggestions = new ArrayList<>();
        for (String pkgName : pkgNames) {
            Tile suggestion = new Tile();
            suggestion.intent = new Intent("action");
            suggestion.intent.setComponent(new ComponentName(pkgName, "cls"));
            suggestions.add(suggestion);
            suggestion.icon = mock(Icon.class);
        }
        return suggestions;
    }

    private List<Suggestion> makeSuggestionsV2(String... pkgNames) {
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
