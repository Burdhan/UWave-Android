package happinessdevelopment.u_wave;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sbstrm.appirater.Appirater;
import com.wnafee.vector.MorphButton;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import happinessdevelopment.u_wave.MusicService.LocalBinder;

/**
 * This is our main class that is displayed after the launcher screen.
 * It is where the user interacts with the application.
 *
 * @author H[App]iness Development
 * @version 1.0.0
 *          <p/>
 *          This is where we set up our buttons, start our music service and
 *          handle other tasks like making an icon notification.
 */
public class MainActivity extends FragmentActivity {
    // This is used for identifying our notification.
    private static final int NOTIFICATION_ID = 1;
    // Used for accessing music service/creating our own instance here to use.
    MusicService musicService;
    // Used for toggling play and pause button.
    boolean volume = false;

    // This is used for monitoring the state of our music service.
    ServiceConnection mConnection = new ServiceConnection() {
        /**
         * This method is for what happens when we disconnect from our service.
         *
         * @param name This is the name of the service that was disconnected from.
         */
        public void onServiceDisconnected(ComponentName name) {
            // If we were to disconnect, we set our music player to null.
            musicService = null;
        }

        /**
         * This is the method used if we were able to connect to our music service.
         *
         * @param name    This is the name of the service.
         * @param service This is an instance of a bind we want to bind to.
         */
        public void onServiceConnected(ComponentName name, IBinder service) {
            /*
             Here we make an instance of our local binder which we assign to the
             binder passed through the parameter.
              */
            LocalBinder mLocalBinder = (LocalBinder) service;
            /*
             Once our local binder is set, we get the instance of the music service from
             the local binder so we can assign it to our instance of the music player so
             that we can access it and manipulate it.
             */
            musicService = mLocalBinder.getServerInstance();
        }
    };

    /*
                 This notification manager allows us to inform the user
                  that this app is running in the background
                  */
    private NotificationManager mNotifyMgr;

    // Assigning our buttons
    private TextView songName;  // Text box for song name
    private TextView albumName; // Text box for album name
    private TextView artistName; // Text box for artist name
    private MorphButton mb;
    private ProgressBar progressBar;

    /**
     * What occurs when the application is started/created.
     *
     * @param savedInstanceState Bundle object that is passed through
     *                           into the method of every activity.
     *                           <p/>
     *                           Here we also set the view of the application to the main screen, add our icon
     *                           in the notification bar, get our track information, and set our play and pause
     *                           buttons to do their actions.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Default Stuff that happens when app created.
        super.onCreate(savedInstanceState);
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // New intent from this class to the music player service class.
        Intent mIntent = new Intent(this, MusicService.class);
        // Binding our intent and also check the connection between the two.
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
        setContentView(R.layout.activity_main); // Setting our view to the main screen.

        // Used for letting user rate the app.
        Appirater.appLaunched(this, null);

        // Create our notification icon
        makeNotificationIcon();

        // Initializing our buttons and text views to their respective icons on the screen.
        songName = (TextView) findViewById(R.id.song_name);
        albumName = (TextView) findViewById(R.id.album_name);
        artistName = (TextView) findViewById(R.id.artist_name);
        mb = (MorphButton) findViewById(R.id.drawerBtn);
        
        mb.setOnStateChangedListener(new MorphButton.OnStateChangedListener() {
            @Override
            public void onStateChanged(MorphButton.MorphState changedTo, boolean isAnimating) {
                if (musicService.isBuffering()) {
                    mb.setState(MorphButton.MorphState.START);
                    Toast.makeText(MainActivity.this, "Connecting", Toast
                            .LENGTH_SHORT).show();
                } else {
                    if (changedTo.equals(MorphButton.MorphState.END)) {
                        musicService.unMute();
                    } else
                        musicService.mute();
                }
            }
        });
        // We now display the song information.
        displaySongInfo();
    }

    /**
     * This method is used for releasing the application when we are done.
     */
    @Override
    public void onDestroy() {
        // Getting rid of the notification icon on notification bar.
        mNotifyMgr.cancel(NOTIFICATION_ID);
        // Removes the bind from the server.
        unbindService(mConnection);
        // Stopping the music service
        musicService.onDestroy();
        // Stop the application
        super.onDestroy();
    }

    /**
     * This method is what we use for getting the song information.
     * <p/>
     * We parse a json from a url containing the song name, the album, and the artist.
     */
    private void displaySongInfo() {
        /*
        Create a new thread so this can happen in the background since it
        will continue to update to keep the information up-to-date.
         */
        Thread t = new Thread() {
            /**
             * This is what will be running in the background.
             */
            @Override
            public void run() {
                try {
                    // Checking if our receiver has been interrupted.
                    while (!isInterrupted()) {
                        /*
                        This line pause this thread for 1000 millis so
                        that it updates our information every second.
                         */
                        Thread.sleep(1000);
                        /*
                        This is similar to our run above that happens but its for our UI,
                        if there is something queued on our UI then this will enter the
                        queue. Otherwise it will happen immediately.
                         */
                        runOnUiThread(new Runnable() {
                            /**
                             * This is what runs on the UI
                             */
                            @Override
                            public void run() {
                                /*
                                Makes a new instance of our JSONTask class
                                (created within the MainActivity class) and parses
                                the given url for get the song, album, and artist.
                                 */
                                new JSONTask().execute("https://uwave.fm/listen/now-playing.json");
                            }
                        });
                    }
                } catch (Exception e) {
                    // If any errors occurred during the thread, the stackTrace will be printed.
                    e.printStackTrace();
                }
            }
        };
        t.start(); // Start our thread
    }

    /**
     * This is where we create our notification icon.
     */
    private void makeNotificationIcon() {
        // This is how we control our notification as well as construct our notification layout.
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.u_wave_play).setContentTitle("U Wave")
                .setContentText("Return to player");
        // Creating an intent from this notification to our main activity.
        Intent resultIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(getPackageManager().getLaunchIntentForPackage(getPackageName()).getComponent());
        /*
         Similar to a regular intent except it includes target actions to perform.
         For this case it simply tales you back to the application main screen when performed.
          */
        mBuilder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, 0));
        //This creates the notification which the user will be able to see based on what we created.
        Notification notification = mBuilder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

        // This tells our notification manager what type of notification it is.
        mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // This notifies the user of our notification that the app is running.
        mNotifyMgr.notify(NOTIFICATION_ID, notification);
    }

    /**
     * This is our separate class that we use for parsing our JSON text from the given url.
     * <p/>
     * Since the audio changes, the JSON track information has to be updated every
     * second to see if there is a change.
     */
    public class JSONTask extends AsyncTask<String, String, String> {

        /**
         * This is what happens in the background when getting the JSON text and parsing it.
         *
         * @param params This is an array of strings containing any useful
         *               information. Like the url of what JSON we are parsing.
         * @return We return a string containing the information we need from the JSON text.
         */
        @Override
        protected String doInBackground(String... params) {
            // This is our url we are getting our track info from.
            HttpURLConnection connection = null;
            // This is the reader used when parsing the JSON text.
            BufferedReader reader = null;

            // Use a try-catch to test if we can parse from the url.
            try {
                URL url = new URL(params[0]); // Assign our url.
                // Attempt to connect to the url so we can pull information.
                connection = (HttpURLConnection) url.openConnection();
                // If it works we connect.
                connection.connect();

                // Create an input stream from the connection we formed to the url.
                InputStream stream = connection.getInputStream();
                // Assign our reader to the stream.
                reader = new BufferedReader(new InputStreamReader(stream));

                /*
                 Create a string buffer that takes a squence of characters and
                 creates strings from it.
                  */
                StringBuffer buffer = new StringBuffer();
                // Used for storing the buffered string.
                String line;

                /*
                 While loop that will continue until the string line (which was set to
                 the reader) equals null.
                  */
                while ((line = reader.readLine()) != null) {
                    buffer.append(line); // Our buffer then appends the line.
                }

                // Next we create a string that is equal to our buffer in string format.
                String finalJson = buffer.toString();

                /*
                Here we being the parsing of the information we want.
                We first create a JSONObject which is everything with a JSON's brackets.
                We create it from our string that we got from the url.
                 */
                JSONObject parentObject = new JSONObject(finalJson);
                // Get the song name by looking for a string with the tag title.
                String songName = parentObject.getString("title");
                // Get the artist name by looking for a string with the tag artist.
                String artistName = parentObject.getString("artist");
                // Get the artist name by looking for a string with the tag album.
                String albumName = parentObject.getString("album");

                // Returning a string containing the information we want.
                return (songName + "\n" + albumName + "\n" + artistName);

            } catch (Exception e) {
                e.printStackTrace(); // Print stack if error occurred during connecting and parsing.
            } finally {
                // Once we are done we check if connection is null, is so then we can disconnect.
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    /*
                    If our reader is empty then we can also close the stream
                     since its no longer needed.
                     */
                    if (reader != null) {
                        reader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace(); // Print any errors if we couldn't close the reader.
                }
            }
            // In case it doesn't work we return null.
            return null;
        }

        /**
         * What occurs once we are done parsing the JSON
         *
         * @param result This is the result that was return after it finished parsing.
         */
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            // Check if results is null
            if (result != null) {
                // Create a scanner from the result string to get individual items.
                Scanner sc = new Scanner(result);
                String[] songInfo = new String[3]; // For song, album, artist.
                for (int i = 0; i < 3; i++) {
                    // If it does contain content the we assign the data.
                    if (sc.hasNextLine()) {
                        songInfo[i] = sc.nextLine();
                    } else {
                        // Otherwise we just leave it blank.
                        songInfo[i] = "";
                    }
                }
                // We finally change the text in the text boxes for each of the three components.
                songName.setText(songInfo[0]);
                albumName.setText(songInfo[1]);
                if (!(songInfo[2].equals(""))) {
                    artistName.setText("By " + songInfo[2]);
                }
            }
        }
    }

    /**
     * Prevents the back button from finishing the activity, so the app isn't
     * closed by accident.
     */
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}