package eu.clarin.cmdi.curation.io;

import eu.clarin.cmdi.curation.main.Configuration;
import eu.clarin.cmdi.curation.report.CMDInstanceReport;
import eu.clarin.cmdi.curation.utils.TimeUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * @author dostojic
 */
public class HTTPLinkChecker {

    private int timeout;
    private HttpURLConnection connection = null;
    private String redirectLink = null;
    private int REDIRECT_FOLLOW_LIMIT = Configuration.REDIRECT_FOLLOW_LIMIT;
    private List<Integer> redirectStatusCodes = new ArrayList<>(Arrays.asList(301, 302, 303, 307, 308));

    private final static Logger logger = LoggerFactory.getLogger(HTTPLinkChecker.class);

    public HTTPLinkChecker() {
        this(Configuration.TIMEOUT);
    }

    public HTTPLinkChecker(final int timeout) {
        HttpURLConnection connection = null;
        redirectLink = null;
        this.timeout = timeout;
    }

    //this method checks link with HEAD, if it fails it calls a check link with GET method
    public int checkLink(String url, CMDInstanceReport report, int redirectFollowLevel, long durationPassed) throws IOException {
        logger.info("Check link requested with url: " + url + " , redirectFollowLevel: " + redirectFollowLevel);
        RequestConfig requestConfig = RequestConfig.custom()//put all timeouts to 5 seconds, should be max 15 seconds per link
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout)
                .build();
        HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

        //try get if head doesnt work

        //valid-example.xml has this url: http://clarin.oeaw.ac.at/lrp/dict-gate/index.html
        //returns 400 for head but browser opens fine

        HttpHead head = new HttpHead(url);

        long start = System.currentTimeMillis();
        HttpResponse response = client.execute(head);
        long end = System.currentTimeMillis();
        long duration = end - start;

        duration += durationPassed;//durationPassed is for previous requests if any that led to a redirect

        int statusCode = response.getStatusLine().getStatusCode();

        CMDInstanceReport.URLElement urlElement = new CMDInstanceReport.URLElement();
        urlElement.message = "Ok";
        urlElement.url = url;
        urlElement.status = statusCode;

        //deal with redirect
        if (redirectStatusCodes.contains(statusCode)) {
            if (redirectFollowLevel >= REDIRECT_FOLLOW_LIMIT) {
                urlElement.message = "Too Many Redirects(Limit:" + REDIRECT_FOLLOW_LIMIT + ")";
            } else {
                String redirectLink = response.getHeaders("Location")[0].getValue();
                if (redirectLink != null) {
                    if (redirectLink.equals(url)) {
                        urlElement.message = "Redirect link is the same";
                    } else {
                        this.redirectLink = redirectLink;
                        return checkLink(redirectLink, report, redirectFollowLevel + 1, duration);
                    }
                } else {
                    urlElement.message = "There is no redirect link('Location' header)";
                }
            }
        }

        //IF HEAD doesnt work and we are not over the redirect limit, try the same thing with get
        if (statusCode != 200) {
            if (redirectFollowLevel < REDIRECT_FOLLOW_LIMIT) {
                HttpGet get = new HttpGet(url);

                start = System.currentTimeMillis();
                response = client.execute(get);
                end = System.currentTimeMillis();
                duration += end - start;

                statusCode = response.getStatusLine().getStatusCode();

                urlElement.status = statusCode;

                //deal with redirect
                if (redirectStatusCodes.contains(statusCode)) {
                    if (redirectFollowLevel >= REDIRECT_FOLLOW_LIMIT) {
                        urlElement.message = "Too Many Redirects(Limit:" + REDIRECT_FOLLOW_LIMIT + ")";
                    } else {
                        String redirectLink = response.getHeaders("Location")[0].getValue();
                        if (redirectLink != null) {
                            if (redirectLink.equals(url)) {
                                urlElement.message = "Redirect link is the same";
                            } else {
                                this.redirectLink = redirectLink;
                                return checkLink(redirectLink, report, redirectFollowLevel + 1, duration);
                            }
                        } else {
                            urlElement.message = "There is no redirect link('Location' header)";
                        }
                    }
                } else {
                    urlElement.message = "Broken Link";
                }

            } else {
                urlElement.message = "Too Many Redirects(Limit:" + REDIRECT_FOLLOW_LIMIT + ")";
            }
        }


        String contentType;
        Header[] contentTypeArray = response.getHeaders("Content-Type");
        if (contentTypeArray.length == 0) {
            contentType = "Not specified";
        } else {
            contentType = contentTypeArray[0].getValue();
        }

        String contentLength;
        Header[] contentLengthArray = response.getHeaders("Content-Length");
        if (contentLengthArray.length == 0) {
            contentLength = "Not specified";
        } else {
            contentLength = contentLengthArray[0].getValue();
        }


        urlElement.contentType = contentType;
        urlElement.byteSize = contentLength;
        urlElement.duration = TimeUtils.humanizeToTime(duration);
        urlElement.timestamp = TimeUtils.humanizeToDate(start);

        report.addURLElement(urlElement);

        return statusCode;
    }


    //todo change this also for downloader
    //this is legacy for downloader
    public int checkLink(String url) throws Exception {
        connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64; rv:45.0) Gecko/20100101 Firefox/45.0");
        connection.setRequestMethod("HEAD");
        String redirectLink = connection.getHeaderField("Location");
        if (redirectLink != null && !redirectLink.equals(url)) {
            this.redirectLink = redirectLink;
            return checkLink(redirectLink);
        } else
            return connection.getResponseCode();
    }

    public String getRedirectLink() {
        return redirectLink;
    }


    public String getResponse() throws IOException {
        if (connection == null)
            return "Connection is null";

        StringBuilder builder = new StringBuilder();
        builder.append(connection.getResponseCode())
                .append(" ")
                .append(connection.getResponseMessage())
                .append("\n");

        Map<String, List<String>> map = connection.getHeaderFields();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            if (entry.getKey() == null)
                continue;
            builder.append(entry.getKey())
                    .append(": ");

            List<String> headerValues = entry.getValue();
            Iterator<String> it = headerValues.iterator();
            if (it.hasNext()) {
                builder.append(it.next());

                while (it.hasNext()) {
                    builder.append(", ")
                            .append(it.next());
                }
            }

            builder.append("\n");
        }

        return builder.toString();
    }

}
