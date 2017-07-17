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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.DashboardAdapter;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.drawer.Tile;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SuggestionAdapterTest {
    @Mock
    private Tile mSuggestion1;
    @Mock
    private Tile mSuggestion2;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SettingsActivity mActivity;

    private Context mContext;
    private SuggestionAdapter mSuggestionAdapter;
    private DashboardAdapter.DashboardItemHolder mSuggestionHolder;
    private List<Tile> mOneSuggestion;
    private List<Tile> mTwoSuggestions;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        FakeFeatureFactory.setupForTest(mActivity);

        mSuggestion1.title = "Test Suggestion 1";
        mSuggestion1.icon = mock(Icon.class);
        mSuggestion2.title = "Test Suggestion 2";
        mSuggestion2.icon = mock(Icon.class);
        mOneSuggestion = new ArrayList<>();
        mOneSuggestion.add(mSuggestion1);
        mTwoSuggestions = new ArrayList<>();
        mTwoSuggestions.add(mSuggestion1);
        mTwoSuggestions.add(mSuggestion2);
    }

    @Test
    public void getItemCount_shouldReturnListSize() {
        mSuggestionAdapter = new SuggestionAdapter(mContext, mOneSuggestion, new ArrayList<>());
        assertThat(mSuggestionAdapter.getItemCount()).isEqualTo(1);

        mSuggestionAdapter = new SuggestionAdapter(mContext, mTwoSuggestions, new ArrayList<>());
        assertThat(mSuggestionAdapter.getItemCount()).isEqualTo(2);
    }

    @Test
    public void getItemViewType_shouldReturnSuggestionTile() {
        mSuggestionAdapter = new SuggestionAdapter(mContext, mOneSuggestion, new ArrayList<>());
        assertThat(mSuggestionAdapter.getItemViewType(0))
            .isEqualTo(R.layout.suggestion_tile);
    }

    @Test
    public void onBindViewHolder_shouldSetListener() {
        final View view = spy(LayoutInflater.from(mContext).inflate(
            R.layout.suggestion_tile, new LinearLayout(mContext), true));
        mSuggestionHolder = new DashboardAdapter.DashboardItemHolder(view);
        mSuggestionAdapter = new SuggestionAdapter(mContext, mOneSuggestion, new ArrayList<>());

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);

        verify(view).setOnClickListener(any(View.OnClickListener.class));
    }

    @Test
    public void onBindViewHolder_shouldInflateRemoteView() {
        List<Tile> packages = makeSuggestions("pkg1");
        RemoteViews remoteViews = mock(RemoteViews.class);
        TextView textView = new TextView(RuntimeEnvironment.application);
        doReturn(textView).when(remoteViews).apply(any(Context.class), any(ViewGroup.class));
        packages.get(0).remoteViews = remoteViews;
        setupSuggestions(mActivity, packages);

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
        setupSuggestions(mActivity, packages);

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);
        mSuggestionHolder.itemView.performClick();

        assertThat(ShadowApplication.getInstance().getNextStartedActivity()).isNull();
        verify(mActivity, never()).startSuggestion(any(Intent.class));

        primary.performClick();

        verify(mActivity).startSuggestion(any(Intent.class));
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
        setupSuggestions(mActivity, packages);

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);
        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);

        ViewGroup itemView = (ViewGroup) mSuggestionHolder.itemView;
        assertThat(itemView.getChildCount()).isEqualTo(1);
    }

    private void setupSuggestions(Context context, List<Tile> suggestions) {
        mSuggestionAdapter = new SuggestionAdapter(context, suggestions, new ArrayList<>());
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

}
