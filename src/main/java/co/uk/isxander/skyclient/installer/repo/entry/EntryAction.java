package co.uk.isxander.skyclient.installer.repo.entry;

public class EntryAction {

    private final String display;
    private final String creator;
    private final String url;

    public EntryAction(String display, String creator, String url) {
        this.display = display;
        this.creator = creator;
        this.url = url;
    }

    public String getDisplay() {
        return display;
    }

    public String getCreator() {
        return creator;
    }

    public String getUrl() {
        return url;
    }
}
