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

import cc.openthings.sender.HttpSender;
import io.apptik.json.*;
import io.reactivex.Single;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;


//used to sync from already obtained jsonld objs
public class MainSyncVlam {
    private static final Logger logger = Logger.getLogger("vlam-sync");
    private static int mergeCandidates = 0;

    public static void main(String[] args) throws IOException {
        String inFile = "./vlam-sync/stage3/belgium/flanders";
        File rootDir = new File(inFile);
        //add_isPrimaryTopicOf(rootDir);
        //cleanDefImage(rootDir);
        //merge_0_doubles(rootDir);
        System.out.println("------------ Merge Candidates: " + mergeCandidates + " --------------");
    }

    private static void cleanDefImage(File dir) throws IOException {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                add_isPrimaryTopicOf(file);
            } else {
                String fname = file.getName();
                if (fname.endsWith(".jsonld")) {
                    JsonObject actor = JsonObject.readFrom(new FileReader(file)).asJsonObject();
                    JsonString defImage
                            = new JsonString("/sites/all/themes/rechtvanbijdeboer/images/beeld_kar.jpg");
                    if (actor.has("schema:image")) {
                        actor.getJsonArray("schema:image").remove(defImage);
                        actor.getJsonArray("foaf:depiction").remove(defImage);
                        writeItNice(file,actor);
                    }

                }
            }
        }
    }

    private static void add_isPrimaryTopicOf(File dir) throws IOException {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                add_isPrimaryTopicOf(file);
            } else {
                String fname = file.getName();
                if (fname.endsWith(".jsonld")) {
                    JsonObject actor = JsonObject.readFrom(new FileReader(file)).asJsonObject();
                    if (!actor.has("foaf:isPrimaryTopicOf")) {
                        actor.put("foaf:isPrimaryTopicOf",
                                Single.just(Config.baseUrl + "/" + fname.substring(0, fname.length() - 7))
                                        .doOnSuccess(o -> logger.info("Calling: " + o))
                                        .map(p -> new HttpSender(p).doCall())
                                        //.doOnSuccess(o -> logger.info("Got Details Producer Response: " + o))
                                        .doOnSuccess(r -> r.headers
                                                .forEach((s, strings) -> System.out.println(s + "::" + strings)))
                                        .map(r -> Jsoup.parse(r.body))
                                        .map(doc -> Funcs.getPrimaryTopicOf(doc))
                                        .blockingGet()
                        );
                        writeItNice(file,actor);
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

}
