/**
 *
 */
package eu.clarin.cmdi.curation.subprocessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import eu.clarin.cmdi.curation.cr.CRService;
import eu.clarin.cmdi.curation.cr.profile_parser.CMDINode;
import eu.clarin.cmdi.curation.cr.profile_parser.ParsedProfile;
import eu.clarin.cmdi.curation.report.CMDIProfileReport;
import eu.clarin.cmdi.curation.report.CMDIProfileReport.Component;
import eu.clarin.cmdi.curation.report.CMDIProfileReport.Components;
import eu.clarin.cmdi.curation.report.CMDIProfileReport.Concepts;
import eu.clarin.cmdi.curation.report.CMDIProfileReport.Elements;
import eu.clarin.cmdi.curation.report.Concept;
import eu.clarin.cmdi.curation.report.Message;
import eu.clarin.cmdi.curation.report.Score;

/**

 *
 */
public class ProfileElementsHandler{

    protected Collection<Message> msgs = null;

    public void process(CMDIProfileReport report) throws ExecutionException {
        ParsedProfile parsedProfile;

        parsedProfile = new CRService().getParsedProfile(report.header);

        report.components = createComponentSegment(parsedProfile);
        report.elements = createElementSegment(parsedProfile);

    }

    public Score calculateScore(CMDIProfileReport report) {
        double score = report.elements.percWithConcept;
        return new Score(score, 1.0, "cmd-concepts-section", msgs);
    }

    private Components createComponentSegment(ParsedProfile parsedProfile) {
        Collection<CMDINode> crComponents = parsedProfile.getComponents();
        Components comp = new Components();
        comp.total = crComponents.size();
        comp.required = 0;
        comp.unique = 0;
        comp.components = new ArrayList<Component>();

        crComponents.forEach(crc -> {

            if (crc.isRequired)
                comp.required++;

            Component component = comp.components.stream().filter(c -> c.id.equals(crc.component.id)).findFirst().orElse(null);
            if (component != null) {
                component.count++;
            } else {
                component = new Component();
                component.id = crc.component.id;
                component.name = crc.component.name;
                component.count = 1;
                comp.components.add(component);
            }

        });

        comp.unique = comp.components.size();
        return comp;
    }

    private Elements createElementSegment(ParsedProfile parsedProfile) {
        Collection<CMDINode> elemNodes = parsedProfile.getElements().entrySet()
                .stream() //dont consider elements from header and resources, they dont have concept
                .filter(e -> e.getKey().startsWith("/cmd:CMD/cmd:Components/"))
                .map(Entry::getValue)
                .collect(Collectors.toList());
        Elements elems = new Elements();
        elems.total = elemNodes.size();
        elems.required = 0;
        elems.withConcept = 0;
        elems.concepts = new Concepts();
        elems.concepts.total = 0;
        elems.concepts.unique = 0;
        elems.concepts.required = 0;
        elems.concepts.concepts = new ArrayList<>();

        elemNodes.forEach(n -> {
            if (n.isRequired)
                elems.required++;

            if (n.concept != null) {
                elems.withConcept++;

                if (n.isRequired)
                    elems.concepts.required++;

                Concept concept = elems.concepts.concepts.stream().filter(c -> c.uri.equals(n.concept.uri)).findFirst().orElse(null);

                if (concept != null) {
                    concept.count++;
                } else {
                    elems.concepts.concepts.add(new Concept(n.concept.uri, n.concept.prefLabel, n.concept.status));
                }
            }

        });

        elems.concepts.total = elems.withConcept;
        elems.concepts.unique = elems.concepts.concepts.size();
        elems.percWithConcept = elems.total == 0.0 ? 0.0 : ((double) elems.withConcept) / elems.total;

        return elems;

    }

}
