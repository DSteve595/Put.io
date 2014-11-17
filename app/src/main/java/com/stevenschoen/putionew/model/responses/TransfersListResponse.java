package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.transfers.PutioTransfer;

import java.util.List;

public class TransfersListResponse extends BasePutioResponse {
	private List<PutioTransfer> transfers;

	public List<PutioTransfer> getTransfers() {
		return transfers;
	}
}
