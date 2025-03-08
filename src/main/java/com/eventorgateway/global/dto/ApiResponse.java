package com.eventorgateway.global.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

	// success 응답 반환
	public static <T> ResponseEntity<ApiResponse<T>> createSuccess(T data, String message) {
		ApiResponse<T> response = new ApiResponse<>(SUCCESS_STATUS, data, message);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public static <T> ResponseEntity<ApiResponse<T>> createSuccess(T data) {
		ApiResponse<T> response = new ApiResponse<>(SUCCESS_STATUS, data, null);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public static <T> ResponseEntity<ApiResponse<T>> createSuccess(String message) {
		ApiResponse<T> response = new ApiResponse<>(SUCCESS_STATUS, null, message);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	public static <T> ResponseEntity<ApiResponse<T>> createSuccess() {
		ApiResponse<T> response = new ApiResponse<>(SUCCESS_STATUS, null, null);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	// Hibernate Validator 에 의한 유효성 검사 실패 시 반환
	public static ResponseEntity<ApiResponse<?>> createFail(BindingResult bindingResult) {
		Map<String, String> errors = new HashMap<>();

		List<ObjectError> allErrors = bindingResult.getAllErrors();
		for (ObjectError error : allErrors) {
			if (error instanceof FieldError) {
				errors.put(((FieldError)error).getField(), error.getDefaultMessage());
			} else {
				errors.put(error.getObjectName(), error.getDefaultMessage());
			}
		}

		ApiResponse<Map<String, String>> response = new ApiResponse<>(FAIL_STATUS, errors, null);
		return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
	}

	// error 응답 반환
	public static <T> ResponseEntity<ApiResponse<T>> createError(HttpStatus status, String message) {
		ApiResponse<T> response = new ApiResponse<>(status.name(), null, message);
		return new ResponseEntity<>(response, status);
	}

	// 생성자
	public ApiResponse(String status, T data, String message) {
		this.status = status;
		this.data = data;
		this.message = message;
	}
}