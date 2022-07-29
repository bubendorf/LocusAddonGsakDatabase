/*
 * DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 * Version 2, December 2004
 *
 * Copyright (C) 2004 Sam Hocevar <sam@hocevar.net>
 *
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document, and changing it is allowed as long
 * as the name is changed.
 *
 * DO WHAT THE FUCK YOU WANT TO PUBLIC LICENSE
 * TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 * 0. You just DO WHAT THE FUCK YOU WANT TO.
 */

package ch.bubendorf.locusaddon.gsakdatabase;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.DialogConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.config.LimiterConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.security.TLS;
import org.acra.sender.HttpSender;

public class GsakApplication extends Application {

    public static String PACKAGE_NAME;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //      if (!BuildConfig.DEBUG) {
        ACRA.init(this, new CoreConfigurationBuilder()
                //core configuration:
                .withBuildConfigClass(BuildConfig.class)
                .withReportFormat(StringFormat.JSON)
                .withPluginConfigurations(
                        new DialogConfigurationBuilder()
                                .withTitle(base.getString(R.string.acra_dialog_title))
                                .withText(base.getString(R.string.acra_dialog_text))
                                .withPositiveButtonText(base.getString(R.string.acra_dialog_yes))
                                .withNegativeButtonText(base.getString(R.string.acra_dialog_no))
                                .withResIcon(R.mipmap.ic_launcher)
                                .withEnabled(true)
                                .build(),
                        new HttpSenderConfigurationBuilder()
                                //required. Https recommended
                                .withUri("https://acra.bubendorf.net/report")
                                //optional. Enables http basic auth
                                .withBasicAuthLogin("7cfq2frH4cHAScuG")
                                //required if above set
                                .withBasicAuthPassword("8hAIp66QIEg6Jz5F")
                                // defaults to POST
                                .withHttpMethod(HttpSender.Method.POST)
                                //defaults to 5000ms
                                .withConnectionTimeout(5000)
                                //defaults to 20000ms
                                .withSocketTimeout(20000)
                                // defaults to false
                                .withDropReportsOnTimeout(false)
                                //defaults to false. Recommended if your backend supports it
                                .withCompress(true)
                                //defaults to all
                                .withTlsProtocols(TLS.V1_3, TLS.V1_2)
                                .build(),
                        new LimiterConfigurationBuilder()
                                .withDeleteReportsOnAppUpdate(true)
                                .withEnabled(true)
                                .build()
                )
        );
        //      }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PACKAGE_NAME = getApplicationContext().getPackageName();

        // Enable CloseGuard
        try {
            Class.forName("dalvik.system.CloseGuard")
                    .getMethod("setEnabled", boolean.class)
                    .invoke(null, true);
        } catch (final ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
