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

import static com.android.internal.telephony.TelephonyIntents.ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED;

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.util.Log;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

public class MobileNetworkRepository extends SubscriptionManager.OnSubscriptionsChangedListener {

    private static final String TAG = "MobileNetworkRepository";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static ExecutorService sExecutor = Executors.newSingleThreadExecutor();
    private static Map<String, SubscriptionInfoEntity> sCacheSubscriptionInfoEntityMap =
            new ArrayMap<>();
    private static Map<String, MobileNetworkInfoEntity> sCacheMobileNetworkInfoEntityMap =
            new ArrayMap<>();
    private static Map<String, UiccInfoEntity> sCacheUiccInfoEntityMap = new ArrayMap<>();

    private SubscriptionManager mSubscriptionManager;
    private TelephonyManager mTelephonyManager;
    private MobileNetworkDatabase mMobileNetworkDatabase;
    private SubscriptionInfoDao mSubscriptionInfoDao;
    private UiccInfoDao mUiccInfoDao;
    private MobileNetworkInfoDao mMobileNetworkInfoDao;
    private List<SubscriptionInfoEntity> mAvailableSubInfoEntityList = new ArrayList<>();
    private List<SubscriptionInfoEntity> mActiveSubInfoEntityList = new ArrayList<>();
    private List<UiccInfoEntity> mUiccInfoEntityList = new ArrayList<>();
    private List<MobileNetworkInfoEntity> mMobileNetworkInfoEntityList = new ArrayList<>();
    private MobileNetworkCallback mCallback;
    private Context mContext;
    private AirplaneModeObserver mAirplaneModeObserver;
    private Uri mAirplaneModeSettingUri;
    private MetricsFeatureProvider mMetricsFeatureProvider;
    private IntentFilter mFilter = new IntentFilter();
    private Map<Integer, MobileDataContentObserver> mDataContentObserverMap = new HashMap<>();
    private int mPhysicalSlotIndex = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
    private int mLogicalSlotIndex = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
    private int mCardState = UiccSlotInfo.CARD_STATE_INFO_ABSENT;
    private int mPortIndex = TelephonyManager.INVALID_PORT_INDEX;
    private int mCardId = TelephonyManager.UNINITIALIZED_CARD_ID;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private boolean mIsEuicc = false;
    private boolean mIsRemovable = false;
    private boolean mIsActive = false;
    private Map<Integer, SubscriptionInfo> mSubscriptionInfoMap = new ArrayMap<>();
    private Map<Integer, TelephonyManager> mTelephonyManagerMap = new HashMap<>();
    private Map<Integer, PhoneCallStateTelephonyCallback> mTelephonyCallbackMap = new HashMap<>();

    public static MobileNetworkRepository create(Context context,
            MobileNetworkCallback mobileNetworkCallback) {
        return new MobileNetworkRepository(context, mobileNetworkCallback,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);
    }

    public static MobileNetworkRepository createBySubId(Context context,
            MobileNetworkCallback mobileNetworkCallback, int subId) {
        return new MobileNetworkRepository(context, mobileNetworkCallback, subId);
    }

    private MobileNetworkRepository(Context context, MobileNetworkCallback mobileNetworkCallback,
            int subId) {
        mSubId = subId;
        mContext = context;
        mMobileNetworkDatabase = MobileNetworkDatabase.getInstance(context);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_MOBILE_NETWORK_DB_CREATED,
                subId);
        mSubscriptionManager = context.getSystemService(SubscriptionManager.class);
        mCallback = mobileNetworkCallback;
        mSubscriptionInfoDao = mMobileNetworkDatabase.mSubscriptionInfoDao();
        mUiccInfoDao = mMobileNetworkDatabase.mUiccInfoDao();
        mMobileNetworkInfoDao = mMobileNetworkDatabase.mMobileNetworkInfoDao();
        mAirplaneModeObserver = new AirplaneModeObserver(new Handler(Looper.getMainLooper()));
        mAirplaneModeSettingUri = Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON);
        mFilter.addAction(TelephonyManager.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);
        mFilter.addAction(SubscriptionManager.ACTION_DEFAULT_SUBSCRIPTION_CHANGED);
        mFilter.addAction(ACTION_DEFAULT_VOICE_SUBSCRIPTION_CHANGED);
        mFilter.addAction(SubscriptionManager.ACTION_DEFAULT_SMS_SUBSCRIPTION_CHANGED);
    }

    private class AirplaneModeObserver extends ContentObserver {
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
                mCallback.onAirplaneModeChanged(isAirplaneModeOn());
            }
        }
    }

    private final BroadcastReceiver mDataSubscriptionChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            onSubscriptionsChanged();
        }
    };

    public void addRegister(LifecycleOwner lifecycleOwner) {
        mSubscriptionManager.addOnSubscriptionsChangedListener(mContext.getMainExecutor(), this);
        mAirplaneModeObserver.register(mContext);
        mContext.registerReceiver(mDataSubscriptionChangedReceiver, mFilter);
        observeAllSubInfo(lifecycleOwner);
        observeAllUiccInfo(lifecycleOwner);
        observeAllMobileNetworkInfo(lifecycleOwner);
    }

    private void addRegisterBySubId(int subId) {
        if (!mTelephonyCallbackMap.containsKey(subId)) {
            PhoneCallStateTelephonyCallback
                    telephonyCallback = new PhoneCallStateTelephonyCallback();
            mTelephonyManager.registerTelephonyCallback(mContext.getMainExecutor(),
                    telephonyCallback);
            mTelephonyCallbackMap.put(subId, telephonyCallback);
            mTelephonyManagerMap.put(subId, mTelephonyManager);
        }
        if (!mDataContentObserverMap.containsKey(subId)) {
            MobileDataContentObserver dataContentObserver = new MobileDataContentObserver(
                    new Handler(Looper.getMainLooper()));
            dataContentObserver.register(mContext, subId);
            dataContentObserver.setOnMobileDataChangedListener(() -> {
                sExecutor.execute(() -> {
                    insertMobileNetworkInfo(mContext, String.valueOf(subId));
                });
            });
            mDataContentObserverMap.put(subId, dataContentObserver);
        }
    }

    private void removerRegisterBySubId(int subId) {
        if (mTelephonyCallbackMap.containsKey(subId)) {
            TelephonyManager tm = mTelephonyManagerMap.get(subId);
            PhoneCallStateTelephonyCallback callback = mTelephonyCallbackMap.get(subId);
            if (callback != null) {
                tm.unregisterTelephonyCallback(callback);
                mTelephonyCallbackMap.remove(subId);
            }
        }
        if (mDataContentObserverMap.containsKey(subId)) {
            mDataContentObserverMap.get(subId).unRegister(mContext);
            mDataContentObserverMap.remove(subId);
        }
    }

    public void removeRegister() {
        mSubscriptionManager.removeOnSubscriptionsChangedListener(this);
        mAirplaneModeObserver.unRegister(mContext);
        if (mDataSubscriptionChangedReceiver != null) {
            mContext.unregisterReceiver(mDataSubscriptionChangedReceiver);
        }
        mDataContentObserverMap.forEach((id, observer) -> {
            observer.unRegister(mContext);
        });
        mDataContentObserverMap.clear();

        mTelephonyManagerMap.forEach((id, manager) -> {
            TelephonyCallback callback = mTelephonyCallbackMap.get(manager.getSubscriptionId());
            if (callback != null) {
                manager.unregisterTelephonyCallback(callback);
            }
        });
        mTelephonyCallbackMap.clear();
        mTelephonyManagerMap.clear();
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

    public int getSubInfosCount() {
        return mSubscriptionInfoDao.count();
    }

    public int getUiccInfosCount() {
        return mUiccInfoDao.count();
    }

    public int getMobileNetworkInfosCount() {
        return mMobileNetworkInfoDao.count();
    }

    private void getUiccInfoBySubscriptionInfo(UiccSlotInfo[] uiccSlotInfos,
            SubscriptionInfo subInfo) {
        for (int i = 0; i < uiccSlotInfos.length; i++) {
            UiccSlotInfo curSlotInfo = uiccSlotInfos[i];
            if (curSlotInfo.getCardStateInfo() == CARD_STATE_INFO_PRESENT) {
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
                                + mSubId);
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
        mAvailableSubInfoEntityList = availableSubInfoEntityList;
        mActiveSubInfoEntityList = mAvailableSubInfoEntityList.stream()
                .filter(SubscriptionInfoEntity::isActiveSubscription)
                .filter(SubscriptionInfoEntity::isSubscriptionVisible)
                .collect(Collectors.toList());
        if (DEBUG) {
            Log.d(TAG, "onAvailableSubInfoChanged, availableSubInfoEntityList = "
                    + availableSubInfoEntityList);
        }
        mCallback.onAvailableSubInfoChanged(availableSubInfoEntityList);
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_MOBILE_NETWORK_DB_NOTIFY_SUB_INFO_IS_CHANGED, 0);
        setActiveSubInfoList(mActiveSubInfoEntityList);
    }

    private void setActiveSubInfoList(List<SubscriptionInfoEntity> activeSubInfoEntityList) {
        if (DEBUG) {
            Log.d(TAG,
                    "onActiveSubInfoChanged, activeSubInfoEntityList = " + activeSubInfoEntityList);
        }
        mCallback.onActiveSubInfoChanged(mActiveSubInfoEntityList);
    }

    private void onAllUiccInfoChanged(List<UiccInfoEntity> uiccInfoEntityList) {
        mUiccInfoEntityList = uiccInfoEntityList;
        mCallback.onAllUiccInfoChanged(uiccInfoEntityList);
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_MOBILE_NETWORK_DB_NOTIFY_UICC_INFO_IS_CHANGED, 0);
    }

    private void onAllMobileNetworkInfoChanged(
            List<MobileNetworkInfoEntity> mobileNetworkInfoEntityList) {
        mMobileNetworkInfoEntityList = mobileNetworkInfoEntityList;
        mCallback.onAllMobileNetworkInfoChanged(mobileNetworkInfoEntityList);
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_MOBILE_NETWORK_DB_NOTIFY_MOBILE_NETWORK_INFO_IS_CHANGED, 0);
    }

    public void insertSubInfo(Context context, SubscriptionInfo info) {
        SubscriptionInfoEntity subInfoEntity =
                convertToSubscriptionInfoEntity(context, info);
        String subId = String.valueOf(mSubId);
        if (subInfoEntity != null) {
            if (!sCacheSubscriptionInfoEntityMap.containsKey(subId)
                    || (sCacheSubscriptionInfoEntityMap.get(subId) != null
                    && !sCacheSubscriptionInfoEntityMap.get(subId).equals(subInfoEntity))) {
                sCacheSubscriptionInfoEntityMap.put(subId, subInfoEntity);
                if (DEBUG) {
                    Log.d(TAG, "convert subId " + subId + "to SubscriptionInfoEntity: "
                            + subInfoEntity);
                }
                mMobileNetworkDatabase.insertSubsInfo(subInfoEntity);
                addRegisterBySubId(mSubId);
                insertUiccInfo(subId);
                insertMobileNetworkInfo(context, subId);
                mMetricsFeatureProvider.action(mContext,
                        SettingsEnums.ACTION_MOBILE_NETWORK_DB_INSERT_SUB_INFO, mSubId);
            } else if (DEBUG) {
                Log.d(TAG, "Can not insert subInfo, the entity is null");
            }
        }
    }

    public void deleteAllInfoBySubId(String subId) {
        if (DEBUG) {
            Log.d(TAG, "deleteAllInfoBySubId, subId = " + subId);
        }
        mMobileNetworkDatabase.deleteSubInfoBySubId(subId);
        mMobileNetworkDatabase.deleteUiccInfoBySubId(subId);
        mMobileNetworkDatabase.deleteMobileNetworkInfoBySubId(subId);
        mAvailableSubInfoEntityList.removeIf(info -> info.subId.equals(subId));
        mActiveSubInfoEntityList.removeIf(info -> info.subId.equals(subId));
        mUiccInfoEntityList.removeIf(info -> info.subId.equals(subId));
        mMobileNetworkInfoEntityList.removeIf(info -> info.subId.equals(subId));
        int id = Integer.parseInt(subId);
        removerRegisterBySubId(id);
        mSubscriptionInfoMap.remove(id);
        mTelephonyManagerMap.remove(id);
        sCacheSubscriptionInfoEntityMap.remove(subId);
        sCacheUiccInfoEntityMap.remove(subId);
        sCacheMobileNetworkInfoEntityMap.remove(subId);
        mMetricsFeatureProvider.action(mContext,
                SettingsEnums.ACTION_MOBILE_NETWORK_DB_DELETE_DATA, id);
    }

    public SubscriptionInfoEntity convertToSubscriptionInfoEntity(Context context,
            SubscriptionInfo subInfo) {
        mSubId = subInfo.getSubscriptionId();
        mTelephonyManager = context.getSystemService(
                TelephonyManager.class).createForSubscriptionId(mSubId);

        UiccSlotInfo[] uiccSlotInfos = mTelephonyManager.getUiccSlotsInfo();
        if (uiccSlotInfos == null || uiccSlotInfos.length == 0) {
            if (DEBUG) {
                Log.d(TAG, "uiccSlotInfos = null or empty");
            }
            return null;
        } else {
            getUiccInfoBySubscriptionInfo(uiccSlotInfos, subInfo);
            SubscriptionInfo firstRemovableSubInfo = SubscriptionUtil.getFirstRemovableSubscription(
                    context);
            SubscriptionInfo subscriptionOrDefault = SubscriptionUtil.getSubscriptionOrDefault(
                    context, mSubId);
            if(DEBUG){
                Log.d(TAG, "convert subscriptionInfo to entity for subId = " + mSubId);
            }
            return new SubscriptionInfoEntity(String.valueOf(mSubId),
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
                            : firstRemovableSubInfo.getSubscriptionId() == mSubId,
                    String.valueOf(SubscriptionUtil.getDefaultSimConfig(context, mSubId)),
                    subscriptionOrDefault == null ? false
                            : subscriptionOrDefault.getSubscriptionId() == mSubId,
                    mSubscriptionManager.isValidSubscriptionId(mSubId),
                    mSubscriptionManager.isUsableSubscriptionId(mSubId),
                    mSubscriptionManager.isActiveSubscriptionId(mSubId),
                    true /*availableSubInfo*/,
                    mSubscriptionManager.getDefaultVoiceSubscriptionId() == mSubId,
                    mSubscriptionManager.getDefaultSmsSubscriptionId() == mSubId,
                    mSubscriptionManager.getDefaultDataSubscriptionId() == mSubId,
                    mSubscriptionManager.getDefaultSubscriptionId() == mSubId,
                    mSubscriptionManager.getActiveDataSubscriptionId() == mSubId);
        }
    }

    public void insertUiccInfo(String subId) {
        UiccInfoEntity uiccInfoEntity = convertToUiccInfoEntity();
        if (DEBUG) {
            Log.d(TAG, "uiccInfoEntity = " + uiccInfoEntity);
        }
        if (!sCacheUiccInfoEntityMap.containsKey(subId)
                || !sCacheUiccInfoEntityMap.get(subId).equals(uiccInfoEntity)) {
            sCacheUiccInfoEntityMap.put(subId, uiccInfoEntity);
            mMobileNetworkDatabase.insertUiccInfo(uiccInfoEntity);
            mMetricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_MOBILE_NETWORK_DB_INSERT_UICC_INFO,
                    Integer.parseInt(subId));
        }
    }

    public void insertMobileNetworkInfo(Context context, String subId) {
        MobileNetworkInfoEntity mobileNetworkInfoEntity = convertToMobileNetworkInfoEntity(context);
        if (DEBUG) {
            Log.d(TAG, "mobileNetworkInfoEntity = " + mobileNetworkInfoEntity);
        }
        if (!sCacheMobileNetworkInfoEntityMap.containsKey(subId)
                || !sCacheMobileNetworkInfoEntityMap.get(subId).equals(mobileNetworkInfoEntity)) {
            sCacheMobileNetworkInfoEntityMap.put(subId, mobileNetworkInfoEntity);
            mMobileNetworkDatabase.insertMobileNetworkInfo(mobileNetworkInfoEntity);
            mMetricsFeatureProvider.action(mContext,
                    SettingsEnums.ACTION_MOBILE_NETWORK_DB_INSERT_MOBILE_NETWORK_INFO,
                    Integer.parseInt(subId));
        }
    }

    public MobileNetworkInfoEntity convertToMobileNetworkInfoEntity(Context context) {
        return new MobileNetworkInfoEntity(String.valueOf(mSubId),
                MobileNetworkUtils.isContactDiscoveryEnabled(context, mSubId),
                MobileNetworkUtils.isContactDiscoveryVisible(context, mSubId),
                mTelephonyManager.isDataEnabled(),
                MobileNetworkUtils.isCdmaOptions(context, mSubId),
                MobileNetworkUtils.isGsmOptions(context, mSubId),
                MobileNetworkUtils.isWorldMode(context, mSubId),
                MobileNetworkUtils.shouldDisplayNetworkSelectOptions(context, mSubId),
                MobileNetworkUtils.isTdscdmaSupported(context, mSubId),
                MobileNetworkUtils.activeNetworkIsCellular(context),
                SubscriptionUtil.showToggleForPhysicalSim(mSubscriptionManager),
                mTelephonyManager.isDataRoamingEnabled()
        );
    }

    private UiccInfoEntity convertToUiccInfoEntity() {
        return new UiccInfoEntity(String.valueOf(mSubId), String.valueOf(mPhysicalSlotIndex),
                mLogicalSlotIndex, mCardId, mIsEuicc, isMultipleEnabledProfilesSupported(),
                mCardState, mIsRemovable, mIsActive, mPortIndex
        );
    }

    private boolean isMultipleEnabledProfilesSupported() {
        List<UiccCardInfo> cardInfos = mTelephonyManager.getUiccCardsInfo();
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

    private void insertAvailableSubInfoToEntity(List<SubscriptionInfo> availableInfoList) {
        sExecutor.execute(() -> {
            SubscriptionInfoEntity[] availableInfoArray = mAvailableSubInfoEntityList.toArray(
                    new SubscriptionInfoEntity[0]);
            if ((availableInfoList == null || availableInfoList.size() == 0)
                    && mAvailableSubInfoEntityList.size() != 0) {
                if (DEBUG) {
                    Log.d(TAG, "availableSudInfoList from framework is empty, remove all subs");
                }

                for (SubscriptionInfoEntity info : availableInfoArray) {
                    deleteAllInfoBySubId(info.subId);
                }

            } else if (availableInfoList != null) {
                SubscriptionInfo[] infoArray = availableInfoList.toArray(new SubscriptionInfo[0]);
                // Remove the redundant subInfo
                if (availableInfoList.size() <= mAvailableSubInfoEntityList.size()) {
                    for (SubscriptionInfo subInfo : infoArray) {
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
                    }
                }

                // Insert all new available subInfo to database.
                for (SubscriptionInfo subInfo : infoArray) {
                    if (DEBUG) {
                        Log.d(TAG, "insert subInfo to subInfoEntity, subInfo = " + subInfo);
                    }
                    if (subInfo.isEmbedded()
                            && subInfo.getProfileClass() == PROFILE_CLASS_PROVISIONING) {
                        if (DEBUG) {
                            Log.d(TAG, "Do not insert the provision eSIM");
                        }
                        continue;
                    }
                    mSubscriptionInfoMap.put(mSubId, subInfo);
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
            mCallback.onCallStateChanged(state);
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

        default void onCallStateChanged(int state) {
        }
    }
}
