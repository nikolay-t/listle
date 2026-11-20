package org.inline.listle.theme;

import android.graphics.Color;

import org.inline.listle.R;

public class DarkTheme extends UiTheme {
    static final DarkTheme INSTANCE = new DarkTheme();

    private DarkTheme() {
    }

    @Override
    public int getTextColor() {
        return Color.parseColor("#F2F2F2");
    }

    @Override
    public int getBackgroundColor() {
        return Color.parseColor("#080808");
    }

    @Override
    public int getGradientResId() {
        return R.drawable.gradient_dark;
    }

    @Override
    public int getThemeResId() {
        return R.style.AppTheme_Dark;
    }

    @Override
    public boolean isLight() {
        return false;
    }
}
