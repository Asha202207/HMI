package com.goldwind.javafxboot.service;

import com.goldwind.javafxboot.adapter.PointCategory;
import com.goldwind.javafxboot.adapter.PointDefinition;
import com.goldwind.javafxboot.adapter.PointTableAdapter;
import com.goldwind.javafxboot.protocol.modbus.domain.datatype.numeric.P_AB;
import com.goldwind.javafxboot.protocol.modbus.exceptiom.ModbusException;
import javafx.beans.property.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Modbus数据服务层 - 协议层与显示层解耦的核心
 * 
 * 设计目标：
 * 1. 提供统一的数据读写接口，界面层不需要知道协议细节
 * 2. 通过点名称访问数据，而不是硬编码的地址
 * 3. 支持数据变化监听，方便界面响应式更新
 * 4. 数据格式转换在这一层完成
 * 
 * @author HMI Team
 * @date 2026-01-13
 */
@Slf4j
public class ModbusDataService {
    
    private static volatile ModbusDataService instance;
    
    /**
     * 点表适配器
     */
    private final PointTableAdapter pointTableAdapter;
    
    /**
     * 调度服务引用 - 用于发送命令
     */
    private ThreadScheduledService scheduledService;
    
    /**
     * 数据缓存 - 存储最新的数据值（按点名称索引）
     */
    private final Map<String, Object> dataCache = new ConcurrentHashMap<>();
    
    /**
     * 数据缓存 - 存储最新的数据值（按地址索引，用于部件界面等直接按地址访问）
     */
    private final Map<Integer, Object> addressDataCache = new ConcurrentHashMap<>();
    
    /**
     * 数据属性 - 用于JavaFX数据绑定
     */
    private final Map<String, ObjectProperty<Object>> dataProperties = new ConcurrentHashMap<>();
    
    /**
     * 数据变化监听器
     */
    private final Map<String, List<Consumer<Object>>> dataListeners = new ConcurrentHashMap<>();
    
    /**
     * 连接状态
     */
    private final BooleanProperty connected = new SimpleBooleanProperty(false);
    
    private ModbusDataService() {
        this.pointTableAdapter = PointTableAdapter.getInstance();
    }
    
    /**
     * 获取单例实例
     */
    public static ModbusDataService getInstance() {
        if (instance == null) {
            synchronized (ModbusDataService.class) {
                if (instance == null) {
                    instance = new ModbusDataService();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化服务
     * 应在应用启动时调用
     */
    public void initialize() {
        // 初始化点表
        pointTableAdapter.initFromExistingConfig();
        log.info("ModbusDataService initialized with {} points", pointTableAdapter.getPointCount());
    }
    
    /**
     * 设置调度服务
     */
    public void setScheduledService(ThreadScheduledService service) {
        this.scheduledService = service;
    }
    
    /**
     * 获取调度服务
     */
    public ThreadScheduledService getScheduledService() {
        return this.scheduledService;
    }
    
    // ==================== 数据读取接口 ====================
    
    /**
     * 根据点名称获取数据值
     * 
     * @param pointName 点名称
     * @return 数据值，如果不存在返回null
     */
    public Object getValue(String pointName) {
        return dataCache.get(pointName);
    }
    
    /**
     * 根据点名称获取整数值
     */
    public Integer getIntValue(String pointName) {
        Object value = dataCache.get(pointName);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 根据点名称获取浮点值
     */
    public Double getDoubleValue(String pointName) {
        Object value = dataCache.get(pointName);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 根据点名称获取布尔值
     */
    public Boolean getBoolValue(String pointName) {
        Integer value = getIntValue(pointName);
        if (value == null) return null;
        return value >= 1;
    }
    
    /**
     * 根据点名称获取格式化后的显示字符串
     */
    public String getFormattedValue(String pointName) {
        PointDefinition point = pointTableAdapter.getPointByName(pointName);
        if (point == null) {
            return "--";
        }
        
        Object rawValue = dataCache.get(pointName);
        if (rawValue == null) {
            return "--";
        }
        
        return formatValue(rawValue, point);
    }
    
    /**
     * 格式化数据值
     */
    private String formatValue(Object rawValue, PointDefinition point) {
        try {
            int data = Integer.parseInt(rawValue.toString());
            Integer dataType = point.getDataType();
            Integer multiple = point.getMultiple();
            
            if (dataType == null) {
                return String.valueOf(data);
            }
            
            switch (dataType) {
                case 1: // int
                    if (multiple != null && multiple != 0) {
                        return String.valueOf((int) Math.ceil((double) data / multiple));
                    }
                    return String.valueOf(data);
                    
                case 2: // double
                    if (multiple != null && multiple != 0) {
                        return String.format("%.2f", (double) data / multiple);
                    }
                    return String.valueOf(data);
                    
                case 3: // bool
                    return data >= 1 ? "●" : "○";
                    
                case 4: // 枚举
                    String stateValue = String.valueOf(data);
                    if (multiple != null && multiple != 0) {
                        stateValue = String.valueOf((int) Math.ceil((double) data / multiple));
                    }
                    // 查找枚举描述
                    for (PointDefinition.EnumDefinition enumDef : point.getEnumList()) {
                        if (enumDef.getState().equals(stateValue)) {
                            return enumDef.getIec(); // 返回IEC标识，由调用方进行国际化
                        }
                    }
                    return "未知";
                    
                default:
                    return String.valueOf(data);
            }
        } catch (Exception e) {
            return "--";
        }
    }
    
    /**
     * 获取指定地址的数据
     * 直接从地址缓存获取，用于部件界面等按地址访问的场景
     */
    public Object getValueByAddress(Integer address) {
        return addressDataCache.get(address);
    }
    
    /**
     * 获取数据属性（用于JavaFX绑定）
     */
    public ObjectProperty<Object> getDataProperty(String pointName) {
        return dataProperties.computeIfAbsent(pointName, k -> new SimpleObjectProperty<>());
    }
    
    // ==================== 数据写入接口 ====================
    
    /**
     * 根据点名称发送命令
     * 
     * @param pointName 点名称
     * @param value 要发送的值
     * @return 是否发送成功
     */
    public boolean sendCommand(String pointName, int value) {
        PointDefinition point = pointTableAdapter.getPointByName(pointName);
        if (point == null) {
            log.warn("Point not found: {}", pointName);
            return false;
        }
        
        return sendCommandByAddress(point.getAddress(), value);
    }
    
    /**
     * 根据地址发送命令
     * 
     * @param address Modbus地址
     * @param value 要发送的值
     * @return 是否发送成功
     */
    public boolean sendCommandByAddress(int address, int value) {
        if (scheduledService == null || !scheduledService.isRunning()) {
            log.warn("Modbus service not running!");
            return false;
        }
        
        try {
            scheduledService.postAction(address, new P_AB().setValue(BigDecimal.valueOf(value)));
            log.info("Command sent: address={}, value={}", address, value);
            return true;
        } catch (ModbusException e) {
            log.error("Failed to send command: address={}, value={}", address, value, e);
            return false;
        }
    }
    
    /**
     * 发送启机命令
     */
    public boolean sendStartCommand() {
        Integer address = pointTableAdapter.getSystemControlAddress("CMD_START");
        return sendCommandByAddress(address, 1);
    }
    
    /**
     * 发送停机命令
     */
    public boolean sendStopCommand() {
        Integer address = pointTableAdapter.getSystemControlAddress("CMD_STOP");
        return sendCommandByAddress(address, 1);
    }
    
    /**
     * 发送复位命令
     */
    public boolean sendResetCommand() {
        Integer address = pointTableAdapter.getSystemControlAddress("CMD_RESET");
        return sendCommandByAddress(address, 1);
    }
    
    /**
     * 设置维护模式
     */
    public boolean setMaintainMode(boolean enable) {
        Integer address = pointTableAdapter.getSystemControlAddress("CMD_MAINTAIN");
        return sendCommandByAddress(address, enable ? 1 : 0);
    }
    
    /**
     * 设置定检模式
     */
    public boolean setExaminationMode(boolean enable) {
        Integer address = pointTableAdapter.getSystemControlAddress("CMD_EXAMINATION");
        return sendCommandByAddress(address, enable ? 1 : 0);
    }
    
    // ==================== 数据更新接口 ====================
    
    /**
     * 更新数据（由ThreadScheduledService调用）
     * 
     * @param readMapData 从Modbus读取的原始数据
     */
    public void updateData(Map<Integer, Object> readMapData) {
        if (readMapData == null || readMapData.isEmpty()) {
            return;
        }
        
        for (Map.Entry<Integer, Object> entry : readMapData.entrySet()) {
            Integer address = entry.getKey();
            Object value = entry.getValue();
            
            // 直接按地址存储到缓存（供部件界面等使用）
            addressDataCache.put(address, value);
            
            // 查找对应的点定义（可能有多个点使用同一地址但不同show）
            updatePointData(address, value);
        }
    }
    
    /**
     * 更新指定地址的数据
     */
    private void updatePointData(Integer address, Object value) {
        // 遍历所有点，找到匹配地址的点
        for (String pointName : pointTableAdapter.getAllPointNames()) {
            PointDefinition point = pointTableAdapter.getPointByName(pointName);
            if (point != null && address.equals(point.getAddress())) {
                // 更新缓存
                Object oldValue = dataCache.get(pointName);
                dataCache.put(pointName, value);
                
                // 更新属性
                ObjectProperty<Object> property = dataProperties.get(pointName);
                if (property != null) {
                    property.set(value);
                }
                
                // 通知监听器
                notifyListeners(pointName, value);
            }
        }
    }
    
    /**
     * 通知数据变化监听器
     */
    private void notifyListeners(String pointName, Object value) {
        List<Consumer<Object>> listeners = dataListeners.get(pointName);
        if (listeners != null) {
            for (Consumer<Object> listener : listeners) {
                try {
                    listener.accept(value);
                } catch (Exception e) {
                    log.error("Error in data listener for point: {}", pointName, e);
                }
            }
        }
    }
    
    // ==================== 监听器管理 ====================
    
    /**
     * 添加数据变化监听器
     */
    public void addDataListener(String pointName, Consumer<Object> listener) {
        dataListeners.computeIfAbsent(pointName, k -> new ArrayList<>()).add(listener);
    }
    
    /**
     * 移除数据变化监听器
     */
    public void removeDataListener(String pointName, Consumer<Object> listener) {
        List<Consumer<Object>> listeners = dataListeners.get(pointName);
        if (listeners != null) {
            listeners.remove(listener);
        }
    }
    
    // ==================== 连接状态 ====================
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return scheduledService != null && scheduledService.isRunning();
    }
    
    /**
     * 获取连接状态属性（用于JavaFX绑定）
     */
    public BooleanProperty connectedProperty() {
        return connected;
    }
    
    /**
     * 更新连接状态
     */
    public void setConnected(boolean value) {
        connected.set(value);
    }
    
    // ==================== 查询接口 ====================
    
    /**
     * 获取点表适配器
     */
    public PointTableAdapter getPointTableAdapter() {
        return pointTableAdapter;
    }
    
    /**
     * 获取指定分类的所有点
     */
    public List<PointDefinition> getPointsByCategory(PointCategory category) {
        return pointTableAdapter.getPointsByCategory(category);
    }
    
    /**
     * 获取指定显示分栏的所有点
     */
    public List<PointDefinition> getPointsByShowIndex(Integer showIndex) {
        return pointTableAdapter.getPointsByShowIndex(showIndex);
    }
    
    /**
     * 根据点名称获取点定义
     */
    public PointDefinition getPointDefinition(String pointName) {
        return pointTableAdapter.getPointByName(pointName);
    }
    
    /**
     * 清除所有数据缓存
     */
    public void clearCache() {
        dataCache.clear();
        addressDataCache.clear();
    }
}
