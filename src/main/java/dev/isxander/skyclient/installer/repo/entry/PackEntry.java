package dev.isxander.skyclient.installer.repo.entry;

public class PackEntry {

    private final String id;
    private boolean enabled;
    private final String fileName;
    private final String downloadUrl;
    private final String displayName;
    private final String description;
    private final EntryWarning warning;
    private final String iconFile;
    private final String iconScaling;
    private final EntryAction[] actions;
    private final String creator;
    private final boolean hidden;

    public PackEntry(String id, boolean enabled, String fileName, String downloadUrl, String displayName, String description, EntryWarning warning, String iconFile, String iconScaling, EntryAction[] actions, String creator, boolean hidden) {
        this.id = id;
        this.enabled = enabled;
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
        this.displayName = displayName;
        this.description = description;
        this.warning = warning;
        this.iconFile = iconFile;
        this.iconScaling = iconScaling;
        this.actions = actions;
        this.creator = creator;
        this.hidden = hidden;
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public EntryWarning getWarning() {
        return warning;
    }

    public String getIconFile() {
        return iconFile;
    }

    public String getIconScaling() {
        return iconScaling;
    }

    public String getCreator() {
        return creator;
    }

    public boolean isHidden() {
        return hidden;
    }

    public EntryAction[] getActions() {
        return actions;
    }
}
