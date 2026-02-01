package com.goldwind.javafxboot.ctrl;

import cn.hutool.core.date.DateUtil;
import com.goldwind.javafxboot.model.SettingEnum;
import com.goldwind.javafxboot.model.SettingGroup;
import com.goldwind.javafxboot.model.SettingLocation;
import com.goldwind.javafxboot.model.SettingShow;
import com.goldwind.javafxboot.service.ModbusDataService;
import com.goldwind.javafxboot.service.ThreadScheduledService;
import com.goldwind.javafxboot.util.InitAppSetting;
import com.goldwind.javafxboot.view.NavigationManager;
import com.goldwind.javafxboot.view.NavigationManager.Page;
import com.goldwind.javafxboot.view.ViewFactory;
import de.felixroske.jfxsupport.FXMLController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 主界面控制器
 * 
 * 重构说明：
 * 1. 协议层与显示层解耦，通过ModbusDataService统一访问数据
 * 2. 使用NavigationManager管理页面导航
 * 3. 使用ViewFactory创建各功能界面
 * 4. 控制命令通过数据服务层发送，不再硬编码地址
 *
 * @author xujianhua
 * @date 2024/4/23 2:01
 * @refactor HMI Team 2026-01-13
 */
@Slf4j
@FXMLController
public class MainCtrl implements Initializable {
    
    // ==================== FXML控件 ====================
    
    // 主容器
    public Pane rootPane;
    public StackPane contentArea;
    
    // 控件
    public Label labTime;
    public TextArea txtAreaData;
    public TabPane tabPane;
    public Button btnConnect;
    
    // 操作按钮
    public Button btn_start;
    public Button btn_stop;
    public Button btn_reset;
    public ToggleButton tbtn_maintain;
    
    // 导航按钮
    public Button navMain;
    public Button navComponent;
    public Button navFault;
    public Button navLogin;
    public Button navControl;
    
    // ==================== 服务和工具 ====================
    
    private ResourceBundle messages;
    private ThreadScheduledService threadScheduledService;
    private ModbusDataService dataService;
    private NavigationManager navigationManager;
    private ViewFactory viewFactory;
    
    // 数据缓存
    private String currentTab = "";
    private final Map<String, List<SettingLocation>> tabLocation = new HashMap<>();
    private int msgNumber = 0;
    
    DateFormat currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    // ==================== 初始化 ====================
    
    @SneakyThrows
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 初始化服务
        initServices();
        // 初始化语言及国际化
        initDefaultSetting();
        // 初始化通用控件信息
        initCommonControl();
        // 初始化导航
        initNavigation();
        // 初始化TabPane（用于主界面）
        initTabPaneControl();
        // 初始化时间计时控件
        initTimeControl();
        // 默认显示主界面
        updateNavigationStyle(Page.MAIN);
    }
    
    /**
     * 初始化服务层
     */
    private void initServices() {
        // 初始化数据服务
        dataService = ModbusDataService.getInstance();
        dataService.initialize();
        
        // 初始化导航管理器
        navigationManager = NavigationManager.getInstance();
        
        // 初始化视图工厂
        viewFactory = ViewFactory.getInstance();
        
        // 注册页面创建器
        navigationManager.registerPageCreator(Page.MAIN, this::createMainPageContent);
        navigationManager.registerPageCreator(Page.COMPONENT, viewFactory::createComponentView);
        navigationManager.registerPageCreator(Page.FAULT, viewFactory::createFaultView);
        navigationManager.registerPageCreator(Page.LOGIN, viewFactory::createLoginView);
        navigationManager.registerPageCreator(Page.CONTROL, viewFactory::createControlView);
        
        // 设置页面变化回调
        navigationManager.setOnPageChanged(this::onPageChanged);
    }
    
    /**
     * 创建主界面内容
     */
    private Node createMainPageContent() {
        // 主界面使用TabPane显示核心数据
        return tabPane;
    }
    
    /**
     * 初始化导航
     */
    private void initNavigation() {
        // 初始化导航按钮样式
        navMain.setUserData(Page.MAIN);
        navComponent.setUserData(Page.COMPONENT);
        navFault.setUserData(Page.FAULT);
        navLogin.setUserData(Page.LOGIN);
        navControl.setUserData(Page.CONTROL);
    }
    
    /**
     * 页面变化处理
     */
    private void onPageChanged() {
        Page page = navigationManager.getCurrentPage();
        updateNavigationStyle(page);
        
        // 更新内容区域
        if (contentArea != null) {
            contentArea.getChildren().clear();
            Node pageView = navigationManager.getCurrentPageView();
            if (pageView != null) {
                contentArea.getChildren().add(pageView);
            }
        }
    }
    
    /**
     * 更新导航按钮样式
     */
    private void updateNavigationStyle(Page activePage) {
        String activeStyle = "-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 0;";
        String normalStyle = "-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 0;";
        
        navMain.setStyle(activePage == Page.MAIN ? activeStyle : normalStyle);
        navComponent.setStyle(activePage == Page.COMPONENT ? activeStyle : normalStyle);
        navFault.setStyle(activePage == Page.FAULT ? activeStyle : normalStyle);
        navLogin.setStyle(activePage == Page.LOGIN ? activeStyle : normalStyle);
        navControl.setStyle(activePage == Page.CONTROL ? activeStyle : normalStyle);
    }
    
    // ==================== 导航事件处理 ====================
    
    public void navMainClick(ActionEvent event) {
        navigationManager.navigateTo(Page.MAIN);
    }
    
    public void navComponentClick(ActionEvent event) {
        navigationManager.navigateTo(Page.COMPONENT);
    }
    
    public void navFaultClick(ActionEvent event) {
        navigationManager.navigateTo(Page.FAULT);
    }
    
    public void navLoginClick(ActionEvent event) {
        navigationManager.navigateTo(Page.LOGIN);
    }
    
    public void navControlClick(ActionEvent event) {
        navigationManager.navigateTo(Page.CONTROL);
    }
    
    // ==================== 连接控制 ====================
    
    public void onBtnConnectClick(ActionEvent actionEvent) {
        try {
            if (threadScheduledService != null) {
                if (threadScheduledService.isRunning()) {
                    threadScheduledService.cancel();
                    btnConnect.setStyle("-fx-text-fill: green; -fx-font-size: 18px;");
                    btnConnect.setText("▶");
                    dataService.setConnected(false);
                } else {
                    threadScheduledService.restart();
                    btnConnect.setStyle("-fx-text-fill: red; -fx-font-size: 18px;");
                    btnConnect.setText("●");
                    dataService.setConnected(true);
                }
            } else {
                startModbusService();
            }
        } catch (Exception e) {
            log.error("error: ", e);
        }
    }
    
    /**
     * 启动Modbus服务
     */
    private void startModbusService() {
        btnConnect.setStyle("-fx-text-fill: red; -fx-font-size: 18px;");
        btnConnect.setText("●");
        
        threadScheduledService = new ThreadScheduledService("modbus-tcp service");
        threadScheduledService.setPeriod(Duration.millis(InitAppSetting.getSetting().getSettingServer().getCycle() * 1000.0));
        threadScheduledService.setDelay(Duration.millis(500));
        threadScheduledService.setRestartOnFailure(false);
        
        // 设置数据服务的调度服务引用
        dataService.setScheduledService(threadScheduledService);
        dataService.setConnected(true);
        
        // 数据更新回调
        threadScheduledService.setOnSucceeded(t -> {
            if (t.getSource().getValue() != null && !((Map<Integer, Object>) t.getSource().getValue()).isEmpty()) {
                msgNumber++;
                if (msgNumber >= 10) {
                    txtAreaData.clear();
                    msgNumber = 0;
                }
                
                Map<Integer, Object> readData = (Map<Integer, Object>) t.getSource().getValue();
                txtAreaData.appendText("[" + DateUtil.now() + "] " + readData + "\n");
                
                // 更新数据服务缓存
                dataService.updateData(readData);
                
                // 更新当前Tab的界面（主界面）
                loadModbusData(readData);
                
                // 更新部件界面的数据（如果当前在部件页面）
                if (navigationManager.getCurrentPage() == NavigationManager.Page.COMPONENT) {
                    viewFactory.refreshComponentData();
                }
            }
        });
        
        threadScheduledService.start();
    }
    
    // ==================== 控制按钮事件 ====================
    
    /**
     * 启动按钮点击 - 使用数据服务发送命令
     */
    public void btn_startClick(ActionEvent event) {
        if (dataService.isConnected()) {
            boolean success = dataService.sendStartCommand();
            if (!success) {
                showMessage("启动命令发送失败！");
            }
        } else {
            showMessage("未建立连接！(No connection established!)");
        }
    }
    
    /**
     * 停止按钮点击
     */
    public void btn_stopClick(ActionEvent event) {
        if (dataService.isConnected()) {
            boolean success = dataService.sendStopCommand();
            if (!success) {
                showMessage("停止命令发送失败！");
            }
        } else {
            showMessage("未建立连接！(No connection established!)");
        }
    }
    
    /**
     * 复位按钮点击
     */
    public void btn_resetClick(ActionEvent event) {
        if (dataService.isConnected()) {
            boolean success = dataService.sendResetCommand();
            if (!success) {
                showMessage("复位命令发送失败！");
            }
        } else {
            showMessage("未建立连接！(No connection established!)");
        }
    }
    
    /**
     * 维护按钮点击
     */
    public void tbtn_maintainClick(ActionEvent event) {
        if (dataService.isConnected()) {
            boolean success = dataService.setMaintainMode(tbtn_maintain.isSelected());
            if (!success) {
                tbtn_maintain.setSelected(false);
                showMessage("维护命令发送失败！");
            }
        } else {
            tbtn_maintain.setSelected(false);
            showMessage("未建立连接！(No connection established!)");
        }
    }
    
    /**
     * 显示消息
     */
    private void showMessage(String message) {
        txtAreaData.appendText("[" + DateUtil.now() + "] " + message + "\n");
    }
    
    // ==================== 原有初始化方法 ====================
    
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
        btnConnect.setStyle("-fx-text-fill: green; -fx-font-size: 18px;");
    }
    
    /**
     * 判断是否为主界面显示的分栏
     * 主界面只显示核心状态信息，部件详情移到"部件"导航
     * 
     * @param showIndex 分栏索引
     * @return true表示在主界面显示
     */
    private boolean isMainPageShow(int showIndex) {
        // 主界面显示的分栏：
        // 1 - 主状态
        // 3 - 系统信息
        // 16 - 故障（故障也在单独的故障页面显示）
        return showIndex == 1 || showIndex == 3 || showIndex == 16;
    }

    /**
     * 初始化TabPane - 只显示主界面核心分栏
     * 部件相关的分栏（变流、变桨等）移到"部件"导航页面
     */
    private void initTabPaneControl() {
        for (SettingShow settingShow : InitAppSetting.getSetting().getSettingShowList()) {
            // 只在主界面显示核心分栏，部件相关的分栏移到"部件"导航
            if (!isMainPageShow(settingShow.getIndex())) {
                continue;
            }
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

            javafx.scene.layout.GridPane gridPane = new javafx.scene.layout.GridPane();
            gridPane.setHgap(5);
            gridPane.setVgap(5);

            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setContent(gridPane);
            scrollPane.setFitToHeight(true);
            scrollPane.setFitToWidth(true);

            int row = 1;
            int column = 1;
            int maxColumn = settingShow.getColumn();

            for (SettingLocation settingLocation : currentShowLocation) {
                if (settingLocation.getFnc_lab() == 1) {
                    String lbNameText = messages.getString(settingLocation.getIec());
                    Label lbName = new Label(lbNameText + ": ");
                    Tooltip tooltip = new Tooltip(lbNameText);
                    lbName.setTooltip(tooltip);
                    gridPane.add(lbName, column, row);
                    column++;

                    Button btnValue = new Button(" -- ");
                    btnValue.setId("btnValue_" + settingLocation.getAddress());
                    btnValue.setDisable(false);
                    gridPane.add(btnValue, column, row);
                    column++;

                    Label lbUnit = new Label(settingLocation.getUnit());
                    gridPane.add(lbUnit, column, row);
                    column++;

                    Label lbSplit = new Label("|");
                    gridPane.add(lbSplit, column, row);
                } else if (settingLocation.getFnc_lab() == 2) {
                    String lbNameText = messages.getString(settingLocation.getIec());
                    Button btnValue = new Button(lbNameText);
                    btnValue.setId("btnvalue_fnc2" + settingLocation.getAddress());
                    
                    final int address = settingLocation.getAddress();
                    btnValue.setOnAction((event) -> {
                        if (dataService.isConnected()) {
                            dataService.sendCommandByAddress(address, 1);
                        } else {
                            showMessage("未建立连接！(No connection established!)");
                        }
                    });

                    gridPane.add(btnValue, column, row);
                    column++;
                    column++;
                    column++;
                    Label lbSplit = new Label("|");
                    gridPane.add(lbSplit, column, row);
                } else if (settingLocation.getFnc_lab() == 3) {
                    String lbNameText = messages.getString(settingLocation.getIec());
                    Label lbBtState = new Label(" --- ");
                    Tooltip tooltip = new Tooltip("按钮状态，---为未按下，-V-为按下");
                    lbBtState.setTooltip(tooltip);
                    
                    ToggleButton toggleButton = new ToggleButton(lbNameText);
                    final int address = settingLocation.getAddress();
                    toggleButton.setOnAction((event) -> {
                        if (dataService.isConnected()) {
                            if (toggleButton.isSelected()) {
                                lbBtState.setText(" -√- ");
                                dataService.sendCommandByAddress(address, 1);
                            } else {
                                dataService.sendCommandByAddress(address, 0);
                                lbBtState.setText(" --- ");
                            }
                        } else {
                            toggleButton.setSelected(false);
                            showMessage("未建立连接！(No connection established!)");
                        }
                    });
                    gridPane.add(toggleButton, column, row);
                    column++;
                    gridPane.add(lbBtState, column, row);
                    column++;
                    column++;
                    Label lbSplit = new Label("|");
                    gridPane.add(lbSplit, column, row);
                } else if (settingLocation.getFnc_lab() == 4) {
                    String lbNameText = messages.getString(settingLocation.getIec());
                    TextField tfInput = new TextField("");
                    tfInput.setId("tfivalue_fnc4" + settingLocation.getAddress());
                    
                    Button btnValue = new Button(lbNameText);
                    btnValue.setId("btnvalue_fnc4" + settingLocation.getAddress());
                    
                    final int address = settingLocation.getAddress();
                    btnValue.setOnAction((event) -> {
                        if (dataService.isConnected()) {
                            String tfValueStr = tfInput.getText();
                            try {
                                int tfValueInt = Integer.parseInt(tfValueStr);
                                dataService.sendCommandByAddress(address, tfValueInt);
                            } catch (NumberFormatException e) {
                                showMessage("输入值无效！(The input value is invalid!)");
                            }
                        } else {
                            showMessage("未建立连接！(No connection established!)");
                        }
                    });

                    Label lbUnit = new Label(settingLocation.getUnit());

                    gridPane.add(btnValue, column, row);
                    column++;
                    gridPane.add(tfInput, column, row);
                    column++;
                    gridPane.add(lbUnit, column, row);
                    column++;
                    Label lbSplit = new Label("|");
                    gridPane.add(lbSplit, column, row);
                } else if (settingLocation.getFnc_lab() == 5) {
                    String lbNameText;
                    int listsize = settingLocation.getSettingEnumList().size();
                    int listsize_count = 0;
                    for (int loopList = 0; loopList < (listsize - 1); loopList++) {
                        lbNameText = messages.getString(settingLocation.getSettingEnumList().get(loopList).getIec());
                        Label lbName = new Label(lbNameText);
                        gridPane.add(lbName, column, row);
                        column++;

                        Button btnValue1 = new Button("--");
                        String Idvalue = "btnvalue_fnc5_" + settingLocation.getAddress().toString() + "_" + Integer.toString(listsize_count);
                        btnValue1.setId(Idvalue);
                        listsize_count++;
                        gridPane.add(btnValue1, column, row);
                        column++;
                        column++;
                        Label lbSplit1 = new Label("|");
                        gridPane.add(lbSplit1, column, row);
                        if (column < maxColumn * 4) {
                            column++;
                        } else {
                            row++;
                            column = 1;
                        }
                    }
                    lbNameText = messages.getString(settingLocation.getSettingEnumList().get(listsize - 1).getIec());
                    Label lbName2 = new Label(lbNameText);
                    gridPane.add(lbName2, column, row);
                    column++;

                    Button btnValue2 = new Button("--");
                    String Idvalue2 = "btnvalue_fnc5_" + settingLocation.getAddress().toString() + "_" + Integer.toString(listsize_count);
                    btnValue2.setId(Idvalue2);
                    listsize_count++;
                    gridPane.add(btnValue2, column, row);

                    column++;
                    column++;
                    Label lbSplit2 = new Label("|");
                    gridPane.add(lbSplit2, column, row);
                }

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
                currentTab = newTab.getId();
            }
        });
    }

    private void initTimeControl() {
        Timeline animation = new Timeline(new KeyFrame(Duration.millis(1000), e -> {
            labTime.setText(currentTime.format(new Date()));
        }));
        animation.setCycleCount(Animation.INDEFINITE);
        animation.play();
    }

    /**
     * 加载Modbus数据到界面
     */
    private void loadModbusData(Map<Integer, Object> readMapData) {
        List<SettingLocation> currentShowLocationList = tabLocation.get(currentTab);
        if (currentShowLocationList == null) return;
        
        for (SettingLocation settingLocation : currentShowLocationList) {
            if (settingLocation.getFnc_lab() == 1) {
                Object rawValue = readMapData.get(settingLocation.getAddress());
                if (rawValue == null) continue;
                
                int data = Integer.parseInt(rawValue.toString());
                String formattedData = Integer.toString(data);
                int boolTypeFlag = 0;

                switch (settingLocation.getType()) {
                    case 1:
                        if (settingLocation.getMultiple() != null) {
                            formattedData = String.valueOf((int) Math.ceil((double) data / settingLocation.getMultiple()));
                        }
                        break;
                    case 2:
                        if (settingLocation.getMultiple() != null) {
                            formattedData = String.format("%.2f", (double) data / settingLocation.getMultiple());
                        }
                        break;
                    case 3:
                        formattedData = "●";
                        boolTypeFlag = data >= 1 ? 1 : 2;
                        break;
                    case 4:
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

                Optional<Tab> optionalTab = tabPane.getTabs().stream().filter(k -> k.getId().equals(currentTab)).findAny();
                if (!optionalTab.isPresent()) continue;
                
                ScrollPane sp = (ScrollPane) optionalTab.get().getContent();
                javafx.scene.layout.GridPane pg = (javafx.scene.layout.GridPane) sp.getContent();
                Optional<Node> node = pg.getChildren().stream()
                        .filter(k -> k instanceof Button && k.getId() != null && k.getId().equals("btnValue_" + settingLocation.getAddress()))
                        .findAny();
                if (!node.isPresent()) continue;
                
                ((Button) node.get()).setText(formattedData);
                if (boolTypeFlag == 1) {
                    ((Button) node.get()).setStyle("-fx-text-fill: green;");
                } else if (boolTypeFlag == 2) {
                    ((Button) node.get()).setStyle("-fx-text-fill: red;");
                }
            } else if (settingLocation.getFnc_lab() == 5) {
                Object rawValue = readMapData.get(settingLocation.getAddress());
                if (rawValue == null) continue;
                
                int data = Integer.parseInt(rawValue.toString());
                int sizeofList = settingLocation.getSettingEnumList().size();
                
                if (sizeofList <= 16) {
                    String formattedData = "●";
                    for (int loopkey = 0; loopkey < sizeofList; loopkey++) {
                        int boolTypeFlag = ((data & (0x0001 << loopkey)) != 0) ? 1 : 2;
                        
                        Optional<Tab> optionalTab = tabPane.getTabs().stream().filter(k -> k.getId().equals(currentTab)).findAny();
                        if (!optionalTab.isPresent()) continue;
                        
                        String btnIDvalue = "btnvalue_fnc5_" + settingLocation.getAddress().toString() + "_" + Integer.toString(loopkey);
                        ScrollPane sp = (ScrollPane) optionalTab.get().getContent();
                        javafx.scene.layout.GridPane pg = (javafx.scene.layout.GridPane) sp.getContent();
                        Optional<Node> node = pg.getChildren().stream()
                                .filter(k -> k instanceof Button && k.getId() != null && k.getId().equals(btnIDvalue))
                                .findAny();
                        if (!node.isPresent()) continue;
                        
                        ((Button) node.get()).setText(formattedData);
                        if (boolTypeFlag == 1) {
                            ((Button) node.get()).setStyle("-fx-text-fill: green;");
                        } else if (boolTypeFlag == 2) {
                            ((Button) node.get()).setStyle("-fx-text-fill: red;");
                        }
                    }
                }
            }
        }
    }
}
