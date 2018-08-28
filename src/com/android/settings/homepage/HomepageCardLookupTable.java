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

import com.android.settings.homepage.HomepageCard.CardType;

import java.util.Set;
import java.util.TreeSet;

public class HomepageCardLookupTable {

    static class HomepageMapping implements Comparable<HomepageMapping> {
        @CardType
        private final int mCardType;
        private final Class<? extends HomepageCardController> mControllerClass;
        private final Class<? extends HomepageCardRenderer> mRendererClass;

        private HomepageMapping(@CardType int cardType,
                Class<? extends HomepageCardController> controllerClass,
                Class<? extends HomepageCardRenderer> rendererClass) {
            mCardType = cardType;
            mControllerClass = controllerClass;
            mRendererClass = rendererClass;
        }

        @Override
        public int compareTo(HomepageMapping other) {
            return Integer.compare(this.mCardType, other.mCardType);
        }
    }

    private static final Set<HomepageMapping> LOOKUP_TABLE = new TreeSet<HomepageMapping>() {
            //add(new HomepageMapping(CardType.CONDITIONAL, ConditionHomepageCardController.class,
                   // ConditionHomepageCardRenderer.class));
    };

    public static Class<? extends HomepageCardController> getCardControllerClass(
            @CardType int cardType) {
        for (HomepageMapping mapping : LOOKUP_TABLE) {
            if (mapping.mCardType == cardType) {
                return mapping.mControllerClass;
            }
        }
        return null;
    }

    //TODO(b/112578070): Implement multi renderer cases.
    public static Class<? extends HomepageCardRenderer> getCardRendererClasses(
            @CardType int cardType) {
        for (HomepageMapping mapping : LOOKUP_TABLE) {
            if (mapping.mCardType == cardType) {
                return mapping.mRendererClass;
            }
        }
        return null;
    }
}
