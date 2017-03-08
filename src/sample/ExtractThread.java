package sample;

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
public class ExtractThread extends Thread {
    private ArrayList<String> urls;
    private ArrayList<LinkedHashSet<String>> urlsRegs;
    private HashMap<String,HashSet<String>> metaDataRegs;
    private Set<String> urlsSet ;
    private MongoDBJDBC mongoDBJDBC ;
    private int flag;
    private static int count = 0;
    List<String> urlNeedCheck = new ArrayList<>();
    static Map<String, String> header = new HashMap<String, String>();

    public void initConnectionMap(){
        header.put("Host", "http://info.bet007.com");
        header.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:5.0) Gecko/20100101 Firefox/5.0");
        header.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        header.put("Accept-Language", "zh-cn,zh;q=0.5");
        header.put("Accept-Charset", "GB2312,utf-8;q=0.7,*;q=0.7");
        header.put("Connection", "keep-alive");

    }
    private static Logger logger = Logger.getLogger(ExtractThread.class);


    /**
     * 抓取线程构造函数
     * @param urls 待采集链接
     * @param urlsRegs 链接采集规则
     * @param metaDataRegs 元数据采集规则
     * @param mongoDBJDBC 数据库连接
     * @param flag 标志，flag=0为初次采集，flag=1为异构页面采集
     */
    public ExtractThread(ArrayList<String> urls,
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
        initConnectionMap();
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
            doc = Jsoup.connect(url).data(header).timeout(0).get();
            LinkedHashSet<String> regs = urlsRegs.get(n);
            Elements elements = new Elements();
            for (String regT : regs){
                String reg = regT;
                try {
                    if (regT.contains("::")){
                        reg = regT.split("::")[0];
                        int i = Integer.valueOf(regT.split("::")[1]);
                        Elements elements1 = doc.select(reg);
                        for (Element e : elements1){
                            elements.add(e.select("a").get(i));
                        }
                    }else {
                        elements = doc.select(reg);
                    }
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
            e.printStackTrace();
            logger.debug("[(url level)This url cannot connect:]"+url);
        }
    }
    public void extractMetaData(String url,List<org.bson.Document> bDocs){

        Document jDoc = null;
        try {
            jDoc = Jsoup.connect(url).data(header).timeout(0).get();
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
                bDoc.append("_url",url);
            }
            if (flag) {
                bDocs.add(bDoc);
            }
        } catch (IOException e) {
            System.out.println();
            logger.debug("[(metadata level)This url cannot connect:]"+url);
        }
    }
}
