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

import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settings.homepage.contextualcards.ContextualCard.CardType;
import com.android.settings.homepage.contextualcards.conditional.ConditionContextualCardController;
import com.android.settings.homepage.contextualcards.conditional.ConditionContextualCardRenderer;
import com.android.settings.homepage.contextualcards.slices.SliceContextualCardController;
import com.android.settings.homepage.contextualcards.slices.SliceContextualCardRenderer;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class ContextualCardLookupTable {
    private static final String TAG = "ContextualCardLookup";
    static class ControllerRendererMapping implements Comparable<ControllerRendererMapping> {
        @CardType
        final int mCardType;
        final int mViewType;
        final Class<? extends ContextualCardController> mControllerClass;
        final Class<? extends ContextualCardRenderer> mRendererClass;

        ControllerRendererMapping(@CardType int cardType, int viewType,
                Class<? extends ContextualCardController> controllerClass,
                Class<? extends ContextualCardRenderer> rendererClass) {
            mCardType = cardType;
            mViewType = viewType;
            mControllerClass = controllerClass;
            mRendererClass = rendererClass;
        }

        @Override
        public int compareTo(ControllerRendererMapping other) {
            return Comparator.comparingInt((ControllerRendererMapping mapping) -> mapping.mCardType)
                    .thenComparingInt(mapping -> mapping.mViewType)
                    .compare(this, other);
        }
    }

    @VisibleForTesting
    static final Set<ControllerRendererMapping> LOOKUP_TABLE =
            new TreeSet<ControllerRendererMapping>() {{
                add(new ControllerRendererMapping(CardType.CONDITIONAL,
                        ConditionContextualCardRenderer.HALF_WIDTH_VIEW_TYPE,
                        ConditionContextualCardController.class,
                        ConditionContextualCardRenderer.class));
                add(new ControllerRendererMapping(CardType.CONDITIONAL,
                        ConditionContextualCardRenderer.FULL_WIDTH_VIEW_TYPE,
                        ConditionContextualCardController.class,
                        ConditionContextualCardRenderer.class));
                add(new ControllerRendererMapping(CardType.SLICE,
                        SliceContextualCardRenderer.VIEW_TYPE,
                        SliceContextualCardController.class,
                        SliceContextualCardRenderer.class));
            }};

    public static Class<? extends ContextualCardController> getCardControllerClass(
            @CardType int cardType) {
        for (ControllerRendererMapping mapping : LOOKUP_TABLE) {
            if (mapping.mCardType == cardType) {
                return mapping.mControllerClass;
            }
        }
        return null;
    }

    public static Class<? extends ContextualCardRenderer> getCardRendererClassByCardType(
            @CardType int cardType) {
        return LOOKUP_TABLE.stream()
                .filter(m -> m.mCardType == cardType)
                .findFirst()
                .map(mapping -> mapping.mRendererClass)
                .orElse(null);
    }

    public static Class<? extends ContextualCardRenderer> getCardRendererClassByViewType(
            int viewType) throws IllegalStateException {
        List<ControllerRendererMapping> validMappings = LOOKUP_TABLE.stream()
                .filter(m -> m.mViewType == viewType).collect(Collectors.toList());
        if (validMappings == null || validMappings.isEmpty()) {
            Log.w(TAG, "No matching mapping");
            return null;
        }
        if (validMappings.size() != 1) {
            throw new IllegalStateException("Have duplicate VIEW_TYPE in lookup table.");
        }

        return validMappings.get(0).mRendererClass;
    }
}
