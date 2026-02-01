package com.goldwind.javafxboot.protocol.modbus.netty;


import com.goldwind.javafxboot.protocol.modbus.domain.synchronouswaitingroom.SynchronousWaitingRoom;
import com.goldwind.javafxboot.protocol.modbus.exceptiom.ModbusException;

/**
 * modbus  master的构建器
 *
 * @author:
 * @version:
 */
public interface ModbusMasterBuilderInterface {


	/**
	 * 获取或创建同步等候室
	 * null则创建，有则获取获取EventLoopGroup 用与bootstrap的绑定
	 *
	 * @return or create work group
	 * @throws ModbusException modbus例外
	 */
	SynchronousWaitingRoom getOrCreateSynchronousWaitingRoom() throws ModbusException;

}
