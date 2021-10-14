/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.settings.network.helper;

import android.telephony.TelephonyManager;
import android.telephony.UiccCardInfo;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * This is a Callable class which queries valid card ID for eSIM
 */
public class QueryEsimCardId implements Callable<AtomicIntegerArray> {
    private static final String TAG = "QueryEsimCardId";

    private TelephonyManager mTelephonyManager;

    /**
     * Constructor of class
     * @param TelephonyManager
     */
    public QueryEsimCardId(TelephonyManager telephonyManager) {
        mTelephonyManager = telephonyManager;
    }

    /**
     * Implementation of Callable
     * @return card ID(s) in AtomicIntegerArray
     */
    public AtomicIntegerArray call() {
        List<UiccCardInfo> cardInfos = mTelephonyManager.getUiccCardsInfo();
        if (cardInfos == null) {
            return new AtomicIntegerArray(0);
        }
        return new AtomicIntegerArray(cardInfos.stream()
                .filter(Objects::nonNull)
                .filter(cardInfo -> (!cardInfo.isRemovable() && (cardInfo.getCardId() >= 0)))
                .mapToInt(UiccCardInfo::getCardId)
                .toArray());
    }
}