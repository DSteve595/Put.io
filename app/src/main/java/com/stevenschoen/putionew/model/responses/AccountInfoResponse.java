package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.account.PutioAccountInfo;

public class AccountInfoResponse {
	private String status;
	private PutioAccountInfo info;

	public String getStatus() {
		return status;
	}

	public PutioAccountInfo getInfo() {
		return info;
	}
}
