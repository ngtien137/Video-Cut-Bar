package com.luza.videocutbar

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.text.TextPaint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import com.luza.videocutbar.util.LoadingTask
import com.luza.videocutbar.util.Log
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.abs


class VideoCutBar @JvmOverloads constructor(
    context: Context, var attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var loadingTask: LoadingTask? = null

    private var viewWidth = 0
    private var viewHeight = 0
    private var videoBarHeight = 0
    private var videoBarWidth = 0
    private var realWidth = 0
    private var realHeight = 0
    private var imageWidth = 0f

    private var lastFocusThumbIndex = -1

    var videoPath: String = ""
        set(value) {
            field = value
            setPath()
        }
    var duration = 0L
        set(value){
            field = value
            maxProgress = value
            if (minProgress>maxProgress)
                minProgress = 0
            invalidate()
        }
    private var progress = 0L
    var maxProgress = 0L
    var minProgress = 0L

    var formatDuration = SimpleDateFormat("ss", Locale.getDefault())

    private var drawableThumbLeft: Drawable? = null
    private var drawableThumbRight: Drawable? = null
    private var listBitmap: ArrayList<Bitmap> = ArrayList()

    private var indicatorTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private var indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var rectPadding = RectF()
    private var rectThumbLeft = Rect()
    private var rectThumbRight = Rect()
    private var rectOverlayLeft = RectF()
    private var rectOverlayRight = RectF()
    private var rectIndicator = RectF()
    private var rectTextIndicator = Rect()
    private var listRectDestination: ArrayList<RectF> = ArrayList()

    private var indicatorSize = 0
    private var indicatorCorners = 0F
    private var thumbWidth = 0
    private var numberPreviewImage = 8
    private var touchAreaExtra = 0
    private var paddingIndicator = 0
    private var paddingTextIndicator = 0

    var showThumbCut = true
    var loadingListener:ILoadingListener?=null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.VideoCutBar)
            videoBarHeight =
                ta.getDimensionPixelSize(R.styleable.VideoCutBar_vcb_video_bar_height, 0)
            numberPreviewImage = ta.getInt(R.styleable.VideoCutBar_vcb_number_image_preview,8)

            indicatorSize = ta.getDimensionPixelSize(R.styleable.VideoCutBar_vcb_indicator_size, 0)
            indicatorCorners =
                ta.getDimensionPixelSize(R.styleable.VideoCutBar_vcb_indicator_corners, 0).toFloat()
            paddingIndicator = ta.getDimensionPixelSize(R.styleable.VideoCutBar_vcb_indicator_padding_bottom,0)
            indicatorPaint.color =
                ta.getColor(R.styleable.VideoCutBar_vcb_indicator_color, Color.RED)

            indicatorTextPaint.color =
                ta.getColor(R.styleable.VideoCutBar_vcb_indicator_text_color, Color.RED)
            indicatorTextPaint.textSize =
                ta.getDimensionPixelSize(R.styleable.VideoCutBar_vcb_indicator_text_size, 0)
                    .toFloat()
            paddingTextIndicator = ta.getDimensionPixelSize(R.styleable.VideoCutBar_vcb_indicator_text_padding_bottom,0)

            thumbWidth = ta.getDimensionPixelSize(R.styleable.VideoCutBar_vcb_thumb_width, 0)
            touchAreaExtra = ta.getDimensionPixelSize(R.styleable.VideoCutBar_vcb_thumb_touch_extra_area,0)
            drawableThumbLeft =
                ta.getDrawable(R.styleable.VideoCutBar_vcb_thumb_left) ?: ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_thumb_left
                )
            drawableThumbRight = ta.getDrawable(R.styleable.VideoCutBar_vcb_thumb_right)
                ?: ContextCompat.getDrawable(context, R.drawable.ic_thumb_right)
            if (thumbWidth == 0 && drawableThumbLeft != null)
                thumbWidth = drawableThumbLeft!!.intrinsicWidth
            if (listBitmap.isEmpty()){
                val bitmap = ContextCompat.getDrawable(context,R.drawable.loading)!!.toBitmap()
                for (i in 0 until numberPreviewImage) {
                    listBitmap.add(bitmap)
                }
            }
            ta.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        rectPadding.left = 0f + paddingLeft
        rectPadding.top = 0f + paddingTop
        rectPadding.bottom = (viewHeight - paddingBottom).toFloat()
        rectPadding.right = (viewWidth - paddingRight).toFloat()
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)
        realHeight = (viewHeight - paddingTop - paddingBottom)
        realWidth = (viewWidth - paddingLeft - paddingRight)
        videoBarWidth = realWidth - thumbWidth * 2
        imageWidth = videoBarWidth.toFloat() / numberPreviewImage
        if (videoBarHeight == 0)
            videoBarHeight = (viewHeight - rectPadding.top - rectPadding.bottom).toInt()
        else if (videoBarHeight > realHeight)
            videoBarHeight = realHeight
        rectThumbLeft.set(
            rectPadding.left.toInt(),
            (rectPadding.bottom - videoBarHeight).toInt(),
            rectPadding.left.toInt() + thumbWidth,
            rectPadding.bottom.toInt()
        )
        rectThumbRight.set(
            (rectPadding.right - thumbWidth).toInt(),
            (rectPadding.bottom - videoBarHeight).toInt(),
            rectPadding.right.toInt(),
            rectPadding.bottom.toInt()
        )
        initListRectImage()
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            if (listBitmap.isNotEmpty()) {
                for (i in 0 until listBitmap.size) {
                    val bitmap = listBitmap[i]
                    val rectDestination = listRectDestination[i]
                    canvas.drawBitmap(bitmap, null,rectDestination,imagePaint)
                }
            }
            if (showThumbCut) {
                drawableThumbLeft?.drawAt(rectThumbLeft, canvas)
                drawableThumbRight?.drawAt(rectThumbRight, canvas)
            }
            invalidateIndicator(canvas)
        }
    }

    private fun Drawable.drawAt(rect: Rect, canvas: Canvas) {
        bounds = rect
        draw(canvas)
    }

    private fun setPath() {
        val file = File(videoPath)
        if (file.exists()) {
            loadingTask?.cancelTask()
            loadingTask = LoadingTask(object : LoadingTask.ILoadingTask {
                override fun onLoadingStart() {
                    loadingListener?.onLoadingStart()
                }

                override fun onLoading() {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(videoPath)
                    val sDuration =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    try {
                        duration = sDuration.toLong()
                        maxProgress = duration
                    } catch (e: Exception) {
                        Log.e("Can't get duration")
                    }
                    Log.e("Duration: $duration")
                    listBitmap.clear()
                    for (i in 0 until numberPreviewImage) {
                        val bitmap =
                            retriever.getFrameAtTime(duration / numberPreviewImage * i.toLong() * 1000)
                        listBitmap.add(bitmap)
                    }
                    retriever.release()
                }

                override fun onLoadingEnd() {
                    loadingListener?.onLoadingComplete()
                    invalidate()
                }

            }).run()
        } else
            Log.e("File not exists")
    }

    private fun initListRectImage(){
        listRectDestination.clear()
        var offset = rectPadding.left+thumbWidth
        for (i in 0 until numberPreviewImage) {
            listRectDestination.add(
                RectF(
                    offset,
                    rectThumbLeft.top.toFloat(),
                    offset + imageWidth,
                    rectThumbLeft.bottom.toFloat()
                )
            )
            offset += imageWidth
        }
    }

    private val pointDown = PointF(0f,0f)
    private var isThumbMoving = false
    private var thumbIndex = -1

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let { event->
            when(event.actionMasked){
                MotionEvent.ACTION_DOWN->{
                    pointDown.set(event.x,event.y)
                    thumbIndex = getThumbFocus()
                }
                MotionEvent.ACTION_MOVE->{
                    val disMove = event.x - pointDown.x
                    pointDown.x = event.x
                    if (isThumbMoving){
                        moveThumb(disMove)
                    }else{
                        if (abs(disMove)>=touchSlop){
                            isThumbMoving = true
                        }
                    }
                }
                MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->{
                    pointDown.set(0f,0f)
                    thumbIndex = -1
                    isThumbMoving = false
                    invalidate()
                }
            }
        }
        return true
    }

    private fun getThumbFocus():Int{
        var isFocusThumbLeft = false
        var isFocusThumbRight = false
        if (pointDown.x.toInt() in rectThumbLeft.left-touchAreaExtra..rectThumbLeft.right+touchAreaExtra){
            isFocusThumbLeft = true
        }
        if (pointDown.x.toInt() in rectThumbRight.left-touchAreaExtra..rectThumbRight.right+touchAreaExtra){
            isFocusThumbRight = true
        }
        return if (isFocusThumbLeft&&isFocusThumbRight){
            Log.e("${abs(pointDown.x-rectThumbLeft.centerX())} , ${abs(rectThumbRight.centerX()-pointDown.y)}")
            if (abs(pointDown.x-rectThumbLeft.centerX())>abs(rectThumbRight.centerX()-pointDown.x)){
                1
            }else
                0
        }else if (isFocusThumbLeft)
            0
        else if (isFocusThumbRight)
            1
        else
            -1
    }

    private fun moveThumb(distance:Float){
        val disMove = distance.toInt()
        if (thumbIndex==-1)
            return
        lastFocusThumbIndex = thumbIndex
        val thumbRect:Rect
        if (thumbIndex==0){
            thumbRect = rectThumbLeft
            val minLeft = rectPadding.left
            val maxLeft = (rectThumbRight.left-thumbWidth).toFloat()
            adjustMove(thumbRect,disMove,minLeft,maxLeft)
            minProgress = thumbRect.right.toFloat().ToProgress()
        }else if (thumbIndex==1){
            thumbRect = rectThumbRight
            val minLeft = rectThumbLeft.right.toFloat()
            val maxLeft = rectPadding.right-thumbWidth
            adjustMove(thumbRect,disMove,minLeft,maxLeft)
            maxProgress = thumbRect.left.toFloat().ToProgress()
        }
        //Log.e("Min: $minProgress, Max: $maxProgress, Duration: $duration")
        invalidate()
    }

    private fun adjustMove(thumbRect:Rect,disMove:Int,minLeft:Float,maxLeft:Float){
        if (thumbRect.left+disMove<minLeft)
            thumbRect.left = minLeft.toInt()
        else if (thumbRect.left+disMove>maxLeft)
            thumbRect.left = maxLeft.toInt()
        else
            thumbRect.left +=disMove
        thumbRect.right = thumbRect.left+thumbWidth
    }

    private fun invalidateIndicator(canvas:Canvas){
        if (lastFocusThumbIndex!=-1){
            val centerOfThumb = if (lastFocusThumbIndex==0) rectThumbLeft.centerX() else rectThumbRight.centerX()
            rectIndicator.left = (centerOfThumb - indicatorSize/2).toFloat()
            rectIndicator.right = (centerOfThumb + indicatorSize/2).toFloat()
            rectIndicator.bottom = (rectThumbLeft.top - paddingIndicator).toFloat()
            rectIndicator.top = rectIndicator.bottom - indicatorSize
            canvas.drawRoundRect(rectIndicator,indicatorCorners,indicatorCorners,indicatorPaint)
            if (isThumbMoving){
                val fixBoundsOfText = 10f
                val showProgress = if (lastFocusThumbIndex==0) minProgress else maxProgress
                val formatProgress = formatDuration.format(showProgress)
                indicatorTextPaint.getTextBounds(formatProgress,0,formatProgress.length,rectTextIndicator)
                var xText = (centerOfThumb - rectTextIndicator.width()/2).toFloat()
                if (xText+rectTextIndicator.width()>viewWidth)
                    xText = (viewWidth - rectTextIndicator.width()).toFloat() - fixBoundsOfText
                else if (xText<0)
                    xText = 0F + fixBoundsOfText
                val yText = rectIndicator.top - paddingTextIndicator
                canvas.drawText(formatProgress,xText,yText,indicatorTextPaint)
            }
        }
    }

    private fun Float.ToProgress(): Long {
        val realDimension = (this - thumbWidth - rectPadding.left)
        Log.e("$realDimension, $videoBarWidth, $duration")
        return ((realDimension/videoBarWidth)*duration).toLong()
    }

    interface ILoadingListener{
        fun onLoadingStart(){}
        fun onLoadingComplete(){}
    }

    override fun invalidate() {
        try{
            super.invalidate()
        }catch (e:Exception){}
    }
}