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
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.PropertyValueFactory;
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
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.select.Elements;
import org.w3c.dom.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;
import org.w3c.dom.events.EventTarget;


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
    private MongoDBJDBC mongoDBJDBC = new MongoDBJDBC();
    private static int collectCount = 0; //首次采集此值为0，采集完成之后值加1，之后采集都为异构网页采集，值大于0
    private List<String> urlNeedCheck;
    private StringBuffer downloadBuffer;

    private static Logger logger = Logger.getLogger(Main.class);



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
        webEngine.load("http://jeit.ie.ac.cn/CN/article/showOldVolumn.do");
        jDocument = Jsoup.connect("http://jeit.ie.ac.cn/CN/article/showOldVolumn.do").get();
        webEngine.getLoadWorker().stateProperty().addListener(
                new ChangeListener<Worker.State>() {
                    @Override
                    public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
                        if (newValue == Worker.State.SUCCEEDED){
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
                txtUrl.setText(strUrl);
                webEngine.load(strUrl);
                try {
                    jDocument = Jsoup.connect(strUrl).get();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        btnGo.setOnAction(goAction);
        txtUrl.setOnAction(goAction);
        hOper.getChildren().addAll(btnBack,lblUrl,txtUrl,btnGo);
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
                    String reg = getAttr(3,0);
                    System.out.println(reg);

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
                        System.out.println("baseurl: " + currentSelNode.getBaseURI());
                        System.out.println("namespaceurl: " + currentSelNode.getNamespaceURI());
                        NamedNodeMap nameNodeMap = currentSelNode.getAttributes();
                        for (int i = 0; i < nameNodeMap.getLength(); i++) {
                            if (nameNodeMap.item(i).getNodeName().equals("href")){
                                System.out.println(nameNodeMap.item(i).getNodeValue());
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
                System.out.println("level" + level);
                System.out.println("level size" + urls.get(level).size());
                System.out.println("level url0" + urls.get(level).iterator().next());
                if (!urls.get(level).isEmpty()){
                    String nextLevelUrl = urls.get(level).iterator().next();
                    webEngine.load(nextLevelUrl);
                    try {
                        jDocument = Jsoup.connect(nextLevelUrl).get();
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
                    String reg = getAttr(3,1);
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
                    String reg = getAttr(3,1);
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
                        String url = webEngine.getLocation();
                        urlNeedCheck.remove(url);
                        if (urlNeedCheck.size()>0) {
                            webEngine.load(urlNeedCheck.get(0));
                            try {
                                jDocument = Jsoup.connect(urlNeedCheck.get(0)).get();
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

    public void selectSameNode(NodeList nodeList,ArrayList<Map<String,String>> mapList,
                                ArrayList<String> tagList,int k){

        for (int i = 0; i < nodeList.getLength(); i++) {
            Element e = (Element) nodeList.item(i);
            int flag = 0;
            for (Map.Entry<String,String> entry : mapList.get(k+1).entrySet()){
                String attrName = entry.getKey();
                String attrValue = entry.getValue();
                if (!attrValue.equals(e.getAttribute(attrName))){
                    flag = 1;
                    break;
                }
            }
            if (flag == 0){
                if (k >=0 ) {
                    NodeList listTemp = e.getElementsByTagName(tagList.get(k));
                    selectSameNode(listTemp, mapList, tagList, k - 1);
                }else {
                    lstRectNode.add(nodeList.item(i));
                }
            }

        }

    }

    //如果是循环，则将采集规则最后一个标签规定为a,不然采集不到url
    public String fixReg(String regTemp){
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
        return sb.toString().substring(0,sb.length()-1);
    }

    //采集url
    public void extractURL(String reg){
        Elements elements = jDocument.select(reg);
        for (org.jsoup.nodes.Element e : elements){
            String url = e.absUrl("href");
            System.out.println(url);
            urls.get(level).add(url);
        }
    }

    /**
     * 生成当前节点往上n层的xpath
     * @param n 往上层数
     * @param flag flag=0选择元素-循环；flag=1选择元素-采集
     */
    public String getAttr(int n,int flag){
        ArrayList<Map<String,String>> list = new ArrayList<Map<String, String>>();
        ArrayList<String> tagList = new ArrayList<String>();
        //生成xpath，便于jsoup抽取
        StringBuffer sb = new StringBuffer();
        Node tempNode = currentSelNode;
        for (int i = 0; i < n; i++) {
            Element e = (Element) tempNode;
            String tagName = e.getTagName();
            tagList.add(tagName);
            NamedNodeMap nodeMap = tempNode.getAttributes();
            Map<String,String> currentNodeMap = new HashMap<String, String>();
            for (int j = 0; j < nodeMap.getLength(); j++) {
                String attrName = nodeMap.item(j).getNodeName();
                String attrValue = nodeMap.item(j).getNodeValue();
                if (!"href".equals(attrName) && !"onclick".equals(attrName)){
                    //规则排除href onclick class 三个属性
                    if ("class".equals(attrName)){
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
        }

        String regTemp = sb.toString().substring(1,sb.length());
        String reg = regTemp;
        if (flag ==0 ){
            reg = fixReg(regTemp);
            String tag = tagList.get(tagList.size()-1);
            NodeList nodeListTemp = document.getElementsByTagName(tag);
            selectSameNode(nodeListTemp,list,tagList,tagList.size()-2);
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

        textType.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {

            }
        });

        Button cancel = new Button("取消");
        cancel.setOnAction(e -> window.close());
        Button confirm = new Button("确定");
        confirm.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String regTemp = regStr;
                String metaDataName = textName.getText();
                if ("链接".equals(textType.getValue())){
                    //加个标识，链接的采集方式与文本采集方式不同
                    regTemp = "URL"+fixReg(regStr);
                }
                if ("是".equals(isNeeded.getValue())){
                    //加标识，如果必须，则作为结构检查的字段
                    metaDataName = metaDataName.concat("是");
                }else {
                    metaDataName = metaDataName.concat("否");
                }
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
        grid.add(textName,1,0);
        grid.add(labelType,0,1);
        grid.add(textType,1,1);
        grid.add(lableIsNeeded,0,2);
        grid.add(isNeeded,1,2);

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
        if (urlNeedCheck.size()>0){
            labelName.setText("存在异构网页，请继续选择元数据。");
            String url = urlNeedCheck.get(0);
            buttons.getChildren().addAll(confirm);
            confirm.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent event) {
                    webEngine.load(url);
                    try {
                        jDocument = Jsoup.connect(url).get();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    window.close();
                }
            });
        }else {
            labelName.setText("采集完毕。");
            Button showDatas = new Button("显示数据");
            showDatas.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    showList();
                    window.close();
                }
            });

            confirm.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    window.close();
                }
            });
            buttons.getChildren().addAll(showDatas,confirm);
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
        List<ExtractURLThread> threadList = new ArrayList<ExtractURLThread>();
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
            ExtractURLThread t = new ExtractURLThread(list, urlsRegs, metaDataRegs, mongoDBJDBC,f);
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
            ExtractURLThread t = new ExtractURLThread(list, urlsRegs, metaDataRegs, mongoDBJDBC,f);
            threadList.add(t);
            t.start();
        }

        System.out.println("threadList size: " + threadList.size());

        showProcessBar(threadList);


    }


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
                    textNameList.add(textName);
                    isNeededList.add(isNeeded);
                    gridInner.add(textValue,0,i);
                    gridInner.add(textName,1,i);
                    gridInner.add(isNeeded,2,i);
                    i++;
                }


            }
        });

        Button cancel = new Button("取消");
        cancel.setOnAction(e -> window.close());
        Button confirm = new Button("确定");
        confirm.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                for (int i = 0; i < textNameList.size(); i++) {
                    String regTemp = regStr + ";" + splitRegList.get(i); //存放组合采集规则reg;bigin,end
                    TextField textName = textNameList.get(i);
                    ChoiceBox isNeeded = isNeededList.get(i);
                    String metaDataName = textName.getText();

                    if ("是".equals(isNeeded.getValue())) {
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




    public void showProcessBar(List<ExtractURLThread> threads)  {
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
                for (ExtractURLThread thread : threads){
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
        confirm.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (task.isDone()){
                    if (!urlNeedCheck.isEmpty()){
                        //弹框显示存在异构网页
                        alertNeedCheck();
                    }
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

    public void showList(){

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
        for (Map.Entry entry : document.entrySet()){
            String columnMapKey = entry.getKey().toString();
            if ("_id".equals(columnMapKey)){
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
        download.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                downloadBuffer = new StringBuffer();
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
                FileWriter fileWriter = null;
                try {
                    fileWriter = new FileWriter(file);
                    fileWriter.write(downloadBuffer.toString());
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

    public static void main(String[] args) {
        PropertyConfigurator.configure("log4j.properties");
        launch(args);
    }
}
