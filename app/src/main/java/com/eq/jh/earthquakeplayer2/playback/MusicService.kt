package com.eq.jh.earthquakeplayer2.playback

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import com.eq.jh.earthquakeplayer2.R
import com.eq.jh.earthquakeplayer2.constants.ContentType
import com.eq.jh.earthquakeplayer2.constants.DebugConstant
import com.eq.jh.earthquakeplayer2.playback.data.SongSource
import com.eq.jh.earthquakeplayer2.playback.data.VideoSource
import com.eq.jh.earthquakeplayer2.playback.player.EarthquakePlaybackPreparer
import com.eq.jh.earthquakeplayer2.playback.player.EarthquakePlayer
import com.eq.ljh.flags.constants.MediaBrowserIdConstant
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch


/**
 * This class is the entry point for browsing and playback commands from the APP's UI
 * and other apps that wish to play music via UAMP (for example, Android Auto or
 * the Google Assistant).
 *
 * Browsing begins with the method [MusicService.onGetRoot], and continues in
 * the callback [MusicService.onLoadChildren].
 *
 * For more information on implementing a MediaBrowserService,
 * visit [https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice.html](https://developer.android.com/guide/topics/media-apps/audio-app/building-a-mediabrowserservice.html).
 */
class MusicService : MediaBrowserServiceCompat() {
    companion object {
        const val TAG = "MusicService"
    }

    private lateinit var packageValidator: PackageValidator

    private val songSource: SongSource by lazy {
        SongSource(context = this)
    }

    private val videoSource: VideoSource by lazy {
        VideoSource(context = this)
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    // media session
    private lateinit var player: EarthquakePlayer
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    @Suppress("PropertyName")
    val EMPTY_PLAYBACK_STATE: PlaybackStateCompat = PlaybackStateCompat.Builder()
        .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
        .build()

//    private var isPrepared = false

    private fun createPlayer(): EarthquakePlayer {
        Log.d(TAG, "createPlayer")
        player = EarthquakePlayer(this).also {
            it.setCallback(object : EarthquakePlayer.ExoPlayerCallback {
                override fun onCompletion() {
                    if (DebugConstant.DEBUG) {
                        Log.d(TAG, "onCompletion()")
                    }
                }

                override fun onExoPlayerPlaybackStatusChanged(playWhenReady: Boolean, state: Int) {
                    Log.d(TAG, "onExoPlayerPlaybackStatusChanged playWhenReady : $playWhenReady, state : ${player.stateName(state)}")
                }

                override fun onError(error: String) {
                    Log.d(TAG, "onError error : $error")
                }
            })
        }
        return player
    }

    // Create a new MediaSession.
    private fun createMediaSession(): MediaSessionCompat {
        mediaSession = MediaSessionCompat(this, "MusicService").apply {
            isActive = true
        }
        return mediaSession
    }

    private fun createMediaController(): MediaControllerCompat {
        mediaController = MediaControllerCompat(this, mediaSession).also {
            it.registerCallback(object : MediaControllerCompat.Callback() {
                override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                    Log.d(TAG, "MediaControllerCallback onMetadataChanged metadata : $metadata")
                }

                override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                    when (state?.state) {
                        PlaybackStateCompat.STATE_NONE -> {
                            Log.d(TAG, "createMediaController onPlaybackStateChanged STATE_NONE")
                        }
                        PlaybackStateCompat.STATE_BUFFERING -> {
                            Log.d(TAG, "createMediaController onPlaybackStateChanged STATE_BUFFERING")
                        }
                        PlaybackStateCompat.STATE_PLAYING -> {
                            Log.d(TAG, "createMediaController onPlaybackStateChanged STATE_PLAYING")
                        }
                        PlaybackStateCompat.STATE_STOPPED -> {
                            Log.d(TAG, "createMediaController onPlaybackStateChanged STATE_STOPPED")
                        }
                        PlaybackStateCompat.STATE_PAUSED -> {
                            Log.d(TAG, "createMediaController onPlaybackStateChanged STATE_PAUSED")
                        }
                    }
                }
            })
        }
        return mediaController
    }

    private fun createMediaSessionConnector(): MediaSessionConnector {
        mediaSessionConnector = MediaSessionConnector(mediaSession).also {
            Log.d(TAG, "createMediaSessionConnector")
            val playbackPreparer = EarthquakePlaybackPreparer(player)

            it.setPlayer(player.getPlayer())
            it.setPlaybackPreparer(playbackPreparer)
        }
        return mediaSessionConnector
    }

    override fun onCreate() {
        super.onCreate()

        createPlayer()
        createMediaSession()
        sessionToken = mediaSession.sessionToken
        createMediaController()
        createMediaSessionConnector()

        /**
         * In order for [MediaBrowserCompat.ConnectionCallback.onConnected] to be called,
         * a [MediaSessionCompat.Token] needs to be set on the [MediaBrowserServiceCompat].
         *
         * It is possible to wait to set the session token, if required for a specific use-case.
         * However, the token *must* be set by the time [MediaBrowserServiceCompat.onGetRoot]
         * returns, or the connection will fail silently. (The system will not even call
         * [MediaBrowserCompat.ConnectionCallback.onConnectionFailed].)
         */

        packageValidator = PackageValidator(this, R.xml.allowed_media_browser_callers)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Cancel coroutines when the service is going away.
        serviceJob.cancel()
        mediaSession.release()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        Log.d(TAG, "onGetRoot")
        /*
         * By default, all known clients are permitted to search, but only tell unknown callers
         * about search if permitted by the [BrowseTree].
         */
        val isKnownCaller = packageValidator.isKnownCaller(clientPackageName, clientUid)
        return if (!isKnownCaller) {
            /**
             * Unknown caller. There are two main ways to handle this:
             * 1) Return a root without any content, which still allows the connecting client
             * to issue commands.
             * 2) Return `null`, which will cause the system to disconnect the app.
             *
             * Earthquake player takes the first approach for a variety of reasons, but both are valid
             * options.
             */
            BrowserRoot(MediaBrowserIdConstant.MEDIA_BROWSER_ID_EMPTY_ROOT, null)
        } else {
            when (rootHints?.getString(ContentType.EXTRA_CONTENT_TYPE_KEY)) {
                ContentType.CONTENT_TYPE_SONG -> {
                    BrowserRoot(MediaBrowserIdConstant.MEDIA_BROWSER_ID_SONG, null)
                }
                ContentType.CONTENT_TYPE_VIDEO -> {
                    BrowserRoot(MediaBrowserIdConstant.MEDIA_BROWSER_ID_VIDEO, null)
                }
                else -> {
                    BrowserRoot(MediaBrowserIdConstant.MEDIA_BROWSER_ID_EMPTY_ROOT, null)
                }
            }
        }
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        Log.d(TAG, "onLoadChildren parentId : $parentId")
        if (MediaBrowserIdConstant.MEDIA_BROWSER_ID_EMPTY_ROOT == parentId) {
            result.sendResult(null)
            return
        }

        when (parentId) {
            MediaBrowserIdConstant.MEDIA_BROWSER_ID_SONG -> {
                serviceScope.launch {
                    songSource.load()
                }
                // If the media source is ready, the results will be set synchronously here.
                val resultsSent = songSource.whenReady { successfullyInitialized ->
                    if (successfullyInitialized) {
                        val list = songSource.iterator()
                        val mediaItemList = arrayListOf<MediaBrowserCompat.MediaItem>()

                        for (data in list) {
                            val bundle = Bundle().apply {
                                putParcelable(KEY_MEDIA_METADATA, data)
                            }

                            val description = MediaDescriptionCompat.Builder().apply {
                                val mediaId = parentId + "/" + data.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                                setMediaId(mediaId)
                                setExtras(bundle)
                            }.build()
                            mediaItemList += MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
                        }
                        result.sendResult(mediaItemList)
                    } else {
                        mediaSession.sendSessionEvent(LOAD_SONG_FAILURE, null)
                        result.sendResult(null)
                    }
                }
                // If the results are not ready, the service must "detach" the results before
                // the method returns. After the source is ready, the lambda above will run,
                // and the caller will be notified that the results are ready.
                //
                // See [MediaItemFragmentViewModel.subscriptionCallback] for how this is passed to the
                // UI/displayed in the [RecyclerView].
                if (!resultsSent) {
                    result.detach()
                }
            }
            MediaBrowserIdConstant.MEDIA_BROWSER_ID_VIDEO -> {
                serviceScope.launch {
                    videoSource.load()
                }
                // If the media source is ready, the results will be set synchronously here.
                val resultsSent = videoSource.whenReady { successfullyInitialized ->
                    if (successfullyInitialized) {
                        val list = videoSource.iterator()
                        val mediaItemList = arrayListOf<MediaBrowserCompat.MediaItem>()

                        for (data in list) {
                            val bundle = Bundle().apply {
                                putParcelable(KEY_MEDIA_METADATA, data)
                            }

                            val description = MediaDescriptionCompat.Builder().apply {
                                val mediaId = parentId + "/" + data.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
                                setMediaId(mediaId)
                                setExtras(bundle)
                            }.build()
                            mediaItemList += MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
                        }
                        result.sendResult(mediaItemList)
                    } else {
                        mediaSession.sendSessionEvent(LOAD_VIDEO_FAILURE, null)
                        result.sendResult(null)
                    }
                }
                // If the results are not ready, the service must "detach" the results before
                // the method returns. After the source is ready, the lambda above will run,
                // and the caller will be notified that the results are ready.
                //
                // See [MediaItemFragmentViewModel.subscriptionCallback] for how this is passed to the
                // UI/displayed in the [RecyclerView].
                if (!resultsSent) {
                    result.detach()
                }
            }
            else -> {
                result.sendResult(null)
            }
        }
    }
}

const val KEY_MEDIA_METADATA = "KEY_MEDIA_METADATA"
const val LOAD_SONG_FAILURE = "LOAD_SONG_FAILURE"
const val LOAD_VIDEO_FAILURE = "LOAD_VIDEO_FAILURE"