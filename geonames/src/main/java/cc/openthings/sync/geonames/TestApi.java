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

package cc.openthings.sync.geonames;
import cc.openthings.sender.HttpResponse;
import cc.openthings.sender.HttpSender;

import java.io.IOException;

public class TestApi {

    public static void main(String[] args) {
        HttpResponse httpResponse;
        try {
            //httpResponse = doSearch();
            httpResponse = doWikiBBSearch();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static HttpResponse doSearch() throws IOException {
        return new HttpSender(Config.baseUrl + Config.uriSearch
                + Config.defParams
                + Config.getBoundigboxParams(54.1,4.4,5.2,55.2)
                + "&featureCode=ans" )
                .setMethod(HttpSender.GET)
                .doCall();
    }
    private static HttpResponse doWikiBBSearch() throws IOException {
        return new HttpSender(Config.baseUrl + Config.uriWikiBBSearch
                + Config.defParams
                + Config.getBoundigboxParams(54.1,4.4,5.2,55.2)
               //does not apply :(
               // + "&featureCode=ans"
        )
                .setMethod(HttpSender.GET)
                .doCall();
    }
}
