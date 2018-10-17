package eu.clarin.cmdi.curation.processor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;

import eu.clarin.cmdi.curation.main.Configuration;
import eu.clarin.cmdi.curation.subprocessor.InstanceXMLValidator;
import eu.clarin.cmdi.curation.subprocessor.URLValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.cmdi.curation.entities.CurationEntity;
import eu.clarin.cmdi.curation.io.FileSizeException;
import eu.clarin.cmdi.curation.report.ErrorReport;
import eu.clarin.cmdi.curation.report.Report;
import eu.clarin.cmdi.curation.subprocessor.ProcessingStep;

public abstract class AbstractProcessor<R extends Report<?>> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractProcessor.class);

    public Report<?> process(CurationEntity entity) throws InterruptedException {


        Report<?> report = createReport();

        try {
            for (ProcessingStep step : createPipeline()) {

                step.process(entity, report);
                //logger.info("processed Record: "+report.getName() + ", step: "+ step.getClass().getSimpleName());
                //logger.info("processed Record: "+ entity.getUrl() != null?entity.getUrl().replaceAll("/", "-"):entity.getPath() + ", step: "+ step.getClass().getSimpleName());
                if(step instanceof InstanceXMLValidator){
                    report.addSegmentScore(((InstanceXMLValidator)step).calculateValidityScore());
                }


                if(step instanceof URLValidator){
                    if(Configuration.HTTP_VALIDATION){
                        report.addSegmentScore(step.calculateScore(report));
                    }
                }else {
                    report.addSegmentScore(step.calculateScore(report));
                }


            }

            return report;
        } catch (FileSizeException e) {
            logger.error(e.getMessage());
            return new ErrorReport(report.getName(), e.getMessage());
        } catch (Exception e) {
            logger.error("", e);
            String message = e.getMessage();
            message = message.replace(" java.lang.Exception","");
            if(message==null || message.isEmpty()){
                message = "There was an unknown error. Please report it.";
            }
            logger.error("", e);
            return new ErrorReport(report.getName(), message);
        }

    }

    protected abstract Collection<ProcessingStep> createPipeline();

    protected abstract R createReport();

    private String getStackTrace(final Throwable ex) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        ex.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

}