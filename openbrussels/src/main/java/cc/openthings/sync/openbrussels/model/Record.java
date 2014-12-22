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

package cc.openthings.sync.openbrussels.model;

import org.djodjo.json.JsonElement;
import org.djodjo.json.JsonObject;
import org.djodjo.json.wrapper.JsonObjectWrapper;

import java.util.Iterator;
import java.util.Map;

public class Record extends JsonObjectWrapper{

    public String getDatasetId() {
        return getJson().getString("datasetid");
    }

    public String getId() {
        return getJson().getString("recordid");
    }

    public String getTimestamp() {
        return getJson().getString("record_timestamp");
    }

    public JsonObject getFiledsRow() {
        return getJson().getJsonObject("fields");
    }

    public JsonObject getFileds(DataSet dataSet) {
        JsonObject res = new JsonObject();
        JsonObject rowFields =  getFiledsRow();
        Iterator<Map.Entry<String,JsonElement>> it = rowFields.iterator();
        while(it.hasNext()) {
            Map.Entry<String,JsonElement> ff = it.next();
            res.put(dataSet.getFieldInfo(ff.getKey()).getLabel(), ff.getValue());
        }


        return res;
    }

    public Double getLat() {
        JsonObject geom = getJson().optJsonObject("geometry");
        if(geom==null) return null;
        return geom.getJsonArray("coordinates").getDouble(1);
    }

    public Double getLon() {
        JsonObject geom = getJson().optJsonObject("geometry");
        if(geom==null) return null;
        return geom.getJsonArray("coordinates").getDouble(0);
    }




}
