/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.testutils;

import android.os.IBinder;
import android.os.ServiceManager;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

// This class is for replacing existing system service with the mocked service.
// Copied from CellBroadcastReceiver app.
public final class MockedServiceManager {

    private final String TAG = MockedServiceManager.class.getSimpleName();

    private final HashMap<String, IBinder> mServiceManagerMockedServices = new HashMap<>();

    private final HashMap<InstanceKey, Object> mOldInstances = new HashMap<>();

    private final LinkedList<InstanceKey> mInstanceKeys = new LinkedList<>();

    private static class InstanceKey {
        final Class mClass;
        final String mInstName;
        final Object mObj;

        InstanceKey(final Class c, final String instName, final Object obj) {
            mClass = c;
            mInstName = instName;
            mObj = obj;
        }

        @Override
        public int hashCode() {
            return (mClass.getName().hashCode() * 31 + mInstName.hashCode()) * 31;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != getClass()) {
                return false;
            }

            InstanceKey other = (InstanceKey) obj;
            return (other.mClass == mClass && other.mInstName.equals(mInstName)
                    && other.mObj == mObj);
        }
    }

    public MockedServiceManager() throws Exception {
        replaceInstance(ServiceManager.class, "sCache", null, mServiceManagerMockedServices);
    }

    public void replaceService(String key, IBinder binder) {
        mServiceManagerMockedServices.put(key, binder);
    }

    public void restoreAllServices() throws Exception {
        restoreInstances();
    }

    public synchronized void replaceInstance(final Class c, final String instanceName,
            final Object obj, final Object newValue)
            throws Exception {
        Field field = c.getDeclaredField(instanceName);
        field.setAccessible(true);

        InstanceKey key = new InstanceKey(c, instanceName, obj);
        if (!mOldInstances.containsKey(key)) {
            mOldInstances.put(key, field.get(obj));
            mInstanceKeys.add(key);
        }
        field.set(obj, newValue);
    }

    public synchronized void restoreInstances() throws Exception {
        Iterator<InstanceKey> it = mInstanceKeys.descendingIterator();

        while (it.hasNext()) {
            InstanceKey key = it.next();
            Field field = key.mClass.getDeclaredField(key.mInstName);
            field.setAccessible(true);
            field.set(key.mObj, mOldInstances.get(key));
        }

        mInstanceKeys.clear();
        mOldInstances.clear();
    }
}
