package com.stevenschoen.putionew.transfers;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.flipboard.bottomsheet.BottomSheetLayout;
import com.flipboard.bottomsheet.commons.MenuSheetView;
import com.stevenschoen.putionew.PutioApplication;
import com.stevenschoen.putionew.PutioTransfersService;
import com.stevenschoen.putionew.PutioTransfersService.TransfersServiceBinder;
import com.stevenschoen.putionew.PutioUtils;
import com.stevenschoen.putionew.R;
import com.stevenschoen.putionew.TransfersAdapter;
import com.stevenschoen.putionew.model.responses.BasePutioResponse;
import com.stevenschoen.putionew.model.transfers.PutioTransfer;
import com.trello.rxlifecycle.components.support.RxFragment;

import java.util.ArrayList;
import java.util.List;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public final class Transfers extends RxFragment {

	private List<PutioTransfer> transfers = new ArrayList<>();
	private RecyclerView transfersListView;
	private TransfersAdapter adapter;

	private PutioUtils utils;
	private PutioTransfersService transfersService;

	private int viewMode = 1;
	private static final int VIEWMODE_LIST = 1;
	private static final int VIEWMODE_LISTOREMPTY = 2;
	private static final int VIEWMODE_LOADING = -1;
	private static final int VIEWMODE_EMPTY = -2;
	private static final int VIEWMODE_NONETWORK = 3;

	private View loadingView;
	private View emptyView;
	private View noNetworkView;

	public interface Callbacks {
		void onTransferSelected(PutioTransfer transfer);
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
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.transfers, container, false);

		transfersListView = (RecyclerView) view.findViewById(R.id.transferslist);
		transfersListView.setLayoutManager(new LinearLayoutManager(
				getContext(), LinearLayoutManager.VERTICAL, false));
		final int padding = getResources().getDimensionPixelSize(R.dimen.transfers_card_padding);
		transfersListView.addItemDecoration(new RecyclerView.ItemDecoration() {
			@Override
			public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
				super.getItemOffsets(outRect, view, parent, state);
				if (parent.getChildAdapterPosition(view) != parent.getAdapter().getItemCount() - 1) {
					outRect.bottom = padding;
				}
			}
		});
		PutioUtils.padForFab(transfersListView);

		adapter = new TransfersAdapter(transfers);
		transfersListView.setAdapter(adapter);
		adapter.setOnItemClickListener(new TransfersAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(View view, int position) {
				PutioTransfer transfer = transfers.get(position);
				if (transfer.status.equals("COMPLETED") || transfer.status.equals("SEEDING")) {
					mCallbacks.onTransferSelected(transfers.get(position));
				}
			}
		});
		adapter.setOnItemLongClickListener(new TransfersAdapter.OnItemLongClickListener() {
			@Override
			public void onItemLongClick(View view, int position) {
				PutioTransfer transfer = transfers.get(position);
				showActions(transfer);
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
				transfersListView.setVisibility(View.VISIBLE);
				loadingView.setVisibility(View.GONE);
				emptyView.setVisibility(View.GONE);
				noNetworkView.setVisibility(View.GONE);
				break;
			case VIEWMODE_LOADING:
				transfersListView.setVisibility(View.INVISIBLE);
				loadingView.setVisibility(View.VISIBLE);
				emptyView.setVisibility(View.GONE);
				noNetworkView.setVisibility(View.GONE);
				break;
			case VIEWMODE_EMPTY:
				transfersListView.setVisibility(View.INVISIBLE);
				loadingView.setVisibility(View.GONE);
				emptyView.setVisibility(View.VISIBLE);
				noNetworkView.setVisibility(View.GONE);
				break;
			case VIEWMODE_NONETWORK:
				transfersListView.setVisibility(View.INVISIBLE);
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

	private void showActions(final PutioTransfer transfer) {
		final BottomSheetLayout sheet = (BottomSheetLayout) getActivity().findViewById(R.id.layout_bottomsheet);
		MenuSheetView menuSheetView = new MenuSheetView(getContext(),
				MenuSheetView.MenuType.LIST,
				transfer.name,
				new MenuSheetView.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						if (sheet.isSheetShowing()) sheet.dismissSheet();
						switch (item.getItemId()) {
							case R.id.context_remove:
								initRemoveTransfer(transfer);
								return true;
							case R.id.context_retry:
								initRetryTransfer(transfer);
							default:
								return false;
						}
					}
				});
		menuSheetView.inflateMenu(R.menu.context_transfers);
		sheet.showWithSheetView(menuSheetView);

		Menu sheetMenu = menuSheetView.getMenu();
		MenuItem itemRetry = sheetMenu.findItem(R.id.context_retry);
		if (transfer.status.equals("ERROR")) {
			itemRetry.setVisible(true);
			itemRetry.setEnabled(true);
		} else {
			itemRetry.setVisible(false);
			itemRetry.setEnabled(false);
		}
		menuSheetView.updateMenu();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_transfers, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_clearfinished:
				utils.getRestInterface().cleanTransfers("")
						.compose(this.<BasePutioResponse>bindToLifecycle())
						.subscribe(makeUpdateNowSubscriber());
				return true;
		}

		return false;
	}

	private void initRetryTransfer(PutioTransfer transfer) {
		utils.getRestInterface().retryTransfer(transfer.id)
				.compose(this.<BasePutioResponse>bindToLifecycle())
				.subscribe(makeUpdateNowSubscriber());
	}

	private void initRemoveTransfer(PutioTransfer transfer) {
		if (transfer.status.equals("COMPLETED")) {
			utils.getRestInterface().cancelTransfer(PutioUtils.longsToString(transfer.id))
					.compose(this.<BasePutioResponse>bindToLifecycle())
					.subscribe(makeUpdateNowSubscriber());
		} else {
			utils.removeTransferDialog(getActivity(), makeUpdateNowSubscriber(), transfer.id).show();
		}
	}

	private Subscriber<BasePutioResponse> makeUpdateNowSubscriber() {
		return new Subscriber<BasePutioResponse>() {
			@Override
			public void onCompleted() { }

			@Override
			public void onError(Throwable e) { }

			@Override
			public void onNext(BasePutioResponse response) {
				if (transfersService != null) {
					transfersService.updateNow();
				}
			}
		};
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
			transfersService.getTransfersObservable()
					.compose(Transfers.this.<List<PutioTransfer>>bindToLifecycle())
					.observeOn(AndroidSchedulers.mainThread())
					.subscribe(
							new Action1<List<PutioTransfer>>() {
								@Override
								public void call(List<PutioTransfer> result) {
									updateTransfers(result);
								}
							}, new Action1<Throwable>() {
								@Override
								public void call(Throwable throwable) {
									throwable.printStackTrace();
								}
							});
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {

		}
	};
}