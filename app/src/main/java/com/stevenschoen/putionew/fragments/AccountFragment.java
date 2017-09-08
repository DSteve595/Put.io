package com.stevenschoen.putionew.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.model.account.PutioAccountInfo;
import com.stevenschoen.putionew.model.responses.AccountInfoResponse;
import com.trello.rxlifecycle2.components.support.RxFragment;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;

public class AccountFragment extends RxFragment {

	private PutioUtils utils;

	private TextView textDiskTotal;
	private TextView textDiskFree;
	private TextView textEmail;
	private TextView textName;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.utils = ((PutioApplication) getActivity().getApplication()).getPutioUtils();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View view = inflater.inflate(R.layout.account, container, false);
		
		textName = view.findViewById(R.id.text_behind_accountname);
		textEmail = view.findViewById(R.id.text_behind_accountemail);
		textDiskFree = view.findViewById(R.id.text_storage_amountfree);
		textDiskTotal = view.findViewById(R.id.text_storage_amounttotal);
		
		invalidateAccountInfo();
		
		return view;
	}

	public void invalidateAccountInfo() {
		utils.getRestInterface().account()
				.compose(this.<AccountInfoResponse>bindToLifecycle())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe(new Consumer<AccountInfoResponse>() {
					@Override
					public void accept(@NonNull AccountInfoResponse accountInfoResponse) throws Exception {
						PutioAccountInfo account = accountInfoResponse.getInfo();
						PutioAccountInfo.DiskInfo disk = account.getDisk();

						textName.setText(account.getUsername());
						textEmail.setText(account.getMail());
						textDiskFree.setText(
								PutioUtils.humanReadableByteCount(disk.getAvail(), false));
						textDiskTotal.setText(getString(R.string.total_is,
								PutioUtils.humanReadableByteCount(disk.getSize(), false)));
					}
				}, new Consumer<Throwable>() {
					@Override
					public void accept(@NonNull Throwable throwable) throws Exception {
						PutioUtils.getRxJavaThrowable(throwable).printStackTrace();
					}
				});
	}
}