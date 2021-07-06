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

import android.content.Context;
import android.os.ParcelUuid;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.VisibleForTesting;

import com.android.settings.network.helper.SubscriptionAnnotation;
import com.android.settingslib.utils.ThreadUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This is a Callable class to query user selectable subscription list.
 */
public class SelectableSubscriptions implements Callable<List<SubscriptionAnnotation>> {
    private static final String TAG = "SelectableSubscriptions";

    private static final ParcelUuid mEmptyUuid = ParcelUuid.fromString("0-0-0-0-0");

    private Context mContext;
    private Supplier<List<SubscriptionInfo>> mSubscriptions;
    private Predicate<SubscriptionAnnotation> mFilter;

    /**
     * Constructor of class
     * @param context
     * @param disabledSlotsIncluded query both active and inactive slots when true,
     *                              only query active slot when false.
     */
    public SelectableSubscriptions(Context context, boolean disabledSlotsIncluded) {
        mContext = context;
        mSubscriptions = disabledSlotsIncluded ? (() -> getAvailableSubInfoList(context)) :
                (() -> getActiveSubInfoList(context));
        mFilter = disabledSlotsIncluded ? (subAnno -> subAnno.isExisted()) :
                (subAnno -> subAnno.isActive());
    }

    /**
     * Implementation of Callable
     * @return a list of SubscriptionAnnotation which is user selectable
     */
    public List<SubscriptionAnnotation> call() {
        TelephonyManager telMgr = mContext.getSystemService(TelephonyManager.class);

        try {
            // query in background thread
            Future<AtomicIntegerArray> eSimCardId =
                    ThreadUtils.postOnBackgroundThread(new QueryEsimCardId(telMgr));

            // query in background thread
            Future<AtomicIntegerArray> simSlotIndex =
                    ThreadUtils.postOnBackgroundThread(
                    new QuerySimSlotIndex(telMgr, true, true));

            // query in background thread
            Future<AtomicIntegerArray> activeSimSlotIndex =
                    ThreadUtils.postOnBackgroundThread(
                    new QuerySimSlotIndex(telMgr, false, true));

            List<SubscriptionInfo> subInfoList = mSubscriptions.get();

            // wait for result from background thread
            List<Integer> eSimCardIdList = atomicToList(eSimCardId.get());
            List<Integer> simSlotIndexList = atomicToList(simSlotIndex.get());
            List<Integer> activeSimSlotIndexList = atomicToList(activeSimSlotIndex.get());

            // group by GUID
            Map<ParcelUuid, List<SubscriptionAnnotation>> groupedSubInfoList =
                    IntStream.range(0, subInfoList.size())
                    .mapToObj(subInfoIndex ->
                            new SubscriptionAnnotation.Builder(subInfoList, subInfoIndex))
                    .map(annoBdr -> annoBdr.build(mContext,
                            eSimCardIdList, simSlotIndexList, activeSimSlotIndexList))
                    .filter(mFilter)
                    .collect(Collectors.groupingBy(subAnno -> getGroupUuid(subAnno)));

            // select best one from subscription(s) within the same group
            groupedSubInfoList.replaceAll((uuid, annoList) -> {
                if ((uuid == mEmptyUuid) || (annoList.size() <= 1)) {
                    return annoList;
                }
                return Collections.singletonList(selectBestFromList(annoList));
            });

            // build a list of subscriptions (based on the order of slot index)
            return groupedSubInfoList.values().stream().flatMap(List::stream)
                    .sorted(Comparator.comparingInt(anno -> anno.getSubInfo().getSimSlotIndex()))
                    .collect(Collectors.toList());
        } catch (Exception exception) {
            Log.w(TAG, "Fail to request subIdList", exception);
        }
        return Collections.emptyList();
    }

    protected ParcelUuid getGroupUuid(SubscriptionAnnotation subAnno) {
        ParcelUuid groupUuid = subAnno.getSubInfo().getGroupUuid();
        return (groupUuid == null) ? mEmptyUuid : groupUuid;
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
                // subscription ID in reverse order
                .thenComparingInt(anno -> -anno.getSubscriptionId());
        return annoList.stream().sorted(annoSelector).findFirst().orElse(null);
    }

    protected List<SubscriptionInfo> getSubInfoList(Context context,
            Function<SubscriptionManager, List<SubscriptionInfo>> convertor) {
        SubscriptionManager subManager = getSubscriptionManager(context);
        return (subManager == null) ? Collections.emptyList() : convertor.apply(subManager);
    }

    protected SubscriptionManager getSubscriptionManager(Context context) {
        return context.getSystemService(SubscriptionManager.class);
    }

    protected List<SubscriptionInfo> getAvailableSubInfoList(Context context) {
        return getSubInfoList(context, SubscriptionManager::getAvailableSubscriptionInfoList);
    }

    protected List<SubscriptionInfo> getActiveSubInfoList(Context context) {
        return getSubInfoList(context, SubscriptionManager::getActiveSubscriptionInfoList);
    }

    @Keep
    @VisibleForTesting
    protected static List<Integer> atomicToList(AtomicIntegerArray atomicIntArray) {
        if (atomicIntArray == null) {
            return Collections.emptyList();
        }
        return IntStream.range(0, atomicIntArray.length())
                .map(idx -> atomicIntArray.get(idx)).boxed()
                .collect(Collectors.toList());
    }
}