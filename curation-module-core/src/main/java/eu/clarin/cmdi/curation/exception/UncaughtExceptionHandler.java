package eu.clarin.cmdi.curation.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stack trace is called so that the caller can see it
 */
public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static Logger logger = LoggerFactory.getLogger(UncaughtExceptionHandler.class);

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();//so that the caller gets the stack trace
        logger.error("Uncaught exception: ", e);//so that the logger gets the stack trace in log files.
    }
}
