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

package cc.openthings.sync.openbrussels;

import cc.openthings.sender.HttpResponse;
import cc.openthings.sender.HttpSender;
import cc.openthings.sync.openbrussels.model.DataSet;
import org.djodjo.json.JsonArray;
import org.djodjo.json.JsonElement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class TestApi {



    public static void main(String[] args) {
        HttpResponse httpResponse;
        int expMax = 1000;
        int actualMax = 0;
        int totalRec = 0;
        try {
           //    httpResponse = getDataSets();
            httpResponse = getGeoDataSets();
            //  httpResponse = getRecords("pavillons-seniors");
            JsonArray respDataSets = JsonArray.readFrom(httpResponse.body).asJsonObject().getJsonArray("datasets");

            System.out.println(respDataSets.length());
            final HashMap<String, List<String>> dataSetTypes =  new HashMap<>();
            DataSet ds = null;
            for(JsonElement je:respDataSets) {
                ds =  new DataSet().wrap(je);

                List<String> cats = new ArrayList<>();
                if(ds.getTheme().isJsonArray()) {
                    cats.addAll(ds.getTheme().asJsonArray().toArrayList());
                } else {
                    cats.add(ds.getTheme().toString());
                }
                //put dataset in all buckets
                for(String cat:cats) {
                    List<String> dsIds = new ArrayList<>();
                    if (dataSetTypes.containsKey(cat)) {
                        dsIds = dataSetTypes.get(cat);
                    } else {
                        dataSetTypes.put(cat, dsIds);
                    }
                    dsIds.add(ds.getId());
                }

//                System.out.println("- "
//                                + ds.getId()
//                                //.replace("-", " ")

                System.out.println("- "
                                + ds.getId()
                                //.replace("-", " ")
//                                + " - " + ds.getTitle()
//                                + " - " + ds.getKeyword()
//                                + " - " + ds.getTheme()
//                                + " - " + ds.getFeatures()
//                                + " - " + ds.getPublisher()
                                + " - " + ds.getFields()
                );

            }

                     System.exit(0);

//            httpResponse = getRecords("pavillons-seniors");
            // httpResponse = getRecords("pavillons-seniors", "public-hospitals");


            //          httpResponse = getRecords("tourist-offices");


            for(JsonElement je:respDataSets) {
                httpResponse = getRecords(((DataSet)new DataSet().wrap(je)).getId());
                if(httpResponse.responseCode==200) {
                    JsonArray respRec = JsonArray.readFrom(httpResponse.body).asJsonObject().getJsonArray("records");
//                    FileWriter fw = new FileWriter(((DataSet)new DataSet().wrap(je)).getId()+".json");
//                    respRec.writeTo(fw);
//                    fw.flush();
//                    fw.close();
                    if (actualMax < respRec.length()) actualMax = respRec.length();
                    totalRec += respRec.length();
                    //System.out.println(respRec.length());
                }
            }

            System.out.println("expected max: " + expMax);
            System.out.println("actual max: " + actualMax);
            System.out.println("total: " + totalRec);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static HttpResponse getDataSets() throws IOException {
        return new HttpSender(Config.baseUrl + Config.dataSetSearch + "?rows=500")
                .setMethod(HttpSender.GET)
                .doCall();
    }

    private static HttpResponse getGeoDataSets() throws IOException {
        return new HttpSender(Config.baseUrl + Config.dataSetSearch + "?rows=500" +
                "&refine.language=en" +
                "&refine.features=geo")
                .setMethod(HttpSender.GET)
                .doCall();
    }

    private static HttpResponse getRecords(String dataset) throws IOException {
        return new HttpSender(Config.baseUrl + Config.recordSearch + "?rows=10000&dataset="+dataset)
                .setMethod(HttpSender.GET)
                .doCall();
    }

    private static HttpResponse getRecords(String... datasets) throws IOException {
        StringBuilder datasetsQuery = new StringBuilder();
        for(String dataset:datasets) {
            datasetsQuery.append("&dataset=").append(dataset);
        }

        return new HttpSender(Config.baseUrl + Config.recordSearch + "?rows=1000"+datasetsQuery.toString())
                .setMethod(HttpSender.GET)
                .doCall();
    }

    private static HashMap<String, DataSet> getDataSetMap() {
        HashMap<String, DataSet> res = new HashMap<>();

        return res;
    }
}
