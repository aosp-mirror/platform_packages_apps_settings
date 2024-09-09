/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.accessibility;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroupAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerViewAccessibilityDelegate;

import com.android.settingslib.widget.IllustrationPreference;

/** Utilities for {@code Settings > Accessibility} fragments. */
public class AccessibilityFragmentUtils {
    // TODO: b/350782252 - Replace with an official library-provided solution when available.
    /**
     * Modifies the existing {@link RecyclerViewAccessibilityDelegate} of the provided
     * {@link RecyclerView} for this fragment to report the number of visible and important
     * items on this page via the RecyclerView's {@link AccessibilityNodeInfo}.
     *
     * <p><strong>Note:</strong> This is special-cased to the structure of these fragments:
     * one column, N rows (one per preference, including category titles and header+footer
     * preferences), <=N 'important' rows (image prefs without content descriptions). This
     * is not intended for use with generic {@link RecyclerView}s.
     */
    public static RecyclerView addCollectionInfoToAccessibilityDelegate(RecyclerView recyclerView) {
        if (!Flags.toggleFeatureFragmentCollectionInfo()) {
            return recyclerView;
        }
        final RecyclerViewAccessibilityDelegate delegate =
                recyclerView.getCompatAccessibilityDelegate();
        if (delegate == null) {
            // No delegate, so do nothing. This should not occur for real RecyclerViews.
            return recyclerView;
        }
        recyclerView.setAccessibilityDelegateCompat(
                new RvAccessibilityDelegateWrapper(recyclerView, delegate) {
                    @Override
                    public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                            @NonNull AccessibilityNodeInfoCompat info) {
                        super.onInitializeAccessibilityNodeInfo(host, info);
                        if (!(recyclerView.getAdapter()
                                instanceof final PreferenceGroupAdapter preferenceGroupAdapter)) {
                            return;
                        }
                        final int visibleCount = preferenceGroupAdapter.getItemCount();
                        int importantCount = 0;
                        for (int i = 0; i < visibleCount; i++) {
                            if (isPreferenceImportantToA11y(preferenceGroupAdapter.getItem(i))) {
                                importantCount++;
                            }
                        }
                        info.unwrap().setCollectionInfo(
                                new AccessibilityNodeInfo.CollectionInfo(
                                        /*rowCount=*/visibleCount,
                                        /*columnCount=*/1,
                                        /*hierarchical=*/false,
                                        AccessibilityNodeInfo.CollectionInfo.SELECTION_MODE_SINGLE,
                                        /*itemCount=*/visibleCount,
                                        /*importantForAccessibilityItemCount=*/importantCount));
                    }
                });
        return recyclerView;
    }

    /**
     * Returns whether the preference will be marked as important to accessibility for the sake
     * of calculating {@link AccessibilityNodeInfo.CollectionInfo} counts.
     *
     * <p>The accessibility service itself knows this information for an individual preference
     * on the screen, but it expects the preference's {@link RecyclerView} to also provide the
     * same information for its entire set of adapter items.
     */
    @VisibleForTesting
    static boolean isPreferenceImportantToA11y(Preference pref) {
        if ((pref instanceof IllustrationPreference illustrationPref
                && TextUtils.isEmpty(illustrationPref.getContentDescription()))
                || pref instanceof PaletteListPreference) {
            // Illustration preference that is visible but unannounced by accessibility services.
            return false;
        }
        // All other preferences from the PreferenceGroupAdapter are important.
        return true;
    }

    /**
     * Wrapper around a {@link RecyclerViewAccessibilityDelegate} that allows customizing
     * a subset of methods and while also deferring to the original. All overridden methods
     * in instantiations of this class should call {@code super}.
     */
    private static class RvAccessibilityDelegateWrapper extends RecyclerViewAccessibilityDelegate {
        private final RecyclerViewAccessibilityDelegate mOriginal;

        RvAccessibilityDelegateWrapper(RecyclerView recyclerView,
                RecyclerViewAccessibilityDelegate original) {
            super(recyclerView);
            mOriginal = original;
        }

        @Override
        public boolean performAccessibilityAction(@NonNull View host, int action, Bundle args) {
            return mOriginal.performAccessibilityAction(host, action, args);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(@NonNull View host,
                @NonNull AccessibilityNodeInfoCompat info) {
            mOriginal.onInitializeAccessibilityNodeInfo(host, info);
        }

        @Override
        public void onInitializeAccessibilityEvent(@NonNull View host,
                @NonNull AccessibilityEvent event) {
            mOriginal.onInitializeAccessibilityEvent(host, event);
        }

        @Override
        @NonNull
        public AccessibilityDelegateCompat getItemDelegate() {
            if (mOriginal == null) {
                // Needed for super constructor which calls getItemDelegate before mOriginal is
                // defined, but unused by actual clients of this RecyclerViewAccessibilityDelegate
                // which invoke getItemDelegate() after the constructor finishes.
                return new ItemDelegate(this);
            }
            return mOriginal.getItemDelegate();
        }
    }
}
