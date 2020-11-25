<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/instance-report">
        <html>
            <head>
            </head>
            <body>
                <h1>CMDI Record Report</h1>

                <xsl:variable name="cmdRecord" select="./file-section/location"/>

                <p>CMDI Record:
                    <xsl:choose>
                        <xsl:when test="starts-with($cmdRecord, 'http://') or starts-with($cmdRecord, 'https://')">
                            <a href="{$cmdRecord}" target="_blank">
                                <xsl:copy-of select="$cmdRecord"/>
                            </a>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:copy-of select="$cmdRecord"/>
                        </xsl:otherwise>
                    </xsl:choose>
                </p>

                <p>Url: selfURLPlaceHolder</p>

                <p>ProfileID:
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="./profile-section/schemaLocation"></xsl:value-of>
                        </xsl:attribute>
                        <xsl:value-of select="./profile-section/id"></xsl:value-of>
                    </a>
                </p>
<!--                <p>Status:-->
<!--                    <xsl:value-of select="./profile-section/status"/>-->
<!--                </p>-->
                <p>File Size:
                    <xsl:value-of select="./file-section/size"/> B
                </p>
                <p>Timestamp:
                    <xsl:value-of select="./@timeStamp"/>
                </p>
                <hr/>

                <h2>Score Section</h2>
                <table class="reportTable">
                    <thead>
                        <tr>
                            <th scope="col">Segment</th>
                            <th scope="col">Score</th>
                            <th scope="col">Max</th>
                        </tr>
                    </thead>
                    <tfoot>
                        <tr>
                            <td colspan="3">
                                <b>
                                    Instance:
                                    <xsl:value-of select="format-number(./@ins-score,'0.00')"/>
                                    Total:
                                    <xsl:value-of select="format-number(./@score,'0.00')"/>
                                    Max:
                                    <xsl:value-of select="format-number(./@max-score,'0.00')"/>
                                    Score Percentage:
                                    <xsl:value-of select="format-number(./@score-percentage,'0.0%')"/>
                                </b>
                            </td>
                        </tr>
                    </tfoot>
                    <tbody>
                        <xsl:for-each select="./score-section/score">
                            <tr>
                                <td>
                                    <xsl:value-of select="./@segment"/>
                                </td>
                                <td class="text-right">
                                    <xsl:value-of select="./@score"/>
                                </td>
                                <td class="text-right">
                                    <xsl:value-of select="./@maxScore"/>
                                </td>
                            </tr>
                        </xsl:for-each>
                    </tbody>
                </table>

                <hr/>

                <h2>Facets Section</h2>

                <font color="#ffd100">&#9873;</font>
                - Derived Facet
                <br/>
                <font color="#00aa00">&#9873;</font>
                - Value Mapping
                <br/>


                <button class="btn btn-info" id="facetValuesButton" type="button" onClick="toggleFacets()">Show Facet Values</button>

                <div id="facetTable" hidden="true">
                    <table class="reportTable">
                        <thead>
                            <tr>
                                <th scope="col">Value</th>
                                <th scope="col">Facet</th>
                                <th scope="col">Normalised Value</th>
                                <th scope="col">Concept</th>
                                <th scope="col">XPath</th>
                            </tr>
                        </thead>
                        <tfoot>
                            <tr>
                                <td colspan="5">
                                    <b>
                                        covered by Instance
                                        <xsl:value-of
                                                select="count(./facets-section/coverage/facet[@coveredByInstance = 'true'])"/>
                                        /
                                        <xsl:value-of select="./facets-section/@numOfFacets"/>;

                                        covered by profile
                                        <xsl:value-of
                                                select="count(./facets-section/coverage/facet[@coveredByProfile = 'true'])"/>
                                        /
                                        <xsl:value-of select="./facets-section/@numOfFacets"/>;

                                        instance coverage: <xsl:value-of
                                            select="format-number(./facets-section/@instanceCoverage,'0.0%')"/>;
                                        profile coverage:
                                        <xsl:value-of
                                                select="format-number(./facets-section/@profileCoverage,'0.0%')"/>
                                    </b>
                                </td>
                            </tr>
                        </tfoot>
                        <tbody>

                            <xsl:for-each select="./facets-section/values/valueNode">
                                <xsl:choose>
                                    <xsl:when test="./facet">
                                        <xsl:for-each select="./facet">
                                            <tr>
                                                <xsl:if test="position() = 1">
                                                    <td rowspan="{last()}">
                                                        <xsl:value-of select="../value"/>
                                                    </td>
                                                </xsl:if>
                                                <xsl:choose>
                                                    <xsl:when test="@usesValueMapping">
                                                        <td>
                                                            <font color="#00aa00">
                                                                <xsl:value-of select="@name"/>
                                                            </font>
                                                        </td>
                                                    </xsl:when>
                                                    <xsl:when test="@isDerived">
                                                        <td>
                                                            <font color="#ffd100">
                                                                <xsl:value-of select="@name"/>
                                                            </font>
                                                        </td>
                                                    </xsl:when>
                                                    <xsl:otherwise>
                                                        <td>
                                                            <xsl:value-of select="@name"/>
                                                        </td>
                                                    </xsl:otherwise>
                                                </xsl:choose>
                                                <td>
                                                    <xsl:value-of select="@normalisedValue"/>
                                                </td>
                                                <xsl:if test="position() = 1">
                                                    <td rowspan="{last()}">
                                                        <a href="{../concept/@uri}"
                                                           title="status: {../concept/@status}, uri: {../concept/@uri}"
                                                           target="_blank">
                                                            <xsl:value-of select="../concept/@prefLabel"/>
                                                        </a>
                                                    </td>
                                                </xsl:if>
                                                <xsl:if test="position() = 1">
                                                    <td rowspan="{last()}">
                                                        <xsl:value-of select="../xpath"/>
                                                    </td>
                                                </xsl:if>
                                            </tr>
                                        </xsl:for-each>
                                    </xsl:when>
                                    <xsl:otherwise>
                                        <tr class="noFacet">
                                            <td>
                                                <xsl:value-of select="value"/>
                                            </td>
                                            <td></td>
                                            <td></td>
                                            <td>
                                                <a href="{concept/@uri}"
                                                   title="status: {concept/@status}, uri: {concept/@uri}"
                                                   target="_blank">
                                                    <xsl:value-of select="concept/@prefLabel"/>
                                                </a>
                                            </td>
                                            <td>
                                                <xsl:value-of select="xpath"/>
                                            </td>
                                        </tr>
                                    </xsl:otherwise>
                                </xsl:choose>
                            </xsl:for-each>

                            <xsl:for-each select="./facets-section/coverage/facet[@coveredByProfile = 'false']">
                                <tr>
                                    <td>
                                        <font color="#d33d3d">not covered by profile</font>
                                    </td>
                                    <td>
                                        <font color="#d33d3d">
                                            <xsl:value-of select="@name"/>
                                        </font>
                                    </td>
                                    <td></td>
                                    <td></td>
                                    <td></td>
                                </tr>
                            </xsl:for-each>

                            <xsl:for-each
                                    select="./facets-section/coverage/facet[@coveredByInstance = 'false'][@coveredByProfile = 'true']">
                                <tr>
                                    <td>
                                        <font color="#d33d3d">not covered by instance</font>
                                    </td>
                                    <td>
                                        <font color="#d33d3d">
                                            <xsl:value-of select="@name"/>
                                        </font>
                                    </td>
                                    <td></td>
                                    <td></td>
                                    <td></td>
                                </tr>
                            </xsl:for-each>
                        </tbody>
                    </table>
                </div>

                <hr/>

                <h2>ResProxy Section</h2>
                <p>Total number ResourceProxies:
                    <xsl:value-of select="./resProxy-section/numOfResProxies"/>
                </p>
                <p>Number of ResourceProxies having specified MIME type:
                    <xsl:value-of select="./resProxy-section/numOfResourcesWithMime"/>
                </p>
                <p>Percent of ResourceProxies having specified MIME type:
                    <xsl:value-of select="format-number(./resProxy-section/percOfResourcesWithMime,'0.0%')"/>
                </p>
                <p>Number of ResourceProxies having reference:
                    <xsl:value-of select="./resProxy-section/numOfResProxiesWithReferences"/>
                </p>
                <p>Percent of ResourceProxies having reference:
                    <xsl:value-of select="format-number(./resProxy-section/percOfResProxiesWithReferences,'0.0%')"/>
                </p>
                <table class="reportTable">
                    <thead>
                        <tr>
                            <th scope="col">Resource Type</th>
                            <th scope="col">Count</th>
                        </tr>
                    </thead>
                    <tbody>
                        <xsl:for-each select="./resProxy-section/resourceTypes/resourceType">
                            <tr>
                                <td>
                                    <xsl:value-of select="./@type"/>
                                </td>
                                <td class="text-right">
                                    <xsl:value-of select="./@count"/>
                                </td>
                            </tr>
                        </xsl:for-each>
                    </tbody>
                </table>

                <hr/>

                <h2>Xml Validation Section</h2>
                <p>Validity according to profile:
                    <xsl:value-of select="./xml-validation-section/valid"/>
                </p>

                <hr/>

                <h2>Xml Populated Section</h2>
                <p>Number of XML elements:
                    <xsl:value-of select="./xml-populated-section/numOfXMLElements"/>
                </p>
                <p>Number of simple XML elements:
                    <xsl:value-of select="./xml-populated-section/numOfXMLSimpleElements"/>
                </p>
                <p>Number of empty XML elements:
                    <xsl:value-of select="./xml-populated-section/numOfXMLEmptyElement"/>
                </p>
                <p>Percentage of populated XML elements:
                    <xsl:value-of select="format-number(./xml-populated-section/percOfPopulatedElements,'0.0%')"/>
                </p>

                <hr/>

                <h2>Url Validation Section</h2>
                <p>Number of links:
                    <xsl:value-of select="./url-validation-section/numOfLinks"/>
                </p>
                <p>Number of unique links:
                    <xsl:value-of select="./url-validation-section/numOfUniqueLinks"/>
                </p>
                <p>Number of checked links:
                    <xsl:value-of select="./url-validation-section/numOfCheckedLinks"/>
                </p>
                <p>Number of broken links:
                    <xsl:value-of select="./url-validation-section/numOfBrokenLinks"/>
                </p>
                <p>Number of undetermined links:
                    <xsl:value-of select="./url-validation-section/numOfUndeterminedLinks"/>
                </p>
                <p>Number of restricted access links:
                    <xsl:value-of select="./url-validation-section/numOfRestrictedAccessLinks"/>
                </p>
                <p>Number of blocked by robots.txt links:
                    <xsl:value-of select="./url-validation-section/numOfBlockedByRobotsTxtLinks"/>
                </p>

                <p>Percentage of valid links:
                    <xsl:value-of select="format-number(./url-validation-section/percOfValidLinks,'0.0%')"/>
                </p>

                <hr/>
                <h2>Single Url Report</h2>
                <table class="reportTable">
                    <thead>
                        <tr>
                            <th scope="col">Url</th>
                            <th scope="col">Category</th>
                            <th scope="col">Status</th>
                            <th scope="col">Info</th>
                        </tr>
                    </thead>
                    <tbody>
                        <xsl:for-each select="./single-url-report/url">
                            <xsl:sort select="@category"/>
                            <xsl:variable name="category">
                                <xsl:value-of select="./@category" />
                            </xsl:variable>
                            <xsl:variable name="link">
                                <xsl:value-of select="."/>
                            </xsl:variable>
                            <xsl:variable name="color">
                                <xsl:value-of select="./@color-code"/>
                            </xsl:variable>

                            <tr>
                                <td style="background-color:{$color}">
                                    <a href="{$link}">
                                        <xsl:value-of select="."/>
                                    </a>
                                </td>
                                <td style="background-color:{$color}">
                                    <xsl:copy-of select="$category"/>
                                </td>

                                <td class="text-right">
                                    <xsl:value-of select="./@http-status"/>
                                </td>

                                <td>
                                    <button class='showUrlInfo btn btn-info'>Show</button>
                                </td>

                            </tr>
                            <tr hidden="true">
                                <td colspan='4'>
                                    <b>Message:</b>
                                    <xsl:value-of select="./@message"/>
                                    <br></br>
                                    <b>Expected Content Type:</b>
                                    <xsl:value-of select="./@expected-content-type"/>
                                    <br></br>
                                    <b>Content Type:</b>
                                    <xsl:value-of select="./@content-type"/>
                                    <br></br>
                                    <b>Byte Size:</b>
                                    <xsl:value-of select="./@byte-size"/>
                                    <br></br>
                                    <b>Request Duration:</b>
                                    <xsl:value-of select="./@request-duration"/>
                                    <br></br>
                                    <b>Timestamp:</b>
                                    <xsl:value-of select="./@timestamp"/>
                                    <br></br>
                                    <b>Method:</b>
                                    <xsl:value-of select="./@method"/>
                                    <br></br>
                                </td>
                            </tr>
                        </xsl:for-each>
                    </tbody>
                </table>

                <xsl:if test="./score-section//issue">

                    <hr/>
                    <h2>Issues</h2>
                    <table class="reportTable">
                        <thead>
                            <tr>
                                <th scope="col">Segment</th>
                                <th scope="col">Severity</th>
                                <th scope="col">Message</th>
                            </tr>
                        </thead>
                        <tbody>
                            <xsl:for-each select="./score-section/score">
                                <xsl:variable name="seg">
                                    <xsl:value-of select="./@segment"/>
                                </xsl:variable>
                                <xsl:for-each select="./issue">
                                    <xsl:choose>
                                        <xsl:when test="@lvl = 'ERROR'">
                                            <tr>
                                                <td>
                                                    <font color="#d33d3d">
                                                        <xsl:copy-of select="$seg"/>
                                                    </font>
                                                </td>
                                                <td>
                                                    <font color="#d33d3d">
                                                        <xsl:value-of select="./@lvl"/>
                                                    </font>
                                                </td>
                                                <td>
                                                    <font color="#d33d3d">
                                                        <xsl:value-of select="./@message"/>
                                                    </font>
                                                </td>
                                            </tr>
                                        </xsl:when>
                                        <xsl:when test="@lvl = 'WARNING'">
                                            <tr>
                                                <td>
                                                    <font color="#9b870c">
                                                        <xsl:copy-of select="$seg"/>
                                                    </font>
                                                </td>
                                                <td>
                                                    <font color="#9b870c">
                                                        <xsl:value-of select="./@lvl"/>
                                                    </font>
                                                </td>
                                                <td>
                                                    <font color="#9b870c">
                                                        <xsl:value-of select="./@message"/>
                                                    </font>
                                                </td>
                                            </tr>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <tr>
                                                <td>
                                                    <xsl:copy-of select="$seg"/>
                                                </td>
                                                <td>
                                                    <xsl:value-of select="./@lvl"/>
                                                </td>
                                                <td>
                                                    <xsl:value-of select="./@message"/>
                                                </td>
                                            </tr>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </xsl:for-each>
                            </xsl:for-each>
                        </tbody>
                    </table>
                </xsl:if>

            </body>
        </html>
    </xsl:template>
</xsl:stylesheet> 