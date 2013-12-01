package me.piebridge.bible;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.widget.SeekBar;

public class Settings extends PreferenceActivity implements OnPreferenceChangeListener {

    private static Bible bible = null;

    public static String RED = "red";
    public static String FONTSIZE = "fontsize";
    public static String NIGHTMODE = "nightmode";

    private String body;

    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        setPreferenceScreen(createPreferenceHierarchy(root));
        Intent intent = getIntent();
        body = intent.getStringExtra("body");
    }

    private PreferenceScreen createPreferenceHierarchy(PreferenceScreen root) {
        root.addPreference(addPreference(FONTSIZE, R.string.fontsize));
        root.addPreference(addBooleanPreference(RED, R.string.red, R.string.wojinred));
        root.addPreference(addBooleanPreference(NIGHTMODE, R.string.nightmode, 0));
        return root;
    }

    private Preference addPreference(String key, int title) {
        Preference preference = new Preference(this);
        preference.setKey(key);
        preference.setTitle(title);
        if (FONTSIZE.equals(key)) {
            if (bible == null) {
                bible = Bible.getBible(getBaseContext());
            }
            preference.setSummary(getString(R.string.fontsummary, getInt(FONTSIZE, Chapter.FONTSIZE_MED),
                    bible.getVersionName(bible.getVersion())));
        }
        return preference;
    }

    private Preference addBooleanPreference(String key, int title, int summary) {
        Preference preference;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            preference = getBooleanPreference();
        } else {
            preference = getBooleanPreferenceGB();
        }
        preference.setKey(key);
        preference.setTitle(title);
        if (summary != 0) {
            preference.setSummary(summary);
        }
        preference.setOnPreferenceChangeListener(this);
        switch (title) {
        case R.string.red:
            preference.setDefaultValue(getBoolean(key, true));
            break;
        case R.string.nightmode:
            preference.setDefaultValue(getBoolean(key, false));
            break;
        }
        return preference;
    }

    Preference getBooleanPreferenceGB() {
        return new CheckBoxPreference(this);
    }

    @SuppressLint("NewApi")
    Preference getBooleanPreference() {
        return new SwitchPreference(this);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (newValue instanceof Boolean) {
            setBoolean(key, (Boolean) newValue);
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, final Preference preference) {
        final String key = preference.getKey();
        if (FONTSIZE.equals(key)) {
            setupFontDialog(preference);
        }
        return true;
    }

    private void setupFontDialog(final Preference preference) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog, null);

        final SeekBar seekbar = (SeekBar) view.findViewById(R.id.seekbar);
        final WebView webview = (WebView) view.findViewById(R.id.webview);

        seekbar.setMax(Chapter.FONTSIZE_MAX);
        seekbar.setProgress(getInt(FONTSIZE, Chapter.FONTSIZE_MED));

        body = body.replaceFirst("font-size:\\s*\\d+pt", "font-size: " + seekbar.getProgress() + "pt");
        webview.loadDataWithBaseURL("file:///android_asset/", body, "text/html", "utf-8", null);
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < Chapter.FONTSIZE_MIN) {
                    progress = Chapter.FONTSIZE_MIN;
                }
                body = body.replaceFirst("font-size:\\s*\\d+pt", "font-size: " + progress + "pt");
                webview.loadDataWithBaseURL("file:///android_asset/", body, "text/html", "utf-8", null);
            }
        });

        new AlertDialog.Builder(this)
            .setTitle(R.string.fontsize)
            .setView(view)
            .setPositiveButton(getText(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int fontsize = seekbar.getProgress();
                        if (fontsize < Chapter.FONTSIZE_MIN) {
                            fontsize = Chapter.FONTSIZE_MIN;
                        }
                        setInt(FONTSIZE, fontsize);
                        preference.setSummary(getString(R.string.fontsummary, fontsize,
                                bible.getVersionName(bible.getVersion())));
                    }

                })
                .setNegativeButton(getText(android.R.string.no), null).show();
    }

    private int getInt(String key, int defValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (FONTSIZE.equals(key)) {
            if (bible == null) {
                bible = Bible.getBible(getBaseContext());
            }
            String version = bible.getVersion();
            int fontsize = sp.getInt(FONTSIZE + "-" + version, 0);
            if (fontsize == 0) {
                return sp.getInt(FONTSIZE, defValue);
            }
            return fontsize;
        }
        return sp.getInt(key, defValue);
    }

    private boolean getBoolean(String key, boolean defValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        return sp.getBoolean(key, defValue);
    }

    private void setInt(String key, int value) {
        final Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putInt(key, value);
        if (FONTSIZE.equals(key)) {
            if (bible == null) {
                bible = Bible.getBible(getBaseContext());
            }
            String version = bible.getVersion();
            editor.putInt(key + "-" + version, value);
            editor.putInt(key, value);
        }
        editor.commit();
    }

    private void setBoolean(String key, boolean defValue) {
        final Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean(key, defValue);
        editor.commit();
    }
}
