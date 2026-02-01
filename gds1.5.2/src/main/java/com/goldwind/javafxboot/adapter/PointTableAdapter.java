package com.goldwind.javafxboot.adapter;

import com.goldwind.javafxboot.model.SettingGroup;
import com.goldwind.javafxboot.model.SettingLocation;
import com.goldwind.javafxboot.model.SettingEnum;
import com.goldwind.javafxboot.util.InitAppSetting;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 点表适配器 - 负责点表配置的加载和管理
 * 将协议点表配置转换为业务层使用的PointDefinition
 * 
 * 设计目标：
 * 1. 统一点表管理，方便适配不同厂家风机
 * 2. 提供按名称、地址、分类等多种方式访问点定义
 * 3. 与协议层解耦，界面只需要知道点名称
 * 
 * @author HMI Team
 * @date 2026-01-13
 */
@Slf4j
public class PointTableAdapter {
    
    private static volatile PointTableAdapter instance;
    
    /**
     * 按点名称索引的点定义映射
     */
    private final Map<String, PointDefinition> pointByName = new ConcurrentHashMap<>();
    
    /**
     * 按地址索引的点定义映射 (key: groupIndex_address)
     */
    private final Map<String, PointDefinition> pointByAddress = new ConcurrentHashMap<>();
    
    /**
     * 按分类索引的点定义列表
     */
    private final Map<PointCategory, List<PointDefinition>> pointByCategory = new ConcurrentHashMap<>();
    
    /**
     * 按显示分栏索引的点定义列表
     */
    private final Map<Integer, List<PointDefinition>> pointByShowIndex = new ConcurrentHashMap<>();
    
    /**
     * 系统控制命令点定义（硬编码的控制地址映射）
     */
    private final Map<String, Integer> systemControlAddresses = new HashMap<>();
    
    private PointTableAdapter() {
        // 初始化系统控制命令地址映射
        initSystemControlAddresses();
    }
    
    /**
     * 获取单例实例
     */
    public static PointTableAdapter getInstance() {
        if (instance == null) {
            synchronized (PointTableAdapter.class) {
                if (instance == null) {
                    instance = new PointTableAdapter();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化系统控制命令的地址映射
     * 这些是硬编码在MainCtrl中的控制地址，现在统一管理
     */
    private void initSystemControlAddresses() {
        // 启机命令地址
        systemControlAddresses.put("CMD_START", 102);
        // 停机命令地址
        systemControlAddresses.put("CMD_STOP", 104);
        // 复位命令地址
        systemControlAddresses.put("CMD_RESET", 103);
        // 维护模式命令地址
        systemControlAddresses.put("CMD_MAINTAIN", 105);
        // 定检模式命令地址
        systemControlAddresses.put("CMD_EXAMINATION", 106);
    }
    
    /**
     * 从现有配置初始化点表
     * 将XE2000.xml中的配置转换为PointDefinition
     */
    public void initFromExistingConfig() {
        try {
            List<SettingGroup> groups = InitAppSetting.getSetting().getSettingGroupList();
            if (groups == null || groups.isEmpty()) {
                log.warn("No setting groups found!");
                return;
            }
            
            for (SettingGroup group : groups) {
                for (SettingLocation location : group.getSettingLocationList()) {
                    PointDefinition point = convertToPointDefinition(group.getIndex(), location);
                    registerPoint(point);
                }
            }
            
            // 注册系统控制点
            registerSystemControlPoints();
            
            log.info("Point table initialized with {} points", pointByName.size());
            
        } catch (Exception e) {
            log.error("Failed to initialize point table", e);
        }
    }
    
    /**
     * 注册系统控制点定义
     */
    private void registerSystemControlPoints() {
        for (Map.Entry<String, Integer> entry : systemControlAddresses.entrySet()) {
            PointDefinition point = new PointDefinition();
            point.setName(entry.getKey());
            point.setAddress(entry.getValue());
            point.setGroupIndex(5); // 控制命令在group 5
            point.setCategory(PointCategory.SYSTEM_CONTROL.getCode());
            point.setWritable(true);
            point.setFunctionLabel(2); // 脉冲按钮
            point.setConvert(1);
            point.setDataType(1);
            
            // 设置描述
            switch (entry.getKey()) {
                case "CMD_START":
                    point.setDescription("启机命令");
                    break;
                case "CMD_STOP":
                    point.setDescription("停机命令");
                    break;
                case "CMD_RESET":
                    point.setDescription("复位命令");
                    break;
                case "CMD_MAINTAIN":
                    point.setDescription("维护模式");
                    point.setFunctionLabel(3); // 保持按钮
                    break;
                case "CMD_EXAMINATION":
                    point.setDescription("定检模式");
                    point.setFunctionLabel(3); // 保持按钮
                    break;
            }
            
            registerPoint(point);
        }
    }
    
    /**
     * 将SettingLocation转换为PointDefinition
     */
    private PointDefinition convertToPointDefinition(Integer groupIndex, SettingLocation location) {
        PointDefinition point = new PointDefinition();
        
        // 生成唯一点名称: IEC标识 + 地址
        String pointName = generatePointName(location.getIec(), location.getAddress());
        point.setName(pointName);
        
        point.setAddress(location.getAddress());
        point.setGroupIndex(groupIndex);
        point.setConvert(location.getConvert());
        point.setDataType(location.getType());
        point.setUnit(location.getUnit());
        point.setMultiple(location.getMultiple());
        point.setShowIndex(location.getShow());
        point.setIec(location.getIec());
        point.setFunctionLabel(location.getFnc_lab());
        
        // 设置分类
        PointCategory category = PointCategory.getByShowIndex(location.getShow());
        point.setCategory(category.getCode());
        
        // 设置是否可写
        point.setWritable(location.getFnc_lab() != null && location.getFnc_lab() > 1);
        
        // 转换枚举定义
        if (location.getSettingEnumList() != null) {
            for (SettingEnum settingEnum : location.getSettingEnumList()) {
                PointDefinition.EnumDefinition enumDef = new PointDefinition.EnumDefinition();
                enumDef.setState(settingEnum.getState());
                enumDef.setIec(settingEnum.getIec());
                point.getEnumList().add(enumDef);
            }
        }
        
        return point;
    }
    
    /**
     * 生成点名称
     */
    private String generatePointName(String iec, Integer address) {
        if (iec != null && !iec.isEmpty()) {
            return iec.toUpperCase().replace(".", "_") + "_" + address;
        }
        return "POINT_" + address;
    }
    
    /**
     * 注册点定义
     */
    private void registerPoint(PointDefinition point) {
        // 按名称索引
        pointByName.put(point.getName(), point);
        
        // 按地址索引
        String addressKey = point.getGroupIndex() + "_" + point.getAddress();
        pointByAddress.put(addressKey, point);
        
        // 按分类索引
        PointCategory category = PointCategory.getByCode(point.getCategory());
        pointByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(point);
        
        // 按显示分栏索引
        if (point.getShowIndex() != null) {
            pointByShowIndex.computeIfAbsent(point.getShowIndex(), k -> new ArrayList<>()).add(point);
        }
    }
    
    // ==================== 查询方法 ====================
    
    /**
     * 根据点名称获取点定义
     */
    public PointDefinition getPointByName(String name) {
        return pointByName.get(name);
    }
    
    /**
     * 根据地址获取点定义
     */
    public PointDefinition getPointByAddress(Integer groupIndex, Integer address) {
        String key = groupIndex + "_" + address;
        return pointByAddress.get(key);
    }
    
    /**
     * 获取指定分类的所有点
     */
    public List<PointDefinition> getPointsByCategory(PointCategory category) {
        return pointByCategory.getOrDefault(category, Collections.emptyList());
    }
    
    /**
     * 获取指定显示分栏的所有点
     */
    public List<PointDefinition> getPointsByShowIndex(Integer showIndex) {
        return pointByShowIndex.getOrDefault(showIndex, Collections.emptyList());
    }
    
    /**
     * 获取所有可写点
     */
    public List<PointDefinition> getWritablePoints() {
        return pointByName.values().stream()
                .filter(p -> Boolean.TRUE.equals(p.getWritable()))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取系统控制命令地址
     */
    public Integer getSystemControlAddress(String commandName) {
        return systemControlAddresses.get(commandName);
    }
    
    /**
     * 获取所有点名称
     */
    public Set<String> getAllPointNames() {
        return pointByName.keySet();
    }
    
    /**
     * 获取点总数
     */
    public int getPointCount() {
        return pointByName.size();
    }
}
