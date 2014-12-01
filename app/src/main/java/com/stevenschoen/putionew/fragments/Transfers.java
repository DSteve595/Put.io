package com.stevenschoen.putionew.fragments;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import com.stevenschoen.putionew.PutioTransfersService;
import com.stevenschoen.putionew.PutioTransfersService.TransfersServiceBinder;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.TransfersAdapter;
import com.stevenschoen.putionew.model.PutioRestInterface;
import com.stevenschoen.putionew.model.transfers.PutioTransfer;

import java.util.ArrayList;
import java.util.List;

public final class Transfers extends NoClipSupportFragment {
	
	private TransfersAdapter adapter;
	private List<PutioTransfer> transfers = new ArrayList<>();
	private ListView listview;

	private PutioUtils utils;
	private PutioTransfersService transfersService;
	
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
        public void onTransferSelected(PutioTransfer transfer);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onTransferSelected(PutioTransfer transfer) { }
    };
	
    private Callbacks mCallbacks = sDummyCallbacks;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);

		utils = ((PutioApplication) getActivity().getApplication()).getPutioUtils();
        utils.getEventBus().register(this);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.transfers, container, false);
		
		listview = (ListView) view.findViewById(R.id.transferslist);
        PutioUtils.padForFab(listview);

		adapter = new TransfersAdapter(getActivity(), transfers);
		listview.setAdapter(adapter);
		listview.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		registerForContextMenu(listview);
		listview.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View view, int position, long id) {
				if (transfers.get(position).status.equals("COMPLETED")
                        || transfers.get(position).status.equals("SEEDING")) {
					mCallbacks.onTransferSelected(transfers.get(position));
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
				if (transfers == null || transfers.isEmpty()) {
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
		
		menu.setHeaderTitle(transfers.get(info.position).name);
	    MenuInflater inflater = getActivity().getMenuInflater();
	    inflater.inflate(R.menu.context_transfers, menu);

        MenuItem itemRetry = menu.findItem(R.id.context_retry);
        PutioTransfer transfer = transfers.get(info.position);
        if (transfer.status.equals("ERROR")) {
            itemRetry.setVisible(true);
            itemRetry.setEnabled(true);
        } else {
            itemRetry.setVisible(false);
            itemRetry.setEnabled(false);
        }
	}
	
	@Override
	public boolean onContextItemSelected(android.view.MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
			case R.id.context_remove:
				initRemoveTransfer(transfers.get(info.position));
				return true;
            case R.id.context_retry:
                initRetryTransfer(transfers.get(info.position));
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

    private void initRetryTransfer(PutioTransfer transfer) {
        utils.getJobManager().addJobInBackground(new PutioRestInterface.PostRetryTransferJob(
                utils, transfer.id));
    }

	private void initRemoveTransfer(PutioTransfer transfer) {
		if (transfer.status.equals("COMPLETED")) {
			utils.getJobManager().addJobInBackground(new PutioRestInterface.PostCancelTransferJob(
					utils, transfer.id));
		} else {
			utils.removeTransferDialog(getActivity(), transfer.id).show();
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
    }
    
    @Override
    public void onDetach() {    	
        super.onDetach();
        
        mCallbacks = sDummyCallbacks;
    }
	
	public void updateTransfers(List<PutioTransfer> transfers) {
        if (isAdded()) {
            this.transfers.clear();
            this.transfers.addAll(transfers);
            adapter.notifyDataSetChanged();
        }

		if (transfers == null || transfers.isEmpty()) {
			setViewMode(VIEWMODE_EMPTY);
		} else {
			setViewMode(VIEWMODE_LIST);
		}
	}

    @Override
    public void onResume() {
        super.onResume();

        Intent transfersServiceIntent = new Intent(getActivity(), PutioTransfersService.class);
        getActivity().startService(transfersServiceIntent);
        getActivity().bindService(transfersServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onPause() {
        super.onPause();

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

    public void onEventMainThread(PutioTransfersService.TransfersAvailable ta) {
        updateTransfers(transfersService.getTransfers());
    }
}