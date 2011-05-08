package com.pocketjourney.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class TransparentPanel extends LinearLayout 
{ 
	private Paint	innerPaint, borderPaint ;
    
	public TransparentPanel(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public TransparentPanel(Context context) {
		super(context);
		init();
	}

	// TODO: Clean this up properly, use attributes so that layouts can
	// specify colours, opacity, and maybe border colour information.
	private void init() {
		innerPaint = new Paint();
		innerPaint.setARGB(140, 0, 0, 0); //gray
		innerPaint.setAntiAlias(true);

//		borderPaint = new Paint();
//		borderPaint.setARGB(255, 255, 255, 255);
//		borderPaint.setAntiAlias(true);
//		borderPaint.setStyle(Style.STROKE);
//		borderPaint.setStrokeWidth(2);
	}
	
	public void setInnerPaint(Paint innerPaint) {
		this.innerPaint = innerPaint;
	}

	public void setBorderPaint(Paint borderPaint) {
		this.borderPaint = borderPaint;
	}

    @Override
    protected void dispatchDraw(Canvas canvas) {
    	
//    	RectF drawRect = new RectF();
//    	drawRect.set(0,0, getMeasuredWidth(), getMeasuredHeight());
//    	canvas.drawRect(drawRect, innerPaint);
    	canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), innerPaint);
//    	canvas.drawRoundRect(drawRect, 5, 5, innerPaint);
//		canvas.drawRoundRect(drawRect, 5, 5, borderPaint);
		
		super.dispatchDraw(canvas);
    }
}