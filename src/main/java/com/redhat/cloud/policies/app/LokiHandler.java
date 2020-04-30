/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.cloud.policies.app;

import com.redhat.cloud.policies.app.auth.RhIdPrincipal;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * @author hrupp
 */
@RequestScoped
public class LokiHandler {

  @Inject
  @RestClient
  Loki loki;

  @Inject
  RhIdPrincipal user;

  /*
  {
    "streams": [
      {
        "stream": {
          "label": "value"
        },
        "values": [
            [ "<unix epoch in nanoseconds>", "<log line>" ],
            [ "<unix epoch in nanoseconds>", "<log line>" ]
        ]
      }
    ]
  }

  $ curl -v -H "Content-Type: application/json" -XPOST -s "http://localhost:3100/loki/api/v1/push" --data-raw \
    '{"streams": [{ "stream": { "foo": "bar2" }, "values": [ [ "1570818238000000000", "fizzbuzz" ] ] }]}'
   */


  public void handle(String format, Object... args) {

    String string = String.format(format,args);

    JsonObjectBuilder job = Json.createObjectBuilder();

    JsonArray values = Json.createArrayBuilder()
        .add(System.currentTimeMillis() + "000000") // Golang Nanos
        .add(string)
        .build();
    JsonArray valuesList = Json.createArrayBuilder()
        .add(values)
        .build();

    String account = user.getAccount() != null ? user.getAccount() : "-1";
    JsonObject streamObject = Json.createObjectBuilder()
        .add("app","ui-backend")
        .add("account", account)
        .build();

    JsonObject stream2Object = Json.createObjectBuilder()
        .add("stream",streamObject)
        .add("values",valuesList)
        .build();

    JsonArray streamsArray = Json.createArrayBuilder()
        .add(stream2Object)

        .build();


    job.add("streams", streamsArray);


    JsonObject jo = job.build();

    String body = jo.toString();
    loki.push(body);

  }
}
