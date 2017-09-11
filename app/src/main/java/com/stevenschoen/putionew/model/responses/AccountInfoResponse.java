package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.ResponseOrError;
import com.stevenschoen.putionew.model.account.PutioAccountInfo;

public class AccountInfoResponse extends ResponseOrError.BasePutioResponse {
	private PutioAccountInfo info;

	public PutioAccountInfo getInfo() {
		return info;
	}
}
