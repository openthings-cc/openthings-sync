package cc.openthings.sync.osm;

import cc.openthings.ontomapper.OsmMapper;
import org.djodjo.json.JsonArray;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;

import java.io.*;
import java.util.Map;

public class OsmReader {


    OsmMapper mapper;
    public OsmReader() {
        mapper = new OsmMapper();
    }


    //todo move
    public void readOsm(final File file) throws FileNotFoundException {
        Sink sinkImplementation = new Sink() {
            volatile int cntN = 0;
            volatile int cntW = 0;
            volatile int cntR = 0;
            JsonArray nodes = new JsonArray();

            @Override
            public void initialize(Map<String, Object> map) {

            }

            public void process(EntityContainer entityContainer) {
                Entity entity = entityContainer.getEntity();
                if (entity instanceof Node) {
                    cntN++;
                    if(entity.getTags()!=null && entity.getTags().size()>0) {
                        nodes.put(mapper.getThing(entity));
                    }
                    //System.out.println("NODE-->> " + entity);
                } else if (entity instanceof Way) {
                    cntW++;
                    //System.out.println("WAY-->> " + entity);
                } else if (entity instanceof Relation) {
                    cntR++;
                    //System.out.println("REL-->> " + entity);

                }


            }

            public void release() {
            }

            public void complete() {
                System.out.println("NODE-->> " + cntN);
                System.out.println("WAY-->> " + cntW);
                System.out.println("REL-->> " + cntR);
                System.out.println("UNMAPPED-->> " + mapper.getUnmappedTags());
                try {
                    File outJson = new File(file.getPath() + ".json");
                    Writer wr = new FileWriter(outJson.getPath());
                    System.out.println("writing to -->> " + outJson.getPath());
                    nodes.writeTo(wr);
                    wr.flush();
                    wr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }
        };

        boolean pbf = false;
        CompressionMethod compression = CompressionMethod.None;

        if (file.getName().endsWith(".pbf")) {
            pbf = true;
        } else if (file.getName().endsWith(".gz")) {
            compression = CompressionMethod.GZip;
        } else if (file.getName().endsWith(".bz2")) {
            compression = CompressionMethod.BZip2;
        }

        RunnableSource reader;

        if (pbf) {
            reader = new crosby.binary.osmosis.OsmosisReader(
                    new FileInputStream(file));
        } else {
            reader = new XmlReader(file, false, compression);
        }

        reader.setSink(sinkImplementation);

        Thread readerThread = new Thread(reader);
        readerThread.start();

        while (readerThread.isAlive()) {
            try {
                readerThread.join();
            } catch (InterruptedException e) {
        /* do nothing */
            }
        }

    }
}
