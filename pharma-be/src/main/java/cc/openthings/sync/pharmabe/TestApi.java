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

package cc.openthings.sync.pharmabe;

import cc.openthings.sender.HttpResponse;
import cc.openthings.sender.HttpSender;

import java.io.IOException;

public class TestApi {

    public static void main(String[] args) {
        try {
            getNearByCoord(51, 4);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HttpResponse getNearByCoord(double lat, double lon) throws IOException {
        return new HttpSender(Config.baseUrl + Config.nearCoord + "?longitude="+lon+ "&latitude="+lat)
                .setMethod(HttpSender.GET)
                .doCall();
    }

    public static HttpResponse getNearById(int pharmaId) throws IOException {
        return new HttpSender(Config.baseUrl + Config.nearId + "?caregiver_id="+pharmaId)
                .setMethod(HttpSender.GET)
                .doCall();
    }

}
