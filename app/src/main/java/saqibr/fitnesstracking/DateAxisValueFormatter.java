package saqibr.fitnesstracking;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class DateAxisValueFormatter implements IAxisValueFormatter {
    private final SimpleDateFormat mFormat = new SimpleDateFormat("MM/dd/YYYY", Locale.US);

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        return mFormat.format(new Date((long) value));
    }
}
