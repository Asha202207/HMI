package com.goldwind.javafxboot.adapter;

/**
 * 点表分类枚举 - 用于界面模块划分
 * 
 * @author HMI Team
 * @date 2026-01-13
 */
public enum PointCategory {
    /**
     * 主界面 - 核心状态信息
     */
    MAIN("main", "主界面", 1),
    
    /**
     * 系统控制 - 启停/复位/维护等
     */
    SYSTEM_CONTROL("system_control", "系统控制", 0),
    
    /**
     * 变桨系统
     */
    PITCH("pitch", "变桨系统", 4),
    
    /**
     * 变流器
     */
    CONVERTER("converter", "变流器", 5),
    
    /**
     * 测风/偏航系统
     */
    YAW_WIND("yaw_wind", "测风/偏航", 7),
    
    /**
     * 传动链
     */
    DRIVE_TRAIN("drive_train", "传动链", 11),
    
    /**
     * 电网
     */
    GRID("grid", "电网", 8),
    
    /**
     * 液压系统
     */
    HYDRAULIC("hydraulic", "液压系统", 12),
    
    /**
     * 冷却系统
     */
    COOLING("cooling", "冷却系统", 14),
    
    /**
     * 振动监测
     */
    VIBRATION("vibration", "振动监测", 6),
    
    /**
     * 发电机
     */
    GENERATOR("generator", "发电机", 10),
    
    /**
     * 机舱
     */
    NACELLE("nacelle", "机舱", 13),
    
    /**
     * 塔基
     */
    TOWER_BASE("tower_base", "塔基", 15),
    
    /**
     * 故障信息
     */
    FAULT("fault", "故障", 16),
    
    /**
     * 统计信息
     */
    STATISTICS("statistics", "统计", 17),
    
    /**
     * 系统信息
     */
    SYSTEM_INFO("system_info", "系统信息", 3),
    
    /**
     * 水冷/变流控制
     */
    WATER_COOLING_CTRL("water_cooling_ctrl", "水冷/变流控制", 18),
    
    /**
     * 机舱控制
     */
    NACELLE_CTRL("nacelle_ctrl", "机舱控制", 19),
    
    /**
     * 变桨控制
     */
    PITCH_CTRL("pitch_ctrl", "变桨控制", 20),
    
    /**
     * 功率控制
     */
    POWER_CTRL("power_ctrl", "功率控制", 21),
    
    /**
     * 故障生产文件
     */
    FAULT_FILE("fault_file", "故障生产文件", 22),
    
    /**
     * 偏航控制
     */
    YAW_CTRL("yaw_ctrl", "偏航控制", 23),
    
    /**
     * 登录
     */
    LOGIN("login", "登录", 2);
    
    private final String code;
    private final String displayName;
    private final Integer showIndex;
    
    PointCategory(String code, String displayName, Integer showIndex) {
        this.code = code;
        this.displayName = displayName;
        this.showIndex = showIndex;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public Integer getShowIndex() {
        return showIndex;
    }
    
    /**
     * 根据showIndex获取分类
     */
    public static PointCategory getByShowIndex(Integer showIndex) {
        for (PointCategory category : values()) {
            if (category.getShowIndex().equals(showIndex)) {
                return category;
            }
        }
        return MAIN;
    }
    
    /**
     * 根据code获取分类
     */
    public static PointCategory getByCode(String code) {
        for (PointCategory category : values()) {
            if (category.getCode().equals(code)) {
                return category;
            }
        }
        return MAIN;
    }
}
