package com.tides.exception;

import lombok.Data;

/**
 * @description: 参数错误
 * @author: 19continue
 **/
@Data
public class ArgumentError {
	
	private String argumentName;
	
	private String message;
}
