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

import java.io.IOException;
import cc.openthings.sender.HttpResponse;
import cc.openthings.sender.HttpSender;
import org.djodjo.json.JsonObject;

public final class TestApi {

    public static void main(String[] args) {
        HttpResponse httpResponse;
        try {
            httpResponse = doSearch(null);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static HttpResponse doSearch(String featureCode) throws IOException {
        return new HttpSender(Config.osmOverpassBaseUrl
                + "node" + ((featureCode == null) ? "[name]" : featureCode) +
                + Config.getBoundigboxParams(54.1,4.4,5.2,55.2)
                + "out%20500;" )
                .setMethod(HttpSender.GET)
                .doCall();

    }

    public static HttpResponse doSearch(String featureCode, LatLngBounds bounds) {

                Config.osmOverpassBaseUrl
                        + "(" + Math.min(bounds.northeast.latitude, bounds.southwest.latitude)
                        + "," + Math.min(bounds.northeast.longitude, bounds.southwest.longitude)
                        + "," + Math.max(bounds.northeast.latitude, bounds.southwest.latitude)
                        + "," + Math.max(bounds.northeast.longitude, bounds.southwest.longitude) + ");"

                //  Config.osmOverpassBaseUrl + "node" + ((featureCode==null)?"[name]":featureCode) + "(" + Math.min(bounds.northeast.latitude, bounds.southwest.latitude)+","+Math.min(bounds.northeast.longitude, bounds.southwest.longitude)+","+Math.max(bounds.northeast.latitude, bounds.southwest.latitude)+","+ Math.max(bounds.northeast.longitude, bounds.southwest.longitude)+ ");" + "out%20500;"
                // Config.osmOverpassBaseUrl + "node" + ((featureCode==null)?"":"["+featureCode+"]") + "(" + Math.min(bounds.northeast.latitude, bounds.southwest.latitude)+","+Math.min(bounds.northeast.longitude, bounds.southwest.longitude)+","+Math.max(bounds.northeast.latitude, bounds.southwest.latitude)+","+ Math.max(bounds.northeast.longitude, bounds.southwest.longitude)+ ");" + "out%20500;"
                //Config.osmOverpassBaseUrl + "node" + ((featureCode==null)?"[tourism]":"["+featureCode+"]") + "(" + Math.min(bounds.northeast.latitude, bounds.southwest.latitude)+","+Math.min(bounds.northeast.longitude, bounds.southwest.longitude)+","+Math.max(bounds.northeast.latitude, bounds.southwest.latitude)+","+ Math.max(bounds.northeast.longitude, bounds.southwest.longitude)+ ");" + "out%20500;"
                //Config.osmOverpassBaseUrl + "node" + "[tourism]" + "(" + Math.min(bounds.northeast.latitude, bounds.southwest.latitude)+","+Math.min(bounds.northeast.longitude, bounds.southwest.longitude)+","+Math.max(bounds.northeast.latitude, bounds.southwest.latitude)+","+ Math.max(bounds.northeast.longitude, bounds.southwest.longitude)+ ");" + "out%205;"
                , null
                , listener,
                errorListener);

        System.out.println("overpass created request: " + featureCode + ", bounds: " + bounds);
    }

}
