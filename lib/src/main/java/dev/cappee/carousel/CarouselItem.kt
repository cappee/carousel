package dev.cappee.carousel

import androidx.annotation.DrawableRes

data class CarouselItem constructor(@DrawableRes val imageDrawable: Int? = null, val caption: String? = null) {

    constructor(@DrawableRes imageDrawable: Int? = null) : this(imageDrawable, null)

}