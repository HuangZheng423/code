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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by huangzheng on 2017/1/5.
 */
public class Test {

    public static void main(String[] args) throws IOException {

        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");
        list.add("c");
        list.add("d");
        list.add("e");
        list.remove("a");
        System.out.println(list.get(0));


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
