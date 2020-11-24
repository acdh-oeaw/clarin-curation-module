package eu.clarin.main;

import eu.clarin.cmdi.curation.exception.UncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;

/**
 * This class is called during the start of the web app. It calls the configuration class to load the web app.
 */
@WebListener
public class ApplicationInit implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationInit.class);

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        try {
            Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());//to log uncaught exceptions
            ServletContext servletContext = servletContextEvent.getServletContext();
            Configuration.init(servletContext);

        } catch (IOException e) {
            logger.error("There was a problem loading the properties file:" + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        Configuration.tearDown();
    }


}
