package com.maxieds.sampleapp;

import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import android.view.View;
import android.widget.LinearLayout;

import java.util.Random;

public class IntentLogEntry {

    private static int[] logEntryIcons = new int[] {
            R.drawable.intentlogicon1,
            R.drawable.intentlogicon2,
            R.drawable.intentlogicon3,
            R.drawable.intentlogicon4,
            R.drawable.intentlogicon5,
    };

    public static Drawable getNextLoggingIcon() {
        Random rnGen = new Random(System.currentTimeMillis());
        int randomIndex = rnGen.nextInt(0xff) % logEntryIcons.length;
        return DemoActivity.localInst.getResources().getDrawable(logEntryIcons[randomIndex]);
    }

    public static final int COLOR_BASE = 0;
    public static final int COLOR_HIGHLIGHT = 1;
    public static final int COLOR_TITLE = 2;

    private static int loggingTextBoxColorIdx = 0;

    public static Color[] getNextLoggingColorSet() {

        TypedArray allColorChoices = DemoActivity.localInst.getResources().obtainTypedArray(R.array.loggingBoxHighlightColors);
        int baseColorSpec = allColorChoices.getColor(loggingTextBoxColorIdx, 0);
        Color baseColor = Color.valueOf(baseColorSpec);
        Color lighterHighlightColor = Color.valueOf(ColorUtils.blendARGB(baseColorSpec, Color.WHITE, 0.42f));
        Color darkerTitleColor = Color.valueOf(ColorUtils.blendARGB(baseColorSpec, Color.BLACK, 0.42f));
        loggingTextBoxColorIdx = ++loggingTextBoxColorIdx % allColorChoices.length();
        allColorChoices.recycle();
        return new Color[] {
                baseColor,
                lighterHighlightColor,
                darkerTitleColor,
        };

    }

    public static LinearLayout getLogEntryInstance(Intent intentLog) {

        LinearLayout blankLogEntry = (LinearLayout) DemoActivity.localInst.getLayoutInflater().inflate(R.layout.intent_log_entry, null);
        Color[] colorScheme = getNextLoggingColorSet();
        Drawable logEntryIcon = getNextLoggingIcon();

        ((View) blankLogEntry.findViewById(R.id.border1)).setBackgroundColor(colorScheme[COLOR_HIGHLIGHT].toArgb());
        ((View) blankLogEntry.findViewById(R.id.border2)).setBackgroundColor(colorScheme[COLOR_HIGHLIGHT].toArgb());
        ((View) blankLogEntry.findViewById(R.id.border3)).setBackgroundColor(colorScheme[COLOR_HIGHLIGHT].toArgb());
        ((View) blankLogEntry.findViewById(R.id.border4)).setBackgroundColor(colorScheme[COLOR_HIGHLIGHT].toArgb());


        return blankLogEntry;

    }



}
