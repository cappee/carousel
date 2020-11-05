package dev.cappee.carousel

interface OnItemClickListener {

    fun onClick(position: Int, carouselItem: CarouselItem)

    fun onLongClick(position: Int, dataObject: CarouselItem)

}