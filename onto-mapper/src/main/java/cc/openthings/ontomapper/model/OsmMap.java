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
import io.apptik.json.wrapper.TypedJsonArray;

public class OsmMap extends TypedJsonArray<OsmMapElement> {

    @Override
    protected OsmMapElement get(JsonElement jsonElement, int pos) {
        return new OsmMapElement().wrap(jsonElement.asJsonObject());
    }

    @Override
    protected JsonElement to(OsmMapElement value) {
        return value.getJson();
    }
}
