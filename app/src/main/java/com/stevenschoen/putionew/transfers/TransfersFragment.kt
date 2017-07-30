package com.stevenschoen.putionew.transfers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Rect
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.Toast
import com.stevenschoen.putionew.PutioApplication
import com.stevenschoen.putionew.PutioTransfersService
import com.stevenschoen.putionew.PutioTransfersService.TransfersServiceBinder
import com.stevenschoen.putionew.PutioUtils
import com.stevenschoen.putionew.R
import com.stevenschoen.putionew.model.responses.BasePutioResponse
import com.stevenschoen.putionew.model.transfers.PutioTransfer
import com.trello.rxlifecycle.components.support.RxFragment
import com.trello.rxlifecycle.kotlin.bindToLifecycle
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import java.util.*

class TransfersFragment : RxFragment() {

    companion object {
        private val VIEWMODE_LIST = 1
        private val VIEWMODE_LISTOREMPTY = 2
        private val VIEWMODE_LOADING = -1
        private val VIEWMODE_EMPTY = -2
        private val VIEWMODE_NONETWORK = 3

        const val FRAGTAG_OPTIONS = "options"
    }

    var callbacks: Callbacks? = null

    private val transfers = ArrayList<PutioTransfer>()
    private var transfersListView: RecyclerView? = null
    private var adapter: TransfersAdapter? = null

    private var utils: PutioUtils? = null
    private var transfersService: PutioTransfersService? = null

    private var viewMode = 1

    private var loadingView: View? = null
    private var emptyView: View? = null
    private var noNetworkView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        utils = (activity.application as PutioApplication).putioUtils
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.transfers, container, false)

        transfersListView = view.findViewById<RecyclerView>(R.id.transferslist)
        transfersListView!!.layoutManager = LinearLayoutManager(
                context, LinearLayoutManager.VERTICAL, false)
        val padding = resources.getDimensionPixelSize(R.dimen.transfers_card_padding)
        transfersListView!!.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State?) {
                super.getItemOffsets(outRect, view, parent, state)
                if (parent.getChildAdapterPosition(view) != parent.adapter.itemCount - 1) {
                    outRect.bottom = padding
                }
            }
        })
        PutioUtils.padForFab(transfersListView)

        adapter = TransfersAdapter(transfers)
        transfersListView!!.adapter = adapter
        adapter!!.setOnItemClickListener { view, position ->
            val transfer = transfers[position]
            if (transfer.status == "COMPLETED" || transfer.status == "SEEDING") {
                callbacks?.onTransferSelected(transfers[position])
            }
        }
        adapter!!.setOnItemLongClickListener { view, position ->
            val transfer = transfers[position]
            showOptions(transfer)
        }

        loadingView = view.findViewById(R.id.transfers_loading)
        emptyView = view.findViewById(R.id.transfers_empty)
        noNetworkView = view.findViewById(R.id.transfers_nonetwork)

        setViewMode(VIEWMODE_LOADING)
        return view
    }

    private fun setViewMode(mode: Int) {
        if (mode != viewMode) {
            when (mode) {
                VIEWMODE_LIST -> {
                    transfersListView!!.visibility = View.VISIBLE
                    loadingView!!.visibility = View.GONE
                    emptyView!!.visibility = View.GONE
                    noNetworkView!!.visibility = View.GONE
                }
                VIEWMODE_LOADING -> {
                    transfersListView!!.visibility = View.INVISIBLE
                    loadingView!!.visibility = View.VISIBLE
                    emptyView!!.visibility = View.GONE
                    noNetworkView!!.visibility = View.GONE
                }
                VIEWMODE_EMPTY -> {
                    transfersListView!!.visibility = View.INVISIBLE
                    loadingView!!.visibility = View.GONE
                    emptyView!!.visibility = View.VISIBLE
                    noNetworkView!!.visibility = View.GONE
                }
                VIEWMODE_NONETWORK -> {
                    transfersListView!!.visibility = View.INVISIBLE
                    loadingView!!.visibility = View.GONE
                    emptyView!!.visibility = View.GONE
                    noNetworkView!!.visibility = View.VISIBLE
                }
                VIEWMODE_LISTOREMPTY -> if (transfers.isEmpty()) {
                    setViewMode(VIEWMODE_EMPTY)
                } else {
                    setViewMode(VIEWMODE_LIST)
                }
            }
            viewMode = mode
        }
    }

    private fun showOptions(transfer: PutioTransfer) {
        TransferOptionsFragment.newInstance(context, transfer).show(childFragmentManager, FRAGTAG_OPTIONS)
    }

    private fun hideOptionsIfShowing() {
        childFragmentManager.findFragmentByTag(FRAGTAG_OPTIONS)?.let { fragment ->
            fragment as TransferOptionsFragment
            fragment.dismiss()
        }
    }

    override fun onAttachFragment(childFragment: Fragment) {
        super.onAttachFragment(childFragment)
        when (childFragment.tag) {
            FRAGTAG_OPTIONS -> {
                childFragment as TransferOptionsFragment
                childFragment.callbacks = object : TransferOptionsFragment.Callbacks {
                    override fun onRetrySelected() {
                        initRetryTransfer(childFragment.transfer)
                    }
                    override fun onRemoveSelected() {
                        initRemoveTransfer(childFragment.transfer)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater!!.inflate(R.menu.menu_transfers, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.menu_clearfinished -> {
                utils!!.restInterface
                        .cleanTransfers("")
                        .bindToLifecycle(this@TransfersFragment)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(makeUpdateNowSubscriber())
                return true
            }
        }

        return false
    }

    private fun initRetryTransfer(transfer: PutioTransfer) {
        utils!!.restInterface
                .retryTransfer(transfer.id)
                .bindToLifecycle(this@TransfersFragment)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(makeUpdateNowSubscriber())
        hideOptionsIfShowing()
    }

    private fun initRemoveTransfer(transfer: PutioTransfer) {
        if (transfer.status == "COMPLETED") {
            utils!!.restInterface
                    .cancelTransfer(PutioUtils.longsToString(transfer.id))
                    .bindToLifecycle(this@TransfersFragment)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(makeUpdateNowSubscriber())
        } else {
            utils!!.removeTransferDialog(activity, makeUpdateNowSubscriber(), transfer.id).show()
        }
        hideOptionsIfShowing()
    }

    private fun makeUpdateNowSubscriber(): Subscriber<BasePutioResponse> {
        return object : Subscriber<BasePutioResponse>() {
            override fun onCompleted() { }

            override fun onError(error: Throwable) {
                error.printStackTrace()
                Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show()
            }

            override fun onNext(response: BasePutioResponse) {
                if (transfersService != null) {
                    transfersService!!.updateNow()
                }
            }
        }
    }

    fun setHasNetwork(has: Boolean) {
        if (has) {
            setViewMode(VIEWMODE_LISTOREMPTY)
        } else {
            setViewMode(VIEWMODE_NONETWORK)
        }
    }

    fun updateTransfers(transfers: List<PutioTransfer>?) {
        if (isAdded) {
            this.transfers.clear()
            this.transfers.addAll(transfers!!)
            adapter!!.notifyDataSetChanged()
        }

        if (transfers == null || transfers.isEmpty()) {
            setViewMode(VIEWMODE_EMPTY)
        } else {
            setViewMode(VIEWMODE_LIST)
        }
    }

    override fun onResume() {
        super.onResume()

        val transfersServiceIntent = Intent(activity, PutioTransfersService::class.java)
        activity.startService(transfersServiceIntent)
        activity.bindService(transfersServiceIntent, mConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()

        if (transfersService != null) {
            activity.unbindService(mConnection)
        }
    }

    private val mConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TransfersServiceBinder
            transfersService = binder.service
            transfersService!!.transfersObservable.bindToLifecycle(this@TransfersFragment)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ result ->
                        updateTransfers(result)
                    }, { error ->
                        error.printStackTrace()
                    })
        }

        override fun onServiceDisconnected(name: ComponentName) { }
    }

    interface Callbacks {
        fun onTransferSelected(transfer: PutioTransfer)
    }
}