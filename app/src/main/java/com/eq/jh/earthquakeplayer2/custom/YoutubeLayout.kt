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
class YoutubeLayout : ViewGroup {
    companion object {
        const val TAG = "YoutubeLayout"
        private const val MIN_SLIDING_DISTANCE_ON_CLICK = 10
        private const val SLIDE_TOP = 0f
        private const val SLIDE_BOTTOM = 1f

        private const val STATE_MAXIMIZED = 1
        private const val STATE_MINIMIZED = 2
    }

    interface OnDraggableListener {
        fun onMaximized()
        fun onMinimized()
        fun onDragStart()
    }

    private var onDraggableListener: OnDraggableListener? = null

    fun setOnDraggableListener(onDraggableListener: OnDraggableListener?) {
        this.onDraggableListener = onDraggableListener
    }

    private var playerViewId = View.NO_ID
    private var infoViewId = View.NO_ID
    private lateinit var playerView: View
    private lateinit var infoView: View

    private var playerViewMarginLeft = 0f
    private var playerViewMarginRight = 0f
    private var playerViewMarginBottom = 0f

    private lateinit var viewDragHelper: ViewDragHelper

    private var activePointerId = ViewDragHelper.INVALID_POINTER // 터치영역 확인을 위해서
    private var verticalDragRange = 0 // playerView가 y축으로 움직일수 있는 범위
    private var verticalDragRate = 0f // 스크롤시 infoView의 alpha값 변화 및 playerView의 width 가 좌우 마진에 의해 축소되는 변화를 측정을 위해서 사용

    private var totalScrolledY = 0 // onViewPositionChanged 의 dy의 누적값을 저장하기 위한 값
    private var minimizedWidth = 0 // playerView가 가장 작아졌을때 너비
    private var minimizedHeight = 0f // playerView가 가장 작아졌을때 높이
    private var minimizedScaleX = 0f // playerView가 가장 작아졌을때의 원래 playerView 너비에 대한 비율
    private var minimizedScaleY = 0f // playerView가 가장 작아졌을때의 원래 playerView 높이에 대한 비율

    private var viewDragState: Int = STATE_MAXIMIZED

    /**
     * Minimum velocity to initiate a fling, as measured in pixels per second
     * ViewConfiguration 에서 정해 놓은 값을 사용하자
     */
    private var minVelocity = 0

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.youtube_view)
        playerViewId = a.getResourceId(R.styleable.youtube_view_youtube_top_view_id, 0)
        infoViewId = a.getResourceId(R.styleable.youtube_view_youtube_bottom_view_id, 0)
        playerViewMarginLeft = a.getDimension(R.styleable.youtube_view_youtube_margin_left, 0f)
        playerViewMarginRight = a.getDimension(R.styleable.youtube_view_youtube_margin_right, 0f)
        playerViewMarginBottom = a.getDimension(R.styleable.youtube_view_youtube_margin_bottom, 0f)
        minimizedHeight = a.getDimension(R.styleable.youtube_view_youtube_minimized_height, resources.getDimension(R.dimen.video_default_minimized_height))
        init()
    }

    private fun init() {
        viewDragHelper = ViewDragHelper.create(this, 1f, object : ViewDragHelper.Callback() {
            override fun tryCaptureView(child: View, pointerId: Int): Boolean {
                return child == playerView
            }

            override fun getViewVerticalDragRange(child: View): Int {
                return verticalDragRange
            }

            override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
                val topBound = paddingTop // 전체뷰를 사용하기 때문에 무조건 0임.

                val newTop = min(max(topBound, top), topBound + verticalDragRange) // 움직일수 있는 최대 범위
                if (DebugConstant.DEBUG) {
                    Log.d(TAG, "clampViewPositionVertical newTop : $newTop")
                }
                return newTop
            }

            override fun onViewPositionChanged(changedView: View, left: Int, top: Int, dx: Int, dy: Int) {
                super.onViewPositionChanged(changedView, left, top, dx, dy)
                if (DebugConstant.DEBUG) {
                    Log.d(TAG, "onViewPositionChanged left : $left, top : $top, dx : $dx, dy : $dy")
                }

                totalScrolledY += dy
                verticalDragRate = if (verticalDragRange == 0) 0f else totalScrolledY / verticalDragRange.toFloat()
                if (DebugConstant.DEBUG) {
                    Log.d(TAG, "onViewPositionChanged totalScrolledY : $totalScrolledY, verticalDragRate : $verticalDragRate")
                }

                // 상단뷰를 조금씩 줄인다. View의 좌, 하단 기준으로 x, y축을 움직인다.
                playerView.run {
                    pivotX = width.toFloat()
                    pivotY = height.toFloat()
                    scaleX = computeScaleY()
                    scaleY = computeScaleY()
                    translationX = -playerViewMarginRight * verticalDragRate
                }

                // 하단뷰를 아래로 움직인다.
                infoView.offsetTopAndBottom(dy)
                // 상단뷰가 내려간 만큼 하단뷰의 투명도를 조금씩 낮춘다
                infoView.alpha = 1f - verticalDragRate

                invalidate() // 다시 그린다.
            }

            override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
                super.onViewReleased(releasedChild, xvel, yvel)
                val startY = paddingTop
                if (DebugConstant.DEBUG) {
                    Log.d(TAG, "onViewReleased xvel : $xvel, yvel : $yvel, minVelocity : $minVelocity")
                }
                if (yvel < 0 && yvel <= -minVelocity) { // 위로
                    // maximize
                    viewDragHelper.settleCapturedViewAt(releasedChild.left, startY)
                } else if (yvel > 0 && yvel >= minVelocity) { // 밑으로
                    // minimize
                    viewDragHelper.settleCapturedViewAt(releasedChild.left, startY + verticalDragRange)
                } else { // 천천히 움직이고 어느순간 손을 놓는 경우
                    if (playerView.scaleY < 1f - minimizedScaleY / 2f) {
                        // minimize
                        viewDragHelper.settleCapturedViewAt(releasedChild.left, startY + verticalDragRange)
                    } else {
                        // maximize
                        viewDragHelper.settleCapturedViewAt(releasedChild.left, startY)
                    }
                }
                invalidate() // 다시 그린다.
            }

            var prevDragState = ViewDragHelper.STATE_IDLE

            /**
             * 중요 : onTouchEvent 연관됨 maximized or minimized 인 경우는 뷰내부에 control 이 있을수 있어서 클릭처리를 할수 있어야 한다. 따라서 dragging 처리는 하면 안됨
             */
            override fun onViewDragStateChanged(state: Int) {
                if (DebugConstant.DEBUG) {
                    Log.d(TAG, "onViewDragStateChanged prevDragState : $prevDragState, state : $state")
                }

                if (prevDragState != ViewDragHelper.STATE_IDLE && state == ViewDragHelper.STATE_IDLE) { // minimized or maximized
                    if (totalScrolledY >= verticalDragRange) {
                        totalScrolledY = verticalDragRange // just in case offset is crooked
                        viewDragState = STATE_MINIMIZED
                        onDraggableListener?.onMinimized()
                    } else {
                        viewDragState = STATE_MAXIMIZED
                        onDraggableListener?.onMaximized()
                    }
                    if (DebugConstant.DEBUG) {
                        Log.d(TAG, "onViewDragStateChanged after totalScrolledY : $totalScrolledY")
                        Log.d(TAG, "onViewDragStateChanged dragViewState : $viewDragState")
                    }
                }

                /**
                 * ViewDragHelper.STATE_DRAGGING 처리 금지
                 * ViewDragHelper.STATE_DRAGGING 처리하면 클릭이 안됨
                 * ViewDragHelper.STATE_DRAGGING 처리하면 ViewDragHelper 의 shouldInterceptTouchEvent 에서 intercept를 true해버려서 자식뷰 이벤트를 가로챔
                 */
                if (state == ViewDragHelper.STATE_SETTLING) {
                    onDraggableListener?.onDragStart()
                }
                prevDragState = state
            }
        })

        minimizedWidth = (ScreenUtils.getScreenWidth(context).toFloat() - playerViewMarginLeft - playerViewMarginRight).toInt()
        val configuration = ViewConfiguration.get(context)
        minVelocity = (configuration.scaledMinimumFlingVelocity * 1.5).toInt() // 미세하게 움직여도 반응하기 때문에 크기를 약간 늘림
        if (DebugConstant.DEBUG) {
            Log.d(TAG, "init minVelocity : $minVelocity")
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()
        playerView = findViewById(playerViewId)
        infoView = findViewById(infoViewId)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = measuredWidth - paddingLeft - paddingRight
        val height = measuredHeight - paddingTop - paddingBottom
        if (DebugConstant.DEBUG) {
            Log.d(TAG, "onMeasure ------------------------------------")
            Log.d(TAG, "onMeasure width : $width height : $height")
        }

        playerView.measure( // playerView의 가로세로 비율은 16:9
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(width * 9 / 16, MeasureSpec.EXACTLY)
        )

        infoView.measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(if (height > playerView.measuredHeight) height - playerView.measuredHeight else 0, MeasureSpec.EXACTLY)
        )

        verticalDragRange = height - playerView.measuredHeight - playerViewMarginBottom.toInt()
        if (DebugConstant.DEBUG) {
            Log.d(TAG, "onMeasure verticalDragRange : $verticalDragRange")
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (DebugConstant.DEBUG) {
            Log.d(TAG, "onLayout ------------------------------------")
            Log.d(TAG, "onLayout l : $l, t : $t, r : $r, b : $b")
        }
        var left = l + paddingLeft
        var top = t + paddingTop + totalScrolledY

        var playerViewHeight: Int

        playerView.run {
            playerViewHeight = measuredHeight
            layout(left, top, left + measuredWidth, top + playerViewHeight)

            minimizedScaleX = minimizedWidth / measuredWidth.toFloat()
            minimizedScaleY = minimizedHeight / playerViewHeight.toFloat()
            if (DebugConstant.DEBUG) {
                Log.d(TAG, "onLayout playerView minimizedScaleX $minimizedScaleX, minimizedScaleY : $minimizedScaleY")
            }

            pivotY = width.toFloat()
            pivotY = height.toFloat()
            scaleX = computeScaleY()
            scaleY = computeScaleY()
        }

        // infoView는 dragView height 만큼 밑으로 내리자
        infoView.run {
            layout(left, top + playerViewHeight, left + measuredWidth, top + playerViewHeight + measuredHeight)
        }
    }

    private fun computeScaleX(): Float {
        return 1f + verticalDragRate * (minimizedScaleX - 1f)
    }

    private fun computeScaleY(): Float {
        return 1f + verticalDragRate * (minimizedScaleY - 1f)
    }

    fun isMaximized() = (viewDragState == STATE_MAXIMIZED)
    fun isMinimized() = (viewDragState == STATE_MINIMIZED)

    fun maximize() {
        smoothSlideTo(SLIDE_TOP)
    }

    fun minimized() {
        smoothSlideTo(SLIDE_BOTTOM)
    }

    private fun smoothSlideTo(slideOffset: Float): Boolean {
        val topBound = paddingTop
        val leftBound = paddingLeft
        val y = (topBound + slideOffset * verticalDragRange).toInt()
        if (viewDragHelper.smoothSlideViewTo(playerView, leftBound, y)) {
            ViewCompat.postInvalidateOnAnimation(this)
            return true
        }
        return false
    }

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

        if (disableDragViewItem.visibility == View.VISIBLE) { // seekbar 처리
            if (disableDraggingChecker.getItemValue(ev)) {
                return false
            }
        }

        when (action) {
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                viewDragHelper.cancel()
                return false
            }

            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(ev.actionIndex)
                if (activePointerId == ViewDragHelper.INVALID_POINTER) {
                    return false
                }
            }
        }

        val interceptDrag = viewDragHelper.shouldInterceptTouchEvent(ev)
        val interceptTap = viewDragHelper.isViewUnder(playerView, ev.x.toInt(), ev.y.toInt())

        return interceptDrag || interceptTap
    }

    /**
     * @return Return true if the event was handled, false otherwise
     */
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.pointerCount > 1) { // no multi touch
            return false
        }

        val action = ev.actionMasked
        if (DebugConstant.DEBUG) {
            Log.d(TAG, "onTouchEvent action : $action")
        }

        when (action) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = ev.getPointerId(ev.actionIndex)
            }
        }

        if (activePointerId == ViewDragHelper.INVALID_POINTER) {
            return false
        }

        viewDragHelper.processTouchEvent(ev)

        val isPlayerViewHit = isViewHit(playerView, ev.x.toInt(), ev.y.toInt())
        val isInfoViewHit = isViewHit(infoView, ev.x.toInt(), ev.y.toInt())

//        analyzeTouchToMaximizeIfNeeded(ev, isPlayerViewHit)

        when {
            isMaximized() -> {
                playerView.dispatchTouchEvent(ev)
            }
            isMinimized() -> {
                playerView.dispatchTouchEvent(transformMotionEventWithAction(playerView, ev, ev.action))
            }
            else -> {
                playerView.dispatchTouchEvent(cloneMotionEventWithAction(ev, MotionEvent.ACTION_CANCEL))
            }
        }

        return isPlayerViewHit || isInfoViewHit
    }

    /**
     * Calculate if one position is above any view.
     *
     * @param view to analyze.
     * @param x position.
     * @param y position.
     * @return true if x and y positions are below the view.
     */
    private fun isViewHit(view: View, x: Int, y: Int): Boolean {
        val viewLocation = IntArray(2)
        view.getLocationOnScreen(viewLocation)
        val parentLocation = IntArray(2)
        getLocationOnScreen(parentLocation)
        val screenX = parentLocation[0] + x
        val screenY = parentLocation[1] + y
        return screenX >= viewLocation[0]
                && screenX < viewLocation[0] + view.width
                && screenY >= viewLocation[1]
                && screenY < viewLocation[1] + view.height
    }

    private var lastTouchActionDownXPosition = 0f

    private fun analyzeTouchToMaximizeIfNeeded(ev: MotionEvent, isDragViewHit: Boolean) { // 이 처리는 리스너를 통해서 하는게 맞는것 같음. 안그럼 click이벤트를 뺐어감
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> lastTouchActionDownXPosition = ev.x
            MotionEvent.ACTION_UP -> {
                val clickOffset: Float = ev.x - lastTouchActionDownXPosition
                if (shouldMaximizeOnClick(ev, clickOffset, isDragViewHit)) {
                    if (isMinimized()) {
                        maximize()
                    }
                }
            }
        }
    }

    private fun shouldMaximizeOnClick(ev: MotionEvent, deltaX: Float, isPlayerViewHit: Boolean): Boolean {
        return (abs(deltaX) < MIN_SLIDING_DISTANCE_ON_CLICK && ev.action != MotionEvent.ACTION_MOVE && isPlayerViewHit)
    }

    /**
     * Clone given motion event and set specified action. This method is useful, when we want to
     * cancel event propagation in child views by sending event with android.view.MotionEvent#ACTION_CANCEL action.
     *
     * @param event event to clone
     * @param action new action
     * @return cloned motion event
     */
    private fun cloneMotionEventWithAction(event: MotionEvent, action: Int): MotionEvent {
        return MotionEvent.obtain(event.downTime, event.eventTime, action, event.x, event.y, event.metaState)
    }

    /**
     * player view에 있는 close 버튼을 누르면 차일드뷰 (여기서는 close 버튼)가 이벤트를 가져가야 한다.
     * child 처리로 변경해야 함
     */
    private fun transformMotionEventWithAction(view: View, event: MotionEvent, action: Int): MotionEvent {
        val scaleY = computeScaleY()
        val playerViewHeight = view.height * scaleY
        val closeButtonMarginRight = resources.getDimensionPixelSize(R.dimen.close_button_margin_right) * scaleY // related to x position
        val closeButtonMarginTop = resources.getDimensionPixelSize(R.dimen.close_button_margin_top) * scaleY
        val closeButtonHeight = resources.getDimensionPixelSize(R.dimen.close_button_height) * scaleY

        val marginTop = view.height - playerViewHeight
        val x = event.x - view.left.toFloat() - playerViewMarginLeft - playerViewMarginRight - closeButtonMarginRight
        val y = event.y - view.top.toFloat() - marginTop + closeButtonHeight + closeButtonMarginTop + playerViewMarginBottom

        return MotionEvent.obtain(event.downTime, event.eventTime, action, x, y, event.metaState)
    }

//    private fun dispatchTouchEventToChild(ev: MotionEvent) {
//        val playerViewGroup = playerView as ViewGroup
//        val count = playerViewGroup.childCount
//        Log.d(TAG, "dispatchTouchEventToChild count : $count")
//        if (count > 0) {
//            for (i in 0 until count) {
//                val childView = getChildAt(i)
//
//                val scaleY = computeScaleY()
//                val transformedViewWidth = childView.width * scaleY
//                val transformedViewHeight = childView.height * scaleY
//                val transformedMarginLeft = childView.marginLeft * scaleY
//                val transformedMarginTop = childView.marginTop * scaleY
//                val transformedMarginRight = childView.marginRight * scaleY
//                val transformedMarginBottom = childView.marginBottom * scaleY
//
//                Log.d(TAG, "dispatchTouchEventToChild transformedViewWidth : $transformedViewWidth, transformedViewHeight : $transformedViewHeight")
//                Log.d(
//                    TAG,
//                    "dispatchTouchEventToChild transformedMarginLeft : $transformedMarginLeft, transformedMarginTop : $transformedMarginTop, transformedMarginRight : $transformedMarginRight, transformedMarginBottom : $transformedMarginBottom"
//                )
//
////                val location = IntArray(2)
////                childView.getLocationOnScreen(location)
////
////
////                val x = ev.x.toInt()
////                val y = ev.y.toInt()
////
////                val isChildClicked = (x >= location[0]
////                        && x < location[0] + childView.width
////                        && y >= location[1]
////                        && y < location[1] + childView.height)
////                if (isChildClicked) {
////                    childView.dispatchTouchEvent(cloneMotionEventWithAction(ev, ev.action))
////                }
//            }
//        }
//    }

    /**
     * To ensure the animation is going to work this method has been override to call
     * postInvalidateOnAnimation if the view is not settled yet.
     *
     * settleCapturedViewAt 을 호출했을 때 animation 이 계속 되도록 하려면,
     * computeScroll 에서 아래와 같은 코드가 작성되어야 한다.
     */
    override fun computeScroll() {
        if (viewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    private lateinit var disableDragViewItem: View

    fun addDisableDraggingView(view: View) { // seekbar 처리가 필요함
        disableDragViewItem = view
    }

    /**
     * seek바 움직일때 뷰가 계속 흔들려서 처리함 (카카오 TV참고함)
     */
    private val disableDraggingChecker: RetrieveItemValue<MotionEvent, Boolean> = object : RetrieveItemValue<MotionEvent, Boolean> {
        private val DISABLE_TOUCH_TH = 50

        override fun getItemValue(ev: MotionEvent): Boolean {
            val location = IntArray(2)
            disableDragViewItem.getLocationOnScreen(location)

            val x = ev.x.toInt()
            val y = ev.y.toInt()

            return (x >= location[0] - DISABLE_TOUCH_TH
                    && x < location[0] + disableDragViewItem.width + DISABLE_TOUCH_TH
                    && y >= location[1] - DISABLE_TOUCH_TH
                    && y < location[1] + disableDragViewItem.height + DISABLE_TOUCH_TH)
        }
    }
}