package cc.openthings.sync.geonames;

/*
 * Copyright (C) 2014 OpenThings Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileUtils;
import org.djodjo.json.JsonArray;
import org.djodjo.json.JsonElement;
import org.djodjo.json.JsonObject;
import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import java.io.*;
import java.util.*;

public class ProcessData {

    private static String baseResourceURI = "http://www.openthings.cc/geothing/";
    private static String indexName = "geothings";
    private static String typeName = "geothing";
    private static JsonObject ldC = new JsonObject();
    //set of fields that should be an array
    private static HashSet<String> shouldBeItemArray = new HashSet<>();
    private static HashSet<String> shouldBeObjectArray = new HashSet<>();
    //set of fields which contain ref id that should be changed to point to openthings.cc
    private static HashSet<String> shouldBeRelinked = new HashSet<>();
    private static HashSet<String> toRemove = new HashSet<>();



    static TransportClient tc= null;

    static String infile = "/home/djodjo/data/projects/openThings/geonames.org/all-geonames-rdf.txt";
    static String basedir = "/Users/sic/works/geonames/";


    static {
        shouldBeObjectArray.add("alternateName");
        shouldBeItemArray.add("wikipediaArticle");
        shouldBeItemArray.add("seeAlso");
        shouldBeObjectArray.add("officialName");
        shouldBeItemArray.add("postalCode");
        // shouldBeArray.add("");

        shouldBeRelinked.add("parentFeature");
        shouldBeRelinked.add("parentCountry");
        shouldBeRelinked.add("parentADM1");
        shouldBeRelinked.add("parentADM2");
        shouldBeRelinked.add("parentADM3");
        shouldBeRelinked.add("parentADM4");
        // shouldBeRelinked.add("");

        toRemove.add("@context");
        toRemove.add("nearbyFeatures");
        toRemove.add("neighbouringFeatures");
        toRemove.add("childrenFeatures");
        toRemove.add("lat");
        toRemove.add("long");
//        toRemove.add("");

    }

    public static void main(String[] args) {
       //INIT ES transport
        tc = new TransportClient(
                ImmutableSettings.settingsBuilder()
                        .put("cluster.name", "openthings")
                                //.put("node.name", "djodjo-UWS")
                        .build());
        tc.addTransportAddress(new InetSocketTransportAddress("localhost",
                9300));
        List<DiscoveryNode> nodes = tc.listedNodes();
        System.out.println( "NODES: ");
        for(DiscoveryNode n:nodes) {
            System.out.println( "node: " + n);
        }
        List<DiscoveryNode> nodes2 = tc.connectedNodes();
        System.out.println( "CONNECTED NODES: ");
        for(DiscoveryNode n:nodes2) {
            System.out.println( "node: " + n);
        }
      //  --set geo mapping
        final IndicesExistsResponse res = tc.admin().indices().prepareExists(indexName).execute().actionGet();
        if (!res.isExists())
        {
            CreateIndexRequestBuilder cirb =  tc.admin().indices().prepareCreate(indexName);

                cirb.addMapping(typeName,"{\n" +
                        "\"properties\":{" +
                        "\"coordinates\":{\n" +
                        "\"type\": \"geo_point\",\n" +
                        "\"lat_lon\": true\n" +
                        "}" +
                        "}}");

            cirb.execute().actionGet();
        }
/////////


        InputStream inputStream = null;
        Scanner sc = null;
        try {
            inputStream = FileUtils.openResourceFileAsStream(infile);
            sc = new Scanner(inputStream, "UTF-8");
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                //System.out.println(line);
                if(line.startsWith("<")) {
                    Model model = ModelFactory.createDefaultModel() ;
                    model.read(new StringReader(line), "");
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    model.write(os, "JSON-LD");
                    InputStream is = new ByteArrayInputStream(os.toByteArray());
                    //not need to expand just keep common context for geoThings
                    //Object jObj = JsonUtils.fromInputStream(is);
                    //Object expjObj = JsonLdProcessor.expand(jObj);
                    //System.out.println("Expanded:" + expjObj);
                    InputStreamReader isr = new InputStreamReader(is);

                    JsonObject je = JsonObject.readFrom(isr).asJsonObject();
                    is.close();
//                    ldC.merge(je.getJsonObject("@context"));
//                    System.out.println("New Context len: " +je.get("@id")+" :: " + ldC.length());

                    je.put("coordinates", new JsonObject().put("lat", je.get("lat")).put("lon", je.get("long")));

                    for (String field:toRemove) {
                        je.remove(field);
                    }

                    for (String field:shouldBeObjectArray) {
                        JsonElement fje = je.opt(field);
                        if(fje!=null) {
                           // System.out.println("Will edit: " + fje);
                            if(!fje.isJsonArray()) {
                                if (fje.isJsonObject()) {
                                    je.put(field, new JsonArray().put(fje));
                                } else {
                                    je.put(field, new JsonArray().put(new JsonArray().put(new JsonObject().put("@value", fje))));
                                }

                            } else {
                                //check the array items if are objects
                                Iterator<JsonElement> it = fje.asJsonArray().iterator();
                                ArrayList<JsonElement> toadd = new ArrayList<>();
                                while(it.hasNext()) {
                                    JsonElement tje = it.next();
                                    if(!tje.isJsonObject()) {
                                        it.remove();
                                        toadd.add(new JsonObject().put("@value", tje));
                                    }
                                }
                                if(toadd.size()>0) {
                                   // for(JsonElement tje:toadd)
                                    fje.asJsonArray().addAll(toadd);
                                }
                            }
                        }
                    }

                    for (String field:shouldBeItemArray) {
                        JsonElement fje = je.opt(field);
                        if(fje!=null && !fje.isJsonArray()) {
                            //System.out.println("Will edit: " + fje);
                            JsonElement tmpJe = fje;
                            fje = new JsonArray().put(tmpJe);
                            je.put(field, fje);
                        }
                    }

                    if(je.optJsonArray("seeAlso")!=null
                            && je.optJsonArray("seeAlso").get(0).toString().startsWith("http://dbpedia.org/resource/")
                            ) {
                    je.getJsonArray("seeAlso").put(
                            je.optJsonArray("seeAlso").get(0).toString().replace("http://dbpedia.org/resource/", "http://www.openthings.cc/thing/"));
                    }

                 //
//
//
                    String shortId = je.getString("name");
//
                    if(checkForId(tc, shortId)) {
                        shortId += "_";

                        if (checkForId(tc, shortId)) {
                            int cnt = 1;
                            while (checkForId(tc, shortId + cnt)) {
                                cnt++;
                            }
                            shortId += cnt;
                        }
                    }
                    je.put("sameAs", je.get("@id"));
                    je.put("@id", baseResourceURI + shortId);

                    System.out.println("Will write:" + je.toString());


                    writeObject2es(shortId, je);
                }
            }
            if (sc.ioException() != null) {
                throw sc.ioException();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            //} catch (JsonLdError jsonLdError) {
            //    jsonLdError.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (sc != null) {
                sc.close();
            }
        }

        System.out.println("New Context" + ldC);

        //now substitute refs
        postProcess(tc);


//        Model model = ModelFactory.createDefaultModel() ;
//        model.read(new StringReader(instr), "");
//       System.out.println(model.toString());
//        model.write(System.out, "JSON-LD");


    }



    private static boolean checkForId(TransportClient tc, String geonamesId)  {
        System.out.println("check for id: " + geonamesId);
        GetResponse response = tc.prepareGet(indexName, typeName, geonamesId)
                .execute()
                .actionGet();

        System.out.println("found: " + response.getSourceAsString());
        return response.isExists();
    }


    private static void postProcess(TransportClient tc) {

        QueryBuilder qb = QueryBuilders.matchAllQuery();

        SearchResponse scrollResp = tc.prepareSearch(indexName)
                .setSearchType(SearchType.SCAN)
                .setScroll(new TimeValue(60000))
                .setQuery(qb)
                .setSize(100).execute().actionGet(); //100 hits per shard will be returned for each scroll
//Scroll until no hits are returned
        while (true) {

            for (SearchHit hit : scrollResp.getHits()) {
                System.out.println("Substituting: " + hit.getSource().get("@id") + " :: " + hit.getSource().get("sameAs"));
                substituteRefs(hit.getId(), tc, new JsonObject(hit.getSource()));
            }
            scrollResp = tc.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();
            //Break condition: No hits are returned
            if (scrollResp.getHits().getHits().length == 0) {
                break;
            }
        }
    }

    private static String fetchGeonamesNameFromES(TransportClient tc, String fullGeonamesId)  {
        String res = null;
        //System.out.println("fetch GeonamesName for id: " + geonamesId);

        SearchResponse resp = tc.prepareSearch(indexName)
                .setTypes(typeName)
                .setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                        // .setQuery(QueryBuilders.matchQuery("http\\:\\/\\/www\\.w3\\.org\\/2002\\/07\\/owl#sameAs.@id", "http://sws.geonames.org/"+geonamesId+"/"))
                        //.setQuery(QueryBuilders.matchPhraseQuery("@id", geonamesId))
                .setQuery(QueryBuilders.queryString("sameAs:\"" + fullGeonamesId+"\""))
                .setSize(2)
                        //.setExplain(true)
                .execute().actionGet();

        //System.out.println("Result: " + resp);
        //System.out.println("Object: " + new JsonObject(resp.getHits().getAt(0).getSource()).toString());
        //System.out.println("GeonamesName: " + resp.getHits().getAt(0).getId());
        res = resp.getHits().getAt(0).getId();
        return res;
    }

    private static void substituteRefs(String id, TransportClient tc, JsonObject job) {
        boolean needToUpdate = false;
        for (String field:shouldBeRelinked) {
            String val = job.optString(field);
            if(val != null && val.startsWith("http://sws.geonames.org/")) {
                needToUpdate = true;
                String newId = fetchGeonamesNameFromES(tc, val);
                job.put(field, baseResourceURI + newId);
            }
        }

        if(needToUpdate) {
            writeObject2es(id, job);
        }
    }



    private static JsonObject getJsonLDfromRDF(InputStream is) {
        Model model = ModelFactory.createDefaultModel() ;
        model.read(is, null);
        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        model.write(os, "JSON-LD");
        InputStream iss = new ByteArrayInputStream(os.toByteArray());
        Object jObj = null;
        try {
            jObj = JsonUtils.fromInputStream(iss);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            iss.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Object expjObj = null;
        try {
            expjObj = JsonLdProcessor.expand(jObj);
        } catch (JsonLdError jsonLdError) {
            jsonLdError.printStackTrace();
        }
        //System.out.println("Expanded:" + expjObj);

        JsonObject je = new JsonArray((ArrayList)expjObj).get(0).asJsonObject();
        return je;
    }

//   private static String fetchGeonamesName(String geonamesId)  {
//       String res = geonamesIdName.get(geonamesId);
//       if(res==null) {
//           try {
//               HttpURLConnection conn = (HttpURLConnection) new URL("http://sws.geonames.org/" + geonamesId + "/about.rdf").openConnection();
//               BufferedReader in;
//               if (conn.getResponseCode() > 299) {
//                   in = new BufferedReader(
//                           new InputStreamReader(conn.getErrorStream()));
//                   String inputLine;
//                   StringBuffer response = new StringBuffer();
//
//                   while ((inputLine = in.readLine()) != null) {
//                       response.append(inputLine);
//                   }
//                   in.close();
//                   throw new RuntimeException("Error fetching geonamesURL: " + "http://sws.geonames.org/" + geonamesId + "/about.rdf\nError:" + response);
//               }
//
//               JsonObject job = getJsonLDfromRDF(conn.getInputStream());
//               res = job.getJsonArray("http://www.geonames.org/ontology#name").get(0).asJsonObject().getString("@value");
//               geonamesIdName.put(geonamesId, res);
//           } catch (IOException e) {
//               //should not happen
//               e.printStackTrace();
//               throw new RuntimeException(e);
//           }
//       }
//       return res;
//   }

    private static void writeObject2es(String objectId, JsonObject jsonObject) {
        if(objectId==null || objectId.isEmpty() || jsonObject==null) return;
        //
        IndexRequestBuilder irb = tc
                .prepareIndex(indexName, typeName, objectId);
        irb.setSource(jsonObject.toString());

        final ListenableActionFuture<IndexResponse> laf = irb .execute();
        final IndexResponse response =     laf.actionGet();
        System.out.println( "======= Written idx:" + response.getId() + " :: " + response.getIndex() + " :: " + response.getType() + " :: " + response.getVersion() + " =======");
    }

}
