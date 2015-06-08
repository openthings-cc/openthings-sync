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

package cc.openthings.sync.osm;

import cc.openthings.ontomapper.OsmMapper;
import cc.openthings.sender.HttpResponse;
import cc.openthings.sender.HttpSender;
import org.djodjo.json.JsonArray;
import org.djodjo.json.JsonElement;
import org.djodjo.json.JsonObject;
import org.mapsforge.core.model.BoundingBox;

import java.io.IOException;
import java.util.ArrayList;


public final class TestApi {

    public static void main(String[] args) {
        HttpResponse httpResponse;
        try {
            httpResponse = doDummySearch("[route]", Config.QueryType.way);
            if (httpResponse != null) {
                System.out.println(httpResponse.body);
            }
            OsmMapper mapper = new OsmMapper();

            JsonArray elements = JsonElement.readFrom(httpResponse.body).asJsonObject().getJsonArray("elements");
            JsonArray mappedEls = new JsonArray();
            JsonArray nolabelEls = new JsonArray();

            JsonArray nodes = elements.get(0).asJsonObject().getJsonArray("nodes");
            for (JsonElement el : nodes) {
                httpResponse = doIdSearch(el.asLong(), Config.QueryType.node);
            }
            System.exit(0);
            for (JsonElement el : elements) {
                JsonObject mappedEl = (JsonObject) mapper.getThing(el);
                if (mappedEl != null) {
                    System.out.println(mappedEl);
                    mappedEls.put(mappedEl);
                    if (!mappedEl.has("http://www.w3.org/2000/01/rdf-schema#label")) {
                        nolabelEls.put(mappedEl.put("orig", el));
                    }
                }
            }

            System.out.println("Mapped els: " + mappedEls);
            System.out.println("Nolabel els: " + nolabelEls);
            System.out.println("Unmapped tags: " + JsonElement.wrap(mapper.getUnmappedTags()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static HttpResponse doDummySearch(String kv, Config.QueryType qType) throws IOException {
        ArrayList<String> kvs = new ArrayList<>();
        kvs.add(kv);
        // -0.42228282889341606,-0.2471923828125
        //  0.42228316416045586,0.24719204753637317
        return doSearch(kvs, new BoundingBox(48.29892057002853, 7.485171295702457, 48.8576793564343, 7.9795560613274565), qType);
    }

    public static HttpResponse doSearch(ArrayList<String> kvs, BoundingBox bounds, Config.QueryType qType) {

        try {
            System.out.println("overpass created request: " + kvs + ", bounds: " + bounds);
            return new HttpSender(Config.osmOverpassBaseUrl
                    + Config.buildQuery(kvs, bounds, qType))
                    .setMethod(HttpSender.GET)
                    .doCall();

        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }
        //  Config.osmOverpassBaseUrl + "node" + ((featureCode==null)?"[name]":featureCode) + "(" + Math.min(bounds.northeast.latitude, bounds.southwest.latitude)+","+Math.min(bounds.northeast.longitude, bounds.southwest.longitude)+","+Math.max(bounds.northeast.latitude, bounds.southwest.latitude)+","+ Math.max(bounds.northeast.longitude, bounds.southwest.longitude)+ ");" + "out%20500;"
        // Config.osmOverpassBaseUrl + "node" + ((featureCode==null)?"":"["+featureCode+"]") + "(" + Math.min(bounds.northeast.latitude, bounds.southwest.latitude)+","+Math.min(bounds.northeast.longitude, bounds.southwest.longitude)+","+Math.max(bounds.northeast.latitude, bounds.southwest.latitude)+","+ Math.max(bounds.northeast.longitude, bounds.southwest.longitude)+ ");" + "out%20500;"
        //Config.osmOverpassBaseUrl + "node" + ((featureCode==null)?"[tourism]":"["+featureCode+"]") + "(" + Math.min(bounds.northeast.latitude, bounds.southwest.latitude)+","+Math.min(bounds.northeast.longitude, bounds.southwest.longitude)+","+Math.max(bounds.northeast.latitude, bounds.southwest.latitude)+","+ Math.max(bounds.northeast.longitude, bounds.southwest.longitude)+ ");" + "out%20500;"
        //Config.osmOverpassBaseUrl + "node" + "[tourism]" + "(" + Math.min(bounds.northeast.latitude, bounds.southwest.latitude)+","+Math.min(bounds.northeast.longitude, bounds.southwest.longitude)+","+Math.max(bounds.northeast.latitude, bounds.southwest.latitude)+","+ Math.max(bounds.northeast.longitude, bounds.southwest.longitude)+ ");" + "out%205;"
    }

    public static HttpResponse doIdSearch(long id, Config.QueryType qType) {
        try {
            System.out.println("overpass created ID request: " + id + ", qType: " + qType);
            return new HttpSender(Config.osmOverpassBaseUrl
                    + Config.buildIdQuery(id, qType))
                    .setMethod(HttpSender.GET)
                    .doCall();

        } catch (IOException e) {
            e.printStackTrace();

            return null;
        }
    }
}
