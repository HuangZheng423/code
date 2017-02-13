package sample;

import javafx.application.Application;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static javafx.application.Application.launch;

/**
 * Created by huangzheng on 2017/2/9.
 */
public class ProgressSample extends Application {
    private static ProgressBar progressBar ;
    private static ProgressIndicator progressIndicator ;


        @Override
        public void start(Stage stage) throws InterruptedException {
            Stage window = new Stage();
            window.initModality(Modality.APPLICATION_MODAL);
            window.setMinWidth(300);
            window.setMinHeight(150);
            Label labelName = new Label("下载进度");
            Button confirm = new Button("确定");
            progressBar = new ProgressBar();
            progressIndicator = new ProgressIndicator();
            ArrayList<ThreadTest> threads = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                ThreadTest threadTest = new ThreadTest(i,(i+1)*50);
                threads.add(threadTest);
                threadTest.start();

            }
            Task task = createTask(threads);
            confirm.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    if (task.isDone()){
                        window.close();
                        alertNeedCheck();

                    }
                }
            });

            progressBar.progressProperty().bind(task.progressProperty());
            progressIndicator.progressProperty().bind(task.progressProperty());

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();


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
        public Task createTask(ArrayList<ThreadTest> threads){
            Task task = new Task() {
                @Override
                protected Object call() throws Exception {

                    int max = threads.size();
                    int count = threads.size();
                    for (ThreadTest thread : threads){
                        while (thread.isAlive()) {
                            Thread.sleep(1000);
                        }
                        System.out.println("thread count: " + thread.count);
                        count --;
                        System.out.println(count);
                        updateProgress(max-count,max);
                        if (count < 0){
                            break;
                        }

                    }
                    return null;
                }
            };
            return task;
        }

    public void alertNeedCheck(){
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setMinWidth(300);
        window.setMinHeight(150);
        Label labelName = new Label("你好");
        Button confirm = new Button("确定");



        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25,25,25,25));

        grid.add(labelName,0,0);
        BorderPane bp = new BorderPane();
        HBox buttons = new HBox();
        buttons.setAlignment(Pos.CENTER_RIGHT);
        buttons.setSpacing(10);
        buttons.getChildren().addAll(confirm);
        bp.setCenter(buttons);
        grid.add(bp,1,1);

        Scene scene = new Scene(grid,400,375);

        window.setScene(scene);
        //使用showAndWait()先处理这个窗口，而如果不处理，main中的那个窗口不能响应
        window.showAndWait();

    }

        public static void main(String[] args) throws InterruptedException {
            launch(args);
        }

}
