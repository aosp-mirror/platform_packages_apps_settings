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

import android.os.ParcelUuid;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.VisibleForTesting;

import com.android.settings.network.helper.SubscriptionAnnotation;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * A UnaryOperator for converting a list of SubscriptionAnnotation into
 * another list of SubscriptionAnnotation based on group UUID.
 * Only one SubscriptionAnnotation with entries with same (valid) group UUID would be kept.
 *
 * Here's an example when applying this operation as a finisher of SelectableSubscriptions:
 *
 * Callable<SubscriptionAnnotation> callable = (new SelectableSubscriptions(context, true))
 *         .addFinisher(new SubscriptionGrouping());
 *
 * List<SubscriptionAnnotation> result = ExecutorService.submit(callable).get()
 */
public class SubscriptionGrouping
        implements UnaryOperator<List<SubscriptionAnnotation>> {
    private static final String LOG_TAG = "SubscriptionGrouping";

    // implementation of UnaryOperator
    public List<SubscriptionAnnotation> apply(List<SubscriptionAnnotation> listOfSubscriptions) {
        Log.d(LOG_TAG, "Grouping " + listOfSubscriptions);

        // group by GUID
        Map<ParcelUuid, List<SubscriptionAnnotation>> groupedSubInfoList =
                listOfSubscriptions.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(subAnno -> getGroupUuid(subAnno)));

        // select best one from subscription(s) within the same group
        groupedSubInfoList.replaceAll((uuid, annoList) -> {
            if ((uuid == SubscriptionAnnotation.EMPTY_UUID) || (annoList.size() <= 1)) {
                return annoList;
            }
            return Collections.singletonList(selectBestFromList(annoList));
        });

        // build a stream of subscriptions
        return groupedSubInfoList.values()
                .stream().flatMap(List::stream).collect(Collectors.toList());
    }

    @Keep
    @VisibleForTesting
    protected ParcelUuid getGroupUuid(SubscriptionAnnotation subAnno) {
        ParcelUuid groupUuid = subAnno.getGroupUuid();
        return (groupUuid == null) ? SubscriptionAnnotation.EMPTY_UUID : groupUuid;
    }

    protected SubscriptionAnnotation selectBestFromList(List<SubscriptionAnnotation> annoList) {
        Comparator<SubscriptionAnnotation> annoSelector = (anno1, anno2) -> {
            if (anno1.isDisplayAllowed() != anno2.isDisplayAllowed()) {
                return anno1.isDisplayAllowed() ? -1 : 1;
            }
            if (anno1.isActive() != anno2.isActive()) {
                return anno1.isActive() ? -1 : 1;
            }
            if (anno1.isExisted() != anno2.isExisted()) {
                return anno1.isExisted() ? -1 : 1;
            }
            return 0;
        };
        annoSelector = annoSelector
                // eSIM in front of pSIM
                .thenComparingInt(anno -> -anno.getType())
                // maintain the ordering given within constructor
                .thenComparingInt(anno -> annoList.indexOf(anno));
        return annoList.stream().sorted(annoSelector).findFirst().orElse(null);
    }
}
