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

import static android.widget.Toast.LENGTH_LONG;

import android.content.Context;
import android.os.CountDownTimer;
import android.widget.Toast;

public class ToastUtil {

    public static void show(final Context context, final String text, final int durationSeconds) {
        final Toast toast = Toast.makeText(context, text, LENGTH_LONG);
        show(toast, durationSeconds);
    }

    public static void show(final Context context, final int resId, final int durationSeconds) {
        final Toast toast = Toast.makeText(context, resId, LENGTH_LONG);
        show(toast, durationSeconds);
    }

    public static void show(Toast toast, int durationSeconds) {
        final CountDownTimer countDown = new CountDownTimer(durationSeconds * 1000L, 1000) {
            @Override
            public void onTick(long l) {
                toast.show();
            }

            @Override
            public void onFinish() {
                toast.cancel();
            }
        };
        toast.show();
        countDown.start();
    }
}
