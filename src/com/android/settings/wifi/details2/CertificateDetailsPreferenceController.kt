/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.wifi.details2

import android.content.Context
import android.content.DialogInterface
import android.net.http.SslCertificate
import android.security.KeyChain
import android.security.keystore.KeyProperties
import android.security.keystore2.AndroidKeyStoreLoadStoreParameter
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settings.spa.preference.ComposePreferenceController
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.wifi.flags.Flags
import com.android.wifitrackerlib.WifiEntry
import com.android.wifitrackerlib.WifiEntry.CertificateInfo.CERTIFICATE_VALIDATION_METHOD_USING_CERTIFICATE_PINNING
import com.android.wifitrackerlib.WifiEntry.CertificateInfo.CERTIFICATE_VALIDATION_METHOD_USING_INSTALLED_ROOTCA
import com.android.wifitrackerlib.WifiEntry.CertificateInfo.CERTIFICATE_VALIDATION_METHOD_USING_SYSTEM_CERTIFICATE
import java.security.KeyStore
import java.security.cert.X509Certificate

class CertificateDetailsPreferenceController(context: Context, preferenceKey: String) :
    ComposePreferenceController(context, preferenceKey) {

    private lateinit var wifiEntry: WifiEntry

    fun setWifiEntry(entry: WifiEntry) {
        wifiEntry = entry
    }

    override fun getAvailabilityStatus(): Int {
        return if (Flags.androidVWifiApi() && isCertificateDetailsAvailable(wifiEntry)) AVAILABLE
        else CONDITIONALLY_UNAVAILABLE
    }

    @Composable
    override fun Content() {
        CertificateDetails()
    }

    @Composable
    fun CertificateDetails() {
        val context = LocalContext.current

        val validationMethod = wifiEntry.certificateInfo!!.validationMethod
        val certificateDetailsSummary = when (validationMethod) {
            CERTIFICATE_VALIDATION_METHOD_USING_SYSTEM_CERTIFICATE ->
                stringResource(R.string.wifi_certificate_summary_system)

            CERTIFICATE_VALIDATION_METHOD_USING_INSTALLED_ROOTCA -> {
                val aliasesSize = wifiEntry.certificateInfo?.caCertificateAliases?.size
                if (aliasesSize == 1) stringResource(R.string.one_cacrt)
                else
                    String.format(
                    stringResource(R.string.wifi_certificate_summary_Certificates),
                    aliasesSize
                )
            }

            else -> stringResource(R.string.wifi_certificate_summary_pinning)
        }

        Preference(object : PreferenceModel {
            override val title = stringResource(com.android.internal.R.string.ssl_certificate)
            override val summary = { certificateDetailsSummary }
            override val onClick: () -> Unit = {
                if (validationMethod == CERTIFICATE_VALIDATION_METHOD_USING_INSTALLED_ROOTCA)
                    getCertX509(wifiEntry)?.let {
                        createCertificateDetailsDialog(
                            context,
                            it
                        )
                    }
            }
        })
    }

    private fun getCertX509(wifiEntry: WifiEntry): X509Certificate? {
        val certificateAliases =
            wifiEntry.certificateInfo?.caCertificateAliases?.get(0)
                ?: return null
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(AndroidKeyStoreLoadStoreParameter(KeyProperties.NAMESPACE_WIFI))
            val cert = keyStore.getCertificate(certificateAliases)
            KeyChain.toCertificate(cert.encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Android Keystore.", e)
            null
        }
    }

    private fun createCertificateDetailsDialog(context: Context, certX509: X509Certificate) {
        val listener =
            DialogInterface.OnClickListener { dialog, id ->
                dialog.dismiss()
            }
        val titles = ArrayList<String>()
        val sslCert = SslCertificate(certX509)
        titles.add(sslCert.issuedTo.cName)
        val arrayAdapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            titles
        )
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val spinner = Spinner(context)
        spinner.setAdapter(arrayAdapter)

        val certLayout = LinearLayout(context)
        certLayout.orientation = LinearLayout.VERTICAL
        // Prevent content overlapping with spinner
        certLayout.setClipChildren(true)
        certLayout.addView(spinner)

        val view = sslCert.inflateCertificateView(context)
        view.visibility = View.VISIBLE
        certLayout.addView(view)
        certLayout.visibility = View.VISIBLE

        val dialog = AlertDialog.Builder(context)
            .setView(certLayout)
            .setTitle(com.android.internal.R.string.ssl_certificate)
            .setPositiveButton(R.string.wifi_settings_ssid_block_button_close, null)
            .setNegativeButton(R.string.trusted_credentials_remove_label, listener).create()
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false)
    }

    private fun isCertificateDetailsAvailable(wifiEntry: WifiEntry): Boolean {
        val validationMethod = wifiEntry.certificateInfo?.validationMethod
        return validationMethod in listOf(
            CERTIFICATE_VALIDATION_METHOD_USING_SYSTEM_CERTIFICATE,
            CERTIFICATE_VALIDATION_METHOD_USING_INSTALLED_ROOTCA,
            CERTIFICATE_VALIDATION_METHOD_USING_CERTIFICATE_PINNING
        )
    }

    companion object {
        const val TAG = "CertificateDetailsPreferenceController"
    }
}