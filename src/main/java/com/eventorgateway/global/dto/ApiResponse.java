package com.eventorgateway.global.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {

	private static final String SUCCESS_STATUS = "success";
	private static final String FAIL_STATUS = "fail";
	private static final String ERROR_STATUS = "error";

	private String status;
	private T data;
	private String message;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
	private LocalDateTime serverTime = LocalDateTime.now();

	public static <T> ApiResponse<T> createSuccess(T data, String message) {
		return new ApiResponse<>(SUCCESS_STATUS, data, message);
	}

	public static <T> ApiResponse<T> createSuccess(T data) {
		return new ApiResponse<>(SUCCESS_STATUS, data, null);
	}

	public static <T> ApiResponse<T> createSuccess(String message) {
		return new ApiResponse<>(SUCCESS_STATUS, null, message);
	}

	public static <T> ApiResponse<T> createSuccess() {
		return new ApiResponse<>(SUCCESS_STATUS, null, null);
	}

	// Hibernate Validator 에 의해 유효하지 않은 데이터로 인해 API 호출이 거부될때 반환
	public static ApiResponse<?> createFail(BindingResult bindingResult) {
		Map<String, String> errors = new HashMap<>();

		List<ObjectError> allErrors = bindingResult.getAllErrors();
		for (ObjectError error : allErrors) {
			if (error instanceof FieldError) {
				errors.put(((FieldError)error).getField(), error.getDefaultMessage());
			} else {
				errors.put(error.getObjectName(), error.getDefaultMessage());
			}
		}
		return new ApiResponse<>(FAIL_STATUS, errors, null);
	}

	public static <T> ApiResponse<T> createError(String status, String message) {
		return new ApiResponse<>(status, null, message);
	}

	private ApiResponse(String status, T data, String message) {
		this.status = status;
		this.data = data;
		this.message = message;
	}
}