<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:template match="/instance-report">
        <html>
            <head>
            </head>
            <body>
                <h1>CMD Record Report</h1>

                <xsl:variable name="cmdRecord" select="./file-section/location"/>

                <p>CMD Record:
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

                <!--TODO this link is wrong-->
                <p>ProfileID:
                    <a>
                        <xsl:attribute name="href">
                            <xsl:value-of select="./profile-section/schemaLocation"></xsl:value-of>
                        </xsl:attribute>
                        <xsl:value-of select="./profile-section/id"></xsl:value-of>
                    </a>
                </p>
                <p>Status:
                    <xsl:value-of select="./profile-section/status"/>
                </p>
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
                                    <xsl:value-of select="format-number(./@ins-score,'##.##')"/>
                                    Total:
                                    <xsl:value-of select="format-number(./@score,'##.##')"/>
                                    Max:
                                    <xsl:value-of select="format-number(./@max-score,'##.##')"/>
                                    Score Percentage:
                                    <xsl:value-of select="format-number(./@score-percentage,'##.#%')"/>
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
                                <td>
                                    <xsl:value-of select="./@score"/>
                                </td>
                                <td>
                                    <xsl:value-of select="./@maxScore"/>
                                </td>
                            </tr>
                        </xsl:for-each>
                    </tbody>
                </table>

                <hr/>

                <h2>Facets Section</h2>

                <font color="#F4FA58">&#9873;</font>
                - Derived Facet
                <br/>
                <font color="#FF8000">&#9873;</font>
                - Value Mapping
                <br/>


                <button type="button" onClick="toggleFacets()">Show Facet Values</button>

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
                                            select="format-number(./facets-section/@instanceCoverage,'##.#%')"/>;
                                        profile coverage:
                                        <xsl:value-of
                                                select="format-number(./facets-section/@profileCoverage,'##.#%')"/>
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
                                                            <font color="#FF4000">
                                                                <xsl:value-of select="@name"/>
                                                            </font>
                                                        </td>
                                                    </xsl:when>
                                                    <xsl:when test="@isDerived">
                                                        <td>
                                                            <font color="#dbd839">
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
                    <xsl:value-of select="format-number(./resProxy-section/percOfResourcesWithMime,'##.#%')"/>
                </p>
                <p>Number of ResourceProxies having reference:
                    <xsl:value-of select="./resProxy-section/numOfResProxiesWithReferences"/>
                </p>
                <p>Percent of ResourceProxies having reference:
                    <xsl:value-of select="format-number(./resProxy-section/percOfResProxiesWithReferences,'##.#%')"/>
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
                                <td>
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

                <h2>Xml Populated Ssection</h2>
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
                    <xsl:value-of select="format-number(./xml-populated-section/percOfPopulatedElements,'##.#%')"/>
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
                <p>Number of undetermined links:
                    <xsl:value-of select="./url-validation-section/numOfUndeterminedLinks"/>
                </p>
                <p>Number of broken links:
                    <xsl:value-of select="./url-validation-section/numOfBrokenLinks"/>
                </p>
                <p>Percentage of valid links:
                    <xsl:value-of select="format-number(./url-validation-section/percOfValidLinks,'##.#%')"/>
                </p>

                <hr/>
                <h2>Single Url Report</h2>
                <table class="reportTable">
                    <thead>
                        <tr>
                            <th scope="col">Url</th>
                            <th scope="col">Category</th>
                            <th scope="col">Message</th>
                            <th scope="col">Http-status</th>
                            <th scope="col">Content-type</th>
                            <th scope="col">Expected-content-type</th>
                            <th scope="col">Byte-size</th>
                            <th scope="col">Request-duration</th>
                            <th scope="col">Timestamp</th>
                            <th scope="col">Method</th>
                            <th scope="col">Redirect-count</th>

                        </tr>
                    </thead>
                    <tbody>
                        <xsl:for-each select="./single-url-report/url">
                            <xsl:sort select="@category"/>
                            <xsl:variable name="category">
                                <xsl:value-of select="./@category"/>
                            </xsl:variable>
                            <xsl:variable name="link">
                                <xsl:value-of select="."/>
                            </xsl:variable>

                            <tr>
                                <xsl:if test="$category='Ok'">
                                    <td style="background-color:#cbe7cc" align="right">
                                        <a href="{$link}">
                                            <xsl:value-of select="."/>
                                        </a>
                                    </td>
                                    <td style="background-color:#cbe7cc" align="right">
                                        <xsl:value-of select="./@category"/>
                                    </td>
                                </xsl:if>
                                <xsl:if test="$category='Undetermined'">
                                    <td style="background-color:#fff7b3" align="right">
                                        <a href="{$link}">
                                            <xsl:value-of select="."/>
                                        </a>
                                    </td>
                                    <td style="background-color:#fff7b3" align="right">
                                        <xsl:value-of select="./@category"/>
                                    </td>
                                </xsl:if>
                                <xsl:if test="$category='Broken'">
                                    <td style="background-color:#f2a6a6" align="right">
                                        <a href="{$link}">
                                            <xsl:value-of select="."/>
                                        </a>
                                    </td>
                                    <td style="background-color:#f2a6a6" align="right">
                                        <xsl:value-of select="./@category"/>
                                    </td>
                                </xsl:if>

                                <td>
                                    <xsl:value-of select="./@message"/>
                                </td>
                                <td>
                                    <xsl:value-of select="./@http-status"/>
                                </td>
                                <td>
                                    <xsl:value-of select="./@content-type"/>
                                </td>
                                <td>
                                    <xsl:value-of select="./@expected-content-type"/>
                                </td>
                                <td>
                                    <xsl:value-of select="./@byte-size"/>
                                </td>
                                <td>
                                    <xsl:value-of select="./@request-duration"/>
                                </td>
                                <td>
                                    <xsl:value-of select="./@timestamp"/>
                                </td>
                                <td>
                                    <xsl:value-of select="./@method"/>
                                </td>
                                <td>
                                    <xsl:value-of select="./@redirectCount"/>
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