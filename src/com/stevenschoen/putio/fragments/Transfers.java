package com.stevenschoen.putio.fragments;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.stevenschoen.putio.PutioTransferData;
import com.stevenschoen.putio.PutioTransferLayout;
import com.stevenschoen.putio.PutioUtils;
import com.stevenschoen.putio.R;
import com.stevenschoen.putio.TransfersAdapter;
import com.stevenschoen.putio.UIUtils;

public final class Transfers extends SherlockFragment {
	
	private TransfersAdapter adapter;
	private ArrayList<PutioTransferLayout> transferLayouts = new ArrayList<PutioTransferLayout>();
	PutioTransferData[] transfersData;
	private PutioTransferLayout dummyTransfer = new PutioTransferLayout("Loading...", 0, 0, 0, "FAKE");
	private ListView listview;
	
	private int viewMode = 1;
	public static final int VIEWMODE_LIST = 1;
	public static final int VIEWMODE_LISTOREMPTY = 2;
	public static final int VIEWMODE_LOADING = -1;
	public static final int VIEWMODE_EMPTY = -2;
	public static final int VIEWMODE_NONETWORK = 3;
	
	private View loadingView;
	private View emptyView;
	private View noNetworkView;
	
	public static Transfers newInstance() {
		Transfers fragment = new Transfers();
		
		return fragment;
	}
	
    public interface Callbacks {

    	public void transfersReady();
        public void onTransferSelected(int parentId, int id);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
    	@Override
    	public void transfersReady() {
    	}
        @Override
        public void onTransferSelected(int parentId, int id) {
        }
    };
	
    private Callbacks mCallbacks = sDummyCallbacks;
	
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
		listview.setAdapter(adapter);
		listview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		registerForContextMenu(listview);
		if (UIUtils.isTablet(getSherlockActivity())) {
			listview.setVerticalFadingEdgeEnabled(true);
		}
		listview.setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> a, View view, int position,
					long id) {
				if (transfersData[position].status.matches("COMPLETED") || transfersData[position].status.matches("SEEDING")) {
					mCallbacks.onTransferSelected(transfersData[position].saveParentId, transfersData[position].fileId);
				}
			}
		});
		
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
		
		loadingView = view.findViewById(R.id.transfers_loading);
		emptyView = view.findViewById(R.id.transfers_empty);
		noNetworkView = view.findViewById(R.id.transfers_nonetwork);
		
		setViewMode(VIEWMODE_LOADING);
		return view;
	}
	
	private void setViewMode(int mode) {
		if (mode != viewMode) {
			switch (mode) {
			case VIEWMODE_LIST:
				listview.setVisibility(View.VISIBLE);
				loadingView.setVisibility(View.GONE);
				emptyView.setVisibility(View.GONE);
				noNetworkView.setVisibility(View.GONE);
				break;
			case VIEWMODE_LOADING:
				listview.setVisibility(View.INVISIBLE);
				loadingView.setVisibility(View.VISIBLE);
				emptyView.setVisibility(View.GONE);
				noNetworkView.setVisibility(View.GONE);
				break;
			case VIEWMODE_EMPTY:
				listview.setVisibility(View.INVISIBLE);
				loadingView.setVisibility(View.GONE);
				emptyView.setVisibility(View.VISIBLE);
				noNetworkView.setVisibility(View.GONE);
				break;
			case VIEWMODE_NONETWORK:
				listview.setVisibility(View.INVISIBLE);
				loadingView.setVisibility(View.GONE);
				emptyView.setVisibility(View.GONE);
				noNetworkView.setVisibility(View.VISIBLE);
				break;
			case VIEWMODE_LISTOREMPTY:
				if (transfersData == null || transfersData.length == 0) {
					setViewMode(VIEWMODE_EMPTY);
				} else {
					setViewMode(VIEWMODE_LIST);
				}
				break;
			}
			viewMode = mode;
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		
		menu.setHeaderTitle(transfersData[info.position].name);
	    MenuInflater inflater = getSherlockActivity().getMenuInflater();
	    inflater.inflate(R.menu.context_transfers, menu);
	}
	
	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
			case R.id.context_remove:
				initRemoveTransfer((int) info.id);
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}
	
	private void initRemoveTransfer(int idInList) {
		if(transfersData[idInList].status.matches("COMPLETED")) {
			PutioUtils.removeTransferAsync(getActivity(), transfersData[idInList].id);
		} else {
			PutioUtils.showRemoveTransferDialog(getSherlockActivity(), transfersData[idInList].id);
		}
	}
	
	public void setHasNetwork(boolean has) {
		if (has) {
			setViewMode(VIEWMODE_LISTOREMPTY);
		} else {
			setViewMode(VIEWMODE_NONETWORK);
		}
	}
	
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
    	mCallbacks = (Callbacks) activity;
    	mCallbacks.transfersReady();
    }
    
    @Override
    public void onDetach() {
        super.onDetach();
        
        mCallbacks = sDummyCallbacks;
    }
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putParcelableArray("transfersData", transfersData);
	}
	
	public void updateTransfers(PutioTransferData[] transfers) {
		if (transfers == null || transfers.length == 0) {
			setViewMode(VIEWMODE_EMPTY);
			transfersData = null;
			return;
		} else {
			setViewMode(VIEWMODE_LIST);
		}
		
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