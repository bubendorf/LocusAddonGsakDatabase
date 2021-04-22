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

package ch.bubendorf.locusaddon.gsakdatabase.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Looper;

/**
 * A simple, optionally modal, Alert Dialog with a title, a text and a single OK button.
 */
public class SimpleAlertDialog {
    public static void show(final Context context, final int titleId, final int textId, final boolean modal) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        final Resources resources = context.getResources();
        builder.setTitle(resources.getText(titleId));
        builder.setMessage(resources.getText(textId));

        builder.setPositiveButton(resources.getText(android.R.string.ok), (dialog, which) -> {
            if (modal) {
                Looper.getMainLooper().quitSafely();
            }
            dialog.dismiss();
        });
        /*builder.setNegativeButton(resources.getText(android.R.string.cancel), (dialog, which) -> {
            if (modal) {
                Looper.getMainLooper().quitSafely();
            }
            dialog.cancel();
        });*/

        builder.show();

        if (modal) {
            try {
                Looper.loop();
            } catch (final RuntimeException ignored) {
                // Ignored
            }
        }
    }
}
