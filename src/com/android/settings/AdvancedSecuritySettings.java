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
 * limitations under the License
 */

package com.android.settings;

import com.android.internal.widget.LockPatternUtils;

import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.trust.TrustAgentService;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class AdvancedSecuritySettings extends ListFragment implements View.OnClickListener {
    static final String TAG = "AdvancedSecuritySettings";

    private static final String SERVICE_INTERFACE = TrustAgentService.SERVICE_INTERFACE;

    private final ArraySet<ComponentName> mActiveAgents = new ArraySet<ComponentName>();
    private final ArrayMap<ComponentName, AgentInfo> mAvailableAgents
            = new ArrayMap<ComponentName, AgentInfo>();

    private LockPatternUtils mLockPatternUtils;

    public static final class AgentInfo {
        CharSequence label;
        Drawable icon;
        ComponentName component; // service that implements ITrustAgent
        ComponentName settings; // setting to launch to modify agent.

        @Override
        public boolean equals(Object other) {
            if (other instanceof AgentInfo) {
                return component.equals(((AgentInfo)other).component);
            }
            return true;
        }

        public int compareTo(AgentInfo other) {
            return component.compareTo(other.component);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
	if (mLockPatternUtils == null) {
            mLockPatternUtils = new LockPatternUtils(
                    container.getContext().getApplicationContext());
        }
        setListAdapter(new AgentListAdapter());
        return inflater.inflate(R.layout.advanced_security_settings, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateList();
    }

    void updateList() {
        Context context = getActivity();
        if (context == null) {
            return;
        }

        loadActiveAgents();

        PackageManager pm = getActivity().getPackageManager();
        Intent trustAgentIntent = new Intent(SERVICE_INTERFACE);
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(trustAgentIntent,
                PackageManager.GET_META_DATA);

        mAvailableAgents.clear();
        mAvailableAgents.ensureCapacity(resolveInfos.size());

        for (ResolveInfo resolveInfo : resolveInfos) {
            if (resolveInfo.serviceInfo == null) continue;
            ComponentName name = TrustAgentUtils.getComponentName(resolveInfo);
            if (!mAvailableAgents.containsKey(name)) {
                AgentInfo agentInfo = new AgentInfo();
                agentInfo.label = resolveInfo.loadLabel(pm);
                agentInfo.icon = resolveInfo.loadIcon(pm);
                agentInfo.component = name;
                TrustAgentUtils.TrustAgentComponentInfo trustAgentComponentInfo =
                        TrustAgentUtils.getSettingsComponent(pm, resolveInfo);
                agentInfo.settings = trustAgentComponentInfo.componentName;
                mAvailableAgents.put(name, agentInfo);
            }
        }
        ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
    }

    @Override
    public void onClick(View view) {
        ViewHolder h = (ViewHolder) view.getTag();
        AgentInfo agentInfo = h.agentInfo;

        if (view.getId() == R.id.settings) {
            if (agentInfo.settings != null) {
                Intent intent = new Intent();
                intent.setComponent(agentInfo.settings);
                intent.setAction("TODO");
                startActivity(intent);
            }
        } else if (view.getId() == R.id.clickable) {
            boolean wasActive = mActiveAgents.contains(h.agentInfo.component);
            loadActiveAgents();
            if (!wasActive) {
                mActiveAgents.add(h.agentInfo.component);
            } else {
                mActiveAgents.remove(h.agentInfo.component);
            }
            saveActiveAgents();
            ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
        }
    }

    private void loadActiveAgents() {
        mActiveAgents.clear();
        List<ComponentName> activeTrustAgents = mLockPatternUtils.getEnabledTrustAgents();
        if (activeTrustAgents != null) {
            mActiveAgents.addAll(activeTrustAgents);
        }
    }

    private void saveActiveAgents() {
        mLockPatternUtils.setEnabledTrustAgents(mActiveAgents);
    }

    static class ViewHolder {
        ImageView icon;
        TextView name;
        CheckBox checkbox;
        TextView description;
        AgentInfo agentInfo;
        View clickable;
        View settings;
    }

    class AgentListAdapter extends BaseAdapter {
        final LayoutInflater mInflater;

        AgentListAdapter() {
            mInflater = (LayoutInflater)
                    getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public boolean hasStableIds() {
            return false;
        }

        public int getCount() {
            return mAvailableAgents.size();
        }

        public Object getItem(int position) {
            return mAvailableAgents.valueAt(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public boolean areAllItemsEnabled() {
            return false;
        }

        public boolean isEnabled(int position) {
            return true;
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
            View v = mInflater.inflate(R.layout.trust_agent_item, parent, false);
            ViewHolder h = new ViewHolder();
            h.icon = (ImageView)v.findViewById(R.id.icon);
            h.name = (TextView)v.findViewById(R.id.name);
            h.checkbox = (CheckBox)v.findViewById(R.id.checkbox);
            h.clickable = v.findViewById(R.id.clickable);
            h.clickable.setOnClickListener(AdvancedSecuritySettings.this);
            h.description = (TextView)v.findViewById(R.id.description);
            h.settings = v.findViewById(R.id.settings);
            h.settings.setOnClickListener(AdvancedSecuritySettings.this);
            v.setTag(h);
            h.settings.setTag(h);
            h.clickable.setTag(h);
            return v;
        }

        public void bindView(View view, int position) {
            ViewHolder vh = (ViewHolder) view.getTag();
            AgentInfo item = mAvailableAgents.valueAt(position);
            vh.name.setText(item.label);
            vh.checkbox.setChecked(mActiveAgents.contains(item.component));
            vh.agentInfo = item;
            vh.settings.setVisibility(item.settings != null ? View.VISIBLE : View.INVISIBLE);
        }
    }
}
