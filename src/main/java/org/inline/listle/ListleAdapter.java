package org.inline.listle;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

public class ListleAdapter extends BaseAdapter {
    private Context context;
    private List<ListleItem> items;
    private int fontSize;
    private int verticalInterval;
    private int textColor = Color.BLACK;
    private int backgroundColor = Color.WHITE;
    private int selectedPosition = -1;
    private int movePosition = -1;
    private String[] priorities;
    private OnStatusClickListener onStatusClickListener;
    private OnItemLongClickListener onItemLongClickListener;
    private OnItemClickListener onItemClickListener;

    public interface OnStatusClickListener {
        void onStatusClick(int position);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public ListleAdapter(Context context, List<ListleItem> items, int fontSize, int verticalInterval, String[] priorities) {
        this.context = context;
        this.items = items;
        this.fontSize = fontSize;
        this.verticalInterval = verticalInterval;
        this.priorities = priorities;
    }

    public void setOnStatusClickListener(OnStatusClickListener listener) {
        this.onStatusClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_layout, parent, false);
        }

        LinearLayout container = (LinearLayout) convertView.findViewById(R.id.item_container);
        final TextView statusView = (TextView) convertView.findViewById(R.id.item_status);
        TextView nameView = (TextView) convertView.findViewById(R.id.item_name);

        ListleItem item = items.get(position);
        int priorityIndex = item.getPriority();
        statusView.setText(priorities[priorityIndex]);
        nameView.setText(item.getName());

        statusView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, fontSize);
        nameView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, fontSize);
        statusView.setTextColor(textColor);
        nameView.setTextColor(textColor);

        // Calculate priority column width: measure the widest priority string + 50%
        Paint paint = statusView.getPaint();
        paint.setTextSize(fontSize * context.getResources().getDisplayMetrics().density);
        float maxWidth = 0;
        for (String priority : priorities) {
            float width = paint.measureText(priority);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }
        int priorityWidth = (int) (maxWidth * 1.5);
        ViewGroup.LayoutParams params = statusView.getLayoutParams();
        if (params == null) {
            params = new LinearLayout.LayoutParams(priorityWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
        } else {
            params.width = priorityWidth;
        }
        statusView.setLayoutParams(params);

        // Vertical interval is controlled by ListView divider height in MainActivity

        // Highlight selected item (subtle - calculate based on background)
        if (position == selectedPosition) {
            container.setBackgroundColor(adjustColor(backgroundColor, 0.9f));
        } else {
            container.setBackgroundColor(Color.TRANSPARENT);
        }

        // Status click listener
        statusView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onStatusClickListener != null) {
                    onStatusClickListener.onStatusClick(position);
                }
            }
        });

        // Name view click listener for selection
        nameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(position);
                }
            }
        });

        // Name view long click listener
        nameView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (onItemLongClickListener != null) {
                    onItemLongClickListener.onItemLongClick(position);
                }
                return true;
            }
        });

        return convertView;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
        notifyDataSetChanged();
    }

    public void setVerticalInterval(int verticalInterval) {
        this.verticalInterval = verticalInterval;
        notifyDataSetChanged();
    }

    public void setTextColor(int color) {
        this.textColor = color;
        notifyDataSetChanged();
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    public void setMovePosition(int position) {
        this.movePosition = position;
        notifyDataSetChanged();
    }

    public int getMovePosition() {
        return movePosition;
    }

    public void updateItems(List<ListleItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    private int adjustColor(int color, float factor) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);

        // Calculate luminance to determine if background is dark or light
        float luminance = (0.299f * red + 0.587f * green + 0.114f * blue) / 255;

        if (luminance < 0.5f) {
            // Dark background - make it lighter
            red = (int) Math.min(255, red + (255 - red) * (1 - factor));
            green = (int) Math.min(255, green + (255 - green) * (1 - factor));
            blue = (int) Math.min(255, blue + (255 - blue) * (1 - factor));
        } else {
            // Light background - make it darker
            red = (int) (red * factor);
            green = (int) (green * factor);
            blue = (int) (blue * factor);
        }

        return Color.argb(Color.alpha(color), red, green, blue);
    }
}
