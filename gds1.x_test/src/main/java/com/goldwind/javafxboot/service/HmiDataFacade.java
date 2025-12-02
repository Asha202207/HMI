package com.goldwind.javafxboot.service;

import com.goldwind.javafxboot.protocol.modbus.domain.datatype.numeric.P_AB;
import com.goldwind.javafxboot.protocol.modbus.exceptiom.ModbusException;
import com.goldwind.javafxboot.util.InitAppSetting;
import javafx.concurrent.Service;
import javafx.util.Duration;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Facade for isolating UI components from Modbus protocol details.
 */
public class HmiDataFacade {

    private final ThreadScheduledService scheduledService;
    private Consumer<Map<Integer, Object>> dataConsumer;

    public HmiDataFacade() {
        this.scheduledService = new ThreadScheduledService("modbus-tcp service");
        configureService();
    }

    private void configureService() {
        scheduledService.setPeriod(Duration.millis(InitAppSetting.getSetting().getSettingServer().getCycle() * 1000.0));
        scheduledService.setDelay(Duration.millis(500));
        scheduledService.setRestartOnFailure(false);
    }

    public void setDataConsumer(Consumer<Map<Integer, Object>> consumer) {
        this.dataConsumer = consumer;
        scheduledService.setOnSucceeded(t -> {
            if (t.getSource().getValue() instanceof Map && dataConsumer != null) {
                Map<Integer, Object> values = (Map<Integer, Object>) t.getSource().getValue();
                if (!values.isEmpty()) {
                    dataConsumer.accept(values);
                }
            }
        });
    }

    public void connect() {
        if (scheduledService.isRunning()) {
            return;
        }
        if (scheduledService.getState() == Service.State.READY) {
            scheduledService.start();
        } else {
            scheduledService.restart();
        }
    }

    public void disconnect() {
        scheduledService.cancel();
    }

    public boolean isConnected() {
        return scheduledService.isRunning();
    }

    public void writeRegister(int address, int value) throws ModbusException {
        if (isConnected()) {
            scheduledService.postAction(address, new P_AB().setValue(BigDecimal.valueOf(value)));
        }
    }
}
