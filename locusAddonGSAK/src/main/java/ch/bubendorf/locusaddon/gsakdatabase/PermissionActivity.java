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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import java.util.function.BiConsumer;

/**
 * Activity to ask the user for a permission.
 *
 * @author Markus Bubendorf <gsakforlocus@bubendorf.net>
 */
public class PermissionActivity extends ComponentActivity {

    private final static String PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;

    private static BiConsumer<Context, Object> runnable;
    private static Object data;

    public static <T> void checkPermission(final Context context, final BiConsumer<Context, T> callback, final T data) {
        if (ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            // Everything OK ==> Go On
            callback.accept(context, data);
        } else {
            //noinspection unchecked
            runnable = (BiConsumer<Context, Object>) callback;
            PermissionActivity.data = data;
            final Intent intent = new Intent(context, PermissionActivity.class);
            context.startActivity(intent);
        }
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();
    }

    private void checkPermission() {
        // Register the permissions callback, which handles the user's response to the
        // system permissions dialog. Save the return value, an instance of
        // ActivityResultLauncher, as an instance variable.
        final ActivityResultLauncher<String> requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        // Permission is granted. Continue the action or workflow in your app.
                        goOn();
                    } else {
                        // Explain to the user that the feature is unavailable because the
                        // features requires a permission that the user has denied. At the
                        // same time, respect the user's decision. Don't link to system
                        // settings in an effort to convince the user to change their
                        // decision.
                        //Toast.makeText(this, "So geht das aber nicht!", Toast.LENGTH_LONG).show();
                        finish();
                        goToAppSettings(this);
                    }
                });

        if (ContextCompat.checkSelfPermission(this, PERMISSION) == PackageManager.PERMISSION_GRANTED) {
            // You can use the API that requires the permission.
            goOn();
        } else if (shouldShowRequestPermissionRationale(PERMISSION)) {
            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            /*showAlert(this, "Erlaubnis", "Ohne Erlaubnis geht es nicht!");*/
            requestPermissionLauncher.launch(PERMISSION);
        } else {
            // You can directly ask for the permission.
            // The registered ActivityResultCallback gets the result of this request.
            requestPermissionLauncher.launch(PERMISSION);
        }
    }

    /*private void showAlert(final Context context, final String title, final String text) {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(text);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }*/

    private void goToAppSettings(final Context context) {
        final Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        final Uri uri = Uri.fromParts("package", GsakApplication.PACKAGE_NAME, null);
        intent.setData(uri);
        context.startActivity(intent);
    }

    private void goOn() {
        finish();
        if (runnable != null) {
            runnable.accept(getParent(), data);
            runnable = null;
        }
    }

}
