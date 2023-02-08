/*
 * DSBDirect
 * Copyright (C) 2020 Fynn Godau
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

package godau.fynn.dsbdirect.util;

import android.content.Context;
import android.content.Intent;
import godau.fynn.librariesdirect.AboutLibrariesActivity;
import godau.fynn.librariesdirect.ContextConsumer;
import godau.fynn.librariesdirect.Library;
import godau.fynn.librariesdirect.License;

public class AboutLibrariesConfig {

    public static Intent getIntent(Context context) {
        return new AboutLibrariesActivity.IntentBuilder(context).setLibraries(
                new Library[]{
                        new Library("jsoup", License.MIT_LICENSE, "Copyright © 2009 - 2019 Jonathan Hedley (jonathan@hedley.net)", "Jonathan Hedley", "https://jsoup.org"),
                        new Library("HorizontalPicker", License.APACHE_20_LICENSE, null, "Blaž Šolar", "http://blaz.solar/HorizontalPicker/"),
                        new Library("Humanize", License.APACHE_20_LICENSE, null, "mfornos", "http://mfornos.github.io/humanize/"),
                        new Library("TouchImageView", License.MIT_LICENSE, null, "Michael Ortiz", "https://github.com/MikeOrtiz/TouchImageView"),
                        new Library("HtmlTextView", License.APACHE_20_LICENSE, null, "Dominik Schürmann", "https://github.com/sufficientlysecure/html-textview"),
                        new Library("ShiftColorPicker", License.MIT_LICENSE, "The MIT License (MIT)\n\nCopyright (c) 2015 Bogdasarov Bogdan", "Bogdasarov Bogdan", "https://github.com/DASAR/ShiftColorPicker"),
                        new Library("Picasso", License.APACHE_20_LICENSE, null, "Square, Inc.", "https://square.github.io/picasso/"),
                        new Library("Conscrypt", License.APACHE_20_LICENSE, "This product contains a modified portion of `Netty`, a configurable network\n" +
                                "stack in Java, which can be obtained at:\n" +
                                "\n" +
                                "  * LICENSE:\n" +
                                "    * licenses/LICENSE.netty.txt (Apache License 2.0)\n" +
                                "  * HOMEPAGE:\n" +
                                "    * http://netty.io/\n" +
                                "\n" +
                                "This product contains a modified portion of `Apache Harmony`, modular Java runtime,\n" +
                                "which can be obtained at:\n" +
                                "\n" +
                                "  * LICENSE:\n" +
                                "    * licenses/LICENSE.harmony.txt (Apache License 2.0)\n" +
                                "  * HOMEPAGE:\n" +
                                "    * https://harmony.apache.org/", "The Android Open Source Project", "https://conscrypt.org/"),
                        new Library("librariesDirect", License.CC0_LICENSE, null, "Fynn Godau", "https://codeberg.org/fynngodau/librariesDirect"),
                        new Library("eltern-portal.org API", License.GNU_GPL_V3_OR_LATER_LICENSE, null, "Moritz Zwerger", "https://gitlab.bixilon.de/bixilon/eltern-portal.org-api"),
                })
                .setConsumer(new ContextConsumer() {
                    @Override
                    public void accept(Context context) {
                        new Utility(context).stylize();
                    }
                })
                .build();
    }
}
