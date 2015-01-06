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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SimDialogActivity extends Activity {
    private static String TAG = "SimDialogActivity";

    public static String PREFERRED_SIM = "preferred_sim";
    public static String DIALOG_TYPE_KEY = "dialog_type";
    public static final int INVALID_PICK = -1;
    public static final int DATA_PICK = 0;
    public static final int CALLS_PICK = 1;
    public static final int SMS_PICK = 2;
    public static final int PREFERRED_PICK = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle extras = getIntent().getExtras();
        final int dialogType = extras.getInt(DIALOG_TYPE_KEY, INVALID_PICK);

        switch (dialogType) {
            case DATA_PICK:
            case CALLS_PICK:
            case SMS_PICK:
                createDialog(this, dialogType).show();
                break;
            case PREFERRED_PICK:
                displayPreferredDialog(extras.getInt(PREFERRED_SIM));
                break;
            default:
                throw new IllegalArgumentException("Invalid dialog type " + dialogType + " sent.");
        }

    }

    private void displayPreferredDialog(final int slotId) {
        final Resources res = getResources();
        final Context context = getApplicationContext();
        final SubscriptionInfo sir = Utils.findRecordBySlotId(context, slotId);

        if (sir != null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(R.string.sim_preferred_title);
            alertDialogBuilder.setMessage(res.getString(
                        R.string.sim_preferred_message, sir.getDisplayName()));

            alertDialogBuilder.setPositiveButton(R.string.yes, new
                    DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    final int subId = sir.getSubscriptionId();
                    PhoneAccountHandle phoneAccountHandle =
                            subscriptionIdToPhoneAccountHandle(subId);
                    setDefaultDataSubId(context, subId);
                    setDefaultSmsSubId(context, subId);
                    setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
                    finish();
                }
            });
            alertDialogBuilder.setNegativeButton(R.string.no, new
                    DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog,int id) {
                    finish();
                }
            });

            alertDialogBuilder.create().show();
        } else {
            finish();
        }
    }

    private static void setDefaultDataSubId(final Context context, final int subId) {
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        subscriptionManager.setDefaultDataSubId(subId);
        Toast.makeText(context, R.string.data_switch_started, Toast.LENGTH_LONG).show();
    }

    private static void setDefaultSmsSubId(final Context context, final int subId) {
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        subscriptionManager.setDefaultSmsSubId(subId);
    }

    private void setUserSelectedOutgoingPhoneAccount(PhoneAccountHandle phoneAccount) {
        final TelecomManager telecomManager = TelecomManager.from(this);
        telecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccount);
    }

    private PhoneAccountHandle subscriptionIdToPhoneAccountHandle(final int subId) {
        final TelecomManager telecomManager = TelecomManager.from(this);
        final Iterator<PhoneAccountHandle> phoneAccounts =
                telecomManager.getCallCapablePhoneAccounts().listIterator();

        while (phoneAccounts.hasNext()) {
            final PhoneAccountHandle phoneAccountHandle = phoneAccounts.next();
            final PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);
            final String phoneAccountId = phoneAccountHandle.getId();

            if (phoneAccount.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION)
                    && TextUtils.isDigitsOnly(phoneAccountId)
                    && Integer.parseInt(phoneAccountId) == subId){
                return phoneAccountHandle;
            }
        }

        return null;
    }

    public Dialog createDialog(final Context context, final int id) {
        final ArrayList<String> list = new ArrayList<String>();
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        final List<SubscriptionInfo> subInfoList =
            subscriptionManager.getActiveSubscriptionInfoList();
        final int selectableSubInfoLength = subInfoList == null ? 0 : subInfoList.size();

        final DialogInterface.OnClickListener selectionListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int value) {

                        final SubscriptionInfo sir;

                        switch (id) {
                            case DATA_PICK:
                                sir = subInfoList.get(value);
                                setDefaultDataSubId(context, sir.getSubscriptionId());
                                break;
                            case CALLS_PICK:
                                final TelecomManager telecomManager =
                                        TelecomManager.from(context);
                                final List<PhoneAccountHandle> phoneAccountsList =
                                        telecomManager.getCallCapablePhoneAccounts();
                                setUserSelectedOutgoingPhoneAccount(
                                        value < 1 ? null : phoneAccountsList.get(value - 1));
                                break;
                            case SMS_PICK:
                                sir = subInfoList.get(value);
                                setDefaultSmsSubId(context, sir.getSubscriptionId());
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid dialog type "
                                        + id + " in SIM dialog.");
                        }

                        finish();
                    }
                };

        Dialog.OnKeyListener keyListener = new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                    KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        finish();
                    }
                    return true;
                }
            };

        ArrayList<SubscriptionInfo> callsSubInfoList = new ArrayList<SubscriptionInfo>();
        if (id == CALLS_PICK) {
            final TelecomManager telecomManager = TelecomManager.from(context);
            final Iterator<PhoneAccountHandle> phoneAccounts =
                    telecomManager.getCallCapablePhoneAccounts().listIterator();

            list.add(getResources().getString(R.string.sim_calls_ask_first_prefs_title));
            callsSubInfoList.add(null);
            while (phoneAccounts.hasNext()) {
                final PhoneAccount phoneAccount =
                        telecomManager.getPhoneAccount(phoneAccounts.next());
                list.add((String)phoneAccount.getLabel());
                // Added check to add entry into callsSubInforList only if phoneAccountId is int
                // Todo : Might have to change it later based on b/18904714
                if (phoneAccount.hasCapabilities(PhoneAccount.CAPABILITY_SIM_SUBSCRIPTION) &&
                        TextUtils.isDigitsOnly(phoneAccount.getAccountHandle().getId())) {
                    final String phoneAccountId = phoneAccount.getAccountHandle().getId();
                    final SubscriptionInfo sir = Utils.findRecordBySubId(context,
                            Integer.parseInt(phoneAccountId));
                    callsSubInfoList.add(sir);
                } else {
                    callsSubInfoList.add(null);
                }
            }
        } else {
            for (int i = 0; i < selectableSubInfoLength; ++i) {
                final SubscriptionInfo sir = subInfoList.get(i);
                CharSequence displayName = sir.getDisplayName();
                if (displayName == null) {
                    displayName = "";
                }
                list.add(displayName.toString());
            }
        }

        String[] arr = list.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        ListAdapter adapter = new SelectAccountListAdapter(
                id == CALLS_PICK ? callsSubInfoList : subInfoList,
                builder.getContext(),
                R.layout.select_account_list_item,
                arr, id);

        switch (id) {
            case DATA_PICK:
                builder.setTitle(R.string.select_sim_for_data);
                break;
            case CALLS_PICK:
                builder.setTitle(R.string.select_sim_for_calls);
                break;
            case SMS_PICK:
                builder.setTitle(R.string.sim_card_select_title);
                break;
            default:
                throw new IllegalArgumentException("Invalid dialog type "
                        + id + " in SIM dialog.");
        }

        Dialog dialog = builder.setAdapter(adapter, selectionListener).create();
        dialog.setOnKeyListener(keyListener);

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });

        return dialog;

    }

    private class SelectAccountListAdapter extends ArrayAdapter<String> {
        private Context mContext;
        private int mResId;
        private int mDialogId;
        private final float OPACITY = 0.54f;
        private List<SubscriptionInfo> mSubInfoList;

        public SelectAccountListAdapter(List<SubscriptionInfo> subInfoList,
                Context context, int resource, String[] arr, int dialogId) {
            super(context, resource, arr);
            mContext = context;
            mResId = resource;
            mDialogId = dialogId;
            mSubInfoList = subInfoList;
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
                holder.title = (TextView) rowView.findViewById(R.id.title);
                holder.summary = (TextView) rowView.findViewById(R.id.summary);
                holder.icon = (ImageView) rowView.findViewById(R.id.icon);
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }

            final SubscriptionInfo sir = mSubInfoList.get(position);
            if (sir == null) {
                holder.title.setText(getItem(position));
                holder.summary.setText("");
                holder.icon.setImageDrawable(getResources()
                        .getDrawable(R.drawable.ic_live_help));
                holder.icon.setAlpha(OPACITY);
            } else {
                holder.title.setText(sir.getDisplayName());
                holder.summary.setText(sir.getNumber());
                holder.icon.setImageBitmap(sir.createIconBitmap(mContext));
            }
            return rowView;
        }

        private class ViewHolder {
            TextView title;
            TextView summary;
            ImageView icon;
        }
    }
}
