package godau.fynn.dsbdirect.view.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import androidx.cardview.widget.CardView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.squareup.picasso.Picasso
import godau.fynn.dsbdirect.R
import godau.fynn.dsbdirect.util.Utility
import godau.fynn.dsbdirect.model.noticeboard.NewsItem
import godau.fynn.dsbdirect.model.noticeboard.Notice
import godau.fynn.dsbdirect.model.noticeboard.NoticeBoardItem
import godau.fynn.dsbdirect.view.ZoomImage
import humanize.Humanize
import humanize.time.TimeMillis
import org.sufficientlysecure.htmltextview.HtmlTextView
import java.lang.Error

/*
* NoticeAdapter
* Copyright (C) 2019 Jasper Michalke <jasper.michalke@jasmich.ml>
* Created by Jasper Michalke <jasper.michalke@jasmich.de> under license EUPL 1.2.
*
* Based on Adapter, Copyright (C) 2019 Fynn Godau
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <https://www.gnu.org/licenses/>.
*
* This software is not affiliated with heinekingmedia GmbH, the
* developer of the DSB platform.
*/

class NoticeAdapter(private val mContext: Context, private var mValues: List<NoticeBoardItem>, private val container : View)
    : ArrayAdapter<NoticeBoardItem>(mContext, R.layout.row_notice_card, mValues) {

    private lateinit var zoomImage: ZoomImage

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val notice = mValues[position]
        val u = Utility(mContext)

        // recycle convertView
        val rowView = convertView ?: inflater.inflate(R.layout.row_notice_card, parent, false);

        // fill title and date views with their contents
        fillField(notice.title, rowView.findViewById<View>(R.id.title) as HtmlTextView)
        fillField(Humanize.naturalTime(notice.date, TimeMillis.DAY), rowView.findViewById<View>(R.id.date) as HtmlTextView)


        val card = rowView.findViewById<CardView>(R.id.card)

        // color card
        card.setCardBackgroundColor(u.colorPrimary)

        // remove OnClickListener from card
        card.setOnClickListener(null)

        // remove existing views from recycled view
        val contentHolderView = rowView.findViewById<LinearLayout>(R.id.content)
        contentHolderView.removeAllViews()

        val textView = rowView.findViewById<TextView>(R.id.textContent)

        if (notice is Notice && notice.isImage()) {

            contentHolderView.minimumHeight = u.dpToPx(200)
            textView.visibility = View.GONE

            // add each image to view
            for (imageUrl in notice.data) {
                val preview = ImageView(mContext)

                preview.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, u.dpToPx(200)).apply {
                    gravity = Gravity.CENTER
                }
                preview.adjustViewBounds = true
                preview.scaleType = ImageView.ScaleType.CENTER_CROP

                try {
                    Picasso.get().load(imageUrl).into(preview)
                } catch (e: Error) {
                    e.printStackTrace()
                }

                ZoomImage(preview, container.findViewById(R.id.expanded_image), container, imageUrl)

                contentHolderView.addView(preview)
            }
        } else if (notice is Notice && notice.data.size == 1) {
            // Allow user to open content externally

            textView.visibility = View.VISIBLE
            textView.setTextColor(mContext.resources.getColor(R.color.textSecondaryOverPrimary))
            textView.text = mContext.getString(R.string.notice_board_content_not_displayable_openable)

            card.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(notice.data[0]))
                mContext.startActivity(intent)
            }
        } else if (notice is NewsItem) {
            // display news

            contentHolderView.minimumHeight = 0

            textView.visibility = View.VISIBLE
            textView.setTextColor(mContext.resources.getColor(R.color.textOverPrimary))
            textView.text = notice.message
        } else {
            // not displayable content and more than one – don't offer opening externally and wait for users to complain

            textView.visibility = View.VISIBLE
            textView.setTextColor(mContext.resources.getColor(R.color.textSecondaryOverPrimary))
            textView.text = mContext.getString(R.string.notice_board_content_not_displayable)
        }

        // remove divider
        val listView = parent as ListView
        listView.divider = null
        listView.dividerHeight = 0

        return rowView
    }

    private fun fillField(s: String?, v: HtmlTextView) {
        if (s != null && !s.isEmpty()) {
            v.visibility = View.VISIBLE
            v.setHtml(s)
        } else {
            v.visibility = View.GONE
        }
    }

    fun setData(collection: List<NoticeBoardItem>) {
        mValues = collection
        notifyDataSetChanged()
    }

    fun size(): Int {
        return mValues.size
    }

    operator fun get(index: Int): NoticeBoardItem {
        return mValues[index]
    }

}