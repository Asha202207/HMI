package com.goldwind.javafxboot.model;

import lombok.Data;

import java.util.List;

/**
 * @author xujianhua
 * @date 2024-04-22
 * @apiNote
 */
@Data
public class SettingLocation {
    private Integer address;
    private String unit;
    private Integer convert;
    private Integer type;
    private Integer multiple;
    private Integer show;
    private String iec;
    private Integer fnc_lab;
    private List<SettingEnum> settingEnumList;
}
