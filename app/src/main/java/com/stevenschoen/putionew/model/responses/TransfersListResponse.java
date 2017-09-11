package com.stevenschoen.putionew.model.responses;

import com.stevenschoen.putionew.model.ResponseOrError;
import com.stevenschoen.putionew.model.transfers.PutioTransfer;

import java.util.List;

public class TransfersListResponse extends ResponseOrError.BasePutioResponse {
	private List<PutioTransfer> transfers;

	public List<PutioTransfer> getTransfers() {
		return transfers;
	}
}
