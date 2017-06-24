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

package cc.openthings.lodconvert;


import cc.openthings.sync.Common;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.apptik.json.JsonArray;
import io.apptik.json.JsonObject;
import io.apptik.json.JsonWriter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFParserRegistry;
import org.apache.jena.riot.ReaderRIOTFactory;
import org.apache.jena.riot.system.ErrorHandler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LODIn {

    Model model;
    final HashMap job;
    final ReaderRIOTFactory riotFac =
            (language, profile) -> new ActorReader(language, profile, new ErrorHandler() {
                @Override
                public void warning(String message, long line, long col) {
                    System.err.println(message + ": " + line + "::" + col);
                }

                @Override
                public void error(String message, long line, long col) {
                    System.err.println("------------ THIS IS ME!!!!");
                    System.err.println(message + ": " + line + "::" + col);
                    //throw new RuntimeException(message + ": " + line + "::" + col);
                }

                @Override
                public void fatal(String message, long line, long col) {
                    System.err.println("------------ THIS IS ME!!!!");
                    System.err.println(message + ": " + line + "::" + col);
                    //throw new RuntimeException(message + ": " + line + "::" + col);
                }
            });

    public LODIn(String file) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File from = new File(file);
        TypeReference<HashMap<String, Object>> typeRef
                = new TypeReference<HashMap<String, Object>>() {
        };
        job = mapper.readValue(from, typeRef);
        RDFParserRegistry.registerLangTriples(Lang.JSONLD, riotFac);
        model = Common.getModel(new File(file), RDFFormat.JSONLD.getLang().getName());

        if (model == null) {
            throw new RuntimeException("Error parsing");
        }
    }

    public Model model() {
        return model;
    }

    public void write(String toLang) throws IOException {
        write(toLang, toLang);
    }

    public void write(Lang toLang) throws IOException {
        write(toLang.getName(), toLang.getFileExtensions().get(0));
    }

    public void write(Lang toLang, String outFile) throws IOException {
        write(toLang.getName(), toLang.getFileExtensions().get(0), outFile);
    }

    public void write(String toLang, String ext) throws IOException {
        write(toLang, ext, "out");
    }

    public void write(String toLang, String ext, String outFile) throws IOException {
        File ff = new File(".out/" + outFile.replace("/", "_") +
                "/" + outFile.replace("/", "_") + "." + ext.replace("/", "_"));
        ff.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(ff);
        model.write(fw, toLang);
        fw.flush();
        fw.close();
    }

    public void writeHtml(String outFName, String extraHeader) throws IOException {
        File ff = new File(".out/" + outFName.replace("/", "_") + "/index.html");
        ff.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(ff);
        job.put("extraHeader", extraHeader);
        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("html.mustache");
        try {
            mustache.execute(fw, job).flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fw.close();
        }
    }

    public void writeGeoJson(String outFName) throws IOException {
        Double lon = null, lat = null;
        if (job.containsKey("schema:geo")) {
            Map geo = (Map) job.get("schema:geo");
            if (geo.containsKey("schema:longitude") && geo.containsKey("schema:latitude")) {
                lon = Double.parseDouble((String) geo.get("schema:longitude"));
                lat = Double.parseDouble((String) geo.get("schema:latitude"));
            }
        }
        if (lon == null || lat == null) return;

        File ff = new File(".out/" + outFName.replace("/", "_") + "/"
                + outFName.replace("/", "_") + ".geojson");
        ff.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(ff);
        JsonObject gj = new JsonObject()
                .put("type", "Feature")
                .put("geometry", new JsonObject()
                        .put("type", "Point")
                        .put("coordinates", new JsonArray().put(lon).put(lat))
                )
                .put("properties", job);

        gj.write(new JsonWriter(fw));
        fw.flush();
        fw.close();
    }


    public JsonObject writeGeoJsonMin(String outFName) throws IOException {
        Double lon = null, lat = null;
        if (job.containsKey("schema:geo")) {
            Map geo = (Map) job.get("schema:geo");
            if (geo.containsKey("schema:longitude") && geo.containsKey("schema:latitude")) {
                lon = Double.parseDouble((String) geo.get("schema:longitude"));
                lat = Double.parseDouble((String) geo.get("schema:latitude"));
            }
        }
        if (lon == null || lat == null) return null;

        File ff = new File(".out/" + outFName.replace("/", "_") + "/"
                + outFName.replace("/", "_") + "_min.geojson");
        ff.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(ff);
        JsonObject gj = new JsonObject()
                .put("type", "Feature")
                .put("geometry", new JsonObject()
                        .put("type", "Point")
                        .put("coordinates", new JsonArray().put(lon).put(lat))
                )
                .put("properties", new JsonObject()
                        .put("id", job.get("@id"))
                        .put("name", job.get("schema:name"))
                        .put("img", job.get("schema:image"))
                        .put("type", job.get("ofd:actorType")));
        if (job.containsKey("ofd:upstreamRel")) {
            gj.getJsonObject("properties").put("upstream", job.get("ofd:upstreamRel"));
        }
        if (job.containsKey("ofd:downstreamRel")) {
            gj.getJsonObject("properties").put("downstream", job.get("ofd:downstreamRel"));
        }


        gj.write(new JsonWriter(fw));
        fw.flush();
        fw.close();
        return gj;
    }


}
