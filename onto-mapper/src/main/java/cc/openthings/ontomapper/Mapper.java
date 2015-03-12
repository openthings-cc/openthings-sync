/*
 * Copyright (C) 2015 OpenThings Project
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

package cc.openthings.ontomapper;


import org.djodjo.json.JsonArray;
import org.djodjo.json.JsonElement;

import java.io.IOException;
import java.io.InputStreamReader;

public abstract class Mapper {
    
    JsonArray map;
    
    public Mapper(String jsonMap) {
        loadJsonMap(jsonMap);
    }
    
    protected void loadJsonMap(String jsonMap) {
        try {
            map = JsonElement.readFrom(new InputStreamReader(getClass().getClassLoader().getResourceAsStream(jsonMap))).asJsonArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Generates representation of otherThing using OpenThings ontology *
     * @param otherThing representation of the other thing using the other ontology
     * @return representation of otherThing using OpenThings ontology 
     */
    public abstract Object getThing(Object otherThing);

    /**
     * Generates representation of thing using the Other ontology*
     * @param thing representation of otherThing using OpenThings ontology
     * @return representation of the other thing using the other ontology
     */
    public abstract Object getOtherThing(Object thing);


}
