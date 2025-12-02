package com.goldwind.javafxboot.model;

import lombok.Data;

import java.util.List;

/**
 * @author xujianhua
 * @date 2024-04-22
 * @apiNote
 */
@Data
public class AppSetting {
    private SettingServer settingServer;
    private SettingEnvironment settingEnvironment;
    private List<SettingShow> settingShowList;
    private List<SettingGroup> settingGroupList;
}
