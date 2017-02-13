package sample;

import com.sun.org.apache.xml.internal.utils.DOMBuilder;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by huangzheng on 2016/12/27.
 */
public class W3CDocSelect {
    public static void main(String[] args) throws IOException {
        Document document = Jsoup.connect("http://jeit.ie.ac.cn/CN/article/showOldVolumn.do").get();
        System.out.println(document);
        W3CDom w3CDom = new W3CDom();
        org.w3c.dom.Document doc = w3CDom.fromJsoup(document);
        String reg = "TR>TD[width=90][height=50][valign=top][bgcolor=#CCCCCC][align=left][class=J_WenZhang]>A[class=J_WenZhang]";
        String reg1 = "TD[class=J_WenZhang][align=left][bgcolor=#CCCCCC][valign=top][height=50][width=90]>A[class=J_WenZhang]";
        Elements elements = document.select(reg1);
        System.out.println("ele"+elements.size());
        NodeList nodeList = doc.getElementsByTagName("tr");
        Set<String> set = new HashSet<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element e = (Element) nodeList.item(i);
            NodeList nodeList1 = e.getElementsByTagName("td");
            for (int j = 0; j < nodeList1.getLength(); j++) {
                Element e1 = (Element) nodeList1.item(j);
                String width = e1.getAttribute("width");
                String height = e1.getAttribute("height");
                String valign = e1.getAttribute("valign");
                String bgcolor = e1.getAttribute("bgcolor");
                String align = e1.getAttribute("align");
                String tClass = e1.getAttribute("class");
                if ("90".equals(width)
                        && "50".equals(height)
                        && "top".equals(valign)
                        && "#CCCCCC".equals(bgcolor)
                        && "left".equals(align)
                        && "J_WenZhang".equals(tClass)){
                    NodeList nodeList2 = e1.getElementsByTagName("a");
                    for (int k = 0; k < nodeList2.getLength(); k++) {
                        Element e3 = (Element) nodeList2.item(k);
                        String rClass = e3.getAttribute("class");
                        if ("J_WenZhang".equals(rClass)){

                            String uri = e3.getAttribute("href");
                            set.add(uri);
                        }
                    }
                }

            }

        }
        System.out.println(set.size());
    }
}
