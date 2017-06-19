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

import cc.openthings.sender.HttpResponse;
import cc.openthings.sender.HttpSender;
import cc.openthings.sync.UriUtils;
import io.apptik.json.JsonArray;
import io.apptik.json.JsonObject;
import io.apptik.json.JsonWriter;
import io.reactivex.Flowable;
import io.reactivex.Single;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cc.openthings.sync.vlam.Config.actorBaseUri;
import static io.apptik.json.JsonElement.readFrom;

public class TestApi {
    private static final Logger logger = Logger.getLogger("vlam-sync");

    public static void main(String[] args) throws IOException {
        //example();
        //test1();
        //getAll();
        getFromCache();
    }

    public static void test1() throws IOException {
        String filePath = "/home/djodjo/data/dev/openthings.cc/openthings-sync/vlam-sync/stage2/section_voedselteam-merelbeke.html";
        String body = new Scanner(new File(filePath)).useDelimiter("\\Z").next();
        Object[] object = new Object[]{"52.84,3.56",
                new HttpResponse(200, body, null)};
        logger.info(toJsonLD(object).toString());
    }

    public static void example() throws IOException {
        JsonArray points = (JsonArray) readFrom(Config.points);
        try {
            HttpResponse resp = getOne(points.getJsonArray(456));
            logger.info(resp.headers.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HttpResponse getOne(JsonArray point) throws IOException {
        return new HttpSender(Config.baseSearchUrl)
                .addHeader("Content-Type", Config.contentType)
                .setMethod(HttpSender.POST)
                .doCall(Config.body(point.getString(0), point.getString(1)));
    }

    public static void getFromCache() throws IOException {
        File stage1 = new File("/home/djodjo/data/dev/openthings.cc/openthings-sync/vlam-sync/stage1/");
        String stage2 = "/home/djodjo/data/dev/openthings.cc/openthings-sync/vlam-sync/stage2/";
        Flowable.fromArray(stage1.listFiles())
                .map(f -> new Scanner(f).useDelimiter("\\Z").next())
                .map(body -> Jsoup.parse(body))
                .doOnNext(doc -> logger.info(doc.getElementsByClass("searchresults__text").get(0).text()))
                .map(TestApi::getDetailsLink2)
                .flatMap(details -> Flowable.fromIterable(details))
                .doOnNext(o -> logger.info("fetching Details: " + o[1]))
                .map(o -> new Object[]{o[0], new Scanner(
                        new File(stage2 + "actor_" + o[1].replace("node/","") + ".html"))
                        .useDelimiter("\\Z").next()})
                .map(o -> TestApi.toJsonLD( ((String) o[0]).split(","),(String)o[1]))
                .doOnNext(o -> logger.info("Saving jsonLD: " + o.getString("@id")))
                .blockingSubscribe(actor -> saveStage3(actor));


    }

    public static void getAll() throws IOException {
        JsonArray pointsArray = (JsonArray) readFrom(Config.points);
        logger.info("will process " + pointsArray.size() + " queries..");
        // System.exit(0);
        Flowable<Object[]> points = Flowable.fromIterable(pointsArray)
                .skip(204)
                //delay not to overload the server
                .zipWith(Flowable.interval(3, TimeUnit.SECONDS),
                        (jsonElement, aLong) -> new Object[]{aLong+204, jsonElement}
                );
        points
                .doOnNext(o -> logger.info("processing Query: " + o[0]))
                .map(o -> new Object[]{o[0], getOne((JsonArray) o[1])})
                //save just for diff matching and offline work
                .doOnNext(o -> {
                    logger.info("saving Response: " + o[0]);
                    saveStage1(o);
                })
                .map(o -> Jsoup.parse(((HttpResponse) o[1]).body))
                .doOnNext(doc -> logger.info(doc.getElementsByClass("searchresults__text").get(0).text()))
                .map(TestApi::getDetailsLink2)
                .flatMap(details -> Flowable.fromIterable(details))
                .doOnNext(o -> logger.info("fetching Details: " + o[1]))
                .map(o -> new Object[]{o[0], new HttpSender(Config.baseUrl + "/" + o[1]).doCall()})
                .doOnNext(o -> {
                    logger.info("saving Details Response: " + o[0]);
                    saveStage2((HttpResponse) o[1]);
                })
                .map(TestApi::toJsonLD)
                .doOnNext(o -> logger.info("Saving jsonLD: " + o.getString("@id")))
                .blockingSubscribe(actor -> saveStage3(actor));
    }

    private static JsonObject toJsonLD(Object[] in) {
        String[] latLon = ((String) in[0]).split(",");
        String resp = ((HttpResponse) in[1]).body;
        return toJsonLD(latLon, resp);
    }

    private static JsonObject toJsonLD(String[] latLon, String resp) {
        Document doc = Jsoup.parse(resp);
        JsonObject job = new JsonObject()
                .put("@context", Config.context)
                .put("@type", "foaf:Agent");

        String idLp = UriUtils.getLastBit(doc.select("link[rel=canonical]").first().attr("href"));
        job.put("@id", actorBaseUri + idLp);
        String name = doc.select("div.wrapper > h1").first().text();
        //<div class="wrapper"><h1>Farmer name</h1>
        job.put("foaf:name", name);
        job.put("schema:name", name);


        if (doc.select("body").first().className().equals("node-type-verkooppunt")) {
            JsonArray ats = new JsonArray();
            job.put("ofd:actorType", ats);
            ats.put("at:pointOfSale");
            String kind = doc.select("h2.block-title > a").text();
            if (Config.actorTypesMap.containsKey(kind)) {
                ats.put("at:" + Config.actorTypesMap.get(kind));
            }
        } else {
            job.put("ofd:actorType", "at:producer");
        }
        //<div class="producer-detail__text"><p>
        Element descr = doc.select("div.producer-detail__text").first();
        if(descr!=null) {
            job.put("schema:description", descr.text());
        }

        // ---> topic
        JsonArray topics = new JsonArray();
        job.put("foaf:isPrimaryTopicOf", topics);
        topics.put(doc.select("link[rel=canonical]").first().attr("href"));
        topics.put(doc.select("link[rel=shortlink]").first().attr("href"));
        // <--- topic

        // --> image
        JsonArray imgs = new JsonArray();
        job.put("schema:image", imgs).put("foaf:depiction", imgs);
        doc.select("figure.producer-detail__profile-image > img")
                .forEach(el -> imgs.put(el.attr("src")));
        doc.select("div.producer-detail__picture > img")
                .forEach(el -> imgs.put(el.attr("src")));
        // <-- image

        // ---> contact info
        Element outlets = doc.select("p > a[href=\"#producer-outlets\"]").first();
        if(outlets!=null && outlets.parent().nextElementSibling()!=null) {
            String contactinfotxt = outlets.parent()
                    .nextElementSibling().html();
            if (!contactinfotxt.isEmpty()) {
                JsonObject contactInfo = new JsonObject().put("@type", "schema:ContactPoint");
                job.put("schema:contactPoint", contactInfo);
                JsonArray tel = new JsonArray();
                JsonArray mail = new JsonArray();
                JsonArray web = new JsonArray();
                job.put("schema:url", web).put("foaf:homepage", web);
                contactInfo.put("schema:telephone", tel).put("schema:email", mail)
                        .put("foaf:homepage", web).put("schema:url", web);
                Pattern pTel = Pattern.compile("(?:Tel|Gsm):\\s*(.+?)<br>");
                Pattern pMail = Pattern.compile("<a href=\"mailto:(.+?)\">");
                Pattern pWeb = Pattern.compile("<a href=\"(http.+?)\" target=\"_blank\">");

                Matcher m = pTel.matcher(contactinfotxt);
                while (m.find()) {
                    tel.put(m.group(1));
                }
                m = pMail.matcher(contactinfotxt);
                while (m.find()) {
                    mail.put(m.group(1));
                }
                m = pWeb.matcher(contactinfotxt);
                while (m.find()) {
                    web.put(m.group(1));
                }
            }
        }
        // <--- contact info

        // ---> social
        JsonArray socialData = new JsonArray();
        job.put("foaf:account", socialData);
        Element socialDiv = doc.select("div.producer-detail__social").first();
        if(socialDiv!=null) {
            socialDiv.children().forEach(el -> {
                JsonObject jel = new JsonObject()
                        .put("@type", "foaf:OnlineAccount")
                        .put("@id", el.attr("href"))
                        .put("skos:prefLabel", el.text());

                socialData.add(jel);
                if (el.classNames().contains("social__facebook")) {
                    jel.put("foaf:accountServiceHomepage", "https://www.facebook.com");
                    String[] accinfo = el.attr("href").replace("https://www.facebook.com/pages/", "")
                            .split("/");
                    jel.put("foaf:accountName", accinfo[0]);
                    if (accinfo.length > 1) {
                        jel.put("dct:identifier", accinfo[1]);
                    }
                }

            });
        }
        // <--- social

        // ---> geo
        JsonObject schemaGeo = new JsonObject();
        if(latLon!=null) {
            schemaGeo
                    .put("schema:latitude", latLon[0])
                    .put("schema:longitude", latLon[1]);
        }
        job.put("schema:geo", schemaGeo
                .put("@type", "schema:GeoCoordinates")
                .put("schema:addressCountry", "Belgium"));
        //schemaGeo.put("schema:address", doc.select("div[itemtype=http://schema.org/PostalAddress]").first().text());
        JsonObject addr = new JsonObject().put("@type", "schema:PostalAddress");
        schemaGeo.put("schema:address", addr);
        Element postal = doc.select("div[itemtype=http://schema.org/PostalAddress]").first();
        if(postal!=null) {
            postal.select("span[itemprop]")
                    .forEach(el -> addr.put("schema:" + el.attr("itemprop"), el.text()));
        }

        //<--- geo

        // ---> foodtypes
        JsonArray foodtypes = new JsonArray();
        job.put("ofd:offersFoodType", foodtypes);
        Element products = doc.select("ul.products").first();
        if(products!=null) {
            products.children().forEach(
                    el -> {
                        if (Config.foodTypesMap.containsKey(el.text())) {
                            foodtypes.put("ft:" + Config.foodTypesMap.get(el.text()));
                        }
                    }
            );
        }

        // <--- foodtypes


        // ---> relationships
        if (doc.select("body").first().className().equals("node-type-verkooppunt")) {
            job.put("ofd:upstreamRel", getRels(doc,true));
        } else {
            job.put("ofd:downstreamRel", getRels(doc,false));
        }
        // <--- relationships

        return job;
    }

    private static JsonArray getRels(Document doc, boolean isUpstream) {
        JsonArray rels = new JsonArray();
        final String relType;
        if(isUpstream) {
            relType = "at:producer";
        } else {
            relType = "at:pointOfSale";
        }
        
        Element outlets = doc.select("div#producer-outlets > ul.clearfix").first();
        if(outlets!=null) {
            outlets.children()
                    .forEach((Element el) -> {
                        String otherId = null;
                        Element linkedEl = el.select("div.outlet__kind > a").first();
                        //check if there is a link i.e. actor is in the system
                        if (linkedEl != null) {
                            otherId = UriUtils.getLastBit(linkedEl.attr("href"));
                            //TODO another side effect but easier for now
                            if(otherId!=null && isUpstream) {
                                //go fetch the producer as we don't get that with search.
                                File producer = new File("vlam-sync/stage2/actor_" + otherId + ".html");
                                if(!producer.exists()) {
                                    Single.just(linkedEl.attr("href"))
                                            .map(o -> new HttpSender(Config.baseUrl + "/" + o)
                                                    .doCall())
                                            .doOnSuccess(o -> {
                                                logger.info("saving Details Producer Response: " + o);
                                                saveStage2((HttpResponse) o);
                                            })
                                            .map(response -> toJsonLD(null, response.body))
                                            .doOnSuccess(o -> logger.info("Saving jsonLD: " + o.getString("@id")))
                                            .subscribe(actor -> saveStage3(actor));
                                } else {
                                    try {
                                        Single.just(new Scanner(producer).useDelimiter("\\Z").next())
                                                .map(o -> toJsonLD(null, o))
                                                .doOnSuccess(o -> logger.info("Saving jsonLD: " + o.getString("@id")))
                                                .subscribe(actor -> saveStage3(actor));
                                    } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        } else {
                            Element outlet = el.select("div.outlet__kind").first();
                            if(outlet!=null) {
                                otherId = Normalizer.normalize(outlet.text(), Normalizer.Form.NFD)
                                        .toLowerCase()
                                        .replace(" ", "-")
                                        .replace("'", "")
                                        .replaceAll("[^\\x00-\\x7F]", "");
                                final JsonObject extraActor = new JsonObject()
                                        .put("@context", Config.context)
                                        .put("@type", "foaf:Agent")
                                        .put("@id", actorBaseUri + otherId)
                                        .put("ofd:actorType", relType)
                                        .put("schema:geo", new JsonObject()
                                                .put("@type", "schema:GeoCoordinates")
                                                .put("schema:addressCountry", "Belgium")
                                                .put("schema:address", outlet.parent().ownText())
                                        );

                                //TODO Side effect here but what can we do ...
                                try {
                                    saveStage3(extraActor);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                        }
                        if(otherId!=null) {
                            rels.put(new JsonObject()
                                    .put("@type", "ofd:ActorRelationship")
                                    .put("ofd:relatedContext", relType)
                                    .put("ofd:relatedActor", actorBaseUri + otherId)
                            );
                        }
                    });
        }
        return rels;
    }

    //return Lat-Lon in [0] and HttpResp in [1]
    private static ArrayList<String[]> getDetailsLink2(Document doc) {
        ArrayList<String[]> res = new ArrayList<>();
        Element script = doc.getElementById("resultsmap").select("script").get(0);

        Pattern p = Pattern.compile("var myLatlng\\d+ = new google.maps.LatLng\\((\\d+\\.\\d+,\\d\\.\\d+)\\);" +
                ".+?<p class=\"map-moreinfo\"><a href=\"/([^\"]+)\">");

        Matcher m = p.matcher(script.html()); // you have to use html here and NOT text! Text will drop the 'key' part


        while (m.find()) {
            res.add(new String[]{m.group(1), m.group(2)});
        }
        return res;

    }

    private static ArrayList<String> getDetailsLink(Document doc) {
        ArrayList<String> res = new ArrayList<>();
        doc.getElementsByClass("producer")
                .forEach(new Consumer<Element>() {
                    @Override
                    public void accept(Element element) {
                        res.add(element.getElementsByClass("producer__data").get(0)
                                .getElementsByAttribute("href").get(1).val());
                    }
                });
        return res;

    }

    public static void saveStage1(Object[] obj) throws IOException {
        HttpResponse response = (HttpResponse) obj[1];
        if(response.responseCode>399) throw new RuntimeException("Server Error");
        final File f = new File(getFileName1((Long) obj[0]));
        FileWriter fw = new FileWriter(f);
        fw.write(response.body);
        fw.flush();
        fw.close();
    }


    public static void saveStage2(HttpResponse response) throws IOException {
        if(response.responseCode>399) throw new RuntimeException("Server Error");
        final File f = new File(getFileName2(response));
        FileWriter fw = new FileWriter(f);
        fw.write(response.body);
        fw.flush();
        fw.close();
    }

    public static void saveStage3(JsonObject actor) throws IOException {
        final File f = new File(getFileName3(actor));
        FileWriter fw = new FileWriter(f);
        JsonWriter jw = new JsonWriter(fw);
        jw.setIndent(" ");
        actor.write(jw);
        jw.flush();
        jw.close();
    }


    public static String getFileName1(long num) {
        return "vlam-sync/stage1/section_" + num + ".html";
    }

    public static String getFileName2(HttpResponse response) {
        Document doc = Jsoup.parse(response.body);
        String link = doc.select("link[rel=canonical]").get(0).attr("href");
        String name = UriUtils.getLastBit(link);
        return "vlam-sync/stage2/actor_" + name + ".html";
    }

    public static String getFileName3(JsonObject actor) {
        return "vlam-sync/stage3/" + UriUtils.getLastBit(actor.getString("@id")) + ".jsonld";
    }

}
