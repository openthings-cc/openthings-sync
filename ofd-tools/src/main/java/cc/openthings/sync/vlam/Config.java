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

import io.apptik.json.JsonObject;

public class Config {

    public static final JsonObject context = new JsonObject();
    public static final String actorBaseUri = "https://w3id.org/openfooddata/actors/";

    static {
        context
                .put("ofd", "https://w3id.org/openfooddata/onto/core#")
                .put("ft", "https://w3id.org/openfooddata/vocab/foodtype#")
                .put("at", "https://w3id.org/openfooddata/vocab/actortype#")
                .put("schema", "https://schema.org/")
                .put("foaf", "http://xmlns.com/foaf/0.1/")
                .put("skos", "http://www.w3.org/2004/02/skos/core#")
                .put("xsd", "http://www.w3.org/2001/XMLSchema#")
                .put("frapo", "http://purl.org/cerif/frapo/")
                .freeze();
    }



}
