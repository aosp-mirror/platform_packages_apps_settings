/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.network;

import static android.telephony.SubscriptionManager.PROFILE_CLASS_PROVISIONING;
import static android.telephony.UiccSlotInfo.CARD_STATE_INFO_PRESENT;

import android.annotation.NonNull;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.telephony.UiccCardInfo;
import android.telephony.UiccPortInfo;
import android.telephony.UiccSlotInfo;
import android.util.ArrayMap;
import android.util.IndentingPrintWriter;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.lifecycle.LifecycleOwner;

import com.android.internal.telephony.flags.Flags;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.mobile.dataservice.MobileNetworkDatabase;
import com.android.settingslib.mobile.dataservice.MobileNetworkInfoDao;
import com.android.settingslib.mobile.dataservice.MobileNetworkInfoEntity;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoDao;
import com.android.settingslib.mobile.dataservice.SubscriptionInfoEntity;
import com.android.settingslib.mobile.dataservice.UiccInfoDao;
import com.android.settingslib.mobile.dataservice.UiccInfoEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MobileNetworkRepository extends SubscriptionManager.OnSubscriptionsChangedListener {

    private static final String TAG = "MobileNetworkRepository";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static ExecutorService sExecutor = Executors.newSingleThreadExecutor();
    private static Map<Integer, SubscriptionInfoEntity> sCacheSubscriptionInfoEntityMap =
            new ArrayMap<>();
    private static Map<Integer, MobileNetworkInfoEntity> sCacheMobileNetworkInfoEntityMap =
            new ArrayMap<>();
    private static Map<Integer, UiccInfoEntity> sCacheUiccInfoEntityMap = new ArrayMap<>();
    private static Collection<MobileNetworkCallback> sCallbacks = new CopyOnWriteArrayList<>();
    private static final Object sInstanceLock = new Object();
    @GuardedBy("sInstanceLock")
    private static MobileNetworkRepository sInstance;

    private SubscriptionManager mSubscriptionManager;
    private MobileNetworkDatabase mMobileNetworkDatabase;
    private SubscriptionInfoDao mSubscriptionInfoDao;
    private UiccInfoDao mUiccInfoDao;
    private MobileNetworkInfoDao mMobileNetworkInfoDao;
    private List<SubscriptionInfoEntity> mAvailableSubInfoEntityList = new ArrayList<>();
    private List<SubscriptionInfoEntity> mActiveSubInfoEntityList = new ArrayList<>();
    private List<UiccInfoEntity> mUiccInfoEntityList = new ArrayList<>();
    private List<MobileNetworkInfoEntity> mMobileNetworkInfoEntityList = new ArrayList<>();
    private Context mContext;
    private AirplaneModeObserver mAirplaneModeObserver;
    private DataRoamingObserver mDataRoamingObserver;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private Map<Integer, MobileDataContentObserver> mDataContentObserverMap = new HashMap<>();
    private int mPhysicalSlotIndex = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
    private int mLogicalSlotIndex = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
    private int mCardState = UiccSlotInfo.CARD_STATE_INFO_ABSENT;
    private int mPortIndex = TelephonyManager.INVALID_PORT_INDEX;
    private int mCardId = TelephonyManager.UNINITIALIZED_CARD_ID;
    private boolean mIsEuicc = false;
    private boolean mIsRemovable = false;
    private boolean mIsActive = false;
    private Map<Integer, SubscriptionInfo> mSubscriptionInfoMap = new ArrayMap<>();
    private Map<Integer, TelephonyManager> mTelephonyManagerMap = new HashMap<>();
    private Map<Integer, PhoneCallStateTelephonyCallback> mTelephonyCallbackMap = new HashMap<>();

    @NonNull
    public static MobileNetworkRepository getInstance(Context context) {
        synchronized (sInstanceLock) {
            if (sInstance != null) {
                return sInstance;
            }
            if (DEBUG) {
                Log.d(TAG, "Init the instance.");
            }
            sInstance = new MobileNetworkRepository(context);
            return sInstance;
        }
    }

    private MobileNetworkRepository(Context context) {
        mContext = context;
        mMobileNetworkDatabase = MobileNetworkDatabase.getInstance(context);
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_MOBILE_NETWORK_DB_CREATED);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mSubscriptionInfoDao = mMobileNetworkDatabase.mSubscriptionInfoDao();
        mUiccInfoDao = mMobileNetworkDatabase.mUiccInfoDao();
        mMobileNetworkInfoDao = mMobileNetworkDatabase.mMobileNetworkInfoDao();
        mAirplaneModeObserver = new AirplaneModeObserver(new Handler(Looper.getMainLooper()));
        mDataRoamingObserver = new DataRoamingObserver(new Handler(Looper.getMainLooper()));
    }

    private class AirplaneModeObserver extends ContentObserver {
        private Uri mAirplaneModeSettingUri =
                Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON);

        AirplaneModeObserver(Handler handler) {
            super(handler);
        }

        public void register(Context context) {
            context.getContentResolver().registerContentObserver(mAirplaneModeSettingUri, false,
                    this);
        }

        public void unRegister(Context context) {
            context.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(mAirplaneModeSettingUri)) {
                boolean isAirplaneModeOn = isAirplaneModeOn();
                for (MobileNetworkCallback callback : sCallbacks) {
                    callback.onAirplaneModeChanged(isAirplaneModeOn);
                }
            }
        }
    }

    private class DataRoamingObserver extends ContentObserver {
        private int mRegSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        private String mBaseField = Settings.Global.DATA_ROAMING;

        DataRoamingObserver(Handler handler) {
            super(handler);
        }

        public void register(Context context, int subId) {
            mRegSubId = subId;
            String lastField = mBaseField;
            createTelephonyManagerBySubId(subId);
            TelephonyManager tm = mTelephonyManagerMap.get(subId);
            if (tm.getSimCount() != 1) {
                lastField += subId;
            }
            context.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(lastField), false, this);
        }

        public void unRegister(Context context) {
            context.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            TelephonyManager tm = mTelephonyManagerMap.get(mRegSubId);
            if (tm == null) {
                return;
            }
            sExecutor.execute(() -> {
                insertMobileNetworkInfo(mContext, mRegSubId, tm);
            });
            boolean isDataRoamingEnabled = tm.isDataRoamingEnabled();
            for (MobileNetworkCallback callback : sCallbacks) {
                callback.onDataRoamingChanged(mRegSubId, isDataRoamingEnabled);
            }
        }
    }

    /**
     * Register all callbacks and listener.
     *
     * @param lifecycleOwner The lifecycle owner.
     * @param mobileNetworkCallback A callback to receive all MobileNetwork's changes.
     * @param subId The subscription ID to register relevant changes and listener for specific
     *              subscription.
     */
    public void addRegister(LifecycleOwner lifecycleOwner,
            MobileNetworkCallback mobileNetworkCallback, int subId) {
        if (sCallbacks.isEmpty()) {
            mSubscriptionManager.addOnSubscriptionsChangedListener(mContext.getMainExecutor(),
                    this);
            mAirplaneModeObserver.register(mContext);
            if (DEBUG) {
                Log.d(TAG, "addRegister done");
            }
        }
        sCallbacks.add(mobileNetworkCallback);
        observeAllSubInfo(lifecycleOwner);
        observeAllUiccInfo(lifecycleOwner);
        observeAllMobileNetworkInfo(lifecycleOwner);
        if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            addRegisterBySubId(subId);
            createTelephonyManagerBySubId(subId);
            mDataRoamingObserver.register(mContext, subId);
        }
        // When one client registers callback first time, convey the cached results to the client
        // so that the client is aware of the content therein.
        sendAvailableSubInfoCache(mobileNetworkCallback);
    }

    public void addRegisterBySubId(int subId) {
        MobileDataContentObserver dataContentObserver = new MobileDataContentObserver(
                new Handler(Looper.getMainLooper()));
        dataContentObserver.setOnMobileDataChangedListener(() -> {
            sExecutor.execute(() -> {
                insertMobileNetworkInfo(mContext, subId,
                        getTelephonyManagerBySubId(mContext, subId));
            });
        });
        dataContentObserver.register(mContext, subId);
        mDataContentObserverMap.put(subId, dataContentObserver);
    }

    private void createTelephonyManagerBySubId(int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            return;
        }
        PhoneCallStateTelephonyCallback
                telephonyCallback = new PhoneCallStateTelephonyCallback();
        TelephonyManager telephonyManager = mContext.getSystemService(
                TelephonyManager.class).createForSubscriptionId(subId);
        telephonyManager.registerTelephonyCallback(mContext.getMainExecutor(),
                telephonyCallback);
        mTelephonyCallbackMap.put(subId, telephonyCallback);
        mTelephonyManagerMap.put(subId, telephonyManager);
    }

    private TelephonyManager getTelephonyManagerBySubId(Context context, int subId) {
        TelephonyManager telephonyManager = mTelephonyManagerMap.get(subId);
        if (telephonyManager != null) {
            return telephonyManager;
        }

        if (context != null) {
            telephonyManager = context.getSystemService(TelephonyManager.class);
            if (telephonyManager != null) {
                telephonyManager = telephonyManager.createForSubscriptionId(subId);
            } else if (DEBUG) {
                Log.d(TAG, "Can not get TelephonyManager for subId " + subId);
            }
        }

        return telephonyManager;

    }

    private void removerRegisterBySubId(int subId) {
        if (mTelephonyCallbackMap.containsKey(subId)) {
            TelephonyManager telephonyManager = getTelephonyManagerBySubId(mContext, subId);
            if (telephonyManager != null) {
                PhoneCallStateTelephonyCallback callback = mTelephonyCallbackMap.get(subId);
                if (callback != null) {
                    telephonyManager.unregisterTelephonyCallback(callback);
                    mTelephonyCallbackMap.remove(subId);
                }
            }
        }
        if (mDataContentObserverMap.containsKey(subId)) {
            mDataContentObserverMap.get(subId).unRegister(mContext);
            mDataContentObserverMap.remove(subId);
        }
    }

    public void removeRegister(MobileNetworkCallback mobileNetworkCallback) {
        sCallbacks.remove(mobileNetworkCallback);
        if (sCallbacks.isEmpty()) {
            mSubscriptionManager.removeOnSubscriptionsChangedListener(this);
            mAirplaneModeObserver.unRegister(mContext);
            mDataRoamingObserver.unRegister(mContext);
            mDataContentObserverMap.forEach((id, observer) -> {
                observer.unRegister(mContext);
            });
            mDataContentObserverMap.clear();

            mTelephonyManagerMap.forEach((id, manager) -> {
                TelephonyCallback callback = mTelephonyCallbackMap.get(id);
                if (callback != null) {
                    manager.unregisterTelephonyCallback(callback);
                }
            });
            mTelephonyCallbackMap.clear();
            mTelephonyManagerMap.clear();
            if (DEBUG) {
                Log.d(TAG, "removeRegister done");
            }
        }
    }

    public void updateEntity() {
        // Check the latest state after back to the UI.
        if (sCacheSubscriptionInfoEntityMap != null || !sCacheSubscriptionInfoEntityMap.isEmpty()) {
            sExecutor.execute(() -> {
                onSubscriptionsChanged();
            });
        }

        boolean isAirplaneModeOn = isAirplaneModeOn();
        for (MobileNetworkCallback callback : sCallbacks) {
            callback.onAirplaneModeChanged(isAirplaneModeOn);
        }
    }

    private void observeAllSubInfo(LifecycleOwner lifecycleOwner) {
        if (DEBUG) {
            Log.d(TAG, "Observe subInfo.");
        }
        mMobileNetworkDatabase.queryAvailableSubInfos().observe(
                lifecycleOwner, this::onAvailableSubInfoChanged);
    }

    private void observeAllUiccInfo(LifecycleOwner lifecycleOwner) {
        if (DEBUG) {
            Log.d(TAG, "Observe UICC info.");
        }
        mMobileNetworkDatabase.queryAllUiccInfo().observe(
                lifecycleOwner, this::onAllUiccInfoChanged);
    }

    private void observeAllMobileNetworkInfo(LifecycleOwner lifecycleOwner) {
        if (DEBUG) {
            Log.d(TAG, "Observe mobile network info.");
        }
        mMobileNetworkDatabase.queryAllMobileNetworkInfo().observe(
                lifecycleOwner, this::onAllMobileNetworkInfoChanged);
    }

    public List<SubscriptionInfoEntity> getAvailableSubInfoEntityList() {
        return mAvailableSubInfoEntityList;
    }

    public List<SubscriptionInfoEntity> getActiveSubscriptionInfoList() {
        return mActiveSubInfoEntityList;
    }

    public List<UiccInfoEntity> getUiccInfoEntityList() {
        return mUiccInfoEntityList;
    }

    public List<MobileNetworkInfoEntity> getMobileNetworkInfoEntityList() {
        return mMobileNetworkInfoEntityList;
    }

    public SubscriptionInfoEntity getSubInfoById(String subId) {
        return mSubscriptionInfoDao.querySubInfoById(subId);
    }

    public MobileNetworkInfoEntity queryMobileNetworkInfoBySubId(String subId) {
        return mMobileNetworkInfoDao.queryMobileNetworkInfoBySubId(subId);
    }

    private void getUiccInfoBySubscriptionInfo(@NonNull UiccSlotInfo[] uiccSlotInfos,
            SubscriptionInfo subInfo) {
        for (int i = 0; i < uiccSlotInfos.length; i++) {
            UiccSlotInfo curSlotInfo = uiccSlotInfos[i];
            if (curSlotInfo != null && curSlotInfo.getCardStateInfo() == CARD_STATE_INFO_PRESENT) {
                final int index = i;
                mIsEuicc = curSlotInfo.getIsEuicc();
                mCardState = curSlotInfo.getCardStateInfo();
                mIsRemovable = curSlotInfo.isRemovable();
                mCardId = subInfo.getCardId();

                Collection<UiccPortInfo> uiccPortInfos = curSlotInfo.getPorts();
                uiccPortInfos.forEach(portInfo -> {
                    if (portInfo.getPortIndex() == subInfo.getPortIndex()
                            && portInfo.getLogicalSlotIndex() == subInfo.getSimSlotIndex()) {
                        mPhysicalSlotIndex = index;
                        mLogicalSlotIndex = portInfo.getLogicalSlotIndex();
                        mIsActive = portInfo.isActive();
                        mPortIndex = portInfo.getPortIndex();
                    } else if (DEBUG) {
                        Log.d(TAG, "Can not get port index and physicalSlotIndex for subId "
                                + subInfo.getSubscriptionId());
                    }
                });
                if (mPhysicalSlotIndex != SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
                    break;
                }
            } else if (DEBUG) {
                Log.d(TAG, "Can not get card state info");
            }
        }
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_MOBILE_NETWORK_DB_GET_UICC_INFO,
                subInfo.getSubscriptionId());
    }

    private void onAvailableSubInfoChanged(
            List<SubscriptionInfoEntity> availableSubInfoEntityList) {
        synchronized (this) {
            if (mAvailableSubInfoEntityList != null
                    && mAvailableSubInfoEntityList.size() == availableSubInfoEntityList.size()
                    && mAvailableSubInfoEntityList.containsAll(availableSubInfoEntityList)) {
                Log.d(TAG, "onAvailableSubInfoChanged, duplicates = " + availableSubInfoEntityList);
                return;
            }
            mAvailableSubInfoEntityList = new ArrayList<>(availableSubInfoEntityList);
        }
        if (DEBUG) {
            Log.d(TAG, "onAvailableSubInfoChanged, availableSubInfoEntityList = "
                    + availableSubInfoEntityList);
        }
        for (MobileNetworkCallback callback : sCallbacks) {
            callback.onAvailableSubInfoChanged(availableSubInfoEntityList);
        }
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_MOBILE_NETWORK_DB_NOTIFY_SUB_INFO_IS_CHANGED, 0);
        onActiveSubInfoListChanged(availableSubInfoEntityList);
    }

    private void onActiveSubInfoListChanged(
            List<SubscriptionInfoEntity> availableSubInfoEntityList) {
        List<SubscriptionInfoEntity> activeSubInfoEntityList =
                availableSubInfoEntityList.stream()
                .filter(SubscriptionInfoEntity::isActiveSubscription)
                .filter(SubscriptionInfoEntity::isSubscriptionVisible)
                .collect(Collectors.toList());
        if (DEBUG) {
            Log.d(TAG, "onActiveSubInfoChanged, activeSubInfoEntityList = "
                    + activeSubInfoEntityList);
        }
        List<SubscriptionInfoEntity> tempActiveSubInfoEntityList = new ArrayList<>(
                activeSubInfoEntityList);
        synchronized (this) {
            mActiveSubInfoEntityList = activeSubInfoEntityList;
        }
        for (MobileNetworkCallback callback : sCallbacks) {
            callback.onActiveSubInfoChanged(tempActiveSubInfoEntityList);
        }
    }

    private void sendAvailableSubInfoCache(MobileNetworkCallback callback) {
        if (callback != null) {
             List<SubscriptionInfoEntity> availableSubInfoEntityList = null;
             List<SubscriptionInfoEntity> activeSubInfoEntityList = null;
             synchronized (this) {
                 if (mAvailableSubInfoEntityList != null) {
                     availableSubInfoEntityList = new ArrayList<>(mAvailableSubInfoEntityList);
                 }
                 if (mActiveSubInfoEntityList != null) {
                     activeSubInfoEntityList = new ArrayList<>(mActiveSubInfoEntityList);
                 }
             }
             if (availableSubInfoEntityList != null) {
                 callback.onAvailableSubInfoChanged(availableSubInfoEntityList);
             }
             if (activeSubInfoEntityList != null) {
                 callback.onActiveSubInfoChanged(activeSubInfoEntityList);
             }
        }
    }

    private void onAllUiccInfoChanged(List<UiccInfoEntity> uiccInfoEntityList) {
        mUiccInfoEntityList = new ArrayList<>(uiccInfoEntityList);
        for (MobileNetworkCallback callback : sCallbacks) {
            callback.onAllUiccInfoChanged(uiccInfoEntityList);
        }
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_MOBILE_NETWORK_DB_NOTIFY_UICC_INFO_IS_CHANGED, 0);
    }

    private void onAllMobileNetworkInfoChanged(
            List<MobileNetworkInfoEntity> mobileNetworkInfoEntityList) {
        mMobileNetworkInfoEntityList = new ArrayList<>(mobileNetworkInfoEntityList);
        for (MobileNetworkCallback callback : sCallbacks) {
            callback.onAllMobileNetworkInfoChanged(mobileNetworkInfoEntityList);
        }
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_MOBILE_NETWORK_DB_NOTIFY_MOBILE_NETWORK_INFO_IS_CHANGED, 0);
    }

    private void insertSubInfo(Context context, SubscriptionInfo info) {
        int subId = info.getSubscriptionId();
        createTelephonyManagerBySubId(subId);
        TelephonyManager telephonyManager = getTelephonyManagerBySubId(context, subId);
        SubscriptionInfoEntity subInfoEntity =
                convertToSubscriptionInfoEntity(context, info, telephonyManager);
        if (subInfoEntity != null) {
            if (!sCacheSubscriptionInfoEntityMap.containsKey(subId)
                    || (sCacheSubscriptionInfoEntityMap.get(subId) != null
                    && !sCacheSubscriptionInfoEntityMap.get(subId).equals(subInfoEntity))) {
                sCacheSubscriptionInfoEntityMap.put(subId, subInfoEntity);
                if (DEBUG) {
                    Log.d(TAG, "Convert subId " + subId + " to SubscriptionInfoEntity: "
                            + subInfoEntity);
                }
                mMobileNetworkDatabase.insertSubsInfo(subInfoEntity);
                mMetricsFeatureProvider.action(mContext,
                        SettingsEnums.ACTION_MOBILE_NETWORK_DB_INSERT_SUB_INFO, subId);
                insertUiccInfo(subId, telephonyManager);
                insertMobileNetworkInfo(context, subId, telephonyManager);
            }
        } else if (DEBUG) {
            Log.d(TAG, "Can not insert subInfo, the entity is null");
        }
    }

    private void deleteAllInfoBySubId(String subId) {
        if (DEBUG) {
            Log.d(TAG, "deleteAllInfoBySubId, subId = " + subId);
        }
        mMobileNetworkDatabase.deleteSubInfoBySubId(subId);
        mMobileNetworkDatabase.deleteUiccInfoBySubId(subId);
        mMobileNetworkDatabase.deleteMobileNetworkInfoBySubId(subId);
        mUiccInfoEntityList.removeIf(info -> info.subId.equals(subId));
        mMobileNetworkInfoEntityList.removeIf(info -> info.subId.equals(subId));
        int id = Integer.parseInt(subId);
        removerRegisterBySubId(id);
        mSubscriptionInfoMap.remove(id);
        mTelephonyManagerMap.remove(id);
        sCacheSubscriptionInfoEntityMap.remove(id);
        sCacheUiccInfoEntityMap.remove(id);
        sCacheMobileNetworkInfoEntityMap.remove(id);
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_MOBILE_NETWORK_DB_DELETE_DATA, id);
    }

    private SubscriptionInfoEntity convertToSubscriptionInfoEntity(Context context,
            SubscriptionInfo subInfo, TelephonyManager telephonyManager) {
        int subId = subInfo.getSubscriptionId();
        if (telephonyManager == null) {
            if (DEBUG) {
                Log.d(TAG, "Can not get TelephonyManager for subId " + subId);
            }
            return null;
        }
        UiccSlotInfo[] uiccSlotInfos = telephonyManager.getUiccSlotsInfo();
        if (uiccSlotInfos == null || uiccSlotInfos.length == 0) {
            if (DEBUG) {
                Log.d(TAG, "uiccSlotInfos = null or empty");
            }
            return null;
        } else {
            getUiccInfoBySubscriptionInfo(uiccSlotInfos, subInfo);
            SubscriptionInfo firstRemovableSubInfo = SubscriptionUtil.getFirstRemovableSubscription(
                    context);
            if (DEBUG) {
                Log.d(TAG, "convert subscriptionInfo to entity for subId = " + subId);
            }
            return new SubscriptionInfoEntity(String.valueOf(subId),
                    subInfo.getSimSlotIndex(),
                    subInfo.getCarrierId(), subInfo.getDisplayName().toString(),
                    subInfo.getCarrierName() != null ? subInfo.getCarrierName().toString() : "",
                    subInfo.getDataRoaming(), subInfo.getMccString(), subInfo.getMncString(),
                    subInfo.getCountryIso(), subInfo.isEmbedded(), mCardId,
                    subInfo.getPortIndex(), subInfo.isOpportunistic(),
                    String.valueOf(subInfo.getGroupUuid()),
                    subInfo.getSubscriptionType(),
                    SubscriptionUtil.getUniqueSubscriptionDisplayName(subInfo, context).toString(),
                    SubscriptionUtil.isSubscriptionVisible(mSubscriptionManager, context, subInfo),
                    SubscriptionUtil.getFormattedPhoneNumber(context, subInfo),
                    firstRemovableSubInfo == null ? false
                            : firstRemovableSubInfo.getSubscriptionId() == subId,
                    SubscriptionUtil.isDefaultSubscription(context, subId),
                    mSubscriptionManager.isValidSubscriptionId(subId),
                    mSubscriptionManager.isUsableSubscriptionId(subId),
                    mSubscriptionManager.isActiveSubscriptionId(subId),
                    true /*availableSubInfo*/,
                    mSubscriptionManager.getActiveDataSubscriptionId() == subId);
        }
    }

    private void insertUiccInfo(int subId, TelephonyManager telephonyManager) {
        UiccInfoEntity uiccInfoEntity = convertToUiccInfoEntity(subId, telephonyManager);
        if (DEBUG) {
            Log.d(TAG, "uiccInfoEntity = " + uiccInfoEntity);
        }
        if (!sCacheUiccInfoEntityMap.containsKey(subId)
                || !sCacheUiccInfoEntityMap.get(subId).equals(uiccInfoEntity)) {
            sCacheUiccInfoEntityMap.put(subId, uiccInfoEntity);
            mMobileNetworkDatabase.insertUiccInfo(uiccInfoEntity);
            mMetricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_MOBILE_NETWORK_DB_INSERT_UICC_INFO, subId);
        }
    }

    private void insertMobileNetworkInfo(Context context, int subId,
            TelephonyManager telephonyManager) {
        MobileNetworkInfoEntity mobileNetworkInfoEntity = convertToMobileNetworkInfoEntity(context,
                subId, telephonyManager);

        if (DEBUG) {
            Log.d(TAG, "insertMobileNetworkInfo, mobileNetworkInfoEntity = "
                    + mobileNetworkInfoEntity);
        }

        if (mobileNetworkInfoEntity == null) {
            return;
        }

        if (!sCacheMobileNetworkInfoEntityMap.containsKey(subId)
                || !sCacheMobileNetworkInfoEntityMap.get(subId).equals(mobileNetworkInfoEntity)) {
            sCacheMobileNetworkInfoEntityMap.put(subId, mobileNetworkInfoEntity);
            mMobileNetworkDatabase.insertMobileNetworkInfo(mobileNetworkInfoEntity);
            mMetricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_MOBILE_NETWORK_DB_INSERT_MOBILE_NETWORK_INFO, subId);
        }
    }

    private MobileNetworkInfoEntity convertToMobileNetworkInfoEntity(Context context, int subId,
            TelephonyManager telephonyManager) {
        boolean isDataEnabled = false;
        boolean isDataRoamingEnabled = false;
        if (telephonyManager != null) {
            isDataEnabled = telephonyManager.isDataEnabled();
            isDataRoamingEnabled = telephonyManager.isDataRoamingEnabled();
        } else if (DEBUG) {
            Log.d(TAG, "TelephonyManager is null, subId = " + subId);
        }

        return new MobileNetworkInfoEntity(String.valueOf(subId),
                MobileNetworkUtils.isContactDiscoveryEnabled(context, subId),
                MobileNetworkUtils.isContactDiscoveryVisible(context, subId),
                isDataEnabled,
                MobileNetworkUtils.isCdmaOptions(context, subId),
                MobileNetworkUtils.isGsmOptions(context, subId),
                MobileNetworkUtils.isWorldMode(context, subId),
                MobileNetworkUtils.shouldDisplayNetworkSelectOptions(context, subId),
                MobileNetworkUtils.isTdscdmaSupported(context, subId),
                MobileNetworkUtils.activeNetworkIsCellular(context),
                SubscriptionUtil.showToggleForPhysicalSim(mSubscriptionManager),
                isDataRoamingEnabled
        );
    }

    private UiccInfoEntity convertToUiccInfoEntity(int subId, TelephonyManager telephonyManager) {
        return new UiccInfoEntity(String.valueOf(subId), String.valueOf(mPhysicalSlotIndex),
                mLogicalSlotIndex, mCardId, mIsEuicc,
                isMultipleEnabledProfilesSupported(telephonyManager), mCardState, mIsRemovable,
                mIsActive, mPortIndex
        );
    }

    private boolean isMultipleEnabledProfilesSupported(TelephonyManager telephonyManager) {
        if (telephonyManager == null) {
            if (DEBUG) {
                Log.d(TAG, "TelephonyManager is null");
            }
            return false;
        }

        List<UiccCardInfo> cardInfos = telephonyManager.getUiccCardsInfo();
        if (cardInfos == null) {
            if (DEBUG) {
                Log.d(TAG, "UICC card info list is empty.");
            }
            return false;
        }
        return cardInfos.stream().anyMatch(
                cardInfo -> cardInfo.isMultipleEnabledProfilesSupported());
    }

    @Override
    public void onSubscriptionsChanged() {
        insertAvailableSubInfoToEntity(
                SubscriptionUtil.getSelectableSubscriptionInfoList(mContext));
    }

    private void insertAvailableSubInfoToEntity(List<SubscriptionInfo> inputAvailableInfoList) {
        sExecutor.execute(() -> {
            SubscriptionInfoEntity[] availableInfoArray = null;
            int availableEntitySize = 0;
            synchronized (this) {
                availableInfoArray = mAvailableSubInfoEntityList.toArray(
                    new SubscriptionInfoEntity[0]);
                availableEntitySize = mAvailableSubInfoEntityList.size();
            }
            if ((inputAvailableInfoList == null || inputAvailableInfoList.size() == 0)
                    && availableEntitySize != 0) {
                if (DEBUG) {
                    Log.d(TAG, "availableSudInfoList from framework is empty, remove all subs");
                }

                for (SubscriptionInfoEntity info : availableInfoArray) {
                    deleteAllInfoBySubId(info.subId);
                }

            } else if (inputAvailableInfoList != null) {
                SubscriptionInfo[] inputAvailableInfoArray = inputAvailableInfoList.toArray(
                        new SubscriptionInfo[0]);
                // Remove the redundant subInfo
                if (inputAvailableInfoList.size() <= availableEntitySize) {
                    for (SubscriptionInfo subInfo : inputAvailableInfoArray) {
                        int subId = subInfo.getSubscriptionId();
                        if (mSubscriptionInfoMap.containsKey(subId)) {
                            mSubscriptionInfoMap.remove(subId);
                        }
                    }

                    if (!mSubscriptionInfoMap.isEmpty()) {
                        for (Integer key : mSubscriptionInfoMap.keySet()) {
                            if (key != null) {
                                deleteAllInfoBySubId(String.valueOf(key));
                            }
                        }
                    } else if (inputAvailableInfoList.size() < availableEntitySize) {
                        // Check the subInfo between the new list from framework and old list in
                        // the database, if the subInfo is not existed in the new list, delete it
                        // from the database.
                        for (SubscriptionInfoEntity info : availableInfoArray) {
                            if (sCacheSubscriptionInfoEntityMap.containsKey(info.getSubId())) {
                                deleteAllInfoBySubId(info.subId);
                            }
                        }
                    }
                }

                // Insert all new available subInfo to database.
                for (SubscriptionInfo subInfo : inputAvailableInfoArray) {
                    if (DEBUG) {
                        Log.d(TAG, "insert subInfo to subInfoEntity, subInfo = " + subInfo);
                    }
                    if (subInfo.isEmbedded()
                        && (subInfo.getProfileClass() == PROFILE_CLASS_PROVISIONING
                            || (Flags.oemEnabledSatelliteFlag()
                            && subInfo.isOnlyNonTerrestrialNetwork()))) {
                        if (DEBUG) {
                            Log.d(TAG, "Do not insert the provisioning or satellite eSIM");
                        }
                        continue;
                    }
                    mSubscriptionInfoMap.put(subInfo.getSubscriptionId(), subInfo);
                    insertSubInfo(mContext, subInfo);
                }
            }
        });
    }

    public boolean isAirplaneModeOn() {
        return Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private class PhoneCallStateTelephonyCallback extends TelephonyCallback implements
            TelephonyCallback.CallStateListener {

        @Override
        public void onCallStateChanged(int state) {
            for (MobileNetworkCallback callback : sCallbacks) {
                callback.onCallStateChanged(state);
            }
        }
    }

    /**
     * Callback for clients to get the latest info changes if the framework or content observers.
     * updates the relevant info.
     */
    public interface MobileNetworkCallback {
        default void onAvailableSubInfoChanged(List<SubscriptionInfoEntity> subInfoEntityList) {
        }

        default void onActiveSubInfoChanged(List<SubscriptionInfoEntity> subInfoEntityList) {
        }

        default void onAllUiccInfoChanged(List<UiccInfoEntity> uiccInfoEntityList) {
        }

        default void onAllMobileNetworkInfoChanged(
                List<MobileNetworkInfoEntity> mobileNetworkInfoEntityList) {
        }

        default void onAirplaneModeChanged(boolean enabled) {
        }

        /**
         * Notify clients data roaming changed of subscription.
         */
        default void onDataRoamingChanged(int subId, boolean enabled) {
        }

        default void onCallStateChanged(int state) {
        }
    }

    public void dump(IndentingPrintWriter printwriter) {
        printwriter.println(TAG + ": ");
        printwriter.increaseIndent();
        printwriter.println(" availableSubInfoEntityList= " + mAvailableSubInfoEntityList);
        printwriter.println(" activeSubInfoEntityList=" + mActiveSubInfoEntityList);
        printwriter.println(" mobileNetworkInfoEntityList= " + mMobileNetworkInfoEntityList);
        printwriter.println(" uiccInfoEntityList= " + mUiccInfoEntityList);
        printwriter.println(" CacheSubscriptionInfoEntityMap= " + sCacheSubscriptionInfoEntityMap);
        printwriter.println(" SubscriptionInfoMap= " + mSubscriptionInfoMap);
        printwriter.flush();
        printwriter.decreaseIndent();
    }
}
