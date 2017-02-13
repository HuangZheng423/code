package sample;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

/**
 * Created by huangzheng on 2017/1/28.
 */
public class ExtractMetedataThread implements Runnable {

    private ArrayList<String> urlsList ;
    private HashMap<String,HashSet<String>> metaDataRegs;
    private MongoDBJDBC mongoDBJDBC ;
    private static int count = 0;
    private Logger logger = Logger.getLogger(ExtractMetedataThread.class);

    public ExtractMetedataThread(ArrayList<String> urlsList ,
                                 HashMap<String,HashSet<String>> metaDataRegs,
                                 MongoDBJDBC mongoDBJDBC ){
        this.urlsList = urlsList;
        this.metaDataRegs = metaDataRegs;
        this.mongoDBJDBC = mongoDBJDBC;

    }
    @Override
    public void run() {
        logger.debug("M Thread " + count + "started");
        List<org.bson.Document> bDocs = new ArrayList<>();
        for (String url : urlsList) {
            Document jDoc = null;
            try {
                jDoc = Jsoup.connect(url).timeout(5000).get();
                org.bson.Document bDoc = new org.bson.Document();
                for (Map.Entry<String, HashSet<String>> entry : metaDataRegs.entrySet()) {
                    String key = entry.getKey();
                    HashSet<String> regs = entry.getValue();
                    StringBuffer sb = new StringBuffer();
                    for (String reg : regs) {
                        try {
                            String u = "";
                            Elements elements = null;
                            if (reg.length() > 3) {
                                String identify = reg.substring(0, 3);
                                if ("URL".equals(identify)) {
                                    String r = reg.substring(3, reg.length());
                                    elements = jDoc.select(r);
                                    if (!elements.isEmpty()) {
                                        for (Element e : elements) {
                                            u = e.absUrl("href");
                                            sb.append(u + ";");
                                        }
                                    }
                                } else {
                                    elements = jDoc.select(reg);
                                    if (!elements.isEmpty()) {
                                        for (Element e : elements) {
                                            u = e.text();
                                            sb.append(u + ";");
                                        }
                                    }
                                }

                            }
                        } catch (Exception e) {
//                            logger.debug("The reg [" + reg + "] does not apply to this url: [" + url + "]");
                            continue;
                        }
                    }
                    bDoc.append(key, sb.toString());
                }
                bDocs.add(bDoc);
            } catch (IOException e) {
//                logger.debug("[(metadata level)This url cannot connect:]"+url);
            }
        }
        mongoDBJDBC.insert(bDocs);
        logger.debug("M Thread " + count + "end");
        count++;

    }
}
