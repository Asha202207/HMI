# HMI 工程架构与控件扩展指南

以下内容基于当前工程源码整理，包含架构说明与添加/删除控件的实操方法（主状态、部件-变流器、控制-机舱控制）。

---

## 架构概览

- **启动入口**  
  `src/main/java/com/goldwind/javafxboot/Application.java`  
  负责启动 JavaFX + SpringBoot、初始化配置、窗口大小监听等。

- **配置与协议加载**  
  `src/main/java/com/goldwind/javafxboot/util/InitAppSetting.java`  
  - `setting.xml`：界面栏目、列数、语言等  
  - `protocol/XE2000.xml`：点表、地址、显示定义  

- **数据服务层**  
  `src/main/java/com/goldwind/javafxboot/service/ModbusDataService.java`  
  统一读/写 Modbus 数据，缓存最新数据；UI 通过它取值或发命令。

- **点表适配**  
  `src/main/java/com/goldwind/javafxboot/adapter/PointTableAdapter.java`  
  把 `XE2000.xml` 中 `<location>` 转成 `PointDefinition`，提供按 showIndex、分类、名称等的查询。

- **视图入口**  
  - FXML 壳：`src/main/resources/fxml/Main.fxml`  
  - 控制器：`src/main/java/com/goldwind/javafxboot/ctrl/MainCtrl.java`  
  - 视图工厂：`src/main/java/com/goldwind/javafxboot/view/ViewFactory.java`

- **导航切换**  
  `src/main/java/com/goldwind/javafxboot/view/NavigationManager.java`  
  负责主界面/部件/故障/登录/控制页的切换与缓存。

---

## UI 生成逻辑（关键理解）

- **主界面 Tab（主状态等）**  
  由 `MainCtrl.initTabPaneControl()` 动态生成  
  - 从 `setting.xml` 的 `<show>` 列表读取栏目  
  - 再从 `XE2000.xml` 找 showIndex 对应的 `<location>`  
  - 根据 `fnc_lab` 决定控件类型

- **部件页（组件网格/详情页）**  
  由 `ViewFactory.createComponentView()` 生成  
  - 组件按钮 -> showIndex（例如变流器=5）  
  - 详情页只显示 `fnc_lab=1 或 5` 的点

- **控制页**  
  由 `ViewFactory.createControlView()` 生成  
  - “机舱控制”等按钮 -> showIndex（例如机舱控制=19）  
  - 控制弹窗显示 `fnc_lab=1/2/3/4/5` 不同控件

---

# 三类常见需求

## 1) 主状态里添加/删除显示原件

**逻辑入口**  
- `MainCtrl.initTabPaneControl()`  
- `MainCtrl.isMainPageShow()`（决定哪些 showIndex 参与“主界面”）

**实际修改点**  
1. 在 `XE2000.xml` 添加（或删除）一个 `<location>`，并设置：  
   - `show="1"`（主状态栏目，showIndex=1）  
   - `fnc_lab` 控制控件类型  
2. 在 `messages_zh_CN.properties` 里添加对应 `iec` 的中文文本  
3. 调整列数可改 `setting.xml` 中 `<show index="1" column="..."/>`

**注意**  
主界面目前只显示 showIndex = 1 / 3 / 16（见 `MainCtrl.isMainPageShow()`）。

---

## 2) 部件中“变流器页面”添加显示器件

**逻辑入口**  
- 组件按钮列表：`ViewFactory.createComponentListView()`  
  - “变流器” 对应 showIndex = 5  
- 详情显示逻辑：`ViewFactory.createComponentDataGrid()`  
  - 只显示 `fnc_lab=1 或 5`

**实际修改点**  
1. 在 `XE2000.xml` 添加 `<location ... show="5" ...>`  
2. 在 `messages_zh_CN.properties` 添加对应 `iec` 文案  
3. 如需调整列数，改 `setting.xml` 中 `<show index="5" column="..."/>`

**注意**  
组件详情页不会展示 `fnc_lab=2/3/4`，如需显示需改 `createComponentDataGrid()` 过滤逻辑。

---

## 3) 控制中“机舱控制”添加按钮

**逻辑入口**  
- 控制按钮入口：`ViewFactory.createControlView()`  
  - “机舱控制” showIndex = 19  
- 控制弹窗：`ViewFactory.createControlGridForShow()`

**实际修改点**  
1. 在 `XE2000.xml` 添加 `<location ... show="19" ...>`  
2. 选择合适的 `fnc_lab` 类型（见速查表）  
3. 补充 `messages_zh_CN.properties`  
4. 调整列数可改 `setting.xml` 中 `<show index="19" column="..."/>`

---

## fnc_lab 控件类型速查

- `1` 只读显示：Label + 数值 + 单位  
- `2` 脉冲按钮：点击发送 1  
- `3` 保持按钮：Toggle，发送 1/0  
- `4` 输入框 + 按钮：发送输入值  
- `5` 位状态：按位显示（需要 `<enum>` 列表）

---

## 常用修改路径清单

- 点表/显示定义：`src/main/resources/protocol/XE2000.xml`  
- 显示栏目/列数：`src/main/resources/config/setting.xml`  
- 文案国际化：`src/main/resources/language/messages_zh_CN.properties`  
- 主界面生成逻辑：`src/main/java/com/goldwind/javafxboot/ctrl/MainCtrl.java`  
- 部件/控制页生成逻辑：`src/main/java/com/goldwind/javafxboot/view/ViewFactory.java`

---

## 小提醒（避免踩坑）

- 主界面与组件页使用的是不同的生成逻辑：  
  - 主界面：`MainCtrl` 直接用 `SettingLocation`  
  - 组件/控制：`ViewFactory` + `ModbusDataService`

- 新增 `<location>` 必须包含：  
  - `show`（决定归属页）  
  - `iec`（决定显示文本）  
  - `fnc_lab`（决定控件类型）  
  - `type/multiple/unit`（决定显示格式）
