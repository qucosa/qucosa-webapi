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

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.elasticsearch.index.query.QueryBuilders.*;

@Controller
@RequestMapping(produces = {"application/xml; charset=UTF-8", "application/vnd.slub.qucosa-v1+xml; charset=UTF-8"})
public class SearchResource {

    public static final String XLINK_NAMESPACE_PREFIX = "xlink";
    public static final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";
    private static final Pattern REGEXP_DATE_PATTERN = Pattern.compile("^\\[(\\d{8})\\sTO\\s(\\d{8})\\]$");
    private final Client elasticSearchClient;
    private final XMLOutputFactory xmlOutputFactory;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Autowired
    public SearchResource(Client elasticSearchClient) {
        this.elasticSearchClient = elasticSearchClient;
        xmlOutputFactory = XMLOutputFactory.newFactory();
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String> search(@RequestParam Map<String, String> requestParameterMap) throws Exception {
        Map<String, String> queries = new HashMap<>();

        if (requestParameterMap.isEmpty()) {
            return errorResponse("Bad search request.", HttpStatus.BAD_REQUEST);
        }

        try {
            for (String key : requestParameterMap.keySet()) {
                if (key.startsWith("query")) {
                    String num = key.substring("query".length());
                    String fieldname = "field" + num;
                    if (!requestParameterMap.containsKey("field" + num)) {
                        return errorResponse("No fieldname for query argument " + num + ".", HttpStatus.BAD_REQUEST);
                    }
                    String queryname = "query" + num;
                    queries.put(requestParameterMap.get(fieldname), requestParameterMap.get(queryname));
                }
            }

            List<QueryBuilder> queryBuilders = getFedoraQueryBuilders(queries);
            BoolQueryBuilder bqb = QueryBuilders.boolQuery();
            for (QueryBuilder qb : queryBuilders) {
                bqb.must(qb);
            }
            SearchRequestBuilder searchRequestBuilder = elasticSearchClient
                    .prepareSearch("qucosa")
                    .setTypes("documents")
                    .setSearchType(SearchType.QUERY_AND_FETCH)
                    .setQuery(bqb)
                    .addFields("PID", "PUB_TITLE", "PUB_AUTHOR", "PUB_DATE", "PUB_TYPE");
            SearchResponse searchResponse = searchRequestBuilder.execute().actionGet();
            SearchHits searchHits = searchResponse.getHits();

            return resultlist(searchHits);

        } catch (Exception ex) {
            return errorResponse("Internal Server Error.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    private List<QueryBuilder> getFedoraQueryBuilders(Map<String, String> queries) throws Exception {
        List<QueryBuilder> result = new LinkedList<>();
        for (String k : queries.keySet()) {
            String q = queries.get(k);
            switch (k) {
                case "docid":
                    result.add(termQuery("PID", mapToFedoraId(q)));
                    break;
                case "serverstate":
                    result.add(termQuery("OBJ_STATE", mapToFedoraState(q)));
                    break;
                case "securitylist":
                    result.add(termQuery("OBJ_OWNER_ID", q));
                    break;
                case "completeddate":
                    Matcher matcher = REGEXP_DATE_PATTERN.matcher(q);
                    if (matcher.matches()) {
                        result.add(rangeQuery("PUB_DATE")
                                .from(mapToFedoraDate(matcher.group(1)))
                                .to(mapToFedoraDate(matcher.group(2))));
                    } else {
                        result.add(termQuery("PUB_DATE", mapToFedoraDate(q)));
                    }
                    break;
                case "title":
                    result.add(matchQuery("PUB_TITLE", q));
                    break;
                case "abstract":
                    result.add(matchQuery("PUB_ABSTRACT", q));
                    break;
                case "person":
                    result.add(matchQuery("PUB_SUBMITTER", q));
                    break;
                case "author":
                    result.add(matchQuery("PUB_AUTHOR", q));
                    break;
                case "subject":
                    result.add(multiMatchQuery(q, "PUB_TAG", "PUB_TAG_DDC"));
                    break;
                case "doctype":
                    result.add(termQuery("PUB_TYPE", q));
                    break;
                case "firstlevelname":
                    result.add(matchQuery("PUB_ORIGINATOR", q));
                    break;
                case "secondlevelname":
                    result.add(matchQuery("PUB_ORIGINATOR_SUB", q));
                    break;
                default:
            }
        }
        return result;
    }

    private String mapToFedoraDate(String date) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd").format(new SimpleDateFormat("yyyyMMdd").parse(date));
    }

    private String mapToQucosaDate(String date) throws ParseException {
        return new SimpleDateFormat("yyyyMMdd").format(new SimpleDateFormat("yyyy-MM-dd").parse(date));
    }

    private String mapToFedoraState(String state) throws Exception {
        switch (state) {
            case "published":
                return "A";
            case "deleted":
                return "D";
            case "unpublished":
                return "I";
            default:
                throw new Exception("Unknown object state: " + state);
        }
    }

    private String mapToFedoraId(String pid) {
        return "qucosa:" + pid;
    }

    private String mapToQucosaId(String id) {
        return id.substring("qucosa:".length());
    }

    private ResponseEntity<String> resultlist(SearchHits searchHits) throws XMLStreamException, ParseException {
        StringWriter sw = new StringWriter();
        XMLStreamWriter w = xmlOutputFactory.createXMLStreamWriter(sw);

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
                {
                    int i = 0;
                    for (SearchHit hit : searchHits) {
                        w.writeStartElement("Result");
                        w.writeAttribute("number", String.valueOf(i++));

                        String docid = mapToQucosaId(head(hit.field("PID").values()));

                        w.writeAttribute("docid", docid);
                        w.writeAttribute(XLINK_NAMESPACE, "href", getHrefLink(docid));
                        w.writeAttribute("title", head(hit.field("PUB_TITLE").values()));
                        w.writeAttribute("author", join(hit.field("PUB_AUTHOR").values()));
                        w.writeAttribute("year", "");
                        w.writeAttribute("completeddate", mapToQucosaDate(head(hit.field("PUB_DATE").values())));
                        w.writeAttribute("doctype", head(hit.field("PUB_TYPE").values()));
                        w.writeAttribute("issue", "");
                        w.writeEndElement();
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

    private String head(List l) {
        return (l.isEmpty()) ? "" : (String) l.get(0);
    }

    private String join(List l) {
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
                .append("/documents/")
                .append(pid);
        return sb.toString();
    }

}
