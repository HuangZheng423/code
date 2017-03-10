package sample;

import com.sun.org.apache.regexp.internal.RE;
import com.sun.org.apache.xerces.internal.xs.LSInputList;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by huangzheng on 2017/1/5.
 */
public class Test {

    public static void main(String[] args) throws IOException {
        URL url = new URL("http://journal.yejinfenxi.cn/CN/article/showTenYearOldVolumn.do");
        String host = url.getHost();
        String a = "journal.yejinfenxi.cn";
        System.out.println(host);
        String s[] = host.split("\\.");
        System.out.println(s.length);
        if (host.contains(".")){
            System.out.println("asdf");
        }


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

    public static String resolve(String baseUrl, String relUrl) {
        try {
            URL base;
            try {
                base = new URL(baseUrl);
            } catch (MalformedURLException var5) {
                URL abs = new URL(relUrl);
                return abs.toExternalForm();
            }

            return resolve(base, relUrl).toExternalForm();
        } catch (MalformedURLException var6) {
            return "";
        }
    }
    public static URL resolve(URL base, String relUrl) throws MalformedURLException {
        if(relUrl.startsWith("?")) {
            relUrl = base.getPath() + relUrl;
        }

        if(relUrl.indexOf(46) == 0 && base.getFile().indexOf(47) != 0) {
            base = new URL(base.getProtocol(), base.getHost(), base.getPort(), "/" + base.getFile());
        }

        System.out.println(base);

        return new URL(base, relUrl);
    }


    public static void split(String s){
        char[] chars = s.toCharArray();
        List<String> splitStr = new ArrayList<>();
        List<String> metaDataStr = new ArrayList<>();
        int begin = 0;
        for (int i = 0; i < chars.length; i++) {
            if (i == chars.length-1 && chars[i] != '}'){
                splitStr.add(s.substring(begin,s.length()));
            }
            if (chars[i] == '{'){
                splitStr.add(s.substring(begin,i));
                begin = i+1;
            }else if (chars[i] == '}'){
                metaDataStr.add(s.substring(begin,i));
                begin = i+1;
            }else {
                continue;
            }
        }
        for (String s1 : splitStr){
            System.out.println(s1);
        }
        System.out.println();
        for (String s1 : metaDataStr){
            System.out.println(s1);
        }

        ArrayList<String> splitRegList = new ArrayList<String>();//记录每个字段分割
        for (int i = 0; i < splitStr.size()-1; i++) {
            splitRegList.add(splitStr.get(i)+","+splitStr.get(i+1));
        }
        if (metaDataStr.size() > splitRegList.size()){
            splitRegList.add(splitStr.get(splitStr.size()-1)+",");
        }
        System.out.println();
        for (int i = 0; i < splitRegList.size(); i++) {
            System.out.println(splitRegList.get(i));
        }
        String str = splitRegList.get(0);
        String ss[] = str.split(",");
        if (" ".isEmpty()){
            System.out.println("fas");
        }
    }


}
