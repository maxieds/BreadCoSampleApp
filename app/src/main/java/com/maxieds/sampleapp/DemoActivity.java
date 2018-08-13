package com.maxieds.sampleapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toolbar;

import com.maxieds.chameleonminiusb.ChameleonDeviceConfig;
import com.maxieds.chameleonminiusb.LibraryLogging;
import com.maxieds.chameleonminiusb.Utils;
import com.maxieds.chameleonminiusb.ChameleonLibraryLoggingReceiver;
import com.maxieds.chameleonminiusb.XModem;

import java.io.InputStream;

import static com.maxieds.chameleonminiusb.ChameleonCommands.NODATA;
import static com.maxieds.chameleonminiusb.ChameleonCommands.StandardCommandSet.GET_ACTIVE_SLOT;
import static com.maxieds.chameleonminiusb.ChameleonCommands.StandardCommandSet.GET_MEMORY_SIZE;
import static com.maxieds.chameleonminiusb.ChameleonCommands.StandardCommandSet.GET_UID_SIZE;
import static com.maxieds.chameleonminiusb.ChameleonCommands.StandardCommandSet.QUERY_CONFIG;
import static com.maxieds.chameleonminiusb.ChameleonCommands.StandardCommandSet.QUERY_UID;
import static com.maxieds.chameleonminiusb.ChameleonCommands.StandardCommandSet.SET_CONFIG;
import static com.maxieds.chameleonminiusb.ChameleonDeviceConfig.ChameleonUIDTypeSpec_t.INCREMENT_EXISTING;
import static com.maxieds.chameleonminiusb.ChameleonDeviceConfig.ChameleonUIDTypeSpec_t.TRULY_RANDOM;
import static com.maxieds.chameleonminiusb.ChameleonDeviceConfig.SERIAL_USB_COMMAND_TIMEOUT;
import static com.maxieds.chameleonminiusb.ChameleonDeviceConfig.getChameleonMiniUSBDeviceParams;
import static com.maxieds.chameleonminiusb.ChameleonDeviceConfig.sendCommandToChameleon;
import static com.maxieds.chameleonminiusb.LibraryLogging.LocalLoggingLevel.LOG_ADB_DEBUG;
import static com.maxieds.chameleonminiusb.LibraryLogging.LocalLoggingLevel.LOG_ADB_ERROR;
import static com.maxieds.chameleonminiusb.LibraryLogging.LocalLoggingLevel.LOG_ADB_INFO;
import static com.maxieds.chameleonminiusb.LibraryLogging.LocalLoggingLevel.LOG_ADB_OFF;
import static com.maxieds.chameleonminiusb.LibraryLogging.LocalLoggingLevel.LOG_ADB_VERBOSE;
import static com.maxieds.chameleonminiusb.LibraryLogging.LocalLoggingLevel.LOG_ADB_WARN;

public class DemoActivity extends AppCompatActivity implements ChameleonLibraryLoggingReceiver {

    private static final String TAG = DemoActivity.class.getSimpleName();

    public static final String PACKAGE_NOTIFY_CHANNRLID = "com.maxieda.sampleapp:Channel_01";
    public static final int DEMO_ACTIVITY_NOTIFY_ID = 1;
    public static final int CHAMELEON_UPLOAD_NOTIFY_ID = 2;

    public static DemoActivity localInst;

    public void enableStatusIcon(int iconID, int iconDrawable) {
        ((ImageView) findViewById(iconID)).setAlpha(255);
        ((ImageView) findViewById(iconID)).setImageDrawable(getResources().getDrawable(iconDrawable));
    }

    public void disableStatusIcon(int iconID) {
        ((ImageView) findViewById(iconID)).setAlpha(127);
    }

    public void updateDemoWindowStatusBar() {
        if(!ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.isConfigured()) {
            enableStatusIcon(R.id.statusIconNoUSB, R.drawable.usbdisconnected16);
            disableStatusIcon(R.id.statusIconUSB);
            disableStatusIcon(R.id.statusIconRevE);
            disableStatusIcon(R.id.statusIconRevG);
            ((TextView) findViewById(R.id.deviceConfigText)).setText("DEVICE CONFIGURATION");
            ((TextView) findViewById(R.id.deviceUIDText)).setText("DEVICE UID");
            ((TextView) findViewById(R.id.deviceMiscSettingsText)).setText("SLOT-# : MEM-0K");
            return;
        }
        int standardTimeout = ChameleonDeviceConfig.SERIAL_USB_COMMAND_TIMEOUT;
        ChameleonDeviceConfig.SERIAL_USB_COMMAND_TIMEOUT = 500;
        disableStatusIcon(R.id.statusIconNoUSB);
        enableStatusIcon(R.id.statusIconUSB, R.drawable.usbconnected16);
        if(ChameleonDeviceConfig.isRevisionEDevice()) {
            enableStatusIcon(R.id.statusIconRevE, R.drawable.reveboard24);
            disableStatusIcon(R.id.statusIconRevG);
        }
        else if(ChameleonDeviceConfig.isRevisionGDevice()) {
            enableStatusIcon(R.id.statusIconRevG, R.drawable.revgboard24);
            disableStatusIcon(R.id.statusIconRevE);
        }
        else {
            disableStatusIcon(R.id.statusIconRevE);
            disableStatusIcon(R.id.statusIconRevG);
        }
        try {
            String nfcConfigResp = sendCommandToChameleon(QUERY_CONFIG, null).cmdResponseData;
            String nfcConfig = nfcConfigResp.equals(NODATA) ? "???" : nfcConfigResp;
            String uidResp = sendCommandToChameleon(QUERY_UID, null).cmdResponseData;
            String uid = uidResp.equals(NODATA) ? "???" : String.join(":", uidResp.replaceAll("..(?!$)", "$0:"));
            String memSize;
            try {
                memSize = String.valueOf(Integer.parseInt(sendCommandToChameleon(GET_MEMORY_SIZE, null).cmdResponseData)) + "K";
            } catch(Exception nfe) {
                memSize = "SIZE-???";
            }
            String slotResp = sendCommandToChameleon(GET_ACTIVE_SLOT, null).cmdResponseData;
            String slotNumber = "SLOT-" + (slotResp.equals(NODATA) ? "???" : slotResp);
            ((TextView) findViewById(R.id.deviceConfigText)).setText("CFG: " + nfcConfig);
            ((TextView) findViewById(R.id.deviceUIDText)).setText("UID: " + uid);
            ((TextView) findViewById(R.id.deviceMiscSettingsText)).setText(slotNumber + " / MEM-" + memSize);
        } catch(Exception nfe) {
            Log.e(TAG, nfe.getMessage());
            nfe.printStackTrace();
        }
        ChameleonDeviceConfig.SERIAL_USB_COMMAND_TIMEOUT = standardTimeout;
    }

    public void onReceiveNewLoggingData(Intent intentLog) {
        IntentLogEntry.postNewIntentLog(intentLog);
        ScrollView logScrollView = (ScrollView) findViewById(R.id.log_scroll_view);
        if(logScrollView != null) {
            logScrollView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    ScrollView logScroller = (ScrollView) DemoActivity.localInst.findViewById(R.id.log_scroll_view);
                    LinearLayout loggingParentView = (LinearLayout) DemoActivity.localInst.findViewById(R.id.loggingParentView);
                    LinearLayout lastLogElt = (LinearLayout) loggingParentView.getChildAt(loggingParentView.getChildCount() - 1);
                    lastLogElt.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    int bottomEltHeight = lastLogElt.getMeasuredHeight();
                    logScroller.scrollTo(0, logScroller.getBottom() + bottomEltHeight);
                    logScroller.fullScroll(View.FOCUS_DOWN);
                }
            }, 25);
        }
    }

    private boolean postSystemNotificationIcon(int notifyID, String notifyName, int drawableResID, String notifyAppDesc) {

        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(notifyManager == null) {
            return false;
        }

        NotificationChannel notifyChannel = new NotificationChannel(PACKAGE_NOTIFY_CHANNRLID, notifyName, NotificationManager.IMPORTANCE_LOW);
        notifyChannel.setDescription(notifyAppDesc);
        notifyChannel.enableLights(true);
        notifyChannel.setLightColor(Color.GREEN);
        notifyChannel.enableVibration(true);
        notifyChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        notifyManager.createNotificationChannel(notifyChannel);

        Notification notifyIcon = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(drawableResID)
                .setContentTitle(notifyName)
                .setContentText(notifyAppDesc)
                .setOngoing(false)
                .setChannelId(PACKAGE_NOTIFY_CHANNRLID)
                .build();
        notifyManager.notify(notifyID, notifyIcon);
        return true;

    }

    private void removeSystemNotificationIcon(int notifyID) {
        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(notifyManager != null) {
            notifyManager.cancel(notifyID);
        }
    }

    public static Runnable updateStatusBarRunnable = new Runnable() {
        public void run() {
            DemoActivity.localInst.updateDemoWindowStatusBar();
            updateStatusBarHandler.postDelayed(this, SERIAL_USB_COMMAND_TIMEOUT);
        }
    };
    public static Handler updateStatusBarHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        if(!isTaskRoot()) {
            final Intent intent = getIntent();
            final String intentAction = intent.getAction();
            if (intentAction != null && (intentAction.equals(UsbManager.ACTION_USB_DEVICE_DETACHED) || intentAction.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED))) {
                //localInst.sendBroadcast(intent);
                finish();
                return;
            }
        }
        setContentView(R.layout.activity_demo);
        localInst = this;

        // setup the ultra-pretty toolbar combo:
        Toolbar actionBar = (Toolbar) findViewById(R.id.toolbarActionBar);
        actionBar.setTitleTextColor(getResources().getColor(R.color.actionBarTextColor));
        actionBar.setTitle("Chameleon USB Interface Library");
        actionBar.setSubtitleTextColor(getResources().getColor(R.color.actionBarTextColor));
        actionBar.setSubtitle("Bread Company Demo Application v" + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")");
        actionBar.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        actionBar.setLogo(getResources().getDrawable(R.drawable.chameleonusb64));
        actionBar.setBackground(getResources().getDrawable(R.drawable.status_bar_gradient));
        actionBar.setContentInsetsAbsolute(0, 75);

        // secondary toolbar status icon initial setup:
        enableStatusIcon(R.id.statusIconNoUSB, R.drawable.usbdisconnected16);
        disableStatusIcon(R.id.statusIconUSB);
        disableStatusIcon(R.id.statusIconRevE);
        disableStatusIcon(R.id.statusIconRevG);
        disableStatusIcon(R.id.statusIconCardDumpUpload);

        // configure misc settings for the running sample app:
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR); // keep app from crashing when the screen rotates
        configureLoggingSpinner();

        // setup and request necessary permissions (ESPECIALLY USB):
        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.INTERNET",
                "com.android.example.USB_PERMISSION",
        };
        ActivityCompat.requestPermissions(this, permissions, 0);

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
            usbActionFilter.addAction("CHAMELEON_UPLOAD_SUCCESS");
            usbActionFilter.addAction("CHAMELEON_UPLOAD_FAILURE");
            registerReceiver(usbActionReceiver, usbActionFilter);
        }

        // initialize the Chameleon USB library so it gets up and a' chugging:
        (new ChameleonDeviceConfig()).chameleonUSBInterfaceInitialize(this, LibraryLogging.LocalLoggingLevel.LOG_ADB_INFO);
        if(ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.chameleonPresent()) {
            LibraryLogging.i(TAG, "The chameleon device is connected! :)");
            LibraryLogging.i(TAG, String.join("\n", getChameleonMiniUSBDeviceParams()));
        }
        else {
            LibraryLogging.i(TAG, "Unable to connect to chameleon device :(");
        }
        updateStatusBarHandler.postDelayed(updateStatusBarRunnable, 25);

        // place a chameleon icon in the system (notifications) tray while the app is running:
        postSystemNotificationIcon(DEMO_ACTIVITY_NOTIFY_ID, "DEMO ACTIVITY",
                R.drawable.chameleonnotifyicon32, getString(R.string.app_name) + ": " + getString(R.string.app_desc));

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
            Utils.sleepThreadMillisecond(1250);
            updateDemoWindowStatusBar();
        }
        else if(intentAction.equals("ACTION_USB_DEVICE_DETACHED")) {
            ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.onNewIntent(intent);
            Utils.sleepThreadMillisecond(1250);
            updateDemoWindowStatusBar();
        }
        else if(intentAction.equals("CHAMELEON_UPLOAD_SUCCESS")) {}
        else if(intentAction.equals("CHAMELEON_UPLOAD_FAILURE")) {}

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isFinishing()) {
            removeSystemNotificationIcon(DEMO_ACTIVITY_NOTIFY_ID);
            ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.chameleonUSBInterfaceShutdown();
        }
    }

    private void configureLoggingSpinner() {
        final String[] spinnerList = getResources().getStringArray(R.array.loggingLevels);
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, spinnerList);
        Spinner loggingSpinner = (Spinner) findViewById(R.id.loggingLevelSpinner);
        loggingSpinner.setAdapter(spinnerAdapter);
        loggingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            String[] localSpinnerList = spinnerList;
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String setCmd = localSpinnerList[i];
                if (setCmd.charAt(0) == '-') {
                    return;
                }
                LibraryLogging.LocalLoggingLevel libraryLoggingLevel = LibraryLogging.localLoggingLevel;
                if (setCmd.equals("OFF")) libraryLoggingLevel = LOG_ADB_OFF;
                else if (setCmd.equals("INFO")) libraryLoggingLevel = LOG_ADB_INFO;
                else if (setCmd.equals("WARN")) libraryLoggingLevel = LOG_ADB_WARN;
                else if (setCmd.equals("ERROR")) libraryLoggingLevel = LOG_ADB_ERROR;
                else if (setCmd.equals("DEBUG")) libraryLoggingLevel = LOG_ADB_DEBUG;
                else if (setCmd.equals("VERBOSE")) libraryLoggingLevel = LOG_ADB_VERBOSE;
                LibraryLogging.localLoggingLevel = libraryLoggingLevel;
                LibraryLogging.i(TAG, "Logging level set to " + libraryLoggingLevel.name() + ".");
                //((Spinner) DemoActivity.localInst.findViewById(R.id.loggingLevelSpinner)).setSelection(0);
            }
            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            }
        });
    }

    public void actionButtonUploadCardDump(View button) {

        if(!ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.isConfigured()) {
            LibraryLogging.i(TAG, "Cannot upload a binary dump without a valid device present!");
            return;
        }

        // set a notification icon indicating that an upload is in progress:
        //postSystemNotificationIcon(CHAMELEON_UPLOAD_NOTIFY_ID, "CHAMELEON UPLOAD",
        //        R.drawable.uploadnotifyicon, "Chameleon card dump upload in progress ... ");
        enableStatusIcon(R.id.statusIconCardDumpUpload, R.drawable.uploadstatusicon16);

        // upload the image:
        Spinner dumpImageSpinner = (Spinner) findViewById(R.id.dumpImageSpinner);
        int selectedDumpIndex = dumpImageSpinner.getSelectedItemPosition();
        String dumpImageFormat = getResources().getStringArray(R.array.sampleCardDumpConfigTypes)[selectedDumpIndex];
        String dumpImagePath = dumpImageSpinner.getSelectedItem().toString();
        String dumpImageRawFilename = dumpImagePath.substring(dumpImagePath.lastIndexOf("/") + 1);
        int nextSlotPosition = ((Spinner) findViewById(R.id.slotNumberSpinner)).getSelectedItemPosition();
        ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.prepareChameleonEmulationSlot(nextSlotPosition, true);
        sendCommandToChameleon(SET_CONFIG, dumpImageFormat);
        final InputStream dumpIStream = getResources().openRawResource(getResources().getIdentifier(dumpImageRawFilename, "raw", getPackageName()));
        try {
            LibraryLogging.i(TAG, "Card Image \"" + dumpImageRawFilename + "\" of size " + dumpIStream.available() + "B ready for upload.");
        } catch(Exception ioe) {}

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.chameleonUpload(dumpIStream);
                while(!XModem.EOT) {
                    try {
                        Thread.sleep(50);
                    } catch(InterruptedException ie) {
                        break;
                    }
                }
                try {
                    Thread.sleep(1250);
                } catch(InterruptedException ie) {}
                DemoActivity.localInst.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.verifyChameleonUpload(dumpIStream)) {
                            LibraryLogging.i(TAG, "Successfully uploaded card image! :)");
                            sendBroadcast(new Intent("CHAMELEON_UPLOAD_SUCCESS"));
                        } else {
                            LibraryLogging.i(TAG, "Upload operation failed for card image... :(");
                            sendBroadcast(new Intent("CHAMELEON_UPLOAD_FAILURE"));
                        }
                        //removeSystemNotificationIcon(CHAMELEON_UPLOAD_NOTIFY_ID);
                        disableStatusIcon(R.id.statusIconCardDumpUpload);
                    }
                });
            }
        });

    }

    public void actionButtonWriteLogs(View button) {
        LibraryLogging.LogEntry.writeLogsToXMLFile();
        LibraryLogging.LogEntry.writeLogsToPlainTextFile();
    }

    public void actionButtonClearLogs(View button) {
        LibraryLogging.LogEntry.loggingQueue.clear();
        ((LinearLayout) findViewById(R.id.loggingParentView)).removeAllViewsInLayout();
    }

    public void actionButtonInitUSB(View button) {
        if(ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.chameleonPresent()) {
            LibraryLogging.i(TAG, "The chameleon device is connected! :)");
            LibraryLogging.i(TAG, String.join("\n", getChameleonMiniUSBDeviceParams()));
        }
        else {
            LibraryLogging.i(TAG, "Unable to initialize the Chameleon Mini device. :(");
        }
        updateDemoWindowStatusBar();
    }

    public void actionButtonStopLibrary(View button) {
        ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.chameleonUSBInterfaceShutdown();
        updateDemoWindowStatusBar();
    }

    public void actionButtonQueryDevice(View button) {
        if(!ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.isConfigured()) {
            LibraryLogging.i(TAG, "Cannot query the device without a valid device present!");
            return;
        }
        String[] deviceSettings = getChameleonMiniUSBDeviceParams();
        LibraryLogging.i(TAG, String.join("\n", deviceSettings));
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
