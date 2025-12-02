package com.goldwind.javafxboot.ctrl;

import cn.hutool.core.date.DateUtil;
import com.goldwind.javafxboot.model.SettingEnum;
import com.goldwind.javafxboot.model.SettingGroup;
import com.goldwind.javafxboot.model.SettingLocation;
import com.goldwind.javafxboot.model.SettingShow;
import com.goldwind.javafxboot.protocol.modbus.domain.datatype.numeric.P_AB;
import com.goldwind.javafxboot.protocol.modbus.exceptiom.ModbusException;
import com.goldwind.javafxboot.service.ThreadScheduledService;
import com.goldwind.javafxboot.util.InitAppSetting;
import de.felixroske.jfxsupport.FXMLController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 主界面控制器
 *
 * @author xujianhua
 * @date 2024/4/23 2:01
 */
@Slf4j
@FXMLController
public class MainCtrl implements Initializable {
    // 主容器
    public Pane rootPane;
    public Pane paneAction;
    // 控件
    public Label labTime;
    public TextArea txtAreaData;
    public TextField txtActionAddress;
    public TextField txtActionValue;
    public TabPane tabPane;
    public Button btnConnect;
//    public Button btnAction;


    //------------wyc添加   start --------------//
    public Button btn_start;
    public Button btn_stop;
    public Button btn_reset;
    public Button btn_maintain;
    public Button btn_examination;
    public Button sendNumber;
    //------------wyc添加   end --------------//

    private ResourceBundle messages;
    private ThreadScheduledService threadScheduledService;
    private String currentTab = "";
    private final Map<String, List<SettingLocation>> tabLocation = new HashMap<>();
    DateFormat currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @SneakyThrows
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化语言及国际化
        initDefaultSetting();
        // 初始化通用控件信息
        initCommonControl();
        // 初始化TabPane
        initTabPaneControl();
        // 初始化时间计时控件
        initTimeControl();
    }

    public void onBtnConnectClick(ActionEvent actionEvent) {
        try {
            if (threadScheduledService != null) {
                if (threadScheduledService.isRunning()) {
                    threadScheduledService.cancel();
                    btnConnect.setStyle("-fx-text-fill: green;");
                    btnConnect.setText("▶");
                    paneAction.setDisable(true);
                } else {
                    threadScheduledService.restart();
                    btnConnect.setStyle("-fx-text-fill: red;");
                    btnConnect.setText("●");
                    paneAction.setDisable(false);
                }
            } else {
                btnConnect.setStyle("-fx-text-fill: red;");
                btnConnect.setText("●");
                paneAction.setDisable(false);
                threadScheduledService = new ThreadScheduledService("modbus-tcp service");
                //上次运行开始和下一次运行开始之间允许的最短时间。
                threadScheduledService.setPeriod(Duration.millis(InitAppSetting.getSetting().getSettingServer().getCycle() * 1000.0));
                //ScheduledService 首次启动与开始运行之间的初始延迟。
                threadScheduledService.setDelay(Duration.millis(500));
                //设置 ScheduledService 是否应在 Task 失败的情况下自动重新启动。
                threadScheduledService.setRestartOnFailure(false);

                //给ScheduledService添加Task执行成功事件
                threadScheduledService.setOnSucceeded(t -> {
                    //获取Task返回的值
                    if (t.getSource().getValue() != null && !((Map<Integer, Object>) t.getSource().getValue()).isEmpty()) {
                        txtAreaData.appendText("[" + DateUtil.now() + "] " + t.getSource().getValue() + "\n");
                        loadModbusData((Map<Integer, Object>) t.getSource().getValue());
                    }
                });
                threadScheduledService.start();
            }
        } catch (Exception e) {
            log.error("error: ", e);
        }
    }

    //------------wyc添加   start --------------//
    //启动按钮按下
    public void btn_startClick (ActionEvent event) throws ModbusException {
        if (threadScheduledService != null && threadScheduledService.isRunning()) {
            threadScheduledService.postAction(
                    Integer.parseInt("102")
                    , new P_AB().setValue(BigDecimal.valueOf(Integer.parseInt("1"))));
        }
    }

    //停止按钮按下
    public void btn_stopClick (ActionEvent event) throws ModbusException {
        if (threadScheduledService != null && threadScheduledService.isRunning()) {
            threadScheduledService.postAction(
                    Integer.parseInt("104")
                    , new P_AB().setValue(BigDecimal.valueOf(Integer.parseInt("1"))));
        }
    }

    //复位按钮按下
    public void btn_resetClick (ActionEvent event) throws ModbusException {
        if (threadScheduledService != null && threadScheduledService.isRunning()) {
            threadScheduledService.postAction(
                    Integer.parseInt("103")
                    , new P_AB().setValue(BigDecimal.valueOf(Integer.parseInt("1"))));
        }
    }

    //维护按钮按下
    public void btn_maintainClick (ActionEvent event) throws ModbusException {
        if (threadScheduledService != null && threadScheduledService.isRunning()) {
            threadScheduledService.postAction(
                    Integer.parseInt("105")
                    , new P_AB().setValue(BigDecimal.valueOf(Integer.parseInt("1"))));
        }
    }

    //定检按钮按下
    public void btn_examinationClick (ActionEvent event) throws ModbusException {
        if (threadScheduledService != null && threadScheduledService.isRunning()) {
            threadScheduledService.postAction(
                    Integer.parseInt("106")
                    , new P_AB().setValue(BigDecimal.valueOf(Integer.parseInt("1"))));
        }
    }

    //Modbus发送函数
    public void mb_SendData(int addr, int senddata) {

        if (threadScheduledService != null && threadScheduledService.isRunning()) {
            try {
                threadScheduledService.postAction(
                        addr
                        , new P_AB().setValue(BigDecimal.valueOf(senddata)));
            } catch (ModbusException e) {
                // 处理异常，打印错误信息
                txtAreaData.appendText("[" + DateUtil.now() + "] " + "Modbus响应异常！(Modbus response exception!)" + "\n");
                e.printStackTrace();
            }

        }else {
            txtAreaData.appendText("[" + DateUtil.now() + "] " + "未建立连接！(No connection established!)" + "\n");
        }
    }


    //------------wyc添加   stop --------------//



    public void onBtnActionClick() throws ModbusException {
        if (threadScheduledService != null && threadScheduledService.isRunning()) {
            threadScheduledService.postAction(
                    Integer.parseInt(txtActionAddress.textProperty().getValue())
                    , new P_AB().setValue(BigDecimal.valueOf(Integer.parseInt(txtActionValue.textProperty().getValue()))));
        }
    }

    private void initDefaultSetting() {
        Locale local;
        if (InitAppSetting.getSetting().getSettingEnvironment().getLanguage().equals("cn")) {
            local = Locale.CHINA;
        } else {
            local = Locale.ENGLISH;
        }

        messages = ResourceBundle.getBundle("language/messages", local);
    }

    private void initCommonControl() {
        // 控件只可输入数字
//        texFieldNumberMatches(txtActionAddress);
//        texFieldNumberMatches(txtActionValue);
        btnConnect.setStyle("-fx-text-fill: green;");
        // 控件文本国际化
//        btnAction.setText(messages.getString("main.action"));
    }





    private void initTabPaneControl() {
        for (SettingShow settingShow : InitAppSetting.getSetting().getSettingShowList()) {
            List<SettingLocation> currentShowLocation = new ArrayList<>();
            for (SettingGroup settingGroup : InitAppSetting.getSetting().getSettingGroupList()) {
                currentShowLocation.addAll(settingGroup.getSettingLocationList()
                        .stream()
                        .filter(k -> k.getShow().equals(settingShow.getIndex()))
                        .collect(Collectors.toList()));
            }
            Tab showTab = new Tab(messages.getString(settingShow.getIec()));
            if (currentTab.equals("")) {
                currentTab = "tab_" + settingShow.getIndex();
            }
            showTab.setId("tab_" + settingShow.getIndex());

            GridPane gridPane = new GridPane();
            // 设置列间隔
            gridPane.setHgap(5);
            // 设置行间隔
            gridPane.setVgap(5);

            // 创建一个ScrollPane并设置其属性
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setContent(gridPane);
            scrollPane.setFitToHeight(true);
            scrollPane.setFitToWidth(true);


            int row = 1;
            int column = 1;
            int maxColumn = settingShow.getColumn();

            for (SettingLocation settingLocation : currentShowLocation) {

                //------------zh添加   start --------------//
                if(settingLocation.getFnc_lab() == 1) {

                    String lbNameText = messages.getString(settingLocation.getIec());
                    Label lbName = new Label(lbNameText + ": ");

                    // 创建一个Tooltip，并设置其文本
                    Tooltip tooltip = new Tooltip(lbNameText);
                    lbName.setTooltip(tooltip);
                    gridPane.add(lbName, column, row);
                    column++;

                    //按钮
                    Button btnValue = new Button(" -- ");
                    btnValue.setId("btnValue_" + settingLocation.getAddress());
                    btnValue.setDisable(false);
                    gridPane.add(btnValue, column, row);
                    column++;

                    //单位
                    Label lbUnit = new Label(settingLocation.getUnit());
                    gridPane.add(lbUnit, column, row);
                    column++;

                    Label lbSplit = new Label("|");
                    gridPane.add(lbSplit, column, row);
                //------------wyc添加   start --------------//
                } else if(settingLocation.getFnc_lab() == 2){

                    //获取Iec文本
                    String lbNameText = messages.getString(settingLocation.getIec());

                    //按钮
                    Button btnValue = new Button(lbNameText);
                    btnValue.setId("btnvalue_fnc2" + settingLocation.getAddress());
                    // 添加按钮的点击事件处理器
                    btnValue.setOnAction((event) -> {
                        if (threadScheduledService != null && threadScheduledService.isRunning()){
                            mb_SendData(settingLocation.getAddress(),1);
                        }
                        else {
                            txtAreaData.appendText("[" + DateUtil.now() + "] " + "未建立连接！(No connection established!)" + "\n");
                        }
                    });

                    gridPane.add(btnValue, column, row);
                    column++;

                    column++;

                    column++;
                    Label lbSplit = new Label("|");
                    gridPane.add(lbSplit, column, row);

                } else if (settingLocation.getFnc_lab() == 3) {
                    //获取Iec文本
                    String lbNameText = messages.getString(settingLocation.getIec());

                    //label 提示开关按钮是否按下
                    Label lbBtState = new Label(" --- ");
                    Tooltip tooltip = new Tooltip("按钮状态，“---”为未按下，“-√-”为按下");
                    lbBtState.setTooltip(tooltip);
                    //开关按钮
                    ToggleButton toggleButton = new ToggleButton(lbNameText);
                    toggleButton.setOnAction((event) -> {
                        //判断modbusTCP是否建立连接，未连接则禁止状态开关按钮按下
                        if (threadScheduledService != null && threadScheduledService.isRunning()){
                            if (toggleButton.isSelected()) {
                                lbBtState.setText(" -√- ");
                                mb_SendData(settingLocation.getAddress(),1);
                            } else {
                                mb_SendData(settingLocation.getAddress(),0);
                                lbBtState.setText(" --- ");
                            }
                        }else {
                            toggleButton.setSelected(false);
                            txtAreaData.appendText("[" + DateUtil.now() + "] " + "未建立连接！(No connection established!)" + "\n");
                        }
                    });
                    gridPane.add(toggleButton, column, row);
                    column++;
                    gridPane.add(lbBtState, column, row);
                    column++;

                    column++;
                    Label lbSplit = new Label("|");
                    gridPane.add(lbSplit, column, row);

                } else if(settingLocation.getFnc_lab() == 4){
                    //获取Iec文本
                    String lbNameText = messages.getString(settingLocation.getIec());

                    //文本输入框
                    TextField TFInput = new TextField("");

                    //按钮
                    Button btnValue = new Button(lbNameText);
                    //btnValue.setId("btnvalue_fnc4" + settingLocation.getAddress());
                    // 添加按钮的点击事件处理器
                    btnValue.setOnAction((event) -> {
                        if (threadScheduledService != null && threadScheduledService.isRunning()){
                            String tfValueStr = TFInput.getText();
                            try {
                                int tfValueInt = Integer.parseInt(tfValueStr);
                                mb_SendData(settingLocation.getAddress(),tfValueInt);
                            } catch (NumberFormatException e) {
                                txtAreaData.appendText("[" + DateUtil.now() + "] " + "输入值无效！(The input value is invalid!)" + "\n");
                            }
                        }
                        else {
                            txtAreaData.appendText("[" + DateUtil.now() + "] " + "未建立连接！(No connection established!)" + "\n");
                        }
                    });


                    //单位
                    Label lbUnit = new Label(settingLocation.getUnit());

                    gridPane.add(btnValue, column, row);
                    column++;
                    gridPane.add(TFInput, column, row);
                    column++;
                    gridPane.add(lbUnit, column, row);
                    column++;
                    Label lbSplit = new Label("|");
                    gridPane.add(lbSplit, column, row);

                }
                //------------wyc添加   stop --------------//

                //------------zh添加   stop --------------//

                if (column < maxColumn * 4) {
                    column++;
                } else {
                    row++;
                    column = 1;
                }
            }
            showTab.setContent(scrollPane);
            tabPane.getTabs().add(showTab);
            tabLocation.put("tab_" + settingShow.getIndex(), currentShowLocation);
        }
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab != null) {
                // 当用户切换Tab时，打印当前选中的Tab的文本
                currentTab = newTab.getId();
            }
        });
    }

    private void initTimeControl() {
        EventHandler<ActionEvent> eventHandler = e -> {
            labTime.setText(currentTime.format(new Date()));
        };
        //一秒刷新一次
        Timeline animation = new Timeline(new KeyFrame(Duration.millis(1000), eventHandler));
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
    }

    private void loadModbusData(Map<Integer, Object> readMapData) {
        List<SettingLocation> currentShowLocationList = tabLocation.get(currentTab);
        for (SettingLocation settingLocation : currentShowLocationList) {
            if(settingLocation.getFnc_lab() == 1) {
                int data = Integer.parseInt(readMapData.get(settingLocation.getAddress()).toString());
                String formattedData = Integer.toString(data);
                int boolTypeFlag = 0;
                switch (settingLocation.getType()) {
                    case 1:
                        // int
                        if (settingLocation.getMultiple() != null) {
                            formattedData = String.valueOf((int) Math.ceil((double) data / settingLocation.getMultiple()));
                        }
                        break;
                    case 2:
                        // double
                        if (settingLocation.getMultiple() != null) {
                            formattedData = String.format("%.2f", (double) data / settingLocation.getMultiple());
                        }
                        break;
                    case 3:
                        // bool
                        formattedData = "●";
                        boolTypeFlag = data >= 1 ? 1 : 2;
                        break;
                    case 4:
                        // 自定义枚举类型
                        String stateValue = String.valueOf(data);
                        if (settingLocation.getMultiple() != null) {
                            stateValue = String.valueOf((int) Math.ceil((double) data / settingLocation.getMultiple()));
                        }
                        String finalStateValue = stateValue;
                        Optional<SettingEnum> optionalSettingEnum
                                = settingLocation.getSettingEnumList().stream().filter(k -> k.getState().equals(finalStateValue)).findAny();
                        formattedData = optionalSettingEnum.map(settingEnum -> messages.getString(settingEnum.getIec())).orElseGet(() -> messages.getString("enum.default"));
                        break;
                }

                // 赋值到界面
                Optional<Tab> optionalTab
                        = tabPane.getTabs().stream().filter(k -> k.getId().equals(currentTab)).findAny();
                if (!optionalTab.isPresent()) {
                    continue;
                }
                ScrollPane sp = (ScrollPane) optionalTab.get().getContent();
                GridPane pg = (GridPane) sp.getContent();
                Optional<Node> node
                        = pg.getChildren().stream().filter(k -> k instanceof Button && k.getId().equals("btnValue_" + settingLocation.getAddress())).findAny();
                if (!node.isPresent()) {
                    continue;
                }
                ((Button) node.get()).setText(formattedData);

                if (boolTypeFlag == 1) {
                    ((Button) node.get()).setStyle("-fx-text-fill: green;");
                } else if (boolTypeFlag == 2) {
                    ((Button) node.get()).setStyle("-fx-text-fill: red;");
                }
            }
        }
    }

    /**
     * 控件监听文本只可以输入数字
     *
     * @param textField 需要监听的文本
     */
    private void texFieldNumberMatches(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d")) {
                textField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }
}
