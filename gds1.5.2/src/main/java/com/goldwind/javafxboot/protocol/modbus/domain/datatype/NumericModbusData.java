package com.goldwind.javafxboot.protocol.modbus.domain.datatype;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 数字的类型
 *
 * @author:
 * @version:
 */
@NoArgsConstructor
public abstract class NumericModbusData extends RegisterValue {

	public NumericModbusData(BigDecimal value) {
		this.value = value;
	}

	@Setter
	@Getter
	@Accessors(chain = true)
	protected BigDecimal value;


}
