package eu.clarin.helpers.HTMLHelpers;

/**
 * Pojo for navbar button on the left side
 */
public class NavbarButton {

    private String link;
    private String displayText;

    public NavbarButton(String link, String displayText) {
        this.displayText = displayText;
        this.link = link;
    }

    public String getLink() {
        return link;
    }

    public String getDisplayText() {
        return displayText;
    }

}