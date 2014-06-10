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
    private String[] fields = new String[]{};
    private String query;
    private QueryType queryType = QueryType.TermQuery;
    private MappingType mappingType = MappingType.NoMapping;

    private InternalQueryBuilder(QueryType type) {
        this.queryType = type;
    }

    static InternalQueryBuilder termQuery() {
        return new InternalQueryBuilder(QueryType.TermQuery);
    }

    static InternalQueryBuilder matchQuery() {
        return new InternalQueryBuilder(QueryType.MatchQuery);
    }

    static InternalQueryBuilder multiMatchQuery() {
        return new InternalQueryBuilder(QueryType.MultiMatchQuery);
    }

    static InternalQueryBuilder stringQuery() {
        return new InternalQueryBuilder(QueryType.StringQuery);
    }

    static InternalQueryBuilder dateRangeQuery() {
        return new InternalQueryBuilder(QueryType.DateRangeQuery);
    }

    public InternalQueryBuilder field(@Nullable String... names) {
        this.fields = names;
        return this;
    }

    public InternalQueryBuilder query(String query) {
        this.query = query;
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

    public InternalQueryBuilder replaceQuestionmarkQuoting() {
        this.mappingType = MappingType.MapToFedoraDoctype;
        return this;
    }

    public QueryBuilder build() throws Exception {
        String mappedQuery = mapQueryString();

        switch (queryType) {
            case TermQuery:
                return QueryBuilders.termQuery(fields[0], mappedQuery);
            case MatchQuery:
                return QueryBuilders.matchQuery(fields[0], mappedQuery);
            case MultiMatchQuery:
                MultiMatchQueryBuilder mqb = QueryBuilders.multiMatchQuery(mappedQuery);
                for (String f : fields) mqb.field(f);
                return mqb;
            case StringQuery:
                QueryStringQueryBuilder qsb = QueryBuilders.queryString(mappedQuery);
                for (String f : fields) qsb.field(f);
                return qsb;
            case DateRangeQuery:
                Matcher matcher = REGEXP_DATE_PATTERN.matcher(mappedQuery);
                if (matcher.matches()) {
                    return QueryBuilders.rangeQuery(fields[0])
                            .from(mapToFedoraDate(matcher.group(1)))
                            .to(mapToFedoraDate(matcher.group(2)));
                } else {
                    return QueryBuilders.termQuery(fields[0], mapToFedoraDate(mappedQuery));
                }
            default:
                throw new Exception("No search query type specified.");
        }
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
            case MapToFedoraDoctype:
                mappedQuery = query.replace('?', '_');
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
        NoMapping, MapToFedoraState, MapToFedoraId, MapToFedoraDoctype
    }


}
