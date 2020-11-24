package eu.clarin.cmdi.curation.exception;

/**
 * Crawl delay dictated by robots.txt is longer than our delay limit
 */
public class CrawlDelayTooLongException extends Exception {

    public CrawlDelayTooLongException(String message) {
        super(message);
    }
}
