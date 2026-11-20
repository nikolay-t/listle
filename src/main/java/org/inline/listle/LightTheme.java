package org.inline.listle;

import android.graphics.Color;

public class LightTheme extends UiTheme {
    static final LightTheme INSTANCE = new LightTheme();

    private LightTheme() {
    }

    @Override
    public int getTextColor() {
        return Color.parseColor("#0D0D0D");
    }

    @Override
    public int getBackgroundColor() {
        return Color.parseColor("#F7F7F7");
    }

    @Override
    public int getGradientResId() {
        return R.drawable.gradient_light;
    }

    @Override
    public int getThemeResId() {
        return R.style.AppTheme;
    }

    @Override
    public boolean isLight() {
        return true;
    }
}
