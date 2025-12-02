package com.goldwind.javafxboot.protocol.modbus.exceptiom;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * modbus 的异常
 *
 * @author:
 * @version:
 */
@Data
@AllArgsConstructor
public class ModbusException extends Exception {

	public ModbusException(String msg) {
		this.msg = msg;
	}

	Integer code;

	String msg;
}
