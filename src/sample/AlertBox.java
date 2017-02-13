package sample;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Created by huangzheng on 2017/1/17.
 */
public class AlertBox extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        Button button = new Button();
        button.setText("Open a window");
        button.setOnAction(e -> new AlertBox1().display());

        AnchorPane layout = new AnchorPane();
        layout.getChildren().add(button);

        Scene scene=new Scene(layout,300,300);

        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
class AlertBox1 {

    public void display(){
        Stage window = new Stage();
        window.setTitle("请填写元数据字段信息");
        //modality要使用Modality.APPLICATION_MODEL
        window.initModality(Modality.APPLICATION_MODAL);
        window.setMinWidth(300);
        window.setMinHeight(150);

        ChoiceBox textType = new ChoiceBox(FXCollections.observableArrayList("a","b"));

        textType.setValue("cc");
        Button cancel = new Button("取消");
        cancel.setOnAction(e -> window.close());
        Button confirm = new Button("确定");
        confirm.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println(textType.getValue());
                window.close();

            }
        });

        Label labelName = new Label("字段名称");
        Label labelType = new Label("字段类型");
        TextField textName = new TextField();

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25,25,25,25));

        grid.add(labelName,0,0);
        grid.add(textName,1,0);
        grid.add(labelType,0,1);
        grid.add(textType,1,1);

        BorderPane bp = new BorderPane();
        HBox buttons = new HBox();
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setSpacing(10);
        buttons.getChildren().addAll(cancel, confirm);
        bp.setCenter(buttons);
        grid.add(bp,1,2);

        Scene scene = new Scene(grid,300,275);

        window.setScene(scene);
        //使用showAndWait()先处理这个窗口，而如果不处理，main中的那个窗口不能响应
        window.showAndWait();
    }
}
