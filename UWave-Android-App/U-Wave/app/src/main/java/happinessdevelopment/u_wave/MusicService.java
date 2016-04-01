package happinessdevelopment.u_wave;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

/**
 * This class is where our media player is made and controlled from.
 *
 * @author H[App]iness Development
 * @version 1.0.0
 *          <p/>
 *          This class is used as a service, so that it allows us to run the program in the
 *          background so that the user can do other activities while the music is playing.
 */
public class MusicService extends Service {
    // Where we will be streaming from. Keep it as final since it shouldn't change.
    final String streamURL = "http://live.uwave.fm:8000/listen-128.mp3";
    /*
    This is our binder. It allows us to communicate from our main class (MainActivity.java)
    with this music player service. That way we can use methods created here in our main class.
     */
    IBinder mBinder = new LocalBinder();
    // This is our media player which we will use to play our stream.
    private MediaPlayer mediaPlayer;
    /* Used for muting and un-muting our stream since pausing and un-pausing our
    stream takes a long time to buffer.
    */
    private float volumeLevel = 0;
    // Used for checking if media player is still buffering.
    private boolean buffering = true;

    /**
     * This is our onBind method, it is used for returning the communication channel to this music
     * service.
     *
     * @param intent This was the intent that was used to bind to this music service.
     * @return We return an IBinder that allows our main activity to call on to this music service.
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * This is what happens when this music service is called/created.
     * <p/>
     * We assign our music player to the stream we want and begin preparing
     * our stream, when its ready it will begin playing the stream.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MusiceService", "onCreate"); // Used for keeping logs, shows up in console.

        // Surround our assigning in a try-catch incase it doesn't work.
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            // Its a stream type from web
            mediaPlayer.setDataSource(streamURL); // Setting our url as data source
            mediaPlayer.prepareAsync(); // Buffering
            mediaPlayer.setVolume(0, 0); // Set volume to 0 until play button is hit.

            //We create a listener to keep a track of whether it's done buffering.
            try {
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    /**
                     * Once its done preparing the music stream on the music player
                     * we can begin playing music.
                     * @param player This is our music player being passed through.
                     */
                    @Override
                    public void onPrepared(MediaPlayer player) {
                        buffering = false; // Since it is no longer buffering
                        /*
                        This simply displays a message on the app to inform the user that we
                         have connected to the stream and the player is done buffering.
                         */
                        Toast.makeText(MusicService.this, "Connected", Toast.LENGTH_SHORT).show();
                        player.start(); // Start playing music, but its mute since volume is 0.
                    }
                });
            } catch (Exception e) {
                //Print where the error came from if any occurred during the set up of the listener.
                e.printStackTrace();
            }
        } catch (Exception e) {
            //Print where the error came from if any occurred during the set up of the media player.
            e.printStackTrace();
        }
    }

    /**
     * This is how we 'stop' the stream.
     * <p/>
     * Because actually stopping the stream creates a delay that the user has to
     * experience for the music to restart, since it has to buffer again. So our simple solution
     * for now is to just mute it.
     */
    public void mute() {
        mediaPlayer.setVolume(0, 0);
    }

    /**
     * This is how we 'play' the stream.
     * <p/>
     * We simply put the volume back up since we did the opposite to mute it.
     */
    public void unMute() {
        mediaPlayer.setVolume(1, 1);
    }

    /**
     * This is what we use to check if the music player has completed buffering.
     * <p/>
     * If it has not, then the user cannot press the play button.
     *
     * @return Returns a true or false based on whether it has completely buffered or not.
     */
    public boolean isBuffering() {
        return buffering;
    }

    /**
     * This is used to end the service.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public MediaPlayer getMediaPlayer(){
        return mediaPlayer;
    }
    /**
     * Create a local binder that we use for connecting between the MainActivity
     * and this music player service.
     */
    public class LocalBinder extends Binder {
        /**
         * This is the method we use for getting the instance of this music player service.
         *
         * @return Returns this instance of the class.
         */
        public MusicService getServerInstance() {
            return MusicService.this;
        }
    }
}