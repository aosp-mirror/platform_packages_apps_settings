/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.sim;

import android.provider.SearchIndexableResource;
import com.android.settings.R;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContentUris;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Telephony;
import android.telephony.SubInfoRecord;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.PhoneNumberUtils;
import android.telecom.PhoneAccount;
import android.telephony.CellInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.app.Dialog;
import android.app.DialogFragment;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;

import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyIntents;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.notification.DropDownPreference;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.Indexable.SearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SimSettings extends RestrictedSettingsFragment implements Indexable {
    private static final String TAG = "SimSettings";

    private static final String DISALLOW_CONFIG_SIM = "no_config_sim";
    private static final String SIM_CARD_CATEGORY = "sim_cards";
    private static final String KEY_CELLULAR_DATA = "sim_cellular_data";
    private static final String KEY_CALLS = "sim_calls";
    private static final String KEY_SMS = "sim_sms";
    private static final String KEY_ACTIVITIES = "activities";
    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int PROXY_INDEX = 3;
    private static final int PORT_INDEX = 4;
    private static final int USER_INDEX = 5;
    private static final int SERVER_INDEX = 6;
    private static final int PASSWORD_INDEX = 7;
    private static final int MMSC_INDEX = 8;
    private static final int MCC_INDEX = 9;
    private static final int MNC_INDEX = 10;
    private static final int NUMERIC_INDEX = 11;
    private static final int MMSPROXY_INDEX = 12;
    private static final int MMSPORT_INDEX = 13;
    private static final int AUTH_TYPE_INDEX = 14;
    private static final int TYPE_INDEX = 15;
    private static final int PROTOCOL_INDEX = 16;
    private static final int CARRIER_ENABLED_INDEX = 17;
    private static final int BEARER_INDEX = 18;
    private static final int ROAMING_PROTOCOL_INDEX = 19;
    private static final int MVNO_TYPE_INDEX = 20;
    private static final int MVNO_MATCH_DATA_INDEX = 21;
    private static final int DATA_PICK = 0;
    private static final int CALLS_PICK = 1;
    private static final int SMS_PICK = 2;

    /**
     * By UX design we use only one Subscription Information(SubInfo) record per SIM slot.
     * mAvalableSubInfos is the list of SubInfos we present to the user.
     * mSubInfoList is the list of all SubInfos.
     */
    private List<SubInfoRecord> mAvailableSubInfos = null;
    private List<SubInfoRecord> mSubInfoList = null;

    private SubInfoRecord mCellularData = null;
    private SubInfoRecord mCalls = null;
    private SubInfoRecord mSMS = null;

    private PreferenceCategory mSimCards = null;

    private int mNumSims;
    /**
     * Standard projection for the interesting columns of a normal note.
     */
    private static final String[] sProjection = new String[] {
            Telephony.Carriers._ID,     // 0
            Telephony.Carriers.NAME,    // 1
            Telephony.Carriers.APN,     // 2
            Telephony.Carriers.PROXY,   // 3
            Telephony.Carriers.PORT,    // 4
            Telephony.Carriers.USER,    // 5
            Telephony.Carriers.SERVER,  // 6
            Telephony.Carriers.PASSWORD, // 7
            Telephony.Carriers.MMSC, // 8
            Telephony.Carriers.MCC, // 9
            Telephony.Carriers.MNC, // 10
            Telephony.Carriers.NUMERIC, // 11
            Telephony.Carriers.MMSPROXY,// 12
            Telephony.Carriers.MMSPORT, // 13
            Telephony.Carriers.AUTH_TYPE, // 14
            Telephony.Carriers.TYPE, // 15
            Telephony.Carriers.PROTOCOL, // 16
            Telephony.Carriers.CARRIER_ENABLED, // 17
            Telephony.Carriers.BEARER, // 18
            Telephony.Carriers.ROAMING_PROTOCOL, // 19
            Telephony.Carriers.MVNO_TYPE,   // 20
            Telephony.Carriers.MVNO_MATCH_DATA  // 21
    };

    public SimSettings() {
        super(DISALLOW_CONFIG_SIM);
    }

    @Override
    public void onCreate(final Bundle bundle) {
        super.onCreate(bundle);

        if (mSubInfoList == null) {
            mSubInfoList = SubscriptionManager.getActiveSubInfoList();
        }

        createPreferences();
        updateAllOptions();
    }

    private void createPreferences() {
        final TelephonyManager tm =
            (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);

        addPreferencesFromResource(R.xml.sim_settings);

        mSimCards = (PreferenceCategory)findPreference(SIM_CARD_CATEGORY);

        final int numSlots = tm.getSimCount();
        mAvailableSubInfos = new ArrayList<SubInfoRecord>(numSlots);
        mNumSims = 0;
        for (int i = 0; i < numSlots; ++i) {
            final SubInfoRecord sir = findRecordBySlotId(i);
            mSimCards.addPreference(new SimPreference(getActivity(), sir, i));
            mAvailableSubInfos.add(sir);
            if (sir != null) {
                mNumSims++;
            }
        }

        updateActivitesCategory();
    }

    private void updateAvailableSubInfos(){
        final TelephonyManager tm =
            (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        final int numSlots = tm.getSimCount();

        mNumSims = 0;
        mAvailableSubInfos = new ArrayList<SubInfoRecord>(numSlots);
        for (int i = 0; i < numSlots; ++i) {
            final SubInfoRecord sir = findRecordBySlotId(i);
            mAvailableSubInfos.add(sir);
            if (sir != null) {
                mNumSims++;
            }
        }
    }

    private void updateAllOptions() {
        updateSimSlotValues();
        updateActivitesCategory();
    }

    private void updateSimSlotValues() {
        SubscriptionManager.getAllSubInfoList();

        final int prefSize = mSimCards.getPreferenceCount();
        for (int i = 0; i < prefSize; ++i) {
            Preference pref = mSimCards.getPreference(i);
            if (pref instanceof SimPreference) {
                ((SimPreference)pref).update();
            }
        }
    }

    private void updateActivitesCategory() {
        updateCellularDataValues();
        updateCallValues();
        updateSmsValues();
    }

    /**
     * finds a record with subId.
     * Since the number of SIMs are few, an array is fine.
     */
    private SubInfoRecord findRecordBySubId(final long subId) {
        final int availableSubInfoLength = mAvailableSubInfos.size();

        for (int i = 0; i < availableSubInfoLength; ++i) {
            final SubInfoRecord sir = mAvailableSubInfos.get(i);
            if (sir != null && sir.subId == subId) {
                return sir;
            }
        }

        return null;
    }

    /**
     * finds a record with slotId.
     * Since the number of SIMs are few, an array is fine.
     */
    private SubInfoRecord findRecordBySlotId(final int slotId) {
        if (mSubInfoList != null){
            final int availableSubInfoLength = mSubInfoList.size();

            for (int i = 0; i < availableSubInfoLength; ++i) {
                final SubInfoRecord sir = mSubInfoList.get(i);
                if (sir.slotId == slotId) {
                    //Right now we take the first subscription on a SIM.
                    return sir;
                }
            }
        }

        return null;
    }

    private void updateSmsValues() {
        final Preference simPref = (Preference) findPreference(KEY_SMS);
        final SubInfoRecord sir = findRecordBySubId(SubscriptionManager.getDefaultSmsSubId());
        simPref.setTitle(R.string.sms_messages_title);
        if (mSubInfoList.size() == 1) {
            simPref.setSummary(mSubInfoList.get(0).displayName);
        } else if (sir != null) {
            simPref.setSummary(sir.displayName);
        } else if (sir == null) {
            simPref.setSummary(R.string.sim_selection_required_pref);
        }
        simPref.setEnabled(mNumSims >= 1);
    }

    private void updateCellularDataValues() {
        final Preference simPref = (Preference) findPreference(KEY_CELLULAR_DATA);
        final SubInfoRecord sir = findRecordBySubId(SubscriptionManager.getDefaultDataSubId());
        simPref.setTitle(R.string.cellular_data_title);
        if (mSubInfoList.size() == 1) {
            simPref.setSummary(mSubInfoList.get(0).displayName);
        } else if (sir != null) {
            simPref.setSummary(sir.displayName);
        } else if (sir == null) {
            simPref.setSummary(R.string.sim_selection_required_pref);
        }
        simPref.setEnabled(mNumSims >= 1);
    }

    private void updateCallValues() {
        final Preference simPref = (Preference) findPreference(KEY_CALLS);
        final SubInfoRecord sir = findRecordBySubId(SubscriptionManager.getDefaultVoiceSubId());
        simPref.setTitle(R.string.calls_title);
        if (mSubInfoList.size() == 1) {
            simPref.setSummary(mSubInfoList.get(0).displayName);
        } else if (sir != null) {
            simPref.setSummary(sir.displayName);
        } else if (sir == null) {
            simPref.setSummary(R.string.sim_calls_ask_first_prefs_title);
        }
        simPref.setEnabled(mNumSims >= 1);
    }

    @Override
    public void onResume() {
        super.onResume();

        mSubInfoList = SubscriptionManager.getActiveSubInfoList();
        updateAvailableSubInfos();
        updateAllOptions();
    }

    @Override
    public boolean onPreferenceTreeClick(final PreferenceScreen preferenceScreen,
            final Preference preference) {
        if (preference instanceof SimPreference) {
            ((SimPreference)preference).createEditDialog((SimPreference)preference);
        } else if ((Preference) findPreference(KEY_CELLULAR_DATA) == preference) {
            showDialog(DATA_PICK);
        } else if ((Preference) findPreference(KEY_CALLS) == preference) {
            showDialog(CALLS_PICK);
        } else if ((Preference) findPreference(KEY_SMS) == preference) {
            showDialog(SMS_PICK);
        }

        return true;
    }

    @Override
    public Dialog onCreateDialog(final int id) {
        final ArrayList<String> list = new ArrayList<String>();
        final int availableSubInfoLength = mAvailableSubInfos.size();

        final DialogInterface.OnClickListener selectionListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int value) {

                        final SubInfoRecord sir;

                        if (id == DATA_PICK) {
                            sir = mAvailableSubInfos.get(value);
                            SubscriptionManager.setDefaultDataSubId(sir.subId);
                        } else if (id == CALLS_PICK) {
                            if (value != 0) {
                                sir = mAvailableSubInfos.get(value -1);
                                SubscriptionManager.setDefaultVoiceSubId(sir.subId);
                            } //have to figure out value of subid when user selects "ask everytime"
                        } else if (id == SMS_PICK) {
                            sir = mAvailableSubInfos.get(value);
                            SubscriptionManager.setDefaultSmsSubId(sir.subId);
                        }

                        updateActivitesCategory();
                    }
                };

        if (id == CALLS_PICK) {
            list.add(getResources().getString(R.string.sim_calls_ask_first_prefs_title));
        }
        for (int i = 0; i < availableSubInfoLength; ++i) {
            final SubInfoRecord sir = mAvailableSubInfos.get(i);
            list.add(sir.displayName);
        }

        String[] arr = new String[availableSubInfoLength];
        arr = list.toArray(arr);

        ArrayAdapter<String> adapter =  new ArrayAdapter<String>(getActivity(),
               android.R.layout.simple_list_item_1, arr);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if (id == DATA_PICK) {
            builder.setTitle(R.string.select_sim_for_data);
        } else if (id == CALLS_PICK) {
            builder.setTitle(R.string.select_sim_for_calls);
        } else if (id == SMS_PICK) {
            builder.setTitle(R.string.sim_card_select_title);
        }

        return builder.setAdapter(adapter, selectionListener)
            .create();
    }

    private void setActivity(Preference preference, SubInfoRecord sir) {
        final String key = preference.getKey();

        if (key.equals(KEY_CELLULAR_DATA)) {
            mCellularData = sir;
        } else if (key.equals(KEY_CALLS)) {
            mCalls = sir;
        } else if (key.equals(KEY_SMS)) {
            mSMS = sir;
        }

        updateActivitesCategory();
    }

    private class SimPreference extends Preference{
        private SubInfoRecord mSubInfoRecord;
        private int mSlotId;

        public SimPreference(Context context, SubInfoRecord subInfoRecord, int slotId) {
            super(context);

            mSubInfoRecord = subInfoRecord;
            mSlotId = slotId;
            setKey("sim" + mSlotId);
            update();
        }

        public void update() {
            final Resources res = getResources();

            if (mSubInfoRecord != null) {
                if(TextUtils.isEmpty(mSubInfoRecord.displayName)) {
                    setTitle(getCarrierName());
                } else {
                    setTitle(mSubInfoRecord.displayName);
                }
                setSummary(mSubInfoRecord.number.toString());
                setEnabled(true);
            } else {
                setSummary(R.string.sim_slot_empty);
                setFragment(null);
                setEnabled(false);
            }
        }

        public String getCarrierName() {
            Uri mUri = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, mSubInfoRecord.subId);
            Cursor mCursor = getActivity().managedQuery(mUri, sProjection, null, null);
            mCursor.moveToFirst();
            return mCursor.getString(1);
        }

        public String getFormattedPhoneNumber() {
            try{
                final String rawNumber = PhoneFactory.getPhone(mSlotId).getLine1Number();
                String formattedNumber = null;
                if (!TextUtils.isEmpty(rawNumber)) {
                    formattedNumber = PhoneNumberUtils.formatNumber(rawNumber);
                }

                return formattedNumber;
            } catch (java.lang.IllegalStateException ise){
                return "Unknown";
            }
        }

        public SubInfoRecord getSubInfoRecord() {
            return mSubInfoRecord;
        }

        public void createEditDialog(SimPreference simPref) {
            final Resources res = getResources();

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            final View dialogLayout = getActivity().getLayoutInflater().inflate(
                    R.layout.multi_sim_dialog, null);
            builder.setView(dialogLayout);

            EditText nameText = (EditText)dialogLayout.findViewById(R.id.sim_name);
            nameText.setText(mSubInfoRecord.displayName);

            TextView numberView = (TextView)dialogLayout.findViewById(R.id.number);
            numberView.setText(simPref.getFormattedPhoneNumber());

            TextView carrierView = (TextView)dialogLayout.findViewById(R.id.carrier);
            carrierView.setText(getCarrierName());

            builder.setTitle(String.format(res.getString(R.string.sim_editor_title),
                    (mSubInfoRecord.slotId + 1)));

            builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    final EditText nameText = (EditText)dialogLayout.findViewById(R.id.sim_name);

                    mSubInfoRecord.displayName = nameText.getText().toString();
                    SubscriptionManager.setDisplayName(mSubInfoRecord.displayName,
                        mSubInfoRecord.subId);

                    updateAllOptions();
                    update();
                }
            });

            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                }
            });

            builder.create().show();
        }
    }

    /**
     * Sort Subscription List in SIM Id, Subscription Id
     * @param context The Context
     * @return Sorted Subscription List or NULL if no activated Subscription
     */
    public static List<SubInfoRecord> getSortedSubInfoList(Context context) {
        List<SubInfoRecord> infoList = SubscriptionManager.getActiveSubInfoList();
        if (infoList != null) {
            Collections.sort(infoList, new Comparator<SubInfoRecord>() {

                @Override
                public int compare(SubInfoRecord arg0, SubInfoRecord arg1) {
                    int flag = arg0.slotId - arg1.slotId;
                    if (flag == 0) {
                        return (int) (arg0.subId - arg1.subId);
                    }
                    return flag;
                }
            });
        }
        return infoList;
    }

    /**
     * For search
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    if (Utils.showSimCardTile(context)) {
                        SearchIndexableResource sir = new SearchIndexableResource(context);
                        sir.xmlResId = R.xml.sim_settings;
                        result.add(sir);
                    }

                    return result;
                }
            };

}
