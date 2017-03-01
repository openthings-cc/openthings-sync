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
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.IRI;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

public class Convert {

    public static void main(String[] args) throws Exception {
        //convert("GeoThingsOnto.jsonld", "JSONLD", "TTL");
     //   convertOnt(".out/all_vf.TTL", "TTL", "TTL");
        convertOnt(".out/all_vf.TTL", "TTL", "JSONLD");
        //  convert(".out/out.JSONLD", "JSONLD", "TTL");
        // convertToJsonDocs("GeoThingsOnto.jsonld", "JSONLD");
        //convertToVowl(".onto/ofd-core.ttl");
        //  convertToVowl(".onto/ofd/ofd-food.ttl");
        // convertToVowl(".out/out.TTL");
    }

    public static void convertOnt(String file, String fromLang, String toLang) throws IOException {
        OntModel model = Common.getOntModel(new File(file).getAbsoluteFile().toString(), OntModelSpec.OWL_MEM, fromLang);
//        ExtendedIterator<OntClass> classes = model.listClasses();
//        classes.forEachRemaining(System.out::println);
//        ExtendedIterator<OntProperty> props = model.listAllOntProperties();
//        props.forEachRemaining(Convert::unionRangeAndDomainsForProp);
        FileWriter fw = new FileWriter(new File(".out/out." + toLang));
        model.write(fw, toLang);
        model.close();
    }

    public static void printOntProp(OntProperty p) {
        System.out.println("\n### " + p + ":");
        ExtendedIterator ress = p.listDomain();
        if (ress != null) {
            System.out.println("    domain: ");
            ress.forEachRemaining(r ->
                    System.out.println("           - " + r.toString()));
        }

        ress = p.listRange();
        if (ress != null) {
            System.out.println("    range: ");
            ress.forEachRemaining(r ->
                    System.out.println("           - " + r.toString()));
        }
    }

    public static void unionRangeAndDomainsForProp(OntProperty p) {
        unionRangeAndDomainsForProp(p, true);
    }

    public static void unionRangeAndDomainsForProp(OntProperty p, boolean splitProps) {
        System.out.println("\n### " + p + ":");
        ExtendedIterator<? extends OntResource> ress = p.listDomain();
        if (ress != null && ress.toSet().size() > 1) {
            ress = p.listDomain();
            ArrayList<RDFNode> nodes = new ArrayList<>();
            System.out.println("    domain: ");
            while (ress.hasNext()) {
                RDFNode r = (RDFNode) ress.removeNext();
                System.out.println("           - " + r.toString());
                nodes.add(r);
                if (splitProps) {
                    ExtendedIterator<? extends OntResource> ranges = p.listRange();
                    if(!ranges.hasNext()) {
                        OntProperty nProp = p.getOntModel().createOntProperty(p.getURI() + "_" + ((Resource) r).getLocalName());
                        nProp.addDomain((Resource) r);
                        nProp.addSuperProperty(p);
                    } else {
                        while (ranges.hasNext()) {
                            Resource rRange = ranges.removeNext();
                            OntProperty nProp = p.getOntModel().createOntProperty(p.getURI()
                                    + "_" + ((Resource) r).getLocalName()
                                    + "_" + (rRange).getLocalName());
                            nProp.addDomain((Resource) r);
                            nProp.addRange(rRange);
                            nProp.addSuperProperty(p);
                        }
                    }
                }
            }
            p.addDomain(p.getOntModel().createUnionClass(p.getURI() + "_domainuc",
                    p.getModel().createList(nodes.toArray(new RDFNode[]{}))));
        }

        ress = p.listRange();
        if (ress != null && ress.toSet().size() > 1) {
            ress = p.listRange();
            ArrayList<RDFNode> nodes = new ArrayList<>();
            System.out.println("    range: ");
            while (ress.hasNext()) {
                RDFNode r = (RDFNode) ress.removeNext();
                System.out.println("           - " + r.toString());
                nodes.add(r);
                if (splitProps) {
                    OntProperty nProp = p.getOntModel().createOntProperty(p.getURI() + "_" + ((Resource) r).getLocalName());
                    nProp.addRange((Resource) r);
                    //if still here we gace single domain
                    nProp.addDomain(p.getDomain());
                    nProp.addSuperProperty(p);
                }
            }
            p.addRange(p.getOntModel().createUnionClass(p.getURI() + "_rangeuc",
                    p.getModel().createList(nodes.toArray(new RDFNode[]{}))));
        }
    }

    public static void convert(String file, String fromLang, String toLang) throws IOException {
        Model model = Common.getModel(new File(file), fromLang);
        FileWriter fw = new FileWriter(new File(".out/out." + toLang));
        model.write(fw, toLang);
        model.close();
    }

    public static void convertToJsonDocs(String file, String fromLang) throws IOException, JsonLdError {
        Model model = Common.getModel(new File(file), fromLang);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        model.write(os, "JSON-LD");
        InputStream is = new ByteArrayInputStream(os.toByteArray());
        Object jObj = JsonUtils.fromInputStream(is);
        ArrayList<Object> expjObj = (ArrayList<Object>) JsonLdProcessor.expand(jObj);
        for (Object el : expjObj) {
            JsonObject job = new JsonObject((Map) el);
            FileWriter fw = new FileWriter(
                    ".out/jsonld/" + UriUtils.getSchemaId(job.get("@id").asString() + ".json"));
            job.writeTo(fw);
            fw.close();
        }
    }

    public static void convertToVowl(String file) throws Exception {
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
