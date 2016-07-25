/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings.dashboard.conditional;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ConditionManager {

    private static final String TAG = "ConditionManager";

    private static final boolean DEBUG = false;

    private static final String PKG = "com.android.settings.dashboard.conditional.";

    private static final String FILE_NAME = "condition_state.xml";
    private static final String TAG_CONDITIONS = "cs";
    private static final String TAG_CONDITION = "c";
    private static final String ATTR_CLASS = "cls";

    private static ConditionManager sInstance;

    private final Context mContext;
    private final ArrayList<Condition> mConditions;
    private File mXmlFile;

    private final ArrayList<ConditionListener> mListeners = new ArrayList<>();

    private ConditionManager(Context context, boolean loadConditionsNow) {
        mContext = context;
        mConditions = new ArrayList<>();
        if (loadConditionsNow) {
            ConditionLoader loader = new ConditionLoader();
            loader.onPostExecute(loader.doInBackground());
        } else {
            new ConditionLoader().execute();
        }
    }

    public void refreshAll() {
        final int N = mConditions.size();
        for (int i = 0; i < N; i++) {
            mConditions.get(i).refreshState();
        }
    }

    private void readFromXml(File xmlFile, ArrayList<Condition> conditions) {
        if (DEBUG) Log.d(TAG, "Reading from " + xmlFile.toString());
        try {
            XmlPullParser parser = Xml.newPullParser();
            FileReader in = new FileReader(xmlFile);
            parser.setInput(in);
            int state = parser.getEventType();

            while (state != XmlPullParser.END_DOCUMENT) {
                if (TAG_CONDITION.equals(parser.getName())) {
                    int depth = parser.getDepth();
                    String clz = parser.getAttributeValue("", ATTR_CLASS);
                    if (!clz.startsWith(PKG)) {
                        clz = PKG + clz;
                    }
                    Condition condition = createCondition(Class.forName(clz));
                    PersistableBundle bundle = PersistableBundle.restoreFromXml(parser);
                    if (DEBUG) Log.d(TAG, "Reading " + clz + " -- " + bundle);
                    condition.restoreState(bundle);
                    conditions.add(condition);
                    while (parser.getDepth() > depth) {
                        parser.next();
                    }
                }
                state = parser.next();
            }
            in.close();
        } catch (XmlPullParserException | IOException | ClassNotFoundException e) {
            Log.w(TAG, "Problem reading " + FILE_NAME, e);
        }
    }

    private void saveToXml() {
        if (DEBUG) Log.d(TAG, "Writing to " + mXmlFile.toString());
        try {
            XmlSerializer serializer = Xml.newSerializer();
            FileWriter writer = new FileWriter(mXmlFile);
            serializer.setOutput(writer);

            serializer.startDocument("UTF-8", true);
            serializer.startTag("", TAG_CONDITIONS);

            final int N = mConditions.size();
            for (int i = 0; i < N; i++) {
                PersistableBundle bundle = new PersistableBundle();
                if (mConditions.get(i).saveState(bundle)) {
                    serializer.startTag("", TAG_CONDITION);
                    final String clz = mConditions.get(i).getClass().getSimpleName();
                    serializer.attribute("", ATTR_CLASS, clz);
                    bundle.saveToXml(serializer);
                    serializer.endTag("", TAG_CONDITION);
                }
            }

            serializer.endTag("", TAG_CONDITIONS);
            serializer.flush();
            writer.close();
        } catch (XmlPullParserException | IOException e) {
            Log.w(TAG, "Problem writing " + FILE_NAME, e);
        }
    }

    private void addMissingConditions(ArrayList<Condition> conditions) {
        addIfMissing(AirplaneModeCondition.class, conditions);
        addIfMissing(HotspotCondition.class, conditions);
        addIfMissing(DndCondition.class, conditions);
        addIfMissing(BatterySaverCondition.class, conditions);
        addIfMissing(CellularDataCondition.class, conditions);
        addIfMissing(BackgroundDataCondition.class, conditions);
        addIfMissing(WorkModeCondition.class, conditions);
        addIfMissing(NightDisplayCondition.class, conditions);
        Collections.sort(conditions, CONDITION_COMPARATOR);
    }

    private void addIfMissing(Class<? extends Condition> clz, ArrayList<Condition> conditions) {
        if (getCondition(clz, conditions) == null) {
            if (DEBUG) Log.d(TAG, "Adding missing " + clz.getName());
            conditions.add(createCondition(clz));
        }
    }

    private Condition createCondition(Class<?> clz) {
        if (AirplaneModeCondition.class == clz) {
            return new AirplaneModeCondition(this);
        } else if (HotspotCondition.class == clz) {
            return new HotspotCondition(this);
        } else if (DndCondition.class == clz) {
            return new DndCondition(this);
        } else if (BatterySaverCondition.class == clz) {
            return new BatterySaverCondition(this);
        } else if (CellularDataCondition.class == clz) {
            return new CellularDataCondition(this);
        } else if (BackgroundDataCondition.class == clz) {
            return new BackgroundDataCondition(this);
        } else if (WorkModeCondition.class == clz) {
            return new WorkModeCondition(this);
        } else if (NightDisplayCondition.class == clz) {
            return new NightDisplayCondition(this);
        }
        throw new RuntimeException("Unexpected Condition " + clz);
    }

    Context getContext() {
        return mContext;
    }

    public <T extends Condition> T getCondition(Class<T> clz) {
        return getCondition(clz, mConditions);
    }

    private <T extends Condition> T getCondition(Class<T> clz, List<Condition> conditions) {
        final int N = conditions.size();
        for (int i = 0; i < N; i++) {
            if (clz.equals(conditions.get(i).getClass())) {
                return (T) conditions.get(i);
            }
        }
        return null;
    }

    public List<Condition> getConditions() {
        return mConditions;
    }

    public List<Condition> getVisibleConditions() {
        List<Condition> conditions = new ArrayList<>();
        final int N = mConditions.size();
        for (int i = 0; i < N; i++) {
            if (mConditions.get(i).shouldShow()) {
                conditions.add(mConditions.get(i));
            }
        }
        return conditions;
    }

    public void notifyChanged(Condition condition) {
        saveToXml();
        Collections.sort(mConditions, CONDITION_COMPARATOR);
        final int N = mListeners.size();
        for (int i = 0; i < N; i++) {
            mListeners.get(i).onConditionsChanged();
        }
    }

    public void addListener(ConditionListener listener) {
        mListeners.add(listener);
        listener.onConditionsChanged();
    }

    public void remListener(ConditionListener listener) {
        mListeners.remove(listener);
    }

    private class ConditionLoader extends AsyncTask<Void, Void, ArrayList<Condition>> {
        @Override
        protected ArrayList<Condition> doInBackground(Void... params) {
            ArrayList<Condition> conditions = new ArrayList<>();
            mXmlFile = new File(mContext.getFilesDir(), FILE_NAME);
            if (mXmlFile.exists()) {
                readFromXml(mXmlFile, conditions);
            }
            addMissingConditions(conditions);
            return conditions;
        }

        @Override
        protected void onPostExecute(ArrayList<Condition> conditions) {
            mConditions.clear();
            mConditions.addAll(conditions);
            refreshAll();
        }
    }

    public static ConditionManager get(Context context) {
        return get(context, true);
    }

    public static ConditionManager get(Context context, boolean loadConditionsNow) {
        if (sInstance == null) {
            sInstance = new ConditionManager(context.getApplicationContext(), loadConditionsNow);
        }
        return sInstance;
    }

    public interface ConditionListener {
        void onConditionsChanged();
    }

    private static final Comparator<Condition> CONDITION_COMPARATOR = new Comparator<Condition>() {
        @Override
        public int compare(Condition lhs, Condition rhs) {
            return Long.compare(lhs.getLastChange(), rhs.getLastChange());
        }
    };
}
