package sample;

import com.mongodb.client.FindIterable;
import com.sun.javafx.robot.impl.FXRobotHelper;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.MapValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.bson.Document;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by huangzheng on 2017/2/11.
 */
public class MongoTest extends Application{
    StringBuffer stringBuffer = new StringBuffer();
    public static void main(String[] args) {
        launch(args);

    }

    @Override
    public void start(Stage stage) throws Exception {
        MongoDBJDBC mongoDBJDBC = new MongoDBJDBC("");
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
                    fileWriter.write(stringBuffer.toString());
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

    private ObservableList<Map> generateDataInMap(FindIterable<Document> documents) {
        ObservableList<Map> allData = FXCollections.observableArrayList();
        for (Document document : documents){
            Map<String, String> dataRow = new HashMap<>();
            for (Map.Entry entry : document.entrySet()){
                stringBuffer.append(entry.getValue()+"\t");
                dataRow.put(entry.getKey().toString(),entry.getValue().toString());
            }
            stringBuffer.delete(stringBuffer.length()-1,stringBuffer.length());
            stringBuffer.append("\n");
            allData.addAll(dataRow);
        }
        return allData;

    }
}
