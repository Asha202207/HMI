package com.goldwind.javafxboot.model;

import lombok.Data;

import java.util.List;

/**
 * @author xujianhua
 * @date 2024-04-22
 * @apiNote
 */
@Data
public class SettingGroup {
    private Integer index;
    private Integer start;
    private Integer end;
    private List<SettingLocation> settingLocationList;
}
