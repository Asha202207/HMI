package com.goldwind.javafxboot;

import com.goldwind.javafxboot.util.InitAppSetting;
import com.goldwind.javafxboot.util.MySplashScreen;
import com.goldwind.javafxboot.view.MainView;
import de.felixroske.jfxsupport.AbstractJavaFxApplicationSupport;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Collection;
import java.util.Collections;

@Slf4j
@SpringBootApplication
public class Application extends AbstractJavaFxApplicationSupport implements ApplicationRunner {

    public static void main(String[] args) {
        launch(Application.class, MainView.class, new MySplashScreen(), args);
    }

    /**
     * Spring 容器启动时执行一些初始化操作，如：加载自定义资源...
     * 此方法自行完之后，JavaFx应用程序启动画面才会关闭，原因分析：
     * 1 de.felixroske.jfxsupport.AbstractJavaFxApplicationSupport[row:120].init() 重写了 javafx.application.Application.init()
     * 2 先启动SpringBoot应用，当SpringBoot应用启动完毕时，执行了两个异步操作，第二个异步操作是关闭启动画面
     *
     * @param args
     * @throws Exception
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("app run");
        // 加载资源
        Thread.sleep(500);
    }

    @Override
    public void init() throws Exception {
        log.info("init");
        super.init();
    }

    @Override
    public void start(Stage stage) throws Exception {
        log.info("start");
        // 初始化配置信息
        if (Boolean.FALSE.equals(InitAppSetting.init())) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "初始化文档信息异常！");
            alert.showAndWait();
            return;
        }
        // 监听窗口面板宽度变化
        stage.widthProperty().addListener((observable, oldValue, newValue) -> {
            // 更新GUI组件
            Platform.runLater(() -> {
                Scene scene = stage.getScene();
                // 自适应按钮 对应 fx:id
                BorderPane borderPane = (BorderPane) scene.lookup("#borderPane");
                double width = stage.getWidth();
                borderPane.setPrefWidth(width * 0.98);
            });
        });
        // 监听窗口面板高度变化
        stage.heightProperty().addListener((observable, oldValue, newValue) -> {
            // 更新GUI组件
            Platform.runLater(() -> {
                Scene scene = stage.getScene();
                // 自适应按钮 对应 fx:id
                BorderPane borderPane = (BorderPane) scene.lookup("#borderPane");
                double height = stage.getHeight();
                borderPane.setPrefHeight(height * 0.93);
            });
        });
        super.start(stage);
    }

    @Override
    public void beforeShowingSplash(Stage splashStage) {
        log.info("before showing splash");
        super.beforeShowingSplash(splashStage);
    }

    @SneakyThrows
    @Override
    public void beforeInitialView(Stage stage, ConfigurableApplicationContext ctx) {
        log.info("before initial view");
        super.beforeInitialView(stage, ctx);
    }

    @Override
    public void stop() throws Exception {
        log.info("stop");
        super.stop();
    }

    // 虽然在application.yml中可以设置应用图标，但是首屏启动时的应用图标未改变，建议在此处覆盖默认图标
    @Override
    public Collection<Image> loadDefaultIcons() {
        return Collections.singletonList(new Image(getClass().getResource("/icon/icon.png").toExternalForm()));
    }
}
