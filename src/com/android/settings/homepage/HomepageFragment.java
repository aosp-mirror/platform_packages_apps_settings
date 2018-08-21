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

package com.android.settings.homepage;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsHomepageActivity;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.dashboard.DashboardSummary;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.SearchFeatureProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class HomepageFragment extends InstrumentedFragment {

    private static final String TAG = "HomepageFragment";
    private static final String SAVE_BOTTOMBAR_STATE = "bottombar_state";
    private static final String SAVE_BOTTOM_FRAGMENT_LOADED = "bottom_fragment_loaded";

    private RecyclerView mCardsContainer;
    private LinearLayoutManager mLayoutManager;

    private FloatingActionButton mSearchButton;
    private BottomSheetBehavior mBottomSheetBehavior;
    private View mBottomBar;
    private View mSearchBar;
    private boolean mBottomFragmentLoaded;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.settings_homepage,
                container, false);
        mCardsContainer = (RecyclerView) rootView.findViewById(R.id.card_container);
        //TODO(b/111822407): May have to swap to GridLayoutManager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mCardsContainer.setLayoutManager(mLayoutManager);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupBottomBar();
        setupSearchBar();
        if (savedInstanceState != null) {
            final int bottombarState = savedInstanceState.getInt(SAVE_BOTTOMBAR_STATE);
            mBottomFragmentLoaded = savedInstanceState.getBoolean(SAVE_BOTTOM_FRAGMENT_LOADED);
            mBottomSheetBehavior.setState(bottombarState);
            setBarState(bottombarState);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mBottomSheetBehavior != null) {
            outState.putInt(SAVE_BOTTOMBAR_STATE, mBottomSheetBehavior.getState());
            outState.putBoolean(SAVE_BOTTOM_FRAGMENT_LOADED, mBottomFragmentLoaded);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SETTINGS_HOMEPAGE;
    }

    private void setupBottomBar() {
        final Activity activity = getActivity();

        mSearchButton = activity.findViewById(R.id.search_fab);
        mSearchButton.setOnClickListener(v -> {
            final Intent intent = SearchFeatureProvider.SEARCH_UI_INTENT;
            intent.setPackage(FeatureFactory.getFactory(activity)
                    .getSearchFeatureProvider().getSettingsIntelligencePkgName());
            startActivityForResult(intent, 0 /* requestCode */);
        });
        mBottomSheetBehavior = BottomSheetBehavior.from(activity.findViewById(R.id.bottom_sheet));
        mSearchBar = activity.findViewById(R.id.search_bar_container);
        mBottomBar = activity.findViewById(R.id.bar);
        mBottomBar.setOnClickListener(v -> {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        final int screenWidthpx = getResources().getDisplayMetrics().widthPixels;
        final Toolbar searchActionBar = activity.findViewById(R.id.search_action_bar);
        searchActionBar.setNavigationIcon(R.drawable.ic_search_floating_24dp);

        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (!mBottomFragmentLoaded) {
                    // TODO(b/110405144): Switch to {@link TopLevelSettings} when it's ready.
                    SettingsHomepageActivity.switchToFragment(getActivity(),
                            R.id.bottom_sheet_fragment, DashboardSummary.class.getName());
                    mBottomFragmentLoaded = true;
                }
                setBarState(newState);
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                mBottomBar.setAlpha(1 - slideOffset);
                mSearchButton.setAlpha(1 - slideOffset);
                mSearchBar.setAlpha(slideOffset);
                mSearchBar.setPadding((int) (screenWidthpx * (1 - slideOffset)), 0, 0, 0);
            }
        });
    }

    private void setBarState(int bottomSheetState) {
        if (bottomSheetState == BottomSheetBehavior.STATE_EXPANDED) {
            mBottomBar.setVisibility(View.INVISIBLE);
            mSearchBar.setVisibility(View.VISIBLE);
            mSearchButton.setVisibility(View.GONE);
        } else if (bottomSheetState == BottomSheetBehavior.STATE_COLLAPSED) {
            mBottomBar.setVisibility(View.VISIBLE);
            mSearchBar.setVisibility(View.INVISIBLE);
            mSearchButton.setVisibility(View.VISIBLE);
        } else if (bottomSheetState == BottomSheetBehavior.STATE_SETTLING) {
            mBottomBar.setVisibility(View.VISIBLE);
            mSearchBar.setVisibility(View.VISIBLE);
            mSearchButton.setVisibility(View.VISIBLE);
        }
    }

    //TODO(110767984), copied from settingsActivity. We have to merge them
    private void setupSearchBar() {
        final Activity activity = getActivity();
        final Toolbar toolbar = activity.findViewById(R.id.search_action_bar);
        FeatureFactory.getFactory(activity).getSearchFeatureProvider()
                .initSearchToolbar(activity, toolbar);
        activity.setActionBar(toolbar);

        // Please forgive me for what I am about to do.
        //
        // Need to make the navigation icon non-clickable so that the entire card is clickable
        // and goes to the search UI. Also set the background to null so there's no ripple.
        final View navView = toolbar.getNavigationView();
        navView.setClickable(false);
        navView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        navView.setBackground(null);

        final ActionBar actionBar = activity.getActionBar();
        if (actionBar != null) {
            boolean deviceProvisioned = Utils.isDeviceProvisioned(activity);
            actionBar.setDisplayHomeAsUpEnabled(deviceProvisioned);
            actionBar.setHomeButtonEnabled(deviceProvisioned);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }
}
