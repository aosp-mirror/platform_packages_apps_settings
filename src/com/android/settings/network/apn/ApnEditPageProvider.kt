/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.network.apn

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.android.settings.R
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.widget.scaffold.RegularScaffold
import java.util.Base64

const val URI_TYPE = "uriType"
const val URI = "uri"
const val SUB_ID = "subId"
const val MVNO_TYPE = "mvnoType"
const val MVNO_MATCH_DATA = "mvnoMatchData"
const val EDIT_URL = "editUrl"

object ApnEditPageProvider : SettingsPageProvider {

    override val name = "Apn"
    const val TAG = "ApnPageProvider"

    override val parameter = listOf(
        navArgument(URI_TYPE) { type = NavType.StringType },
        navArgument(URI) { type = NavType.StringType },
        navArgument(SUB_ID) { type = NavType.IntType },
        navArgument(MVNO_TYPE) { type = NavType.StringType },
        navArgument(MVNO_MATCH_DATA) { type = NavType.StringType },
    )

    @Composable
    override fun Page(arguments: Bundle?) {
        val context = LocalContext.current
        ApnPage(context)
    }

    fun getRoute(
        uriType: String,
        uri: Uri,
        subId: Int,
        mMvnoType: String,
        mMvnoMatchData: String
    ): String = "${name}/$uriType/${
        Base64.getUrlEncoder().encodeToString(uri.toString().toByteArray())
    }/$subId/$mMvnoType/$mMvnoMatchData"
}

@Composable
fun ApnPage(context: Context) {
    RegularScaffold(
        title = stringResource(id = R.string.apn_edit),
    ) {
    }
}