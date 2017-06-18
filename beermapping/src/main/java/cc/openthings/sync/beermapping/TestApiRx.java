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

package cc.openthings.sync.beermapping;

import org.djodjo.json.JsonArray;
import org.djodjo.json.JsonElement;
import org.djodjo.json.JsonObject;
import org.json.JSONObject;
import org.json.XML;

import java.io.FileWriter;
import java.io.IOException;

import cc.openthings.sender.HttpResponse;
import cc.openthings.sender.HttpSender;

import static cc.openthings.sync.beermapping.Config.userString;

public class TestApiRx {

    public static void main(String[] args) {
      int maxItems=20;
      //Flowable
    }

    private static HttpResponse doGenQuery(String locQuery) throws IOException {
        System.out.println(Config.baseUrl + Config.locquery
                + "/" + userString + "/" + locQuery);
        return new HttpSender(Config.baseUrl + Config.locquery
                + "/" + userString + "/" + locQuery
        )
                .setMethod(HttpSender.GET)
                .doCall();


    }

    private static HttpResponse doLocQuery(String locQuery) throws IOException {
        System.out.println(Config.baseUrl + Config.locmap
                + "/" + userString + "/" + locQuery);
        return new HttpSender(Config.baseUrl + Config.locmap
                + "/" + userString + "/" + locQuery
        )
                .setMethod(HttpSender.GET)
                .doCall();


    }

    private static HttpResponse doRatingsQuery(String locQuery) throws IOException {
        System.out.println(Config.baseUrl + Config.locscore
                + "/" + userString + "/" + locQuery);
        return new HttpSender(Config.baseUrl + Config.locscore
                + "/" + userString + "/" + locQuery
        )
                .setMethod(HttpSender.GET)
                .doCall();


    }

    private static HttpResponse doPicsQuery(String locQuery) throws IOException {
        System.out.println(Config.baseUrl + Config.locimage
                + "/" + userString + "/" + locQuery);
        return new HttpSender(Config.baseUrl + Config.locimage
                + "/" + userString + "/" + locQuery
        )
                .setMethod(HttpSender.GET)
                .doCall();


    }

}
