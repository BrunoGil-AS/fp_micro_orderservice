package com.aspiresys.fp_micro_orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FpMicroOrderserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(FpMicroOrderserviceApplication.class, args);
	}

}
