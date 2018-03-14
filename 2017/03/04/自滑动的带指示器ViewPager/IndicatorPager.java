package com.exercise.p.indicator_blog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Scroller;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by p on 2017/2/28.
 */

public class IndicatorPager extends FrameLayout implements ViewPager.OnPageChangeListener {
//    原生ViewPager
    private ViewPager pager;
//    自己实现的底部指示器
    private Indicator indicator;
    private Context mContext;
//    自动滑动停留时间
    private final int DELAY = 4000;
//    ViewPager当前页数
    private int mCurrentPosition = 0;
//    ViewPager总页数
    private int itemCount = 0;
//    配合Timer和mTask实现自动滑动
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            Log.i("Pager","MSG" + msg.what);
            if (msg.what == 0)
                pager.setCurrentItem(msg.what,false);
            else
                pager.setCurrentItem(msg.what,true);
        }
    };
//    计时任务
    private TimerTask task = new mTask();
    private Timer timer = new Timer();

    public IndicatorPager(Context context) {
        super(context);
        mContext = context;
        initPagerView();
    }

    public IndicatorPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initPagerView();
    }

    public IndicatorPager(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        initPagerView();
    }

    private void initPagerView() {
        pager = new ViewPager(mContext);
        ViewPager.LayoutParams params = new ViewPager.LayoutParams();
        params.width = ViewPager.LayoutParams.MATCH_PARENT;
        params.height = ViewPager.LayoutParams.MATCH_PARENT;
        pager.setLayoutParams(params);
//        设置PagerView页面切换监听器
        pager.addOnPageChangeListener(this);
//        设置PagerView左右缓存数量
        pager.setOffscreenPageLimit(5);

//        ViewPagerScroller是内部类，通过这个类，设置ViewPager切换过度时间，产生阻尼效果
        ViewPagerScroller scroller = new ViewPagerScroller(mContext);
//        这个是设置切换过渡时间为1.5秒
        scroller.setScrollDuration(1500);
        scroller.initViewPagerScroll(pager);

        addView(pager);
    }

//    添加指示器
    private void initIndicator(){
        indicator = new Indicator(mContext,itemCount);
        LayoutParams params1 = new LayoutParams
                (LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params1.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params1.bottomMargin = 20;
        addView(indicator,params1);
    }

    /**
     * 设置ViewPager的View内容，函数内部自动使用本类自定义的Adapter
     * 初始化itemCount之后，再根据数量初始化Indicator
     * @param pager_views View集合
     */
    public void setAdapterViews(ArrayList<View> pager_views) {
        pager.setAdapter(new mPagerAdapter(pager_views));
        itemCount = pager_views.size();
//        设置计时任务
        timer.schedule(task,DELAY,DELAY);
        initIndicator();
    }

    /**
     * 设置ViewPager的Adapter
     * 初始化itemCount之后，再根据数量初始化Indicator
     * @param adapterViews
     */
    public void setAdapterViews(PagerAdapter adapterViews){
        pager.setAdapter(adapterViews);
        itemCount = adapterViews.getCount();
//        设置计时任务
        timer.schedule(task,DELAY,DELAY);
        initIndicator();
    }

    /**
     * 内部类，实现Adapter
     */
    private class mPagerAdapter extends PagerAdapter {
        ArrayList<View> pager_views;

        public mPagerAdapter(ArrayList<View> pager_views) {
            this.pager_views = pager_views;
        }

        @Override
        public int getCount() {
            return pager_views.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(pager_views.get(position));
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(pager_views.get(position), position);
            return pager_views.get(position);
        }
    }

    /**
     * 实现ViewPager.OnPageChangeListener
     * @param position ViewPager当前页数
     * @param positionOffset 滑动比例
     * @param positionOffsetPixels 滑动像素
     */
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//        Log.i("Pager", "Position:" + position + "Offset:" + positionOffset);
        pager.getParent().requestDisallowInterceptTouchEvent(true);
        indicator.move(position,positionOffset);
    }

    @Override
    public void onPageSelected(int position) {
        mCurrentPosition = position;
        Log.i("Pager", "Position:" + position);
    }

    /**
     * ViewPager状态改变时回调
     * ViewPager.SCROLL_STATE_IDLE == 0：停止状态，什么都没干
     * ViewPager.SCROLL_STATE_DRAGGING == 1：ViewPager处于滑动状态
     * ViewPager.SCROLL_STATE_SETTLING == 2:滑动停止状态
     * 滑动ViewPager一次，状态变化为 1 -> 2 -> 0
     * @param state
     */
    @Override
    public void onPageScrollStateChanged(int state) {
        Log.i("Pager","Changed");
        if (state == ViewPager.SCROLL_STATE_DRAGGING) {
//            滑动状态时，取消计时器
            if (timer!=null) {
                timer.cancel();
                Log.i("Pager","cancel");
            }
            timer = null;
            task = null;
        }else if (state == ViewPager.SCROLL_STATE_IDLE) {
//            什么都没做的状态下，启动计时器
            if (timer == null&&task == null) {
                timer = new Timer();
                task = new mTask();
                timer.schedule(task, DELAY, DELAY);
            }
        }
        Log.i("Pager", "status:" + state);
    }

    /**
     * 计时任务
     */
    private class mTask extends TimerTask{

        @Override
        public void run() {
            if (itemCount!=0)
                handler.sendEmptyMessage((mCurrentPosition + 1)%itemCount);
        }
    }

    /**
     * 自定义指示器
     */
    private class Indicator extends LinearLayout{
        private Context mIndicatorContext;
        private int padding = 20;
        private int radius = 12;
        private int indicatorItemCount = 0;
        MoveView mMoveView;
        Paint mPaint;

        public Indicator(Context context) {
            super(context);
            this.mIndicatorContext = context;
            init();
        }

        public Indicator(Context context,int indicatorItemCount) {
            super(context);
            this.mIndicatorContext = context;
            this.indicatorItemCount = indicatorItemCount;
            init();
        }

        public Indicator(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.mIndicatorContext = context;
            init();
        }

        public Indicator(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            this.mIndicatorContext = context;
            init();
        }

        public void setIndicatorItemCount(int indicatorItemCount) {
            this.indicatorItemCount = indicatorItemCount;
        }

        public void move(int position,float positionOffset){
            LayoutParams params = (LayoutParams) mMoveView.getLayoutParams();
            params.leftMargin = (int) ((2 * radius + padding) * (position + positionOffset));
            mMoveView.setLayoutParams(params);
        }

        private void init() {
//            setOrientation(LinearLayout.HORIZONTAL);
            setWillNotDraw(false);
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setColor(Color.GRAY);
            mPaint.setAlpha(180);
            mMoveView = new MoveView(mIndicatorContext);
            addView(mMoveView);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(padding + (radius * 2 + padding) * indicatorItemCount, (padding + radius) * 2 );
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            Log.i("Indicator","count" + indicatorItemCount);
            for(int i = 0;i < indicatorItemCount;i++){
                canvas.drawCircle(radius + padding + radius * i *2 + padding * i,
                        radius + padding,radius,mPaint);
//                canvas.drawCircle(20,20,20,mPaint);
            }
        }

        private class MoveView extends View{

            public MoveView(Context context) {
                super(context);
            }

            public MoveView(Context context, AttributeSet attrs) {
                super(context, attrs);
            }

            public MoveView(Context context, AttributeSet attrs, int defStyleAttr) {
                super(context, attrs, defStyleAttr);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                setMeasuredDimension(padding * 2 + radius * 2, padding * 2 + radius * 2);
            }

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                setWillNotDraw(false);
                Paint paint = new Paint();
                paint.setAntiAlias(true);
                paint.setColor(Color.parseColor("#FFD54F"));
                canvas.drawCircle(radius + padding,radius + padding,radius,paint);
            }
        }
    }

    /**
     * 使用此类修改ViewPager切换过渡时间
     */
    private class ViewPagerScroller extends Scroller {
        // 过渡时间
        private int mScrollDuration = 2000;
        /**
         * 设置速度
         * @param duration
         */
        public void setScrollDuration(int duration){
            this.mScrollDuration = duration;
        }

        public ViewPagerScroller(Context context) {
            super(context);
        }

        public ViewPagerScroller(Context context, Interpolator interpolator) {
            super(context, interpolator);
        }

        public ViewPagerScroller(Context context, Interpolator interpolator, boolean flywheel) {
            super(context, interpolator, flywheel);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy, int duration) {
//            200是使用setCurrenItem设置ViewPager页面时的过渡时间
            if (duration == 200)
                super.startScroll(startX, startY, dx, dy, mScrollDuration);
            else
                super.startScroll(startX, startY, dx, dy, duration);
        }

        @Override
        public void startScroll(int startX, int startY, int dx, int dy) {
            super.startScroll(startX, startY, dx, dy, mScrollDuration);
        }

        public void initViewPagerScroll(ViewPager viewPager) {
            try {
                Field mScroller = ViewPager.class.getDeclaredField("mScroller");
                mScroller.setAccessible(true);
                mScroller.set(viewPager, this);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
