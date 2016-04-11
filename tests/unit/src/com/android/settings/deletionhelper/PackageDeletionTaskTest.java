/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.deletionhelper;

import android.test.AndroidTestCase;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import android.test.mock.MockPackageManager;
import android.test.suitebuilder.annotation.SmallTest;

import com.android.settings.deletionhelper.PackageDeletionTask;
import com.android.settings.deletionhelper.PackageDeletionTask.Callback;

import java.util.Set;
import java.util.HashSet;

public class PackageDeletionTaskTest extends AndroidTestCase {
    private FakePackageManager mPackageManager;
    private Set<String> mDeletedApps;

    @Override
    protected void setUp() throws Exception {
        mPackageManager = new FakePackageManager();
        mDeletedApps = new HashSet<String>();
    }

    @SmallTest
    public void testDeleteNoApps() throws Exception {
        runTask(new HashSet<String>(), false);
    }

    @SmallTest
    public void testDeleteOneApp() throws Exception {
        HashSet<String> appsToDelete = new HashSet<String>();
        appsToDelete.add("app.test1");
        runTask(appsToDelete, false);
    }

    @SmallTest
    public void testDeleteManyApps() throws Exception {
        HashSet<String> appsToDelete = new HashSet<String>();
        appsToDelete.add("app.test1");
        appsToDelete.add("app.test2");
        runTask(appsToDelete, false);
    }

    @SmallTest
    public void testDeleteFails() throws Exception {
        HashSet<String> appsToDelete = new HashSet<String>();
        appsToDelete.add("app.test1");
        mPackageManager.deletionSucceeds = false;
        runTask(appsToDelete, true);
    }

    private void runTask(HashSet<String> appsToDelete, boolean shouldFail) {
        PackageDeletionTask task = new PackageDeletionTask(mPackageManager, appsToDelete,
                new VerifierCallback(appsToDelete, shouldFail));
        task.run();
    }

    class FakePackageManager extends MockPackageManager {
        public boolean deletionSucceeds = true;

        @Override
        public void deletePackageAsUser(String packageName, IPackageDeleteObserver observer,
                                        int flags, int userId) {
            int resultCode;
            if (deletionSucceeds) {
                resultCode = PackageManager.DELETE_SUCCEEDED;
                mDeletedApps.add(packageName);
            } else {
                resultCode = PackageManager.DELETE_FAILED_INTERNAL_ERROR;
            }

            try {
                observer.packageDeleted(packageName, resultCode);
            } catch (RemoteException e) {
                fail(e.toString());
            }
        }
    }

    class VerifierCallback extends Callback {
        private Set<String> mExpectedDeletedApps;
        private boolean mShouldFail;

        public VerifierCallback(HashSet<String> expectedDeletedApps, boolean shouldFail) {
            mExpectedDeletedApps = expectedDeletedApps;
            mShouldFail = shouldFail;
        }

        @Override
        public void onSuccess() {
            System.out.println("lol");
            assertFalse(mShouldFail);
            assertEquals(mExpectedDeletedApps, mDeletedApps);
        }

        @Override
        public void onError() {
            assertTrue(mShouldFail);
        }
    }

}
