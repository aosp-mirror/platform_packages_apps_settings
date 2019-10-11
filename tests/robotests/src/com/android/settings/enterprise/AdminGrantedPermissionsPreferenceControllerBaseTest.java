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

package com.android.settings.enterprise;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AdminGrantedPermissionsPreferenceControllerBaseTest
    extends AdminGrantedPermissionsPreferenceControllerTestBase {

    public AdminGrantedPermissionsPreferenceControllerBaseTest() {
        super("some.key", new String[] {"some.permission"});
    }

    @Override
    protected AdminGrantedPermissionsPreferenceControllerBase createController(boolean async) {
        return new AdminGrantedPermissionsPreferenceControllerBaseTestable(async);
    }

    private class AdminGrantedPermissionsPreferenceControllerBaseTestable extends
            AdminGrantedPermissionsPreferenceControllerBase {

        AdminGrantedPermissionsPreferenceControllerBaseTestable(boolean async) {
            super(AdminGrantedPermissionsPreferenceControllerBaseTest.this.mContext,
                    async, mPermissions);
        }

        @Override
        public String getPreferenceKey() {
            return "some.key";
        }
    }
}
