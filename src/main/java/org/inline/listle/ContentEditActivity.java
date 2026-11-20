package org.inline.listle;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.inline.listle.PagesStore.Page;
import org.inline.listle.R;

import java.util.ArrayList;
import java.util.List;

public class ContentEditActivity extends Activity {
    public static final String EXTRA_PAGE_INDEX = "page_index";

    private static final String PREFS_NAME = "ListlePrefs";
    private static final String KEY_THEME_LIGHT = "theme_light";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final int DEFAULT_FONT_SIZE = 22;
    private static final boolean DEFAULT_THEME_LIGHT = true;

    private SharedPreferences prefs;
    private List<Page> pages;
    private int pageIndex;
    private EditText editText;
    private boolean isThemeLight = DEFAULT_THEME_LIGHT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isThemeLight = prefs.getBoolean(KEY_THEME_LIGHT, DEFAULT_THEME_LIGHT);

        if (isThemeLight) {
            setTheme(R.style.AppTheme);
        } else {
            setTheme(R.style.AppTheme_Dark);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_edit);

        pageIndex = getIntent().getIntExtra(EXTRA_PAGE_INDEX, 0);
        pages = PagesStore.load(prefs);
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            pageIndex = 0;
        }

        editText = (EditText) findViewById(R.id.content_edit_text);
        editText.setText(buildContent(pages.get(pageIndex)));

        int fontSize = prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize);

        Button btnSave = (Button) findViewById(R.id.btn_save);
        Button btnCancel = (Button) findViewById(R.id.btn_cancel);
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveContent();
                finish();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        applyTheme();
    }

    private String buildContent(Page page) {
        StringBuilder sb = new StringBuilder();
        for (ListleItem item : page.items) {
            sb.append(PagesStore.PRIORITIES[item.getPriority()])
                    .append(' ')
                    .append(item.getName())
                    .append('\n');
        }
        return sb.toString();
    }

    private void saveContent() {
        Page page = pages.get(pageIndex);
        List<ListleItem> newItems = new ArrayList<>();
        String[] lines = editText.getText().toString().split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String name = trimmed;
            int priorityIndex = PagesStore.DEFAULT_PRIORITY_INDEX;
            String[] parts = trimmed.split("\\s+", 2);
            if (parts.length == 2) {
                int markIndex = PagesStore.getPriorityIndex(parts[0]);
                if (markIndex >= 0) {
                    priorityIndex = markIndex;
                    name = parts[1].trim();
                }
            }
            if (name.isEmpty()) {
                continue;
            }
            newItems.add(new ListleItem(name, priorityIndex));
        }
        page.items.clear();
        page.items.addAll(newItems);
        PagesStore.save(prefs, pages);
    }

    private void applyTheme() {
        int textColor = isThemeLight ? Color.parseColor("#0D0D0D") : Color.parseColor("#F2F2F2");
        editText.setTextColor(textColor);
        View root = findViewById(R.id.content_edit_root);
        if (isThemeLight) {
            root.setBackgroundResource(R.drawable.gradient_light);
        } else {
            root.setBackgroundResource(R.drawable.gradient_dark);
        }
    }
}
