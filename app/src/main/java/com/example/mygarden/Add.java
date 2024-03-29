package com.example.mygarden;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.CursorWindow;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.mygarden.database.DBHelper;
import com.example.mygarden.database.DatabaseAccess;
import com.example.mygarden.model.Notification;
import com.example.mygarden.model.Plant;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class Add extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    EditText name, localization, notes;
    Spinner spinner;
    ImageView photo;
    DBHelper DB;

    public Uri imageFilePath;
    public Bitmap imageToStore;
    private static final int PICK_IMAGE_REQUEST=100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        setContentView(R.layout.activity_add);

        ////////////////////////////////spinner///////////////////////////////////////////////
        this.spinner = findViewById(R.id.spinner);
        DatabaseAccess databaseAccess = DatabaseAccess.getInstance(this);
        databaseAccess.open();
        List<String> plant_species = databaseAccess.getSpecies();
        databaseAccess.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_custom, plant_species);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_layout);
        this.spinner.setAdapter(adapter);
        //////////////////////////////////////////////////////////////////////////////////////

        //////////////////////////////////button - previous////////////////////////////////////////
        Button previous_button = findViewById(R.id.previous_add);
        previous_button.setOnClickListener(v -> onBackPressed());
        ////////////////////////////////////////////////////////////////////////////////////////////

        try {
            photo = findViewById(R.id.image);
            name = findViewById(R.id.plant_name);
            localization = findViewById(R.id.plant_localization);
            notes = findViewById(R.id.plant_notes);
            DB = new DBHelper(this);
        }
        catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        try {
            Field field = CursorWindow.class.getDeclaredField("sCursorWindowSize");
            field.setAccessible(true);
            field.set(null, 50 * 1024 * 1024); //the 100MB is the new size
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /////////////////////////////////////////image//////////////////////////////////////////////
    public void chooseImage(View view){
        try {
            Intent objectIntent=new Intent();
            objectIntent.setType("image/*");

            objectIntent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(objectIntent, PICK_IMAGE_REQUEST);
        }
        catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        try {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
                imageFilePath = data.getData();
                imageToStore = MediaStore.Images.Media.getBitmap(getContentResolver(), imageFilePath);

                photo.setImageBitmap(imageToStore);
            }
        }
        catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    ////////////////////////////////////store data//////////////////////////////////////////////////
    public void storeData(View view){
        try {
            if (name.getText().toString().isEmpty()) {
                Toast.makeText(this, "Podaj nazwę rośliny", Toast.LENGTH_SHORT).show();
            } else if (localization.getText().toString().isEmpty()) {
                Toast.makeText(this, "Podaj lokalizację rośliny", Toast.LENGTH_SHORT).show();
            } else if (!name.getText().toString().isEmpty() && !localization.getText().toString().isEmpty() && photo.getDrawable() != null && imageToStore != null) {
                Plant plant = new Plant(name.getText().toString(), localization.getText().toString(), spinner.getSelectedItem().toString(), notes.getText().toString(), imageToStore);
                DB.storeData(plant);
                Intent i = new Intent(Add.this, Plants.class);
                startActivity(i);
                overridePendingTransition(0,0 );

                /////////////////////////////////////notifications//////////////////////////////////////
                DatabaseAccess databaseAccess=DatabaseAccess.getInstance(getApplicationContext());

                databaseAccess.open();
                int[] wfr = databaseAccess.getWFR(spinner.getSelectedItem().toString());
                int water = wfr[0] *24* 60*60*1000;
                int fertilizer = wfr[1]*24* 60*60*1000;
                int  repot = wfr[2]*24* 60*60*1000;
                databaseAccess.close();

                ByteArrayOutputStream blob = new ByteArrayOutputStream();
                imageToStore.compress(Bitmap.CompressFormat.JPEG, 0 , blob);
                byte[] picture = blob.toByteArray();
                
                int notificationid = (int) System.currentTimeMillis();

                int l_roslin = DB.getAllPlantsData().size()-1;
                ArrayList<Plant> plantArrayList = DB.getAllPlantsData();
                Plant p = plantArrayList.get(l_roslin);

                Notification notif = new Notification(p.getId(),notificationid);
                PlantInfo.notifications.add(notif);

                createNotificationChannel();
                generateNotification(p.getId(),picture,R.drawable.ic_watercan,name.getText().toString(), localization.getText().toString(),"Woda", " potrzebuje wody", notificationid, water);
                generateNotification(p.getId(),picture,R.drawable.ic_fertilizer,name.getText().toString(), localization.getText().toString(),"Nawóz", " potrzebuje nawozu", notificationid+1, fertilizer);
                generateNotification(p.getId(),picture,R.drawable.ic_repot,name.getText().toString(), localization.getText().toString(),"Przesadzanie", " potrzebuje przesadzenia", notificationid+2, repot);

            } else {
                Bitmap plant_img = BitmapFactory.decodeResource(this.getResources(), R.drawable.plant_photo);
                Plant plant = new Plant(name.getText().toString(), localization.getText().toString(), spinner.getSelectedItem().toString(), notes.getText().toString(), plant_img);
                DB.storeData(plant);
                Intent i = new Intent(Add.this, Plants.class);
                startActivity(i);
                overridePendingTransition(0,0 );

                /////////////////////////////////////notifications//////////////////////////////////////
                DatabaseAccess databaseAccess=DatabaseAccess.getInstance(getApplicationContext());

                databaseAccess.open();
                int[] wfr = databaseAccess.getWFR(spinner.getSelectedItem().toString());
                int water = wfr[0];
                int fertilizer = wfr[1];
                int  repot = wfr[2];
                databaseAccess.close();

                ByteArrayOutputStream blob = new ByteArrayOutputStream();
                plant_img.compress(Bitmap.CompressFormat.JPEG, 0 , blob);
                byte[] picture = blob.toByteArray();

                int notificationid = (int) System.currentTimeMillis();

                int l_roslin = DB.getAllPlantsData().size()-1;
                ArrayList<Plant> plantArrayList = DB.getAllPlantsData();
                Plant p = plantArrayList.get(l_roslin);

                Notification notif = new Notification(p.getId(),notificationid);
                PlantInfo.notifications.add(notif);

                createNotificationChannel();
                generateNotification(p.getId(),picture,R.drawable.ic_watercan,name.getText().toString(), localization.getText().toString(),"Woda", " potrzebuje wody", notificationid, water);
                generateNotification(p.getId(),picture,R.drawable.ic_fertilizer,name.getText().toString(), localization.getText().toString(),"Nawóz", " potrzebuje nawozu", notificationid+1, fertilizer);
                generateNotification(p.getId(),picture,R.drawable.ic_repot,name.getText().toString(), localization.getText().toString(),"Przesadzanie", " potrzebuje przesadzenia", notificationid+2, repot);
            }

        }
        catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Channel";
            String description = "Channel for notification";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel;
            channel = new NotificationChannel("notificationID", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void generateNotification(int plantID, byte[] image,int notificationicon, String name, String localization, String activity, String activitydetails, int id, int time){
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        GregorianCalendar calendar = (GregorianCalendar) GregorianCalendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        Intent intent = new Intent(this, ReminderBroadcast.class);
        intent.putExtra("keynotificationicon", notificationicon);
        intent.putExtra("keyname", name);
        intent.putExtra("keyactivity", activity);
        intent.putExtra("keyactivitydetails", activitydetails);
        intent.putExtra("keyid", id);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, id, intent, 0);


        Intent intent2 = new Intent(this, TaskBroadcast.class);
        intent2.putExtra("keyplantid", plantID);
        intent2.putExtra("keyactivity", activity);
        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(this, id, intent2, 0);

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), time, pendingIntent); //AlarmManager.INTERVAL_DAY
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), time, pendingIntent2);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////spinner/////////////////////////////////////////////////////////////
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        String item = parent.getItemAtPosition(position).toString();
    }
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void setLocale(String lang) {
        Resources resources = getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.JELLY_BEAN_MR1){
            config.setLocale(new Locale(lang.toLowerCase()));
        } else {
            config.locale = new Locale(lang.toLowerCase());
        }
        resources.updateConfiguration(config, dm);

        PreferenceManager.getDefaultSharedPreferences(this).edit().putString("My_Lang", lang).apply();
    }

    public void loadLocale() {
        String language = PreferenceManager.getDefaultSharedPreferences(this).getString("My_Lang", "");
        setLocale(language);
    }
}