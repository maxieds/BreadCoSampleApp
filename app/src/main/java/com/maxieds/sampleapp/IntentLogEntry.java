package com.maxieds.sampleapp;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.v4.graphics.ColorUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;
import java.util.Random;

public class IntentLogEntry {

    private static final String TAG = IntentLogEntry.class.getSimpleName();

    private static int[] logEntryIcons = new int[] {
            R.drawable.intentlogicon1,
            R.drawable.intentlogicon2,
            R.drawable.intentlogicon3,
            R.drawable.intentlogicon4,
            R.drawable.intentlogicon5,
    };

    public static Drawable getNextLoggingIcon() {
        return DemoActivity.localInst.getResources().getDrawable(logEntryIcons[loggingTextBoxColorIdx % logEntryIcons.length]);
    }

    public static final int COLOR_BASE = 0;
    public static final int COLOR_HIGHLIGHT = 1;
    public static final int COLOR_TITLE = 2;

    private static int loggingTextBoxColorIdx = 0;
    private static int[] loggingTextBoxColors = new int[] {
            R.color.rainbow1,
            R.color.rainbow2,
            R.color.rainbow3,
            R.color.rainbow4,
            R.color.rainbow5,
            R.color.rainbow6,
            R.color.rainbow7,
            R.color.rainbow8,
            R.color.rainbow9,
            R.color.rainbow10,
            R.color.rainbow11,
            R.color.rainbow12,
            R.color.rainbow13,
    };

    public static int[] getNextLoggingColorSet() {
        int baseColor = loggingTextBoxColors[loggingTextBoxColorIdx];
        int lighterHighlightColor = ColorUtils.blendARGB(DemoActivity.localInst.getResources().getColor(baseColor), Color.WHITE, 0.42f);
        int darkerTitleColor = ColorUtils.blendARGB(DemoActivity.localInst.getResources().getColor(baseColor), Color.BLACK, 0.42f);
        loggingTextBoxColorIdx = ++loggingTextBoxColorIdx % loggingTextBoxColors.length;
        return new int[] {
                baseColor,
                lighterHighlightColor + 0xAA000000,
                darkerTitleColor + 0xAA000000,
        };
    }

    public static LinearLayout getLogEntryInstance(Intent intentLog) {

        Log.i(TAG, "UniqueLogID: " + intentLog.getLongExtra("UniqueLogID", 0L));
        Log.i(TAG, "Timestamp: " + intentLog.getStringExtra("Timestamp"));
        Log.i(TAG, "Timestamp: " + intentLog.getStringExtra("LogSeverity"));
        Log.i(TAG, "SourceCodeRefs: " + intentLog.getStringExtra("SourceCodeRefs"));
        Log.i(TAG, "MessageData: \"" + String.join(", \"", intentLog.getStringArrayExtra("MessageData")) + "\"");

        LayoutInflater layoutInflater = (LayoutInflater) DemoActivity.localInst.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout blankLogEntry = (LinearLayout) layoutInflater.inflate(R.layout.intent_log_entry, null);
        int[] colorScheme = getNextLoggingColorSet();
        Drawable logEntryIcon = getNextLoggingIcon();

        ((View) blankLogEntry.findViewById(R.id.border1)).setBackgroundColor(colorScheme[COLOR_HIGHLIGHT]);
        ((View) blankLogEntry.findViewById(R.id.border2)).setBackgroundColor(colorScheme[COLOR_HIGHLIGHT]);
        ((View) blankLogEntry.findViewById(R.id.border3)).setBackgroundColor(colorScheme[COLOR_HIGHLIGHT]);
        ((View) blankLogEntry.findViewById(R.id.border4)).setBackgroundColor(colorScheme[COLOR_HIGHLIGHT]);

        TextView upperTitle = (TextView) blankLogEntry.findViewById(R.id.upperTitle);
        upperTitle.setBackgroundColor(colorScheme[COLOR_TITLE]);
        upperTitle.setCompoundDrawablesWithIntrinsicBounds(logEntryIcon, null, null, null);
        String topMsg = String.format(Locale.ENGLISH, "LOG #%04X -- %s -- %s", intentLog.getLongExtra("UniqueLogID", 0L),
                intentLog.getStringExtra("Timestamp"), intentLog.getStringExtra("LogSeverity"));
        upperTitle.setText(topMsg);
        upperTitle.setEnabled(true);
        upperTitle.setVisibility(View.VISIBLE);

        TextView lowerTitle = (TextView) blankLogEntry.findViewById(R.id.lowerTitle);
        lowerTitle.setBackgroundColor(colorScheme[COLOR_TITLE]);
        lowerTitle.setText("Stack Trace: " + intentLog.getStringExtra("SourceCodeRefs"));
        upperTitle.setEnabled(true);
        upperTitle.setVisibility(View.VISIBLE);

        TextView msgDataField = (TextView) blankLogEntry.findViewById(R.id.textContent);
        msgDataField.setBackgroundResource(colorScheme[COLOR_BASE]);
        msgDataField.setText(String.join("\n", intentLog.getStringArrayExtra("MessageData")));
        msgDataField.setTextColor(colorScheme[COLOR_TITLE]);
        upperTitle.setEnabled(true);
        upperTitle.setVisibility(View.VISIBLE);

        return blankLogEntry;

    }

    public static void postNewIntentLog(Intent intentLog) {
        LinearLayout ilogLayout = getLogEntryInstance(intentLog);
        LinearLayout loggingParentView = (LinearLayout) DemoActivity.localInst.findViewById(R.id.loggingParentView);
        loggingParentView.addView(ilogLayout);
        loggingParentView.getChildAt(loggingParentView.getChildCount() - 1).setVisibility(View.VISIBLE);
        // TODO: later, can add in code to scroll to the bottom of the window as the new logs are added.
    }

}
