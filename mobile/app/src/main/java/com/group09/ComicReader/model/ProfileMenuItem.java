package com.group09.ComicReader.model;

public class ProfileMenuItem {
    private final String type;
    private final String label;
    private final String badge;
    private final int iconResId;
    private final boolean navigatesToWallet;

    public ProfileMenuItem(String type, String label, String badge, int iconResId, boolean navigatesToWallet) {
        this.type = type;
        this.label = label;
        this.badge = badge;
        this.iconResId = iconResId;
        this.navigatesToWallet = navigatesToWallet;
    }

    public String getType() { return type; }
    public String getLabel() { return label; }
    public String getBadge() { return badge; }
    public int getIconResId() { return iconResId; }
    public boolean isNavigatesToWallet() { return navigatesToWallet; }
}
