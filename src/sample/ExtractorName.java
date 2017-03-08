package sample;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by huangzheng on 2017/2/23.
 */
public class ExtractorName {
    public static void main(String[] args) throws IOException {

        String baseUrl = "http://person.zju.edu.cn/dept-person-A-A06000.html?count=&sort=&page=";
        List<String> urlList = new ArrayList<>();
        List<String> data = new ArrayList<>();
        for (int i = 1; i <= 46; i++) {
            String url = baseUrl + String.valueOf(i);
            urlList.add(url);
        }
        for (String url : urlList) {
            Document document = Jsoup.parse(new URL(url).openStream(), "gbk", url);
            Elements elements = document.select("table[class=tab_blue]>tbody>tr");
            elements.remove(0);
            elements.remove(0);
            elements.remove(elements.size()-1);
            for (Element e : elements){
                String name = e.select("td").get(0).text();
                String pos = e.select("td").get(1).text();
                String dept = e.select("td").get(2).text();
                System.out.println(name);
                data.add(name + "\t" + pos + "\t" + dept);
            }
        }


        String filePath = "/Users/huangzheng/Desktop/file.txt";
        File file = new File(filePath);
        if (file.exists()){
            file.delete();
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        for (String d : data){
            bw.write(d + "\n");
        }
        bw.close();
    }

    public void getFirst() throws IOException {




        String url = "http://cee.xmu.edu.cn/faculities/all";
        List<String> data = new ArrayList<>();
        Document document = Jsoup.parse(new URL(url).openStream(), "utf8", url);
        Elements elements = document.select("div[class=fac-detail]>h3>a");
        for (Element e : elements){
            String name = e.text();
            System.out.println(name);
            data.add(name);
        }
    }
}
