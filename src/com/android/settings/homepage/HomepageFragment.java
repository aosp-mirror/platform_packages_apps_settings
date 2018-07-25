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

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsHomepageActivity;
import com.android.settings.Utils;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.dashboard.DashboardSummary;
import com.android.settings.overlay.FeatureFactory;
import com.android.settings.search.SearchFeatureProvider;

import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.NonNull;

public class HomepageFragment extends InstrumentedFragment {

    private static final String TAG = "HomepageFragment";

    private FloatingActionButton mSearchButton;
    private BottomSheetBehavior mBottomSheetBehavior;
    private boolean mBottomFragmentLoaded = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.dashboard, container, false);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setupBottomBar();
        setupSearchBar();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SETTINGS_HOMEPAGE;
    }

    private void setupBottomBar() {
        final Activity activity = getActivity();
        mSearchButton = (FloatingActionButton) activity.findViewById(R.id.search_fab);

        mSearchButton.setOnClickListener(v -> {
            final Intent intent = SearchFeatureProvider.SEARCH_UI_INTENT;
            intent.setPackage(FeatureFactory.getFactory(activity)
                    .getSearchFeatureProvider().getSettingsIntelligencePkgName());
            startActivityForResult(intent, 0 /* requestCode */);
        });
        mBottomSheetBehavior = BottomSheetBehavior.from(activity.findViewById(R.id.bottom_sheet));
        final BottomAppBar bottomBar = (BottomAppBar) activity.findViewById(R.id.bar);
        bottomBar.setOnClickListener(v -> {
            mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        });

        final int screenWidthpx = getResources().getDisplayMetrics().widthPixels;
        final View searchbar = activity.findViewById(R.id.search_bar_container);
        final View bottombar = activity.findViewById(R.id.bar);
        final Toolbar searchActionBar = (Toolbar) activity.findViewById(R.id.search_action_bar);
        searchActionBar.setNavigationIcon(R.drawable.ic_search_floating_24dp);


        mBottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (!mBottomFragmentLoaded) {
                    SettingsHomepageActivity.switchToFragment(getActivity(),
                            R.id.bottom_sheet_fragment, DashboardSummary.class.getName());
                    mBottomFragmentLoaded = true;
                }
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    bottombar.setVisibility(View.INVISIBLE);
                    searchbar.setVisibility(View.VISIBLE);
                    mSearchButton.setVisibility(View.GONE);
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottombar.setVisibility(View.VISIBLE);
                    searchbar.setVisibility(View.INVISIBLE);
                    mSearchButton.setVisibility(View.VISIBLE);
                } else if (newState == BottomSheetBehavior.STATE_SETTLING) {
                    bottombar.setVisibility(View.VISIBLE);
                    searchbar.setVisibility(View.VISIBLE);
                    mSearchButton.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                bottombar.setAlpha(1 - slideOffset);
                mSearchButton.setAlpha(1 - slideOffset);
                searchbar.setAlpha(slideOffset);
                searchbar.setPadding((int) (screenWidthpx * (1 - slideOffset)), 0, 0, 0);
            }
        });
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
