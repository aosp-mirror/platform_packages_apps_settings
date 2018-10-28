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

import com.android.settings.homepage.contextualcards.ContextualCard.CardType;
import com.android.settings.homepage.contextualcards.conditional.ConditionContextualCardController;
import com.android.settings.homepage.contextualcards.conditional.ConditionContextualCardRenderer;
import com.android.settings.homepage.contextualcards.slices.SliceContextualCardController;
import com.android.settings.homepage.contextualcards.slices.SliceContextualCardRenderer;

import java.util.Set;
import java.util.TreeSet;

public class ContextualCardLookupTable {

    static class ControllerRendererMapping implements Comparable<ControllerRendererMapping> {
        @CardType
        private final int mCardType;
        private final Class<? extends ContextualCardController> mControllerClass;
        private final Class<? extends ContextualCardRenderer> mRendererClass;

        private ControllerRendererMapping(@CardType int cardType,
                Class<? extends ContextualCardController> controllerClass,
                Class<? extends ContextualCardRenderer> rendererClass) {
            mCardType = cardType;
            mControllerClass = controllerClass;
            mRendererClass = rendererClass;
        }

        @Override
        public int compareTo(ControllerRendererMapping other) {
            return Integer.compare(this.mCardType, other.mCardType);
        }
    }

    private static final Set<ControllerRendererMapping> LOOKUP_TABLE =
            new TreeSet<ControllerRendererMapping>() {{
                add(new ControllerRendererMapping(CardType.CONDITIONAL,
                        ConditionContextualCardController.class,
                        ConditionContextualCardRenderer.class));
                add(new ControllerRendererMapping(CardType.SLICE,
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

    //TODO(b/112578070): Implement multi renderer cases.
    public static Class<? extends ContextualCardRenderer> getCardRendererClasses(
            @CardType int cardType) {
        for (ControllerRendererMapping mapping : LOOKUP_TABLE) {
            if (mapping.mCardType == cardType) {
                return mapping.mRendererClass;
            }
        }
        return null;
    }
}
