package com.goldwind.javafxboot.view;

import com.goldwind.javafxboot.adapter.PointDefinition;
import com.goldwind.javafxboot.model.SettingShow;
import com.goldwind.javafxboot.service.ModbusDataService;
import com.goldwind.javafxboot.util.InitAppSetting;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * 视图工厂 - 负责创建各个功能界面
 * 
 * @author HMI Team
 * @date 2026-01-13
 */
@Slf4j
public class ViewFactory {
    
    private static volatile ViewFactory instance;
    
    private final ModbusDataService dataService;
    private ResourceBundle messages;
    
    private ViewFactory() {
        this.dataService = ModbusDataService.getInstance();
        initMessages();
    }
    
    public static ViewFactory getInstance() {
        if (instance == null) {
            synchronized (ViewFactory.class) {
                if (instance == null) {
                    instance = new ViewFactory();
                }
            }
        }
        return instance;
    }
    
    private void initMessages() {
        Locale locale;
        String language = InitAppSetting.getSetting().getSettingEnvironment().getLanguage();
        if ("cn".equals(language)) {
            locale = Locale.CHINA;
        } else {
            locale = Locale.ENGLISH;
        }
        messages = ResourceBundle.getBundle("language/messages", locale);
    }
    
    /**
     * 获取国际化文本
     */
    public String getMessage(String key) {
        try {
            return messages.getString(key);
        } catch (Exception e) {
            return key;
        }
    }
    
    // ==================== 主界面 ====================
    
    /**
     * 创建主界面视图
     */
    public Node createMainView() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // 获取显示分栏配置
        List<SettingShow> showList = InitAppSetting.getSetting().getSettingShowList();
        
        for (SettingShow show : showList) {
            // 只显示部分核心分栏在主界面
            if (isMainPageShow(show.getIndex())) {
                Tab tab = createDataTab(show);
                tabPane.getTabs().add(tab);
            }
        }
        
        return tabPane;
    }
    
    /**
     * 判断是否为主界面显示的分栏
     */
    private boolean isMainPageShow(int showIndex) {
        // 主界面显示：主状态(1)、系统信息(3)、振动监测(6)、电网(8)
        return showIndex == 1 || showIndex == 3 || showIndex == 6 || showIndex == 8;
    }
    
    // ==================== 部件界面 ====================
    
    /**
     * 部件主容器（用于切换列表/详情视图）
     */
    private StackPane componentMainContainer;
    
    /**
     * 部件列表视图
     */
    private Node componentListView;
    
    /**
     * 当前选中的部件showIndex
     */
    private int currentComponentShowIndex = -1;
    
    /**
     * 当前部件名称
     */
    private String currentComponentName = "";
    
    /**
     * 部件详情数据控件映射 (用于数据更新)
     */
    private final Map<String, Button> componentDataButtons = new HashMap<>();
    
    /**
     * 创建部件界面视图
     */
    public Node createComponentView() {
        componentMainContainer = new StackPane();
        
        // 创建部件列表视图
        componentListView = createComponentListView();
        componentMainContainer.getChildren().add(componentListView);
        
        return componentMainContainer;
    }
    
    /**
     * 创建部件列表视图（按钮网格）
     */
    private Node createComponentListView() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setAlignment(Pos.TOP_CENTER);
        container.setStyle("-fx-background-color: #f5f6fa;");
        
        Label title = new Label("部件监控");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label hint = new Label("请选择要查看的部件");
        hint.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        
        // 部件按钮网格
        GridPane buttonGrid = new GridPane();
        buttonGrid.setHgap(20);
        buttonGrid.setVgap(20);
        buttonGrid.setAlignment(Pos.CENTER);
        
        // 定义部件按钮
        String[][] components = {
            {"变流器", "5"},    
            {"变桨系统", "4"},   
            {"测风/偏航系统", "7"},     
            {"传动链", "11"},    
            {"电网", "8"},       
            {"液压系统", "12"},  
            {"冷却系统", "14"},  
            {"发电机", "10"},    
            {"机舱", "13"},      
            {"塔基", "15"},      
            {"振动监测", "6"},   
            {"统计", "17"}       
        };
        
        int col = 0, row = 0;
        for (String[] comp : components) {
            Button btn = createComponentButton(comp[0], Integer.parseInt(comp[1]));
            buttonGrid.add(btn, col, row);
            col++;
            if (col >= 4) {
                col = 0;
                row++;
            }
        }
        
        container.getChildren().addAll(title, hint, buttonGrid);
        
        return container;
    }
    
    /**
     * 创建部件按钮
     */
    private Button createComponentButton(String name, int showIndex) {
        Button btn = new Button(name);
        btn.setPrefSize(150, 80);
        btn.setStyle("-fx-font-size: 14px; -fx-background-color: #3498db; -fx-text-fill: white; " +
                    "-fx-background-radius: 8px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);");
        
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-font-size: 14px; -fx-background-color: #2980b9; " +
                "-fx-text-fill: white; -fx-background-radius: 8px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 3);"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-font-size: 14px; -fx-background-color: #3498db; " +
                "-fx-text-fill: white; -fx-background-radius: 8px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);"));
        
        btn.setOnAction(e -> navigateToComponentDetail(name, showIndex));
        
        return btn;
    }
    
    /**
     * 跳转到部件详情页面（全屏显示）
     */
    private void navigateToComponentDetail(String name, int showIndex) {
        currentComponentShowIndex = showIndex;
        currentComponentName = name;
        componentDataButtons.clear();
        
        // 获取该分栏配置
        List<SettingShow> showList = InitAppSetting.getSetting().getSettingShowList();
        Optional<SettingShow> settingShow = showList.stream()
                .filter(s -> s.getIndex() == showIndex)
                .findFirst();
        
        if (settingShow.isPresent() && componentMainContainer != null) {
            // 创建详情页面
            Node detailView = createComponentDetailView(name, settingShow.get());
            
            // 切换视图
            componentMainContainer.getChildren().clear();
            componentMainContainer.getChildren().add(detailView);
        }
    }
    
    /**
     * 创建部件详情视图（全屏）
     */
    private Node createComponentDetailView(String name, SettingShow show) {
        BorderPane detailPane = new BorderPane();
        detailPane.setStyle("-fx-background-color: #f5f6fa;");
        
        // 顶部：标题栏和返回按钮
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15, 20, 15, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: #3498db;");
        
        Button backBtn = new Button("← 返回");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; " +
                        "-fx-cursor: hand; -fx-border-color: white; -fx-border-radius: 5px; -fx-padding: 8 15;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle("-fx-background-color: rgba(255,255,255,0.2); -fx-text-fill: white; " +
                "-fx-font-size: 14px; -fx-cursor: hand; -fx-border-color: white; -fx-border-radius: 5px; -fx-padding: 8 15;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 14px; " +
                "-fx-cursor: hand; -fx-border-color: white; -fx-border-radius: 5px; -fx-padding: 8 15;"));
        backBtn.setOnAction(e -> navigateBackToComponentList());
        
        Label titleLabel = new Label(name + " - 实时监控数据");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // 右侧显示数据点数量
        Label countLabel = new Label();
        countLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.8);");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        
        topBar.getChildren().addAll(backBtn, titleLabel, spacer, countLabel);
        
        // 中间：数据内容区域
        Node dataContent = createComponentDataGrid(show);
        
        // 统计数据点数量
        int pointCount = componentDataButtons.size();
        countLabel.setText("共 " + pointCount + " 个数据点");
        
        ScrollPane scrollPane = new ScrollPane(dataContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        scrollPane.setPadding(new Insets(10));
        
        detailPane.setTop(topBar);
        detailPane.setCenter(scrollPane);
        
        return detailPane;
    }
    
    /**
     * 返回部件列表
     */
    private void navigateBackToComponentList() {
        currentComponentShowIndex = -1;
        currentComponentName = "";
        componentDataButtons.clear();
        
        if (componentMainContainer != null && componentListView != null) {
            componentMainContainer.getChildren().clear();
            componentMainContainer.getChildren().add(componentListView);
        }
    }
    
    /**
     * 创建部件数据网格（带ID便于更新）
     */
    private Node createComponentDataGrid(SettingShow show) {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20));
        gridPane.setStyle("-fx-background-color: white; -fx-background-radius: 10px;");
        
        // 从现有配置获取该分栏的点
        List<com.goldwind.javafxboot.model.SettingLocation> locations = new ArrayList<>();
        for (com.goldwind.javafxboot.model.SettingGroup group : InitAppSetting.getSetting().getSettingGroupList()) {
            for (com.goldwind.javafxboot.model.SettingLocation loc : group.getSettingLocationList()) {
                if (loc.getShow().equals(show.getIndex())) {
                    locations.add(loc);
                }
            }
        }
        
        int row = 0, col = 0;
        int maxCol = Math.max(show.getColumn(), 3);
        
        for (com.goldwind.javafxboot.model.SettingLocation loc : locations) {
            if (loc.getFnc_lab() == 1 || loc.getFnc_lab() == 5) {
                // 创建数据项容器
                VBox itemBox = new VBox(3);
                itemBox.setAlignment(Pos.CENTER);
                itemBox.setPadding(new Insets(8));
                itemBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5px; -fx-border-color: #e9ecef; -fx-border-radius: 5px;");
                itemBox.setPrefWidth(180);
                
                // 标签名称
                String labelText = getMessage(loc.getIec());
                Label nameLabel = new Label(labelText);
                nameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
                nameLabel.setWrapText(true);
                nameLabel.setMaxWidth(170);
                nameLabel.setTooltip(new Tooltip(labelText));
                
                // 数值按钮
                Button valueBtn = new Button("--");
                valueBtn.setMinWidth(80);
                valueBtn.setMinHeight(30);
                valueBtn.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 3px; " +
                                 "-fx-font-size: 14px; -fx-font-weight: bold;");
                
                // 注册到映射表以便更新
                String btnId = "comp_" + show.getIndex() + "_" + loc.getAddress();
                valueBtn.setId(btnId);
                componentDataButtons.put(btnId, valueBtn);
                
                // 立即获取当前值
                updateComponentButtonValue(valueBtn, loc);
                
                // 单位
                Label unitLabel = new Label(loc.getUnit() != null ? loc.getUnit() : "");
                unitLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #adb5bd;");
                
                itemBox.getChildren().addAll(nameLabel, valueBtn, unitLabel);
                
                gridPane.add(itemBox, col, row);
                
                col++;
                if (col >= maxCol) {
                    col = 0;
                    row++;
                }
            }
        }
        
        return gridPane;
    }
    
    /**
     * 更新部件数据按钮的值
     */
    private void updateComponentButtonValue(Button btn, com.goldwind.javafxboot.model.SettingLocation loc) {
        // 从数据缓存获取值
        Object rawValue = dataService.getValueByAddress(loc.getAddress());
        if (rawValue == null) {
            btn.setText("--");
            return;
        }
        
        try {
            int data = Integer.parseInt(rawValue.toString());
            String formattedData;
            int boolTypeFlag = 0;
            
            switch (loc.getType()) {
                case 1: // int
                    if (loc.getMultiple() != null && loc.getMultiple() != 0) {
                        formattedData = String.valueOf((int) Math.ceil((double) data / loc.getMultiple()));
                    } else {
                        formattedData = String.valueOf(data);
                    }
                    break;
                case 2: // double
                    if (loc.getMultiple() != null && loc.getMultiple() != 0) {
                        formattedData = String.format("%.2f", (double) data / loc.getMultiple());
                    } else {
                        formattedData = String.valueOf(data);
                    }
                    break;
                case 3: // bool
                    formattedData = "●";
                    boolTypeFlag = data >= 1 ? 1 : 2;
                    break;
                case 4: // 枚举
                    String stateValue = String.valueOf(data);
                    if (loc.getMultiple() != null && loc.getMultiple() != 0) {
                        stateValue = String.valueOf((int) Math.ceil((double) data / loc.getMultiple()));
                    }
                    formattedData = "未知";
                    for (com.goldwind.javafxboot.model.SettingEnum enumDef : loc.getSettingEnumList()) {
                        if (enumDef.getState().equals(stateValue)) {
                            formattedData = getMessage(enumDef.getIec());
                            break;
                        }
                    }
                    break;
                default:
                    formattedData = String.valueOf(data);
            }
            
            btn.setText(formattedData);
            
            if (boolTypeFlag == 1) {
                btn.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 3px; " +
                            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
            } else if (boolTypeFlag == 2) {
                btn.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 3px; " +
                            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #e74c3c;");
            } else {
                btn.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 3px; " +
                            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            }
        } catch (Exception e) {
            btn.setText("--");
        }
    }
    
    /**
     * 刷新当前部件详情数据（由外部定时调用）
     */
    public void refreshComponentData() {
        if (currentComponentShowIndex < 0 || componentDataButtons.isEmpty()) {
            return;
        }
        
        // 获取该分栏的所有点
        for (com.goldwind.javafxboot.model.SettingGroup group : InitAppSetting.getSetting().getSettingGroupList()) {
            for (com.goldwind.javafxboot.model.SettingLocation loc : group.getSettingLocationList()) {
                if (loc.getShow().equals(currentComponentShowIndex)) {
                    String btnId = "comp_" + currentComponentShowIndex + "_" + loc.getAddress();
                    Button btn = componentDataButtons.get(btnId);
                    if (btn != null) {
                        updateComponentButtonValue(btn, loc);
                    }
                }
            }
        }
    }
    
    /**
     * 获取当前部件showIndex
     */
    public int getCurrentComponentShowIndex() {
        return currentComponentShowIndex;
    }
    
    /**
     * 是否在部件详情页面
     */
    public boolean isInComponentDetail() {
        return currentComponentShowIndex > 0;
    }
    
    // ==================== 故障界面 ====================
    
    /**
     * 创建故障界面视图
     */
    public Node createFaultView() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(20));
        
        Label title = new Label("故障信息");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // 故障列表
        TableView<FaultItem> faultTable = new TableView<>();
        faultTable.setPrefHeight(400);
        
        TableColumn<FaultItem, String> nameCol = new TableColumn<>("故障项");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        nameCol.setPrefWidth(200);
        
        TableColumn<FaultItem, String> valueCol = new TableColumn<>("故障码");
        valueCol.setCellValueFactory(data -> data.getValue().valueProperty());
        valueCol.setPrefWidth(100);
        
        TableColumn<FaultItem, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(data -> data.getValue().statusProperty());
        statusCol.setPrefWidth(100);
        
        faultTable.getColumns().addAll(nameCol, valueCol, statusCol);
        
        // 获取故障点数据 (showIndex = 16)
        List<PointDefinition> faultPoints = dataService.getPointsByShowIndex(16);
        for (PointDefinition point : faultPoints) {
            FaultItem item = new FaultItem();
            item.setName(getMessage(point.getIec()));
            
            Object value = dataService.getValue(point.getName());
            item.setValue(value != null ? value.toString() : "--");
            
            // 判断状态
            Integer intValue = dataService.getIntValue(point.getName());
            if (intValue != null && intValue > 0) {
                item.setStatus("异常");
            } else {
                item.setStatus("正常");
            }
            
            faultTable.getItems().add(item);
        }
        
        // 刷新按钮
        Button refreshBtn = new Button("刷新");
        refreshBtn.setOnAction(e -> refreshFaultTable(faultTable));
        
        container.getChildren().addAll(title, faultTable, refreshBtn);
        
        return new ScrollPane(container);
    }
    
    /**
     * 刷新故障表格
     */
    private void refreshFaultTable(TableView<FaultItem> table) {
        table.getItems().clear();
        List<PointDefinition> faultPoints = dataService.getPointsByShowIndex(16);
        for (PointDefinition point : faultPoints) {
            FaultItem item = new FaultItem();
            item.setName(getMessage(point.getIec()));
            
            Object value = dataService.getValue(point.getName());
            item.setValue(value != null ? value.toString() : "--");
            
            Integer intValue = dataService.getIntValue(point.getName());
            if (intValue != null && intValue > 0) {
                item.setStatus("异常");
            } else {
                item.setStatus("正常");
            }
            
            table.getItems().add(item);
        }
    }
    
    // ==================== 登录界面 ====================
    
    /**
     * 创建登录界面视图
     */
    public Node createLoginView() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(50));
        container.setAlignment(Pos.CENTER);
        container.setMaxWidth(400);
        
        Label title = new Label("用户登录");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        // 用户名
        HBox userBox = new HBox(10);
        userBox.setAlignment(Pos.CENTER);
        Label userLabel = new Label("用户名：");
        userLabel.setPrefWidth(80);
        TextField userField = new TextField();
        userField.setPrefWidth(200);
        userBox.getChildren().addAll(userLabel, userField);
        
        // 密码
        HBox pwdBox = new HBox(10);
        pwdBox.setAlignment(Pos.CENTER);
        Label pwdLabel = new Label("密  码：");
        pwdLabel.setPrefWidth(80);
        PasswordField pwdField = new PasswordField();
        pwdField.setPrefWidth(200);
        pwdBox.getChildren().addAll(pwdLabel, pwdField);
        
        // 按钮
        HBox btnBox = new HBox(20);
        btnBox.setAlignment(Pos.CENTER);
        
        Button loginBtn = new Button("登入");
        loginBtn.setPrefSize(100, 35);
        loginBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        loginBtn.setOnAction(e -> handleLogin(userField.getText(), pwdField.getText()));
        
        Button logoutBtn = new Button("登出");
        logoutBtn.setPrefSize(100, 35);
        logoutBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
        logoutBtn.setOnAction(e -> handleLogout());
        
        btnBox.getChildren().addAll(loginBtn, logoutBtn);
        
        // 状态提示
        Label statusLabel = new Label("");
        statusLabel.setId("loginStatus");
        
        container.getChildren().addAll(title, userBox, pwdBox, btnBox, statusLabel);
        
        // 包装在居中容器中
        BorderPane wrapper = new BorderPane();
        wrapper.setCenter(container);
        
        return wrapper;
    }
    
    /**
     * 处理登录
     */
    private void handleLogin(String username, String password) {
        if (username == null || username.isEmpty()) {
            showAlert("请输入用户名");
            return;
        }
        
        // 发送登录命令到PLC
        // 地址130 = 登入命令
        boolean success = dataService.sendCommandByAddress(130, 1);
        if (success) {
            // 发送用户名 (地址101)
            try {
                int userCode = Integer.parseInt(username);
                dataService.sendCommandByAddress(101, userCode);
            } catch (NumberFormatException e) {
                // 用户名不是数字，忽略
            }
            
            // 发送密码 (地址128)
            try {
                int pwdCode = Integer.parseInt(password);
                dataService.sendCommandByAddress(128, pwdCode);
            } catch (NumberFormatException e) {
                // 密码不是数字，忽略
            }
            
            showAlert("登录命令已发送");
        } else {
            showAlert("登录失败：未建立连接");
        }
    }
    
    /**
     * 处理登出
     */
    private void handleLogout() {
        // 发送登出命令到PLC (地址131)
        boolean success = dataService.sendCommandByAddress(131, 1);
        if (success) {
            showAlert("登出命令已发送");
        } else {
            showAlert("登出失败：未建立连接");
        }
    }
    
    // ==================== 控制界面 ====================
    
    /**
     * 创建控制界面视图
     */
    public Node createControlView() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(20));
        container.setAlignment(Pos.TOP_CENTER);
        
        Label title = new Label("控制功能");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // 控制按钮网格
        GridPane buttonGrid = new GridPane();
        buttonGrid.setHgap(15);
        buttonGrid.setVgap(15);
        buttonGrid.setAlignment(Pos.CENTER);
        
        // 定义控制按钮 - 与现有工程分类保持一致
        String[][] controls = {
            {"水冷/变流控制", "18"},
            {"机舱控制", "19"},
            {"变桨控制", "20"},
            {"功率控制", "21"},
            {"故障生产文件", "22"},
            {"偏航控制", "23"}
        };
        
        int col = 0, row = 0;
        for (String[] ctrl : controls) {
            Button btn = createControlButton(ctrl[0], Integer.parseInt(ctrl[1]));
            buttonGrid.add(btn, col, row);
            col++;
            if (col >= 3) {
                col = 0;
                row++;
            }
        }
        
        container.getChildren().addAll(title, buttonGrid);
        
        return new ScrollPane(container);
    }
    
    /**
     * 创建控制按钮
     */
    private Button createControlButton(String name, int showIndex) {
        Button btn = new Button(name);
        btn.setPrefSize(180, 80);
        btn.setStyle("-fx-font-size: 14px; -fx-background-color: #e67e22; -fx-text-fill: white; " +
                    "-fx-background-radius: 5px; -fx-cursor: hand;");
        
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-font-size: 14px; -fx-background-color: #d35400; " +
                "-fx-text-fill: white; -fx-background-radius: 5px; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-font-size: 14px; -fx-background-color: #e67e22; " +
                "-fx-text-fill: white; -fx-background-radius: 5px; -fx-cursor: hand;"));
        
        btn.setOnAction(e -> showControlPanel(name, showIndex));
        
        return btn;
    }
    
    /**
     * 显示控制面板
     */
    private void showControlPanel(String name, int showIndex) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(name);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // 获取该分栏的控制点
        List<SettingShow> showList = InitAppSetting.getSetting().getSettingShowList();
        Optional<SettingShow> settingShow = showList.stream()
                .filter(s -> s.getIndex() == showIndex)
                .findFirst();
        
        if (settingShow.isPresent()) {
            Node content = createControlGridForShow(settingShow.get());
            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().setPrefSize(900, 500);
        }
        
        dialog.showAndWait();
    }
    
    // ==================== 通用方法 ====================
    
    /**
     * 创建数据显示Tab
     */
    private Tab createDataTab(SettingShow show) {
        Tab tab = new Tab(getMessage(show.getIec()));
        tab.setId("tab_" + show.getIndex());
        
        Node content = createDataGridForShow(show);
        tab.setContent(content);
        
        return tab;
    }
    
    /**
     * 为指定分栏创建数据网格
     */
    private Node createDataGridForShow(SettingShow show) {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(5);
        gridPane.setPadding(new Insets(10));
        
        List<PointDefinition> points = dataService.getPointsByShowIndex(show.getIndex());
        
        int row = 0, col = 0;
        int maxCol = show.getColumn();
        
        for (PointDefinition point : points) {
            // 根据功能标签类型创建不同控件
            if (point.getFunctionLabel() != null) {
                switch (point.getFunctionLabel()) {
                    case 1: // 只读显示
                        addReadOnlyControl(gridPane, point, row, col);
                        col += 4;
                        break;
                    case 2: // 脉冲按钮
                    case 3: // 保持按钮
                    case 4: // 数据下发
                        // 只读视图中不显示控制类型
                        addReadOnlyControl(gridPane, point, row, col);
                        col += 4;
                        break;
                    case 5: // 按位显示 - 展开显示所有位
                        int unitsUsed = addBitStatusControlExpanded(gridPane, point, row, col, maxCol);
                        col += unitsUsed * 4;
                        while (col >= maxCol * 4) {
                            col -= maxCol * 4;
                            row++;
                        }
                        continue; // 跳过下面的通用换行逻辑
                    default:
                        addReadOnlyControl(gridPane, point, row, col);
                        col += 4;
                }
            }
            
            if (col >= maxCol * 4) {
                col = 0;
                row++;
            }
        }
        
        ScrollPane scrollPane = new ScrollPane(gridPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        return scrollPane;
    }
    
    /**
     * 为指定分栏创建控制网格
     */
    private Node createControlGridForShow(SettingShow show) {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(5);
        gridPane.setVgap(8);
        gridPane.setPadding(new Insets(10));
        
        int maxCol = Math.max(show.getColumn(), 3);
        
        // 设置列宽约束，确保对齐
        int totalCols = maxCol * 4; // 每个数据项占4列: 名称、值、单位、分隔符
        for (int i = 0; i < totalCols; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            if (i % 4 == 0) {
                // 名称列 - 固定宽度
                cc.setMinWidth(100);
                cc.setPrefWidth(120);
                cc.setMaxWidth(150);
            } else if (i % 4 == 1) {
                // 值/按钮列
                cc.setMinWidth(60);
                cc.setPrefWidth(80);
            } else if (i % 4 == 2) {
                // 单位列
                cc.setMinWidth(30);
                cc.setPrefWidth(50);
            } else {
                // 分隔符列
                cc.setMinWidth(15);
                cc.setPrefWidth(15);
                cc.setMaxWidth(15);
            }
            gridPane.getColumnConstraints().add(cc);
        }
        
        List<PointDefinition> points = dataService.getPointsByShowIndex(show.getIndex());
        
        int row = 0, col = 0;
        
        for (PointDefinition point : points) {
            if (point.getFunctionLabel() != null) {
                switch (point.getFunctionLabel()) {
                    case 1: // 只读显示
                        addReadOnlyControl(gridPane, point, row, col);
                        col += 4;
                        break;
                    case 2: // 脉冲按钮
                        addPulseButtonControl(gridPane, point, row, col);
                        col += 4;
                        break;
                    case 3: // 保持按钮
                        addToggleButtonControl(gridPane, point, row, col);
                        col += 4;
                        break;
                    case 4: // 数据下发
                        addDataInputControl(gridPane, point, row, col);
                        col += 4;
                        break;
                    case 5: // 按位显示 - 展开显示所有位
                        int unitsUsed = addBitStatusControlExpanded(gridPane, point, row, col, maxCol);
                        // 计算按位显示后的位置
                        col += unitsUsed * 4;
                        while (col >= maxCol * 4) {
                            col -= maxCol * 4;
                            row++;
                        }
                        continue; // 跳过下面的通用换行逻辑
                    default:
                        addReadOnlyControl(gridPane, point, row, col);
                        col += 4;
                }
            }
            
            if (col >= maxCol * 4) {
                col = 0;
                row++;
            }
        }
        
        ScrollPane scrollPane = new ScrollPane(gridPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        return scrollPane;
    }
    
    /**
     * 添加只读控件
     */
    private void addReadOnlyControl(GridPane grid, PointDefinition point, int row, int col) {
        String labelText = getMessage(point.getIec());
        Label nameLabel = new Label(labelText + ": ");
        nameLabel.setWrapText(true);
        nameLabel.setMinWidth(80);
        nameLabel.setMaxWidth(140);
        nameLabel.setTooltip(new Tooltip(labelText));
        
        Button valueBtn = new Button("--");
        valueBtn.setId("btn_" + point.getName());
        valueBtn.setMinWidth(60);
        valueBtn.setDisable(true);
        
        Label unitLabel = new Label(point.getUnit() != null ? point.getUnit() : "");
        unitLabel.setMinWidth(30);
        
        Label splitLabel = new Label("|");
        
        grid.add(nameLabel, col, row);
        grid.add(valueBtn, col + 1, row);
        grid.add(unitLabel, col + 2, row);
        grid.add(splitLabel, col + 3, row);
    }
    
    /**
     * 添加脉冲按钮控件
     */
    private void addPulseButtonControl(GridPane grid, PointDefinition point, int row, int col) {
        String labelText = getMessage(point.getIec());
        Button btn = new Button(labelText);
        btn.setMinWidth(100);
        btn.setMaxWidth(180);
        btn.setWrapText(true);
        btn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        btn.setTooltip(new Tooltip(labelText));
        
        btn.setOnAction(e -> {
            if (dataService.isConnected()) {
                dataService.sendCommandByAddress(point.getAddress(), 1);
            } else {
                showAlert("未建立连接！");
            }
        });
        
        Label splitLabel = new Label("|");
        
        grid.add(btn, col, row, 2, 1); // 按钮跨2列
        grid.add(splitLabel, col + 3, row);
    }
    
    /**
     * 添加保持按钮控件
     */
    private void addToggleButtonControl(GridPane grid, PointDefinition point, int row, int col) {
        String labelText = getMessage(point.getIec());
        ToggleButton btn = new ToggleButton(labelText);
        btn.setMinWidth(100);
        btn.setMaxWidth(140);
        btn.setWrapText(true);
        btn.setTooltip(new Tooltip(labelText));
        
        Label statusLabel = new Label("---");
        statusLabel.setMinWidth(40);
        
        btn.setOnAction(e -> {
            if (dataService.isConnected()) {
                int value = btn.isSelected() ? 1 : 0;
                dataService.sendCommandByAddress(point.getAddress(), value);
                statusLabel.setText(btn.isSelected() ? "-√-" : "---");
            } else {
                btn.setSelected(false);
                showAlert("未建立连接！");
            }
        });
        
        Label splitLabel = new Label("|");
        
        grid.add(btn, col, row);
        grid.add(statusLabel, col + 1, row);
        grid.add(splitLabel, col + 3, row);
    }
    
    /**
     * 添加数据输入控件
     */
    private void addDataInputControl(GridPane grid, PointDefinition point, int row, int col) {
        String labelText = getMessage(point.getIec());
        
        TextField inputField = new TextField();
        inputField.setPrefWidth(70);
        inputField.setMaxWidth(80);
        
        Button sendBtn = new Button(labelText);
        sendBtn.setMinWidth(80);
        sendBtn.setMaxWidth(120);
        sendBtn.setWrapText(true);
        sendBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white;");
        sendBtn.setTooltip(new Tooltip(labelText));
        
        sendBtn.setOnAction(e -> {
            if (dataService.isConnected()) {
                try {
                    int value = Integer.parseInt(inputField.getText());
                    dataService.sendCommandByAddress(point.getAddress(), value);
                } catch (NumberFormatException ex) {
                    showAlert("输入值无效！");
                }
            } else {
                showAlert("未建立连接！");
            }
        });
        
        Label unitLabel = new Label(point.getUnit() != null ? point.getUnit() : "");
        Label splitLabel = new Label("|");
        
        grid.add(sendBtn, col, row);
        grid.add(inputField, col + 1, row);
        grid.add(unitLabel, col + 2, row);
        grid.add(splitLabel, col + 3, row);
    }
    
    /**
     * 添加按位状态控件 - 显示单个位状态（只显示点位名称和状态）
     */
    private void addBitStatusControl(GridPane grid, PointDefinition point, int row, int col) {
        String labelText = getMessage(point.getIec());
        Label nameLabel = new Label(labelText + ": ");
        nameLabel.setWrapText(true);
        nameLabel.setMinWidth(80);
        nameLabel.setMaxWidth(140);
        nameLabel.setTooltip(new Tooltip(labelText));
        
        Button statusBtn = new Button("●");
        statusBtn.setId("btnBit_" + point.getAddress());
        statusBtn.setMinWidth(40);
        statusBtn.setStyle("-fx-text-fill: red;");
        
        Label splitLabel = new Label("|");
        
        grid.add(nameLabel, col, row);
        grid.add(statusBtn, col + 1, row);
        grid.add(splitLabel, col + 3, row);
    }
    
    /**
     * 添加按位状态控件 - 展开显示所有位（用于有枚举列表的情况）
     * @return 返回实际占用的"控件单元"数量（每单元4列）
     */
    private int addBitStatusControlExpanded(GridPane grid, PointDefinition point, int row, int col, int maxCol) {
        List<PointDefinition.EnumDefinition> enums = point.getEnumList();
        if (enums == null || enums.isEmpty()) {
            // 没有枚举列表，使用普通显示
            addBitStatusControl(grid, point, row, col);
            return 1;
        }
        
        int currentCol = col;
        int currentRow = row;
        int unitsUsed = 0;
        
        for (int i = 0; i < enums.size() && i < 16; i++) {
            PointDefinition.EnumDefinition enumDef = enums.get(i);
            String text = getMessage(enumDef.getIec());
            
            Label nameLabel = new Label(text);
            nameLabel.setWrapText(true);
            nameLabel.setMinWidth(80);
            nameLabel.setMaxWidth(140);
            nameLabel.setTooltip(new Tooltip(text));
            
            Button statusBtn = new Button("●");
            statusBtn.setId("btnBit_" + point.getAddress() + "_" + i);
            statusBtn.setMinWidth(40);
            statusBtn.setStyle("-fx-text-fill: red;");
            
            Label splitLabel = new Label("|");
            
            grid.add(nameLabel, currentCol, currentRow);
            grid.add(statusBtn, currentCol + 1, currentRow);
            grid.add(splitLabel, currentCol + 3, currentRow);
            
            currentCol += 4;
            unitsUsed++;
            
            // 如果到达行尾，换行
            if (currentCol >= maxCol * 4) {
                currentCol = 0;
                currentRow++;
            }
        }
        
        return unitsUsed;
    }
    
    /**
     * 显示提示框
     */
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 故障项数据模型
     */
    public static class FaultItem {
        private final javafx.beans.property.StringProperty name = new javafx.beans.property.SimpleStringProperty();
        private final javafx.beans.property.StringProperty value = new javafx.beans.property.SimpleStringProperty();
        private final javafx.beans.property.StringProperty status = new javafx.beans.property.SimpleStringProperty();
        
        public String getName() { return name.get(); }
        public void setName(String value) { name.set(value); }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        
        public String getValue() { return value.get(); }
        public void setValue(String v) { value.set(v); }
        public javafx.beans.property.StringProperty valueProperty() { return value; }
        
        public String getStatus() { return status.get(); }
        public void setStatus(String value) { status.set(value); }
        public javafx.beans.property.StringProperty statusProperty() { return status; }
    }
}
