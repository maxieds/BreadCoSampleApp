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
import android.graphics.drawable.Drawable;
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

import com.maxieds.chameleonminiusb.ChameleonCommands;
import com.maxieds.chameleonminiusb.ChameleonDeviceConfig;
import com.maxieds.chameleonminiusb.LibraryLogging;
import com.maxieds.chameleonminiusb.Utils;
import com.maxieds.chameleonminiusb.ChameleonLibraryLoggingReceiver;
import com.maxieds.chameleonminiusb.XModem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import static com.maxieds.chameleonminiusb.ChameleonCommands.NODATA;
import static com.maxieds.chameleonminiusb.ChameleonCommands.StandardCommandSet.GET_ACTIVE_SLOT;
import static com.maxieds.chameleonminiusb.ChameleonCommands.StandardCommandSet.GET_MEMORY_SIZE;
import static com.maxieds.chameleonminiusb.ChameleonCommands.StandardCommandSet.GET_UID_SIZE;
import static com.maxieds.chameleonminiusb.ChameleonCommands.StandardCommandSet.QUERY_CONFIG;
import static com.maxieds.chameleonminiusb.ChameleonCommands.StandardCommandSet.QUERY_READONLY;
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

    public static final String PACKAGE_NOTIFY_CHANNELID = "com.maxieds.sampleapp:Channel_01";
    public static final int DEMO_ACTIVITY_NOTIFY_ID = 1;

    public static final int SHORT_PAUSE = 25;
    public static final int MEDIUM_PAUSE = 1250;
    public static final int REDUCED_CMD_TIMEOUT = 500;

    public static final String[] intentBroadcastTypes = new String[] {
            UsbManager.ACTION_USB_DEVICE_ATTACHED,
            UsbManager.ACTION_USB_DEVICE_DETACHED,
            "CHAMELEON_UPLOAD_SUCCESS",
            "CHAMELEON_UPLOAD_FAILURE",
            "CHAMELEON_DOWNLOAD_SUCCESS",
            "CHAMELEON_DOWNLOAD_FAILURE",
    };

    public static final String[] activityPermissions = new String[] {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.INTERNET",
            "com.android.example.USB_PERMISSION",
    };

    public static DemoActivity localInst;
    private static boolean updateStatusBar = true;

    public static void setUpdateStatusBar(boolean enabled) {
        updateStatusBar = enabled;
    }

    public void enableStatusIcon(int iconID, int iconDrawable) {
        ((ImageView) findViewById(iconID)).setAlpha(255);
        ((ImageView) findViewById(iconID)).setImageDrawable(getResources().getDrawable(iconDrawable));
    }

    public void disableStatusIcon(int iconID) {
        ((ImageView) findViewById(iconID)).setAlpha(127);
    }

    public void updateDemoWindowStatusBar() {
        if(!updateStatusBar) {
            return;
        }
        else if(!ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.isConfigured()) {
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
        ChameleonDeviceConfig.SERIAL_USB_COMMAND_TIMEOUT = REDUCED_CMD_TIMEOUT;
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
            }, SHORT_PAUSE);
        }
    }

    private boolean postSystemNotificationIcon(int notifyID, String notifyName, int drawableResID, String notifyAppDesc,
                                               String[] statusMessages, boolean createChannel) {

        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(notifyManager == null) {
            return false;
        }

        if(createChannel) {
            NotificationChannel notifyChannel = new NotificationChannel(PACKAGE_NOTIFY_CHANNELID, notifyName, NotificationManager.IMPORTANCE_LOW);
            notifyChannel.setDescription(notifyAppDesc);
            notifyChannel.enableLights(true);
            notifyChannel.setLightColor(Color.GREEN);
            notifyChannel.enableVibration(true);
            notifyChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            notifyManager.createNotificationChannel(notifyChannel);
        }

        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        if(statusMessages != null) {
            for(int msg = 0; msg < statusMessages.length; msg++) {
                inboxStyle.addLine(statusMessages[msg]);
            }
        }
        Notification notifyIcon = new NotificationCompat.Builder(getApplicationContext())
                .setSmallIcon(drawableResID)
                .setContentTitle(notifyName)
                .setContentText(notifyAppDesc)
                .setOngoing(false)
                .setChannelId(PACKAGE_NOTIFY_CHANNELID)
                .setStyle(inboxStyle)
                .build();

        notifyManager.notify(notifyID, notifyIcon);
        return true;

    }

    private boolean postPackageNotification(int notifyID, String[] statusMessages, boolean createChannel) {
        return postSystemNotificationIcon(notifyID, getString(R.string.app_name), R.drawable.chameleonnotifyicon32,
                getString(R.string.app_desc), statusMessages, createChannel);
    }

    private void removeSystemNotificationIcon(int notifyID) {
        NotificationManager notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(notifyManager != null) {
            notifyManager.cancel(notifyID);
            notifyManager.deleteNotificationChannel(PACKAGE_NOTIFY_CHANNELID);
        }
    }

    public static Runnable updateStatusBarRunnable = new Runnable() {
        public void run() {
            DemoActivity.localInst.updateDemoWindowStatusBar();
            updateStatusBarHandler.postDelayed(this, 4 * SERIAL_USB_COMMAND_TIMEOUT);
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
        Drawable toolbarLogo = getResources().getDrawable(R.drawable.chameleonusb64);
        toolbarLogo.setAlpha(127);
        actionBar.setLogo(toolbarLogo);
        actionBar.setBackground(getResources().getDrawable(R.drawable.status_bar_gradient));
        actionBar.setContentInsetsAbsolute(0, 75);

        // secondary toolbar status icon initial setup:
        enableStatusIcon(R.id.statusIconNoUSB, R.drawable.usbdisconnected16);
        disableStatusIcon(R.id.statusIconUSB);
        disableStatusIcon(R.id.statusIconRevE);
        disableStatusIcon(R.id.statusIconRevG);
        disableStatusIcon(R.id.statusIconCardDumpUpload);
        disableStatusIcon(R.id.statusIconCardDumpDownload);

        // configure misc settings for the running sample app:
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR); // keep app from crashing when the screen rotates
        configureLoggingSpinner();

        // setup and request necessary permissions (ESPECIALLY USB):
        ActivityCompat.requestPermissions(this, activityPermissions, 0);

        // now setup the basic serial port so that we can accept attached USB device connections:
        if(!ChameleonDeviceConfig.usbReceiversRegistered) {
            BroadcastReceiver usbActionReceiver = new BroadcastReceiver() {
                @RequiresPermission("com.android.example.USB_PERMISSION")
                public void onReceive(Context context, Intent intent) {
                    onNewIntent(intent);
                }
            };
            IntentFilter usbActionFilter = new IntentFilter();
            for(int i = 0; i < intentBroadcastTypes.length; i++) {
                usbActionFilter.addAction(intentBroadcastTypes[i]);
            }
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
        updateStatusBarHandler.postDelayed(updateStatusBarRunnable, REDUCED_CMD_TIMEOUT);

        // place a chameleon icon in the system (notifications) tray while the app is running:
        postPackageNotification(DEMO_ACTIVITY_NOTIFY_ID, null, true);

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
            postPackageNotification(DEMO_ACTIVITY_NOTIFY_ID, new String[] { "New Chameleon Mini Device Attached" }, false);
            Utils.sleepThreadMillisecond(MEDIUM_PAUSE);
            updateDemoWindowStatusBar();
        }
        else if(intentAction.equals("ACTION_USB_DEVICE_DETACHED")) {
            ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.onNewIntent(intent);
            postPackageNotification(DEMO_ACTIVITY_NOTIFY_ID, new String[] { "Chameleon Mini Device Detached" }, false);
            Utils.sleepThreadMillisecond(MEDIUM_PAUSE);
            updateDemoWindowStatusBar();
        }
        else if(intentAction.equals("CHAMELEON_UPLOAD_SUCCESS")) {
            String uploadedDumpConfig = intent.getStringExtra("ChameleonConfig");
            String statusMsg = "New " + uploadedDumpConfig + " Configuration Loaded";
            postPackageNotification(DEMO_ACTIVITY_NOTIFY_ID, new String[] { statusMsg }, false);
        }
        else if(intentAction.equals("CHAMELEON_UPLOAD_FAILURE")) {}
        else if(intentAction.equals("CHAMELEON_DOWNLOAD_SUCCESS")) {
            String dumpFilePath = intent.getStringExtra("FilePath");
            int lastFileSlash = dumpFilePath.lastIndexOf("/");
            dumpFilePath = dumpFilePath.substring(lastFileSlash + 1);
            String statusMsg = "Binary Card Dump Downloaded to \"" + dumpFilePath + "\"";
            postPackageNotification(DEMO_ACTIVITY_NOTIFY_ID, new String[] { statusMsg }, false);
        }
        else if(intentAction.equals("CHAMELEON_DOWNLOAD_FAILURE")) {}

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
            LibraryLogging.w(TAG, "Cannot upload a binary dump without a valid device present!");
            return;
        }
        setUpdateStatusBar(false);
        enableStatusIcon(R.id.statusIconCardDumpUpload, R.drawable.uploadstatusicon16);

        // upload the image:
        Spinner dumpImageSpinner = (Spinner) findViewById(R.id.dumpImageSpinner);
        int selectedDumpIndex = dumpImageSpinner.getSelectedItemPosition();
        final String dumpImageFormat = getResources().getStringArray(R.array.sampleCardDumpConfigTypes)[selectedDumpIndex];
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
                        Thread.sleep(2 * SHORT_PAUSE);
                    } catch(InterruptedException ie) {
                        break;
                    }
                }
                try {
                    Thread.sleep(MEDIUM_PAUSE);
                } catch(InterruptedException ie) {}
                DemoActivity.localInst.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.verifyChameleonUpload(dumpIStream)) {
                            LibraryLogging.i(TAG, "Successfully uploaded card image! :)");
                            Intent successIntent = new Intent("CHAMELEON_UPLOAD_SUCCESS");
                            successIntent.putExtra("ChameleonConfig", dumpImageFormat);
                            sendBroadcast(successIntent);
                        } else {
                            LibraryLogging.i(TAG, "Upload operation failed for card image... :(");
                            sendBroadcast(new Intent("CHAMELEON_UPLOAD_FAILURE"));
                        }
                        disableStatusIcon(R.id.statusIconCardDumpUpload);
                        setUpdateStatusBar(true);
                        updateDemoWindowStatusBar();
                    }
                });
            }
        });

    }

    public void actionButtonDownloadByXModem(View button) {

        if(!ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.isConfigured()) {
            LibraryLogging.w(TAG, "Cannot download a binary dump without a valid device present!");
            return;
        }
        setUpdateStatusBar(false);
        enableStatusIcon(R.id.statusIconCardDumpDownload, R.drawable.downloadstatusicon16);

        // get information for the filename:
        File binaryDumpFile;
        try {
            String cardMemSize = sendCommandToChameleon(GET_MEMORY_SIZE, null).cmdResponseData;
            String cardConfig = sendCommandToChameleon(QUERY_CONFIG, null).cmdResponseData.replace("_", "-");
            String cardUID = sendCommandToChameleon(QUERY_UID, null).cmdResponseData.toUpperCase();
            String dumpFileName = String.format(Locale.ENGLISH, "carddump-%s-%s-%sK-%s.bin", cardConfig, cardUID, cardMemSize, Utils.getTimestamp());
            binaryDumpFile = LibraryLogging.createTimestampedLogFile("BinaryCardDumps", dumpFileName);
            Log.i(TAG, "Writing binary card dump of size " + cardMemSize + "K out to file \"" + binaryDumpFile.getAbsolutePath() + "\".");
        } catch(Exception ioe) {
            Log.e(TAG, ioe.getMessage());
            ioe.printStackTrace();
            disableStatusIcon(R.id.statusIconCardDumpDownload);
            setUpdateStatusBar(true);
            return;
        }

        final File binaryDumpFileFinal = binaryDumpFile;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                ChameleonDeviceConfig.THE_CHAMELEON_DEVICE.chameleonDownload(binaryDumpFileFinal);
                while(!XModem.EOT) {
                    try {
                        Thread.sleep(2 * SHORT_PAUSE);
                    } catch(InterruptedException ie) {
                        break;
                    }
                }
                try {
                    Thread.sleep(MEDIUM_PAUSE);
                } catch(InterruptedException ie) {}
                DemoActivity.localInst.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(!XModem.transmissionError()) {
                            Intent downloadSuccessIntent = new Intent("CHAMELEON_DOWNLOAD_SUCCESS");
                            downloadSuccessIntent.putExtra("FilePath", binaryDumpFileFinal.getAbsolutePath());
                            sendBroadcast(downloadSuccessIntent);
                            Log.i(TAG, "Successfully downloaded binary card dump to file.");
                        }
                        else {
                            Intent downloadFailureIntent = new Intent("CHAMELEON_DOWNLOAD_FAILURE");
                            sendBroadcast(downloadFailureIntent);
                            Log.i(TAG, "Failed to download binary card dump to file.");
                        }
                        disableStatusIcon(R.id.statusIconCardDumpDownload);
                        setUpdateStatusBar(true);
                        updateDemoWindowStatusBar();
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
        try {
            int readOnlySetting = Integer.parseInt(ChameleonDeviceConfig.sendCommandToChameleon(QUERY_READONLY, null).cmdResponseData);
            int nextReadOnlySetting = (readOnlySetting + 1) % 2;
            String nextROStatus = (nextReadOnlySetting == 0) ? "Read-Write" : "Read-Only";
            ChameleonCommands.ChameleonCommandResult roCmdResult = sendCommandToChameleon(SET_CONFIG, nextReadOnlySetting);
            if(roCmdResult.isValid) {
                Log.i(TAG, "Successfully set active slot status to " + nextROStatus + ".");
            }
            else {
                Log.i(TAG, "Unable to toggle active slot setting to " + nextROStatus + ": " + roCmdResult.cmdResponseMsg);
            }
        } catch(Exception nfe) {
            Log.e(TAG, "Unable to change RO/RW status of the active slot: " + nfe.getMessage());
        }
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
