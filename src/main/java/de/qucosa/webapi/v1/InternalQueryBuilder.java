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

import org.elasticsearch.common.Nullable;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class InternalQueryBuilder {
    private static final Pattern REGEXP_DATE_PATTERN = Pattern.compile("^\\[(\\d{8})\\sTO\\s(\\d{8})\\]$");
    private String fieldName;
    private String[] moreFields;
    private String query;

    private InternalQueryBuilder(String name, @Nullable String... moreNames) {
        this.fieldName = name;
        this.moreFields = moreNames;
    }

    static InternalQueryBuilder field(String name, @Nullable String... names) {
        return new InternalQueryBuilder(name, names);
    }

    public InternalQueryBuilder query(String query) {
        this.query = query;
        return this;
    }

    private QueryType queryType = QueryType.TermQuery;

    public InternalQueryBuilder termQuery() {
        this.queryType = QueryType.TermQuery;
        return this;
    }

    public InternalQueryBuilder mapToFedoraState() {
        this.mappingType = MappingType.MapToFedoraState;
        return this;
    }

    public InternalQueryBuilder mapToFedoraId() {
        this.mappingType = MappingType.MapToFedoraId;
        return this;
    }

    public InternalQueryBuilder matchQuery() {
        this.queryType = QueryType.MatchQuery;
        return this;
    }

    public InternalQueryBuilder multiMatchQuery() {
        this.queryType = QueryType.MultiMatchQuery;
        return this;
    }

    public InternalQueryBuilder stringQuery() {
        this.queryType = QueryType.StringQuery;
        return this;
    }

    private MappingType mappingType = MappingType.NoMapping;

    public InternalQueryBuilder dateRangeQuery() {
        this.queryType = QueryType.DateRangeQuery;
        return this;
    }

    private String mapToFedoraDate(String date) throws ParseException {
        return new SimpleDateFormat("yyyy-MM-dd").format(new SimpleDateFormat("yyyyMMdd").parse(date));
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

    private String mapQueryString() throws Exception {
        String mappedQuery;
        switch (mappingType) {
            case MapToFedoraState:
                mappedQuery = mapToFedoraState(query);
                break;
            case MapToFedoraId:
                mappedQuery = mapToFedoraId(query);
                break;
            default:
                mappedQuery = query;
        }
        return mappedQuery;
    }

    private enum QueryType {
        TermQuery, MatchQuery, MultiMatchQuery, StringQuery, DateRangeQuery
    }

    private enum MappingType {
        NoMapping, MapToFedoraState, MapToFedoraId
    }

    QueryBuilder build() throws Exception {
        String mappedQuery = mapQueryString();

        switch (queryType) {
            case TermQuery:
                return QueryBuilders.termQuery(fieldName, mappedQuery);
            case MatchQuery:
                return QueryBuilders.matchQuery(fieldName, mappedQuery);
            case MultiMatchQuery:
                MultiMatchQueryBuilder mqb = QueryBuilders.multiMatchQuery(mappedQuery);
                mqb.field(fieldName);
                for (String f : moreFields) mqb.field(f);
                return mqb;
            case StringQuery:
                QueryStringQueryBuilder qsb = QueryBuilders.queryString(mappedQuery);
                qsb.field(fieldName);
                for (String f : moreFields) qsb.field(f);
                return qsb;
            case DateRangeQuery:
                Matcher matcher = REGEXP_DATE_PATTERN.matcher(mappedQuery);
                if (matcher.matches()) {
                    return QueryBuilders.rangeQuery(fieldName)
                            .from(mapToFedoraDate(matcher.group(1)))
                            .to(mapToFedoraDate(matcher.group(2)));
                } else {
                    return QueryBuilders.termQuery(fieldName, mapToFedoraDate(mappedQuery));
                }
            default:
                throw new Exception("No search query type specified.");
        }
    }


}
