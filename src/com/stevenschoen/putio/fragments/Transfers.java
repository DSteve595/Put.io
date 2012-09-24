package com.stevenschoen.putio.fragments;

import java.util.ArrayList;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.stevenschoen.putio.PutioTransferData;
import com.stevenschoen.putio.PutioTransferLayout;
import com.stevenschoen.putio.R;
import com.stevenschoen.putio.TransfersAdapter;
import com.stevenschoen.putio.UIUtils;

public final class Transfers extends SherlockFragment {
	
	private TransfersAdapter adapter;
	private ArrayList<PutioTransferLayout> transferLayouts = new ArrayList<PutioTransferLayout>();
	PutioTransferData[] transfersData;
	private PutioTransferLayout dummyTransfer = new PutioTransferLayout("Loading...", 0, 0, 0, "FAKE");
	private ListView listview;
	
	public static Transfers newInstance() {
		Transfers fragment = new Transfers();
		
		return fragment;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.transfers, container, false);
		
		listview = (ListView) view.findViewById(R.id.transferslist);
		
		adapter = new TransfersAdapter(getSherlockActivity(), R.layout.transfer,
				transferLayouts);
		listview.setEmptyView(view.findViewById(R.id.loading));
		listview.setAdapter(adapter);
		listview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		registerForContextMenu(listview);
		if (UIUtils.isTablet(getSherlockActivity())) {
			listview.setVerticalFadingEdgeEnabled(true);
		}
		
		if (savedInstanceState != null && savedInstanceState.containsKey("transfersData")) {
			try {
				Parcelable[] transferParcelables = savedInstanceState.getParcelableArray("transfersData");
	
				PutioTransferData[] transfers = new PutioTransferData[transferParcelables.length];
				for (int i = 0; i < transferParcelables.length; i++) {
					transfers[i] = (PutioTransferData) transferParcelables[i];
				}
				updateTransfers(transfers);
			} catch (NullPointerException e) {
			}
		}
		return view;
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putParcelableArray("transfersData", transfersData);
	}
	
	public void updateTransfers(PutioTransferData[] transfers) {
		final ArrayList<PutioTransferLayout> transferLayoutsTemp = new ArrayList<PutioTransferLayout>();
		for (int i = 0; i < transfers.length; i++) {
			if (isAdded()) {
				transferLayoutsTemp.add(new PutioTransferLayout(
						transfers[i].name,
						transfers[i].downSpeed,
						transfers[i].upSpeed,
						transfers[i].percentDone,
						transfers[i].status));
			}
		}
		
		int index = listview.getFirstVisiblePosition();
		View v = listview.getChildAt(0);
		int top = (v == null) ? 0 : v.getTop();
		
		listview.setSelectionFromTop(index, top);
		
		while (transferLayouts.size() > transferLayoutsTemp.size()) {
			transferLayouts.remove(transferLayouts.size() - 1);
		}
		
		for (int i = 0; i < transferLayoutsTemp.size(); i++) {
			if (transferLayouts.size() <= i) {
				transferLayouts.add(transferLayoutsTemp.get(i));
			} else {
				transferLayouts.set(i, transferLayoutsTemp.get(i));
			}
			adapter.notifyDataSetChanged();
		}
		
		transfersData = transfers;
	}
}