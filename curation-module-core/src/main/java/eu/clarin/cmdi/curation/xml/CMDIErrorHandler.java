package eu.clarin.cmdi.curation.xml;

import java.util.Collection;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import eu.clarin.cmdi.curation.report.CMDIInstanceReport;
import eu.clarin.cmdi.curation.report.Message;
import eu.clarin.cmdi.curation.report.Severity;

/**
 * A custom SAX error handler
 */

public class CMDIErrorHandler implements ErrorHandler {

	CMDIInstanceReport report;
	Collection<Message> msgs;

	public CMDIErrorHandler(CMDIInstanceReport report, Collection<Message> msgs) {
		this.report = report;
		this.msgs = msgs;
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		addMessage(Severity.FATAL, exception.getLineNumber(), exception.getColumnNumber(), exception.getMessage());
		throw exception;
	}

	@Override
	public void error(SAXParseException exception){
		addMessage(Severity.ERROR, exception.getLineNumber(), exception.getColumnNumber(), exception.getMessage());
	}

	@Override
	public void warning(SAXParseException exception) {
		addMessage(Severity.WARNING, exception.getLineNumber(), exception.getColumnNumber(), exception.getMessage());

	}

	private void addMessage(Severity lvl, int line, int col, String msg) {
		msgs.add(new Message(lvl, "line: " + line + ", col: " + col + " - " + msg));

	}
}
