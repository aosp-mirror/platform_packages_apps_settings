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
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Keep;
import androidx.annotation.VisibleForTesting;

import com.android.settings.network.helper.SubscriptionAnnotation;
import com.android.settingslib.utils.ThreadUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This is a Callable class to query user selectable subscription list.
 *
 * Here's example of creating a Callable for retrieving a list of SubscriptionAnnotation
 * for active Subscriptions:
 *
 * List<SubscriptionAnnotation> result = (new SelectableSubscriptions(context, false)).call();
 *
 * Another example for retrieving a list of SubscriptionAnnotation for all subscriptions
 * accessible in another thread.
 *
 * List<SubscriptionAnnotation> result = ExecutorService.submit(
 *     new SelectableSubscriptions(context, true)).get()
 */
public class SelectableSubscriptions implements Callable<List<SubscriptionAnnotation>> {
    private static final String TAG = "SelectableSubscriptions";

    private Context mContext;
    private Supplier<List<SubscriptionInfo>> mSubscriptions;
    private Predicate<SubscriptionAnnotation> mFilter;
    private Function<List<SubscriptionAnnotation>, List<SubscriptionAnnotation>> mFinisher;

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
        if (disabledSlotsIncluded) {
            mFilter = subAnno -> {
                if (subAnno.isExisted()) {
                    return true;
                }
                return ((subAnno.getType() == SubscriptionAnnotation.TYPE_ESIM)
                        && (subAnno.isDisplayAllowed()));
            };
        } else {
            mFilter = subAnno -> subAnno.isActive();
        }
        mFinisher = annoList -> annoList;
    }

    /**
     * Add UnaryOperator to be applied to the final result.
     * @param finisher a function to be applied to the final result.
     */
    public SelectableSubscriptions addFinisher(
            UnaryOperator<List<SubscriptionAnnotation>> finisher) {
        mFinisher = mFinisher.andThen(finisher);
        return this;
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

            // build a list of SubscriptionAnnotation
            return IntStream.range(0, subInfoList.size())
                    .mapToObj(subInfoIndex ->
                            new SubscriptionAnnotation.Builder(subInfoList, subInfoIndex))
                    .map(annoBdr -> annoBdr.build(mContext,
                            eSimCardIdList, simSlotIndexList, activeSimSlotIndexList))
                    .filter(mFilter)
                    .collect(Collectors.collectingAndThen(Collectors.toList(), mFinisher));
        } catch (Exception exception) {
            Log.w(TAG, "Fail to request subIdList", exception);
        }
        return Collections.emptyList();
    }

    protected List<SubscriptionInfo> getSubInfoList(Context context,
            Function<SubscriptionManager, List<SubscriptionInfo>> convertor) {
        SubscriptionManager subManager = getSubscriptionManager(context);
        List<SubscriptionInfo> result = (subManager == null) ? null : convertor.apply(subManager);
        return (result == null) ? Collections.emptyList() : result;
    }

    protected SubscriptionManager getSubscriptionManager(Context context) {
        return context.getSystemService(SubscriptionManager.class).createForAllUserProfiles();
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