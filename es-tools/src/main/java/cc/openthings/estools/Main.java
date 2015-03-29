package cc.openthings.estools;


import org.djodjo.json.JsonArray;
import org.djodjo.json.JsonElement;
import org.djodjo.json.JsonObject;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        Main mm = new Main();
        mm.processHades();
    }

    private void inputData(JsonObject job) {
        EsClient esClient = new EsClient("", "");
        esClient.writeObject2es("ardennes-hicking", "poi", job.getInt("id").toString(), job);
    }

    private JsonObject processHades(JsonObject hadesJob) {
        JsonObject res = new JsonObject();
        //--> id
        res.put("id", hadesJob.getInt("id"));
        //--> NAME
        JsonArray name = new JsonArray();
        JsonElement jeName = hadesJob.get("titre");
        if (jeName.isJsonArray()) {
            name = jeName.asJsonArray();
        } else {
            name.add(jeName);
        }
        //PUT name
        res.put("name", name);
        // --> LOC
        //y=lat,x=lon, but we do [lat, lon]

        if (hadesJob.optJsonObject("geocodes") != null) {
            JsonArray latLon = new JsonArray();
            latLon.add(hadesJob.getJsonObject("geocodes").getJsonObject("geocode").get("y"));
            latLon.add(hadesJob.getJsonObject("geocodes").getJsonObject("geocode").get("x"));
            //PUT location
            res.put("location", latLon);
        }


        // -->DESC

        if (hadesJob.optJsonObject("descriptions") != null) {
            JsonArray desc = new JsonArray();
            if (hadesJob.getJsonObject("descriptions").get("description").isJsonArray()) {
                HashMap<String, StringBuilder> descs = new HashMap<>();
                descs.put("en", new StringBuilder());
                descs.put("fr", new StringBuilder());
                descs.put("nl", new StringBuilder());
                descs.put("de", new StringBuilder());
                for (JsonElement descJe : hadesJob.getJsonObject("descriptions").get("description").asJsonArray()) {
                  if(descJe.asJsonObject().opt("texte")!=null) {
                      JsonElement jeDesc = descJe.asJsonObject().get("texte");
                      if (jeDesc.isJsonArray()) {
                          for (JsonElement jeLang : jeDesc.asJsonArray()) {
                              descs.get(jeLang.asJsonObject().getString("lg")).append(jeLang.asJsonObject().getString("content") + "\\n");
                          }
                      } else {
                          descs.get(jeDesc.asJsonObject().getString("lg")).append(jeDesc.asJsonObject().getString("content") + "\\n");
                      }
                  }
                }
                if(descs.get("en").length()>0) {
                    desc.put(new JsonObject().put("lg", "en").put("content", descs.get("en").toString()));
                }
                if(descs.get("fr").length()>0) {
                    desc.put(new JsonObject().put("lg", "fr").put("content", descs.get("fr").toString()));
                }
                if(descs.get("nl").length()>0) {
                    desc.put(new JsonObject().put("lg", "nl").put("content", descs.get("nl").toString()));
                }
                if(descs.get("de").length()>0) {
                    desc.put(new JsonObject().put("lg", "de").put("content", descs.get("nl").toString()));
                }
            } else {
                JsonElement jeDesc = hadesJob.getJsonObject("descriptions").getJsonObject("description").get("texte");
                if (jeDesc.isJsonArray()) {
                    desc = jeDesc.asJsonArray();
                } else {
                    desc.add(jeDesc);
                }
            }

            //PUT desc
            res.put("desc", desc);

        }

        //--> PIC
        JsonArray pic = new JsonArray();
        if (hadesJob.optJsonObject("medias") != null) {
            JsonElement jePic = hadesJob.getJsonObject("medias").get("media");
            if (jePic.isJsonArray()) {
                pic = jePic.asJsonArray();
            } else {
                pic.add(jePic);
            }

            res.put("pic", pic);
        }

        //--> CAT
        JsonArray category = new JsonArray();
        if (hadesJob.optJsonObject("categories") != null) {
            JsonElement jeCategorie = hadesJob.getJsonObject("categories").get("categorie");
            if (jeCategorie.isJsonArray()) {
                for(JsonElement jeCat:jeCategorie.asJsonArray()) {
                    category.add(jeCat.asJsonObject().get("id"));
                }
            } else {
                category.add(jeCategorie.asJsonObject().get("id"));
            }

            res.put("category", category);
        }

      //  res.put("category", hadesJob.getJsonObject("categories").getJsonObject("categorie").get("id"));

        if (hadesJob.optJsonObject("contacts") != null) {
            JsonArray comm = new JsonArray();

            if (hadesJob.getJsonObject("contacts").get("contact").isJsonArray()) {
                if (hadesJob.getJsonObject("contacts").getJsonArray("contact").getJsonObject(0).optJsonObject("communications") != null) {
                    JsonElement jeComm = hadesJob.getJsonObject("contacts").getJsonArray("contact").getJsonObject(0).getJsonObject("communications").get("communication");
                    if (jeComm.isJsonArray()) {
                        for (JsonElement commJe : jeComm.asJsonArray()) {
                            pic.add(new JsonObject()
                                            .put("val", commJe.asJsonObject().get("val"))
                                            .put("typ", commJe.asJsonObject().get("typ"))
                            );
                        }
                    } else {
                        pic.add(new JsonObject()
                                        .put("val", jeComm.asJsonObject().get("val"))
                                        .put("typ", jeComm.asJsonObject().get("typ"))
                        );
                    }
                }
                res.put("contact", comm);
            } else {
                if (hadesJob.getJsonObject("contacts").getJsonObject("contact").optJsonObject("communications") != null) {
                    JsonElement jeComm = hadesJob.getJsonObject("contacts").getJsonObject("contact").getJsonObject("communications").get("communication");
                    if (jeComm.isJsonArray()) {
                        for (JsonElement commJe : jeComm.asJsonArray()) {
                            pic.add(new JsonObject()
                                            .put("val", commJe.asJsonObject().get("val"))
                                            .put("typ", commJe.asJsonObject().get("typ"))
                            );
                        }
                    } else {
                        pic.add(new JsonObject()
                                        .put("val", jeComm.asJsonObject().get("val"))
                                        .put("typ", jeComm.asJsonObject().get("typ"))
                        );
                    }
                }
                res.put("contact", comm);
            }


        }
        return res;
    }

    private void processHades() {
        try {
            JsonArray jarr = JsonElement.readFrom(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("AhGood.json"))).asJsonObject().getJsonObject("root").getJsonObject("offres").getJsonArray("offre");
            JsonArray newArr = new JsonArray();
            for (JsonElement je : jarr) {
                JsonObject toPut = processHades(je.asJsonObject());
                newArr.put(toPut);
                System.out.println("Will put: " + toPut);
                //inputData(toPut);
                Writer wr = new FileWriter("points.json");
                newArr.writeTo(wr);
                wr.close();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
