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


import org.djodjo.json.JsonArray;
import org.djodjo.json.JsonElement;
import org.djodjo.json.wrapper.JsonObjectWrapper;
import org.djodjo.json.wrapper.TypedJsonArray;

import java.util.ArrayList;

public class DataSet extends JsonObjectWrapper {

    public String getId() {
        return getJson().getString("datasetid");
    }

    public String getPublisher() {
        return getJson().getJsonObject("metas").getString("publisher");
    }

    public String getDomain() {
        return getJson().getJsonObject("metas").getString("domain");
    }

    public String getDescription() {
        return getJson().getJsonObject("metas").getString("description").replace("<p>", "").replace("</p>", "");
    }

    public String getRecordsCount() {
        return getJson().getJsonObject("metas").getString("records_count");
    }

    public String getTitle() {
        return getJson().getJsonObject("metas").getString("title");
    }

    public String getModified() {
        return getJson().getJsonObject("metas").getString("modified");
    }

    public String getLanguage() {
        return getJson().getJsonObject("metas").getString("language");
    }

    public String getTheme() {
        return getJson().getJsonObject("metas").getString("theme");
    }

    public String getKeyword() {
        return getJson().getJsonObject("metas").getString("keyword");
    }


    public String getLink() {
        return getJson().getJsonObject("metas").getString("references");
    }

    public ArrayList<String> getFeatures() {
        return getJson().getJsonArray("features").toArrayList();
    }

    public TypedJsonArray<FieldInfo> getFields() {
        return new TypedJsonArray<FieldInfo>() {
            @Override
            protected JsonElement to(FieldInfo value) {
                return value.getJson();
            }

            @Override
            protected FieldInfo get(JsonElement jsonElement, int pos) {
                return new FieldInfo().wrap(jsonElement);
            }
        }.wrap(getJson().getJsonArray("fields"));
    }

    public FieldInfo getFieldInfo(String fieldName) {
        FieldInfo res = new FieldInfo();
        JsonArray fields = getJson().getJsonArray("fields");
        for(JsonElement fi:fields) {
            if(fi.asJsonObject().getString("name").equals(fieldName)) {
               return res.wrap(fi);
            }
        }
       return res;
    }

}
