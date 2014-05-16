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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.app.ListFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.trust.TrustAgentService;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.Slog;
import android.util.Xml;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

public class TrustAgentSettings extends ListFragment implements View.OnClickListener {
    static final String TAG = "TrustAgentSettings";

    private static final String SERVICE_INTERFACE = TrustAgentService.SERVICE_INTERFACE;
    private static final String TRUST_AGENT_META_DATA = TrustAgentService.TRUST_AGENT_META_DATA;


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
        return inflater.inflate(R.layout.trust_agent_settings, container, false);
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
            ComponentName name = getComponentName(resolveInfo);
            if (!mAvailableAgents.containsKey(name)) {
                AgentInfo agentInfo = new AgentInfo();
                agentInfo.label = resolveInfo.loadLabel(pm);
                agentInfo.icon = resolveInfo.loadIcon(pm);
                agentInfo.component = name;
                agentInfo.settings = getSettingsComponentName(pm, resolveInfo);
                mAvailableAgents.put(name, agentInfo);
            }
        }
        ((BaseAdapter) getListAdapter()).notifyDataSetChanged();
    }

    private ComponentName getComponentName(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null) return null;
        return new ComponentName(resolveInfo.serviceInfo.packageName, resolveInfo.serviceInfo.name);
    }

    private ComponentName getSettingsComponentName(PackageManager pm, ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null
                || resolveInfo.serviceInfo.metaData == null) return null;
        String cn = null;
        XmlResourceParser parser = null;
        Exception caughtException = null;
        try {
            parser = resolveInfo.serviceInfo.loadXmlMetaData(pm, TRUST_AGENT_META_DATA);
            if (parser == null) {
                Slog.w(TAG, "Can't find " + TRUST_AGENT_META_DATA + " meta-data");
                return null;
            }
            Resources res = pm.getResourcesForApplication(resolveInfo.serviceInfo.applicationInfo);
            AttributeSet attrs = Xml.asAttributeSet(parser);
            int type;
            while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                    && type != XmlPullParser.START_TAG) {
            }
            String nodeName = parser.getName();
            if (!"trust-agent".equals(nodeName)) {
                Slog.w(TAG, "Meta-data does not start with trust-agent tag");
                return null;
            }
            TypedArray sa = res
                    .obtainAttributes(attrs, com.android.internal.R.styleable.TrustAgent);
            cn = sa.getString(com.android.internal.R.styleable.TrustAgent_settingsActivity);
            sa.recycle();
        } catch (PackageManager.NameNotFoundException e) {
            caughtException = e;
        } catch (IOException e) {
            caughtException = e;
        } catch (XmlPullParserException e) {
            caughtException = e;
        } finally {
            if (parser != null) parser.close();
        }
        if (caughtException != null) {
            Slog.w(TAG, "Error parsing : " + resolveInfo.serviceInfo.packageName, caughtException);
            return null;
        }
        if (cn != null && cn.indexOf('/') < 0) {
            cn = resolveInfo.serviceInfo.packageName + "/" + cn;
        }
        return cn == null ? null : ComponentName.unflattenFromString(cn);
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
            h.clickable.setOnClickListener(TrustAgentSettings.this);
            h.description = (TextView)v.findViewById(R.id.description);
            h.settings = v.findViewById(R.id.settings);
            h.settings.setOnClickListener(TrustAgentSettings.this);
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
