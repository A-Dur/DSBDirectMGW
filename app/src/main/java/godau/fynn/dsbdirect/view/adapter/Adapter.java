/*
 * DSBDirect
 * Copyright (C) 2019 Fynn Godau
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

package godau.fynn.dsbdirect.view.adapter;

import android.app.AlertDialog;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import godau.fynn.dsbdirect.R;
import godau.fynn.dsbdirect.util.Utility;
import godau.fynn.dsbdirect.model.entry.Entry;
import godau.fynn.dsbdirect.model.entry.InfoEntry;
import org.sufficientlysecure.htmltextview.HtmlTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private final Context mContext;
    private final ArrayList<Entry> mValues;
    private final String layout;

    private final LayoutInflater inflater;

    private final boolean shareOnHold;
    private final boolean longClickAddCourseActive;

    private Utility u;

    public static final String LAYOUT_LIST = "list";
    public static final String LAYOUT_CARDS = "cards";
    public static final String LAYOUT_CARDS_REVERSED = "cards reversed";

    public Adapter(Context context, ArrayList<Entry> values) {
        mContext = context;
        mValues = values;

        // which layout should be loaded?
        u = new Utility(mContext);
        layout = u.getSharedPreferences().getString("layout", LAYOUT_CARDS);

        inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // Enable long pressing to share unless super secret setting disables it
        shareOnHold = u.getSharedPreferences().getBoolean(Utility.SUPER_SECRET_SETTING_HOLD_TO_SHARE, true);
        longClickAddCourseActive = u.getSharedPreferences().getBoolean("pLongClickAddCourse", false);
    }



    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {


        // load correct layout
        switch (layout) {
            case LAYOUT_LIST:
                return new ViewHolder(inflater.inflate(R.layout.row_list, parent, false));
            case LAYOUT_CARDS_REVERSED:
                return new ViewHolder(inflater.inflate(R.layout.row_card_reversed, parent, false));
            case LAYOUT_CARDS:
            default:
                return new ViewHolder(inflater.inflate(R.layout.row_card, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        final Entry entry = mValues.get(position);

        holder.entry = entry;

        // populate fields

        if (entry instanceof InfoEntry) {
            holder.affectedClass.setVisibility(View.GONE);
            holder.lesson.setVisibility(View.GONE);
            holder.replacementTeacher.setVisibility(View.GONE);
            holder.info.setHtml(((InfoEntry) entry).getInfo());
        } else if (layout.equals(LAYOUT_LIST)) {
            Entry.CompatEntry concatenatedEntry = entry.getCompatEntry();
            fillFieldAlwaysVisible(concatenatedEntry.getAffectedClass(), holder.affectedClass);
            fillFieldAlwaysVisible(concatenatedEntry.getLesson(), holder.lesson);
            fillFieldAlwaysVisible(concatenatedEntry.getReplacementTeacher(), holder.replacementTeacher);
            fillFieldAlwaysVisible(concatenatedEntry.getInfo(), holder.info);
        } else if (layout.equals(LAYOUT_CARDS_REVERSED)){
            Entry.CompatEntry concatenatedEntry = entry.getCompatEntry();
            fillFieldHideIfNotUsed(concatenatedEntry.getAffectedClass(), holder.affectedClass);
            fillFieldHideIfNotUsed(concatenatedEntry.getLesson(), holder.lesson);
            fillFieldHideIfNotUsed(concatenatedEntry.getReplacementTeacherReversedCards(), holder.replacementTeacher);

            fillFieldHideIfNotUsed(concatenatedEntry.getInfo(), holder.info);
        }else {
            Entry.CompatEntry concatenatedEntry = entry.getCompatEntry();
            fillFieldHideIfNotUsed(concatenatedEntry.getAffectedClass(), holder.affectedClass);
            fillFieldHideIfNotUsed(concatenatedEntry.getLesson(), holder.lesson);
            fillFieldHideIfNotUsed(concatenatedEntry.getReplacementTeacher(), holder.replacementTeacher);
            fillFieldHideIfNotUsed(concatenatedEntry.getInfo(), holder.info);
        }

        // special code for cards
        if (layout.equals(LAYOUT_CARDS) || layout.equals(LAYOUT_CARDS_REVERSED)) {

            // color card according to status
            if (entry.isHighlighted()) {
                // highlight card
                holder.card.setCardBackgroundColor(u.getColorAccent());
            } else {
                // un-highlight card
                holder.card.setCardBackgroundColor(u.getColorPrimary());
            }
        } else {
            // highlighting code for list

            // Get color from attr
            TypedValue typedColor = new TypedValue();
            mContext.getTheme().resolveAttribute(R.attr.TextOverBackground, typedColor, true);

            int color = entry.isHighlighted() ? u.getColorAccent() :
                    typedColor.data;

            holder.affectedClass.setTextColor(color);
            holder.lesson.setTextColor(color);
            holder.replacementTeacher.setTextColor(color);
            holder.info.setTextColor(color);
        }

        try {
            if (position != 0 && !mValues.get(position - 1).getDate().equals(entry.getDate())) {
                String date = u.formatDate(entry.getDate());
                holder.date.setText(date);
                holder.date.setVisibility(View.VISIBLE);
            } else {
                holder.date.setVisibility(View.GONE);
            }
        } catch (NullPointerException e) {
            e.printStackTrace(); // Bad parser
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    private void fillFieldAlwaysVisible(String s, HtmlTextView v) {

        if (v.getVisibility() != View.VISIBLE)
            v.setVisibility(View.VISIBLE);

        if (s == null)
            v.setHtml("");
        else
            v.setHtml(s);
    }

    private void fillFieldHideIfNotUsed(String s, HtmlTextView v) {
        if (s != null && !s.isEmpty()) {
            v.setVisibility(View.VISIBLE);
            v.setHtml(s);
        } else {
            v.setVisibility(View.GONE);
        }
    }

    public void addAll(ArrayList<Entry> collection) {
        int initialSize = getItemCount();
        mValues.addAll(collection);
        notifyItemRangeInserted(initialSize, collection.size());
    }

    public Entry get(int index) {
        return mValues.get(index);
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final HtmlTextView affectedClass, lesson, replacementTeacher, info;
        private final TextView date;
        private final CardView card;

        private Entry entry;

        ViewHolder(@NonNull View itemView) {
            super(itemView);

            affectedClass = itemView.findViewById(R.id.affectedClass);
            lesson = itemView.findViewById(R.id.lesson);
            replacementTeacher = itemView.findViewById(R.id.replacementTeacher);
            info = itemView.findViewById(R.id.info);

            date = itemView.findViewById(R.id.date);

            if (layout.equals(LAYOUT_CARDS) || layout.equals(LAYOUT_CARDS_REVERSED))
                card = itemView.findViewById(R.id.card);
            else
                card = null;

            // Share on long click when long click to add course setting is disabled
            if (shareOnHold && !longClickAddCourseActive) itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {

                        // Find entry to which this ViewHolder belongs

                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_TEXT, entry.getShareText(new Utility(mContext)));
                        shareIntent.setType("text/plain");
                        mContext.startActivity(
                                Intent.createChooser(shareIntent, mContext.getString(R.string.share_title))
                        );

                        return true;
                    }
                });


            //Adds and removes courses by longclicking them when setting is enabled
            if (longClickAddCourseActive) itemView.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View view){
                    Set<String> courseSet = u.getSharedPreferences().getStringSet("courses", new HashSet<String>());
                    String[] courseArray = courseSet.toArray(new String[courseSet.size()]);
                    String courseString = Utility.smartConcatenate(courseArray, " ");

                    String entryCourse = entry.getCompatEntry().getSubject();
                    entryCourse = entryCourse.replaceAll("</*strike>", "");

                    if (entryCourse.length() == 0) {
                        entryCourse = entry.getCompatEntry().getOldSubject();
                        entryCourse = entryCourse.replaceAll("</*strike>", "");
                    }

                    //this if-statement removes an existing course by longclicking the entry
                    if (Arrays.asList(courseArray).contains(entryCourse)) {
                        courseString = courseString.replaceAll(entryCourse, "");
                        courseString = courseString.replaceAll("  ", " ");
                        if (!courseString.equals("") && courseString.charAt(0) == ' ') {
                                courseString = courseString.substring(1, courseString.length());
                        }

                        String[] courses1 = courseString.split(" ");
                        u.getSharedPreferences().edit()
                                .putStringSet("courses", new HashSet<String>(Arrays.asList(courses1)))
                                .apply();

                        Toast.makeText(mContext, entryCourse + " " + mContext.getString(R.string.removed), Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    courseString += " " + entryCourse;
                    if (courseString.charAt(0) == ' ') {
                        courseString = courseString.substring(1, courseString.length());
                    }
                    String[] courses = courseString.split(" ");
                    u.getSharedPreferences().edit()
                            .putStringSet("courses", new HashSet<String>(Arrays.asList(courses)))
                            .apply();
                    Toast.makeText(mContext, entryCourse + " " + mContext.getString(R.string.added), Toast.LENGTH_SHORT).show();
                    return true;

                }
                //inputCourses.setText(courseString);
                //String[] courses = inputCourses.getText().toString().split(" ");
            });
        }
    }
}
