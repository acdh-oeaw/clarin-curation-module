package eu.clarin.helpers.HTMLHelpers;

import eu.clarin.helpers.FileManager;
import eu.clarin.main.Configuration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.List;

/**
 * This class is used to add content to the generic html template file. So most responses to the front end go through this class.
 */
public class HtmlManipulator {

    public static String addContentToGenericHTML(String content) throws IOException {
        return addContentToGenericHTML(content, null);
    }


    //This method adds html content passed as a string to the generic html which can be found in the resources
    //It also takes a list of buttons to add to the left menu. If the list is null or empty, nothing is added to the left menu.
    public static String addContentToGenericHTML(String content, NavbarButton button) throws IOException {

        String genericHTML = FileManager.readFile(Configuration.VIEW_RESOURCES_PATH + "/html/generic.html");

        Document doc = Jsoup.parse(genericHTML);

        if (button != null) {

            Element navbarButtons = doc.getElementById("buttons");
            Element template = navbarButtons.child(0).clone();

            /* left menu buttons looks like this.
            We need to change href of a element and the innermost text.

            <div class="col">
                <a href="/instances">
                    <button type="button" class="btn leftButton navbar navbar-brand navbar-light bg-light">
                        Instances
                    </button>
                </a>
            </div>
            * */
            template.child(0).attr("href", button.getLink());
            template.child(0).child(0).text(button.getDisplayText());

            navbarButtons.appendChild(template);

        }


        Element contentElement = doc.getElementById("content");
        contentElement.append(content);
        return doc.html();
    }
}
