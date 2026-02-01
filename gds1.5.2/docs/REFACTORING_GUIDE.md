# 风机塔基控制柜HMI界面优化说明

> 文档版本：1.0  
> 更新日期：2026-01-13  
> 作者：HMI Team

---

## 目录

- [一、优化概述](#一优化概述)
- [二、界面布局优化](#二界面布局优化)
- [三、协议层与显示层解耦](#三协议层与显示层解耦)
- [四、新增文件说明](#四新增文件说明)
- [五、点表适配指南](#五点表适配指南)
- [六、兼容性说明](#六兼容性说明)

---

## 一、优化概述

本次优化主要完成两项工作：

1. **界面布局优化**：重新设计界面结构，底部增加5个导航按钮（主界面、部件、故障、登录、控制），上方增加4个操作按钮（启机、停机、复位、维护）

2. **协议层与显示层解耦**：保持现有Modbus-TCP协议不变，通过引入数据服务层和点表适配器，实现显示层与协议层的完全解耦

---

## 二、界面布局优化

### 2.1 新的界面结构

```
┌──────────────────────────────────────────────────────┐
│  LOGO    │    2MW机组塔底柜控制面板      │  时间显示  │  ← 标题栏
├──────────────────────────────────────────────────────┤
│                                                      │
│                   主内容区域                          │  ← 根据导航动态切换
│               (TabPane/各功能界面)                    │
│                                                      │
├──────────────────────────────────────────────────────┤
│    [启机]    [停机]    [复位]    [维护]              │  ← 操作按钮区
├──────────────────────────────────────────────────────┤
│  ▶  │         状态/日志显示区域                      │  ← 状态栏
├──────────────────────────────────────────────────────┤
│  主界面  │  部件  │  故障  │  登录  │  控制          │  ← 底部导航栏
└──────────────────────────────────────────────────────┘
```

### 2.2 导航功能说明

| 导航按钮 | 功能说明 |
|---------|---------|
| 主界面 | 显示核心状态信息（主状态、系统信息、故障），不包含部件详情 |
| 部件 | 各部件监控入口，点击按钮可查看实时数据（变流、变桨、测风、偏航、传动链、电网、液压系统、冷却系统、发电机、机舱、塔基、振动监测、统计） |
| 故障 | 故障信息显示界面 |
| 登录 | 用户登录/登出界面 |
| 控制 | 各控制功能入口（水冷/变流控制、机舱控制、变桨控制、功率控制、偏航控制等） |

### 2.3 主界面与部件界面分工

为避免内容重复，界面分工如下：

| 界面 | 显示内容 | showIndex |
|------|---------|-----------|
| 主界面 | 主状态、系统信息、故障 | 1, 3, 16 |
| 部件界面 | 变桨、变流、测风、偏航、传动链、电网、液压、冷却、发电机、机舱、塔基、振动、统计 | 4,5,6,7,8,9,10,11,12,13,14,15,17 |

### 2.3 操作按钮说明

| 操作按钮 | 功能说明 | 按钮类型 |
|---------|---------|---------|
| 启机 | 发送启机命令 | 脉冲按钮（点击触发） |
| 停机 | 发送停机命令 | 脉冲按钮（点击触发） |
| 复位 | 发送复位命令 | 脉冲按钮（点击触发） |
| 维护 | 进入/退出维护模式 | 保持按钮（切换状态） |

---

## 三、协议层与显示层解耦

### 3.1 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                      显示层 (View)                          │
│  Main.fxml → MainCtrl.java → 各子界面(部件/控制/故障等)      │
└─────────────────────────────────────────────────────────────┘
                              ↓ 调用
┌─────────────────────────────────────────────────────────────┐
│                   数据服务层 (DataService)                   │
│  ModbusDataService - 提供统一的数据读写接口                  │
│  - getValue(String pointName)                               │
│  - sendCommand(String pointName, int value)                 │
│  - sendStartCommand() / sendStopCommand() / ...             │
└─────────────────────────────────────────────────────────────┘
                              ↓ 使用
┌─────────────────────────────────────────────────────────────┐
│                   点表适配层 (PointAdapter)                  │
│  PointTableAdapter - 点表映射管理                            │
│  PointDefinition - 点定义模型                                │
│  pointtable.xml - 可配置的点表定义                           │
└─────────────────────────────────────────────────────────────┘
                              ↓ 调用
┌─────────────────────────────────────────────────────────────┐
│                    协议层 (Protocol)                        │
│  ThreadScheduledService → ModbusTcpMasterBuilder            │
│  ★ 保持现有modbus-tcp实现不变 ★                              │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 解耦前后对比

#### 控制命令发送

**改动前（硬编码地址）**：
```java
// 启动按钮 - 地址102直接硬编码
threadScheduledService.postAction(
    Integer.parseInt("102"),
    new P_AB().setValue(BigDecimal.valueOf(Integer.parseInt("1"))));

// 停止按钮 - 地址104直接硬编码
threadScheduledService.postAction(
    Integer.parseInt("104"),
    new P_AB().setValue(BigDecimal.valueOf(Integer.parseInt("1"))));
```

**改动后（通过数据服务）**：
```java
// 启动按钮 - 通过数据服务发送命令
dataService.sendStartCommand();

// 停止按钮 - 通过数据服务发送命令  
dataService.sendStopCommand();

// 或者通过点名称发送
dataService.sendCommand("CMD_START", 1);
```

#### 数据读取

**改动前**：
```java
// 直接通过地址获取数据
Object value = readMapData.get(settingLocation.getAddress());
```

**改动后**：
```java
// 通过点名称获取数据
Object value = dataService.getValue("WIND_SPEED");
Integer intValue = dataService.getIntValue("ACTIVE_POWER");
String formatted = dataService.getFormattedValue("UNIT_STATUS");
```

---

## 四、新增文件说明

### 4.1 适配器层

| 文件 | 路径 | 说明 |
|------|------|------|
| `PointDefinition.java` | `adapter/` | 点定义模型类，包含点名称、地址、类型、单位等属性 |
| `PointCategory.java` | `adapter/` | 点分类枚举，定义各功能模块分类 |
| `PointTableAdapter.java` | `adapter/` | 点表适配器，统一管理点表配置 |

### 4.2 服务层

| 文件 | 路径 | 说明 |
|------|------|------|
| `ModbusDataService.java` | `service/` | 数据服务层，提供统一的数据读写接口 |

### 4.3 视图层

| 文件 | 路径 | 说明 |
|------|------|------|
| `NavigationManager.java` | `view/` | 导航管理器，管理页面切换 |
| `ViewFactory.java` | `view/` | 视图工厂，创建各功能界面 |

### 4.4 配置文件

| 文件 | 路径 | 说明 |
|------|------|------|
| `pointtable_template.xml` | `config/` | 点表配置模板，用于适配新点表 |

### 4.5 更新的文件

| 文件 | 说明 |
|------|------|
| `Main.fxml` | 更新界面布局，增加导航栏和操作按钮 |
| `MainCtrl.java` | 重构控制器，集成新架构 |
| `Main.css` | 更新样式，支持新的界面元素 |

---

## 五、点表适配指南

### 5.1 适配新风机点表的步骤

当需要适配不同厂家的风机时，只需要以下步骤：

#### 步骤1：复制点表模板
```bash
cp config/pointtable_template.xml config/pointtable_XXX.xml
```

#### 步骤2：修改点表配置

打开新的点表文件，修改各点的地址：

```xml
<pointtable version="1.0" protocol="XXX">
    <!-- 系统控制命令 -->
    <category name="system_control" displayName="系统控制">
        <point name="CMD_START" address="新地址" ... />
        <point name="CMD_STOP" address="新地址" ... />
        ...
    </category>
    
    <!-- 主状态信息 -->
    <category name="main" displayName="主状态">
        <point name="UNIT_STATUS" address="新地址" ... />
        <point name="ACTIVE_POWER" address="新地址" ... />
        ...
    </category>
</pointtable>
```

#### 步骤3：更新setting.xml

在 `setting.xml` 中指定使用新的协议点表：

```xml
<environment language="cn" protocol="XXX"/>
```

#### 步骤4：更新XE2000.xml（如需要）

如果新风机的点表结构差异较大，需要同步更新 `protocol/XXX.xml` 文件。

### 5.2 点表配置属性说明

| 属性 | 说明 | 示例 |
|------|------|------|
| `name` | 点名称（业务层标识） | `CMD_START`, `WIND_SPEED` |
| `address` | Modbus寄存器地址 | `102`, `100` |
| `group` | 分组索引 | `1`, `2`, `3` |
| `category` | 分类 | `main`, `pitch`, `converter` |
| `dataType` | 数据类型 | `1`-int, `2`-double, `3`-bool, `4`-enum |
| `convert` | 数据转换 | `1`-ushort, `2`-short |
| `unit` | 单位 | `kW`, `°C`, `m/s` |
| `multiple` | 倍率 | `1`, `10`, `100` |
| `showIndex` | 显示分栏索引 | `1`, `4`, `8` |
| `fnc` | 功能类型 | `1`-只读, `2`-脉冲, `3`-保持, `4`-数据下发 |
| `iec` | 国际化标识 | `iec100`, `iec121` |

### 5.3 系统控制命令地址配置

系统控制命令的地址在 `PointTableAdapter.java` 中初始化：

```java
private void initSystemControlAddresses() {
    systemControlAddresses.put("CMD_START", 102);      // 启机命令地址
    systemControlAddresses.put("CMD_STOP", 104);       // 停机命令地址
    systemControlAddresses.put("CMD_RESET", 103);      // 复位命令地址
    systemControlAddresses.put("CMD_MAINTAIN", 105);   // 维护模式地址
    systemControlAddresses.put("CMD_EXAMINATION", 106);// 定检模式地址
}
```

如需修改，直接修改此处的地址映射即可。

---

## 六、兼容性说明

### 6.1 保持不变的内容

| 内容 | 说明 |
|------|------|
| Modbus-TCP协议层 | `protocol/modbus/` 目录下的所有文件保持不变 |
| 点表配置文件 | `XE2000.xml` 继续使用 |
| 国际化文件 | `messages_zh_CN.properties` 继续使用 |
| 系统配置文件 | `setting.xml` 结构不变 |
| ThreadScheduledService | 调度服务保持原有逻辑 |

### 6.2 向后兼容

- 原有的TabPane数据显示功能完全保留
- 原有的按钮功能（fnc_lab 1-5）完全兼容
- 原有的数据格式化逻辑（type 1-4）保持不变

### 6.3 升级注意事项

1. 确保新增的Java文件已正确添加到项目中
2. 确保FXML和CSS文件已更新
3. 首次运行时会自动从现有配置初始化点表

---

## 附录

### A. 分类与显示分栏对应关系

| 分类代码 | 显示名称 | showIndex |
|---------|---------|-----------|
| `main` | 主界面 | 1 |
| `login` | 登录 | 2 |
| `system_info` | 系统信息 | 3 |
| `pitch` | 变桨系统 | 4 |
| `converter` | 变流器 | 5 |
| `vibration` | 振动监测 | 6 |
| `yaw_wind` | 测风/偏航 | 7 |
| `grid` | 电网 | 8 |
| `yaw` | 偏航 | 9 |
| `generator` | 发电机 | 10 |
| `drive_train` | 传动链 | 11 |
| `hydraulic` | 液压系统 | 12 |
| `nacelle` | 机舱 | 13 |
| `cooling` | 冷却系统 | 14 |
| `tower_base` | 塔基 | 15 |
| `fault` | 故障 | 16 |
| `statistics` | 统计 | 17 |
| `water_cooling_ctrl` | 水冷/变流控制 | 18 |
| `nacelle_ctrl` | 机舱控制 | 19 |
| `pitch_ctrl` | 变桨控制 | 20 |
| `power_ctrl` | 功率控制 | 21 |
| `fault_file` | 故障生产文件 | 22 |
| `yaw_ctrl` | 偏航控制 | 23 |

### B. 文件目录结构

```
src/main/java/com/goldwind/javafxboot/
├── adapter/                    # 【新增】适配器层
│   ├── PointDefinition.java   # 点定义模型
│   ├── PointCategory.java     # 点分类枚举
│   └── PointTableAdapter.java # 点表适配器
├── ctrl/
│   └── MainCtrl.java          # 【更新】主控制器
├── service/
│   ├── ModbusDataService.java # 【新增】数据服务层
│   └── ThreadScheduledService.java
├── view/                       # 【新增】视图层
│   ├── NavigationManager.java # 导航管理器
│   └── ViewFactory.java       # 视图工厂
└── ...

src/main/resources/
├── config/
│   ├── setting.xml
│   └── pointtable_template.xml # 【新增】点表模板
├── css/
│   └── Main.css               # 【更新】样式表
├── fxml/
│   └── Main.fxml              # 【更新】界面布局
└── ...
```

---

*文档结束*
