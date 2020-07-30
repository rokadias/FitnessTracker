package saqibr.fitnesstracking;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressLint("ViewConstructor")
public class WeightMarkerView extends MarkerView {

    private final TextView tvContent;
    private final SimpleDateFormat mFormat = new SimpleDateFormat("MM/dd/YYYY", Locale.US);

    public WeightMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);

        tvContent = findViewById(R.id.tvContent);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        Object data = e.getData();

        String date = mFormat.format(new Date((long) e.getX()));
        if (data instanceof Float) {
            tvContent.setText(String.format("%s: %.2f lbs", date, data));
        } else {
            tvContent.setText(date);
        }

        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2), -getHeight());
    }
}
