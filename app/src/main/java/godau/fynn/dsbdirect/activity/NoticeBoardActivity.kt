package godau.fynn.dsbdirect.activity

// Created by Jasper Michalke <jasper.michalke@jasmich.de> under license EUPL 1.2.

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.View
import android.widget.ListView
import android.widget.TextView
import godau.fynn.dsbdirect.model.Login
import godau.fynn.dsbdirect.R
import godau.fynn.dsbdirect.util.Utility
import godau.fynn.dsbdirect.view.ZoomImage
import godau.fynn.dsbdirect.download.DsbAppDownloadManager
import godau.fynn.dsbdirect.persistence.LoginManager
import godau.fynn.dsbdirect.view.adapter.NoticeAdapter
import godau.fynn.dsbdirect.model.noticeboard.NoticeBoardItem


class NoticeBoardActivity : AppCompatActivity() {

    private lateinit var noticeAdapter: NoticeAdapter
    private lateinit var login: Login

    companion object {
        const val EXTRA_NOTICE_BOARD_ITEMS = "content"
        @JvmStatic var lastZoomedImage: ZoomImage? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notice_board)

        val u = Utility(this)
        u.stylize()

        val mLoginManager = LoginManager(this)

        login = mLoginManager.activeLogin
        // Start loading
        Thread(Runnable { loadNotices() }).start()

        val toolbar: Toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setBackgroundColor(u.colorPrimary)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    fun loadNotices() {
        var dManager = DsbAppDownloadManager(this)

        val content: List<NoticeBoardItem>
        if (intent.hasExtra(EXTRA_NOTICE_BOARD_ITEMS)) {
            content = intent.getSerializableExtra(EXTRA_NOTICE_BOARD_ITEMS) as List<NoticeBoardItem>
        } else {
            content = dManager.downloadNoticeBoardItems(login)
        }

        runOnUiThread {
            val listView = findViewById<ListView>(R.id.table)

            if (::noticeAdapter.isInitialized) {
                noticeAdapter.setData(content)
            } else {
                noticeAdapter = NoticeAdapter(this, content, findViewById(R.id.contentCoordinator))
                listView.adapter = noticeAdapter
            }
            listView.visibility = View.VISIBLE
            findViewById<TextView>(R.id.text).visibility = View.GONE

        }
    }

    override fun onBackPressed() {
        if (lastZoomedImage != null && lastZoomedImage!!.zoomed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            lastZoomedImage!!.zoomOut()
        } else
            super.onBackPressed()
    }
}