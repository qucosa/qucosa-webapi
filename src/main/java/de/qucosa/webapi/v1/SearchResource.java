/*
 * Copyright (C) 2013 SLUB Dresden
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.qucosa.webapi.v1;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHitField;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.index.query.QueryBuilders.termQuery;

@Controller
@RequestMapping(produces = {"application/xml; charset=UTF-8", "application/vnd.slub.qucosa-v1+xml; charset=UTF-8"})
public class SearchResource {

    public static final String XLINK_NAMESPACE_PREFIX = "xlink";
    public static final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";
    private static Map<String, String> searchFieldnameMap;
    private static Map<String, SortBuilder> sortBuilderMap;
    private static Map<String, InternalQueryBuilder> queryBuilderMap;
    private final Logger log = LoggerFactory.getLogger(SearchResource.class);
    private final Client elasticSearchClient;
    private final XMLOutputFactory xmlOutputFactory;
    @Autowired
    private HttpServletRequest httpServletRequest;

    static {
        searchFieldnameMap = new HashMap<>();
        searchFieldnameMap.put("abstract", "PUB_ABSTRACT");
        searchFieldnameMap.put("author", "PUB_AUTHOR");
        searchFieldnameMap.put("completeddate", "PUB_DATE");
        searchFieldnameMap.put("docid", "PID");
        searchFieldnameMap.put("doctype", "PUB_TYPE");
        searchFieldnameMap.put("firstlevelname", "PUB_ORIGINATOR");
        searchFieldnameMap.put("person", "PUB_SUBMITTER");
        searchFieldnameMap.put("secondlevelname", "PUB_ORIGINATOR_SUB");
        searchFieldnameMap.put("serverstate", "OBJ_STATE");
        searchFieldnameMap.put("title", "PUB_TITLE");

        sortBuilderMap = new HashMap<>();
        sortBuilderMap.put("abstract", SortBuilders.fieldSort("PUB_ABSTRACT"));
        sortBuilderMap.put("author", SortBuilders.fieldSort("PUB_AUTHOR"));
        sortBuilderMap.put("completeddate", SortBuilders.fieldSort("PUB_DATE"));
        sortBuilderMap.put("docid", SortBuilders.fieldSort("PID"));
        sortBuilderMap.put("person", SortBuilders.fieldSort("PUB_SUBMITTER"));
        sortBuilderMap.put("title", SortBuilders.fieldSort("PUB_TITLE"));

        queryBuilderMap = new HashMap<>();
        queryBuilderMap.put("abstract", InternalQueryBuilder.field("PUB_ABSTRACT").matchQuery());
        queryBuilderMap.put("author", InternalQueryBuilder.field("PUB_AUTHOR").matchQuery());
        queryBuilderMap.put("completeddate", InternalQueryBuilder.field("PUB_DATE").dateRangeQuery());
        queryBuilderMap.put("defaultsearchfield", InternalQueryBuilder
                .field("PUB_ABSTRACT", "PUB_AUTHOR", "PUB_ORIGINATOR", "PUB_TAG", "PUB_TITLE", "PUB_TYPE")
                .stringQuery());
        queryBuilderMap.put("docid", InternalQueryBuilder.field("PID").termQuery().mapToFedoraId());
        queryBuilderMap.put("doctype", InternalQueryBuilder.field("PUB_TYPE").termQuery().replaceQuestionmarkQuoting());
        queryBuilderMap.put("firstlevelname", InternalQueryBuilder.field("PUB_ORIGINATOR").matchQuery());
        queryBuilderMap.put("person", InternalQueryBuilder.field("PUB_SUBMITTER").matchQuery());
        queryBuilderMap.put("secondlevelname", InternalQueryBuilder.field("PUB_ORIGINATOR_SUB").matchQuery());
        queryBuilderMap.put("serverstate", InternalQueryBuilder.field("OBJ_STATE").termQuery().mapToFedoraState());
        queryBuilderMap.put("subject", InternalQueryBuilder.field("PUB_TAG", "PUB_TAG_DDC").multiMatchQuery());
        queryBuilderMap.put("title", InternalQueryBuilder.field("PUB_TITLE").matchQuery());
    }

    @Autowired
    public SearchResource(Client elasticSearchClient) {
        this.elasticSearchClient = elasticSearchClient;
        xmlOutputFactory = XMLOutputFactory.newFactory();
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> search(@RequestParam Map<String, String> requestParameterMap) throws Exception {
        try {
            Map<String, String> queries = new HashMap<>();
            Map<String, String> orderby = new HashMap<>();

            assertParametersPresent(requestParameterMap);
            extractQueriesAndSortParameters(requestParameterMap, queries, orderby);
            BoolQueryBuilder bqb = createBoolQueryBuilder(queries);
            SearchRequestBuilder searchRequestBuilder = elasticSearchClient
                    .prepareSearch("qucosa")
                    .setTypes("documents")
                    .setScroll(new TimeValue(60, TimeUnit.SECONDS))
                    .setSize(100)
                    .setQuery(bqb)
                    .addFields(
                            searchFieldnameMap.get("docid"),
                            searchFieldnameMap.get("title"),
                            searchFieldnameMap.get("author"),
                            searchFieldnameMap.get("completeddate"),
                            searchFieldnameMap.get("doctype"));
            addSortParameter(orderby, searchRequestBuilder);

            log.debug("Issue query: " + searchRequestBuilder.toString());
            SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();

            return scrollAndBuildResultList(searchResponse);
        } catch (ElasticsearchException esx) {
            log.error("ElasticSearch specific error: {}", esx.getMessage());
            return errorResponse("Internal Server Error.", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (BadSearchRequestException bsr) {
            log.error("Bad search request: {}", bsr.getMessage());
            return errorResponse(bsr.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            log.error("Unexpected Exception: {}", ex.getMessage());
            return errorResponse("Internal Server Error.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void addSortParameter(Map<String, String> orderby, SearchRequestBuilder searchRequestBuilder) {
        List<SortBuilder> sortBuilders = getFedoraSortBuilders(orderby);
        for (SortBuilder sb : sortBuilders) {
            searchRequestBuilder.addSort(sb);
        }
    }

    private BoolQueryBuilder createBoolQueryBuilder(Map<String, String> queries) throws Exception {
        List<QueryBuilder> queryBuilders = getFedoraQueryBuilders(queries);
        BoolQueryBuilder bqb = QueryBuilders.boolQuery();
        for (QueryBuilder qb : queryBuilders) {
            bqb.must(qb);
        }
        return bqb;
    }

    private void extractQueriesAndSortParameters(final Map<String, String> requestParameterMap, Map<String, String> queries, Map<String, String> orderby)
            throws BadSearchRequestException {
        for (String key : requestParameterMap.keySet()) {
            if (key.startsWith("query")) {
                String num = key.substring("query".length());
                String fieldname = "field" + num;
                if (!requestParameterMap.containsKey(fieldname)) {
                    throw new BadSearchRequestException("No fieldname for query argument " + num + ".");
                }
                String queryname = "query" + num;
                queries.put(requestParameterMap.get(fieldname), requestParameterMap.get(queryname));
            } else if (key.startsWith("orderby")) {
                String num = key.substring("orderby".length());
                String orderargument = "orderhow" + num;
                if (!requestParameterMap.containsKey(orderargument)) {
                    throw new BadSearchRequestException("No sort order argument for order query " + num + ".");
                }
                String fieldname = "orderby" + num;
                orderby.put(requestParameterMap.get(fieldname), requestParameterMap.get(orderargument));
            }
        }
    }

    private void assertParametersPresent(Map<String, String> requestParameterMap) throws BadSearchRequestException {
        if (requestParameterMap.isEmpty()) {
            throw new BadSearchRequestException();
        }
    }

    private List<SortBuilder> getFedoraSortBuilders(Map<String, String> sortArguments) {
        List<SortBuilder> result = new LinkedList<>();
        for (String fieldname : sortArguments.keySet()) {
            SortOrder order = mapToSortOrder(sortArguments.get(fieldname));
            if (sortBuilderMap.containsKey(fieldname)) {
                result.add(sortBuilderMap.get(fieldname).order(order));
            }
        }
        return result;
    }

    private SortOrder mapToSortOrder(String order) {
        return (order.equals("desc")) ? SortOrder.DESC : SortOrder.ASC;
    }

    private List<QueryBuilder> getFedoraQueryBuilders(Map<String, String> queries) throws Exception {
        List<QueryBuilder> result = new LinkedList<>();
        for (String k : queries.keySet()) {
            String q = queries.get(k);
            result.add(queryBuilderMap.get(k).query(q).build());
        }
        result.add(termQuery("OBJ_OWNER_ID", "qucosa"));
        result.add(termQuery("IDX_ERROR", false));
        return result;
    }


    private String mapToQucosaDate(String date) throws ParseException {
        if (date.isEmpty()) return "";
        return new SimpleDateFormat("yyyyMMdd").format(new SimpleDateFormat("yyyy-MM-dd").parse(date));
    }

    private String mapToQucosaId(String id) {
        return id.substring("qucosa:".length());
    }

    private ResponseEntity<String> scrollAndBuildResultList(SearchResponse searchResponse) throws XMLStreamException, ParseException {
        StringWriter sw = new StringWriter();
        XMLStreamWriter w = xmlOutputFactory.createXMLStreamWriter(sw);

        SearchHits searchHits = searchResponse.getHits();

        w.writeStartDocument("UTF-8", "1.0");
        w.writeStartElement("Opus");
        {
            w.writeStartElement("SearchResult");
            {
                w.writeStartElement("Search");
                w.writeAttribute("hits", String.valueOf(searchHits.getTotalHits()));
                w.writeEndElement();
                w.writeStartElement("ResultList");
                w.writeNamespace(XLINK_NAMESPACE_PREFIX, XLINK_NAMESPACE);

                SearchResponse scrollResponse = searchResponse;
                int hitcount = 0;
                while (true) {
                    hitcount = writeResultElements(w, searchHits, hitcount);
                    scrollResponse = elasticSearchClient.prepareSearchScroll(scrollResponse.getScrollId())
                            .setScroll(new TimeValue(60, TimeUnit.SECONDS)).execute().actionGet();
                    searchHits = scrollResponse.getHits();
                    if (searchHits.getHits().length == 0) {
                        log.debug("Stop scrolling at hitcount: {}", hitcount);
                        break;
                    }
                }

                w.writeEndElement();
            }
            w.writeEndElement();
        }
        w.writeEndElement();
        w.writeEndDocument();
        w.flush();

        return new ResponseEntity<>(sw.toString(), HttpStatus.OK);
    }

    private int writeResultElements(XMLStreamWriter w, SearchHits searchHits, int starthitcount) throws XMLStreamException, ParseException {
        int i = starthitcount;
        for (SearchHit hit : searchHits) {
            w.writeStartElement("Result");
            w.writeAttribute("number", String.valueOf(i++));

            String docid = mapToQucosaId(head(values(hit.field(searchFieldnameMap.get("docid")))));

            w.writeAttribute("docid", docid);
            w.writeAttribute(XLINK_NAMESPACE, "href", getHrefLink(docid));
            w.writeAttribute("title", head(values(hit.field(searchFieldnameMap.get("title")))));
            w.writeAttribute("author", join(values(hit.field(searchFieldnameMap.get("author")))));
            w.writeAttribute("year", "");
            w.writeAttribute("completeddate", mapToQucosaDate(head(values(hit.field(searchFieldnameMap.get("completeddate"))))));
            w.writeAttribute("doctype", head(values(hit.field(searchFieldnameMap.get("doctype")))));
            w.writeAttribute("issue", "");
            w.writeEndElement();
        }
        return i;
    }

    private List values(SearchHitField searchHitField) {
        return (searchHitField == null) ? new LinkedList() : searchHitField.values();
    }

    private String head(List l) {
        return ((l == null) || (l.isEmpty())) ? "" : (String) l.get(0);
    }

    private String join(List l) {
        if (l == null) return "";
        StringBuilder sb = new StringBuilder();
        Iterator it = l.iterator();
        while (it.hasNext()) {
            String s = (String) it.next();
            sb.append(s);
            if (it.hasNext()) {
                sb.append("; ");
            }
        }
        return sb.toString();
    }

    private ResponseEntity<String> errorResponse(String message, HttpStatus status) throws XMLStreamException {
        StringWriter sw = new StringWriter();
        XMLStreamWriter w = xmlOutputFactory.createXMLStreamWriter(sw);
        w.writeStartDocument("UTF-8", "1.0");
        w.writeStartElement("Opus");
        w.writeEmptyElement("SearchResult");
        w.writeStartElement("Error");
        w.writeAttribute("message", message);
        w.writeEndElement();
        w.writeEndElement();
        w.writeEndDocument();
        w.flush();
        return new ResponseEntity<>(sw.toString(), status);
    }

    private String getHrefLink(String pid) {
        if (httpServletRequest == null) {
            return "/" + pid;
        }
        StringBuilder sb = new StringBuilder()
                .append(httpServletRequest.getScheme())
                .append("://")
                .append(httpServletRequest.getServerName())
                .append(":")
                .append(httpServletRequest.getServerPort())
                .append(httpServletRequest.getContextPath())
                .append("/document/")
                .append(pid);
        return sb.toString();
    }

}
