package org.inline.listle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.inline.listle.PagesStore.Page;
import org.inline.listle.R;

import java.util.List;

public class PagesActivity extends Activity {
    private static final String PREFS_NAME = "ListlePrefs";
    private static final String KEY_THEME_LIGHT = "theme_light";
    private static final boolean DEFAULT_THEME_LIGHT = true;
    private static final int ACTION_ADD = 1;

    private ListView pagesListView;
    private SharedPreferences prefs;
    private List<Page> pages;
    private PageAdapter adapter;
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
        setContentView(R.layout.activity_pages);

        pages = PagesStore.load(prefs);

        pagesListView = (ListView) findViewById(R.id.pages_list);
        adapter = new PageAdapter();
        pagesListView.setAdapter(adapter);

        applyTheme();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuItemAdd = menu.add(Menu.NONE, ACTION_ADD, 1, getString(R.string.add_page));
        menuItemAdd.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == ACTION_ADD) {
            showAddPageDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int tintColor() {
        return isThemeLight ? Color.parseColor("#0D0D0D") : Color.parseColor("#F2F2F2");
    }

    private void saveAndRefresh() {
        PagesStore.save(prefs, pages);
        adapter.notifyDataSetChanged();
    }

    private void showAddPageDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_page);

        final EditText input = new EditText(this);
        input.setHint(R.string.enter_page_name);
        builder.setView(input);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    pages.add(new Page(name));
                    saveAndRefresh();
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showRenamePageDialog(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.rename_page);

        final EditText input = new EditText(this);
        input.setText(pages.get(position).name);
        builder.setView(input);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    pages.get(position).name = name;
                    saveAndRefresh();
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void movePage(int fromPos, int toPos) {
        if (fromPos < 0 || toPos < 0 || fromPos >= pages.size() || toPos >= pages.size()) {
            return;
        }
        Page page = pages.remove(fromPos);
        pages.add(toPos, page);
        saveAndRefresh();
    }

    private void showDeletePageConfirmation(final int position) {
        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_page_confirmation)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pages.remove(position);
                        if (pages.isEmpty()) {
                            pages.add(new Page(PagesStore.DEFAULT_PAGE_NAME));
                        }
                        saveAndRefresh();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private class PageAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return pages.size();
        }

        @Override
        public Object getItem(int position) {
            return pages.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(PagesActivity.this)
                        .inflate(R.layout.page_item, parent, false);
            }

            Page page = pages.get(position);

            TextView nameView = (TextView) convertView.findViewById(R.id.page_name);
            nameView.setText(page.name);
            nameView.setTextColor(tintColor());

            ImageView editIcon = (ImageView) convertView.findViewById(R.id.page_edit);
            editIcon.setImageResource(R.drawable.pencil);
            editIcon.setColorFilter(tintColor(), PorterDuff.Mode.SRC_IN);
            editIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showRenamePageDialog(position);
                }
            });

            ImageView upIcon = (ImageView) convertView.findViewById(R.id.page_up);
            upIcon.setImageResource(R.drawable.arrow_up);
            upIcon.setColorFilter(tintColor(), PorterDuff.Mode.SRC_IN);
            upIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    movePage(position, position - 1);
                }
            });

            ImageView downIcon = (ImageView) convertView.findViewById(R.id.page_down);
            downIcon.setImageResource(R.drawable.arrow_down);
            downIcon.setColorFilter(tintColor(), PorterDuff.Mode.SRC_IN);
            downIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    movePage(position, position + 1);
                }
            });

            ImageView deleteIcon = (ImageView) convertView.findViewById(R.id.page_delete);
            deleteIcon.setImageResource(R.drawable.trash_can);
            deleteIcon.setColorFilter(tintColor(), PorterDuff.Mode.SRC_IN);
            deleteIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDeletePageConfirmation(position);
                }
            });

            return convertView;
        }
    }

    private void applyTheme() {
        int bgColor;
        if (isThemeLight) {
            pagesListView.setBackgroundResource(R.drawable.gradient_light);
            bgColor = Color.parseColor("#F7F7F7");
        } else {
            pagesListView.setBackgroundResource(R.drawable.gradient_dark);
            bgColor = Color.parseColor("#080808");
        }
        pagesListView.setBackgroundColor(bgColor);
    }
}
