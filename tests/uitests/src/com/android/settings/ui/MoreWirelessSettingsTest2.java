/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.settings.ui;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.system.helpers.CommandsHelper;
import android.system.helpers.SettingsHelper;
import android.test.InstrumentationTestCase;
import android.test.suitebuilder.annotation.MediumTest;
import android.test.suitebuilder.annotation.Suppress;
import android.util.Log;

import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.Direction;
import androidx.test.uiautomator.StaleObjectException;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Ignore;

/**
 * Additional tests for Wifi Settings.
 */
@Ignore
public class MoreWirelessSettingsTest2 extends InstrumentationTestCase {
    // These back button presses are performed in tearDown() to exit Wifi
    // Settings sub-menus that a test might finish in. This number should be
    // high enough to account for the deepest sub-menu a test might enter.
    private static final int NUM_BACK_BUTTON_PRESSES = 5;
    private static final int TIMEOUT = 2000;
    private static final int SLEEP_TIME = 500;
    private static final String AIRPLANE_MODE_BROADCAST =
            "am broadcast -a android.intent.action.AIRPLANE_MODE";
    private static final String TAG="WirelessNetworkSettingsTests";

    // Note: The values of these variables might affect flakiness in tests that involve
    // scrolling. Adjust where necessary.
    private static final float SCROLL_UP_PERCENT = 10.0f;
    private static final float SCROLL_DOWN_PERCENT = 0.5f;
    private static final int MAX_SCROLL_ATTEMPTS = 10;
    private static final int MAX_ADD_NETWORK_BUTTON_ATTEMPTS = 3;
    private static final int SCROLL_SPEED = 2000;

    private static final String TEST_SSID = "testSsid";
    private static final String TEST_PW_GE_8_CHAR = "testPasswordGreaterThan8Char";
    private static final String TEST_PW_LT_8_CHAR = "lt8Char";
    private static final String TEST_DOMAIN = "testDomain.com";

    private static final String SETTINGS_PACKAGE = "com.android.settings";

    private static final String CHECKBOX_CLASS = "android.widget.CheckBox";
    private static final String SPINNER_CLASS = "android.widget.Spinner";
    private static final String EDIT_TEXT_CLASS = "android.widget.EditText";
    private static final String SCROLLVIEW_CLASS = "android.widget.ScrollView";
    private static final String LISTVIEW_CLASS = "android.widget.ListView";

    private static final String ADD_NETWORK_MENU_CANCEL_BUTTON_TEXT = "CANCEL";
    private static final String ADD_NETWORK_MENU_SAVE_BUTTON_TEXT = "SAVE";
    private static final String ADD_NETWORK_PREFERENCE_TEXT = "Add network";
    private static final String CONFIGURE_WIFI_PREFERENCE_TEXT = "Wi‑Fi preferences";
    private static final String CONFIGURE_WIFI_ADVANCED_PREFERENCE_TEXT = "Advanced";
    private static final String CACERT_MENU_PLEASE_SELECT_TEXT = "Please select";
    private static final String CACERT_MENU_USE_SYSTEM_CERTS_TEXT = "Use system certificates";
    private static final String CACERT_MENU_DO_NOT_VALIDATE_TEXT = "Do not validate";
    private static final String USERCERT_MENU_PLEASE_SELECT_TEXT = "Please select";
    private static final String USERCERT_MENU_DO_NOT_PROVIDE_TEXT = "Do not provide";
    private static final String SECURITY_OPTION_NONE_TEXT = "None";
    private static final String SECURITY_OPTION_WEP_TEXT = "WEP";
    private static final String SECURITY_OPTION_PSK_TEXT = "WPA/WPA2 PSK";
    private static final String SECURITY_OPTION_EAP_TEXT = "802.1x EAP";
    private static final String EAP_METHOD_PEAP_TEXT = "PEAP";
    private static final String EAP_METHOD_TLS_TEXT = "TLS";
    private static final String EAP_METHOD_TTLS_TEXT = "TTLS";
    private static final String EAP_METHOD_PWD_TEXT = "PWD";
    private static final String EAP_METHOD_SIM_TEXT = "SIM";
    private static final String EAP_METHOD_AKA_TEXT = "AKA";
    private static final String EAP_METHOD_AKA_PRIME_TEXT = "AKA'";
    private static final String PHASE2_MENU_NONE_TEXT = "None";
    private static final String PHASE2_MENU_MSCHAPV2_TEXT = "MSCHAPV2";
    private static final String PHASE2_MENU_GTC_TEXT = "GTC";

    private static final String ADD_NETWORK_MENU_ADV_TOGGLE_RES_ID = "wifi_advanced_togglebox";
    private static final String ADD_NETWORK_MENU_IP_SETTINGS_RES_ID = "ip_settings";
    private static final String ADD_NETWORK_MENU_PROXY_SETTINGS_RES_ID = "proxy_settings";
    private static final String ADD_NETWORK_MENU_SECURITY_OPTION_RES_ID = "security";
    private static final String ADD_NETWORK_MENU_EAP_METHOD_RES_ID = "method";
    private static final String ADD_NETWORK_MENU_SSID_RES_ID = "ssid";
    private static final String ADD_NETWORK_MENU_PHASE2_RES_ID = "phase2";
    private static final String ADD_NETWORK_MENU_CACERT_RES_ID = "ca_cert";
    private static final String ADD_NETWORK_MENU_USERCERT_RES_ID = "user_cert";
    private static final String ADD_NETWORK_MENU_NO_DOMAIN_WARNING_RES_ID = "no_domain_warning";
    private static final String ADD_NETWORK_MENU_NO_CACERT_WARNING_RES_ID = "no_ca_cert_warning";
    private static final String ADD_NETWORK_MENU_DOMAIN_LAYOUT_RES_ID = "l_domain";
    private static final String ADD_NETWORK_MENU_DOMAIN_RES_ID = "domain";
    private static final String ADD_NETWORK_MENU_IDENTITY_LAYOUT_RES_ID = "l_identity";
    private static final String ADD_NETWORK_MENU_ANONYMOUS_LAYOUT_RES_ID = "l_anonymous";
    private static final String ADD_NETWORK_MENU_PASSWORD_LAYOUT_RES_ID = "password_layout";
    private static final String ADD_NETWORK_MENU_SHOW_PASSWORD_LAYOUT_RES_ID =
            "show_password_layout";
    private static final String ADD_NETWORK_MENU_PASSWORD_RES_ID = "password";

    private static final BySelector ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR =
            By.scrollable(true).clazz(SCROLLVIEW_CLASS);
    private static final BySelector SPINNER_OPTIONS_SCROLLABLE_BY_SELECTOR =
            By.scrollable(true).clazz(LISTVIEW_CLASS);

    private UiDevice mDevice;
    private CommandsHelper mCommandsHelper;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mDevice = UiDevice.getInstance(getInstrumentation());
        try {
            mDevice.setOrientationNatural();
        } catch (RemoteException e) {
            throw new RuntimeException("failed to freeze device orientation", e);
        }
        // Ensure airplane mode is OFF so that wifi can be enabled using WiFiManager.
        Settings.Global.putString(getInstrumentation().getContext().getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, "0");
        Log.d(TAG, "sending airplane mode broadcast to device");
        mCommandsHelper = CommandsHelper.getInstance();
        mCommandsHelper.executeShellCommand(AIRPLANE_MODE_BROADCAST);
    }

    @Override
    protected void tearDown() throws Exception {
        // Exit all settings sub-menus.
        for (int i = 0; i < NUM_BACK_BUTTON_PRESSES; ++i) {
            mDevice.pressBack();
        }
        mDevice.pressHome();
        super.tearDown();
    }

    @MediumTest
    public void testWifiMenuLoadConfigure() throws Exception {
        loadWiFiConfigureMenu();
        Thread.sleep(SLEEP_TIME);
        UiObject2 configureWiFiHeading = mDevice.wait(Until.findObject
                (By.text(CONFIGURE_WIFI_PREFERENCE_TEXT)), TIMEOUT);
        assertNotNull("Configure WiFi menu has not loaded correctly", configureWiFiHeading);
    }

    @MediumTest
    public void testNetworkNotificationsOn() throws Exception {
        verifyNetworkNotificationsOnOrOff(true);
    }

    @MediumTest
    public void testNetworkNotificationsOff() throws Exception {
        verifyNetworkNotificationsOnOrOff(false);
    }

    @MediumTest
    public void testAddNetworkMenu_Default() throws Exception {
        loadAddNetworkMenu();

        // Submit button should be disabled by default, while cancel button should be enabled.
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());
        assertTrue(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_CANCEL_BUTTON_TEXT)), TIMEOUT).isEnabled());

        // Check that the SSID field is defaults to the hint.
        assertEquals("Enter the SSID", mDevice.wait(Until.findObject(By
                .res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_SSID_RES_ID)
                .clazz(EDIT_TEXT_CLASS)), TIMEOUT*2)
                .getText());

        // Check Security defaults to None.
        assertEquals("None", mDevice.wait(Until.findObject(By
                .res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_SECURITY_OPTION_RES_ID)
                .clazz(SPINNER_CLASS)), TIMEOUT)
                .getChildren().get(0).getText());

        // Check advanced options are collapsed by default.
        assertFalse(mDevice.wait(Until.findObject(By
                .res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_ADV_TOGGLE_RES_ID)
                .clazz(CHECKBOX_CLASS)), TIMEOUT).isChecked());

    }

    @Suppress
    @MediumTest
    public void testAddNetworkMenu_Proxy() throws Exception {
        loadAddNetworkMenu();

        // Toggle advanced options.
        mDevice.wait(Until.findObject(By
                .res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_ADV_TOGGLE_RES_ID)
                .clazz(CHECKBOX_CLASS)), TIMEOUT).click();

        // Verify Proxy defaults to None.
        BySelector proxySettingsBySelector =
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_PROXY_SETTINGS_RES_ID)
                .clazz(SPINNER_CLASS);
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR, proxySettingsBySelector);
        assertEquals("None", mDevice.wait(Until.findObject(proxySettingsBySelector), TIMEOUT)
                .getChildren().get(0).getText());

        // Verify that Proxy Manual fields appear.
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR, proxySettingsBySelector);
        mDevice.wait(Until.findObject(proxySettingsBySelector), TIMEOUT).click();
        mDevice.wait(Until.findObject(By.text("Manual")), TIMEOUT).click();
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, "proxy_warning_limited_support"));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, "proxy_hostname"));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, "proxy_exclusionlist"));

        // Verify that Proxy Auto-Config options appear.
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR, proxySettingsBySelector);
        mDevice.wait(Until.findObject(proxySettingsBySelector), TIMEOUT).click();
        mDevice.wait(Until.findObject(By.text("Proxy Auto-Config")), TIMEOUT).click();
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, "proxy_pac"));
    }

    @Suppress
    @MediumTest
    public void testAddNetworkMenu_IpSettings() throws Exception {
        loadAddNetworkMenu();

        // Toggle advanced options.
        mDevice.wait(Until.findObject(By
                .res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_ADV_TOGGLE_RES_ID)
                .clazz(CHECKBOX_CLASS)), TIMEOUT).click();

        // Verify IP settings defaults to DHCP.
        BySelector ipSettingsBySelector =
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_IP_SETTINGS_RES_ID).clazz(SPINNER_CLASS);
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR, ipSettingsBySelector);
        assertEquals("DHCP", mDevice.wait(Until.findObject(ipSettingsBySelector), TIMEOUT)
                .getChildren().get(0).getText());

        // Verify that Static IP settings options appear.
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR, ipSettingsBySelector).click();
        mDevice.wait(Until.findObject(By.text("Static")), TIMEOUT).click();
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, "ipaddress"));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, "gateway"));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, "network_prefix_length"));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, "dns1"));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, "dns2"));
    }

    @Suppress
    @MediumTest
    public void testPhase2Settings() throws Exception {
        loadAddNetworkMenu();
        selectSecurityOption(SECURITY_OPTION_EAP_TEXT);

        BySelector phase2SettingsBySelector =
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_PHASE2_RES_ID).clazz(SPINNER_CLASS);
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR, phase2SettingsBySelector);
        assertEquals(PHASE2_MENU_NONE_TEXT, mDevice.wait(Until
                .findObject(phase2SettingsBySelector), TIMEOUT).getChildren().get(0).getText());
        mDevice.wait(Until.findObject(phase2SettingsBySelector), TIMEOUT).click();
        Thread.sleep(SLEEP_TIME);

        // Verify Phase 2 authentication spinner options.
        assertNotNull(mDevice.wait(Until.findObject(By.text(PHASE2_MENU_NONE_TEXT)), TIMEOUT));
        assertNotNull(mDevice.wait(Until.findObject(By.text(PHASE2_MENU_MSCHAPV2_TEXT)), TIMEOUT));
        assertNotNull(mDevice.wait(Until.findObject(By.text(PHASE2_MENU_GTC_TEXT)), TIMEOUT));
    }

    @Suppress
    @MediumTest
    public void testCaCertSettings() throws Exception {
        loadAddNetworkMenu();
        selectSecurityOption(SECURITY_OPTION_EAP_TEXT);

        BySelector caCertSettingsBySelector =
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_CACERT_RES_ID).clazz(SPINNER_CLASS);
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR, caCertSettingsBySelector);
        assertEquals(CACERT_MENU_PLEASE_SELECT_TEXT, mDevice.wait(Until
                .findObject(caCertSettingsBySelector), TIMEOUT).getChildren().get(0).getText());
        mDevice.wait(Until.findObject(caCertSettingsBySelector), TIMEOUT).click();
        Thread.sleep(SLEEP_TIME);

        // Verify CA certificate spinner options.
        assertNotNull(mDevice.wait(Until.findObject(
                By.text(CACERT_MENU_PLEASE_SELECT_TEXT)), TIMEOUT));
        assertNotNull(mDevice.wait(Until.findObject(
                By.text(CACERT_MENU_USE_SYSTEM_CERTS_TEXT)), TIMEOUT));
        assertNotNull(mDevice.wait(Until.findObject(
                By.text(CACERT_MENU_DO_NOT_VALIDATE_TEXT)), TIMEOUT));

        // Verify that a domain field and warning appear when the user selects the
        // "Use system certificates" option.
        mDevice.wait(Until.findObject(By.text(CACERT_MENU_USE_SYSTEM_CERTS_TEXT)), TIMEOUT).click();
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_DOMAIN_LAYOUT_RES_ID));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_NO_DOMAIN_WARNING_RES_ID));

        // Verify that a warning appears when the user chooses the "Do Not Validate" option.
        mDevice.wait(Until.findObject(caCertSettingsBySelector), TIMEOUT).click();
        mDevice.wait(Until.findObject(By.text(CACERT_MENU_DO_NOT_VALIDATE_TEXT)), TIMEOUT).click();
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_NO_CACERT_WARNING_RES_ID));
    }

    @Suppress
    @MediumTest
    public void testAddNetwork_NoSecurity() throws Exception {
        loadAddNetworkMenu();
        selectSecurityOption(SECURITY_OPTION_NONE_TEXT);

        // Entering an SSID is enough to enable the submit button. // TODO THIS GUY
        enterSSID(TEST_SSID);
        assertTrue(mDevice.wait(Until
                .findObject(By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());
    }

    @Suppress
    @MediumTest
    public void testAddNetwork_WEP() throws Exception {
        loadAddNetworkMenu();
        selectSecurityOption(SECURITY_OPTION_WEP_TEXT);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        // Verify that WEP fields appear.
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_PASSWORD_LAYOUT_RES_ID));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_SHOW_PASSWORD_LAYOUT_RES_ID));

        // Entering an SSID alone does not enable the submit button.
        enterSSID(TEST_SSID);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        // Submit button is only enabled after a password is entered.
        enterPassword(TEST_PW_GE_8_CHAR);
        assertTrue(mDevice.wait(Until
                .findObject(By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());
    }

    @Suppress
    @MediumTest
    public void testAddNetwork_PSK() throws Exception {
        loadAddNetworkMenu();
        selectSecurityOption(SECURITY_OPTION_PSK_TEXT);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        // Verify that PSK fields appear.
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_PASSWORD_LAYOUT_RES_ID));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_SHOW_PASSWORD_LAYOUT_RES_ID));

        // Entering an SSID alone does not enable the submit button.
        enterSSID(TEST_SSID);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        // Entering an password that is too short does not enable submit button.
        enterPassword(TEST_PW_LT_8_CHAR);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        // Submit button is only enabled after a password of valid length is entered.
        enterPassword(TEST_PW_GE_8_CHAR);
        assertTrue(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());
    }

    @Suppress
    @MediumTest
    public void testAddNetwork_EAP_PEAP() throws Exception {
        loadAddNetworkMenu();
        selectSecurityOption(SECURITY_OPTION_EAP_TEXT);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        selectEAPMethod(EAP_METHOD_PEAP_TEXT);

        // Verify that EAP-PEAP fields appear.
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_PHASE2_RES_ID));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_CACERT_RES_ID));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_IDENTITY_LAYOUT_RES_ID));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_ANONYMOUS_LAYOUT_RES_ID));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_PASSWORD_LAYOUT_RES_ID));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_SHOW_PASSWORD_LAYOUT_RES_ID));

        // Entering an SSID alone does not enable the submit button.
        enterSSID(TEST_SSID);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        verifyCaCertificateSubmitConditions();
    }

    @Suppress
    @MediumTest
    public void testAddNetwork_EAP_TLS() throws Exception {
        loadAddNetworkMenu();
        selectSecurityOption(SECURITY_OPTION_EAP_TEXT);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        selectEAPMethod(EAP_METHOD_TLS_TEXT);

        // Verify that EAP-TLS fields appear.
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_CACERT_RES_ID));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_USERCERT_RES_ID));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_IDENTITY_LAYOUT_RES_ID));

        // Entering an SSID alone does not enable the submit button.
        enterSSID(TEST_SSID);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        // Selecting the User certificate "Do not provide" option alone does not enable the submit
        // button.
        selectUserCertificateOption(USERCERT_MENU_DO_NOT_PROVIDE_TEXT);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        verifyCaCertificateSubmitConditions();
    }

    @Suppress
    @MediumTest
    public void testAddNetwork_EAP_TTLS() throws Exception {
        loadAddNetworkMenu();
        selectSecurityOption(SECURITY_OPTION_EAP_TEXT);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        selectEAPMethod(EAP_METHOD_TTLS_TEXT);

        // Verify that EAP-TLS fields appear.
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_PHASE2_RES_ID));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_CACERT_RES_ID));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_IDENTITY_LAYOUT_RES_ID));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_ANONYMOUS_LAYOUT_RES_ID));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_PASSWORD_LAYOUT_RES_ID));

        // Entering an SSID alone does not enable the submit button.
        enterSSID(TEST_SSID);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        verifyCaCertificateSubmitConditions();
    }

    @Suppress
    @MediumTest
    public void testAddNetwork_EAP_PWD() throws Exception {
        loadAddNetworkMenu();
        selectSecurityOption(SECURITY_OPTION_EAP_TEXT);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        selectEAPMethod(EAP_METHOD_PWD_TEXT);

        // Verify that EAP-TLS fields appear.
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_IDENTITY_LAYOUT_RES_ID));
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_PASSWORD_LAYOUT_RES_ID));

        // Entering an SSID alone enables the submit button.
        enterSSID(TEST_SSID);
        assertTrue(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());
    }

    @Suppress
    @MediumTest
    public void testAddNetwork_EAP_SIM() throws Exception {
        loadAddNetworkMenu();
        selectSecurityOption(SECURITY_OPTION_EAP_TEXT);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        selectEAPMethod(EAP_METHOD_SIM_TEXT);

        // Entering an SSID alone enables the submit button.
        enterSSID(TEST_SSID);
        assertTrue(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());
    }

    @Suppress
    @MediumTest
    public void testAddNetwork_EAP_AKA() throws Exception {
        loadAddNetworkMenu();
        selectSecurityOption(SECURITY_OPTION_EAP_TEXT);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        selectEAPMethod(EAP_METHOD_AKA_TEXT);

        // Entering an SSID alone enables the submit button.
        enterSSID(TEST_SSID);
        assertTrue(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());
    }

    @Suppress
    @MediumTest
    public void testAddNetwork_EAP_AKA_PRIME() throws Exception {
        loadAddNetworkMenu();
        selectSecurityOption(SECURITY_OPTION_EAP_TEXT);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        selectEAPMethod(EAP_METHOD_AKA_PRIME_TEXT);

        // Entering an SSID alone enables the submit button.
        enterSSID(TEST_SSID);
        assertTrue(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());
    }

    private void verifyKeepWiFiOnDuringSleep(String settingToBeVerified, int settingValue)
            throws Exception {
        loadWiFiConfigureMenu();
        mDevice.wait(Until.findObject(By.text("Keep Wi‑Fi on during sleep")), TIMEOUT)
                .click();
        mDevice.wait(Until.findObject(By.clazz("android.widget.CheckedTextView")
                .text(settingToBeVerified)), TIMEOUT).click();
        Thread.sleep(SLEEP_TIME);
        int keepWiFiOnSetting =
                Settings.Global.getInt(getInstrumentation().getContext().getContentResolver(),
                Settings.Global.WIFI_SLEEP_POLICY);
        assertEquals(settingValue, keepWiFiOnSetting);
    }

    private void verifyNetworkNotificationsOnOrOff(boolean verifyOn)
            throws Exception {
        // Enable network recommendations to enable the toggle switch for Network
        // notifications
        Settings.Global.putString(getInstrumentation().getContext().getContentResolver(),
                Settings.Global.NETWORK_RECOMMENDATIONS_ENABLED, "1");
        if (verifyOn) {
            Settings.Global.putString(getInstrumentation().getContext().getContentResolver(),
                    Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, "0");
        }
        else {
            Settings.Global.putString(getInstrumentation().getContext().getContentResolver(),
                    Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON, "1");
        }
        loadWiFiConfigureMenu();
        mDevice.wait(Until.findObject(By.text("Open network notification")), TIMEOUT)
                .click();
        Thread.sleep(SLEEP_TIME);
        String wifiNotificationValue =
                Settings.Global.getString(getInstrumentation().getContext().getContentResolver(),
                Settings.Global.WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON);
        if (verifyOn) {
            assertEquals("1", wifiNotificationValue);
        }
        else {
            assertEquals("0", wifiNotificationValue);
        }
    }

    private void verifyWiFiOnOrOff(boolean verifyOn) throws Exception {
         String switchText = "On";
         if (verifyOn) {
             switchText = "Off";
         }
         loadWiFiSettingsPage(!verifyOn);
         mDevice.wait(Until
                 .findObject(By.res(SETTINGS_PACKAGE, "switch_bar").text(switchText)), TIMEOUT)
                 .click();
         Thread.sleep(SLEEP_TIME);
         String wifiValue =
                 Settings.Global.getString(getInstrumentation().getContext().getContentResolver(),
                 Settings.Global.WIFI_ON);
         if (verifyOn) {
             // 1 is Enabled, 2 is Enabled while airplane mode is ON.
             assertTrue(wifiValue.equals("1") || wifiValue.equals("2"));
         }
         else {
             assertEquals("0", wifiValue);
         }
    }

    private void verifyCaCertificateSubmitConditions() throws Exception {
        // Selecting the CA certificate "Do not validate" option enables the submit button.
        selectCaCertificateOption(CACERT_MENU_DO_NOT_VALIDATE_TEXT);
        assertTrue(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        // However, selecting the CA certificate "Use system certificates option" is not enough to
        // enable the submit button.
        selectCaCertificateOption(CACERT_MENU_USE_SYSTEM_CERTS_TEXT);
        assertFalse(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());

        // Submit button is only enabled after a domain is entered as well.
        enterDomain(TEST_DOMAIN);
        assertTrue(mDevice.wait(Until.findObject(
                By.text(ADD_NETWORK_MENU_SAVE_BUTTON_TEXT)), TIMEOUT).isEnabled());
    }

    private void loadWiFiSettingsPage(boolean wifiEnabled) throws Exception {
        WifiManager wifiManager = (WifiManager)getInstrumentation().getContext()
                .getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(wifiEnabled);
        SettingsHelper.launchSettingsPage(getInstrumentation().getContext(),
                Settings.ACTION_WIFI_SETTINGS);
    }

    private void loadWiFiConfigureMenu() throws Exception {
        loadWiFiSettingsPage(false);
        Thread.sleep(TIMEOUT);
        mDevice.wait(Until.findObject(By.text(CONFIGURE_WIFI_PREFERENCE_TEXT)), TIMEOUT).click();
        mDevice.wait(Until.findObject(
                By.text(CONFIGURE_WIFI_ADVANCED_PREFERENCE_TEXT)), TIMEOUT).click();
    }

    private void loadAddNetworkMenu() throws Exception {
        loadWiFiSettingsPage(true);
        for (int attempts = 0; attempts < MAX_ADD_NETWORK_BUTTON_ATTEMPTS; ++attempts) {
            try {
                findOrScrollToObject(By.scrollable(true), By.text(ADD_NETWORK_PREFERENCE_TEXT))
                        .click();
            } catch (StaleObjectException e) {
                // The network list might have been updated between when the Add network button was
                // found, and when it UI automator attempted to click on it. Retry.
                continue;
            }
            // If we get here, we successfully clicked on the Add network button, so we are done.
            Thread.sleep(SLEEP_TIME*5);
            return;
        }

        fail("Failed to load Add Network Menu after " + MAX_ADD_NETWORK_BUTTON_ATTEMPTS
                + " retries");
    }

    private void selectSecurityOption(String securityOption) throws Exception {
        // We might not need to scroll to the security options if not enough add network menu
        // options are visible.
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_SECURITY_OPTION_RES_ID)
                .clazz(SPINNER_CLASS)).click();
        Thread.sleep(SLEEP_TIME);
        mDevice.wait(Until.findObject(By.text(securityOption)), TIMEOUT).click();
    }

    private void selectEAPMethod(String eapMethod) throws Exception {
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_EAP_METHOD_RES_ID).clazz(SPINNER_CLASS))
                .click();
        Thread.sleep(SLEEP_TIME);
        findOrScrollToObject(SPINNER_OPTIONS_SCROLLABLE_BY_SELECTOR, By.text(eapMethod)).click();
    }

    private void selectUserCertificateOption(String userCertificateOption) throws Exception {
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_USERCERT_RES_ID).clazz(SPINNER_CLASS))
                .click();
        mDevice.wait(Until.findObject(By.text(userCertificateOption)), TIMEOUT).click();
    }

    private void selectCaCertificateOption(String caCertificateOption) throws Exception {
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_CACERT_RES_ID).clazz(SPINNER_CLASS))
                .click();
        mDevice.wait(Until.findObject(By.text(caCertificateOption)), TIMEOUT).click();
    }

    private void enterSSID(String ssid) throws Exception {
        // We might not need to scroll to the SSID option if not enough add network menu options
        // are visible.
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_SSID_RES_ID).clazz(EDIT_TEXT_CLASS))
                .setText(ssid);
    }

    private void enterPassword(String password) throws Exception {
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_PASSWORD_RES_ID).clazz(EDIT_TEXT_CLASS))
                .setText(password);
    }

    private void enterDomain(String domain) throws Exception {
        findOrScrollToObject(ADD_NETWORK_MENU_SCROLLABLE_BY_SELECTOR,
                By.res(SETTINGS_PACKAGE, ADD_NETWORK_MENU_DOMAIN_RES_ID)).setText(domain);
    }

    // Use this if the UI object might or might not need to be scrolled to.
    private UiObject2 findOrScrollToObject(BySelector scrollableSelector, BySelector objectSelector)
            throws Exception {
        UiObject2 object = mDevice.wait(Until.findObject(objectSelector), TIMEOUT);
        if (object == null) {
            object = scrollToObject(scrollableSelector, objectSelector);
        }
        return object;
    }

    private UiObject2 scrollToObject(BySelector scrollableSelector, BySelector objectSelector)
            throws Exception {
        UiObject2 scrollable = mDevice.wait(Until.findObject(scrollableSelector), TIMEOUT);
        if (scrollable == null) {
            fail("Could not find scrollable UI object identified by " + scrollableSelector);
        }
        UiObject2 found = null;
        // Scroll all the way up first, then all the way down.
        while (true) {
            // Optimization: terminate if we find the object while scrolling up to reset, so
            // we save the time spent scrolling down again.
            boolean canScrollAgain = scrollable.scroll(Direction.UP, SCROLL_UP_PERCENT,
                    SCROLL_SPEED);
            found = mDevice.findObject(objectSelector);
            if (found != null) return found;
            if (!canScrollAgain) break;
        }
        for (int attempts = 0; found == null && attempts < MAX_SCROLL_ATTEMPTS; ++attempts) {
            // Return value of UiObject2.scroll() is not reliable, so do not use it in loop
            // condition, in case it causes this loop to terminate prematurely.
            scrollable.scroll(Direction.DOWN, SCROLL_DOWN_PERCENT, SCROLL_SPEED);
            found = mDevice.findObject(objectSelector);
        }
        if (found == null) {
            fail("Could not scroll to UI object identified by " + objectSelector);
        }
        return found;
    }
}
