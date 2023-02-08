package godau.fynn.dsbdirect.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StyleRes;
import godau.fynn.dsbdirect.R;
import godau.fynn.dsbdirect.util.Utility;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Wrapper class around AlertDialog.Builder to set the dialog to show
 * the filter configuration
 */
public class FilterConfigDialog {

    private final AlertDialog alertDialog;

    public FilterConfigDialog(Context context, @Nullable @StyleRes Integer style, @Nullable final Handler ok) {

        // spawn layout
        View promptView = LayoutInflater.from(context).inflate(R.layout.action_filter, null);

        // get sharedPreferences
        final SharedPreferences sharedPreferences1 = new Utility(context).getSharedPreferences();

        // find fields
        final EditText inputNumber = promptView.findViewById(R.id.input_number);
        final EditText inputLetter = promptView.findViewById(R.id.input_letter);
        final EditText inputCourses = promptView.findViewById(R.id.input_courses);
        final EditText inputName = promptView.findViewById(R.id.input_name);

        // fill in data from sharedPreferences

        Set<String> courseSet = sharedPreferences1.getStringSet("courses", new HashSet<String>());
        String[] courseArray = courseSet.toArray(new String[courseSet.size()]);
        String courseString = Utility.smartConcatenate(courseArray, " ");

        inputNumber.setText(sharedPreferences1.getString("number", ""));
        inputLetter.setText(sharedPreferences1.getString("letter", ""));
        inputCourses.setText(courseString);
        inputName.setText(sharedPreferences1.getString("name", ""));

        AlertDialog.Builder builder;
        if (style == null) {
            builder = new AlertDialog.Builder(context);
        } else {
            builder = new AlertDialog.Builder(context, style);
        }
        alertDialog = builder
                .setView(promptView)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //removes all unnecessary spaces
                        String textInputCourses = inputCourses.getText().toString();
                        if (!textInputCourses.equals("")) {
                            if (textInputCourses.charAt(0) == ' ')
                                textInputCourses = textInputCourses.substring(1, textInputCourses.length());
                        }

                        while (textInputCourses.contains("  "))
                            textInputCourses = textInputCourses.replaceAll("  ", " ");
                        String[] courses = textInputCourses.split(" ");

                        String number = inputNumber.getText().toString();
                        String letter = inputLetter.getText().toString();
                        String name = inputName.getText().toString();
                        sharedPreferences1.edit()
                                .putString("number", number)
                                .putString("letter", letter)
                                .putStringSet("courses", new HashSet<String>(Arrays.asList(courses)))
                                .putString("name", name)
                                .apply();

                        if (ok != null) {
                            ok.sendEmptyMessage(0);
                        }


                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(false) // user might accidentally click it away otherwise
                .setTitle(R.string.action_filter_popup_title)
                .setMessage(R.string.action_filter_popup_message)
                .create();


    }

    public void show() {
        alertDialog.show();
    }
}
