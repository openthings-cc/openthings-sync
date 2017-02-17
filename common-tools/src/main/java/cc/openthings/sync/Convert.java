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

package cc.openthings.sync;


import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import de.uni_stuttgart.vis.vowl.owl2vowl.converter.IRIConverter;
import de.uni_stuttgart.vis.vowl.owl2vowl.export.types.FileExporter;
import io.apptik.json.JsonObject;
import org.apache.jena.rdf.model.Model;
import org.semanticweb.owlapi.model.IRI;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

public class Convert {

    public static void main(String[] args) throws Exception {
        convert("GeoThingsOnto.jsonld", "JSONLD", "TTL");
       // convertToJsonDocs("GeoThingsOnto.jsonld", "JSONLD");
       // convertToVowl(".out/out.TTL");
    }

    public static void convert(String file, String fromLang, String toLang) throws IOException {
        Model model = Common.getModel(new File(file), fromLang);
        FileWriter fw = new FileWriter(new File(".out/out." + toLang));
        model.write(fw,toLang);
        model.close();
    }
    public static void convertToJsonDocs(String file, String fromLang) throws IOException, JsonLdError {
        Model model = Common.getModel(new File(file), fromLang);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        model.write(os, "JSON-LD");
        InputStream is = new ByteArrayInputStream(os.toByteArray());
        Object jObj = JsonUtils.fromInputStream(is);
        ArrayList<Object> expjObj = (ArrayList<Object>) JsonLdProcessor.expand(jObj);
        for(Object el:expjObj) {
            JsonObject job = new JsonObject((Map)el);
            FileWriter fw = new FileWriter(
                    ".out/jsonld/" + UriUtils.getSchemaId(job.get("@id").asString()+".json"));
            job.writeTo(fw);
            fw.close();
        }
    }
    public static void convertToVowl(String file)  throws Exception {
        IRI ontologyIri = IRI.create(new File(file));
        try {
            IRIConverter converter = new IRIConverter(ontologyIri);
            //Converter converter = new Converter(ontologyIri);
            converter.convert();
            converter.export(new FileExporter(new File(".out/out.json")));
        } catch (Exception e) {
            System.err.println(e.getClass().getName());
            throw e;
        }
    }
}
