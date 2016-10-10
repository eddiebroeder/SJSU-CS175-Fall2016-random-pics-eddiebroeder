package com.example.eddie.randompictures;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.StrictMode;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    SQLiteDatabase db;
    String client_id = "dc66384ce800c4031a45ee529316c40fd0169bb39c49bd1ceaa2c7cc20881e9e";
    final int PHOTO_COUNT = 10;
    Bitmap[] photos = new Bitmap[10];
    URL[] photoURLs = new URL[10];
    ImageView image;
    int currentImageIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        image = (ImageView) findViewById(R.id.photo);
    }

    public void getPhotos(View view) {
        currentImageIndex = 0;
        EditText searchField= (EditText) findViewById(R.id.searchText);
        String searchText = searchField.getText().toString();
        String loadString;
        image.setImageResource(R.drawable.loading);
        if (!searchText.equals("")) {
            loadString = "https://api.unsplash.com/search/photos/?client_id=" + client_id + "&query=" + searchText + "&count=" + PHOTO_COUNT;
        }
        else {
            loadString = "https://api.unsplash.com/photos/random/?client_id=" + client_id + "&count=" + PHOTO_COUNT;
        }
        Ion.with(this)
                .load(loadString)
                .asString()
                .setCallback(new FutureCallback<String>() {
                    @Override
                    public void onCompleted(Exception e, String result) {
                        processArrayData(result);
                    }
                });
    }

    public void processArrayData(String result) {
        try {
            JSONArray jsonArray;
            if (result.contains("\"total\":0,\"total_pages\":0,\"results\":[]")) {
                Log.d("RESULTS: ", result);
                image.setImageResource(R.drawable.noresults);
                return;
            }
            else if (result.startsWith("[")) {
                jsonArray = new JSONArray(result);
            }
            else {
                JSONObject json = new JSONObject(result);
                jsonArray = json.getJSONArray("results");
            }
            for (int i = 0; i < PHOTO_COUNT; i++) {
                photoURLs[i] = new URL(jsonArray.getJSONObject(i).getJSONObject("urls").getString("regular"));
                photos[i] = BitmapFactory.decodeStream(photoURLs[i].openConnection().getInputStream());
            }
            image.setImageBitmap(photos[0]);

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getNextPhoto(View view) {
        if (currentImageIndex < 9) {
            image.setImageBitmap(photos[++currentImageIndex]);
        }
    }

    public void getPreviousPhoto(View view) {
        if (currentImageIndex > 0) {
            image.setImageBitmap(photos[--currentImageIndex]);
        }
    }

    public void savePhoto(View view) {
        ContentValues values = new ContentValues();
        try {
            values.put("url", photoURLs[currentImageIndex].toString());
        }
        catch (NullPointerException e) {
            return;
        }
        db = openOrCreateDatabase("photos", MODE_APPEND, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS saved (url varchar)");
        db.beginTransaction();
        db.insertOrThrow("saved", null, values);
        db.endTransaction();
        db.close();
    }

    public void showSavedPhotos(View view) {

        String urlString = "";
        db = openOrCreateDatabase("photos", MODE_APPEND, null);
        Cursor cursor = db.rawQuery("SELECT * FROM saved", null);
        cursor.moveToFirst();
        while (cursor.moveToNext()) {
            urlString += cursor.getString(0) + "\n";
        }
        cursor.close();
        db.close();
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Saved Photo Urls");
        alertDialog.setMessage(urlString);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "CLOSE",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }
}
