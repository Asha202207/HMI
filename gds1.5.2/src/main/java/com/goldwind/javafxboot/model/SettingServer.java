package com.goldwind.javafxboot.model;

import lombok.Data;

/**
 * @author xujianhua
 * @date 2024-04-22
 * @apiNote
 */
@Data
public class SettingServer {
    private String ip;
    private Integer slaveId;
    private Integer port;
    private Integer cycle;
}
