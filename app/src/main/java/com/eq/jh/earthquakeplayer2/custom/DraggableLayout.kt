package com.eq.jh.earthquakeplayer2.custom

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import com.eq.jh.earthquakeplayer2.R
import com.eq.jh.earthquakeplayer2.constants.DebugConstant
import com.eq.jh.earthquakeplayer2.utils.ScreenUtils
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


/**
 * Copyright (C) 2020 LOEN Entertainment Inc. All rights reserved.
 *
 * Created by Invincible on 2020-03-28
 * 참고
 * https://m.blog.naver.com/PostView.nhn?blogId=pluulove84&logNo=100199406955&proxyReferer=https%3A%2F%2Fwww.google.co.kr%2F
 * https://darksilber.tistory.com/135
 * http://i5on9i.blogspot.com/2013/10/android-viewdraghelper.html
 * https://github.com/pedrovgs/DraggablePanel
 * https://github.com/ikew0ng/SwipeBackLayout/blob/master/library/src/main/java/me/imid/swipebacklayout/lib/ViewDragHelper.java
 * https://developer.android.com/training/gestures/viewgroup?hl=ko
 *
 */
class DraggableLayout : ViewGroup {
    companion object {
        const val TAG = "DraggableLayout"
        private const val MIN_SLIDING_DISTANCE_ON_CLICK = 10
        private const val SLIDE_TOP = 0f
        private const val SLIDE_BOTTOM = 1f

        @IntDef(
            STATE_MAXIMIZED,
            STATE_DRAGGING,
            STATE_MINIMIZED
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class State

        /**
         * State indicating dragView is maximized.
         */
        private const val STATE_MAXIMIZED = 1

        /**
         * State indicating dragView is now dragging.
         */
        private const val STATE_DRAGGING = 2

        /**
         * State indicating dragView is minimized.
         */
        private const val STATE_MINIMIZED = 3
    }

    @State
    private var dragViewState: Int = STATE_MAXIMIZED

    private var onDraggableListener: OnDraggableListener? = null

    fun setOnDraggableListener(onDraggableListener: OnDraggableListener?) {
        this.onDraggableListener = onDraggableListener
    }

    interface OnDraggableListener {
        fun onMaximized()
        fun onMinimized()
        fun onDragStart()
    }

    private lateinit var dragHelper: ViewDragHelper
    private lateinit var dragView: View // 상단뷰 : 유투브로 치면 영상이 나오는 곳
    private lateinit var infoView: View // 하단뷰 : 상단뷰를 제외한 나머지 뷰

    private var activePointerId = ViewDragHelper.INVALID_POINTER // 터치영역 확인을 위해서
    private var verticalDragRange = 0 // 영상뷰가 아래쪽으로 내려갈수 있는 범위
    private var verticalDragRate = 0f // 하단 recyclerview의 alpha값, 스크롤 다운 또는 업시 좌우 마진 변화를 측정을 위해서 사용

    private var offset = 0 // onViewPositionChanged 의 dy의 누적값을 저장하기 위한 값
    private var minimizedWidth = 0 // 상단뷰가 가장 작아졌을때 너비
    private var minimizedHeight = 0 // 상단뷰가 가장 작아졌을때 높이
    private var minimizedScaleX = 0f // 상단뷰가 가장 작아졌을때의 screen 너비에 대한 비율
    private var minimizedScaleY = 0f // 상단뷰가 가장 작아졌을때의 screen 높이에 대한 비율
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

    //attrs
    private var topViewId = View.NO_ID // eg) 유투브 상단 영상 나오는 곳 뷰 아이디
    private var bottomViewId = View.NO_ID // eg) 유투브 하단 정보 나오는 곳 뷰 아이디
    /**
     * 컨텐츠 이외의 영역 eg)타이틀, 하단 탭등 영역
     * 스크롤 하는 뷰 여기서는 tryCaptureView 가 중간아래인지 위인지 판단하기 위해서
     */
    private var minimizedMargin = Rect()
    private var windowMargin = 0f
    private var lastTouchActionDownXPosition = 0f

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.draggable_layout)

        topViewId = a.getResourceId(R.styleable.draggable_layout_top_view_id, 0)
        bottomViewId = a.getResourceId(R.styleable.draggable_layout_bottom_view_id, 0)
        minimizedMargin.left = a.getDimensionPixelSize(R.styleable.draggable_layout_top_view_margin_left, 0)
        minimizedMargin.right = a.getDimensionPixelSize(R.styleable.draggable_layout_top_view_margin_right, 0)
        minimizedMargin.bottom = a.getDimensionPixelSize(R.styleable.draggable_layout_top_view_margin_bottom, 0)
        minimizedHeight = a.getDimensionPixelSize(R.styleable.draggable_layout_minimized_height, ScreenUtils.dipToPixel(context, 65f))
        windowMargin = a.getDimension(R.styleable.draggable_layout_window_margin, 0f)

        Log.d(TAG, "constructor topViewId : $topViewId, bottomViewId : $bottomViewId")
        Log.d(TAG, "constructor minimizedMargin.left : ${minimizedMargin.left}")
        Log.d(TAG, "constructor minimizedMargin.right : ${minimizedMargin.right}")
        Log.d(TAG, "constructor minimizedMargin.bottom : ${minimizedMargin.bottom}")
        Log.d(TAG, "constructor minimizedHeight : $minimizedHeight")
        Log.d(TAG, "constructor windowMargin : $windowMargin")

        a.recycle()

        init()
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        Log.d(TAG, "onFinishInflate")

        dragView = findViewById(topViewId)
        infoView = findViewById(bottomViewId)
    }

    private fun init() {
        dragHelper = ViewDragHelper.create(this, 1f, dragCallback)
        minimizedWidth = ScreenUtils.getScreenWidth(context) - minimizedMargin.left - minimizedMargin.right
        Log.d(TAG, "init minimizedWidth : $minimizedWidth")

        val configuration = ViewConfiguration.get(context)
        minVelocity = configuration.scaledMinimumFlingVelocity // for debugging : value is 131 based on gallaxy note9
        touchSlop = configuration.scaledPagingTouchSlop // for debugging : value is 42 based on gallaxy note9
        Log.d(TAG, "init minVelocity : $minVelocity, touchSlop : $touchSlop")
    }

    private val dragCallback = object : ViewDragHelper.Callback() {
        // return true means this view is drag target
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            return child == dragView
        }

        // return new top position
        override fun clampViewPositionVertical(child: View, aTop: Int, dy: Int): Int {
            val startY = top + paddingTop // 전체뷰를 사용하기 때문에 무조건 0임.

            val newTop = min(max(startY, aTop), startY + verticalDragRange) // 움직일수 있는 최대 범위
            if (DebugConstant.DEBUG) {
                Log.d(TAG, "clampViewPositionVertical newTop : $newTop")
            }
            return newTop
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return verticalDragRange
        }

        /**
         * user가 손을 놓았을 경우 max로 만들지 min으로 만들지 판단하자
         * 위 아래로만 움직이기 때문에 yvel(y축 속도)만 필요함
         */
        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            super.onViewReleased(releasedChild, xvel, yvel)
            val startY = top + paddingTop
            if (DebugConstant.DEBUG) {
                Log.d(TAG, "onViewReleased xvel : $xvel, yvel : $yvel, minVelocity : $minVelocity")
            }
            if (yvel < 0 && yvel <= -minVelocity) { // 위로
                // maximize
                settleCapturedViewAt(releasedChild.left, startY)
            } else if (yvel > 0 && yvel >= minVelocity) { // 밑으로
                // minimize
                settleCapturedViewAt(releasedChild.left, startY + verticalDragRange)
            } else { // 천천히 움직이고 어느순간 손을 놓는 경우
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
            invalidate() // 다시 그린다.
        }

        private fun settleCapturedViewAt(finalLeft: Int, finalTop: Int) {
            dragHelper.settleCapturedViewAt(finalLeft, finalTop)
        }

        // if you decide to change the UI in onViewPositionChanged(), you should, definitely, call the invalidate() or requestLayout() to redraw
        // prefer invalidate()
        override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
            super.onViewPositionChanged(changedView, left, top, dx, dy)
            if (DebugConstant.DEBUG) {
                Log.d(TAG, "onViewPositionChanged left : $left, top : $top, dx : $dx, dy : $dy")
            }

            offset += dy
            verticalDragRate = if (verticalDragRange == 0) 0f else offset / verticalDragRange.toFloat()
            if (DebugConstant.DEBUG) {
                Log.d(TAG, "onViewPositionChanged offset : $offset, verticalDragRate : $verticalDragRate")
            }

            // 상단뷰를 조금씩 줄인다
            dragView.run {
                pivotX = 0f
                pivotY = height.toFloat()
                scaleX = computeScaleY()
                scaleY = computeScaleY()
                translationX = -minimizedMargin.right * verticalDragRate
            }
            // 하단뷰를 아래로 움직인다.
            infoView.offsetTopAndBottom(dy)
            // 상단뷰가 내려간 만큼 하단뷰의 투명도를 조금씩 낮춘다
            infoView.alpha = 1f - verticalDragRate

            invalidate() // 다시 그린다.
        }

        var mPrevDragState = ViewDragHelper.STATE_IDLE

        /**
         * 중요 : onTouchEvent 연관됨 maximized or minimized 인 경우는 뷰내부에 control 이 있을수 있어서 클릭처리를 할수 있어야 한다. 따라서 dragging 처리는 하면 안됨
         */
        override fun onViewDragStateChanged(state: Int) {
            if (DebugConstant.DEBUG) {
                Log.d(TAG, "onViewDragStateChanged mPrevDragState : $mPrevDragState, state : $state")
            }

            if (mPrevDragState != ViewDragHelper.STATE_IDLE && state == ViewDragHelper.STATE_IDLE) { // minimized or maximized
                if (offset >= verticalDragRange) {
                    offset = verticalDragRange // just in case offset is crooked

                    dragViewState = STATE_MINIMIZED
                    onDraggableListener?.onMinimized()
                } else {
                    dragViewState = STATE_MAXIMIZED
                    onDraggableListener?.onMaximized()
                }
                Log.d(TAG, "onViewDragStateChanged after offset : $offset")
                Log.d(TAG, "onViewDragStateChanged dragViewState : $dragViewState")
            }

            when (state) {
                ViewDragHelper.STATE_DRAGGING, ViewDragHelper.STATE_SETTLING -> {
                    onDraggableListener?.onDragStart()
                }
            }
            mPrevDragState = state
        }
    }

    private var initialMotionX = 0f
    private var initialMotionY = 0f
    /**
     * @return Return true to steal motion events from the children and have
     * them dispatched to this ViewGroup through onTouchEvent().
     * The current target will receive an ACTION_CANCEL event, and no further
     * messages will be delivered here.
     * 이건 패턴임.
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (ev.pointerCount > 1) { // no multi touch
            return false
        }

        val action = ev.actionMasked
        if (DebugConstant.DEBUG) {
            Log.d(TAG, "onInterceptTouchEvent action : $action")
        }

        val interceptDrag = dragHelper.shouldInterceptTouchEvent(ev)
        var interceptTap = false

        if (action != MotionEvent.ACTION_DOWN) {
            dragHelper.cancel()
            return false
        }

        activePointerId = ev.getPointerId(ev.actionIndex)
        if (activePointerId == ViewDragHelper.INVALID_POINTER) {
            return false
        }

        initialMotionX = ev.x
        initialMotionY = ev.y

        interceptTap = dragHelper.isViewUnder(dragView, ev.x.toInt(), ev.y.toInt())
        return interceptDrag || interceptTap
    }

    /**
     * @return Return true if the event was handled, false otherwise
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount > 1) { // no multi touch
            return false
        }

        val action = event.actionMasked
        Log.d(TAG, "onTouchEvent action : $action")
        if (DebugConstant.DEBUG) {
        }

        if (action == MotionEvent.ACTION_DOWN) {
            activePointerId = event.getPointerId(event.actionIndex)
        }

        if (activePointerId == ViewDragHelper.INVALID_POINTER) {
            return false
        }

        dragHelper.processTouchEvent(event)

        if (action == MotionEvent.ACTION_MOVE) {
            when (dragViewState) {
                STATE_MAXIMIZED, STATE_MINIMIZED -> {
                    Log.d(TAG, "onTouchEvent action move")
                    if (dragHelper.checkTouchSlop(ViewDragHelper.DIRECTION_VERTICAL, activePointerId)) {
                        dragViewState = STATE_DRAGGING
                    }
                }
            }
        }

        val isViewUnder = dragHelper.isViewUnder(dragView, event.x.toInt(), event.y.toInt())

//        analyzeTouchToMaximizeIfNeeded(event, isViewUnder)

        if (dragViewState == STATE_MAXIMIZED || dragViewState == STATE_MINIMIZED) {
            Log.d(TAG, "onTouchEvent dispatch mini max")
            dragView.dispatchTouchEvent(event)
        } else {
            Log.d(TAG, "onTouchEvent dispatch mini max not")
            dragView.dispatchTouchEvent(cloneMotionEventWithAction(event, MotionEvent.ACTION_CANCEL))
        }
        return isViewUnder || dragViewState == STATE_DRAGGING
    }

    /**
     *
     */
    private fun analyzeTouchToMaximizeIfNeeded(ev: MotionEvent, isDragViewHit: Boolean) {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchActionDownXPosition = ev.x
            }
            MotionEvent.ACTION_UP -> {
                val clickOffset = ev.x - lastTouchActionDownXPosition
                if (shouldMaximizeOnClick(ev, clickOffset, isDragViewHit)) {
                    if (dragViewState == STATE_MINIMIZED) {
//                        dragHelper.smoothSlideViewTo(dragView, left + paddingLeft, top + paddingTop)
//                        ViewCompat.postInvalidateOnAnimation(this)
                        maximize()
                    }
                }
            }
        }
    }

    private fun shouldMaximizeOnClick(ev: MotionEvent, deltaX: Float, isDragViewHit: Boolean): Boolean {
        return (abs(deltaX) < MIN_SLIDING_DISTANCE_ON_CLICK && ev.action != MotionEvent.ACTION_MOVE && isDragViewHit)
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
        return MotionEvent.obtain(event.downTime, event.eventTime, action, event.x, event.y, event.metaState)
    }

    private fun computeScaleX(): Float {
        return 1f + verticalDragRate * (minimizedScaleX - 1f)
    }

    private fun computeScaleY(): Float {
        return 1f + verticalDragRate * (minimizedScaleY - 1f)
    }

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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = measuredWidth - paddingLeft - paddingRight
        val height = measuredHeight - paddingTop - paddingBottom
        Log.d(TAG, "onMeasure ------------------------------------")
        Log.d(TAG, "onMeasure width : $width height : $height")

        val videoViewWidth = minimizedHeight * 16 / 9
        val restOfVideoViewWidth = width - videoViewWidth
        val ratio = (width.toFloat() / videoViewWidth.toFloat())
        Log.d(TAG, "onMeasure width : $width")
        Log.d(TAG, "onMeasure videoViewWidth : $videoViewWidth restOfVideoViewWidth : $restOfVideoViewWidth")
        Log.d(TAG, "onMeasure ratio : $ratio")

        val scaledRestOfVideoViewWidth = (restOfVideoViewWidth * ratio).toInt()
        Log.d(TAG, "onMeasure scaledRestOfVideoViewWidth : $scaledRestOfVideoViewWidth")

        var dragViewHeight: Int
        dragView.run {
            measure(
                MeasureSpec.makeMeasureSpec(width + scaledRestOfVideoViewWidth, MeasureSpec.EXACTLY),
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

        verticalDragRange = height - dragViewHeight - minimizedMargin.bottom
        Log.d(TAG, "onMeasure verticalDragRange : $verticalDragRange")
    }

    private var topContainerWidth = 0
    private var topContainerHeight = 0

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        Log.d(TAG, "onLayout l : $l, t : $t, r : rt, b : $b")
        var left = l + paddingLeft
        var top = t + paddingTop

        dragView.run {
            topContainerWidth = measuredWidth
            topContainerHeight = measuredHeight
            Log.d(TAG, "onLayout dragView measuredWidth : $measuredWidth measuredHeight : $measuredHeight")
            layout(left, top, left + measuredWidth, top + measuredHeight)

            minimizedScaleX = minimizedWidth / measuredWidth.toFloat()
            minimizedScaleY = minimizedHeight / measuredHeight.toFloat()
            Log.d(TAG, "onLayout dragView minimizedScaleX $minimizedScaleX, minimizedScaleY : $minimizedScaleY")
        }

        // infoView는 dragView height 만큼 밑으로 내리자
        infoView.run {
            Log.d(TAG, "onLayout infoView measuredWidth : $measuredWidth measuredHeight : $measuredHeight")
            layout(left, top + topContainerHeight, left + measuredWidth, top + topContainerHeight + measuredHeight)
        }
    }

    fun isMaximized() = (dragViewState == STATE_MAXIMIZED)
    fun isMinimized() = (dragViewState == STATE_MINIMIZED)

    private fun maximize() {
        smoothSlideTo(SLIDE_TOP)
    }

    private fun minimized() {
        smoothSlideTo(SLIDE_BOTTOM)
    }

    private fun smoothSlideTo(slideOffset: Float): Boolean {
        val topBound = paddingTop
        val leftBound = paddingLeft
        val y = (topBound + slideOffset * verticalDragRange).toInt()
        if (dragHelper.smoothSlideViewTo(dragView, leftBound, y)) {
            ViewCompat.postInvalidateOnAnimation(this)
            return true
        }
        return false
    }
}