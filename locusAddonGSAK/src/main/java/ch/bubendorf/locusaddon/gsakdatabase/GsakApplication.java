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
import android.widget.Toast;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.HttpSenderConfigurationBuilder;
import org.acra.config.ToastConfigurationBuilder;
import org.acra.data.StringFormat;
import org.acra.security.TLS;
import org.acra.sender.HttpSender;

public class GsakApplication extends Application {

    public static String PACKAGE_NAME;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (!BuildConfig.DEBUG) {
            ACRA.init(this, new CoreConfigurationBuilder()
                    //core configuration:
                    .withBuildConfigClass(BuildConfig.class)
                    .withReportFormat(StringFormat.JSON)
                    .withPluginConfigurations(
                            //each plugin you chose above can be configured with its builder like this:
                            new ToastConfigurationBuilder()
                                    .withText("GSAKForLocus crashed. A report has been sent to the developer.")
                                    .withLength(Toast.LENGTH_LONG)
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
                                    .build()
                    )
            );
        }
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
