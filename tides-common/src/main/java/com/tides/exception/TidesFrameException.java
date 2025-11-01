package com.tides.exception;

import com.tides.common.ApiResponse;
import com.tides.enums.BaseCode;
import lombok.Data;

/**
 * @description: 业务异常
 * @author: 19continue
 **/
@Data
public class TidesFrameException extends BaseException {

	private Integer code;
	
	private String message;

	public TidesFrameException() {
		super();
	}

	public TidesFrameException(String message) {
		super(message);
	}
	
	
	public TidesFrameException(String code, String message) {
		super(message);
		this.code = Integer.parseInt(code);
		this.message = message;
	}
	
	public TidesFrameException(Integer code, String message) {
		super(message);
		this.code = code;
		this.message = message;
	}
	
	public TidesFrameException(BaseCode baseCode) {
		super(baseCode.getMsg());
		this.code = baseCode.getCode();
		this.message = baseCode.getMsg();
	}
	
	public TidesFrameException(ApiResponse apiResponse) {
		super(apiResponse.getMessage());
		this.code = apiResponse.getCode();
		this.message = apiResponse.getMessage();
	}

	public TidesFrameException(Throwable cause) {
		super(cause);
	}

	public TidesFrameException(String message, Throwable cause) {
		super(message, cause);
		this.message = message;
	}

	public TidesFrameException(Integer code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
		this.message = message;
	}
}
