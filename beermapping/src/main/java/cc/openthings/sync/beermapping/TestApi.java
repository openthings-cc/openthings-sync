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

package cc.openthings.sync.beermapping;

import cc.openthings.sender.HttpResponse;
import cc.openthings.sender.HttpSender;
import org.djodjo.json.JsonArray;
import org.djodjo.json.JsonElement;
import org.djodjo.json.JsonObject;
import org.json.JSONObject;
import org.json.XML;

import java.io.FileWriter;
import java.io.IOException;

import static cc.openthings.sync.beermapping.Config.userString;

public class TestApi {

    public static void main(String[] args) {
        int max = 20000;
        HttpResponse httpResponse;
        JsonArray jarr = new JsonArray();
        for (int i = 0; i < max; i++) {
            try {
                //i = 8528;
                System.out.println("Try ID: " + i);
                httpResponse = doGenQuery("" + i);
                if (httpResponse.responseCode > 299) {
                    i--;
                    continue;
                }
                String data = httpResponse.body.replace("& ", "and ");
                JSONObject jsonOut = XML.toJSONObject(data);
                JsonObject job = JsonElement.readFrom(jsonOut.getJSONObject("bmp_locations").getJSONObject("location").toString()).asJsonObject();

                if (job.optInt("id")!=null && job.getInt("id") > 0) {
                    //we have a result, lets complete it

                    //fill in loc
                    httpResponse = doLocQuery("" + i);
                    if (httpResponse.responseCode > 299) {
                        i--;
                        continue;
                    }
                    data = httpResponse.body.replace("& ", "and ");
                    jsonOut = XML.toJSONObject(data);
                    job.merge(JsonElement.readFrom(jsonOut.getJSONObject("bmp_locations").getJSONObject("location").toString()).asJsonObject());

                    //fill in ratings
                    httpResponse = doRatingsQuery("" + i);
                    if (httpResponse.responseCode > 299) {
                        i--;
                        continue;
                    }
                    data = httpResponse.body.replace("& ", "and ");
                    jsonOut = XML.toJSONObject(data);
                    job.merge(JsonElement.readFrom(jsonOut.getJSONObject("bmp_locations").getJSONObject("location").toString()).asJsonObject());

                    if (job.getInt("imagecount") > 0) {
                        //fill in pics if we have any
                        httpResponse = doPicsQuery("" + i);
                        if (httpResponse.responseCode > 299) {
                            i--;
                            continue;
                        }
                        data = httpResponse.body.replace("& ", "and ");
                        jsonOut = XML.toJSONObject(data);
                        job.put("images", JsonElement.readFrom(jsonOut.getJSONObject("bmp_locations").get("location").toString()));
                    }

                    System.out.println(job);
                    jarr.add(job);
                } else {
                    System.out.println("Empty ID");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileWriter fw = new FileWriter("beer.json");
            jarr.writeTo(fw);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static HttpResponse doGenQuery(String locQuery) throws IOException {
        System.out.println(Config.baseUrl + Config.locquery
                + "/" + userString + "/" + locQuery);
        return new HttpSender(Config.baseUrl + Config.locquery
                + "/" + userString + "/" + locQuery
        )
                .setMethod(HttpSender.GET)
                .doCall();


    }

    private static HttpResponse doLocQuery(String locQuery) throws IOException {
        System.out.println(Config.baseUrl + Config.locmap
                + "/" + userString + "/" + locQuery);
        return new HttpSender(Config.baseUrl + Config.locmap
                + "/" + userString + "/" + locQuery
        )
                .setMethod(HttpSender.GET)
                .doCall();


    }

    private static HttpResponse doRatingsQuery(String locQuery) throws IOException {
        System.out.println(Config.baseUrl + Config.locscore
                + "/" + userString + "/" + locQuery);
        return new HttpSender(Config.baseUrl + Config.locscore
                + "/" + userString + "/" + locQuery
        )
                .setMethod(HttpSender.GET)
                .doCall();


    }

    private static HttpResponse doPicsQuery(String locQuery) throws IOException {
        System.out.println(Config.baseUrl + Config.locimage
                + "/" + userString + "/" + locQuery);
        return new HttpSender(Config.baseUrl + Config.locimage
                + "/" + userString + "/" + locQuery
        )
                .setMethod(HttpSender.GET)
                .doCall();


    }

}
