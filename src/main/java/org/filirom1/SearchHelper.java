package org.filirom1;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchHelper {
    Logger log = LoggerFactory.getLogger(SearchHelper.class);

    private SolrServer server;

    public void setServer(SolrServer server) {
        this.server = server;
    }

    public SolrServer getServer() {
        return server;
    }

    /**
     * @param id
     * @param params
     */
    public void addToIndex(String id, Map<String, String> params) {
        log.info("addToIndex " + id + " " + params);
        SolrInputDocument doc = new SolrInputDocument();
        doc.addField("id", "id1", 1.0f);
        doc.addField("name", "doc1", 1.0f);
        doc.addField("price", "10");

        /*doc.addField("id", id, 1.0f);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            doc.addField(entry.getKey(), entry.getValue(), 1.0f);
        } */
        try {
            server.add(doc);
            server.commit();
        } catch (Exception e) {
            throw new SearchException("Unable to add " + id + "(" + params + ") to index : " + params, e);
        }
    }

    /**
     * @param id     mandatory
     * @param params a map to index.
     * @param file   a file to index.
     */
    public void addToIndex(String id, Map<String, String> params, File file) {
        log.info("addToIndex " + id + " " + params + " " + file);
        if (null == id || id.isEmpty()) {
            throw new SearchException("id is missing");
        }
        ContentStreamUpdateRequest req = new ContentStreamUpdateRequest("/update/extract");
        req.setParam("literal.id", id);
        if (null != params) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                req.setParam("literal." + entry.getKey(), (String) entry.getValue());
            }
        }
        if (file != null && file.exists() && file.isFile()) {
            try {
                req.addFile(file);
            } catch (IOException e) {
                throw new SearchException("Unable to read file : " + file.getAbsolutePath(), e);
            }
        } else {
            log.info("File not valid " + file + ". Data indexed without file : " + id + " " + params);
            addToIndex(id, params);
            return;
        }
        req.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
        try {
            NamedList<Object> result = getServer().request(req);
        } catch (Exception e) {
            throw new SearchException("Unable to add " + id + "(" + params + ", " + file + ") to index : " + params, e);
        }
    }

    public List<String> search(String query) {
        return search(new SolrQuery(query));
    }

    /**
     * @param query
     * @return une liste de id, correspondant à la recherche
     */
    public List<String> search(SolrQuery query) {
        QueryResponse rsp = null;
        try {
            rsp = getServer().query(query);
        } catch (SolrServerException e) {
            throw new SearchException("Unable to search in the index", e);
        }
        SolrDocumentList docs = rsp.getResults();
        List<String> resultIds = new ArrayList<String>();
        for (SolrDocument doc : docs) {
            Object id = doc.getFieldValue("id");
            if (id instanceof String) {
                resultIds.add((String) id);
            } else {
                throw new SearchException("Only String are accepted as id : " + id);
            }
        }
        return resultIds;
    }

    /**
     * @param id the id to delete
     */
    public void removeFromIndex(String id) {
        log.info("removeFromIndex " + id);
        try {
            getServer().deleteById(id);
        } catch (Exception e) {
            throw new SearchException("Unable to delete from index : " + id);
        }
    }

    /**
     * Update the index. Push all pending insert and deletion.
     */
    public void updateIndex() {
        log.info("updateIndex");
        try {
            getServer().commit();
        } catch (Exception e) {
            throw new SearchException("Unable update");
        }
    }


    public List<String> list() {
        return search("*");
    }

}
