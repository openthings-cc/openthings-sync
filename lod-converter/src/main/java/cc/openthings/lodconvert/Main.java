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


import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;

import java.io.IOException;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) throws Exception {
        //genLD(".onto/all_vf.TTL", "TTL", "vf");
        if(args == null || args.length<2) {
            System.out.println("Expected at least input file and format parameters");
            System.exit(1);
        }
        String inFile = args[0];
        String format = args[1];
        String outFName = "";
        LODIn lodIn = new LODIn(inFile, format, args.length>3);

        if(args.length>2) {
            outFName = args[2];
            genLD(lodIn, outFName);
        }

        if(args.length>3) {
            String ontDocIRI =args[3];
            lodIn.writeOntologyHtml(outFName, ontDocIRI);
        }
    }

    public static void genLD(LODIn lodIn, String outFile) throws IOException {
        StringBuilder sb = new StringBuilder();
        StringBuilder sb2 = new StringBuilder();
        ArrayList<Lang> langs = new ArrayList<>();
        langs.add(RDFFormat.RDFXML.getLang());
        langs.add(RDFFormat.JSONLD.getLang());
        langs.add(RDFFormat.NQUADS.getLang());
        langs.add(RDFFormat.NTRIPLES.getLang());
        langs.add(RDFFormat.TURTLE.getLang());
        langs.add(RDFFormat.RDFJSON.getLang());
        langs.add(RDFFormat.TRIG.getLang());
        langs.add(RDFFormat.TRIX.getLang());

        for (Lang lang : langs) {
            System.out.println("generating: " + lang);

            lodIn.write(lang, outFile);

            sb.append(
                    "# Rewrite rule to serve " + lang.getLabel() + " content if requested\n" +
                            "RewriteCond %{HTTP_ACCEPT} " + lang.getContentType().toHeaderString().replace("+", "\\+") + "\n" +
                            "RewriteRule ^$ /" + outFile + "." + lang.getFileExtensions().get(0) + " [R=303,L]\n\n");
            sb2.append("<link rel=\"alternate\" href=\"/" + outFile + "." + lang.getFileExtensions().get(0) + "\" " +
                    "type=\"" + lang.getContentType().toHeaderString() + "\"/>\n");
        }

        System.out.print(sb);
        System.out.print(sb2);
    }



}
