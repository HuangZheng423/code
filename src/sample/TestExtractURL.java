package sample;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by huangzheng on 2017/2/28.
 */
public class TestExtractURL {
    public static void main(String[] args) throws IOException {
        HashSet<String> allUrlList = new HashSet<>();
        HashSet<String> firstUrlSet = new HashSet<>();
        Document document = Jsoup.connect("http://onlinelibrary.wiley.com/journal/10.1002/(ISSN)2198-3844/issues").get();
        Elements elements = document.select("ol[xmlns=http://www.w3.org/1999/xhtml][class=issueVolumes]>li>a");
        for (Element element : elements){
            String url = element.absUrl("href");
            firstUrlSet.add(url);
        }
        for (String url : firstUrlSet){
//        String url = "http://onlinelibrary.wiley.com/journal/10.1002/(ISSN)2198-3844/issues?activeYear=2017";
            Document document1 = Jsoup.connect(url).timeout(0).get();
            Elements elements1 = document1.select("div[class=issue]>a");
            HashSet<String> secondUrlSet = new HashSet<>();
            for (Element element : elements1){
                String url1 = element.absUrl("href");
                secondUrlSet.add(url1);
            }
//        System.out.println(secondUrlSet.size());
            for (String url1 : secondUrlSet){
//        String url1="http://onlinelibrary.wiley.com/doi/10.1002/advs.v4.2/issuetoc";
                Document document2 = Jsoup.connect(url1).timeout(0).get();
                Elements elements2 = document2.select("div[class=citation tocArticle]>a");
                for (Element element : elements2){
                    String url2 = element.absUrl("href");
                    allUrlList.add(url2);
                }
            }
        }

        System.out.println(allUrlList.size());
    }


    public void ijse() throws IOException {
        HashSet<String> allUrlList = new HashSet<>();
        HashSet<String> firstUrlSet = new HashSet<>();
        Document document = Jsoup.connect("http://hipatiapress.com/hpjournals/index.php/rise/issue/archive").get();
        Elements elements = document.select("div[style=clear:left;]>h4>a");
        for (Element element : elements){
            String url = element.absUrl("href");
            firstUrlSet.add(url);
        }
//        System.out.println(firstUrlSet.size());
        for (String url : firstUrlSet){
            Document document1 = Jsoup.connect(url).timeout(0).get();
            Elements elements1 = document1.select("td[class=tocArticleTitleAuthors]>div[class=tocTitle]>a");
            HashSet<String> secondUrlSet = new HashSet<>();
            for (Element element : elements1){
                String url1 = element.absUrl("href");
                allUrlList.add(url1);
            }

        }

        System.out.println(allUrlList.size());
    }
    public void xinli() throws IOException {
        HashSet<String> allUrlList = new HashSet<>();
        HashSet<String> firstUrlSet = new HashSet<>();
        Document document = Jsoup.connect("http://118.145.16.229:81/Jweb_xlxb/CN/article/showOldVolumn.do").get();
        Elements elements = document.select("td[width=90][height=50][valign=top][bgcolor=#CCCCCC][align=left][class=J_WenZhang]>a");
        for (Element element : elements){
            String url = element.absUrl("href");
            firstUrlSet.add(url);
        }
        for (String url : firstUrlSet){
//        String url = "http://118.145.16.229:81/Jweb_xlxb/CN/volumn/volumn_1849.shtml";
            Document document1 = Jsoup.connect(url).get();
            Elements elements1 = document1.select("tr>td[class=J_VM][valign=center][align=left][colspan=2][height=22]>a[class=12][target=_blank]");
            for (Element element : elements1){
                String url2 = element.absUrl("href");
                allUrlList.add(url2);
            }
        }

        System.out.println(allUrlList.size());
    }






    public void jeit() throws IOException {
        HashSet<String> allUrlList = new HashSet<>();
        HashSet<String> firstUrlSet = new HashSet<>();
        Document document = Jsoup.connect("http://jeit.ie.ac.cn/CN/article/showOldVolumn.do").get();
        Elements elements = document.select("td[width=90][height=50][valign=top][bgcolor=#CCCCCC][align=left][class=J_WenZhang]>a");
        for (Element element : elements){
            String url = element.absUrl("href");
            firstUrlSet.add(url);
        }
        for (String url : firstUrlSet){
            Document document1 = Jsoup.connect(url).get();
            Elements elements1 = document1.select("td[valign=top][bgcolor=#FFF9DF][class=J_VM]>a");
            for (Element element : elements1){
                String url2 = element.absUrl("href");
                allUrlList.add(url2);
            }
        }
    }
    public void beihang() throws IOException {
        HashSet<String> allUrlList = new HashSet<>();
        HashSet<String> firstUrlSet = new HashSet<>();
        Document document = Jsoup.connect("http://118.145.16.213/bhxb_skb/CN/article/showTenYearOldVolumn.do").get();
        Elements elements = document.select("td[width=60][height=30][valign=middle][bgcolor=#CCCCCC][align=left][class=J_WenZhang]>a");
        for (Element element : elements){
            String url = element.absUrl("href");
            firstUrlSet.add(url);
        }
        for (String url : firstUrlSet){
            Document document1 = Jsoup.connect(url).get();
            Elements elements1 = document1.select("td[height=20][valign=top][align=left][class=J_WenZhang]>a");
            HashSet<String> secondUrlSet = new HashSet<>();
            for (Element element : elements1){
                String url1 = element.absUrl("href");
                secondUrlSet.add(url1);
            }
            for (String url1 : secondUrlSet){
                Document document2 = Jsoup.connect(url1).get();
                Elements elements2 = document2.select("tr>td[class=J_VM][valign=center][align=left][colspan=2][height=22]>a[target=_blank]");
                for (Element element : elements2){
                    String url2 = element.absUrl("href");
                    allUrlList.add(url2);
                }
            }
        }

        System.out.println(allUrlList.size());
    }


    public  void dilikexue() throws IOException {
        HashSet<String> allUrlList = new HashSet<>();
        HashSet<String> firstUrlSet = new HashSet<>();
        Document document = Jsoup.connect("http://geoscien.neigae.ac.cn/CN/article/showOldVolumn.do").get();
        Elements elements = document.select("td[width=90][height=50][valign=top][bgcolor=#CCCCCC][align=left][class=J_WenZhang]>a");
        for (Element element : elements){
            String url = element.absUrl("href");
            firstUrlSet.add(url);
        }
        for (String url : firstUrlSet){
            Document document1 = Jsoup.connect(url).get();
            Elements elements1 = document1.select("td[align=left][style=PADDING-BOTTOM: 3px; PADDING-LEFT: 0px; WIDTH: 100%; PADDING-RIGHT: 0px; VERTICAL-ALIGN: top; PADDING-TOP: 0px]>a");
            for (Element element : elements1){
                String url2 = element.absUrl("href");
                allUrlList.add(url2);
            }
        }
        System.out.println(allUrlList.size());
    }


    public void chailiaoyanjiu () throws IOException {
        HashSet<String> allUrlList = new HashSet<>();
        HashSet<String> firstUrlSet = new HashSet<>();
        Document document = Jsoup.connect("http://www.cjmr.org/CN/article/showOldVolumnList.do").get();
        Elements elements = document.select("td[height=20][valign=top][align=left][class=J_WenZhang]>a");
        for (Element element : elements){
            String url = element.absUrl("href");
            firstUrlSet.add(url);
        }
        for (String url : firstUrlSet){
//           String url = "http://www.cjmr.org/CN/volumn/volumn_2188.shtml";
            System.out.println(url);
            Document document1 = Jsoup.connect(url).get();
            Elements elements1 = document1.select("td[align=left][style=PADDING-BOTTOM: 3px; PADDING-LEFT: 0px; WIDTH: 100%; PADDING-RIGHT: 0px; VERTICAL-ALIGN: top; PADDING-TOP: 0px]>a");
            for (Element element : elements1){
                String url2 = element.absUrl("href");
                allUrlList.add(url2);
            }
        }
        System.out.println(allUrlList.size());
    }


    public void yejinfenxi() throws IOException {
        HashSet<String> allUrlList = new HashSet<>();
        HashSet<String> firstUrlSet = new HashSet<>();
        Document document = Jsoup.connect("http://journal.yejinfenxi.cn/CN/article/showTenYearOldVolumn.do").get();
        Elements elements = document.select("td[width=60][height=30][valign=middle][bgcolor=#CCCCCC][align=left][class=J_WenZhang]>a");
        for (Element element : elements){
            String url = element.absUrl("href");
            firstUrlSet.add(url);
        }
//        System.out.println(firstUrlSet.size());
        for (String url : firstUrlSet){
            Document document1 = Jsoup.connect(url).get();
            Elements elements1 = document1.select("td[height=20][valign=top][align=left][class=J_WenZhang]>a");
            HashSet<String> secondUrlSet = new HashSet<>();
            for (Element element : elements1){
                String url1 = element.absUrl("href");
                secondUrlSet.add(url1);
            }
            for (String url1 : secondUrlSet){
//        String url1 = "http://journal.yejinfenxi.cn/CN/volumn/volumn_15.shtml";
                Document document2 = Jsoup.connect(url1).get();
                Elements elements2 = document2.select("tr>td[class=J_VM][valign=center][align=left][colspan=2][height=22]>a[target=_blank]");
                for (Element element : elements2){
                    String url2 = element.absUrl("href");
                    allUrlList.add(url2);
                }
            }
        }

        System.out.println(allUrlList.size());
    }

    public void dianzishejigongcheng() throws IOException {
        HashSet<String> allUrlList = new HashSet<>();
        HashSet<String> firstUrlSet = new HashSet<>();
        Document document = Jsoup.connect("http://mag.ieechina.com/oa/dlistnum.aspx").get();
        Elements elements = document.select("BODY[class]>DIV>DIV>DIV>DIV>DIV[class]>TABLE[border=0][cellpadding=0][cellspacing=0]>TBODY>TR>TD>TABLE>TBODY>TR>TD>TABLE[border=0][cellspacing=0]>TBODY>TR>TD>A");
        for (Element element : elements){
            String url = element.absUrl("href");
            url = toUtf8String(url);
            firstUrlSet.add(url);
        }
//        System.out.println(firstUrlSet.size());
        for (String url : firstUrlSet){
//            String url = "http://mag.ieechina.com/oa/scriptlsit.aspx?kind=Issue&issnum=01%C6%DA&year=2017%C4%EA";
            Document document1 = Jsoup.connect(url).get();
            Elements elements1 = document1.select("BODY[class]>DIV>DIV>DIV>DIV>DIV[class]>TABLE[width=100%][border=0][cellpadding=0][cellspacing=0]>TBODY>TR>TD>DIV[class]>DIV[class]>A[rel=external]");
            for (Element element : elements1){
                if (element.hasAttr("target")){
                    continue;
                }
                String url2 = element.absUrl("href");
                allUrlList.add(url2);
            }
        }
        System.out.println(allUrlList.size());
    }


    public void gongchengsheji() throws IOException {
        HashSet<String> allUrlList = new HashSet<>();
        HashSet<String> firstUrlSet = new HashSet<>();
        Document document = Jsoup.connect("http://www.zjujournals.com/gcsjxb/CN/article/showTenYearOldVolumn.do").get();
        Elements elements = document.select("td[width=60][height=30][valign=middle][bgcolor=#CCCCCC][align=left][class=J_WenZhang]>a");
        for (Element element : elements){
            String url = element.absUrl("href");
            firstUrlSet.add(url);
        }
        for (String url : firstUrlSet){
            Document document1 = Jsoup.connect(url).get();
            Elements elements1 = document1.select("td[height=20][valign=top][align=left][class=J_WenZhang]>a");
            HashSet<String> secondUrlSet = new HashSet<>();
            for (Element element : elements1){
                String url1 = element.absUrl("href");
                secondUrlSet.add(url1);
            }
            for (String url1 : secondUrlSet){
//        String url1 = "http://www.zjujournals.com/gcsjxb/CN/volumn/volumn_1152.shtml";
                Document document2 = Jsoup.connect(url1).get();
                Elements elements2 = document2.select("tr>td[class=J_VM][valign=center][align=left][colspan=2][height=22]:has(a)");
                for (Element element : elements2){
                    Element e = element.select("a[target=_blank]").get(0);
                    String url2 = e.absUrl("href");
                    allUrlList.add(url2);
                }
            }
        }

        System.out.println(allUrlList.size());
    }


    public void jilindaxue() throws IOException {
        HashSet<String> allUrlList = new HashSet<>();
        HashSet<String> firstUrlSet = new HashSet<>();
        Document document = Jsoup.connect("http://xuebao.jlu.edu.cn/gxb/CN/article/showTenYearOldVolumn.do").get();
        Elements elements = document.select("td[width=60][height=30][valign=middle][bgcolor=#CCCCCC][align=left][class=J_WenZhang]>a");
        for (Element element : elements){
            String url = element.absUrl("href");
            firstUrlSet.add(url);
        }
        for (String url : firstUrlSet){
            Document document1 = Jsoup.connect(url).get();
            Elements elements1 = document1.select("td[height=20][valign=top][align=left][class=J_WenZhang]>a");
            HashSet<String> secondUrlSet = new HashSet<>();
            for (Element element : elements1){
                String url1 = element.absUrl("href");
                secondUrlSet.add(url1);
            }
            for (String url1 : secondUrlSet){
//        String url1 = "http://xuebao.jlu.edu.cn/gxb/CN/volumn/volumn_1182.shtml";
                Document document2 = Jsoup.connect(url1).get();
                Elements elements2 = document2.select("TD[style=PADDING-BOTTOM: 3px; PADDING-LEFT: 0px; WIDTH: 100%; PADDING-RIGHT: 0px; VERTICAL-ALIGN: top; PADDING-TOP: 3px]>P[class]>A[target=_blank]");
                for (Element element : elements2){
                    String url2 = element.absUrl("href");
                    allUrlList.add(url2);
                }
            }
        }

        System.out.println(allUrlList.size());
    }

    public void guangxuejingmigongcheng() throws IOException {
        HashSet<String> allUrlList = new HashSet<>();
        HashSet<String> firstUrlSet = new HashSet<>();
        Document document = Jsoup.connect("http://www.eope.net/CN/article/showTenYearOldVolumn.do").get();
        Elements elements = document.select("td[width=60][height=30][valign=middle][bgcolor=#CCCCCC][align=left][class=J_WenZhang]>a");
        for (Element element : elements){
            String url = element.absUrl("href");
            firstUrlSet.add(url);
        }
        for (String url : firstUrlSet){
            Document document1 = Jsoup.connect(url).get();
            Elements elements1 = document1.select("td[height=20][valign=top][align=left][class=J_WenZhang]>a");
            HashSet<String> secondUrlSet = new HashSet<>();
            for (Element element : elements1){
                String url1 = element.absUrl("href");
                secondUrlSet.add(url1);
            }
            for (String url1 : secondUrlSet){
//        String url1 = "http://www.eope.net/CN/volumn/volumn_1195.shtml";
                Document document2 = Jsoup.connect(url1).get();
                Elements elements2 = document2.select("tr>td[height=22]:has(a)");
                for (Element element : elements2){
                    Element e = element.select("a[target=_blank]").get(0);
                    String url2 = e.absUrl("href");
                    allUrlList.add(url2);
                }
            }
        }

        System.out.println(allUrlList.size());
    }













    public static String toUtf8String(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0 && c <= 255) {
                sb.append(c);
            } else {
                byte[] b;
                try {
                    b = String.valueOf(c).getBytes("gb2312");
                } catch (Exception ex) {
                    System.out.println(ex);
                    b = new byte[0];
                }
                for (int j = 0; j < b.length; j++) {
                    int k = b[j];
                    if (k < 0)
                        k += 256;
                    sb.append("%" + Integer.toHexString(k).toUpperCase());
                }
            }
        }
        return sb.toString();
    }

}
