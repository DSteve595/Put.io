package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.account.PutioAccountInfo;

public class AccountInfoResponse extends BasePutioResponse {
	private PutioAccountInfo info;

	public PutioAccountInfo getInfo() {
		return info;
	}
}
