package com.eq.jh.earthquakeplayer2.custom

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import com.eq.jh.earthquakeplayer2.R
import com.eq.jh.earthquakeplayer2.utils.ScreenUtils
import java.lang.Float.isNaN
import kotlin.math.max
import kotlin.math.min


/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-28
 * https://m.blog.naver.com/PostView.nhn?blogId=pluulove84&logNo=100199406955&proxyReferer=https%3A%2F%2Fwww.google.co.kr%2F
 * https://darksilber.tistory.com/135
 * http://i5on9i.blogspot.com/2013/10/android-viewdraghelper.html
 * https://github.com/pedrovgs/DraggablePanel
 *
 */
class DraggableLayout : ViewGroup {
    companion object {
        const val TAG = "DraggableViewLayout"

        private const val INVALID_POINTER = -1

//        private const val VIDEO_STATUS_MAXIMIZED = 0
//        private const val VIDEO_STATUS_DRAGGING = 1
//        private const val VIDEO_STATUS_MINIMIZED = 2
//        private const val VIDEO_STATUS_CLOSED = 3
//
//        private const val DIRECTION_IDLE = 0
//        private const val DIRECTION_VERTICAL = ViewDragHelper.DIRECTION_VERTICAL
//        private const val DIRECTION_HORIZONTAL = ViewDragHelper.DIRECTION_HORIZONTAL
    }

    private lateinit var dragHelper: ViewDragHelper
    private lateinit var dragView: View // 상단뷰 : 유투브로 치면 영상이 나오는 곳
    private lateinit var infoView: View // 하단뷰 : 상단뷰를 제외한 나머지 뷰

    private var activePointerId = INVALID_POINTER
    private var verticalDragRange = 0 // 영상뷰가 아래쪽으로 내려갈수 있는 범위
    private var verticalDragRate = 0f // 하단 recyclerview의 alpha값 측정을 위해서 사용

    private var offsetVertical = 0 // onViewPositionChanged 의 dy의 누적값을 저장하기 위한 값
    private var minimizedHeight = 0 // 상단뷰가 가장 작아졌을때 높이
    private var minimizedScaleY = 0f
    /**
     * Minimum velocity to initiate a fling, as measured in pixels per second
     * ViewConfiguration 에서 정해 놓은 값을 사용하자
     */
    private var minVelocity = 0
    /**
     * Distance in pixels a touch can wander before we think the user is scrolling a full
     * ViewConfiguration 에서 정해 놓은 값을 사용하자
     */
    private var touchSlop = 0

    //attr
    private var topViewId = View.NO_ID // eg) 유투브 상단 영상 나오는 곳 뷰 아이디
    private var bottomViewId = View.NO_ID // eg) 유투브 하단 정보 나오는 곳 뷰 아이디
    /**
     * 컨텐츠 이외의 영역 eg)타이틀, 하단 탭등 영역
     * 스크롤 하는 뷰 여기서는 tryCaptureView 가 중간아래인지 위인지 판단하기 위해서
     */
    private var windowMargin = 0f

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.draggable_layout)

        topViewId = a.getResourceId(R.styleable.draggable_layout_top_view_id, 0)
        bottomViewId = a.getResourceId(R.styleable.draggable_layout_bottom_view_id, 0)
        windowMargin = a.getDimension(R.styleable.draggable_layout_window_margin, 0f)

        Log.d(TAG, "constructor topViewId : $topViewId, bottomViewId : $bottomViewId")
        Log.d(TAG, "constructor windowMargin : $windowMargin")

        a.recycle()

        init()
    }

    private fun init() {
        dragHelper = ViewDragHelper.create(this, 1f, dragCallback)
        minimizedHeight = ScreenUtils.dipToPixel(context, 80f)

        val configuration = ViewConfiguration.get(context)
        minVelocity = configuration.scaledMinimumFlingVelocity // for debugging 131
        touchSlop = configuration.scaledPagingTouchSlop // for debugging 42
        Log.d(TAG, "init minVelocity : $minVelocity, touchSlop : $touchSlop")
    }

    private val dragCallback = object : ViewDragHelper.Callback() {

        // return true means this view is drag target
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return child == dragView
        }

        // return new top position
        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            Log.d(TAG, "clampViewPositionVertical top : $top, dy : $dy")
            val startY = getTop() + paddingTop

            val newTop = min(max(startY, top), startY + verticalDragRange)
            Log.d(TAG, "clampViewPositionVertical newTop : $newTop")
            return newTop
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            Log.d(TAG, "clampViewPositionHorizontal left : $left, dx : $dx")
            return super.clampViewPositionHorizontal(child, left, dx)
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return verticalDragRange
        }

        /**
         * user가 손을 놓았을 경우 max로 만들지 min으로 만들지 판단하자
         */
        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
            val startY = top + paddingTop
            Log.d(TAG, "onViewReleased top : $top, paddingTop : $paddingTop, startY : $startY")

            Log.d(TAG, "onViewReleased xvel : $xvel, yvel : $yvel, minVelocity : $minVelocity")
            if (yvel < 0 && yvel <= -minVelocity) { // 위로
                // maximize
                settleCapturedViewAt(releasedChild.left, startY)
            } else if (yvel > 0 && yvel >= minVelocity) { // 밑으로
                // minimize
                settleCapturedViewAt(releasedChild.left, startY + verticalDragRange)
            } else { // 천천히 움직이고 어느순간 손을 놓는 경우
                Log.d(TAG, "onViewReleased releasedChild.top : ${releasedChild.top}")
                Log.d(TAG, "onViewReleased (ScreenUtils.getScreenHeight(context) - windowMargin - releasedChild.measuredHeight) / 2 : ${(ScreenUtils.getScreenHeight(context) - windowMargin - releasedChild.measuredHeight) / 2}")
                val moveHalf = (ScreenUtils.getScreenHeight(context) - windowMargin - releasedChild.measuredHeight) / 2
                // 상단뷰가 1/2보다 내려왔으면 min, vice versa
                if (moveHalf < releasedChild.top) {
                    // minimize
                    settleCapturedViewAt(releasedChild.left, startY + verticalDragRange)
                } else {
                    // maximize
                    settleCapturedViewAt(releasedChild.left, startY)
                }
            }
            invalidate()
        }

        private fun settleCapturedViewAt(finalLeft: Int, finalTop: Int) {
            dragHelper.settleCapturedViewAt(finalLeft, finalTop)
        }

        // if you decide to change the UI in onViewPositionChanged(), you should, definitely, call the invalidate() or requestLayout() to redraw
        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            Log.d(TAG, "onViewPositionChanged left : $left, top : $top, dx : $dx, dy : $dy")

            offsetVertical += dy
            verticalDragRate = if (verticalDragRange == 0) 0f else offsetVertical / verticalDragRange.toFloat()
            Log.d(TAG, "onViewPositionChanged offsetVertical : $offsetVertical, verticalDragRate : $verticalDragRate")

            infoView.alpha = 1f - verticalDragRate

            // 상단뷰를 조금씩 줄인다
            changedView.run {
                pivotX = width.toFloat()
                pivotY = height.toFloat()
                scaleX = 1f
                scaleY = computeScaleY()
            }
            // 하단뷰를 아래로 움직인다.
            infoView.offsetTopAndBottom(dy)
            // 상단뷰가 내려간 만큼 하단뷰의 투명도를 조금씩 낮춘다
            infoView.alpha = 1f - verticalDragRate

            invalidate() // 다시 그린다.
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        Log.d(TAG, "onFinishInflate")

        dragView = findViewById(topViewId)
        infoView = findViewById(bottomViewId)
    }

    private fun maximize() {
        if (dragHelper.smoothSlideViewTo(dragView, left + paddingLeft, top + paddingTop)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    private var initialMotionX = 0f
    private var initialMotionY = 0f
    /**
     * @return Return true to steal motion events from the children and have
     * them dispatched to this ViewGroup through onTouchEvent().
     * The current target will receive an ACTION_CANCEL event, and no further
     * messages will be delivered here.
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.pointerCount > 1) { // no multi touch
            return false
        }

        val action = ev.actionMasked
        Log.d(TAG, "onInterceptTouchEvent action : $action")

        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            dragHelper.cancel()
            return false
        }

        if (action != MotionEvent.ACTION_DOWN) {
            dragHelper.cancel()
            return false
        }

        activePointerId = ev.getPointerId(ev.actionIndex)
        if (activePointerId == INVALID_POINTER) {
            return false
        }

        initialMotionX = ev.x
        initialMotionY = ev.y

        val interceptTap = dragHelper.isViewUnder(dragView, ev.x.toInt(), ev.y.toInt())
        return dragHelper.shouldInterceptTouchEvent(ev) || interceptTap
    }

    /**
     * @return Return true if the event was handled, false otherwise
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1) { // no multi touch
            return false
        }

        dragHelper.processTouchEvent(event)

        val action = event.actionMasked
//        Log.d(TAG, "onTouchEvent action : $action")

        if (action == MotionEvent.ACTION_MOVE) {
            val dx = event.x - initialMotionX
            val dy = event.y - initialMotionY

            Log.d(TAG, "onTouchEvent dx : $dx, dy : $dy")

            /**
             * Pass the touch screen motion event down to the target view, or this
             * view if it is the target.
             *
             * @param event The motion event to be dispatched.
             * @return True if the event was handled by the view, false otherwise.
             */
            dragView.dispatchTouchEvent(cloneMotionEventWithAction(event, MotionEvent.ACTION_CANCEL))
        }

        return true
    }

    /**
     * Clone given motion event and set specified action. This method is useful, when we want to
     * cancel event propagation in child views by sending event with {@link
     * android.view.MotionEvent#ACTION_CANCEL}
     * action.
     *
     * @param event event to clone
     * @param action new action
     * @return cloned motion event
     */
    private fun cloneMotionEventWithAction(event: MotionEvent, action: Int): MotionEvent {
        return MotionEvent.obtain(
            event.downTime,
            event.eventTime,
            action,
            x,
            y,
            event.metaState
        )
    }

    private fun computeScaleY(): Float {
        return 1f + verticalDragRate * (minimizedScaleY - 1f)
    }

    // settleCapturedViewAt 을 호출했을 때 animation 이 계속 되도록 하려면,
    // computeScroll 에서 아래와 같은 코드가 작성되어야 한다.
    /**
     * To ensure the animation is going to work this method has been override to call
     * postInvalidateOnAnimation if the view is not settled yet.
     *
     * settleCapturedViewAt 을 호출했을 때 animation 이 계속 되도록 하려면,
     * computeScroll 에서 아래와 같은 코드가 작성되어야 한다.
     */
    override fun computeScroll() {
        if (dragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    private var topContainerWidth = 0
    private var topContainerHeight = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = measuredWidth - paddingLeft - paddingRight
        val height = measuredHeight - paddingTop - paddingBottom
        Log.d(TAG, "onMeasure width : $width height : $height")

        var dragViewHeight: Int
        dragView.run {
            measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(width * 9 / 16, MeasureSpec.EXACTLY)
            )
            dragViewHeight = measuredHeight
            Log.d(TAG, "onMeasure dragViewHeight : $dragViewHeight")
        }

        infoView.run {
            measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(if (height > dragViewHeight) height - dragViewHeight else 0, MeasureSpec.EXACTLY)
            )
        }

        verticalDragRange = height - dragViewHeight
        Log.d(TAG, "onMeasure verticalDragRange : $verticalDragRange")
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        Log.d(TAG, "onLayout l : $l, t : $t, r : rt, b : $b")
        var left = l + paddingLeft
        var top = t + paddingTop

        dragView.run {
            topContainerWidth = measuredWidth
            topContainerHeight = measuredHeight
            Log.d(TAG, "onLayout dragView topContainerHeight : $topContainerHeight")
            Log.d(TAG, "onLayout dragView measuredWidth : $measuredWidth measuredHeight : $measuredHeight")
            layout(left, top, left + measuredWidth, top + measuredHeight)

            minimizedScaleY = minimizedHeight / measuredHeight.toFloat()
            Log.d(TAG, "onLayout dragView minimizedScaleY : $minimizedScaleY")
            if (isNaN(minimizedScaleY)) {
                minimizedScaleY = 0f
            }
        }

        // infoView는 dragView height 만큼 밑으로 내리자
        infoView.run {
            Log.d(TAG, "onLayout infoView measuredWidth : $measuredWidth measuredHeight : $measuredHeight")
            layout(left, top + topContainerHeight, left + measuredWidth, top + topContainerHeight + measuredHeight)
        }
    }
}