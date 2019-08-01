package com.xva.musiclife.utils

import com.xva.musiclife.models.Song

class EventBusHelper {

    internal class playingSong(var song: Song)
    internal class isShowMiniPl(var status: String)
    internal class songInformations(var song: Song)
    internal class songsQueue(var queue: ArrayList<Song>)
    internal class songForQueue(var song: Song)
    internal class playList(var playList: ArrayList<Song>)
    internal class favouriteStatus(var status: Boolean)
}