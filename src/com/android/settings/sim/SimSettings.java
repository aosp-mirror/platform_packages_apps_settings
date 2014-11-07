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

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.SearchIndexableResource;
import android.provider.Telephony;
import android.telephony.SubInfoRecord;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.PhoneNumberUtils;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.app.Dialog;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;

import com.android.internal.telephony.PhoneFactory;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
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
     * mSelectableSubInfos is the list of SubInfos that a user can select for data, calls, and SMS.
     */
    private List<SubInfoRecord> mAvailableSubInfos = null;
    private List<SubInfoRecord> mSubInfoList = null;
    private List<SubInfoRecord> mSelectableSubInfos = null;

    private SubInfoRecord mCellularData = null;
    private SubInfoRecord mCalls = null;
    private SubInfoRecord mSMS = null;

    private PreferenceCategory mSimCards = null;

    private int mNumSims;

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
        mSelectableSubInfos = new ArrayList<SubInfoRecord>();
        mNumSims = 0;
        for (int i = 0; i < numSlots; ++i) {
            final SubInfoRecord sir = Utils.findRecordBySlotId(i);
            mSimCards.addPreference(new SimPreference(getActivity(), sir, i));
            mAvailableSubInfos.add(sir);
            if (sir != null) {
                mNumSims++;
                mSelectableSubInfos.add(sir);
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
            final SubInfoRecord sir = Utils.findRecordBySlotId(i);
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

    private void updateSmsValues() {
        final Preference simPref = (Preference) findPreference(KEY_SMS);
        final SubInfoRecord sir = Utils.findRecordBySubId(SubscriptionManager.getDefaultSmsSubId());
        simPref.setTitle(R.string.sms_messages_title);
        if (mSubInfoList.size() == 1) {
            simPref.setSummary(mSubInfoList.get(0).getDisplayName());
        } else if (sir != null) {
            simPref.setSummary(sir.getDisplayName());
        } else if (sir == null) {
            simPref.setSummary(R.string.sim_selection_required_pref);
        }
        simPref.setEnabled(mNumSims >= 1);
    }

    private void updateCellularDataValues() {
        final Preference simPref = findPreference(KEY_CELLULAR_DATA);
        final SubInfoRecord sir = Utils.findRecordBySubId(SubscriptionManager.getDefaultDataSubId());
        simPref.setTitle(R.string.cellular_data_title);
        if (mSubInfoList.size() == 1) {
            simPref.setSummary(mSubInfoList.get(0).getDisplayName());
        } else if (sir != null) {
            simPref.setSummary(sir.getDisplayName());
        } else if (sir == null) {
            simPref.setSummary(R.string.sim_selection_required_pref);
        }
        simPref.setEnabled(mNumSims >= 1);
    }

    private void updateCallValues() {
        final Preference simPref = findPreference(KEY_CALLS);
        final TelecomManager telecomManager = TelecomManager.from(getActivity());
        final PhoneAccountHandle phoneAccount =
            telecomManager.getUserSelectedOutgoingPhoneAccount();

        simPref.setTitle(R.string.calls_title);
        simPref.setSummary(phoneAccount == null
                ? getResources().getString(R.string.sim_selection_required_pref)
                : (String)telecomManager.getPhoneAccount(phoneAccount).getLabel());
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
        final int selectableSubInfoLength = mSelectableSubInfos.size();

        final DialogInterface.OnClickListener selectionListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int value) {

                        final SubInfoRecord sir;

                        if (id == DATA_PICK) {
                            sir = mSelectableSubInfos.get(value);
                            SubscriptionManager.setDefaultDataSubId(sir.getSubscriptionId());
                        } else if (id == CALLS_PICK) {
                            final TelecomManager telecomManager =
                                    TelecomManager.from(getActivity());
                            final List<PhoneAccountHandle> phoneAccountsList =
                                    telecomManager.getCallCapablePhoneAccounts();
                            telecomManager.setUserSelectedOutgoingPhoneAccount(
                                    value < 1 ? null : phoneAccountsList.get(value - 1));
                        } else if (id == SMS_PICK) {
                            sir = mSelectableSubInfos.get(value);
                            SubscriptionManager.setDefaultSmsSubId(sir.getSubscriptionId());
                        }

                        updateActivitesCategory();
                    }
                };

        if (id == CALLS_PICK) {
            final TelecomManager telecomManager = TelecomManager.from(getActivity());
            final Iterator<PhoneAccountHandle> phoneAccounts =
                    telecomManager.getCallCapablePhoneAccounts().listIterator();

            list.add(getResources().getString(R.string.sim_calls_ask_first_prefs_title));
            while (phoneAccounts.hasNext()) {
                final PhoneAccount phoneAccount =
                        telecomManager.getPhoneAccount(phoneAccounts.next());
                list.add((String)phoneAccount.getLabel());
            }
        } else {
            for (int i = 0; i < selectableSubInfoLength; ++i) {
                final SubInfoRecord sir = mSelectableSubInfos.get(i);
                CharSequence displayName = sir.getDisplayName();
                if (displayName == null) {
                    displayName = "";
                }
                list.add(displayName.toString());
            }
        }

        String[] arr = new String[selectableSubInfoLength];
        arr = list.toArray(arr);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        ListAdapter adapter = new SelectAccountListAdapter(
                builder.getContext(),
                R.layout.select_account_list_item,
                arr);

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

    private class SelectAccountListAdapter extends ArrayAdapter<String> {
        private Context mContext;
        private int mResId;

        public SelectAccountListAdapter(
                Context context, int resource, String[] arr) {
            super(context, resource, arr);
            mContext = context;
            mResId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView;
            final ViewHolder holder;

            if (convertView == null) {
                // Cache views for faster scrolling
                rowView = inflater.inflate(mResId, null);
                holder = new ViewHolder();
                holder.textView = (TextView) rowView.findViewById(R.id.text);
                holder.imageView = (ImageView) rowView.findViewById(R.id.icon);
                rowView.setTag(holder);
            }
            else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }

            holder.textView.setText(getItem(position));
            holder.imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_sim_sd));
            return rowView;
        }

        private class ViewHolder {
            TextView textView;
            ImageView imageView;
        }
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
        private int[] colorArr;

        public SimPreference(Context context, SubInfoRecord subInfoRecord, int slotId) {
            super(context);

            mSubInfoRecord = subInfoRecord;
            mSlotId = slotId;
            setKey("sim" + mSlotId);
            update();
            colorArr = context.getResources().getIntArray(com.android.internal.R.array.sim_colors);
        }

        public void update() {
            final Resources res = getResources();

            if (mSubInfoRecord != null) {
                setTitle(String.format(res.getString(R.string.sim_editor_title),
                        (mSubInfoRecord.getSimSlotIndex() + 1)));
                setSummary(mSubInfoRecord.getDisplayName() + " - " +
                        mSubInfoRecord.getNumber().toString());
                setEnabled(true);
            } else {
                setSummary(R.string.sim_slot_empty);
                setFragment(null);
                setEnabled(false);
            }
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
            nameText.setText(mSubInfoRecord.getDisplayName());

            final Spinner colorSpinner = (Spinner) dialogLayout.findViewById(R.id.spinner);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.color_picker, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            colorSpinner.setAdapter(adapter);

            for (int i = 0; i < colorArr.length; i++) {
                if (colorArr[i] == mSubInfoRecord.getColor()) {
                    colorSpinner.setSelection(i);
                    break;
                }
            }

            colorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view,
                    int pos, long id){
                    colorSpinner.setSelection(pos);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            TextView numberView = (TextView)dialogLayout.findViewById(R.id.number);
            numberView.setText(simPref.getFormattedPhoneNumber());

            TextView carrierView = (TextView)dialogLayout.findViewById(R.id.carrier);
            carrierView.setText(mSubInfoRecord.getCarrierName());

            builder.setTitle(String.format(res.getString(R.string.sim_editor_title),
                    (mSubInfoRecord.getSimSlotIndex() + 1)));

            builder.setPositiveButton(R.string.okay, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                    final EditText nameText = (EditText)dialogLayout.findViewById(R.id.sim_name);

                    String displayName = nameText.getText().toString();
                    int subId = mSubInfoRecord.getSubscriptionId();
                    mSubInfoRecord.setDisplayName(displayName);
                    SubscriptionManager.setDisplayName(displayName, subId,
                            SubscriptionManager.NAME_SOURCE_USER_INPUT);
                    Utils.findRecordBySubId(subId).setDisplayName(displayName);

                    final int colorSelected = colorSpinner.getSelectedItemPosition();
                    int subscriptionId = mSubInfoRecord.getSubscriptionId();
                    int color = colorArr[colorSelected];
                    mSubInfoRecord.setColor(color);
                    SubscriptionManager.setColor(color, subscriptionId);
                    Utils.findRecordBySubId(subscriptionId).setColor(color);

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
                    int flag = arg0.getSimSlotIndex() - arg1.getSimSlotIndex();
                    if (flag == 0) {
                        return (int) (arg0.getSubscriptionId() - arg1.getSubscriptionId());
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
