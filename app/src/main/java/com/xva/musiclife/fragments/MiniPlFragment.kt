package com.xva.musiclife.fragments

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.xva.musiclife.R
import com.xva.musiclife.data.Database
import com.xva.musiclife.data.SharedPrefencesHelper
import com.xva.musiclife.models.Song
import com.xva.musiclife.services.PlayerServices
import com.xva.musiclife.services.PlayerServicesBinder
import com.xva.musiclife.utils.EventBusHelper
import kotlinx.android.synthetic.main.fragment_mini_pl.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe


class MiniPlFragment() : Fragment() {


    private lateinit var mView: View
    private lateinit var database:Database
    private lateinit var songName: TextView
    private lateinit var artist: TextView
    private lateinit var actionButton: ImageView
    private lateinit var refrainButton: ImageView
    private lateinit var expandButton: ImageView
    private lateinit var favouriteButton: ImageView
    private lateinit var progressBar: ProgressBar
    // Çalma Listeleri
    private var queue = ArrayList<Song>()
    private var playlist = ArrayList<Song>()
    //
    private lateinit var sharedPrefencesHelper: SharedPrefencesHelper
    // şarkını ilerleme  göstergeci handleri
    private var audioProgressUpdateHandler: Handler? = null
    private var handler: Handler = Handler()
    // service binder ı
    private var serviceBinder: PlayerServicesBinder? = null
    //
    private lateinit var playingSong: Song
    // oynat , durdur durum bilgisi
    private var actionStatus = "pause"
    // Play Liste de hangi indexteki şarkıda kaldık bilgisi
    private var playListSongIndex = 0
    // Favorilere eklenmis mi durumu
    private var isSongAddedToFavourite = false
    // Favorilere Ekleme Gibi Kısımlarda Kullanacagız
    private var songId = -1


    override fun onAttach(context: Context?) {
        super.onAttach(context)
        // Services
        bindAudioService()
        EventBus.getDefault().register(this)
    }

    override fun onDetach() {
        super.onDetach()
        unBoundAudioService()
        EventBus.getDefault().unregister(this)
    }

    private fun bindAudioService() {
        if (serviceBinder == null) {
            val intent = Intent(activity!!, PlayerServices::class.java)

            // Servise bağlanma
            activity!!.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun unBoundAudioService() {
        if (serviceBinder != null) {
            activity!!.unbindService(serviceConnection)
        }
    }


    // Service Connection
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            Log.e("connect", "not")
            // DO Nothing...
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.e("connect", "yes")
            serviceBinder = service as PlayerServicesBinder
            setBinderContext()
            updateSongPath()
            createProgressBarHandler()
        }

    }

    private fun setBinderContext() {
        // Set application context.
        serviceBinder!!.context = activity!!
    }


    private fun updateSongPath() {
        if (serviceBinder!!.audioFileUri != null) {
            serviceBinder!!.stopAudio()
        }

        serviceBinder!!.audioFileUri = Uri.parse(playingSong.path)

        // Initialize audio progress bar updater Handler object.
        // createAudioProgressbarUpdater();
        //audioServiceBinder.setAudioProgressUpdateHandler(audioProgressUpdateHandler);
    }


    private fun createProgressBarHandler() {

        /* Initialize audio progress handler. */
        if (audioProgressUpdateHandler == null) {

            audioProgressUpdateHandler = @SuppressLint("HandlerLeak")
            object : Handler() {
                override fun handleMessage(msg: Message) {
                    // The update process message is sent from AudioServiceBinder class's thread object.
                    if (serviceBinder != null && msg.what === serviceBinder!!.UPDATE_AUDIO_PROGRESS_BAR) {
                        // Calculate the percentage.
                        val currProgress = serviceBinder!!.audioProgress

                        // Update progressbar. Make the value 10 times to show more clear UI change.
                        progressBar.progress = currProgress
                        if (currProgress == 100) {
                            // şarkı bitirse servisi durdur
                            serviceBinder!!.stopAudio()
                            // TODO : çalma kuyrugunu kontor et şarkı yok ise durdur varsa yeni şarkıya geç
                            nextSong()
                        }

                    }
                }
            }
            serviceBinder!!.audioProgressUpdateHandler = audioProgressUpdateHandler
        }
    }

    // Kuyruga Eklenecek Şarkıyı Aldık
    @Subscribe(sticky = true)
    internal fun onDataEvent(data: EventBusHelper.songForQueue) {
        queue.add(data.song)
    }

    @Subscribe(sticky = true)
    internal fun onDataEvent(data: EventBusHelper.playList) {
        // TODO : PlayList Fragmenttan(Songs Activity) Şarkı Açılırsa Ordan Yayınalanacak
        // Play List Zaten Buraya Yayınlanarak Geliyor Player Activitydede Çek Direk
        playlist = data.playList
    }

    // Şarkı Çalarken Song Setting Gidip Favorite İşlemleri Yapılırsa MiniPl Haberdar Olsun Diye
    @Subscribe(sticky = true)
    internal fun onDataEvent(data: EventBusHelper.favouriteStatus) {
        if(data.status){
            showGreenFavourite()
        }else{
            showBlueFavourite()
        }
    }

    private fun nextSong() {
        if (queue.size > 0) {
            playingSong = queue[0]
            updateSongPath()
            queue.removeAt(0)
            handler.postDelayed(playSong(), 5)
            setSongDetail()
            checkSongIsAvailable()
            actionStatus = "playing"
            //TODO : Kuyrugu Player Activity İçin Yayınla
        } else if (playlist.size > 0) {
            playingSong = playlist[playListSongIndex]
            updateSongPath()
            queue.removeAt(0)
            handler.postDelayed(playSong(), 5)
            setSongDetail()
            checkSongIsAvailable()
            actionStatus = "playing"
            playListSongIndex++

        } else {
            showPlayImage()
            actionStatus = "pause"
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mView = inflater.inflate(R.layout.fragment_mini_pl, container, false)
        load()
        setClick()
        database = Database(activity!!)
        checkSongIsAvailable()
        return mView
    }


    //
    private fun load() {
        songName = mView.findViewById(R.id.textViewSongName) as TextView
        artist = mView.findViewById(R.id.textViewArtist) as TextView
        actionButton = mView.findViewById(R.id.imageViewAction) as ImageView
        refrainButton = mView.findViewById(R.id.imageViewBestPart) as ImageView
        expandButton = mView.findViewById(R.id.imageViewExpand) as ImageView
        favouriteButton = mView.findViewById(R.id.imageViewFavourite) as ImageView
        progressBar = mView.findViewById(R.id.progressBar) as ProgressBar
        sharedPrefencesHelper = SharedPrefencesHelper(activity!!)


        // Check Last Song
        isLastSongAvailable()
    }

    private fun setClick() {

        actionButton.setOnClickListener {

            if (actionStatus == "pause") {
                handler.postDelayed(playSong(), 5)
                actionStatus = "playing"
                showStopImage()
                showActionAnimation()
            } else {
                handler.postDelayed(pauseSong(), 5)
                actionStatus = "pause"
                showPlayImage()
                showActionAnimation()
            }
        }

        refrainButton.setOnClickListener {
            showRefrainAnimation()
        }

        expandButton.setOnClickListener {
            showExpandAnimation()
        }


        favouriteButton.setOnClickListener {

            if(!isSongAddedToFavourite){
                showGreenFavourite()
                database.addToFavourite(songId)
            }else{
                showBlueFavourite()
                database.removeFromFavourite(songId)
            }
            showFavouriteAnimation()
        }


    }


    // En son dinlenen şarkı bilgisi var mı kontrol et
    private fun isLastSongAvailable() {
        playingSong = sharedPrefencesHelper.getLastSong()

        if (playingSong.path != "-1") {
            // şarkının detaylarını componentlerde göster
            setSongDetail()
        }

    }

    private fun setSongDetail() {
        songName.text = playingSong.name
        artist.text = playingSong.artist
    }


    // Şarkıya Çalmak için Tıklanıldıgında şarkı bilgisi buraya düşecek
    @Subscribe(sticky = true)
    internal fun onDataEvent(data: EventBusHelper.playingSong) {
        playingSong = data.song
        updateSongPath()
        handler.postDelayed(playSong(), 5)
        showStopImage()
        setSongDetail()
        checkSongIsAvailable()
        actionStatus = "playing"
    }

    // Database de şarkıya ait kayıt var mı kontrol yoksa oluştur
    private fun checkSongIsAvailable() {
         songId = database.isSongAvailable(playingSong.path)
        if (songId == -1) {
            database.addSong(playingSong.path)
            songId = database.isSongAvailable(playingSong.path)
            // Lazım İşte Boş Yapma
            showBlueFavourite()
        }
        checkIsSongAddedToFavourite()
    }

    // şarkı favorilere eklenmiş mi kontrol et
    private fun checkIsSongAddedToFavourite() {
        if (database.isSongAddedToFavourite(songId) != -1) {
            showGreenFavourite()
        }else{
            showBlueFavourite()
        }
    }


    private fun playSong() = Runnable {
        serviceBinder!!.startAudio()
    }

    private fun pauseSong() = Runnable {
        serviceBinder!!.pauseAudio()
    }


    private fun showPlayImage() {
        actionButton.setImageResource(R.drawable.ic_play_circle_filled_blue_40dp)
    }

    private fun showStopImage() {
        actionButton.setImageResource(R.drawable.ic_pause_circle_filled_blue_40dp)
    }

    private fun showGreenFavourite() {
        favouriteButton.setImageResource(R.drawable.ic_favorite_green_40dp)
        isSongAddedToFavourite = true
    }

    private fun showBlueFavourite() {
        favouriteButton.setImageResource(R.drawable.ic_favorite_blue_40dp)
        isSongAddedToFavourite = false
    }

    private fun showActionAnimation() {
        actionButton.startAnimation(AnimationUtils.loadAnimation(activity!!, R.anim.scale))
    }

    private fun showRefrainAnimation() {
        refrainButton.startAnimation(AnimationUtils.loadAnimation(activity!!, R.anim.scale))
    }

    private fun showExpandAnimation() {
        expandButton.startAnimation(AnimationUtils.loadAnimation(activity!!, R.anim.scale))
    }

    private fun showFavouriteAnimation() {
        favouriteButton.startAnimation(AnimationUtils.loadAnimation(activity!!, R.anim.scale))
    }

}