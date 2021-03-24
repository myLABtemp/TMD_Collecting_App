package com.TMDDataApp.crc_test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

public class RestartService extends BroadcastReceiver {

    public static final String ACTION_RESTART_SERVICE = "ACTION.Restart.BackGroundCollecting";
    private String mode;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        mode = intent.getStringExtra("mode");
        if (intent.getAction().equals(ACTION_RESTART_SERVICE)) {
            //mode = intent.getStringExtra("mode");
            //intent.putExtra("mode", mode);
            Intent i = new Intent(context, BackGroundCollecting.class);
            //mode = i.getStringExtra("mode");
            i.putExtra("mode", mode);
            //BackGroundCollecting ba = new BackGroundCollecting();
            //ba.setAlarmTimer();
            //ba.StartForground();
            //context.startForegroundService(i);
            //context.startService(i);
        }

        /*if (Collecting.sCpuWakeLock != null) {
            return;
        }
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        Collecting.sCpuWakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP |
                        PowerManager.ON_AFTER_RELEASE, "hi");

        Collecting.sCpuWakeLock.acquire();


        if (Collecting.sCpuWakeLock != null) {
            Collecting.sCpuWakeLock.release();
            Collecting.sCpuWakeLock = null;
        }*/

    }
}
