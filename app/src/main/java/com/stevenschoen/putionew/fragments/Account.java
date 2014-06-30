package com.stevenschoen.putionew.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.model.PutioRestInterface;
import com.stevenschoen.putionew.model.account.PutioAccountInfo;
import com.stevenschoen.putionew.model.responses.AccountInfoResponse;

public class Account extends Fragment {

	private PutioUtils utils;

	private TextView textDiskTotal;
	private TextView textDiskFree;
	private TextView textEmail;
	private TextView textName;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.utils = ((PutioApplication) getActivity().getApplication()).getPutioUtils();

		utils.getEventBus().register(this);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.account, container, false);
		
		textName = (TextView) view.findViewById(R.id.text_behind_accountname);
		textEmail = (TextView) view.findViewById(R.id.text_behind_accountemail);
		textDiskFree = (TextView) view.findViewById(R.id.text_storage_amountfree);
		textDiskTotal = (TextView) view.findViewById(R.id.text_storage_amounttotal);
		
		invalidateAccountInfo();
		
		return view;
	}

	public void invalidateAccountInfo() {
		utils.getJobManager().addJobInBackground(new PutioRestInterface.GetAccountInfoJob(utils));
	}

	public void onEventMainThread(AccountInfoResponse result) {
		PutioAccountInfo account = result.getInfo();
		PutioAccountInfo.DiskInfo disk = account.getDisk();

		textName.setText(account.getUsername());
		textEmail.setText(account.getMail());
		textDiskFree.setText(
				PutioUtils.humanReadableByteCount(disk.getAvail(), false));
		textDiskTotal.setText(getString(R.string.total_is,
				PutioUtils.humanReadableByteCount(disk.getSize(), false)));
	}

	@Override
	public void onDestroy() {
		utils.getEventBus().unregister(this);

		super.onDestroy();
	}
}