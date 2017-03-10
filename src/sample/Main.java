package sample;

import com.mongodb.client.FindIterable;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseButton;
import javafx.scene.control.ContextMenu;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.StringConverter;
import netscape.javascript.JSObject;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.w3c.dom.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class Main extends Application {
    private TextField txtUrl;
    private WebView webView;
    private WebEngine webEngine;
    private Node currentSelNode ;
    private LinkedHashSet<Node> lstRectNode = new LinkedHashSet<>();
    private double dScrollBarHHeight = -1;
    private double dScrollBarVWidth = -1;
    private ContextMenu contextMenu;
    private ContextMenu contextMenu1;
    private Document document;
    private org.jsoup.nodes.Document jDocument;
    private int level = 0;//代表当前采集的层
    private ArrayList<LinkedHashSet<String>> urls = new ArrayList<>();//存放需要采集的url，以下标来代表层
    private ArrayList<LinkedHashSet<String>> urlsRegs = new ArrayList<>();//与urls相对应，存放每一层的采集规则
    HashMap<String,HashSet<String>> metaDataRegs = new HashMap<>(); //元数据采集规则,key:元数据字段名；value：元数据采集规则
    private static int collectCount = 0; //首次采集此值为0，采集完成之后值加1，之后采集都为异构网页采集，值大于0
    private List<String> urlNeedCheck;
    private StringBuffer downloadBuffer = new StringBuffer();
    private HashMap<String,String> attrMap = new HashMap<>();
    private static Logger logger = Logger.getLogger(Main.class);

//    private static int dbCollctionCount = 0; //数据库集合，递增
    private static MongoDBJDBC mongoDBJDBC  ;

    static Map<String, String> header = new HashMap<String, String>();

    public void initConnectionMap(){
        header.put("Host", "http://info.bet007.com");
        header.put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:5.0) Gecko/20100101 Firefox/5.0");
        header.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        header.put("Accept-Language", "zh-cn,zh;q=0.5");
        header.put("Accept-Charset", "GB2312,utf-8;q=0.7,*;q=0.7");
        header.put("Connection", "keep-alive");

    }

    final ChangeListener<Number> scrollChangeListener = new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            drawRectangle();
        }
    };

    Timeline animationRect = new Timeline(
            new KeyFrame(Duration.seconds(0.05),
                    new EventHandler<ActionEvent>() {
                        @Override
                        public void handle(ActionEvent event) {
                            drawRectangle();
                        }
                    })
    );
    @Override
    public void start(Stage primaryStage) throws Exception{
        initConnectionMap();
        GridPane root = new GridPane();
        root.setPadding(new Insets(0,0,0,0));
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(100);
        root.getColumnConstraints().add(column1);
        RowConstraints row1 = new RowConstraints();
        RowConstraints row2 = new RowConstraints();
        row2.setPercentHeight(95);
        root.getRowConstraints().addAll(row1,row2);
        webView = new WebView();
        VBox vBrowser = this.addCenterBrowder(webView);
        root.add(vBrowser,0,1);
        webEngine = webView.getEngine();
        webEngine.load("https://www.baidu.com");
        jDocument = Jsoup.connect("https://www.baidu.com").data(header).timeout(10*1000).get();

        webEngine.getLoadWorker().stateProperty().addListener(
                new ChangeListener<Worker.State>() {
                    @Override
                    public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
                        if (newValue == Worker.State.SUCCEEDED){
                            txtUrl.setText(webEngine.getLocation());
                            lstRectNode.clear();
                            drawRectangle();
                            setNodeEventListener();
                            ObservableList<javafx.scene.Node> list = webView.getChildrenUnmodifiable();
                            for (javafx.scene.Node n : list){
                                if (ScrollBar.class.isInstance(n)){
                                    ScrollBar scrollBar = (ScrollBar) n;
                                    if (scrollBar.getParent() == webView){
                                        scrollBar.valueProperty().addListener(scrollChangeListener);
                                        if (dScrollBarHHeight == -1 && scrollBar.getOrientation() == Orientation.HORIZONTAL){
                                            dScrollBarHHeight = scrollBar.getLayoutBounds().getHeight();
                                        }
                                        if (dScrollBarVWidth == -1 && scrollBar.getOrientation() == Orientation.VERTICAL){
                                            dScrollBarVWidth = scrollBar.getLayoutBounds().getWidth();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        );
        webView.setContextMenuEnabled(false);
        createContextMenu();
        webView.layoutBoundsProperty().addListener(new ChangeListener<Bounds>() {
            @Override
            public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
                animationRect.play();
            }
        });



        primaryStage.show();
        primaryStage.setMaximized(true);


    }



    public VBox addCenterBrowder(WebView paraWebView) {
        //中心浏览器布局
        VBox vBrowser = new VBox();
        vBrowser.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        //浏览器，头部地址
        HBox hOper = new HBox();
        hOper.setPadding(new Insets(15,12,15,12));
        hOper.setSpacing(10);
        Button btnBack = new Button("后退");
        btnBack.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if(webView.getEngine().getHistory().getCurrentIndex()>0)
                    webView.getEngine().getHistory().go(-1);
            }
        });
        //在窗口缩小时，不让按钮变成三个点
        btnBack.setMinWidth(Button.USE_PREF_SIZE);
        Label lblUrl = new Label("网址：");
        lblUrl.setMinWidth(Label.USE_PREF_SIZE);
        txtUrl = new TextField();
        txtUrl.setPrefWidth(700);
        Button btnGo = new Button("跳转");
        btnGo.setMinWidth(Button.USE_PREF_SIZE);
        EventHandler<ActionEvent> goAction = new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String strUrl = txtUrl.getText().trim();
                strUrl = strUrl.startsWith("http://") || strUrl.startsWith("https://") ? strUrl:"http://"+strUrl;
                strUrl = toUtf8String(strUrl);
                txtUrl.setText(strUrl);
                webEngine.load(strUrl);
                try {
                    jDocument = Jsoup.connect(strUrl).data(header).timeout(10*1000).get();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                level = 0;
                urls.clear();
                urlsRegs.clear();//与urls相对应，存放每一层的采集规则
                metaDataRegs.clear(); //元数据采集规则,key:元数据字段名；value：元数据采集规则
                collectCount = 0; //首次采集此值为0，采集完成之后值加1，之后采集都为异构网页采集，值大于0
                attrMap.clear();

                downloadBuffer = new StringBuffer();
            }
        };
        btnGo.setOnAction(goAction);
        txtUrl.setOnAction(goAction);
        Button showDatas = new Button("显示数据");
        showDatas.setMinWidth(Button.USE_PREF_SIZE);
        showDatas.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (mongoDBJDBC != null){
                    showList(mongoDBJDBC);
                }
            }
        });
        hOper.getChildren().addAll(btnBack,lblUrl,txtUrl,btnGo,showDatas);
        HBox.setHgrow(txtUrl,Priority.SOMETIMES);
        hOper.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(paraWebView,Priority.SOMETIMES);
        StackPane stack = new StackPane();
        stack.getChildren().addAll(paraWebView);
        StackPane.setAlignment(paraWebView, Pos.TOP_LEFT);
        vBrowser.getChildren().addAll(hOper,stack);
        VBox.setVgrow(stack,Priority.SOMETIMES);
        return vBrowser;
    }
    private void createContextMenu(){
        if (webView == null){
            return;
        }
         contextMenu = new ContextMenu();
        MenuItem selectElement = new MenuItem("选择元素-循环");
        selectElement.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                if (urls.size()==0 ){
                    LinkedHashSet<String> urlSet = new LinkedHashSet<String>();
                    LinkedHashSet<String> regSet = new LinkedHashSet<String>();
                    urls.add(urlSet);
                    urlsRegs.add(regSet);
                }
                if (currentSelNode != null){
                    //添加高亮节点
                    if (!lstRectNode.contains(currentSelNode)){
                        lstRectNode.add(currentSelNode);
                        drawRectangle();
                    }
                    String reg = getAttr(0);
                    System.out.println(reg);
                    logger.debug(reg);

                }
            }
        });
        contextMenu.getItems().add(selectElement);

        MenuItem selectSingleElement = new MenuItem("选择元素-单个");
        selectSingleElement.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (urls.size()==0 ){
                    LinkedHashSet<String> urlSet = new LinkedHashSet<String>();
                    LinkedHashSet<String> regSet = new LinkedHashSet<String>();
                    urls.add(urlSet);
                    urlsRegs.add(regSet);
                }
                if (currentSelNode != null){
                    //添加高亮节点
                    if (!lstRectNode.contains(currentSelNode)){
                        lstRectNode.add(currentSelNode);
                        drawRectangle();
                        Node node = currentSelNode;
                        while (!"A".equals(node.getNodeName())){
                            node = node.getParentNode();
                        }
                        String baseURL = node.getBaseURI();

                        NamedNodeMap nameNodeMap = node.getAttributes();
                        for (int i = 0; i < nameNodeMap.getLength(); i++) {
                            if (nameNodeMap.item(i).getNodeName().equals("href")){
                                String urlTemp = nameNodeMap.item(i).getNodeValue();
                                String url = resolve(baseURL,urlTemp);
                                urls.get(level).add(url);
                            }
                        }
                    }

                }
            }
        });
        contextMenu.getItems().add(selectSingleElement);
        MenuItem clickElement = new MenuItem("选择完毕-进入下一层");
        clickElement.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!urls.get(level).isEmpty()){
                    String nextLevelUrl = urls.get(level).iterator().next();
                    nextLevelUrl = toUtf8String(nextLevelUrl);
                    webEngine.load(nextLevelUrl);
                    try {
                        jDocument = Jsoup.connect(nextLevelUrl).data(header).timeout(10*1000).get();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    level++;
                    LinkedHashSet<String> urlSet = new LinkedHashSet<String>();
                    LinkedHashSet<String> regSet = new LinkedHashSet<String>();
                    urls.add(urlSet);
                    urlsRegs.add(regSet);
                }
            }
        });
        contextMenu.getItems().add(clickElement);

        Menu extractorSelectElement = new Menu("选择元素-采集");
        MenuItem singleElement = new MenuItem("单一型元数据");
        singleElement.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (currentSelNode != null){
                    //添加高亮节点
                    if (!lstRectNode.contains(currentSelNode)){
                        lstRectNode.add(currentSelNode);
                        drawRectangle();
                    }
                    String reg = getAttr(1);
                    wirteSingleMetadataInfo(reg);

                }
            }
        });
        MenuItem complexElement = new MenuItem("组合型元数据");
        complexElement.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (currentSelNode != null){
                    //添加高亮节点
                    if (!lstRectNode.contains(currentSelNode)){
                        lstRectNode.add(currentSelNode);
                        drawRectangle();
                    }
                    String reg = getAttr(1);
                    String text = currentSelNode.getTextContent();
                    writeComplexMetadataInfo(reg,text);

                }
            }
        });

        extractorSelectElement.getItems().addAll(singleElement,complexElement);
        contextMenu.getItems().add(extractorSelectElement);

        MenuItem extractElement = new MenuItem("选择完毕-采集");
        extractElement.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (collectCount == 0) {
                    String cName = getCName();
                    mongoDBJDBC = new MongoDBJDBC(cName);
                }

                LinkedHashSet<String> urlSet = urls.get(0);
                if (urlSet.size() == 0){
                    urlSet.add(webEngine.getLocation());
                }
                urlsRegs.remove(urlsRegs.size()-1);
                if (collectCount == 0){
                    Object[] urlList = urlSet.toArray();
                    extractMetaData(urlList,0);
                    collectCount ++;
                }else {
                    if (urlNeedCheck.size() > 0){
                        extractMetaData(urlNeedCheck.toArray(),1);
                    }
                }

            }
        });
        contextMenu.getItems().add(extractElement);

        MenuItem deletePage = new MenuItem("跳过当前页");
        deletePage.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (collectCount >0){
                    if (urlNeedCheck.size()>0){
                        urlNeedCheck.remove(0);
                        if (urlNeedCheck.size()>0) {
                            String urlT = urlNeedCheck.get(0);
                            urlT = toUtf8String(urlT);
                            webEngine.load(urlT);
                            try {
                                jDocument = Jsoup.connect(urlT).data(header).timeout(10*1000).get();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else {
                            alertNeedCheck();
                        }

                    }
                }

            }
        });
        contextMenu.getItems().add(deletePage);

        webView.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (contextMenu1 != null){
                    contextMenu1.hide();
                }
                if (event.getButton() == MouseButton.SECONDARY){
                    contextMenu.show(webView,event.getScreenX(), event.getScreenY());
                }else {
                    contextMenu.hide();
                }
            }
        });
    }
    private class MouseRightClick implements EventListener{

        @Override
        public void handleEvent(Event evt) {
            if (evt instanceof org.w3c.dom.events.MouseEvent){
                org.w3c.dom.events.MouseEvent mEvent = (org.w3c.dom.events.MouseEvent) evt;
                currentSelNode = (Node) mEvent.getTarget();
            }
        }
    }

    private void setNodeEventListener(){
        WebEngine webEngine = webView.getEngine();
        document = (Document) webEngine.executeScript("document");
        Queue<Integer> countQueue = new LinkedList<>();
        Queue<Node> nodeQueue = new LinkedList<>();
        Node eTemp = document.getDocumentElement();
        nodeQueue.add(eTemp);
        do {
            eTemp = nodeQueue.poll();
            ((EventTarget)eTemp).addEventListener("mousedown",new MouseRightClick(),false);
            if (eTemp instanceof Element){
                Node ndChild = eTemp.getFirstChild();

                do {
                    if (ndChild != null){
                        if (!(ndChild instanceof Element)){
                            if (ndChild.getNodeValue().trim().length() == 0){
                                continue;
                            }
                            nodeQueue.add(ndChild);
                        }
                    }
                } while (ndChild != null && (ndChild = ndChild.getNextSibling()) != null);
            }

        }while (!nodeQueue.isEmpty());
    }
    private void drawRectangle(){
        if (webView != null){
            StackPane stack = (StackPane) webView.getParent();
            // 清空所有非WebView的子对象
            ObservableList<javafx.scene.Node> obserList = stack.getChildren();
            for (int i = obserList.size() - 1; i >= 0; i--) {
                if (obserList.get(i) != webView) {
                    obserList.remove(i);
                }
            }
            for (Node nd : lstRectNode){
                double scrollBarHHeight = dScrollBarHHeight == -1 ? 14 : dScrollBarHHeight;
                double scrollBarVWidth = dScrollBarVWidth == -1 ? 14 :dScrollBarVWidth;
                JSObject jsNd = (JSObject) nd;
                JSObject bounds = (JSObject) jsNd.call("getBoundingClientRect");
                Double right = Double.parseDouble(bounds.getMember("right").toString());
                Double left = Double.parseDouble(bounds.getMember("left").toString());
                Double top = Double.parseDouble(bounds.getMember("top").toString());
                Double bottom = Double.parseDouble(bounds.getMember("bottom").toString());

                double dRight = stack.getWidth() - scrollBarVWidth;
                double dTop = 0;
                double dBottom = stack.getHeight() - scrollBarHHeight;
                double dLeft = 0;
                dRight = dRight > right ? right : dRight;
                dLeft = left > dLeft ? left : dLeft;
                dTop = top > dTop ? top : dTop;
                dBottom = dBottom > bottom ? bottom : dBottom;
                if (dRight > dLeft && dBottom > dTop){
                    Rectangle rect = new Rectangle(dRight - dLeft,dBottom - dTop, Color.YELLOW);
                    rect.setTranslateX(dLeft);
                    rect.setTranslateY(dTop);
                    rect.setOpacity(0.5);
                    rect.setStrokeWidth(1);
                    rect.setUserData(nd);
                    StackPane.setAlignment(rect,Pos.TOP_LEFT);
                    rect.setOnMouseClicked(new EventHandler<MouseEvent>() {
                        @Override
                        public void handle(MouseEvent event) {
                            if (event.getButton() == MouseButton.SECONDARY){
                                contextMenu1 = new ContextMenu();
                                MenuItem cancel = new MenuItem("取消选择");
                                Rectangle rectTemp = (Rectangle) event.getTarget();
                                cancel.setUserData(new Object[]{rectTemp.getUserData(),rectTemp});
                                cancel.setOnAction(new EventHandler<ActionEvent>() {
                                    @Override
                                    public void handle(ActionEvent event) {
                                        MenuItem item = (MenuItem) event.getTarget();
                                        Object[] arrObj = (Object[]) item.getUserData();
                                        lstRectNode.remove((Node) arrObj[0]);
                                        ((Rectangle)arrObj[1]).setVisible(false);
                                    }
                                });
                                contextMenu1.getItems().add(cancel);
                                contextMenu1.show(webView, event.getScreenX(), event.getScreenY());
                            }
                        }
                    });
                    stack.getChildren().add(rect);
                }


            }
        }
    }

    /**
     * 选择同类型元素，从最顶层开始
     * @param nodeList 与当前比较元属路径中当前层标签名相同的元素集合
     * @param mapList 当前元素所在路径的属性键值对数组
     * @param tagList 当前元素所在路径的标签值数组
     * @param k 当前比较的层
     * @param n 当有同类型当兄弟节点时，应选择节点应该在第n个
     */

    public void selectSameNode(NodeList nodeList,ArrayList<Map<String,String>> mapList,
                                ArrayList<String> tagList,int k,int n){



        for (int i = 0; i < nodeList.getLength(); i++) {

            Element e = (Element) nodeList.item(i);
            //先判读节点属性个数是否与原节点相同
            NamedNodeMap namedNodeMap = e.getAttributes();
            int count = 0;
            for (int j = 0; j < namedNodeMap.getLength(); j++) {
                Node node = namedNodeMap.item(j);
                String attrName = node.getNodeName();
                if (!"text".equals(attrName) && !"id".equals(attrName) &&
                        !"href".equals(attrName) && !"onclick".equals(attrName)){
                    count ++;
                }
            }
            int flag = 0;
            int c = mapList.get(k+1).size();
            if (count != c){
                flag = 1;
            }else {
                for (Map.Entry<String, String> entry : mapList.get(k + 1).entrySet()) {
                    String attrName = entry.getKey();
                    if (attrName.equals("id")) {
                        continue;
                    }
                    String attrValue = entry.getValue();
                    if (!attrValue.equals(e.getAttribute(attrName))) {
                        flag = 1;
                        break;
                    }
                }
            }
            if (flag == 0){
                if (n == -1) {
                    if (k >=0 ) {
                        NodeList listTemp = e.getElementsByTagName(tagList.get(k));
                        selectSameNode(listTemp, mapList, tagList, k - 1, n);
                    }else {
                        lstRectNode.add(nodeList.item(i));
                    }
                }else {
                    NodeList listTemp = e.getElementsByTagName(tagList.get(k));
                    if (tagList.get(k).equals("A")) {

                        if (listTemp.getLength() > 1) {
                            lstRectNode.add(listTemp.item(n));
                            continue;
                        }
                    } else {
                        selectSameNode(listTemp, mapList, tagList, k - 1, n);
                    }
                }
            }

        }

    }


    /**
     * 如果是循环，则将采集规则最后一个标签规定为a,不然采集不到url
     * @param regTemp 采集规则
     * @param value 链接值
     * @return
     */
    public String fixReg(String regTemp,String value){


        StringBuffer sb = new StringBuffer();
        String []splits = regTemp.split(">");
        int i =  splits.length-1;
        while (true){
            String attr = splits[i].substring(0,1);
            if ("A".equals(attr.toUpperCase())){
                break;
            }else {
                i--;
            }
        }
        for (int j = 0; j <= i; j++) {
            sb.append(splits[j]+">");
        }
        String returnStr = sb.toString().substring(0, sb.length() - 1);
        if (value != null && !value.isEmpty()) {
            String baseURL = value.split(";")[0];
            String urlT = value.split(";")[1];
            String url = resolve(baseURL, urlT);

            //查看当前链接是否有同类型的兄弟节点
            StringBuffer sb1 = new StringBuffer();
            String aReg = returnStr.substring(returnStr.lastIndexOf(">") + 1, returnStr.length());
            for (int j = 0; j < i; j++) {
                sb1.append(splits[j] + ">");
            }
            String strTemp = sb1.toString().substring(0, sb1.length() - 1) + ":has(a)";
            Elements elements = jDocument.select(strTemp);
            org.jsoup.nodes.Element element = elements.get(0);
            Elements elements1 = element.select(aReg);
            if (elements1.size() > 1) {
                int j = 0;
                for (org.jsoup.nodes.Element e : elements1) {
                    String u = e.absUrl("href");
                    if (u.equals(url)) {
                        break;
                    }
                    j++;

                }
                returnStr = strTemp + "::" + j;
            }
        }


        return returnStr;
    }

    //采集url
    public void extractURL(String reg){
        String regT = "";
        int n = 0;
        Elements elements = null;
        if (reg.contains("::")){
            regT = reg.split("::")[0];
            n = Integer.valueOf(reg.split("::")[1]);
            elements = jDocument.select(regT);
            for (org.jsoup.nodes.Element e : elements){
                String url = e.select("a").get(n).absUrl("href");
                url = toUtf8String(url);
                urls.get(level).add(url);
            }

        }else {
            elements = jDocument.select(reg);
            for (org.jsoup.nodes.Element e : elements) {
                String url = e.absUrl("href");
                url = toUtf8String(url);
                urls.get(level).add(url);
            }
        }
    }

    /**
     * 生成当前节点往上n层的xpath
     * @param flag flag=0选择元素-循环；flag=1选择元素-采集
     */
    public String getAttr(int flag){
        ArrayList<Map<String,String>> list = new ArrayList<Map<String, String>>();
        ArrayList<String> tagList = new ArrayList<String>();
        //生成xpath，便于jsoup抽取
        StringBuffer sb = new StringBuffer();
        Node tempNode = currentSelNode;
        String value = "";
        boolean attrAflag = true;
        Element e = (Element) tempNode;
        String tagName ="";
        while (!(tagName = e.getTagName()).equals("HTML")){
            tagList.add(tagName);
            NamedNodeMap nodeMap = tempNode.getAttributes();
            Map<String,String> currentNodeMap = new HashMap<String, String>();
            int length = nodeMap.getLength();
            for (int j = 0; j < nodeMap.getLength(); j++) {
                String attrName = nodeMap.item(j).getNodeName();
                String attrValue = nodeMap.item(j).getNodeValue();
                if (attrAflag && tagName.toUpperCase().equals("A") && attrName.equals("href")){
                    String baseURL = currentSelNode.getBaseURI();
                    value = baseURL + ";" + attrValue;
                    attrAflag = false;
                }
                if (!"text".equals(attrName) && !"id".equals(attrName) &&
                        !"href".equals(attrName) && !"onclick".equals(attrName)){
                    //规则排除id,href,onclick,text 四个属性 class属性保留作为条件
                    //元数据采集规则形成保留class属性和属性值，中间链接采集如果标签中属性大于1个则不要属性值

                    if ("class".equals(attrName) && flag ==0 && length > 1){
                        sb.insert(0,"["+attrName+"]");
                    }else {
                        sb.insert(0, "[" + attrName + "=" + attrValue + "]");
                    }
                    currentNodeMap.put(attrName,attrValue);
                }
            }
            sb.insert(0,tagName);
            sb.insert(0,">");
            list.add(currentNodeMap);
            tempNode = tempNode.getParentNode();
            e = (Element) tempNode;
        }

        String regTemp = sb.toString().substring(1,sb.length());
        String reg = regTemp;
        if (flag ==0 ){
            reg = fixReg(regTemp,value);
            int n = -1;
            if (reg.contains("::")){
                n = Integer.valueOf(reg.split("::")[1]);
            }


            String tag = tagList.get(tagList.size()-1);
            NodeList nodeListTemp = document.getElementsByTagName(tag);

            selectSameNode(nodeListTemp,list,tagList,tagList.size()-2,n);
            drawRectangle();
            if (urlsRegs.get(level).contains(reg)){
                System.out.println("todo");
            }else {
                extractURL(reg);
            }
            urlsRegs.get(level).add(reg);
        } else if (flag == 1){

        }
        return reg;
    }

    public void wirteSingleMetadataInfo(String regStr){

        Stage window = new Stage();
        window.setTitle("请填写元数据字段信息");
        //modality要使用Modality.APPLICATION_MODEL
        window.initModality(Modality.APPLICATION_MODAL);
        window.setMinWidth(300);
        window.setMinHeight(150);
        Label labelName = new Label("字段名称");
        Label labelType = new Label("字段类型");
        Label lableIsNeeded = new Label("是否必须");



        TextField textName = new TextField();
        ChoiceBox textType = new ChoiceBox(FXCollections.observableArrayList(
                 "文本", "链接")
        );
        ChoiceBox isNeeded = new ChoiceBox(FXCollections.observableArrayList(
                "是", "否")
        );

        ArrayList<String> nameList = new ArrayList<>();
        for (Map.Entry entry : attrMap.entrySet()){
            nameList.add(entry.getKey().toString());
        }

        ChoiceBox choiceName = new ChoiceBox(FXCollections.observableArrayList(nameList));
        TextField choiceType = new TextField();
        TextField choiceIsNeeded = new TextField();
        choiceType.setEditable(false);
        choiceIsNeeded.setEditable(false);

        choiceName.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String name = choiceName.getValue().toString();
                String s = attrMap.get(name);
                String type = s.split(";")[0];
                String isNeeded = s.split(";")[1];
                choiceType.setText(type);
                choiceIsNeeded.setText(isNeeded);
            }
        });


        Button cancel = new Button("取消");
        cancel.setMinWidth(Button.USE_PREF_SIZE);
        cancel.setOnAction(e -> window.close());
        Button confirm = new Button("确定");
        confirm.setMinWidth(Button.USE_PREF_SIZE);
        confirm.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String regTemp = regStr;
                String metaDataName = "";
                String metaDataType = "";
                String metaDataIsNeeded = "";
                if (collectCount == 0) {
                    metaDataName = textName.getText();
                    metaDataType = textType.getValue().toString();
                    metaDataIsNeeded = isNeeded.getValue().toString();
                    attrMap.put(metaDataName,metaDataType + ";" + metaDataIsNeeded);
                }else {
                    metaDataName = choiceName.getValue().toString();
                    metaDataType = choiceType.getText();
                    metaDataIsNeeded = choiceIsNeeded.getText();
                }
                if ("链接".equals(metaDataType)){
                    //加个标识，链接的采集方式与文本采集方式不同
                    regTemp = "URL"+fixReg(regStr,"");
                }
                if ("是".equals(metaDataIsNeeded)){
                    //加标识，如果必须，则作为结构检查的字段
                    metaDataName = metaDataName.concat("是");
                }else {
                    metaDataName = metaDataName.concat("否");
                }
                //元数据采集规则只取前3层
                String []splits = regTemp.split(">");
                StringBuffer sb = new StringBuffer();
                int i = splits.length > 4 ? splits.length-4 : 0;
                for ( ; i < splits.length; i++) {
                    sb.append(splits[i] + ">");
                }
                regTemp = sb.toString().substring(0,sb.length()-1);
                if ("链接".equals(metaDataType)){
                    //加个标识，链接的采集方式与文本采集方式不同
                    regTemp = "URL"+regTemp;
                }
                System.out.println(regTemp);



                if (!metaDataRegs.containsKey(metaDataName)){
                    HashSet<String> regSet = new HashSet<String>();
                    regSet.add(regTemp);
                    metaDataRegs.put(metaDataName,regSet);
                }else {
                    if (!metaDataRegs.get(metaDataName).contains(regTemp)){
                        metaDataRegs.get(metaDataName).add(regTemp);
                    }
                }
                window.close();
            }
        });
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25,25,25,25));

        grid.add(labelName,0,0);
        grid.add(labelType,0,1);
        grid.add(lableIsNeeded,0,2);
        if (collectCount == 0) {
            grid.add(textName, 1, 0);
            grid.add(textType, 1, 1);
            grid.add(isNeeded, 1, 2);
        }else {
            grid.add(choiceName, 1, 0);
            grid.add(choiceType, 1, 1);
            grid.add(choiceIsNeeded, 1, 2);
        }

        BorderPane bp = new BorderPane();
        HBox buttons = new HBox();
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setSpacing(10);
        buttons.getChildren().addAll(cancel, confirm);
        bp.setCenter(buttons);
        grid.add(bp,1,3);

        Scene scene = new Scene(grid,400,375);

        window.setScene(scene);
        //使用showAndWait()先处理这个窗口，而如果不处理，main中的那个窗口不能响应
        window.showAndWait();
    }

    public void alertNeedCheck(){
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setMinWidth(300);
        window.setMinHeight(150);
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25,25,25,25));


        BorderPane bp = new BorderPane();
        HBox buttons = new HBox();
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setSpacing(10);

        bp.setCenter(buttons);
        grid.add(bp,1,1);

        Label labelName = new Label();
        Button confirm = new Button("确定");
        confirm.setMinWidth(Button.USE_PREF_SIZE);
        if (urlNeedCheck.size()>0){
            labelName.setText("存在异构网页，请继续选择元数据。");

            buttons.getChildren().addAll(confirm);
            confirm.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent event) {
                    String url = urlNeedCheck.get(0);
                    url = toUtf8String(url);
                    webEngine.load(url);
                    try {
                        jDocument = Jsoup.connect(url).data(header).timeout(10*1000).get();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    window.close();
                }
            });
        }else {
            labelName.setText("采集完毕。");
            Button showDatas = new Button("显示数据");
            showDatas.setMinWidth(Button.USE_PREF_SIZE);
            showDatas.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
//                    dbCollctionCount++;
                    showList(mongoDBJDBC);
                    window.close();
                }
            });

            confirm.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    window.close();
                }
            });
            buttons.getChildren().addAll(showDatas);
        }



        grid.add(labelName,0,0);
        Scene scene = new Scene(grid,400,375);

        window.setScene(scene);
        //使用showAndWait()先处理这个窗口，而如果不处理，main中的那个窗口不能响应
        window.showAndWait();

    }

    /**
     * f为标识，f=0则初次采集，f=1则对异构采集
     * @param urlList
     * @param f
     */
    public void extractMetaData(Object[] urlList,int f){

        int length = urlList.length;
        List<ExtractThread> threadList = new ArrayList<ExtractThread>();
        urlNeedCheck = new ArrayList<String>();
        int flag = 0;
        int n = 0;
        while (flag + 50 < length){
            ArrayList<String> list = new ArrayList<String>();
            for (int i = flag; i < flag+50; i++) {
                list.add(urlList[i].toString());
            }
            flag += 50;

            long begin = Calendar.getInstance().getTimeInMillis();
            ExtractThread t = new ExtractThread(list, urlsRegs, metaDataRegs, mongoDBJDBC,f);
            threadList.add(t);
            t.start();
            n++;
            long end = Calendar.getInstance().getTimeInMillis();
            logger.debug((end - begin));
        }
        if (flag < length){
            ArrayList<String> list = new ArrayList<String>();
            for (int i = flag; i < length; i++) {
                list.add(urlList[i].toString());
            }
            ExtractThread t = new ExtractThread(list, urlsRegs, metaDataRegs, mongoDBJDBC,f);
            threadList.add(t);
            t.start();
        }

        System.out.println("threadList size: " + threadList.size());

        showProcessBar(threadList);


    }

    /**
     * 分割填写组合元数据字段信息，同时记录采集规则。
     * a{b}c{d}
     * 分割符{}之间的为单一元数据，该单一型元数据采集规则时regStr加上相邻的字符，
     * @param regStr  待采集节点定位规则
     * @param info  待采集节点文本信息
     */

    public void writeComplexMetadataInfo(String regStr,String info){

        Stage window = new Stage();
        window.setTitle("请填写元数据字段信息");
        //modality要使用Modality.APPLICATION_MODEL
        window.initModality(Modality.APPLICATION_MODAL);
        window.setMinWidth(300);
        window.setMinHeight(150);
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25,25,25,25));
        Label note = new Label("请使用{}标识出需要采集的元数据");
        TextField text = new TextField(info);
        grid.add(note,0,0);
        grid.add(text,0,1);
        Button finish = new Button("结束分割");
        finish.setMinWidth(Button.USE_PREF_SIZE);
        BorderPane b = new BorderPane();
        HBox button = new HBox();
        button.setAlignment(Pos.CENTER_RIGHT);
        button.setSpacing(10);
        button.getChildren().addAll(finish);
        b.setCenter(button);
        grid.add(b,1,1);
        GridPane gridInner = new GridPane();
        grid.add(gridInner,0,2);

        ArrayList<TextField> textNameList = new ArrayList<TextField>();
        ArrayList<ChoiceBox> isNeededList = new ArrayList<ChoiceBox>();
        ArrayList<TextField> choiceIsNeededList = new ArrayList<TextField>();
        ArrayList<ChoiceBox> choiceNameList = new ArrayList<ChoiceBox>();
        ArrayList<String> splitRegList = new ArrayList<String>();//记录每个字段分割
        finish.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String newText = text.getText();
                ArrayList<String> metaDataList = new ArrayList<String>();
                ArrayList<String> splitList = new ArrayList<String>();
                char []chars = newText.toCharArray();
                int begin = 0;
                for (int i = 0; i < chars.length; i++) {
                    if (i == chars.length-1 && chars[i] != '}'){
                        splitList.add(newText.substring(begin,newText.length()));
                    }
                    if (chars[i] == '{'){
                        splitList.add(newText.substring(begin,i));
                        begin = i+1;
                    }else if (chars[i] == '}'){
                        metaDataList.add(newText.substring(begin,i));
                        begin = i+1;
                    }else {
                        continue;
                    }
                }

                for (int i = 0; i < splitList.size()-1; i++) {
                    splitRegList.add(splitList.get(i).trim()+","+splitList.get(i+1).trim());
                }
                if (metaDataList.size() > splitRegList.size()){
                    splitRegList.add(splitList.get(splitList.size()-1).trim()+",");
                }



                Label labelValue = new Label("字段值");
                Label labelName = new Label("字段名称");
                Label lableIsNeeded = new Label("是否必须");
                gridInner.add(labelValue,0,0);
                gridInner.add(labelName,1,0);
                gridInner.add(lableIsNeeded,2,0);
                int i = 1;


                for (String metaData : metaDataList){

                    TextField textValue = new TextField(metaData);
                    TextField textName = new TextField();
                    ChoiceBox isNeeded = new ChoiceBox(FXCollections.observableArrayList(
                            "是", "否")
                    );




                    gridInner.add(textValue, 0, i);
                    if (collectCount == 0) {
                        textNameList.add(textName);
                        isNeededList.add(isNeeded);
                        gridInner.add(textName, 1, i);
                        gridInner.add(isNeeded, 2, i);
                    }else {

                        ArrayList<String> nameList = new ArrayList<>();
                        for (Map.Entry entry : attrMap.entrySet()){
                            nameList.add(entry.getKey().toString());
                        }
                        ChoiceBox choiceName = new ChoiceBox(FXCollections.observableArrayList(nameList));
                        TextField choiceIsNeeded = new TextField();
                        choiceIsNeeded.setEditable(false);
                        choiceNameList.add(choiceName);
                        choiceIsNeededList.add(choiceIsNeeded);
                        gridInner.add(choiceName, 1, i);
                        gridInner.add(choiceIsNeeded, 2, i);
                        choiceName.setOnAction(new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                String name = choiceName.getValue().toString();
                                String s = attrMap.get(name);
                                String isNeeded = s.split(";")[1];
                                choiceIsNeeded.setText(isNeeded);
                            }
                        });
                    }
                    i++;
                }


            }
        });

        Button cancel = new Button("取消");
        cancel.setMinWidth(Button.USE_PREF_SIZE);
        cancel.setOnAction(e -> window.close());
        Button confirm = new Button("确定");
        confirm.setMinWidth(Button.USE_PREF_SIZE);
        confirm.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                int length = collectCount == 0 ? textNameList.size() : choiceNameList.size();
                for (int i = 0; i < length; i++) {
                    String regTemp = regStr + ";" + splitRegList.get(i); //存放组合采集规则reg;bigin,end
                    System.out.println("complexSingleReg: " + regTemp);
                    String metaDataName = "";
                    String metaDataIsNeeded = "";
                    if (collectCount == 0){
                        TextField textName = textNameList.get(i);
                        ChoiceBox isNeeded = isNeededList.get(i);
                        metaDataName = textName.getText();
                        metaDataIsNeeded = isNeeded.getValue().toString();
                        attrMap.put(metaDataName,"文本;"+metaDataIsNeeded);
                    }else {
                        ChoiceBox choiceName = choiceNameList.get(i);
                        TextField choiceIsNeeded = choiceIsNeededList.get(i);
                        metaDataName = choiceName.getValue().toString();
                        metaDataIsNeeded = choiceIsNeeded.getText();
                    }


                    if ("是".equals(metaDataIsNeeded)) {
                        //加标识，如果必须，则作为结构检查的字段
                        metaDataName = metaDataName.concat("是");
                    } else {
                        metaDataName = metaDataName.concat("否");
                    }
                    if (!metaDataRegs.containsKey(metaDataName)) {
                        HashSet<String> regSet = new HashSet<String>();
                        regSet.add(regTemp);
                        metaDataRegs.put(metaDataName, regSet);
                    } else {
                        if (!metaDataRegs.get(metaDataName).contains(regTemp)) {
                            metaDataRegs.get(metaDataName).add(regTemp);
                        }
                    }
                }
                window.close();
            }
        });




        BorderPane bp = new BorderPane();
        HBox buttons = new HBox();
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setSpacing(10);
        buttons.getChildren().addAll(cancel, confirm);
        bp.setCenter(buttons);
        grid.add(bp,1,3);

        Scene scene = new Scene(grid,650,425);

        window.setScene(scene);
        //使用showAndWait()先处理这个窗口，而如果不处理，main中的那个窗口不能响应
        window.showAndWait();
    }




    public void showProcessBar(List<ExtractThread> threads)  {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setMinWidth(300);
        window.setMinHeight(150);
        Label labelName = new Label("采集中");
        ProgressBar progressBar = new ProgressBar();

        Task task = new Task() {
            @Override
            protected Object call() throws Exception {
                int max = threads.size();
                int count = threads.size();
                for (ExtractThread thread : threads){
                    while (thread.isAlive()) {
                        Thread.sleep(1000);
                    }
                    List<String> urlNeedCheckTemp = thread.urlNeedCheck;
                    if (urlNeedCheckTemp.size()>0){
                        urlNeedCheck.addAll(urlNeedCheckTemp);
                    }
                    count --;
                    updateProgress(max-count,max);
                    if (count < 0){
                        break;
                    }

                }
                System.out.println("urlNeedCheckTemp size: " + urlNeedCheck.size());

                return null;
            }
        };

        ProgressIndicator progressIndicator = new ProgressIndicator();
        progressBar.progressProperty().bind(task.progressProperty());
        progressIndicator.progressProperty().bind(task.progressProperty());
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();

        Button confirm = new Button("确定");
        confirm.setMinWidth(Button.USE_PREF_SIZE);
        confirm.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (task.isDone()){
                    //弹框显示存在异构网页
                    alertNeedCheck();
                    window.close();
                }
            }
        });

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25,25,25,25));

        grid.add(labelName,0,0);
        HBox hb = new HBox();
        hb.setSpacing(5);
        hb.setAlignment(Pos.CENTER);
        hb.getChildren().addAll(progressBar,progressIndicator);
        final VBox vb = new VBox();
        vb.setSpacing(5);
        vb.getChildren().addAll(hb);
        grid.add(vb,0,1);

        BorderPane bp = new BorderPane();
        HBox buttons = new HBox();
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setSpacing(10);
        buttons.getChildren().addAll(confirm);
        bp.setCenter(buttons);
        grid.add(bp,1,2);

        Scene scene = new Scene(grid,400,375);

        window.setScene(scene);
        //使用showAndWait()先处理这个窗口，而如果不处理，main中的那个窗口不能响应
        window.showAndWait();

    }

    public void showList(MongoDBJDBC mongoDBJDBC){

        Stage stage = new Stage();
        stage.setTitle("采集数据");
        stage.setHeight(500);

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25,25,25,25));

        FindIterable<org.bson.Document> documents = mongoDBJDBC.selectAll();
        Label label = new Label("总共采集数据： ");
        TextField textField = new TextField();
        long recordsCount = mongoDBJDBC.getRecordsCount();
        textField.setText(String.valueOf(recordsCount));
        label.setFont(new Font("Arial", 20));
        textField.setEditable(false);

        BorderPane borderPane = new BorderPane();
        HBox firstLine = new HBox();
        firstLine.setAlignment(Pos.TOP_LEFT);
        firstLine.setSpacing(10);
        firstLine.getChildren().addAll(label,textField);
        borderPane.setCenter(firstLine);

        grid.add(borderPane,0,0);


        org.bson.Document document = documents.first();
        TableView tableView = new TableView<>(generateDataInMap(documents));
        tableView.setEditable(false);
        tableView.getSelectionModel().setCellSelectionEnabled(true);

        Callback<TableColumn<Map, String>, TableCell<Map, String>>
                cellFactoryForMap = (TableColumn<Map, String> p) ->
                new TextFieldTableCell(new StringConverter() {
                    @Override
                    public String toString(Object t) {
                        return t.toString();
                    }
                    @Override
                    public Object fromString(String string) {
                        return string;
                    }
                });
        if (document.isEmpty()){
            return;
        }
        for (Map.Entry entry : document.entrySet()){
            String columnMapKey = entry.getKey().toString();
            if ("_id".equals(columnMapKey) || "_url".equals(columnMapKey)){
                continue;
            }
            TableColumn<Map,String> column = new TableColumn<Map,String>(columnMapKey);
            column.setCellValueFactory(new MapValueFactory<>(columnMapKey));
            column.setMinWidth(130);
            tableView.getColumns().add(column);
            column.setCellFactory(cellFactoryForMap);
        }


        grid.add(tableView,0,1);
        Button download = new Button("下载数据");
        download.setMinWidth(Button.USE_PREF_SIZE);
        download.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

                FileChooser fileChooser = new FileChooser();
                FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
                fileChooser.getExtensionFilters().add(extFilter);
                Stage s = new Stage();
                File file = fileChooser.showSaveDialog(s);
                if (file == null)
                    return;
                if(file.exists()){//文件已存在，则删除覆盖文件
                    file.delete();
                }
                String exportFilePath = file.getAbsolutePath();
                System.out.println("导出文件的路径" + exportFilePath);
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                    String info = downloadBuffer.toString();
                    bw.write(info);
                    bw.close();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("下载");
                    alert.setHeaderText("下载完成");
                    alert.setContentText("下载路径："+exportFilePath);
                    alert.showAndWait();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                stage.close();
            }
        });
        BorderPane bp = new BorderPane();
        HBox buttons = new HBox();
        buttons.setAlignment(Pos.TOP_RIGHT);
        buttons.setSpacing(10);
        buttons.getChildren().addAll(download);
        bp.setCenter(buttons);
        grid.add(bp,0,2);

        Scene scene = new Scene(grid);

        stage.setScene(scene);

        stage.show();
    }

    private ObservableList<Map> generateDataInMap(FindIterable<org.bson.Document> documents) {
        ObservableList<Map> allData = FXCollections.observableArrayList();
        boolean flag = true;
        for (org.bson.Document document : documents){
            Map<String, String> dataRow = new HashMap<>();
            if (flag){
                //添加表头
                for (Map.Entry entry : document.entrySet()){
                    if ("_id".equals(entry.getKey())){
                        continue;
                    }
                    downloadBuffer.append(entry.getKey()+"\t");
                }
                downloadBuffer.delete(downloadBuffer.length()-1,downloadBuffer.length());
                downloadBuffer.append("\n");
                flag = false;
            }
            for (Map.Entry entry : document.entrySet()){
                if ("_id".equals(entry.getKey())){
                    continue;
                }
                dataRow.put(entry.getKey().toString(),entry.getValue().toString());
                downloadBuffer.append(entry.getValue()+"\t");
            }
            downloadBuffer.delete(downloadBuffer.length()-1,downloadBuffer.length());
            downloadBuffer.append("\n");

            allData.addAll(dataRow);
        }
        return allData;

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

        return new URL(base, relUrl);
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

    public String getCName(){
        String baseStr = webEngine.getLocation();
        String h = "default";
        try {
            URL baseURL = new URL(baseStr);
            String host = baseURL.getHost();
            StringBuffer sb = new StringBuffer();
            if (host.contains(".")){
                String splits[] = host.split("\\.");
                for (String s : splits){
                    sb.append(s + "-");
                }
                if (sb.length()>0) {
                    h = sb.substring(0, sb.length() - 1);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return h;
    }


    public static void main(String[] args) {
        PropertyConfigurator.configure("log4j.properties");
        launch(args);
    }
}
