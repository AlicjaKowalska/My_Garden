package com.example.mygarden;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationManagerCompat;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorWindow;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mygarden.database.DBHelper;
import com.example.mygarden.database.DatabaseAccess;
import com.example.mygarden.database.Plant;
import com.example.mygarden.database.Task;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class Edit extends AppCompatActivity {

    public Uri imageFilePath;
    public Bitmap imageToStore;
    private static final int PICK_IMAGE_REQUEST=100;

    TextView name, localization, notes;
    Spinner species;
    ImageView photo;
    int edit_id;

    String edit_name, edit_localization, edit_species, edit_notes;
    Bitmap edit_photo;

    DBHelper DB;
    Plant plant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        try {
            Field field = CursorWindow.class.getDeclaredField("sCursorWindowSize");
            field.setAccessible(true);
            field.set(null, 50 * 1024 * 1024); //the 100MB is the new size
        } catch (Exception e) {
            e.printStackTrace();
        }

        ////////////////////////////////spinner///////////////////////////////////////////////
        this.species = (Spinner) findViewById(R.id.edit_spinner);
        DatabaseAccess databaseAccess = DatabaseAccess.getInstance(this);
        databaseAccess.open();
        List<String> plant_species = databaseAccess.getSpecies();


        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.custom_spinner, plant_species);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_layout);
        this.species.setAdapter(adapter);
        //////////////////////////////////////////////////////////////////////////////////////
        name = (TextView) findViewById(R.id.edit_name);
        localization = (TextView) findViewById(R.id.edit_localization);
        notes = (TextView) findViewById(R.id.edit_notes);
        photo = (ImageView) findViewById(R.id.edit_image);

        edit_id = getIntent().getIntExtra("editid", 0);
        DatabaseAccess dbAccess=DatabaseAccess.getInstance(getApplicationContext());
        DB = new DBHelper(this);
        ArrayList<Plant> plantArrayList = DB.getAllPlantsData();
        plant = plantArrayList.get(edit_id);

        edit_name = getIntent().getStringExtra("editname");
        edit_localization = getIntent().getStringExtra("editlocalization");
        edit_species = getIntent().getStringExtra("editspecies");
        edit_notes = getIntent().getStringExtra("editnotes");
        edit_photo = plant.getImage();

        name.setText(edit_name);
        localization.setText(edit_localization);
        species.setSelection(plant_species.indexOf(edit_species));
        notes.setText(edit_notes);
        photo.setImageBitmap(edit_photo);

        databaseAccess.close();
        Button previous_button = findViewById(R.id.previous_edit);
        previous_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    ///////////////////////////////////////update data//////////////////////////////////////////////
    public void updateData(View view){
        try{
            edit_id = getIntent().getIntExtra("editid", 0);
            DatabaseAccess dbAccess=DatabaseAccess.getInstance(getApplicationContext());
            DB = new DBHelper(this);
            ArrayList<Plant> plantArrayList = DB.getAllPlantsData();
            plant = plantArrayList.get(edit_id);

            if(name.getText().toString().equals(plant.getName())) plant.setName(plant.getName()); else plant.setName(name.getText().toString());
            if(localization.getText().toString().equals(plant.getLocalization())) plant.setLocalization(plant.getLocalization()); else plant.setLocalization(localization.getText().toString());
            if(species.getSelectedItem().toString().equals(plant.getSpecies())) plant.setSpecies(plant.getSpecies()); else plant.setSpecies(species.getSelectedItem().toString());
            if(notes.getText().toString().equals(plant.getNotes())) plant.setNotes(plant.getNotes());  else  plant.setNotes(notes.getText().toString());
            if(imageToStore==null) plant.setImage(plant.getImage()); else plant.setImage(imageToStore);

            DB.updateData(plant);
            Intent i = new Intent(Edit.this, Plants.class);
            startActivity(i);


            ///////////////////////////////notifications////////////////////////////////////////////
            DatabaseAccess databaseAccess=DatabaseAccess.getInstance(getApplicationContext());

            databaseAccess.open();
            int[] wfr = databaseAccess.getWFR(species.getSelectedItem().toString());
            int water = wfr[0] *24* 60*60*1000;
            int fertilizer = wfr[1]*24* 60*60*1000;
            int  repot = wfr[2]*24* 60*60*1000;
            databaseAccess.close();

            String n, l;
            Bitmap p;

            if(name.getText().toString().equals(plant.getName())) n = plant.getName(); else n = name.getText().toString();
            if(localization.getText().toString().equals(plant.getLocalization())) l = plant.getLocalization(); else l = localization.getText().toString();
            if(imageToStore==null) p = plant.getImage(); else p = imageToStore;

            ByteArrayOutputStream blob = new ByteArrayOutputStream();
            p.compress(Bitmap.CompressFormat.JPEG, 0 , blob);
            byte[] picture = blob.toByteArray();

            int id2 = (int) System.currentTimeMillis();
            com.example.mygarden.Notification notif = new com.example.mygarden.Notification(plant.getId(),id2);
            PlantInfo.notifications.add(notif);

            int j=0;
            if(PlantInfo.notifications.size()>0) {
                while (j < PlantInfo.notifications.size()) {
                    if (PlantInfo.notifications.get(j).getPlantID()==plant.getId()) {
                        int id = PlantInfo.notifications.get(j).getNotificationID();
                        createNotificationChannel();
                        generateNotification(picture,R.drawable.watercan,n, l,"Woda", " potrzebuje wody", id, water,true);
                        generateNotification(picture,R.drawable.fertilizer,n, l,"Nawóz", " potrzebuje nawozu", id+1, fertilizer,true);
                        generateNotification(picture,R.drawable.repot,n, l,"Przesadzanie", " potrzebuje przesadzenia", id+2, repot,true);
                        NotificationManagerCompat.from(this).cancel(id);
                        NotificationManagerCompat.from(this).cancel(id+1);
                        NotificationManagerCompat.from(this).cancel(id+2);
                        com.example.mygarden.Notification notif1 = new com.example.mygarden.Notification(plant.getId(),id);
                        PlantInfo.notifications.remove(notif1);
                        createNotificationChannel();
                        generateNotification(picture,R.drawable.watercan,n, l,"Woda", " potrzebuje wody", id2, water,false);
                        generateNotification(picture,R.drawable.fertilizer,n, l,"Nawóz", " potrzebuje nawozu", id2+1, fertilizer,false);
                        generateNotification(picture,R.drawable.repot,n, l,"Przesadzanie", " potrzebuje przesadzenia", id2+2, repot,false);
                    }
                    j++;
                }
            }
        }
        catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /////////////////////////////////////////image//////////////////////////////////////////////
    public void editImage(View view){
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
    /////////////////////////////////////////////////////////////////////////////////////////////////

    public void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            CharSequence name = "Channel";
            String description = "Channel for notification";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = null;
            channel = new NotificationChannel("notificationID", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void generateNotification(byte[] image,int notificationicon, String name, String localization, String activity, String activitydetails, int id, int time, boolean cancel){
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        GregorianCalendar calendar = (GregorianCalendar) GregorianCalendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 00);
        calendar.set(Calendar.SECOND,00);
        long timeInMillis = time;//86400000=1 dzień

        Intent intent = new Intent(this, ReminderBroadcast.class);
        intent.putExtra("keynotificationicon", notificationicon);
        intent.putExtra("keyname", name);
        intent.putExtra("keyactivity", activity);
        intent.putExtra("keyactivitydetails", activitydetails);
        intent.putExtra("keyid", id);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        //int RQS_1 = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, id, intent, 0);


        Intent intent2 = new Intent(this, TaskBroadcast.class);
        intent2.putExtra("keyname",name);
        intent2.putExtra("keyactivity", activity);
        intent2.putExtra("keylocalization", localization);
        intent2.putExtra("keyphoto", image);
        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(this, id, intent2, 0);

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), timeInMillis, pendingIntent); //AlarmManager.INTERVAL_DAY
        if(cancel==true) alarmManager.cancel(pendingIntent);
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), timeInMillis, pendingIntent2);
        if(cancel==true) alarmManager.cancel(pendingIntent2);
    }
}

