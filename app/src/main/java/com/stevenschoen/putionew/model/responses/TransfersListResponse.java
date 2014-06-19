package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.transfers.PutioTransferData;

import java.util.List;

public class TransfersListResponse {
	private String status;
	private List<PutioTransferData> transfers;

	public String getStatus() {
		return status;
	}

	public List<PutioTransferData> getTransfers() {
		return transfers;
	}
}
