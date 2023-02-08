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

package godau.fynn.dsbdirect.view;

import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.util.AttributeSet;
import android.widget.TextView;
import androidx.preference.PreferenceViewHolder;
import godau.fynn.dsbdirect.util.Utility;

/**
 * To color the title programmatically. See https://stackoverflow.com/a/11607334
 */
public class PreferenceCategory extends androidx.preference.PreferenceCategory {

    private Context context;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public PreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
    }

    public PreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
    }

    public PreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public PreferenceCategory(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder viewHolder) {
        super.onBindViewHolder(viewHolder);

        // Can't test below lollipop but believing that it doesn't make sense below
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TextView titleView = viewHolder.itemView.findViewById(android.R.id.title);
            titleView.setTextColor(new Utility(context).getColorAccent());
        }
    }

}
