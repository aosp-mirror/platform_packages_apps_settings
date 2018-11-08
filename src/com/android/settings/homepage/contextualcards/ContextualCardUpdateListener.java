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

import androidx.annotation.MainThread;

import java.util.List;
import java.util.Map;

/**
 * When {@link ContextualCardController} detects changes, it will notify the listeners registered.
 */
public interface ContextualCardUpdateListener {

    /**
     * Called when a set of cards are updated.
     *
     * @param cards A map of updates grouped by {@link ContextualCard.CardType}. Values can be
     *              null, which means all cards from corresponding {@link
     *              ContextualCard.CardType} are removed.
     */
    @MainThread
    void onContextualCardUpdated(Map<Integer, List<ContextualCard>> cards);
}