package ch.bubendorf.locusaddon.gsakdatabase;

import android.Manifest;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

public class GsakApplication extends Application {

    public static String PACKAGE_NAME;

    @Override
    public void onCreate() {
        super.onCreate();
        PACKAGE_NAME = getApplicationContext().getPackageName();
    }
}
