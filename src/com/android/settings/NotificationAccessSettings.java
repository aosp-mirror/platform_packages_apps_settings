/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.pm.PackageItemInfo;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.service.notification.NotificationListenerService;
import android.util.Slog;
import android.widget.ArrayAdapter;

import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;

public class NotificationAccessSettings extends ListFragment {
    static final String TAG = NotificationAccessSettings.class.getSimpleName();
    private static final boolean SHOW_PACKAGE_NAME = false;

    private PackageManager mPM;
    private ContentResolver mCR;

    private final HashSet<ComponentName> mEnabledListeners = new HashSet<ComponentName>();
    private ListenerListAdapter mList;

    private final Uri ENABLED_NOTIFICATION_LISTENERS_URI
            = Settings.Secure.getUriFor(Settings.Secure.ENABLED_NOTIFICATION_LISTENERS);

    private final ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateList();
        }
    };

    private final BroadcastReceiver mPackageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateList();
        }
    };

    public class ListenerWarningDialogFragment extends DialogFragment {
        static final String KEY_COMPONENT = "c";
        static final String KEY_LABEL = "l";

        public ListenerWarningDialogFragment setListenerInfo(ComponentName cn, String label) {
            Bundle args = new Bundle();
            args.putString(KEY_COMPONENT, cn.flattenToString());
            args.putString(KEY_LABEL, label);
            setArguments(args);

            return this;
        }
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final Bundle args = getArguments();
            final String label = args.getString(KEY_LABEL);
            final ComponentName cn = ComponentName.unflattenFromString(args.getString(KEY_COMPONENT));

            final String title = getResources().getString(
                    R.string.notification_listener_security_warning_title, label);
            final String summary = getResources().getString(
                    R.string.notification_listener_security_warning_summary, label);
            return new AlertDialog.Builder(getActivity())
                    .setMessage(summary)
                    .setTitle(title)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    mEnabledListeners.add(cn);
                                    saveEnabledListeners();
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    // pass
                                }
                            })
                    .create();
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mPM = getActivity().getPackageManager();
        mCR = getActivity().getContentResolver();
        mList = new ListenerListAdapter(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.notification_access_settings, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateList();

        // listen for package changes
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        getActivity().registerReceiver(mPackageReceiver, filter);

        mCR.registerContentObserver(ENABLED_NOTIFICATION_LISTENERS_URI, false, mSettingsObserver);
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mPackageReceiver);
        mCR.unregisterContentObserver(mSettingsObserver);
    }

    void loadEnabledListeners() {
        mEnabledListeners.clear();
        final String flat = Settings.Secure.getString(mCR,
                Settings.Secure.ENABLED_NOTIFICATION_LISTENERS);
        if (flat != null && !"".equals(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    mEnabledListeners.add(cn);
                }
            }
        }
    }

    void saveEnabledListeners() {
        StringBuilder sb = null;
        for (ComponentName cn : mEnabledListeners) {
            if (sb == null) {
                sb = new StringBuilder();
            } else {
                sb.append(':');
            }
            sb.append(cn.flattenToString());
        }
        Settings.Secure.putString(mCR,
                Settings.Secure.ENABLED_NOTIFICATION_LISTENERS,
                sb != null ? sb.toString() : "");
    }

    void updateList() {
        loadEnabledListeners();

        getListeners(mList, mPM);
        mList.sort(new PackageItemInfo.DisplayNameComparator(mPM));

        getListView().setAdapter(mList);
    }

    static int getListenersCount(PackageManager pm) {
        return getListeners(null, pm);
    }

    private static int getListeners(ArrayAdapter<ServiceInfo> adapter, PackageManager pm) {
        int listeners = 0;
        if (adapter != null) {
            adapter.clear();
        }
        final int user = ActivityManager.getCurrentUser();

        List<ResolveInfo> installedServices = pm.queryIntentServicesAsUser(
                new Intent(NotificationListenerService.SERVICE_INTERFACE),
                PackageManager.GET_SERVICES | PackageManager.GET_META_DATA,
                user);

        for (int i = 0, count = installedServices.size(); i < count; i++) {
            ResolveInfo resolveInfo = installedServices.get(i);
            ServiceInfo info = resolveInfo.serviceInfo;

            if (!android.Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE.equals(
                    info.permission)) {
                Slog.w(TAG, "Skipping notification listener service "
                        + info.packageName + "/" + info.name
                        + ": it does not require the permission "
                        + android.Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE);
                continue;
            }
            if (adapter != null) {
                adapter.add(info);
            }
            listeners++;
        }
        return listeners;
    }

    boolean isListenerEnabled(ServiceInfo info) {
        final ComponentName cn = new ComponentName(info.packageName, info.name);
        return mEnabledListeners.contains(cn);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        ServiceInfo info = mList.getItem(position);
        final ComponentName cn = new ComponentName(info.packageName, info.name);
        if (mEnabledListeners.contains(cn)) {
            // the simple version: disabling
            mEnabledListeners.remove(cn);
            saveEnabledListeners();
        } else {
            // show a scary dialog
            new ListenerWarningDialogFragment()
                .setListenerInfo(cn, info.loadLabel(mPM).toString())
                .show(getFragmentManager(), "dialog");
        }
    }

    static class ViewHolder {
        ImageView icon;
        TextView name;
        CheckBox checkbox;
        TextView description;
    }

    class ListenerListAdapter extends ArrayAdapter<ServiceInfo> {
        final LayoutInflater mInflater;

        ListenerListAdapter(Context context) {
            super(context, 0, 0);
            mInflater = (LayoutInflater)
                    getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public boolean hasStableIds() {
            return true;
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v;
            if (convertView == null) {
                v = newView(parent);
            } else {
                v = convertView;
            }
            bindView(v, position);
            return v;
        }

        public View newView(ViewGroup parent) {
            View v = mInflater.inflate(R.layout.notification_listener_item, parent, false);
            ViewHolder h = new ViewHolder();
            h.icon = (ImageView) v.findViewById(R.id.icon);
            h.name = (TextView) v.findViewById(R.id.name);
            h.checkbox = (CheckBox) v.findViewById(R.id.checkbox);
            h.description = (TextView) v.findViewById(R.id.description);
            v.setTag(h);
            return v;
        }

        public void bindView(View view, int position) {
            ViewHolder vh = (ViewHolder) view.getTag();
            ServiceInfo info = getItem(position);

            vh.icon.setImageDrawable(info.loadIcon(mPM));
            vh.name.setText(info.loadLabel(mPM));
            if (SHOW_PACKAGE_NAME) {
                vh.description.setText(info.packageName);
                vh.description.setVisibility(View.VISIBLE);
            } else {
                vh.description.setVisibility(View.GONE);
            }
            vh.checkbox.setChecked(isListenerEnabled(info));
        }
    }
}
