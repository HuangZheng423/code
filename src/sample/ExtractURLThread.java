package sample;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

/**
 * Created by huangzheng on 2017/1/23.
 */
public class ExtractURLThread extends Thread {
    private ArrayList<String> urls;
    private ArrayList<LinkedHashSet<String>> urlsRegs;
    private HashMap<String,HashSet<String>> metaDataRegs;
    private Set<String> urlsSet ;
    private MongoDBJDBC mongoDBJDBC ;
    private int flag;
    private static int count = 0;
    List<String> urlNeedCheck = new ArrayList<>();

    private static Logger logger = Logger.getLogger(ExtractURLThread.class);


    public ExtractURLThread(ArrayList<String> urls,
                            ArrayList<LinkedHashSet<String>> urlsRegs,
                            HashMap<String,HashSet<String>> metaDataRegs,
                            MongoDBJDBC mongoDBJDBC,
                            int flag){
        PropertyConfigurator.configure("log4j.properties");
        this.urls = urls;
        this.urlsRegs = urlsRegs;
        this.metaDataRegs = metaDataRegs;
        this.mongoDBJDBC = mongoDBJDBC;
        this.flag = flag;
    }

    /**
     * 先获取详情页的链接，再根据元数据采集规则，对详情页元数据采集
     */
    @Override
    public void run() {
        logger.debug("U Thread " + count + " started");
//        logger.debug("[Extract url is:] " + url);

        List<org.bson.Document> bDocs = new ArrayList<>();
        for (String urlT : urls) {
            urlsSet = new HashSet<>();
            if (flag == 0) {
                getUrls(urlT, urlsRegs, 1);
                System.out.println(urlsSet.size());
                for (String url : urlsSet) {
                    extractMetaData(url,bDocs);
                }
            }else if (flag == 1){
                extractMetaData(urlT,bDocs);
            }

        }
        mongoDBJDBC.insert(bDocs);
//        System.out.println(urlsSet.size());
//        int len = urlsSet.size();
//        Object urls[] = urlsSet.toArray();
//        int flag = 0;
//        int n = 0;
//        while (flag + 500 < len){
//            ArrayList<String> urlsList = new ArrayList<>();
//            for (int i = flag; i < flag + 500; i++) {
//                urlsList.add(urls[i].toString());
//            }
//
//            ExtractMetedataThread emt = new ExtractMetedataThread(urlsList,metaDataRegs,mongoDBJDBC);
//            Thread t = new Thread(emt);
//            t.start();
//            flag += 500;
//            n++;
//        }
//        if (flag < len){
//            ArrayList<String> urlsList = new ArrayList<>();
//            for (int i = flag; i < len; i++) {
//                urlsList.add(urls[i].toString());
//            }
//
//            ExtractMetedataThread emt = new ExtractMetedataThread(urlsList,metaDataRegs,mongoDBJDBC);
//            Thread t = new Thread(emt);
//            t.start();
//        }

        logger.debug("U Thread " + count + " end");
        count++;

    }

    /**
     * 根据第一层的url和采集规则列表，获取详情页的链接，详情页的链接加到当前线程的urlsSet中
     * @param url 第一层url
     * @param urlsRegs 采集规则列表
     * @param n 采集层
     */
    public void getUrls(String url,
                        ArrayList<LinkedHashSet<String>> urlsRegs,
                        int n){
        if (n >= urlsRegs.size()){
            return;
        }

        Document doc = null;
        try {
            doc = Jsoup.connect(url).timeout(5000).get();
            LinkedHashSet<String> regs = urlsRegs.get(n);
            Elements elements = null;
            for (String reg : regs){
                try {
                    elements = doc.select(reg);
                } catch (Exception e1){
                    logger.debug("(url level)The reg [" + reg + "] does not apply to this url: [" + url + "]");
                    continue;
                }
                if (elements != null && elements.size()>0) {
                    for (Element e : elements) {
                        String urlTemp = e.absUrl("href");
                        if (n == urlsRegs.size()-1){
                            urlsSet.add(urlTemp);
                        }else {
                            getUrls(urlTemp,urlsRegs,n+1);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.debug("[(url level)This url cannot connect:]"+url);
        }
    }
    public void extractMetaData(String url,List<org.bson.Document> bDocs){

        Document jDoc = null;
        try {
            jDoc = Jsoup.connect(url).timeout(5000).get();
            org.bson.Document bDoc = new org.bson.Document();
            boolean flag = true; //用来判断是否插入该条数据，当结构发生变化后则不插入
            for (Map.Entry<String, HashSet<String>> entry : metaDataRegs.entrySet()) {
                String metaDataName = entry.getKey();
                //页面检查,如果文章标题不存在的话，就说明结构发生了变化。或者选择一个可以作为检查标准的字段，以此来作为检测标准
                String key = metaDataName.substring(0,metaDataName.length()-1);
                String isNeeded = metaDataName.substring(metaDataName.length()-1,metaDataName.length());

                HashSet<String> regs = entry.getValue();
                StringBuffer sb = new StringBuffer();
                Elements elements = null;
                for (String regTemp : regs) {
                    if (elements==null || elements.isEmpty()) {
                        String reg = regTemp;
                        try {
                            if (regTemp.contains(";")){
                                reg = regTemp.substring(0,regTemp.indexOf(";"));
                            }
                            String u = "";
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
                                            if (regTemp.contains(";")){
                                                String begin = regTemp.substring(regTemp.indexOf(";")+1,
                                                        regTemp.indexOf(","));
                                                String end = regTemp.substring(regTemp.indexOf(",")+1,
                                                        regTemp.length());
                                                String data = u.substring(u.indexOf(begin)+begin.length(),u.indexOf(end));
                                                sb.append(data + ";");
                                            }else {
                                                sb.append(u + ";");
                                            }
                                        }
                                    }
                                }

                            }


                        } catch (Exception e) {
                            logger.debug("(metadata level)The reg [" + reg + "] does not apply to this url: [" + url + "]");
                            continue;
                        }
                    }
                }
                if (elements.isEmpty() && "是".equals(isNeeded)){
                    //结构异常需要重新选择
                    System.out.println("url need check : " + url);
                    urlNeedCheck.add(url);
                    flag = false;
                    break;

                }
                String value = sb.toString();
                if (sb.length()>1) {
                    value = sb.substring(0, sb.length() - 1);
                }
                bDoc.append(key, value);
            }
            if (flag) {
                bDocs.add(bDoc);
            }
        } catch (IOException e) {
            logger.debug("[(metadata level)This url cannot connect:]"+url);
        }
    }
}
