package org.inline.listle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;

import org.inline.listle.R;
import org.inline.listle.theme.UiTheme;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class OptionsActivity extends Activity {
    private static final String PREFS_NAME = "ListlePrefs";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_VERTICAL_INTERVAL = "vertical_interval";
    private static final String KEY_THEME_LIGHT = "theme_light";
    private static final String KEY_ITEMS = "items";
    private static final int DEFAULT_FONT_SIZE = 22;
    private static final int DEFAULT_VERTICAL_INTERVAL = 8;
    private static final boolean DEFAULT_THEME_LIGHT = true;
    private static final int CREATE_FILE_REQUEST_CODE = 100;
    private static final int OPEN_FILE_REQUEST_CODE = 101;

    private ListView optionsListView;
    private SharedPreferences prefs;
    private int fontSize = DEFAULT_FONT_SIZE;
    private int verticalInterval = DEFAULT_VERTICAL_INTERVAL;
    private UiTheme theme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        theme = UiTheme.from(prefs.getBoolean(KEY_THEME_LIGHT, DEFAULT_THEME_LIGHT));
        setTheme(theme.getThemeResId());

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        loadPreferences();

        optionsListView = (ListView) findViewById(R.id.options_list);
        ArrayAdapter<String> optionsAdapter = new ArrayAdapter<String>(this,
                R.layout.options_item,
                new String[]{
                        getString(R.string.change_font_size),
                        getString(R.string.change_vertical_interval),
                        getString(R.string.theme),
                        getString(R.string.save_list),
                        getString(R.string.load_list)
                }) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                textView.setTextColor(theme.getTextColor());
                return view;
            }
        };
        optionsListView.setAdapter(optionsAdapter);

        setupListeners();
        applyTheme();
    }

    private void setupListeners() {
        optionsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                handleOptionClick(position);
            }
        });
    }

    private void handleOptionClick(int position) {
        switch (position) {
            case 0:
                showFontSizeDialog();
                break;
            case 1:
                showVerticalIntervalDialog();
                break;
            case 2:
                showThemeDialog();
                break;
            case 3:
                saveList();
                break;
            case 4:
                loadList();
                break;
        }
    }

    private void showFontSizeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.change_font_size);

        final NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(10);
        picker.setMaxValue(40);
        picker.setValue(fontSize);
        builder.setView(picker);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                fontSize = picker.getValue();
                savePreferences();
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showVerticalIntervalDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.change_vertical_interval);

        final NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(0);
        picker.setMaxValue(30);
        picker.setValue(verticalInterval);
        builder.setView(picker);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                verticalInterval = picker.getValue();
                savePreferences();
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showThemeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.theme);

        final String[] themes = {getString(R.string.light), getString(R.string.dark)};
        int selectedIndex = theme.isLight() ? 0 : 1;

        builder.setSingleChoiceItems(themes, selectedIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                theme = (which == 0) ? UiTheme.light() : UiTheme.dark();
                savePreferences();
                recreate();
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void applyTheme() {
        optionsListView.setBackgroundResource(theme.getGradientResId());
        optionsListView.setBackgroundColor(theme.getBackgroundColor());
    }

    private void saveList() {
        openFileSaveDialog("listle.json");
    }

    private void openFileSaveDialog(String defaultFileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, defaultFileName);
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
    }

    private void loadList() {
        openFilePicker();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        startActivityForResult(intent, OPEN_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri fileUri = data.getData();
                saveJsonToFile(fileUri);
            }
        } else if (requestCode == OPEN_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri fileUri = data.getData();
                loadJsonFromFile(fileUri);
            }
        }
    }

    private void saveJsonToFile(Uri uri) {
        try {
            String itemsJson = prefs.getString(KEY_ITEMS, null);
            if (itemsJson == null || itemsJson.isEmpty()) {
                new AlertDialog.Builder(this)
                        .setMessage("No items to save")
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return;
            }

            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                outputStream.write(itemsJson.getBytes());
                outputStream.close();
            }

            new AlertDialog.Builder(this)
                    .setMessage("List saved successfully")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
            new AlertDialog.Builder(this)
                    .setMessage("Error saving list: " + e.getMessage())
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private void loadJsonFromFile(Uri uri) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line).append("\n");
                }
                inputStream.close();
            }

            String jsonContent = stringBuilder.toString();
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_ITEMS, jsonContent);
            editor.apply();

            new AlertDialog.Builder(this)
                    .setMessage("List loaded successfully. Please return to main activity to see changes.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
            new AlertDialog.Builder(this)
                    .setMessage("Error loading list: " + e.getMessage())
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    private void loadPreferences() {
        fontSize = prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
        verticalInterval = prefs.getInt(KEY_VERTICAL_INTERVAL, DEFAULT_VERTICAL_INTERVAL);
        theme = UiTheme.from(prefs.getBoolean(KEY_THEME_LIGHT, DEFAULT_THEME_LIGHT));
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_FONT_SIZE, fontSize);
        editor.putInt(KEY_VERTICAL_INTERVAL, verticalInterval);
        editor.putBoolean(KEY_THEME_LIGHT, theme.isLight());
        editor.apply();
    }
}
