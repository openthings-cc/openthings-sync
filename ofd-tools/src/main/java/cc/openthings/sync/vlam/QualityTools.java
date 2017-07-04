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

package cc.openthings.sync.vlam;

import cc.openthings.sync.Common;
import io.apptik.json.*;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.function.FunctionBase2;
import org.apache.jena.sparql.function.FunctionRegistry;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class QualityTools {
    private static final Logger logger = Logger.getLogger("OFD-QTools");
    private static int mergeCandidates = 0;

    public static void main(String[] args) throws IOException {
        FunctionRegistry.get().put("http://openthings.cc/func#", lev.class) ;
        String inFile = "./ofd-tools/works/actors/data";
        File rootDir = new File(inFile);
        //add_isPrimaryTopicOf(rootDir);
        //merge_0_doubles(rootDir);
        //System.out.println("------------ Merge Candidates: " + mergeCandidates + " --------------");
        //checkGeo(rootDir);
        //checkForLogo(rootDir);
        //checkForSimilar(rootDir);
    }

    private static Model loadModel(File dir) throws IOException {
        Model model = ModelFactory.createDefaultModel();
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                loadModel(file);
            } else {
                String fname = file.getName();
                if (fname.endsWith(".jsonld")) {
                   Model actModel = Common.getModel(file, RDFFormat.JSONLD.getLang().getName());
                    if (actModel == null) {
                        throw new RuntimeException("Error parsing");
                    }
                    model.add(actModel);
                }
            }
        }
        return model;
    }

    private static void checkForSimilar(Model model) throws IOException {
        Query query = QueryFactory.create("" +
                "PREFIX foaf: <http://xmlns.com/foaf/0.1/> " +
                "PREFIX func: <http://openthings.cc/func#> " +
                "select ?x,?y where { " +
                "?x a foaf:Agent . " +
                "?x foaf:name ?nameX . " +
                "filter exists { " +
                "?y a foaf:Agent . " +
                "?y foaf:name ?nameY . " +
                " FILTER (?x != ?y && func:lev(?nameX,?nameY) < 3 ) " +
                        "} "+
                "}"
        );
        QueryExecution qe = QueryExecutionFactory.create(query, model);
        ResultSet results = qe.execSelect();

        while (results.hasNext()) {
            QuerySolution row = results.next();
            //  String value= row.getLiteral("name").toString();
            System.out.println("-------------"
                    + row.getResource("a") + ": "
                    + row.getLiteral("lat") + ", " + row.getLiteral("lon")
            );
//            row.get("x").asResource().listProperties()
//                    .forEachRemaining(statement -> System.out.println(statement));


        }
    }

    private static void checkGeo(File dir) throws IOException {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                checkGeo(file);
            } else {
                String fname = file.getName();
                if (fname.endsWith(".jsonld")) {
                    JsonObject actor = JsonObject.readFrom(new FileReader(file)).asJsonObject();
                    String lat = actor.getJsonObject("schema:geo").optString("schema:latitude");
                    String lon = actor.getJsonObject("schema:geo").optString("schema:latitude");
                    if(lat == null || lon == null || Double.parseDouble(lat) == 0 || Double.parseDouble(lon) == 0) {
                       logger.severe("Geo Wrong: " + actor.get("@id"));
                       logger.severe("Geo: " + actor.getJsonObject("schema:geo").toString());
                    }
                }
            }
        }
    }

    private static void checkForLogo(File dir) throws IOException {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                checkForLogo(file);
            } else {
                String fname = file.getName();
                if (fname.endsWith(".jsonld")) {
                    JsonObject actor = JsonObject.readFrom(new FileReader(file)).asJsonObject();
                    if (!actor.has("schema:logo")) {
                        logger.info("No Logo:" + actor.getString("@id"));
                        
                    }

                }
            }
        }
    }



    private static String getAddrAsString(JsonElement je) {
        String addr = "";
        if (je.isJsonObject()) {
            JsonObject job = je.asJsonObject();
            addr = job.optString("schema:streetAddress", "")
                    + " " + job.optString("schema:postalCode", "")
            //        + " " + job.optString("schema:addressLocality", "")
            ;
        } else if (je.isString()) {
            //addr = je.asString();
            addr = je.asString().substring(0, je.asString().lastIndexOf(" "));
        }
        return addr;
    }

    private static JsonArray mergeArrays(JsonArray jarr1, JsonArray jarr2) {
        Set<Object> allImgs = new HashSet();
        if (jarr1 != null) {
            allImgs.addAll(jarr1);
        }
        if (jarr2 != null) {
            allImgs.addAll(jarr2);
        }
        return new JsonArray(allImgs);
    }

    /**
     * Merges Actors assuming they are the same. considering that act2 is the "producer type" if rechtvanbijdeboer
     *
     * @param act1 "farm shop"
     * @param act2 "producer"
     * @return Actor the result of the merge
     */
    private static JsonObject mergeActors(JsonObject act1, JsonObject act2) {
        System.out.println("@ID1:" + act1.get("@id") + ", @ID2: " + act2.get("@id"));
        //this merge just adds elements from act2 that are not in act1. arrays are not merged
        JsonObject res = new JsonObject().merge(act1.cloneAsThawed());
        //choose shorter id
        if(res.getString("@id").length()>act2.getString("@id").length()) {
            res.put("@id", act2.get("@id"));
        }
        //add isPrimaryTopicOf
        res.getJsonArray("foaf:isPrimaryTopicOf")
                .addAll(act2.getJsonArray("foaf:isPrimaryTopicOf").cloneAsThawed());
        //ActorType
        JsonArray types1 = res.optJsonArray("ofd:actorType");
        JsonArray types2 = act2.optJsonArray("ofd:actorType");
        JsonArray allTypes = mergeArrays(types1, types2);
        res.put("ofd:actorType", allTypes);
//
//        JsonElement actType = res.get("ofd:actorType");
//        if (actType.isJsonArray()) {
//            res.getJsonArray("ofd:actorType").put("at:producer");
//        } else {
//            JsonArray actTypes = new JsonArray().put(actType);
//            actTypes.addAll(act2.getJsonArray("ofd:actorType"));
//            res.put("ofd:actorType", actTypes);
//        }

        //depiction
        JsonArray imgs1 = res.optJsonArray("schema:image");
        JsonArray imgs2 = act2.optJsonArray("schema:image");
        JsonArray allImgs = mergeArrays(imgs1, imgs2);
        allImgs.remove(new JsonString("/sites/all/themes/rechtvanbijdeboer/images/beeld_kar.jpg"));
        res.put("schema:image", allImgs);
        res.put("foaf:depiction", allImgs);

        //check description
        String currDescr = res.optString("schema:description", "");
        String otherDescr = act2.optString("schema:description", "");
        if (!otherDescr.equals("") || !otherDescr.equalsIgnoreCase(currDescr)) {
            if (currDescr.equals("")) {
                res.put("schema:description", otherDescr);
            } else {
                res.put("schema:description", new JsonArray()
                        .put(currDescr).put(otherDescr));
            }
        }

        //account
        JsonArray accs1 = res.optJsonArray("foaf:account");
        JsonArray accs2 = act2.optJsonArray("foaf:account");
        JsonArray allAccs = mergeArrays(accs1, accs2);
        res.put("foaf:account", allAccs);

        //check offered products
        JsonArray fts1 = res.optJsonArray("ofd:offersFoodType");
        JsonArray fts2 = act2.optJsonArray("ofd:offersFoodType");
        JsonArray allFts = mergeArrays(fts1, fts2);
        res.put("ofd:offersFoodType", allFts);

        //URL
        JsonArray urls1 = res.optJsonArray("schema:url");
        JsonArray urls2 = act2.optJsonArray("schema:url");
        JsonArray allurls = mergeArrays(urls1, urls2);
        res.put("schema:url", allurls);
        res.put("foaf:homepage", allurls);

        //lat/lon
        if (res.getJsonObject("schema:geo").opt("schema:latitude") == null) {
            res.put("schema:geo", act2.get("schema:geo"));
        }

        //contact point
        if (res.optJsonObject("schema:contactPoint") == null
                && act2.optJsonObject("schema:contactPoint") != null
                ) {
            res.put("schema:contactPoint", act2.getJsonObject("schema:contactPoint"));
        }

        ///upstream
        JsonArray ups1 = res.optJsonArray("ofd:upstreamRel");
        JsonArray ups2 = act2.optJsonArray("ofd:upstreamRel");
        JsonArray allups = mergeArrays(ups1, ups2);
        allups = new JsonArray(allups.stream().filter(je ->
                !je.asJsonObject().getString("ofd:relatedActor").equals(act1.getString("@id")) &&
                        !je.asJsonObject().getString("ofd:relatedActor").equals(act2.getString("@id"))
        ).collect(Collectors.toList()));
        res.put("ofd:upstreamRel", allups);

        //downstream
        JsonArray downs1 = res.optJsonArray("ofd:downstreamRel");
        JsonArray downs2 = act2.optJsonArray("ofd:downstreamRel");
        JsonArray alldowns = mergeArrays(downs1, downs2);
        alldowns = new JsonArray(alldowns.stream().filter(je ->
                !je.asJsonObject().getString("ofd:relatedActor").equals(act1.getString("@id")) &&
                        !je.asJsonObject().getString("ofd:relatedActor").equals(act2.getString("@id"))
        ).collect(Collectors.toList()));
        res.put("ofd:downstreamRel", alldowns);


        return res;
    }

    private static void merge_0_doubles(File dir) throws IOException {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                merge_0_doubles(file);
            } else {
                String fname = file.getName();
                if (fname.endsWith("-1.jsonld")) {
                    File file2 = new File(file.getAbsolutePath()
                            .substring(0, file.getAbsolutePath().length() - 9) + ".jsonld");
                    //System.out.println("---------------- "+file.getAbsolutePath()+" -----------------");

                    if (file2.exists()) {
                        //switch job1/2 for -0 and -1
                        JsonObject job2 = JsonObject.readFrom(new FileReader(file)).asJsonObject();
                        JsonObject job1 = JsonObject.readFrom(new FileReader(file2)).asJsonObject();
                        String addr1 = getAddrAsString(job1.getJsonObject("schema:geo").get("schema:address"));
                        String addr2 = getAddrAsString(job2.getJsonObject("schema:geo").get("schema:address"));

                        if (addr1.equals(addr2)) {
                            mergeCandidates++;
                            JsonObject jobOut = mergeActors(job1, job2);
                            System.out.println("---------------- JOB 1 -----------------");
                            System.out.println(job1.getJsonObject("schema:geo").opt("schema:address"));
                            System.out.println(job1.opt("@id"));

                            System.out.println("---------------- JOB 2 -----------------");
                            System.out.println(job2.getJsonObject("schema:geo").opt("schema:address"));
                            System.out.println(job2.opt("@id"));

                            System.out.println("---------------- JOB Out -----------------");
                            System.out.println(jobOut.getJsonObject("schema:geo").get("schema:address"));
                            System.out.println(jobOut.opt("@id"));

                            writeItNice(file2,jobOut);
                            file.delete();

                        }
                    }
                }
            }
        }
    }


    public static void writeItNice(File file, JsonObject actor) throws IOException {
        FileWriter fw = new FileWriter(file);
        JsonWriter jw = new JsonWriter(fw);
        jw.setIndent(" ");
        actor.write(jw);
        jw.flush();
        jw.close();
    }

    public class lev extends FunctionBase2
    {
        public lev() { super() ; }
        public NodeValue exec(NodeValue nv1, NodeValue nv2)
        {
            return NodeValue.makeInteger(Common.levenshtein(nv1.toString(), nv2.toString()));
        }
    }

}
