package com.goldwind.javafxboot.adapter;

import lombok.Data;
import java.util.List;
import java.util.ArrayList;

/**
 * 点表定义 - 单个数据点的配置
 * 用于将协议地址映射到业务变量名
 * 
 * @author HMI Team
 * @date 2026-01-13
 */
@Data
public class PointDefinition {
    /**
     * 点名称 - 业务层使用的唯一标识
     * 例如：START_CMD, STOP_CMD, WIND_SPEED 等
     */
    private String name;
    
    /**
     * 点描述 - 用于显示
     */
    private String description;
    
    /**
     * 协议地址 - Modbus寄存器地址
     */
    private Integer address;
    
    /**
     * 分组索引 - 对应XE2000.xml中的group index
     */
    private Integer groupIndex;
    
    /**
     * 数据类型转换：1: 0~65535 ushort（P_AB）; 2: -32767~32768（PM_AB）
     */
    private Integer convert;
    
    /**
     * 显示类型：1-int, 2-double, 3-bool, 4-枚举, 5-位状态
     */
    private Integer dataType;
    
    /**
     * 单位
     */
    private String unit;
    
    /**
     * 倍率
     */
    private Integer multiple;
    
    /**
     * 功能标签：1-只读, 2-脉冲按钮, 3-保持按钮, 4-数据下发, 5-按位显示
     */
    private Integer functionLabel;
    
    /**
     * 是否可写
     */
    private Boolean writable;
    
    /**
     * 所属分类 - 用于界面分组
     * 如：MAIN（主界面）, PITCH（变桨）, CONVERTER（变流）, GRID（电网）等
     */
    private String category;
    
    /**
     * 界面显示分栏索引 - 对应setting.xml中的show index
     */
    private Integer showIndex;
    
    /**
     * 枚举值定义
     */
    private List<EnumDefinition> enumList = new ArrayList<>();
    
    /**
     * IEC标识 - 用于国际化
     */
    private String iec;
    
    /**
     * 枚举值定义内部类
     */
    @Data
    public static class EnumDefinition {
        private String state;
        private String iec;
        private String description;
    }
}
