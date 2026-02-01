package com.goldwind.javafxboot.protocol.modbus.netty;

import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import com.goldwind.javafxboot.protocol.modbus.domain.synchronouswaitingroom.RtuSynchronousWaitingRoom;
import com.goldwind.javafxboot.protocol.modbus.domain.synchronouswaitingroom.SynchronousWaitingRoom;
import com.goldwind.javafxboot.protocol.modbus.exceptiom.ModbusException;
import com.goldwind.javafxboot.protocol.core.netty.AbstractRtuModeBuilder;
import com.goldwind.javafxboot.protocol.core.netty.ProtocolChannelInitializer;
import com.goldwind.javafxboot.protocol.core.purejavacomm.PureJavaCommChannel;

/**
 * cdt的客户端
 *
 * @author:
 * @version:
 */
@Accessors(chain = true)
@Slf4j
public class ModbusRtuMasterBuilder extends AbstractRtuModeBuilder implements ModbusMasterBuilderInterface {


	private SynchronousWaitingRoom synchronousWaitingRoom;

	public ModbusRtuMasterBuilder(String commPortId) {
		super(commPortId);
	}

	@Override
	public SynchronousWaitingRoom getOrCreateSynchronousWaitingRoom() throws ModbusException {
		if (this.synchronousWaitingRoom == null) {
			this.synchronousWaitingRoom = new RtuSynchronousWaitingRoom();
		}
		return this.synchronousWaitingRoom;
	}


	@Override
	protected ProtocolChannelInitializer getOrCreateChannelInitializer() {
		if (this.channelInitializer == null) {
			this.channelInitializer = new ProtocolChannelInitializer<PureJavaCommChannel>(this) {
				@Override
				protected void initChannel(PureJavaCommChannel ch) throws Exception {
					ch.pipeline().addLast(new ModbusRtuMasterDelimiterHandler().setLog(getLog()));
					ch.pipeline().addLast(new ModbusRtuMasterHandler((ModbusRtuMasterBuilder) builder));
				}
			};
		}
		return this.channelInitializer;
	}
}
