package com.goldwind.javafxboot.service;

import com.alibaba.fastjson.JSON;
import com.goldwind.javafxboot.model.SettingGroup;
import com.goldwind.javafxboot.model.SettingLocation;
import com.goldwind.javafxboot.protocol.modbus.domain.FunctionCode;
import com.goldwind.javafxboot.protocol.modbus.domain.Obj4RequestRegister;
import com.goldwind.javafxboot.protocol.modbus.domain.datatype.*;
import com.goldwind.javafxboot.protocol.modbus.domain.synchronouswaitingroom.TcpSynchronousWaitingRoom;
import com.goldwind.javafxboot.protocol.modbus.exceptiom.ModbusException;
import com.goldwind.javafxboot.protocol.modbus.netty.ModbusTcpMasterBuilder;
import com.goldwind.javafxboot.protocol.modbus.utils.ModbusCommandDataUtils;
import com.goldwind.javafxboot.protocol.modbus.utils.ModbusRequestDataUtils;
import com.goldwind.javafxboot.util.InitAppSetting;
import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * @author xujianhua
 * @date 2024-04-18
 * @apiNote ScheduledService是一个可以自动重新开始的任务执行器。
 * ScheduledService在fails失败时是否会自动重新开始以及自动执行的次数取决于：
 * restartOnFailure,backoffStrategy,cumulativePeriod，maximumFailureCount几个property
 */
@Slf4j
public class ThreadScheduledService extends ScheduledService<Object> {
    /**
     * 服务名称
     */
    private final String name;
    private ModbusTcpMasterBuilder master;
    private final Map<Integer, List<Obj4RequestRegister>> groupRequestRegisterMap = new HashMap<>();
    private Map<Integer, Object> collectionData = new HashMap<>();

    /**
     * 构造函数
     *
     * @param name 服务名称
     */
    public ThreadScheduledService(String name) {
        this.name = name;
    }

    @Override
    protected Task<Object> createTask() {
        return new Task<Object>() {
            @Override
            protected Map<Integer, Object> call() {
                if (master == null) {
                    master = new ModbusTcpMasterBuilder(InitAppSetting.getSetting().getSettingServer().getIp()
                            , InitAppSetting.getSetting().getSettingServer().getPort());
                    master.createByUnBlock();
                    TcpSynchronousWaitingRoom.waitTime = 5000L;
                }

                if (groupRequestRegisterMap.isEmpty()) {
                    initGroupRequestRegisterMap();
                }

                if (master.isConnected()) {
                    readModbusData();
                }
                return collectionData;
            }
        };
    }

    @Override
    protected void succeeded() {
        super.succeeded();
        log.info(this + " is done!");
    }

    @Override
    protected void cancelled() {
        super.cancelled();
        log.info(this + " is cancelled!");
    }

    @Override
    protected void failed() {
        super.failed();
        log.info(this + " is failed!");
    }

    @Override
    protected void running() {
        super.running();
        log.info(this + " is running!");
    }

    @Override
    protected void scheduled() {
        super.scheduled();
        log.info(this + " is scheduled!");
    }

    @Override
    public void reset() {
        super.reset();
        log.info(this + " is reset!");
    }

    @Override
    public String toString() {
        return "Thread [name = " + name + "]";
    }

    /**
     * 获取服务名称
     *
     * @return 服务名称
     */
    public String getName() {
        return name;
    }

    public void postAction(Integer address, RegisterValue registerValue) throws ModbusException {
        if (master != null && master.isConnected()) {
            ModbusCommandDataUtils.commandRegister(master
                    , InitAppSetting.getSetting().getSettingServer().getSlaveId()
                    , address
                    , registerValue);
        }
    }

    /**
     * 初始化采集变量
     */
    private void initGroupRequestRegisterMap() {
        try {
            for (SettingGroup settingGroup : InitAppSetting.getSetting().getSettingGroupList()) {
                // 分组初始化采集参数
                Map<Integer, ModbusDataTypeEnum> modbusDataTypeEnumMap = new HashMap<>();

                // 初始化地址
                for (int i = settingGroup.getStart(); i <= settingGroup.getEnd(); i++) {
                    int finalI = i;
                    Optional<SettingLocation> settingLocationOptional
                            = settingGroup.getSettingLocationList()
                            .stream().filter(k -> k.getAddress().equals(finalI)).findAny();
                    if (settingLocationOptional.isPresent()) {
                        // 存在需要采集的地址
                        // 数据类型根据type定义
                        switch (settingLocationOptional.get().getConvert()) {
                            //type, 1:0~65535 ushort; 2: -32767~32768 short
                            case 2:
                                modbusDataTypeEnumMap.put(finalI, ModbusDataTypeEnum.PM_AB);
                                break;
                            default:
                                modbusDataTypeEnumMap.put(finalI, ModbusDataTypeEnum.P_AB);
                                break;
                        }
                    }
                }
                List<Obj4RequestRegister> requestRegisters = ModbusRequestDataUtils.splitModbusRequest(modbusDataTypeEnumMap
                        , InitAppSetting.getSetting().getSettingServer().getSlaveId()
                        , FunctionCode.READ_INPUT_REGISTERS);
                groupRequestRegisterMap.put(settingGroup.getIndex(), requestRegisters);
            }
        } catch (Exception e) {
            log.error("error to init group request register map: ", e);
        }
    }

    /**
     * 读取modbus数据
     */
    private void readModbusData() {
        try {
            for (Map.Entry<Integer, List<Obj4RequestRegister>> requestRegisterMap : groupRequestRegisterMap.entrySet()) {
                Map<Integer, IModbusDataType> readData = ModbusRequestDataUtils.getRegisterData(master, requestRegisterMap.getValue());
                ArrayList<Integer> lll = new ArrayList<>(readData.keySet());
                Collections.sort(lll);
                for (Integer i : lll) {
                    if (readData.get(i) instanceof NumericModbusData) {
                        collectionData.put(i, ((NumericModbusData) readData.get(i)).getValue());
                    } else {
                        collectionData.put(i, JSON.toJSONString(((BooleanModbusDataInRegister) readData.get(i)).getValues()));
                    }
                }
            }
        } catch (Exception e) {
            log.error("error to read modbus data: ", e);
        }
    }
}
