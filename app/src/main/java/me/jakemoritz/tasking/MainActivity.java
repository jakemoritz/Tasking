package me.jakemoritz.tasking;

import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.plus.Plus;
import com.google.android.gms.plus.model.people.Person;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

    private static final String TAG = "MainActivity";

    /* Request code used to invoke sign in user interactions. */
    private static final int RC_SIGN_IN = 0;

    /* Client used to interact with Google APIs. */
    private GoogleApiClient mGoogleApiClient;

    /* Is there a ConnectionResult resolution in progress? */
    private boolean mIsResolving = false;

    /* Should we automatically resolve ConnectionResults when possible? */
    private boolean mShouldResolve = false;

    // Declare variables for views
    ImageView navUserAvatar;
    TextView navUserName;
    TextView navUserEmail;
    LinearLayout navUserCover;

    boolean wantToLoadUserImages;
    boolean wantToSignOut;

    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Plus.API)
                .addScope(new Scope(Scopes.PROFILE))
                .addScope(new Scope(Scopes.EMAIL))
                .build();
        mGoogleApiClient.connect();

        wantToLoadUserImages = true;

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View header = LayoutInflater.from(this).inflate(R.layout.nav_header_main, navigationView);

        navUserAvatar = (ImageView) header.findViewById(R.id.user_avatar);
        navUserName = (TextView) header.findViewById(R.id.user_name);
        navUserEmail = (TextView) header.findViewById(R.id.user_email);
        navUserCover = (LinearLayout) header.findViewById(R.id.user_cover);

        loadNavUserName();
        loadNavUserEmail();

        // Initialize default fragment
        getFragmentManager().beginTransaction()
                .replace(R.id.content_main, new TaskListFragment())
                .commit();
    }

    public void loadNavUserName(){
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shared_prefs_account), 0);
        String name = sharedPreferences.getString(getString(R.string.shared_prefs_name), "");

        navUserName.setText(name);
    }

    public void loadNavUserEmail(){
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shared_prefs_account), 0);
        String email = sharedPreferences.getString(getString(R.string.shared_prefs_email), "");

        navUserEmail.setText(email);
    }

    public void saveImageToFile(Bitmap bitmap, String filename){
        FileOutputStream outputStream;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] bitmapByteArray = byteArrayOutputStream.toByteArray();
        bitmap.recycle();
        String filepath = getCacheDir() + File.separator + filename;
        try {
            outputStream = new FileOutputStream(new File(filepath), true);
            outputStream.write(bitmapByteArray);
            outputStream.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public Bitmap loadImageFromFile(String filename){
        // Try to load image from file
        FileInputStream inputStream = null;
        File file = new File(getCacheDir() + File.separator + filename);

        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException fileNotFoundException){
            return null;
        } catch (Exception e){
            e.printStackTrace();
        }

        return BitmapFactory.decodeStream(inputStream);
    }

    public void loadNavUserImage(){
        // If an image is loaded, pass it
        if (loadImageFromFile(getString(R.string.user_image)) != null){
            navUserAvatar.setImageBitmap(getCircleBitmap(loadImageFromFile(getString(R.string.user_image))));
        }
        // If no image is loaded, pull from servers
        else {
            //mGoogleApiClient.connect();
            Person user = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
            if (user != null && user.getImage() != null){
                new AsyncTask<String, Void, Bitmap>(){
                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        if (bitmap != null){
                            Bitmap copy = bitmap.copy(Bitmap.Config.ARGB_8888, false);

                            Bitmap userImage = getCircleBitmap(bitmap);
                            navUserAvatar.setImageBitmap(userImage);
                            saveImageToFile(copy, getString(R.string.user_image));
                        }
                    }

                    @Override
                    protected Bitmap doInBackground(String... params) {
                        try {
                            URL url = new URL(params[0]);
                            InputStream in = url.openStream();
                            return BitmapFactory.decodeStream(in);
                        } catch (Exception e){
                            Log.e(TAG, e.toString());
                        }
                        return null;
                    }
                }.execute(user.getImage().getUrl());
            }
            //mGoogleApiClient.disconnect();
        }
    }

    public void loadNavUserCoverImage(){
        final Paint darken = new Paint();
        darken.setColor(Color.BLACK);
        darken.setAlpha(100);

        // If an image is loaded, pass it
        if (loadImageFromFile(getString(R.string.user_cover_image)) != null){
            Bitmap bitmapCopy = loadImageFromFile(getString(R.string.user_cover_image)).copy(Bitmap.Config.ARGB_8888, true);
            Canvas c = new Canvas(bitmapCopy);
            c.drawPaint(darken);
            navUserCover.setBackground(new BitmapDrawable(getResources(), bitmapCopy));
        }
        // If no image is loaded, pull from servers
        else {
            //mGoogleApiClient.connect();
            Person user = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient);
            if (user != null && user.getCover() != null) {
                new AsyncTask<String, Void, Bitmap>() {
                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        if (bitmap != null){
                            Bitmap copy = bitmap.copy(Bitmap.Config.ARGB_8888, false);

                            Bitmap bitmapCopy = bitmap.copy(Bitmap.Config.ARGB_8888, true);
                            Canvas c = new Canvas(bitmapCopy);
                            c.drawPaint(darken);
                            navUserCover.setBackground(new BitmapDrawable(getResources(), bitmapCopy));

                            saveImageToFile(copy, getString(R.string.user_cover_image));
                        }
                    }

                    @Override
                    protected Bitmap doInBackground(String... params) {
                        try {
                            URL url = new URL(params[0]);
                            InputStream in = url.openStream();
                            return BitmapFactory.decodeStream(in);
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                        return null;
                    }
                }.execute(user.getCover().getCoverPhoto().getUrl());
            }
            mGoogleApiClient.disconnect();
            wantToLoadUserImages = false;

        }
    }

    public void signOutHelper(){
        mGoogleApiClient.connect();
        Plus.AccountApi.clearDefaultAccount(mGoogleApiClient);
        mGoogleApiClient.disconnect();

        clearAppData();

        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shared_prefs_account), 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.shared_prefs_logged_in), false);
        editor.commit();

        wantToSignOut = false;
        startActivity(new Intent(this, HelperActivity.class));

    }

    public void clearAppData(){
        File cache = getCacheDir();
        File appDir = new File(cache.getParent());
        if (appDir.exists()){
            String[] children = appDir.list();
            for (String s : children){
                if (!s.equals("lib")){
                    deleteDir(new File(appDir, s));
                    Log.i(TAG, "File /data/data/me.jakemoritz.tasking/" + s + " DELETED");
                }
            }
        }
    }

    public static boolean deleteDir(File dir){
        if (dir != null && dir.isDirectory()){
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++){
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success){
                    return false;
                }
            }
        }

        return dir.delete();
    }

    public void signOut(){
        //wantToSignOut = true;
        //mGoogleApiClient.connect();
        signOutHelper();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_tasks) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_main, new TaskListFragment())
                    .commit();
        }
        else if (id == R.id.nav_settings) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.content_main, new SettingsFragment())
                    .commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onConnected(Bundle bundle) {
        // onConnected indicates that an account was selected on the device, that the selected
        // account has granted any requested permissions to our app and that we were able to
        // establish a service connection to Google Play services.
        Log.d(TAG, "onConnected:" + bundle);
        mShouldResolve = false;

        if (wantToLoadUserImages){
            loadNavUserImage();
            loadNavUserCoverImage();
        }
        if (wantToSignOut){
            //signOutHelper();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mGoogleApiClient.connect();
    }

    private Bitmap getCircleBitmap(Bitmap bitmap){
        final Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        final Canvas canvas = new Canvas(output);

        final int color = Color.RED;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawOval(rectF, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        bitmap.recycle();

        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, getResources().getDisplayMetrics());
        return Bitmap.createScaledBitmap(output, px, px, true);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
//        Couldn't connect to Google Play Services. The user needs to select an account,
//        grant permissions or resolve an error in order to sign in. Refer to the javadoc for
//        Connection Result to see possible error codes.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);

        if (!mIsResolving && mShouldResolve){
            if (connectionResult.hasResolution()){
                try {
                    connectionResult.startResolutionForResult(this, RC_SIGN_IN);
                    mIsResolving = true;
                } catch (IntentSender.SendIntentException e){
                    Log.e(TAG, "Could not resolve ConnectionResult.", e);
                    mIsResolving = false;
                    mGoogleApiClient.connect();
                }
            } else {
                // Could not resolve the connection result, show the user an
                // error dialog.

            }
        } else {
            // Show the signed-out UI

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult:" + requestCode + ":" + resultCode + ":" + data);

        if (requestCode == RC_SIGN_IN) {
            // If the error resolution was not successful we should not resolve further.
            if (resultCode != RESULT_OK) {
                mShouldResolve = false;
            }

            mIsResolving = false;
            mGoogleApiClient.connect();
        }
    }
}
