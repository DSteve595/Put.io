package com.stevenschoen.putio.fragments;

import java.net.SocketTimeoutException;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.stevenschoen.putio.PutioAccountInfo;
import com.stevenschoen.putio.PutioUtils;
import com.stevenschoen.putio.R;
import com.stevenschoen.putio.UIUtils;

public class Account extends SherlockFragment {
	public static Account newInstance() {
		Account fragment = new Account();
		
		return fragment;
	}
	
	SharedPreferences sharedPrefs;
	
	PutioUtils utils;

	private TextView textDiskTotal;
	private TextView textDiskFree;
	private TextView textEmail;
	private TextView textName;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getSherlockActivity());		
		String token = sharedPrefs.getString("token", null);
		utils = new PutioUtils(token, sharedPrefs);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		int accountLayoutId = R.layout.account;
		if (!UIUtils.hasHoneycomb() && PutioUtils.dpFromPx(getSherlockActivity(), getResources().getDisplayMetrics().heightPixels) < 400) {
			accountLayoutId = R.layout.accountgbhori;
		} else if (!UIUtils.hasHoneycomb() && PutioUtils.dpFromPx(getSherlockActivity(), getResources().getDisplayMetrics().heightPixels) >= 400) {
			accountLayoutId = R.layout.accountgbvert;
		} else if (!UIUtils.hasHoneycomb()) {
			accountLayoutId = R.layout.accountgbvert;
		}
		final View view = inflater.inflate(accountLayoutId, container, false);
		
		textName = (TextView) view.findViewById(R.id.text_behind_accountname);
		textEmail = (TextView) view.findViewById(R.id.text_behind_accountemail);
		textDiskFree = (TextView) view.findViewById(R.id.text_storage_amountfree);
		textDiskTotal = (TextView) view.findViewById(R.id.text_storage_amounttotal);
		
		invalidateAccountInfo();
		
		return view;
	}
	
	public void invalidateAccountInfo() {
		class GetAccountInfoTask extends AsyncTask<Void, Void, PutioAccountInfo> {

			@Override
			protected PutioAccountInfo doInBackground(Void... nothing) {
				try {
					return utils.getAccountInfo();
				} catch (SocketTimeoutException e) {
					return null;
				}
			}
			
			@Override
			protected void onPostExecute(final PutioAccountInfo info) {
				if (info != null) {
					textName.setText(info.username);
					textEmail.setText(info.email);
					textDiskFree.setText(PutioUtils.humanReadableByteCount(info.diskAvailable, false));
					textDiskTotal.setText(PutioUtils.humanReadableByteCount(info.diskSize, false));
				}
			}
		}
		new GetAccountInfoTask().execute();
	}
}