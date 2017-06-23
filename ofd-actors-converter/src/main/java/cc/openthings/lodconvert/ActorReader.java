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


import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.jsonldjava.core.*;
import com.github.jsonldjava.utils.JsonUtils;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.lib.InternalErrorException;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RIOT;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.lang.JsonLDReader;
import org.apache.jena.riot.system.ErrorHandler;
import org.apache.jena.riot.system.ParserProfile;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Context;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ActorReader extends JsonLDReader {

    //static Context jenaCtx;
    private final ErrorHandler errorHandler;
    private final ParserProfile profile;


//    static {
//        jenaCtx = new Context();
//        Object jsonldContextAsObject = null;
//        try {
//            jsonldContextAsObject = JsonUtils
//                    .fromURLJavaNet(new URL("https://openfooddata.github.io/actors/_common/context.jsonld"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        jenaCtx.set(RIOT.JSONLD_CONTEXT, jsonldContextAsObject);
//    }

    public ActorReader(Lang lang, ParserProfile profile, ErrorHandler errorHandler) {
        super(lang, profile, errorHandler);
        this.errorHandler = errorHandler;
        this.profile = profile;
    }



    @Override
    public void read(Reader reader, String baseURI, ContentType ct, StreamRDF output, Context context) {
        super.read(reader, baseURI, ct, output, context);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void read(InputStream in, String baseURI, ContentType ct, StreamRDF output, Context context) {
        try {
            Object jsonObject = JsonUtils.fromInputStream(in) ;

            if (context != null) {
                Object jsonldCtx = context.get(RIOT.JSONLD_CONTEXT);
                if (jsonldCtx != null) {
                    if (jsonObject instanceof Map) {
                        ((Map) jsonObject).put("@context", jsonldCtx);
                    } else {
                        errorHandler.warning("Unexpected: not a Map; unable to set JsonLD's @context",-1,-1);
                    }
                }
            }
            read(jsonObject, baseURI, ct, output, context) ;
        }
        catch (JsonProcessingException ex) {
            // includes JsonParseException
            // The Jackson JSON parser, or addition JSON-level check, throws up something.
            JsonLocation loc = ex.getLocation() ;
            errorHandler.error(ex.getOriginalMessage(), loc.getLineNr(), loc.getColumnNr());
            throw new RiotException(ex.getOriginalMessage()) ;
        }
        catch (IOException e) {
            errorHandler.error(e.getMessage(), -1, -1);
            IO.exception(e) ;
        }
    }

    private void read(Object jsonObject, String baseURI, ContentType ct, final StreamRDF output, Context context) {
        output.start() ;
        try {
            JsonLdTripleCallback callback = new JsonLdTripleCallback() {
                @Override
                public Object call(RDFDataset dataset) {

                    // Copy across namespaces
                    for (Map.Entry<String, String> namespace : dataset.getNamespaces().entrySet()) {
                        output.prefix(namespace.getKey(), namespace.getValue());
                    }

                    // Copy triples and quads
                    for ( String gn : dataset.keySet() ) {
                        Object x = dataset.get(gn) ;
                        if ( "@default".equals(gn) ) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> triples = (List<Map<String, Object>>)x ;
                            for ( Map<String, Object> t : triples ) {
                                Node s = createNode(t, "subject") ;
                                Node p = createNode(t, "predicate") ;
                                Node o = createNode(t, "object") ;
                                Triple triple = profile.createTriple(s, p, o, -1, -1) ;
                                output.triple(triple) ;
                            }
                        } else {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> quads = (List<Map<String, Object>>)x ;
                            Node g = createURI(gn) ;
                            for ( Map<String, Object> q : quads ) {
                                Node s = createNode(q, "subject") ;
                                Node p = createNode(q, "predicate") ;
                                Node o = createNode(q, "object") ;
                                Quad quad = profile.createQuad(g, s, p, o, -1, -1) ;
                                output.quad(quad) ;
                            }
                        }
                    }
                    return null ;
                }
            } ;
            JsonLdOptions options = new JsonLdOptions(baseURI);
            options.useNamespaces = true;
            JsonLdProcessor.toRDF(jsonObject, callback, options) ;
        }
        catch (JsonLdError e) {
            System.err.println(" ---------------- JOB: " + jsonObject);
            errorHandler.error(e.getMessage(), -1, -1);
            throw new RiotException(e) ;
        }
        output.finish() ;
    }

    public static String LITERAL    = "literal" ;
    public static String BLANK_NODE = "blank node" ;
    public static String IRI        = "IRI" ;

    private Node createNode(Map<String, Object> tripleMap, String key) {
        @SuppressWarnings("unchecked")
        Map<String, Object> x = (Map<String, Object>)(tripleMap.get(key)) ;
        return createNode(x) ;
    }

    private static final String xsdString = XSDDatatype.XSDstring.getURI() ;

    private Node createNode(Map<String, Object> map) {
        String type = (String)map.get("type") ;
        String lex = (String)map.get("value") ;
        if ( type.equals(IRI) )
            return createURI(lex) ;
        else if ( type.equals(BLANK_NODE) )
            return createBlankNode(lex);
        else if ( type.equals(LITERAL) ) {
            String lang = (String)map.get("language") ;
            String datatype = (String)map.get("datatype") ;
            if ( Objects.equals(xsdString, datatype) )
                // In RDF 1.1, simple literals and xsd:string are the same.
                // During migration, we prefer simple literals to xsd:strings.
                datatype = null ;
            if ( lang == null && datatype == null )
                return profile.createStringLiteral(lex,-1, -1) ;
            if ( lang != null )
                return profile.createLangLiteral(lex, lang, -1, -1) ;
            RDFDatatype dt = NodeFactory.getType(datatype) ;
            return profile.createTypedLiteral(lex, dt, -1, -1) ;
        } else
            throw new InternalErrorException("Node is not a IRI, bNode or a literal: " + type) ;
    }

    private Node createBlankNode(String str) {
        if ( str.startsWith("_:") )
            str = str.substring(2);
        return profile.createBlankNode(null, str, -1,-1);
    }

    private Node createURI(String str) {
        if ( str.startsWith("_:") )
            return createBlankNode(str);
        else
            return profile.createURI(str, -1, -1) ;
    }

}
