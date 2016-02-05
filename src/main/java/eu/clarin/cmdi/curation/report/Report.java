package eu.clarin.cmdi.curation.report;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.Nonnull;

import eu.clarin.cmdi.curation.main.Config;

/**
 * @author dostojic
 * 
 *         A report about CMDI files, for details check
 *         https://trac.clarin.eu/wiki/Cmdi/QualityCriteria
 *
 */
public class Report {

    private List<Message> messages;
    private Severity verbose;
    private String name;

    public Report(String name, Severity verbose) {
	this.name = name;
	this.verbose = verbose;
	messages = new LinkedList<>();
    }

    public Report(String name) {
	this(name, Config.REPORT_VERBOSITY);
    }

    public void addMessage(@Nonnull Message msg) {
	if (msg.getSeverity().priority >= verbose.priority)
	    messages.add(msg);
    }

    public void addMessage(@Nonnull String m) {
	this.addMessage(new Message(m));
    }

    public Severity getHighestSeverity() {
	// if list is empty we run info, we care only about fatals
	return messages.isEmpty() ? Severity.INFO
		: messages.stream().max(Message::compareTo).map(Message::getSeverity).get();
    }

    public Message getFirst() {
	return messages == null || messages.isEmpty() ? null : messages.iterator().next();
    }

    /*
     * Take this value with caution, it is very speculative. We consider
     * validation/evaluation/assessment as successful (passed) if the highest
     * severity in messages is INFO or WARNING but how we decide about severity
     * is another story
     */

    public boolean passed() {
	return getHighestSeverity().compareTo(Severity.WARNING) <= 0;
    }

    public String toString() {
	Collections.sort(messages);
	StringBuilder sb = new StringBuilder(2000);
	sb.append(name).append("\n").append("STATUS: ").append(passed() ? "OK" : "FAILED").append("\n");
	messages.forEach(msg -> sb.append("\t").append(msg).append("\n"));
	return sb.toString();
    }
}
