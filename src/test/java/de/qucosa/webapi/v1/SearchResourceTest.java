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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import javax.xml.xpath.XPathExpressionException;
import java.util.Collections;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:testContext.xml")
@WebAppConfiguration
public class SearchResourceTest {

    private static final Map<String, String> NS =
            Collections.singletonMap(SearchResource.XLINK_NAMESPACE_PREFIX, SearchResource.XLINK_NAMESPACE);

    @Autowired
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @Before
    public void setupMockMvc() throws XPathExpressionException {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .alwaysExpect(xpath("/Opus/SearchResult").exists())
                .build();
    }

    @Test
    public void returnsBadRequestWhenCalledWithoutSearchFields() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().isBadRequest())
                .andExpect(xpath("/Opus/Error/@message").string("Bad search request."));
    }

    @Test
    public void returnsBadRequestIfQueryWithoutField() throws Exception {
        mockMvc.perform(get("/search?field0=foo&query1=bar"))
                .andExpect(status().isBadRequest())
                .andExpect(xpath("/Opus/Error/@message").string("No fieldname for query argument 1."));
    }

    @Test
    public void returnsBadRequestIfSortArgumentWithoutOrder() throws Exception {
        mockMvc.perform(get("/search?orderby0=foo"))
                .andExpect(status().isBadRequest())
                .andExpect(xpath("/Opus/Error/@message").string("No sort order argument for order query 0."));
    }


    @Test
    public void findsByDocid() throws Exception {
        mockMvc.perform(get("/search?field0=docid&query0=10044"))
                .andExpect(status().isOk())
                .andExpect(xpath("/Opus/SearchResult/Search/@hits").string("1"));
    }

    @Test
    public void mapsFieldsToOpusSearchResultStructure() throws Exception {
        mockMvc.perform(get("/search?field0=docid&query0=10044"))
                .andExpect(status().isOk())
                .andExpect(xpath("/Opus/SearchResult/Search/@hits").string("1"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result/@number").string("0"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result/@docid").string("10044"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result/@xlink:href", NS).string("http://localhost:80/document/10044"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result/@title").string("A Connection between the Star Problem and the Finite Power Property in Trace Monoids"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result/@author").string("Daniel Kirsten"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result/@year").string(""))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result/@completeddate").string("20121128"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result/@doctype").string("research_paper"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result/@issue").string(""));
    }

    @Test
    public void handlesMultiFieldSearchQuery() throws Exception {
        mockMvc.perform(get("/search?field0=docid&query0=10044&field1=author&query1=kirsten"))
                .andExpect(status().isOk())
                .andExpect(xpath("/Opus/SearchResult/Search/@hits").string("1"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result/@number").string("0"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result/@docid").string("10044"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result/@author").string("Daniel Kirsten"));
    }

    @Test
    public void findsAllPublishedDocuments() throws Exception {
        mockMvc.perform(get("/search?field0=serverstate&query0=published"))
                .andExpect(status().isOk())
                .andExpect(xpath("/Opus/SearchResult/Search/@hits").string("3"));
    }

    @Test
    public void findsByDoctype() throws Exception {
        mockMvc.perform(get("/search?field0=doctype&query0=research?paper"))
                .andExpect(status().isOk())
                .andExpect(xpath("/Opus/SearchResult/Search/@hits").string("1"));
    }

    @Test
    public void findByDate() throws Exception {
        mockMvc.perform(get("/search?field0=completeddate&query0=20121128"))
                .andExpect(status().isOk())
                .andExpect(xpath("/Opus/SearchResult/Search/@hits").string("1"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result/@docid").string("10044"));
    }

    @Test
    public void findWithinDateRange() throws Exception {
        mockMvc.perform(get("/search?field0=completeddate&query0=[20110101 TO 20130101]"))
                .andExpect(status().isOk())
                .andExpect(xpath("/Opus/SearchResult/Search/@hits").string("2"));
    }

    @Test
    public void handlesDocumentsWhereSelectedFieldsAreMissing() throws Exception {
        mockMvc.perform(get("/search?field0=docid&query0=10305"))
                .andExpect(status().isOk())
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result/@docid").string("10305"));
    }

    @Test
    public void skipsDocumentsWithIndexError() throws Exception {
        mockMvc.perform(get("/search?field0=docid&query0=error"))
                .andExpect(status().isOk())
                .andExpect(xpath("/Opus/SearchResult/Search/@hits").string("0"));
    }

    @Test
    public void findByDefaultSearchField() throws Exception {
        mockMvc.perform(get("/search?field0=defaultsearchfield&query0=\"Star Problem\""))
                .andExpect(status().isOk())
                .andExpect(xpath("/Opus/SearchResult/Search/@hits").string("1"));
    }

    @Test
    public void hitsOrderedByDate() throws Exception {
        mockMvc.perform(get("/search?field0=serverstate&query0=published&orderby0=completeddate&orderhow0=desc"))
                .andExpect(status().isOk())
                .andExpect(xpath("/Opus/SearchResult/Search/@hits").string("3"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result[1]/@completeddate").string("20140610"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result[2]/@completeddate").string("20121203"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result[3]/@completeddate").string("20121128"));
    }

    @Test
    public void findByDDCSubject() throws Exception {
        mockMvc.perform(get("/search?field0=subject&query0=616"))
                .andExpect(status().isOk())
                .andExpect(xpath("/Opus/SearchResult/Search/@hits").string("1"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result[1]/@docid").string("10033"));
    }

    @Test
    public void findByUncontrolledSubject() throws Exception {
        mockMvc.perform(get("/search?field0=subject&query0=Halbgruppe"))
                .andExpect(status().isOk())
                .andExpect(xpath("/Opus/SearchResult/Search/@hits").string("1"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result[1]/@docid").string("10044"));
    }

    @Test
    public void findByContent() throws Exception {
        mockMvc.perform(get("/search?field0=defaultsearchfield&query0=Eschweilerhof"))
                .andExpect(status().isOk())
                .andExpect(xpath("/Opus/SearchResult/Search/@hits").string("1"))
                .andExpect(xpath("/Opus/SearchResult/ResultList/Result[1]/@docid").string("1071"));
    }

}
