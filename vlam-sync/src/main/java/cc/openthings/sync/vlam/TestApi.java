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
import io.reactivex.Flowable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.apptik.json.JsonElement.readFrom;

public class TestApi {
    private static final Logger logger = Logger.getLogger("vlam-sync");

    public static void main(String[] args) throws IOException {
       //example();
        test1();
        //getAll();
    }

    public static void test1() throws IOException {
        String filePath = "/home/djodjo/data/dev/openthings.cc/openthings-sync/vlam-sync/stage2/section_voedselteam-merelbeke.html";
        String body = new Scanner(new File(filePath)).useDelimiter("\\Z").next();
        Object[] object = new Object[]{"52.84,3.56",
                new HttpResponse(200, body,null)};
        logger.info(toJsonLD(object));
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

    public static HttpResponse getOne(JsonArray point)  throws IOException  {
        return new HttpSender(Config.baseSearchUrl)
                .addHeader("Content-Type", Config.contentType)
                .setMethod(HttpSender.POST)
                .doCall(Config.body(point.getString(0),point.getString(1)));
    }

    public static void getAll() throws IOException {
        JsonArray pointsArray = (JsonArray)readFrom(Config.points);
        logger.info("will process " + pointsArray.size() + " queries..");
       // System.exit(0);
        Flowable<Object[]> points = Flowable.fromIterable(pointsArray)
                //.skipLast(550)
                //delay not to overload the server
                .zipWith(Flowable.interval(5, TimeUnit.SECONDS),
                        (jsonElement, aLong) -> new Object[]{aLong, jsonElement}
                );
        points
                .doOnNext(o -> logger.info("processing Query: " + o[0]))
                .map(o -> new Object[]{o[0],  getOne((JsonArray) o[1]) })
                //save just for diff matching and offline work
                .doOnNext(o->{ logger.info("saving Response: " + o[0]); saveStage1(o);})
                .map(o -> Jsoup.parse(((HttpResponse) o[1]).body))
                .doOnNext(doc -> logger.info(doc.getElementsByClass("searchresults__text").get(0).text()))
                .map(TestApi::getDetailsLink2)
                .flatMap(details-> Flowable.fromIterable(details))
                .doOnNext(o -> logger.info("fetching Details: " + o[1]))
                .map(o-> new Object[] { o[0],new HttpSender(Config.baseUrl+"/"+o[1]).doCall()})
                .doOnNext(o->{ logger.info("saving Details Response: " + o[0]); saveStage2((HttpResponse) o[1]);})
                .map(TestApi::toJsonLD)
                .blockingSubscribe(o->logger.info(o));
    }


    private static String toJsonLD(Object[] in) {
        String[] latLon = ((String) in[0]).split(",");
        String resp = ((HttpResponse)in[1]).body;
        Document doc = Jsoup.parse(resp);
        JsonObject job = new JsonObject();
        job.put("@context",new JsonObject()
                .put("ofd", "https://w3id.org/openfooddata/onto/core#")
                .put("schema", "https://schema.org/")
                .put("foaf", "http://xmlns.com/foaf/0.1/")
                .put("skos", "http://www.w3.org/2004/02/skos/core#")
                .put("xsd", "http://www.w3.org/2001/XMLSchema#")
                .put("frapo", "http://purl.org/cerif/frapo/")
        )
        .put("@type", "foaf:Agent")
        ;
        JsonObject schemaGeo = new JsonObject();
        job.put("schema:geo", schemaGeo.put("@type", "schema:GeoCoordinates"));
        schemaGeo.put("schema:latitude", latLon[0]);
        schemaGeo.put("schema:longitude", latLon[1]);
        schemaGeo.put("schema:addressCountry","Belgium");
        //schemaGeo.put("schema:address", doc.select("div[itemtype=http://schema.org/PostalAddress]").first().text());
        JsonObject addr = new JsonObject().put("@type","http://schema.org/PostalAddress");
        schemaGeo.put("schema:address", addr);
        doc.select("div[itemtype=http://schema.org/PostalAddress]").first()
                .select("span[itemprop]")
                .forEach(el -> addr.put("schema:"+el.attr("itemprop"), el.text()));


        return job.toString();
    }

    //return Lat-Lon in [0] and HttpResp in [1]
    private static ArrayList<String[]> getDetailsLink2(Document doc) {
        ArrayList<String[]> res =  new ArrayList<>();
        Element script = doc.getElementById("resultsmap").select("script").get(0);

        Pattern p = Pattern.compile("var myLatlng\\d+ = new google.maps.LatLng\\((\\d+\\.\\d+,\\d\\.\\d+)\\);" +
                ".+?<p class=\"map-moreinfo\"><a href=\"/([^\"]+)\">");

        Matcher m = p.matcher(script.html()); // you have to use html here and NOT text! Text will drop the 'key' part


        while( m.find() )
        {
            res.add(new String[]{m.group(1), m.group(2)});
        }
        return res;

    }

    private static ArrayList<String> getDetailsLink(Document doc) {
        ArrayList<String> res =  new ArrayList<>();
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
        HttpResponse response  = (HttpResponse) obj[1];
        final File f = new File(getFileName1((Long) obj[0]));
        FileWriter fw = new FileWriter(f);
        fw.write(response.body);
        fw.flush();
        fw.close();
    }


    public static void saveStage2(HttpResponse response) throws IOException {
        final File f = new File(getFileName2(response));
        FileWriter fw = new FileWriter(f);
        fw.write(response.body);
        fw.flush();
        fw.close();
    }



    public static String getFileName1(long num) {
        return "vlam-sync/stage1/section_"+num+".html";
    }

    public static String getFileName2(HttpResponse response) {
        Document doc = Jsoup.parse(response.body);
        String link =  doc.select("link[rel=canonical]").get(0).attr("href");
        String name = UriUtils.getLastBit(link);
        return "vlam-sync/stage2/actor_"+name+".html";
    }

}
