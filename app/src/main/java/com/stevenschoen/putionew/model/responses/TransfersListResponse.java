package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.transfers.PutioTransferData;

import java.util.List;

public class TransfersListResponse extends BasePutioResponse {
	private List<PutioTransferData> transfers;

	public List<PutioTransferData> getTransfers() {
		return transfers;
	}
}
