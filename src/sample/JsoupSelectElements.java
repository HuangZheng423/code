package sample;

import com.sun.javaws.jnl.XMLUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by huangzheng on 2016/12/27.
 */
public class JsoupSelectElements {
    private org.w3c.dom.Document document ;
    private String reg;
    public JsoupSelectElements(org.w3c.dom.Document document,String reg){
        this.document = document;
        this.reg = reg;
    }
    private List<org.w3c.dom.Node> getNodes(){
        List<org.w3c.dom.Node> w3cNodes = new ArrayList<>();
        Document doc= changeDoc(document);
        Elements elements = doc.select(reg);
        for (Element e : elements){
            org.w3c.dom.Element element = (org.w3c.dom.Element) e;
            w3cNodes.add(element);
        }
        System.out.println(w3cNodes.size());

        return w3cNodes;
    }
    public static Document changeDoc(org.w3c.dom.Document document) {
//将W3C Document对象转成XML字符串
        DOMSource domSource = new DOMSource(document);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = null;
        try {
            transformer = tf.newTransformer();
            transformer.transform(domSource, result);
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        Document document1 = Jsoup.parse(writer.toString());
        return document1;
    }

    public static void main(String[] args) throws IOException{
        Document document = Jsoup.connect("http://jeit.ie.ac.cn/CN/article/showOldVolumn.do").get();
        W3CDom w3CDom = new W3CDom();
        org.w3c.dom.Document document1 = w3CDom.fromJsoup(document);
        Document document2 = changeDoc(document1);
        Elements elements = document2.select("a[class=J_WenZhang]");
        System.out.println(elements.size());
        String reg = "TR>TD[width=90][height=50][valign=top][bgcolor=#CCCCCC][align=left][class=J_WenZhang]>A[class=J_WenZhang]";
        JsoupSelectElements jsoupSelectElements = new JsoupSelectElements(document1,reg);
        jsoupSelectElements.getNodes();

    }

}
