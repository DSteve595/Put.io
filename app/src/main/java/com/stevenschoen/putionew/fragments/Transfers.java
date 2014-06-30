package com.stevenschoen.putionew.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioTransferLayout;
import com.stevenschoen.putionew.PutioTransfersService;
import com.stevenschoen.putionew.PutioTransfersService.TransfersServiceBinder;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.TransfersAdapter;
import com.stevenschoen.putionew.UIUtils;
import com.stevenschoen.putionew.activities.Putio;
import com.stevenschoen.putionew.model.PutioRestInterface;
import com.stevenschoen.putionew.model.transfers.PutioTransferData;

import java.util.ArrayList;
import java.util.List;

public final class Transfers extends Fragment {
	
	private TransfersAdapter adapter;
	private ArrayList<PutioTransferLayout> transferLayouts = new ArrayList<>();
	private List<PutioTransferData> transfersData;
	private ListView listview;

	PutioUtils utils;
	PutioTransfersService transfersService;
	
	private int viewMode = 1;
	public static final int VIEWMODE_LIST = 1;
	public static final int VIEWMODE_LISTOREMPTY = 2;
	public static final int VIEWMODE_LOADING = -1;
	public static final int VIEWMODE_EMPTY = -2;
	public static final int VIEWMODE_NONETWORK = 3;
	
	private View loadingView;
	private View emptyView;
	private View noNetworkView;

	public interface Callbacks {
    	public void transfersReady();
        public void onTransferSelected(int parentId, int id);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
    	@Override
    	public void transfersReady() { }
        @Override
        public void onTransferSelected(int parentId, int id) { }
    };
	
    private Callbacks mCallbacks = sDummyCallbacks;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		this.utils = ((PutioApplication) getActivity().getApplication()).getPutioUtils();
		
		Intent transfersServiceIntent = new Intent(getActivity(), PutioTransfersService.class);
		getActivity().startService(transfersServiceIntent);
		getActivity().bindService(transfersServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.transfers, container, false);
		
		listview = (ListView) view.findViewById(R.id.transferslist);
		
		adapter = new TransfersAdapter(getActivity(), transferLayouts);
		listview.setAdapter(adapter);
		listview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		registerForContextMenu(listview);
		if (UIUtils.isTablet(getActivity())) {
			listview.setVerticalFadingEdgeEnabled(true);
		}
		listview.setOnItemClickListener(new OnItemClickListener() {
			
			@Override
			public void onItemClick(AdapterView<?> a, View view, int position,
					long id) {
				if (transfersData.get(position).status.equals("COMPLETED") || transfersData.get(position).status.equals("SEEDING")) {
					mCallbacks.onTransferSelected(transfersData.get(position).saveParentId, transfersData.get(position).fileId);
				}
			}
		});
		
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
				if (transfersData == null || transfersData.isEmpty()) {
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
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
		
		menu.setHeaderTitle(transfersData.get(info.position).name);
	    MenuInflater inflater = getActivity().getMenuInflater();
	    inflater.inflate(R.menu.context_transfers, menu);
	}
	
	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
			case R.id.context_remove:
				initRemoveTransfer((int) info.id);
				return true;
			default:
				return super.onContextItemSelected(item);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.transfers, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_clearfinished:
				utils.getJobManager().addJobInBackground(new PutioRestInterface.PostCleanTransfersJob(
						utils));
				return true;
		}

		return false;
	}

	private void initRemoveTransfer(int idInList) {
		if (transfersData.get(idInList).status.equals("COMPLETED")) {
			utils.getJobManager().addJobInBackground(new PutioRestInterface.PostCancelTransferJob(
					utils, transfersData.get(idInList).id));
		} else {
			utils.showRemoveTransferDialog(getActivity(), transfersData.get(idInList).id);
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
        
        try {
        	mCallbacks = (Callbacks) activity;
        } catch (ClassCastException e) {
        	mCallbacks = sDummyCallbacks;
        }
    	mCallbacks.transfersReady();
    	
    	getActivity().registerReceiver(
				transfersAvailableReceiver, new IntentFilter(Putio.transfersAvailableIntent));
    }
    
    @Override
    public void onDetach() {    	
        super.onDetach();
        
        mCallbacks = sDummyCallbacks;
        
        getActivity().unregisterReceiver(transfersAvailableReceiver);
    }
	
	public void updateTransfers(List<PutioTransferData> transfers) {
		if (transfers == null || transfers.isEmpty()) {
			setViewMode(VIEWMODE_EMPTY);
			transfersData = null;
			return;
		} else {
			setViewMode(VIEWMODE_LIST);
		}
		
//		final ArrayList<PutioTransferLayout> transferLayoutsTemp = new ArrayList<PutioTransferLayout>();
//		for (int i = 0; i < transfers.length; i++) {
//			if (isAdded()) {
//				transferLayoutsTemp.add(new PutioTransferLayout(
//						transfers[i].name,
//						transfers[i].downSpeed,
//						transfers[i].upSpeed,
//						transfers[i].percentDone,
//						transfers[i].status));
//			}
//		}
//		
//		int index = listview.getFirstVisiblePosition();
//		View v = listview.getChildAt(0);
//		int top = (v == null) ? 0 : v.getTop();
//		
//		listview.setSelectionFromTop(index, top);
//		
//		while (transferLayouts.size() > transferLayoutsTemp.size()) {
//			transferLayouts.remove(transferLayouts.size() - 1);
//		}
//		
//		for (int i = 0; i < transferLayoutsTemp.size(); i++) {
//			if (transferLayouts.size() <= i) {
//				transferLayouts.add(transferLayoutsTemp.get(i));
//			} else {
//				transferLayouts.set(i, transferLayoutsTemp.get(i));
//			}
//			adapter.notifyDataSetChanged();
//		}
		
		transfersData = transfers;
		
		if (isAdded()) {
			transferLayouts.clear();
			for (PutioTransferData transfer : transfers) {
				transferLayouts.add(new PutioTransferLayout(
						transfer.name,
						transfer.downSpeed,
						transfer.upSpeed,
						transfer.percentDone,
						transfer.status));
			}
			adapter.notifyDataSetChanged();
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (transfersService != null) {
			getActivity().unbindService(mConnection);
		}
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			TransfersServiceBinder binder = (TransfersServiceBinder) service;
			transfersService = binder.getService();
			if (transfersService.getTransfers() != null) {
				updateTransfers(transfersService.getTransfers());
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) { }
	};
	
	private BroadcastReceiver transfersAvailableReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			updateTransfers(transfersService.getTransfers());
		}
	};
}