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

import org.djodjo.json.JsonArray;

import java.util.HashMap;

//info from http://opendata.brussels.be/api/doc/
public class Config {


    public static final String baseUrl = "http://opendata.brussels.be/";
    public static final String dataSetSearch = "/api/datasets/1.0/search/";
    public static final String dataSetLookup = "/api/dataset/1.0/";

    public static final String recordSearch = "/api/records/1.0/search/";

    private static HashMap<String, JsonArray> mapBruOpenDataOpenThings =  new HashMap<>();
    static {
        mapBruOpenDataOpenThings.put("public-hospitals", new JsonArray()
                        .put("Amenity")
                        .put("HealthCentre")
                        .put("BuildingHospital")
                        .put("Hospital")
        );
        mapBruOpenDataOpenThings.put("tourist-offices", new JsonArray()
                        .put("TourismThing")
                        .put("TourismInformation")
                        .put("Tourist")
        );
        mapBruOpenDataOpenThings.put("dog-toilets", new JsonArray()
                //TODO
                //  .put("")

        );


    }

}
