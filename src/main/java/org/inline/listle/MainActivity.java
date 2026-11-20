package org.inline.listle;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.ListView;
import android.widget.TextView;

import org.inline.listle.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private static final String[] PRIORITIES = PagesStore.PRIORITIES;
    private static final int DEFAULT_PRIORITY_INDEX = PagesStore.DEFAULT_PRIORITY_INDEX;
    private static final int DEFAULT_FONT_SIZE = 22;
    private static final int DEFAULT_VERTICAL_INTERVAL = 8;
    private static final boolean DEFAULT_THEME_LIGHT = true;
    private static final String PREFS_NAME = "ListlePrefs";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_VERTICAL_INTERVAL = "vertical_interval";
    private static final String KEY_THEME_LIGHT = "theme_light";
    private static final String KEY_CURRENT_PAGE = "current_page";
    private static final int ACTION_ADD = 1;
    private static final int ACTION_EDIT = 2;
    private static final int ACTION_SORT = 3;
    private static final int ACTION_OPTIONS = 4;
    private static final int ACTION_MOVE_UP = 5;
    private static final int ACTION_MOVE_DOWN = 6;
    private static final int ACTION_TOGGLE_EDIT_BAR = 7;

    private ListView itemListView;
    private ListleAdapter adapter;
    private List<ListleItem> items;
    private List<ListleItem> originalItems;
    private int fontSize = DEFAULT_FONT_SIZE;
    private int verticalInterval = DEFAULT_VERTICAL_INTERVAL;
    private boolean isThemeLight = DEFAULT_THEME_LIGHT;
    private boolean isPrioMode = false;
    private boolean isMoveMode = false;
    private boolean isEditBarVisible = false;
    private ListleItem selectedItem = null;
    private List<PagesStore.Page> pages;
    private int currentPageIndex = 0;
    private TextView titleView;

    private SharedPreferences prefs;
    private View editBarView;

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
        setContentView(R.layout.activity_main);

        items = new ArrayList<>();
        originalItems = new ArrayList<>();

        loadPreferences();

        itemListView = (ListView) findViewById(R.id.item_list);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setCustomView(getTitleView(),
                    new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        }
        adapter = new ListleAdapter(this, items, fontSize, verticalInterval, PRIORITIES);
        adapter.setOnStatusClickListener(new ListleAdapter.OnStatusClickListener() {
            @Override
            public void onStatusClick(int position) {
                if (isMoveMode) {
                    // Turn off move mode when any item is selected
                    isMoveMode = false;
                    adapter.setMovePosition(-1);
                    invalidateOptionsMenu();
                }
                selectedItem = items.get(position);
                adapter.setSelectedPosition(position);
                showPriorityDialog();
            }
        });

        adapter.setOnItemLongClickListener(new ListleAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(int position) {
                if (isMoveMode) {
                    // Turn off move mode when any item is selected
                    isMoveMode = false;
                    adapter.setMovePosition(-1);
                    invalidateOptionsMenu();
                }
                selectedItem = items.get(position);
                adapter.setSelectedPosition(position);
                showPriorityDialog();
            }
        });

        adapter.setOnItemClickListener(new ListleAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                if (isMoveMode) {
                    // Turn off move mode when any item is selected
                    isMoveMode = false;
                    adapter.setMovePosition(-1);
                    invalidateOptionsMenu();
                }
                selectedItem = items.get(position);
                adapter.setSelectedPosition(position);
            }
        });

        itemListView.setAdapter(adapter);
        itemListView.setDivider(null);
        itemListView.setDividerHeight(0);
        updateDividerHeight();

        loadPages();

        applyTheme();

        setupListeners();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuItemEdit = menu.add(Menu.NONE, ACTION_EDIT,
                isEditBarVisible ? 10 : 1,
                isEditBarVisible ? "Close" : "Edit");
        menuItemEdit.setIcon(isEditBarVisible ? R.drawable.close : R.drawable.pencil);
        menuItemEdit.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        if (!isEditBarVisible) {
            MenuItem menuItemSort = menu.add(Menu.NONE, ACTION_SORT, 2, "Sort");
            menuItemSort.setIcon(isPrioMode ? R.drawable.sort_active : R.drawable.sort_inactive);
            menuItemSort.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            MenuItem menuItemOptions = menu.add(Menu.NONE, ACTION_OPTIONS, 3, "Options");
            menuItemOptions.setIcon(R.drawable.dots_vertical_circle);
            menuItemOptions.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        // Add move up/down buttons only when in move mode
        if (isMoveMode) {
            MenuItem menuItemMoveUp = menu.add(Menu.NONE, ACTION_MOVE_UP, 4, "↑");
            menuItemMoveUp.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            MenuItem menuItemMoveDown = menu.add(Menu.NONE, ACTION_MOVE_DOWN, 5, "↓");
            menuItemMoveDown.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }

        int tintColor = isThemeLight ? Color.parseColor("#0D0D0D") : Color.parseColor("#F2F2F2");

        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.getIcon() != null) {
                item.getIcon().setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == ACTION_ADD) {
            showAddItemDialog();
            return true;
        } else if (id == ACTION_EDIT) {
            toggleEditBar();
            return true;
        } else if (id == ACTION_SORT) {
            isPrioMode = !isPrioMode;
            // Exit move mode when sort changes
            if (isMoveMode) {
                isMoveMode = false;
                adapter.setMovePosition(-1);
                invalidateOptionsMenu();
            }
            applySorting();
            updateSelection();
            invalidateOptionsMenu(); // Redraw menu to update sort icon color
            return true;
        } else if (id == ACTION_OPTIONS) {
            showOptionsPopup(item);
            return true;
        } else if (id == ACTION_MOVE_UP) {
            if (selectedItem != null) {
                int position = items.indexOf(selectedItem);
                if (position > 0) {
                    moveItem(position, position - 1);
                    adapter.setMovePosition(position - 1);
                    adapter.updateItems(items);
                }
            }
            return true;
        } else if (id == ACTION_MOVE_DOWN) {
            if (selectedItem != null) {
                int position = items.indexOf(selectedItem);
                if (position < items.size() - 1) {
                    moveItem(position, position + 1);
                    adapter.setMovePosition(position + 1);
                    adapter.updateItems(items);
                }
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePreferences();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPreferences();
        loadPages();
        adapter.setFontSize(fontSize);
        adapter.setVerticalInterval(verticalInterval);
        updateDividerHeight();
        applyTheme();
    }

    private void showOptionsPopup(final MenuItem item) {
        final String[] options = {getString(R.string.pages), getString(R.string.search),
                getString(R.string.options)};
        final int[] icons = {R.drawable.pages, R.drawable.text_search, R.drawable.tune_variant};
        View anchor = findMenuItemView(item.getItemId());
        final ListPopupWindow popup = new ListPopupWindow(this);
        if (anchor != null) {
            popup.setAnchorView(anchor);
        }
        popup.setDropDownGravity(Gravity.END);
        popup.setHeight(ListPopupWindow.WRAP_CONTENT);
        popup.setModal(true);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.popup_menu_item, android.R.id.text1, options) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ImageView icon = (ImageView) view.findViewById(R.id.popup_icon);
                icon.setImageResource(icons[position]);
                TextView text = (TextView) view.findViewById(android.R.id.text1);
                text.setTextColor(isThemeLight
                        ? Color.parseColor("#0D0D0D") : Color.parseColor("#F2F2F2"));
                return view;
            }
        };
        popup.setAdapter(adapter);

        int maxWidth = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View row = adapter.getView(i, null, null);
            row.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            maxWidth = Math.max(maxWidth, row.getMeasuredWidth());
        }
        popup.setWidth(maxWidth);
        popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                popup.dismiss();
                if (position == 0) {
                    Intent intent = new Intent(MainActivity.this, PagesActivity.class);
                    startActivity(intent);
                } else if (position == 1) {
                    showSearchDialog();
                } else if (position == 2) {
                    Intent intent = new Intent(MainActivity.this, OptionsActivity.class);
                    startActivity(intent);
                }
            }
        });
        popup.show();
    }

    private View findMenuItemView(int id) {
        View view = findViewById(id);
        if (view != null) {
            return view;
        }
        return findViewByIdRecursive(getWindow().getDecorView(), id);
    }

    private View findViewByIdRecursive(View view, int id) {
        if (view == null) {
            return null;
        }
        if (view.getId() == id) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findViewByIdRecursive(group.getChildAt(i), id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.search);

        final EditText input = new EditText(this);
        input.setHint(R.string.enter_search_string);
        builder.setView(input);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String query = input.getText().toString().trim().toLowerCase();
                if (query.isEmpty()) {
                    return;
                }
                int start = 0;
                if (selectedItem != null) {
                    start = items.indexOf(selectedItem) + 1;
                }
                for (int k = 0; k < items.size(); k++) {
                    int i = (start + k) % items.size();
                    String name = items.get(i).getName();
                    if (name != null && name.toLowerCase().contains(query)) {
                        selectedItem = items.get(i);
                        adapter.setSelectedPosition(i);
                        ensureItemVisible(i);
                        return;
                    }
                }
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(R.string.item_not_found)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void setupListeners() {
    }

    private void setupEditBarListeners(View root) {
        ImageButton btnAdd = (ImageButton) root.findViewById(R.id.btn_add);
        ImageButton btnRename = (ImageButton) root.findViewById(R.id.btn_rename);
        ImageButton btnMoveUp = (ImageButton) root.findViewById(R.id.btn_move_up);
        ImageButton btnMoveDown = (ImageButton) root.findViewById(R.id.btn_move_down);
        ImageButton btnDelete = (ImageButton) root.findViewById(R.id.btn_delete);

        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddItemDialog();
            }
        });

        btnRename.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedItem == null) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(R.string.no_item_selected)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    return;
                }
                showRenameDialog();
            }
        });

        btnMoveUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedItem != null) {
                    int position = items.indexOf(selectedItem);
                    if (position > 0) {
                        moveItem(position, position - 1);
                        adapter.setMovePosition(position - 1);
                        adapter.updateItems(items);
                    }
                }
            }
        });

        btnMoveDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedItem != null) {
                    int position = items.indexOf(selectedItem);
                    if (position < items.size() - 1) {
                        moveItem(position, position + 1);
                        adapter.setMovePosition(position + 1);
                        adapter.updateItems(items);
                    }
                }
            }
        });

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedItem == null) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage(R.string.no_item_selected)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    return;
                }
                showDeleteConfirmation();
            }
        });
    }

    private void toggleEditBar() {
        isEditBarVisible = !isEditBarVisible;
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            if (isEditBarVisible) {
                editBarView = LayoutInflater.from(this).inflate(R.layout.edit_bar, null);
                setupEditBarListeners(editBarView);
                applyEditBarTint();
                actionBar.setCustomView(editBarView,
                        new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));
            } else {
                editBarView = null;
                actionBar.setCustomView(getTitleView(),
                        new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));
            }
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        }
        invalidateOptionsMenu();
    }

    private void applyEditBarTint() {
        if (editBarView == null) {
            return;
        }
        int tintColor = isThemeLight ? Color.parseColor("#0D0D0D") : Color.parseColor("#F2F2F2");
        int[] buttonIds = {R.id.btn_add, R.id.btn_rename, R.id.btn_move_up,
                R.id.btn_move_down, R.id.btn_delete};
        for (int id : buttonIds) {
            ImageButton button = (ImageButton) editBarView.findViewById(id);
            if (button != null) {
                button.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
            }
        }
    }

    private void ensureItemVisible(final int position) {
        // Check after layout so the views reflect the inserted item
        itemListView.post(new Runnable() {
            @Override
            public void run() {
                int firstVisible = itemListView.getFirstVisiblePosition();
                if (position < firstVisible
                        || position > itemListView.getLastVisiblePosition()) {
                    itemListView.setSelection(position);
                    return;
                }
                View child = itemListView.getChildAt(position - firstVisible);
                if (child == null) {
                    return;
                }
                if (child.getTop() < 0 || child.getBottom() > itemListView.getHeight()) {
                    itemListView.setSelection(position);
                }
            }
        });
    }

    private void updateSelection() {
        if (selectedItem != null) {
            int position = items.indexOf(selectedItem);
            adapter.setSelectedPosition(position);
        } else {
            adapter.setSelectedPosition(-1);
        }
    }

    private void updateDividerHeight() {
        float density = getResources().getDisplayMetrics().density;
        int dividerHeight = (int) (verticalInterval * density);
        itemListView.setDividerHeight(dividerHeight);
    }

    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.add_item);

        final EditText input = new EditText(this);
        input.setHint(R.string.enter_item_name);
        builder.setView(input);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    ListleItem item = new ListleItem(name, DEFAULT_PRIORITY_INDEX);
                    int insertPosition;
                    if (selectedItem != null) {
                        int currentPosition = items.indexOf(selectedItem);
                        insertPosition = currentPosition + 1;
                    } else {
                        insertPosition = items.size();
                    }
                    items.add(insertPosition, item);
                    originalItems.add(insertPosition, item);
                    selectedItem = item;
                    adapter.updateItems(items);
                    updateSelection();
                    ensureItemVisible(insertPosition);
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showRenameDialog() {
        if (selectedItem == null) return;
        
        int position = items.indexOf(selectedItem);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.rename);

        final EditText input = new EditText(this);
        input.setText(selectedItem.getName());
        builder.setView(input);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    selectedItem.setName(name);
                    adapter.notifyDataSetChanged();
                    updateSelection();
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private void showModifyMenu() {
        if (selectedItem == null) return;

        final ListleItem item = selectedItem;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.modify_item);

        String[] options;
        if (isPrioMode) {
            options = new String[]{
                    getString(R.string.priority),
                    getString(R.string.rename),
                    getString(R.string.delete)
            };
        } else {
            options = new String[]{
                    getString(R.string.priority),
                    getString(R.string.rename),
                    getString(R.string.move),
                    getString(R.string.delete)
            };
        }

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (isPrioMode) {
                    switch (which) {
                        case 0:
                            showPriorityDialog();
                            break;
                        case 1:
                            showRenameDialog();
                            break;
                        case 2:
                            showDeleteConfirmation();
                            break;
                    }
                } else {
                    switch (which) {
                        case 0:
                            showPriorityDialog();
                            break;
                        case 1:
                            showRenameDialog();
                            break;
                        case 2:
                            // Enter move mode
                            isMoveMode = true;
                            int position = items.indexOf(selectedItem);
                            adapter.setMovePosition(position);
                            adapter.notifyDataSetChanged();
                            invalidateOptionsMenu(); // Show move up/down buttons
                            break;
                        case 3:
                            showDeleteConfirmation();
                            break;
                    }
                }
            }
        });

        builder.show();
    }

    private void showPriorityDialog() {
        if (selectedItem == null) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.priority);

        int currentPriorityIndex = selectedItem.getPriority();

        builder.setSingleChoiceItems(PRIORITIES, currentPriorityIndex, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectedItem.setPriority(which);
                // Exit move mode when priority changes
                if (isMoveMode) {
                    isMoveMode = false;
                    adapter.setMovePosition(-1);
                    invalidateOptionsMenu();
                }
                adapter.notifyDataSetChanged();
                applySorting();
                updateSelection();
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void moveItem(int fromPos, int toPos) {
        ListleItem item = items.remove(fromPos);
        items.add(toPos, item);

        // Also update originalItems to keep in sync
        ListleItem origItem = originalItems.remove(fromPos);
        originalItems.add(toPos, origItem);

        // Update selected item reference
        selectedItem = item;
        updateSelection();
    }

    private void showDeleteConfirmation() {
        if (selectedItem == null) return;
        
        new AlertDialog.Builder(this)
                .setMessage(R.string.delete_confirmation)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int position = items.indexOf(selectedItem);
                        items.remove(position);
                        originalItems.remove(position);
                        selectedItem = null;
                        adapter.setSelectedPosition(-1);
                        // Exit move mode when deleting
                        if (isMoveMode) {
                            isMoveMode = false;
                            adapter.setMovePosition(-1);
                            invalidateOptionsMenu();
                        }
                        adapter.updateItems(items);
                        applySorting();
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void applyTheme() {
        int bgColor;
        if (isThemeLight) {
            itemListView.setBackgroundResource(R.drawable.gradient_light);
            adapter.setTextColor(Color.parseColor("#0D0D0D"));
            bgColor = Color.parseColor("#F7F7F7");
        } else {
            itemListView.setBackgroundResource(R.drawable.gradient_dark);
            adapter.setTextColor(Color.parseColor("#F2F2F2"));
            bgColor = Color.parseColor("#080808");
        }
        adapter.setBackgroundColor(bgColor);
        adapter.notifyDataSetChanged();
        invalidateOptionsMenu();

        // Update edit bar icon colors
        applyEditBarTint();

        if (titleView != null) {
            titleView.setTextColor(isThemeLight
                    ? Color.parseColor("#0D0D0D") : Color.parseColor("#F2F2F2"));
        }
    }

    private void applySorting() {
        if (isPrioMode) {
            Collections.sort(items, new Comparator<ListleItem>() {
                @Override
                public int compare(ListleItem a, ListleItem b) {
                    return a.getPriority() - b.getPriority();
                }
            });
        } else {
            items.clear();
            items.addAll(originalItems);
        }
        adapter.notifyDataSetChanged();
    }

    private TextView getTitleView() {
        if (titleView == null) {
            titleView = new TextView(this);
            titleView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            titleView.setTextSize(20);
            titleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPageSwitchPopup();
                }
            });
        }
        titleView.setTextColor(isThemeLight
                ? Color.parseColor("#0D0D0D") : Color.parseColor("#F2F2F2"));
        updatePageTitle();
        return titleView;
    }

    private void updatePageTitle() {
        if (titleView != null && pages != null
                && currentPageIndex >= 0 && currentPageIndex < pages.size()) {
            titleView.setText(pages.get(currentPageIndex).name);
        }
    }

    private void selectPage(int index) {
        if (pages == null || index == currentPageIndex
                || index < 0 || index >= pages.size()) {
            return;
        }
        currentPageIndex = index;
        prefs.edit().putInt(KEY_CURRENT_PAGE, index).apply();
        loadCurrentPage();
    }

    private void loadCurrentPage() {
        originalItems = pages.get(currentPageIndex).items;
        items.clear();
        items.addAll(originalItems);
        selectedItem = null;
        adapter.updateItems(items);
        applySorting();
        updateSelection();
        updatePageTitle();
    }

    private void showPageSwitchPopup() {
        if (pages == null) {
            return;
        }
        final List<String> names = new ArrayList<>();
        for (PagesStore.Page page : pages) {
            names.add(page.name);
        }
        names.add(getString(R.string.manage));

        final ListPopupWindow popup = new ListPopupWindow(this);
        popup.setAnchorView(titleView != null ? titleView : itemListView);
        popup.setHeight(ListPopupWindow.WRAP_CONTENT);
        popup.setModal(true);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, names) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = (TextView) view;
                text.setTextColor(isThemeLight
                        ? Color.parseColor("#0D0D0D") : Color.parseColor("#F2F2F2"));
                return view;
            }
        };
        popup.setAdapter(adapter);

        int maxWidth = 0;
        for (int i = 0; i < adapter.getCount(); i++) {
            View row = adapter.getView(i, null, null);
            row.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            maxWidth = Math.max(maxWidth, row.getMeasuredWidth());
        }
        popup.setWidth(maxWidth);
        if (titleView != null) {
            popup.setHorizontalOffset((titleView.getWidth() - maxWidth) / 2);
        }
        popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                popup.dismiss();
                if (position == names.size() - 1) {
                    Intent intent = new Intent(MainActivity.this, PagesActivity.class);
                    startActivity(intent);
                } else {
                    selectPage(position);
                }
            }
        });
        popup.show();
    }

    private void loadPreferences() {
        fontSize = prefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
        verticalInterval = prefs.getInt(KEY_VERTICAL_INTERVAL, DEFAULT_VERTICAL_INTERVAL);
        isThemeLight = prefs.getBoolean(KEY_THEME_LIGHT, DEFAULT_THEME_LIGHT);
    }

    private void loadPages() {
        pages = PagesStore.load(prefs);
        currentPageIndex = prefs.getInt(KEY_CURRENT_PAGE, 0);
        if (currentPageIndex < 0 || currentPageIndex >= pages.size()) {
            currentPageIndex = 0;
        }
        loadCurrentPage();
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_FONT_SIZE, fontSize);
        editor.putInt(KEY_VERTICAL_INTERVAL, verticalInterval);
        editor.putBoolean(KEY_THEME_LIGHT, isThemeLight);
        editor.putInt(KEY_CURRENT_PAGE, currentPageIndex);
        editor.apply();

        if (pages != null) {
            PagesStore.save(prefs, pages);
        }
    }
}
