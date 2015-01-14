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

package cc.openthings.sync.geonames;

//info from http://www.geonames.org/export/ws-overview.html
public class Config {


    public static final String userString = "username=demo";
    public static final String baseUrl = "http://api.geonames.org/";
    public static final String uriSearch = "/searchJSON";
    public static final String uriWikiBBSearch = "/wikipediaBoundingBoxJSON";


    public static final String defParams = "?maxRows=50&maxRows=50&style=full&"+userString;

    public static String getBoundigboxParams(double north, double south, double east, double west) {
        return "&north="+north+"&south="+south + "&east="+east+"&west="+west;
    }

}
