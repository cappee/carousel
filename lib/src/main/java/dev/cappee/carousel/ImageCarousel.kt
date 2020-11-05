package dev.cappee.carousel

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.*
import me.relex.circleindicator.CircleIndicator2
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

class ImageCarousel(
        @NotNull context: Context,
        @Nullable private var attributeSet: AttributeSet?
) : ConstraintLayout(context, attributeSet) {

    private var adapter: CarouselAdapter? = null

    private val scaleTypeArray = arrayOf(
            ImageView.ScaleType.MATRIX,
            ImageView.ScaleType.FIT_XY,
            ImageView.ScaleType.FIT_START,
            ImageView.ScaleType.FIT_CENTER,
            ImageView.ScaleType.FIT_END,
            ImageView.ScaleType.CENTER,
            ImageView.ScaleType.CENTER_CROP,
            ImageView.ScaleType.CENTER_INSIDE
    )

    private val carouselTypeArray = arrayOf(
            CarouselType.BLOCK,
            CarouselType.SHOWCASE
    )

    private lateinit var carouselView: View
    private lateinit var recyclerView: RecyclerView
    private lateinit var textViewCaption: TextView
    private lateinit var viewTopShadow: View
    private lateinit var viewBottomShadow: View
    private lateinit var previousButtonContainer: FrameLayout
    private lateinit var nextButtonContainer: FrameLayout
    private lateinit var snapHelper: SnapHelper

    private var circleIndicator: CircleIndicator2? = null
    private var buttonPrevious: View? = null
    private var buttonNext: View? = null

    private var isBuiltInIndicator = false
    private var autoPlayHandler: Handler? = Looper.myLooper()?.let { Handler(it) }
    private var data: List<CarouselItem>? = null
    private var dataSize = 0

    var onItemClickListener: OnItemClickListener? = null
        set(value) {
            field = value

            adapter?.listener = onItemClickListener
        }

    var onScrollListener: CarouselOnScrollListener? = null
        set(value) {
            field = value

            initOnScrollStateChange()
        }

    /**
     * Get or set current item position
     */
    var currentPosition = -1
        get() {
            return snapHelper.getSnapPosition(recyclerView.layoutManager)
        }
        set(value) {
            val position = when {
                value >= dataSize -> {
                    -1
                }
                value < 0 -> {
                    -1
                }
                else -> {
                    value
                }
            }

            field = position

            if (position != -1) {
                recyclerView.smoothScrollToPosition(position)
            }
        }

    /**
     * ****************************************************************
     * Attributes
     * ****************************************************************
     */

    var showCaption = false
        set(value) {
            field = value

            textViewCaption.visibility = if (showCaption) View.VISIBLE else View.GONE
        }

    @Dimension(unit = Dimension.PX)
    var captionMargin: Int = 0
        set(value) {
            field = value

            val captionMarginParams = textViewCaption.layoutParams as LayoutParams
            captionMarginParams.setMargins(
                    0,
                    0,
                    0,
                    captionMargin
            )
            captionMarginParams.goneBottomMargin = captionMargin
            textViewCaption.layoutParams = captionMarginParams
        }

    @Dimension(unit = Dimension.PX)
    var captionTextSize: Int = 0
        set(value) {
            field = value

            textViewCaption.setTextSize(TypedValue.COMPLEX_UNIT_PX, captionTextSize.toFloat())
        }

    var showIndicator = false
        set(value) {
            field = value

            initIndicator()
        }

    @Dimension(unit = Dimension.PX)
    var indicatorMargin: Int = 0
        set(value) {
            field = value

            if (isBuiltInIndicator) {
                circleIndicator?.apply {
                    val indicatorMarginParams = this.layoutParams as LayoutParams
                    indicatorMarginParams.setMargins(
                            0,
                            0,
                            0,
                            indicatorMargin
                    )
                    this.layoutParams = indicatorMarginParams
                }
            }
        }

    var showNavigationButtons = false
        set(value) {
            field = value

            previousButtonContainer.visibility =
                    if (showNavigationButtons) View.VISIBLE else View.GONE
            nextButtonContainer.visibility =
                    if (showNavigationButtons) View.VISIBLE else View.GONE
        }

    var imageScaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP
        set(value) {
            field = value

            initAdapter()
        }

    var carouselBackground: Drawable? = null
        set(value) {
            field = value

            recyclerView.background = carouselBackground
        }

    var imagePlaceholder: Drawable? = null
        set(value) {
            field = value

            initAdapter()
        }

    @LayoutRes
    var itemLayout: Int = R.layout.item
        set(value) {
            field = value

            initAdapter()
        }

    @IdRes
    var imageViewId: Int = R.id.imageViewCarousel
        set(value) {
            field = value

            initAdapter()
        }

    @LayoutRes
    var previousButtonLayout: Int = R.layout.previous_button
        set(value) {
            field = value

            buttonPrevious = null

            previousButtonContainer.removeAllViews()
            LayoutInflater.from(context).apply {
                inflate(previousButtonLayout, previousButtonContainer, true)
            }
        }

    @IdRes
    var previousButtonId: Int = R.id.buttonNext
        set(value) {
            field = value

            buttonPrevious = carouselView.findViewById(previousButtonId)

            buttonPrevious?.setOnClickListener {
                previous()
            }
        }

    @Dimension(unit = Dimension.PX)
    var previousButtonMargin: Int = 0
        set(value) {
            field = value

            val previousButtonParams = previousButtonContainer.layoutParams as LayoutParams
            previousButtonParams.setMargins(
                    previousButtonMargin,
                    0,
                    0,
                    0
            )
            previousButtonContainer.layoutParams = previousButtonParams
        }

    @LayoutRes
    var nextButtonLayout: Int = R.layout.next_button
        set(value) {
            field = value

            buttonNext = null

            nextButtonContainer.removeAllViews()
            LayoutInflater.from(context).apply {
                inflate(nextButtonLayout, nextButtonContainer, true)
            }
        }

    @IdRes
    var nextButtonId: Int = R.id.buttonPrevious
        set(value) {
            field = value

            buttonNext = carouselView.findViewById(nextButtonId)

            buttonNext?.setOnClickListener {
                next()
            }
        }

    @Dimension(unit = Dimension.PX)
    var nextButtonMargin: Int = 0
        set(value) {
            field = value

            val nextButtonParams = nextButtonContainer.layoutParams as LayoutParams
            nextButtonParams.setMargins(
                    0,
                    0,
                    nextButtonMargin,
                    0
            )
            nextButtonContainer.layoutParams = nextButtonParams
        }

    var carouselType: CarouselType = CarouselType.BLOCK
        set(value) {
            field = value

            if (carouselType == CarouselType.BLOCK) {
                snapHelper = PagerSnapHelper()
            } else if (carouselType == CarouselType.SHOWCASE) {
                snapHelper = LinearSnapHelper()
            }

            snapHelper.apply {
                try {
                    attachToRecyclerView(recyclerView)
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }

            circleIndicator?.apply {
                attachToRecyclerView(recyclerView, snapHelper)
            }
        }

    var scaleOnScroll: Boolean = false
        set(value) {
            field = value

            recyclerView.layoutManager?.apply {
                if (this is CarouselLinearLayoutManager) {
                    this.scaleOnScroll = this@ImageCarousel.scaleOnScroll
                }
            }
        }

    var scalingFactor: Float = 0f
        set(value) {
            field = getValueFromZeroToOne(value)

            recyclerView.layoutManager?.apply {
                if (this is CarouselLinearLayoutManager) {
                    this.scalingFactor = this@ImageCarousel.scalingFactor
                }
            }
        }

    var autoWidthFixing: Boolean = false
        set(value) {
            field = value

            initAdapter()
        }

    // Note: We do not need to invalidate the view for this.
    var autoPlay: Boolean = false

    // Note: We do not need to invalidate the view for this.
    var autoPlayDelay: Int = 0


    init {
        initViews()
        initAttributes()
        initAdapter()
        initListeners()
        initAutoPlay()
    }


    private fun initViews() {
        carouselView = LayoutInflater.from(context).inflate(R.layout.carousel, this)

        recyclerView = carouselView.findViewById(R.id.recyclerView)
        textViewCaption = carouselView.findViewById(R.id.textViewCaption)
        previousButtonContainer = carouselView.findViewById(R.id.previous_button_container)
        nextButtonContainer = carouselView.findViewById(R.id.next_button_container)

        recyclerView.layoutManager =
                CarouselLinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                        .apply {
                            scaleOnScroll = this@ImageCarousel.scaleOnScroll
                            scalingFactor = this@ImageCarousel.scalingFactor
                        }
        recyclerView.setHasFixedSize(true)

        // For marquee effect
        textViewCaption.isSelected = true
    }


    private fun initAttributes() {
        context.theme.obtainStyledAttributes(
                attributeSet,
                R.styleable.ImageCarousel,
                0,
                0
        ).apply {

            try {
                showCaption = getBoolean(
                        R.styleable.ImageCarousel_showCaption,
                        true
                )

                captionMargin = getDimension(
                        R.styleable.ImageCarousel_captionMargin,
                        0.dpToPx(context).toFloat()
                ).toInt()

                captionTextSize = getDimension(
                        R.styleable.ImageCarousel_captionTextSize,
                        14.spToPx(context).toFloat()
                ).toInt()

                carouselType = carouselTypeArray[
                        getInteger(
                                R.styleable.ImageCarousel_carouselType,
                                CarouselType.BLOCK.ordinal
                        )
                ]

                showIndicator = getBoolean(
                        R.styleable.ImageCarousel_showIndicator,
                        true
                )

                indicatorMargin = getDimension(
                        R.styleable.ImageCarousel_indicatorMargin,
                        0F
                ).toInt()

                imageScaleType = scaleTypeArray[
                        getInteger(
                                R.styleable.ImageCarousel_imageScaleType,
                                ImageView.ScaleType.CENTER_CROP.ordinal
                        )
                ]

                carouselBackground = getDrawable(
                        R.styleable.ImageCarousel_carouselBackground
                ) ?: ColorDrawable(Color.parseColor("#333333"))

                imagePlaceholder = getDrawable(
                        R.styleable.ImageCarousel_imagePlaceholder
                ) ?: ContextCompat.getDrawable(context, R.drawable.placeholder)

                itemLayout = getResourceId(
                        R.styleable.ImageCarousel_itemLayout,
                        R.layout.item
                )

                imageViewId = getResourceId(
                        R.styleable.ImageCarousel_imageViewId,
                        R.id.imageViewCarousel
                )

                previousButtonLayout = getResourceId(
                        R.styleable.ImageCarousel_previousButtonLayout,
                        R.layout.previous_button
                )

                previousButtonId = getResourceId(
                        R.styleable.ImageCarousel_previousButtonId,
                        R.id.buttonPrevious
                )

                previousButtonMargin = getDimension(
                        R.styleable.ImageCarousel_previousButtonMargin,
                        4.dpToPx(context).toFloat()
                ).toInt()

                nextButtonLayout = getResourceId(
                        R.styleable.ImageCarousel_nextButtonLayout,
                        R.layout.next_button
                )

                nextButtonId = getResourceId(
                        R.styleable.ImageCarousel_nextButtonId,
                        R.id.buttonNext
                )

                nextButtonMargin = getDimension(
                        R.styleable.ImageCarousel_nextButtonMargin,
                        4.dpToPx(context).toFloat()
                ).toInt()

                showNavigationButtons = getBoolean(
                        R.styleable.ImageCarousel_showNavigationButtons,
                        true
                )

                scaleOnScroll = getBoolean(
                        R.styleable.ImageCarousel_scaleOnScroll,
                        false
                )

                scalingFactor = getFloat(
                        R.styleable.ImageCarousel_scalingFactor,
                        .15F
                )

                autoWidthFixing = getBoolean(
                        R.styleable.ImageCarousel_autoWidthFixing,
                        true
                )

                autoPlay = getBoolean(
                        R.styleable.ImageCarousel_autoPlay,
                        false
                )

                autoPlayDelay = getInt(
                        R.styleable.ImageCarousel_autoPlayDelay,
                        3000
                )

            } finally {
                recycle()
            }

        }
    }


    private fun initAdapter() {
        adapter = CarouselAdapter(
                context = context,
                recyclerView = recyclerView,
                carouselType = carouselType,
                autoWidthFixing = autoWidthFixing,
                itemLayout = itemLayout,
                imageViewId = imageViewId,
                listener = onItemClickListener,
                imageScaleType = imageScaleType,
                imagePlaceholder = imagePlaceholder
        )
        recyclerView.adapter = adapter

        data?.apply {
            adapter?.addAll(this)
        }

        circleIndicator?.apply {
            try {
                adapter?.registerAdapterDataObserver(this.adapterDataObserver)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }


    private fun initListeners() {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                if (showCaption) {
                    val position = snapHelper.getSnapPosition(recyclerView.layoutManager)

                    if (position >= 0) {
                        val dataItem = adapter?.getItem(position)

                        dataItem?.apply {
                            textViewCaption.text = this.caption
                        }
                    }
                }

                onScrollListener?.onScrolled(recyclerView, dx, dy)

            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {

                onScrollListener?.apply {
                    val position = snapHelper.getSnapPosition(recyclerView.layoutManager)
                    val carouselItem = data?.get(position)

                    onScrollStateChanged(
                            recyclerView,
                            newState,
                            position,
                            carouselItem
                    )
                }

            }
        })
    }


    /**
     * Initialize and start/stop auto play based on `autoPlay` value.
     */
    private fun initAutoPlay() {
        if (autoPlay) {

            autoPlayHandler?.postDelayed(object : Runnable {
                override fun run() {
                    if (currentPosition == dataSize - 1) {
                        currentPosition = 0
                    } else {
                        currentPosition++
                    }

                    autoPlayHandler!!.postDelayed(this, autoPlayDelay.toLong())
                }
            }, autoPlayDelay.toLong())

        } else {

            autoPlayHandler?.removeCallbacksAndMessages(null)

        }
    }

    private fun initIndicator() {
        // If no custom indicator added, then default indicator will be shown.
        if (circleIndicator == null) {
            circleIndicator = carouselView.findViewById(R.id.circleIndicator)
            isBuiltInIndicator = true
        }

        circleIndicator?.apply {
            if (isBuiltInIndicator) {
                // Indicator margin re-initialize
                val indicatorMarginParams = this.layoutParams as LayoutParams
                indicatorMarginParams.setMargins(
                        indicatorMargin,
                        indicatorMargin,
                        indicatorMargin,
                        indicatorMargin
                )
                this.layoutParams = indicatorMarginParams

                // Indicator visibility
                this.visibility = if (showIndicator) View.VISIBLE else View.GONE
            }

            // Attach to recyclerview
            attachToRecyclerView(recyclerView, snapHelper)

            // Observe the adapter
            adapter?.let { carouselAdapter ->
                try {
                    carouselAdapter.registerAdapterDataObserver(this.adapterDataObserver)
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getValueFromZeroToOne(value: Float): Float {
        return when {
            value < 0 -> {
                0f
            }
            value > 1 -> {
                1f
            }
            else -> {
                value
            }
        }
    }

    private fun initOnScrollStateChange() {
        data?.apply {
            if (isNotEmpty()) {
                onScrollListener?.onScrollStateChanged(
                        recyclerView,
                        RecyclerView.SCROLL_STATE_IDLE,
                        0,
                        this[0]
                )
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        stop()
    }

    // ----------------------------------------------------------------

    /**
     * Add data to the carousel.
     */
    fun addData(data: List<CarouselItem>) {
        adapter?.apply {
            addAll(data)

            this@ImageCarousel.data = data
            this@ImageCarousel.dataSize = data.size

            initOnScrollStateChange()
        }
    }

    // ----------------------------------------------------------------

    /**
     * @return Current indicator or null.
     */
    fun getIndicator(): CircleIndicator2? {
        return circleIndicator
    }

    /**
     * Set custom indicator.
     */
    fun setIndicator(newIndicator: CircleIndicator2) {
        circleIndicator?.apply {
            // if we remove it form the view, then the caption textView constraint won't work.
            this.visibility = View.GONE

            isBuiltInIndicator = false
        }

        this.circleIndicator = newIndicator

        initIndicator()
    }

    // ----------------------------------------------------------------

    /**
     * Goto previous item.
     */
    fun previous() {
        currentPosition--
    }

    // ----------------------------------------------------------------

    /**
     * Goto next item.
     */
    fun next() {
        currentPosition++
    }

    // ----------------------------------------------------------------

    /**
     * Start auto play.
     */
    fun start() {
        autoPlay = true

        initAutoPlay()
    }

    // ----------------------------------------------------------------

    /**
     * Stop auto play.
     */
    fun stop() {
        autoPlay = false

        initAutoPlay()
    }

}