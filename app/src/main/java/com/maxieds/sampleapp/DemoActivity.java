package com.maxieds.sampleapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toolbar;

import com.maxieds.chameleonminiusb.ChameleonDeviceConfig;
import com.maxieds.chameleonminiusb.LibraryLogging;
import com.maxieds.chameleonminiusb.Utils;

import static com.maxieds.chameleonminiusb.LibraryLogging.getChameleonNotifyFilter;

public class DemoActivity extends AppCompatActivity {

    public static DemoActivity localInst;

    public void enableStatusIcon(int iconID, int iconDrawable) {
        ((ImageView) findViewById(iconID)).setAlpha(255);
        ((ImageView) findViewById(iconID)).setImageDrawable(getResources().getDrawable(iconDrawable));
    }

    public void disableStatusIcon(int iconID) {
        ((ImageView) findViewById(iconID)).setAlpha(101);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        localInst = this;

        // setup the ultra-pretty toolbar combo:
        Toolbar actionBar = (Toolbar) findViewById(R.id.toolbarActionBar);
        actionBar.setTitleTextColor(getResources().getColor(R.color.actionBarTextColor));
        actionBar.setTitle("Chameleon USB Interface Library");
        actionBar.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        actionBar.setLogo(getResources().getDrawable(R.drawable.chameleonusb64));
        actionBar.setBackground(getResources().getDrawable(R.drawable.status_bar_gradient));
        actionBar.setContentInsetsAbsolute(0, 75);

        // secondary toolbar status icon initial setup:
        enableStatusIcon(R.id.statusIconNoUSB, R.drawable.usbdisconnected16);
        disableStatusIcon(R.id.statusIconUSB);
        disableStatusIcon(R.id.statusIconRevE);
        disableStatusIcon(R.id.statusIconRevG);
        enableStatusIcon(R.id.signalStrength, R.drawable.signalbars5);

        // configure misc settings for the running sample app:
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR); // keep app from crashing when the screen rotates

        // setup the intent filters packaged convenently by the library:
        IntentFilter chameleonNotifyFilters = getChameleonNotifyFilter();
        BroadcastReceiver chameleonNotifyBCastRecv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                onNewIntent(intent);
            }
        };
        registerReceiver(chameleonNotifyBCastRecv, chameleonNotifyFilters);

        // initialize the Chameleon USB library so it gets up and a' chugging:
        (new ChameleonDeviceConfig()).chameleonUSBInterfaceInitialize(this, LibraryLogging.LocalLoggingLevel.LOG_ADB_VERBOSE);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String intentAction = intent.getAction();
        if(intentAction == null) {
            return;
        }
        boolean displayAlertDialog = false;
        // TODO: Setup a timer (with a semaphore lock) to update the device status:
        if(intentAction.equals("CHAMELEON_REVG_ATTACHED")) {
            displayAlertDialog = true;
            disableStatusIcon(R.id.statusIconNoUSB);
            enableStatusIcon(R.id.statusIconUSB, R.drawable.usbconnected16);
            enableStatusIcon(R.id.statusIconRevG, R.drawable.revgboard24);
        }
        else if(intentAction.equals("CHAMELEON_REVE_ATTACHED")) {
            displayAlertDialog = true;
            disableStatusIcon(R.id.statusIconNoUSB);
            enableStatusIcon(R.id.statusIconUSB, R.drawable.usbconnected16);
            enableStatusIcon(R.id.statusIconRevE, R.drawable.reveboard24);
        }



    }
}
