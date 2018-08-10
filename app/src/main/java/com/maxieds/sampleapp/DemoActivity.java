package com.maxieds.sampleapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toolbar;

import com.maxieds.chameleonminiusb.ChameleonDeviceConfig;
import com.maxieds.chameleonminiusb.LibraryLogging;
import com.maxieds.chameleonminiusb.Utils;
import com.maxieds.chameleonminiusb.ChameleonLibraryLoggingReceiver;

import java.io.InputStream;

import static com.maxieds.chameleonminiusb.ChameleonCommands.StandardCommandSet.GET_ACTIVE_SLOT;
import static com.maxieds.chameleonminiusb.ChameleonCommands.StandardCommandSet.GET_MEMORY_SIZE;
import static com.maxieds.chameleonminiusb.ChameleonCommands.StandardCommandSet.QUERY_CONFIG;
import static com.maxieds.chameleonminiusb.ChameleonCommands.StandardCommandSet.QUERY_UID;
import static com.maxieds.chameleonminiusb.ChameleonCommands.StandardCommandSet.SET_CONFIG;
import static com.maxieds.chameleonminiusb.ChameleonDeviceConfig.ChameleonUIDTypeSpec_t.INCREMENT_EXISTING;
import static com.maxieds.chameleonminiusb.ChameleonDeviceConfig.ChameleonUIDTypeSpec_t.TRULY_RANDOM;
import static com.maxieds.chameleonminiusb.ChameleonDeviceConfig.getChameleonMiniUSBDeviceParams;
import static com.maxieds.chameleonminiusb.ChameleonDeviceConfig.sendCommandToChameleon;
import static com.maxieds.chameleonminiusb.LibraryLogging.LocalLoggingLevel.LOG_ADB_INFO;

public class DemoActivity extends AppCompatActivity implements ChameleonLibraryLoggingReceiver {

    private static final String TAG = DemoActivity.class.getSimpleName();

    public static DemoActivity localInst;

    public void enableStatusIcon(int iconID, int iconDrawable) {
        ((ImageView) findViewById(iconID)).setAlpha(255);
        ((ImageView) findViewById(iconID)).setImageDrawable(getResources().getDrawable(iconDrawable));
    }

    public void disableStatusIcon(int iconID) {
        ((ImageView) findViewById(iconID)).setAlpha(101);
    }

    public void updateDemoWindowStatusBar() {
        if(!ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.isConfigured()) {
            ((TextView) findViewById(R.id.deviceConfigText)).setText("DEVICE CONFIGURATION");
            ((TextView) findViewById(R.id.deviceUIDText)).setText("DEVICE UID");
            ((TextView) findViewById(R.id.deviceMiscSettingsText)).setText("SLOT-# : MEM-0K");
            return;
        }
        try {
            String nfcConfig = sendCommandToChameleon(QUERY_CONFIG, null).cmdResponseData;
            String uid = String.join(":", sendCommandToChameleon(QUERY_UID, null).cmdResponseData.split("(?<=\\G..)"));
            String memSize = String.valueOf(Integer.parseInt(sendCommandToChameleon(GET_MEMORY_SIZE, null).cmdResponseData) / 4096) + "K";
            String slotNumber = "SLOT-" + sendCommandToChameleon(GET_ACTIVE_SLOT, null).cmdResponseData;
            ((TextView) findViewById(R.id.deviceConfigText)).setText(nfcConfig);
            ((TextView) findViewById(R.id.deviceUIDText)).setText(uid);
            ((TextView) findViewById(R.id.deviceMiscSettingsText)).setText(slotNumber + " : MEM-" + memSize);
        } catch(Exception nfe) {}
    }

    public void onReceiveNewLoggingData(Intent intentLog) {
        IntentLogEntry.postNewIntentLog(intentLog);
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

        // setup and request necessary permissions (ESPECIALLY USB):
        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.INTERNET",
                "com.android.example.USB_PERMISSION",
        };
        if (android.os.Build.VERSION.SDK_INT >= 23)
            requestPermissions(permissions, 200);
        else
            ActivityCompat.requestPermissions(this, permissions, 200);

        // now setup the basic serial port so that we can accept attached USB device connections:
        if(!ChameleonDeviceConfig.usbReceiversRegistered) {
            BroadcastReceiver usbActionReceiver = new BroadcastReceiver() {
                @RequiresPermission("com.android.example.USB_PERMISSION")
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction() != null && (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED) || intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED))) {
                        onNewIntent(intent);
                    }
                }
            };
            IntentFilter usbActionFilter = new IntentFilter();
            usbActionFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            usbActionFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
            registerReceiver(usbActionReceiver, usbActionFilter);
            ChameleonDeviceConfig.usbReceiversRegistered = true;
        }

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
        if(intentAction.equals("ACTION_USB_DEVICE_ATTACHED")) {
            ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.onNewIntent(intent);
            Utils.sleepThreadMillisecond(1000);
            if(!ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.chameleonPresent()) {
                return;
            }
            disableStatusIcon(R.id.statusIconNoUSB);
            enableStatusIcon(R.id.statusIconUSB, R.drawable.usbconnected16);
            if(ChameleonDeviceConfig.isRevisionEDevice()) {
                enableStatusIcon(R.id.statusIconRevE, R.drawable.reveboard24);
                disableStatusIcon(R.id.statusIconRevG);
            }
            else {
                enableStatusIcon(R.id.statusIconRevG, R.drawable.revgboard24);
                disableStatusIcon(R.id.statusIconRevE);
            }
            updateDemoWindowStatusBar();
        }
        else if(intentAction.equals("ACTION_USB_DEVICE_DETACHED")) {
            ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.onNewIntent(intent);
            disableStatusIcon(R.id.statusIconUSB);
            enableStatusIcon(R.id.statusIconNoUSB, R.drawable.usbdisconnected16);
            disableStatusIcon(R.id.statusIconRevE);
            disableStatusIcon(R.id.statusIconRevG);
            updateDemoWindowStatusBar();
        }

    }

    public void actionButtonUploadCardDump(View button) {
        if(!ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.isConfigured()) {
            LibraryLogging.i(TAG, "Cannot upload a binary dump without a valid device present!");
            return;
        }
        Spinner dumpImageSpinner = (Spinner) findViewById(R.id.dumpImageSpinner);
        int selectedDumpIndex = dumpImageSpinner.getSelectedItemPosition();
        String dumpImageFormat = getResources().getStringArray(R.array.sampleCardDumpConfigTypes)[selectedDumpIndex];
        int dumpImagePath = (int) dumpImageSpinner.getSelectedItem();
        int nextSlotPosition = ((Spinner) findViewById(R.id.slotNumberSpinner)).getSelectedItemPosition() + 1;
        ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.prepareChameleonEmulationSlot(nextSlotPosition, true);
        sendCommandToChameleon(SET_CONFIG, dumpImageFormat);
        InputStream dumpIStream = getResources().openRawResource(dumpImagePath);
        ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.chameleonUpload(dumpIStream);
    }

    public void actionButtonWriteLogs(View button) {
        LibraryLogging.LogEntry.writeLogsToXMLFile();
        LibraryLogging.LogEntry.writeLogsToPlainTextFile();
    }

    public void actionButtonClearLogs(View button) {
        LibraryLogging.LogEntry.loggingQueue.clear();
        ((LinearLayout) findViewById(R.id.loggingParentView)).removeAllViewsInLayout();
    }

    public void actionButtonStopLibrary(View button) {
        ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.chameleonUSBInterfaceShutdown();
        enableStatusIcon(R.id.statusIconNoUSB, R.drawable.usbdisconnected16);
        disableStatusIcon(R.id.statusIconUSB);
        disableStatusIcon(R.id.statusIconRevE);
        disableStatusIcon(R.id.statusIconRevG);
        updateDemoWindowStatusBar();
    }

    public void actionButtonQueryDevice(View button) {
        if(!ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.isConfigured()) {
            LibraryLogging.i(TAG, "Cannot query the device without a valid device present!");
            return;
        }
        String[] deviceSettings = getChameleonMiniUSBDeviceParams();
        LibraryLogging.LogEntry.enqueueNewLog(LOG_ADB_INFO, TAG, deviceSettings);
    }

    public void actionButtonRandomUID(View button) {
        if(!ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.isConfigured()) {
            LibraryLogging.i(TAG, "Cannot randomize the UID without a valid device present!");
            return;
        }
        ChameleonDeviceConfig.changeChameleonUID(TRULY_RANDOM, null);
        updateDemoWindowStatusBar();
    }

    public void actionButtonIncrementUID(View button) {
        if(!ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.isConfigured()) {
            LibraryLogging.i(TAG, "Cannot increment the UID without a valid device present!");
            return;
        }
        ChameleonDeviceConfig.changeChameleonUID(INCREMENT_EXISTING, null);
        updateDemoWindowStatusBar();
    }

    public void actionButtonSetReadonly(View button) {
        if(!ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.isConfigured()) {
            LibraryLogging.i(TAG, "Cannot set the active slot RO without a valid device present!");
            return;
        }
        sendCommandToChameleon(SET_CONFIG, 1);
    }

    public void actionButtonSetConfiguration(View button) {
        if(!ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.isConfigured()) {
            LibraryLogging.i(TAG, "Cannot set NFC config without a valid device present!");
            return;
        }
        String configShortText = ((Button) button).getText().toString();
        ChameleonDeviceConfig.ChameleonEmulatedConfigType_t nfcConfig = ChameleonDeviceConfig.ChameleonEmulatedConfigType_t.MF_CLASSIC_1K;
        if(configShortText.equals("MFU")) {
            nfcConfig = ChameleonDeviceConfig.ChameleonEmulatedConfigType_t.MF_ULTRALIGHT;
        }
        else if(configShortText.equals("MFC1K")) {
            nfcConfig = ChameleonDeviceConfig.ChameleonEmulatedConfigType_t.MF_CLASSIC_1K;
        }
        else if(configShortText.equals("MFC1K7B")) {
            nfcConfig = ChameleonDeviceConfig.ChameleonEmulatedConfigType_t.MF_CLASSIC_1K_7B;
        }
        else if(configShortText.equals("MFC4K")) {
            nfcConfig = ChameleonDeviceConfig.ChameleonEmulatedConfigType_t.MF_CLASSIC_4K;
        }
        else if(configShortText.equals("MFC4K7B")) {
            nfcConfig = ChameleonDeviceConfig.ChameleonEmulatedConfigType_t.MF_CLASSIC_4K_7B;
        }
        sendCommandToChameleon(SET_CONFIG, nfcConfig.name());
        updateDemoWindowStatusBar();
    }


}
