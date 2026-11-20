package org.inline.listle.theme;

public abstract class UiTheme {
    public abstract int getTextColor();
    public abstract int getBackgroundColor();
    public abstract int getGradientResId();
    public abstract int getThemeResId();
    public abstract boolean isLight();

    public static UiTheme from(boolean isLight) {
        return isLight ? light() : dark();
    }

    public static UiTheme light() {
        return LightTheme.INSTANCE;
    }

    public static UiTheme dark() {
        return DarkTheme.INSTANCE;
    }
}
