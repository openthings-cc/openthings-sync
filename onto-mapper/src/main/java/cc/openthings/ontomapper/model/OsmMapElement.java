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

package cc.openthings.ontomapper.model;


import io.apptik.json.JsonElement;
import io.apptik.json.wrapper.JsonObjectWrapper;

import java.util.ArrayList;

public class OsmMapElement extends JsonObjectWrapper {

    public String getTagKey() {
        return getJson().getString("k");
    }
    
    public Boolean isMain() {
        return getJson().get("v").isNull();
    }

    public String getTagValue() {
        return getJson().getString("v");
    }

    public String getType() {
        return getJson().getString("@type");
    }

    public ArrayList<String> getValue() {
        JsonElement value =  getJson().get("@value");
        if(value.isJsonArray()) {
            return value.asJsonArray().toArrayList();
        }
        ArrayList<String> res = new ArrayList<>();
        res.add(value.asString());
        return res;
    }

    public String getLanguage() {
        return getJson().optString("@language");
    }

    public Boolean isRegex() {
        return getJson().optBoolean("regex", false);
    }




}
