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

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UriUtils {

    private final static Pattern lastBitPattern = Pattern.compile(".*/([^/?]+).*");

    private UriUtils() {

    }

    public static String getLastBit(URI uri) {
        return getLastBit(uri.toString());
    }

    public static String getLastBit(String uri) {
        Matcher m = lastBitPattern.matcher(uri);
        if (m.matches()) {
            return m.group(1);
        }
        //should not happen as uri must be a valid URI
        throw new RuntimeException("Cannot get last bit from uri: " + uri);
    }


    public static String getSchemaId(String uri) {
        return getSchemaId(URI.create(uri));
    }

    public static String getSchemaId(URI uri) {
        return getLastBit(uri) + ((uri.getFragment() != null) ? "_" + uri.getFragment() : "");
    }


}
