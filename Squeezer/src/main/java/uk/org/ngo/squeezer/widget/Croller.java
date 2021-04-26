package uk.org.ngo.squeezer.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

public class Croller extends com.sdsmdg.harjot.crollerTest.Croller {

    public Croller(Context context) {
        super(context);
    }

    public Croller(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Croller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        setIndicatorWidth((float) ((float) getWidth() / 64.0));
        super.onDraw(canvas);
    }
}
