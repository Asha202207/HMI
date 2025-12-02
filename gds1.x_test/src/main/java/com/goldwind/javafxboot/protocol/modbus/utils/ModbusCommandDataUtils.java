package com.goldwind.javafxboot.protocol.modbus.utils;

import com.google.common.primitives.Bytes;
import lombok.extern.slf4j.Slf4j;
import com.goldwind.javafxboot.protocol.modbus.domain.FunctionCode;
import com.goldwind.javafxboot.protocol.modbus.domain.command.AbstractModbusCommand;
import com.goldwind.javafxboot.protocol.modbus.domain.command.RtuModbusCommand;
import com.goldwind.javafxboot.protocol.modbus.domain.command.TcpModbusCommand;
import com.goldwind.javafxboot.protocol.modbus.domain.confirm.AbstractModbusConfirm;
import com.goldwind.javafxboot.protocol.modbus.domain.confirm.RtuModbusConfirm;
import com.goldwind.javafxboot.protocol.modbus.domain.confirm.TcpModbusConfirm;
import com.goldwind.javafxboot.protocol.modbus.domain.datatype.RegisterValue;
import com.goldwind.javafxboot.protocol.modbus.domain.tcpextracode.TransactionIdentifier;
import com.goldwind.javafxboot.protocol.modbus.exceptiom.ModbusException;
import com.goldwind.javafxboot.protocol.modbus.netty.ModbusMasterBuilderInterface;
import com.goldwind.javafxboot.protocol.modbus.netty.ModbusRtuMasterBuilder;
import com.goldwind.javafxboot.protocol.core.netty.AbstractTcpMasterBuilder;
import com.goldwind.javafxboot.protocol.core.netty.MasterInterface;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * modbs下达数据命令的工具类
 *
 * @author:
 * @version:
 */
@Slf4j
public class ModbusCommandDataUtils {


	/**
	 * 下达寄存器控制命令
	 *
	 * @param masterBuilder 主站对象
	 * @param address       起始地址
	 * @param values        命令值
	 */
	public static boolean commandRegister(MasterInterface masterBuilder, Integer slaveId, Integer address, List<RegisterValue> values) throws ModbusException {
		if (!(masterBuilder instanceof ModbusMasterBuilderInterface)) {
			throw new RuntimeException("请传人实现了<ModbusMasterBuilderInterface>的Master");
		}
		List<Byte> bs = new ArrayList<>();
		AbstractModbusCommand modbusCommand;
		ByteBuffer buffer;
		AbstractModbusConfirm confirm;
		try {
			if (masterBuilder instanceof ModbusRtuMasterBuilder) {
				modbusCommand = new RtuModbusCommand();
				modbusCommand.setSlaveId(slaveId).setRegisters(address, values);
				modbusCommand.encode(bs);
				masterBuilder.sendFrameToOpposite(Bytes.toArray(bs));
				buffer = ((ModbusMasterBuilderInterface) masterBuilder).getOrCreateSynchronousWaitingRoom().getData(modbusCommand.getFunctionCode().getCode());
				confirm = new RtuModbusConfirm().decode(buffer);
			} else {
				modbusCommand = new TcpModbusCommand();
				((TcpModbusCommand) modbusCommand).setTransactionIdentifier(TransactionIdentifier.getInstance((AbstractTcpMasterBuilder) masterBuilder));
				modbusCommand.setSlaveId(slaveId).setRegisters(address, values);
				modbusCommand.encode(bs);
				masterBuilder.sendFrameToOpposite(Bytes.toArray(bs));
				buffer = ((ModbusMasterBuilderInterface) masterBuilder).getOrCreateSynchronousWaitingRoom().getData(((TcpModbusCommand) modbusCommand).getTcpExtraCode().getTransactionIdentifier().getSeq());
				confirm = new TcpModbusConfirm().decode(buffer);
			}
		} catch (ModbusException e) {
			log.error("控制命令执行失败:" + e.getMsg());
			return false;
		}
		if (address.equals(confirm.getStartAddress())) {
			if (confirm.getFunctionCode() == modbusCommand.getFunctionCode()) {
				if (confirm.getFunctionCode() == FunctionCode.WRITE_COIL || confirm.getFunctionCode() == FunctionCode.WRITE_REGISTER) {
					if (Bytes.indexOf(confirm.getB2(), modbusCommand.getDataBytes()) == 0) {
						return true;
					} else {
						log.warn("返回的确认值和输出值不同");
					}
				} else {
					if (confirm.getQuantity().equals(modbusCommand.getQuantity())) {
						return true;
					} else {
						log.warn("返回的值数量和输出数量不同");
					}
				}
			}
		}
		return false;
	}


	/**
	 * 下达线圈控制命令
	 *
	 * @param masterBuilder 主站对象
	 * @param address       起始地址
	 * @param values        命令值
	 */
	public static boolean commandCoils(MasterInterface masterBuilder, Integer slaveId, Integer address, List<Boolean> values) throws ModbusException {
		if (!(masterBuilder instanceof ModbusMasterBuilderInterface)) {
			throw new RuntimeException("请传人实现了<ModbusMasterBuilderInterface>的Master");
		}
		List<Byte> bs = new ArrayList<>();
		AbstractModbusCommand modbusCommand;
		ByteBuffer buffer;
		AbstractModbusConfirm confirm;
		try {
			if (masterBuilder instanceof ModbusRtuMasterBuilder) {
				modbusCommand = new RtuModbusCommand();
				modbusCommand.setSlaveId(slaveId).setCoils(address, values);
				modbusCommand.encode(bs);
				masterBuilder.sendFrameToOpposite(Bytes.toArray(bs));
				buffer = ((ModbusMasterBuilderInterface) masterBuilder).getOrCreateSynchronousWaitingRoom().getData(modbusCommand.getFunctionCode().getCode());
				confirm = new RtuModbusConfirm().decode(buffer);
			} else {
				modbusCommand = new TcpModbusCommand();
				((TcpModbusCommand) modbusCommand).setTransactionIdentifier(TransactionIdentifier.getInstance((AbstractTcpMasterBuilder) masterBuilder));
				modbusCommand.setSlaveId(slaveId).setCoils(address, values);
				modbusCommand.encode(bs);
				masterBuilder.sendFrameToOpposite(Bytes.toArray(bs));
				buffer = ((ModbusMasterBuilderInterface) masterBuilder).getOrCreateSynchronousWaitingRoom().getData(((TcpModbusCommand) modbusCommand).getTcpExtraCode().getTransactionIdentifier().getSeq());
				confirm = new TcpModbusConfirm().decode(buffer);
			}
		} catch (ModbusException e) {
			log.error("控制命令执行失败:" + e.getMsg());
			return false;
		}
		if (address.equals(confirm.getStartAddress())) {
			if (confirm.getFunctionCode() == modbusCommand.getFunctionCode()) {
				if (confirm.getFunctionCode() == FunctionCode.WRITE_COIL || confirm.getFunctionCode() == FunctionCode.WRITE_REGISTER) {
					if (Bytes.indexOf(confirm.getB2(), modbusCommand.getDataBytes()) == 0) {
						return true;
					} else {
						log.warn("返回的确认值和输出值不同");
					}
				} else {
					if (confirm.getQuantity().equals(modbusCommand.getQuantity())) {
						return true;
					} else {
						log.warn("返回的值数量和输出数量不同");
					}
				}
			}
		}
		return false;
	}


	/**
	 * 下达寄存器控制命令
	 *
	 * @param masterBuilder 主站对象
	 * @param address       起始地址
	 * @param value         命令值
	 */
	public static boolean commandRegister(MasterInterface masterBuilder, Integer slaveId, Integer address, RegisterValue value) throws ModbusException {
		List<RegisterValue> list = new ArrayList<>();
		list.add(value);
		return commandRegister(masterBuilder, slaveId, address, list);
	}

	/**
	 * 下达线圈控制命令
	 *
	 * @param masterBuilder 主站对象
	 * @param address       起始地址
	 * @param value         命令值
	 */
	public static boolean commandCoil(MasterInterface masterBuilder, Integer slaveId, Integer address, Boolean value) throws ModbusException {
		List<Boolean> list = new ArrayList<>();
		list.add(value);
		return commandCoils(masterBuilder, slaveId, address, list);
	}
}
