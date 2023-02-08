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

package godau.fynn.dsbdirect.table.reader;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import godau.fynn.dsbdirect.util.Utility;
import godau.fynn.dsbdirect.model.entry.Entry;

import java.util.ArrayList;
import java.util.Queue;

public class ReaderRunnable implements Runnable {

    private String data;
    private String credentialsId;
    private Handler errorHandler;
    private Handler dataProcessedHandler;
    private Context context;
    private ArrayList<Entry> result = new ArrayList<>();
    private Queue<Runnable> tasks;

    public ReaderRunnable(Context context, String data, String credentialsId, Queue<Runnable> tasks) {
        super();

        this.data = data;
        this.context = context;
        this.credentialsId = credentialsId;
        this.tasks = tasks;
    }

    public void addHandlers(Handler dataProcessedHandler, Handler errorHandler) {
        this.dataProcessedHandler = dataProcessedHandler;
        this.errorHandler = errorHandler;
    }

    private Reader reader;

    @Override
    public void run() {

        Utility u = new Utility(context);

        try {
            reader = u.getReader(data, credentialsId);
            final ArrayList<Entry> entries;

            entries = Filter.filterUserFilters(reader.read(), context);
            Log.d("ENTRIES", String.valueOf(entries.size()));

            result = entries;

            dataProcessedHandler.sendEmptyMessage(0);

        } catch (Exception e) {
            e.printStackTrace();
            errorHandler.sendEmptyMessage(0);
        }

        // Remove self (first one in queue)
        tasks.remove();

        // Run next task if possible
        if (tasks.size() > 0) {
            tasks.element().run();
        }

    }

    public ArrayList<Entry> getResult() {
        return result;
    }

    public String getSchoolName() {
        return reader.getSchoolName();
    }
}
