package com.example.wayoflife.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.example.wayoflife.BuildConfig;
import com.example.wayoflife.dialog.InfoDialog;
import com.example.wayoflife.util.Constants;
import com.example.wayoflife.util.PermissionRationalActivity;
import com.example.wayoflife.R;
import com.example.wayoflife.workouts.trainings.RunningActivity;
import com.example.wayoflife.workouts.ui.WorkoutHistoryActivity;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    public static final String TAG = "HomeActivity";
    public static final String NOTIFICATION_CHANNEL = "notification_channel";

    private String nicknameUtente;

    /**
     * Oggetti per ActivityRecognition
     */
    private boolean runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;

    /** Variabili di controllo di stato */
    private boolean activityTrackingEnabled;
    private boolean walkingStatus;
    private boolean runningStatus;
    private boolean cyclingStatus;
    private boolean notificationStatus;

    private List<ActivityTransition> activityTransitionList;

    // Action fired when transitions are triggered.
    private final String TRANSITIONS_RECEIVER_ACTION =
            BuildConfig.APPLICATION_ID + "TRANSITIONS_RECEIVER_ACTION";


    private PendingIntent mActivityTransitionsPendingIntent;
    private HomeActivity.TransitionsReceiver mTransitionsReceiver;


    /**
     * Creo un canale per le notifiche
     */
    private void createNotificationChannel(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL, "Canale di Notifiche",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setDescription("Canale delle notifiche della Home");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    /**
     * Converto in stringa i parametri ricevuti dalla classe DetectedActivity
     * @param activity
     * @return
     */
    private static String toActivityString(int activity) {
        switch (activity) {
            case DetectedActivity.STILL:
                return "STILL";
            case DetectedActivity.WALKING:
                return "WALKING";
            case DetectedActivity.IN_VEHICLE:
                return "IN_VEHICLE";
            case DetectedActivity.ON_BICYCLE:
                return "ON_BICYCLE";
            case DetectedActivity.RUNNING:
                return "RUNNING";
            default:
                return "UNKNOWN";
        }
    }
    private static String toTransitionType(int transitionType) {
        switch (transitionType) {
            case ActivityTransition.ACTIVITY_TRANSITION_ENTER:
                return "ENTER";
            case ActivityTransition.ACTIVITY_TRANSITION_EXIT:
                return "EXIT";
            default:
                return "UNKNOWN";
        }
    }

    /** Stampa nella console */
    private void printToScreen(@NonNull String message) { Log.d(TAG, message); }

    /**
     * Nei dispositivi con Android 10 e precedente (29+), bisogna chiedere i permessi attraverso un
     * run-time permissions.
     */
    private boolean activityRecognitionPermissionApproved() {
        // Review permission check for 29+.
        if (runningQOrLater) {
            return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
            );
        } else {
            return true;
        }
    }

    /**
     * Lista di transazioni e attività che voglio che vengano riconosciute
     * @return
     */
    private ArrayList buildTransition(){
        ArrayList activityTransitionList = new ArrayList<>();

        if(walkingStatus) {
            activityTransitionList.add(new ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build());
            activityTransitionList.add(new ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build());
        }
        if(runningStatus) {
            activityTransitionList.add(new ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.RUNNING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build());
            activityTransitionList.add(new ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.RUNNING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build());
        }
        if(cyclingStatus) {
            activityTransitionList.add(new ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.ON_BICYCLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build());
            activityTransitionList.add(new ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.ON_BICYCLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build());
        }

        /** solo per i test
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());
         */

        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        activityTransitionList.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());


        return activityTransitionList;
    }

    /**
     * Controllo se é il primo accesso all'applicazione
     * Se non fosse lancio l'activity che permette di impostare i dati del proprio profilo
     */
    private void firstAccess(){
        SharedPreferences sharedPrefHome = getSharedPreferences(
                Constants.PROFILE_INFO_FILENAME,
                Context.MODE_PRIVATE);

        if(sharedPrefHome.getString(Constants.NOME, "").isEmpty()){
            Intent intent = new Intent(getApplicationContext(), FirstAccessActivity.class);
            intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /** Controllo se é il primo accesso -> a memoria vuota */
        firstAccess();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        /** Controllore della navigation bar */
        BottomNavigationView bottomNavigationView = findViewById(R.id.navigation);
        bottomNavigationView.setSelectedItemId(R.id.home);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch(item.getItemId()){
                    case R.id.home:

                    case R.id.allenamento:
                        startActivity(new Intent(getApplicationContext(), DashboardActivity.class));
                        finish();
                        overridePendingTransition(0, 0);
                        return false;

                    case R.id.profilo:
                        startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                        finish();
                        overridePendingTransition(0, 0);
                        return false;
                }
                return false;
            }
        });

        /** Se ho qualcosa da ripristinare lo rispristino da qui */
        if(savedInstanceState != null) {
            activityTrackingEnabled = savedInstanceState.getBoolean(Constants.ACTIVITY_TRACKING_STATUS);

            walkingStatus = savedInstanceState.getBoolean(Constants.WALKING_STATUS);
            runningStatus = savedInstanceState.getBoolean(Constants.RUNNING_STATUS);
            cyclingStatus = savedInstanceState.getBoolean(Constants.CYCLING_STATUS);

        }else { /** Atrimenti inizializzo gli oggetti da capo */
            activityTrackingEnabled = false;

            walkingStatus = false;
            runningStatus = false;
            cyclingStatus = false;
        }

        /** Inizializzo il canale per le notifiche */
        createNotificationChannel();

        notificationStatus = true;
        SharedPreferences sharedPref = getSharedPreferences(
                Constants.HOME_INFO_FILENAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(Constants.NOTIFICATION_STATUS, notificationStatus);
        editor.apply();


        /** Creo un PendingIntent che posso triggerare quando un ActivityTransition accorre */
        Intent intent = new Intent(TRANSITIONS_RECEIVER_ACTION);
        mActivityTransitionsPendingIntent =
                PendingIntent.getBroadcast(HomeActivity.this, 0, intent, 0);

        /** Inizializzo un BroadcastReceiver per ascoltare le transazioni dell'utente */
        mTransitionsReceiver = new HomeActivity.TransitionsReceiver();

        /** Recupero le informazioni che sono state memorizzate all'interno del file */
        resumeInformation();

        /** Gestisco il bottone che fornisce le informazioni all'utente */
        Chip chip = findViewById(R.id.chipInfo);
        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                infoDialog();
            }
        });

        printToScreen("App initialized.");
    }
    @Override
    protected void onStart() {
        firstAccess();

        super.onStart();

        // Registro il BroadcastReceiver per ascoltare le attività
        registerReceiver(mTransitionsReceiver, new IntentFilter(TRANSITIONS_RECEIVER_ACTION));
    }

    @Override
    protected void onResume() {
        SharedPreferences sharedPrefProfile = getSharedPreferences(
                Constants.PROFILE_INFO_FILENAME,
                Context.MODE_PRIVATE);

        int calorie = Integer.parseInt(
                sharedPrefProfile.getString(Constants.KCAL, "0"));

        TextView tvCalorie = findViewById(R.id.calorieTv);

        if(calorie != 0){
            tvCalorie.setText(calorie + " kcal");
        }

        super.onResume();
    }

    @Override
    protected void onPause() {
        // Rimuovere il commento per stoppare riconoscimento quando si esce dall'activity
//        if (activityTrackingEnabled) { disableActivityTransitions(); }

        super.onPause();
    }
    @Override
    protected void onStop() {
        // Rimuovere il commento per stoppare riconoscimento quando si esce dall'activity
//        unregisterReceiver(mTransitionsReceiver);

        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(Constants.ACTIVITY_TRACKING_STATUS, activityTrackingEnabled);

        outState.putBoolean(Constants.WALKING_STATUS, walkingStatus);
        outState.putBoolean(Constants.RUNNING_STATUS, runningStatus);
        outState.putBoolean(Constants.CYCLING_STATUS, cyclingStatus);
    }

    /**
     * Salvo le informazioni degli switch (che sono stati premuto oppure no)
     * In questo modo ad ogni avvio dell'applicazione avrei i dati salvati
     */
    private void saveInformation(){
        SharedPreferences sharedPref = getSharedPreferences(
                Constants.HOME_INFO_FILENAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        /** Salvo se é stato attivato oppure no l'ActivityRecognitionClient */
        editor.putBoolean(Constants.ACTIVITY_TRACKING_STATUS, activityTrackingEnabled);

        editor.putBoolean(Constants.WALKING_STATUS, walkingStatus);
        editor.putBoolean(Constants.RUNNING_STATUS, runningStatus);
        editor.putBoolean(Constants.CYCLING_STATUS, cyclingStatus);

        editor.putBoolean(Constants.NOTIFICATION_STATUS, notificationStatus);

        editor.apply();
    }
    private void resumeInformation(){
        /** Gestisco informazioni della Home */
        SharedPreferences sharedPrefHome = getSharedPreferences(
                Constants.HOME_INFO_FILENAME,
                Context.MODE_PRIVATE);

        activityTrackingEnabled = sharedPrefHome.getBoolean(
                Constants.ACTIVITY_TRACKING_STATUS, false);

        walkingStatus = sharedPrefHome.getBoolean(
                Constants.WALKING_STATUS, false);
        runningStatus = sharedPrefHome.getBoolean(
                Constants.RUNNING_STATUS, false);
        cyclingStatus = sharedPrefHome.getBoolean(
                Constants.CYCLING_STATUS, false);

        manageSwitch();

        if(activityTrackingEnabled) enableActivityTransitions();

        /** Gestisco informazioni del profilo utente */
        SharedPreferences sharedPrefProfile = getSharedPreferences(
                Constants.PROFILE_INFO_FILENAME,
                Context.MODE_PRIVATE);

        nicknameUtente = sharedPrefProfile.getString(
                Constants.NICKNAME, "Nickname");

        double calorie = Double.parseDouble(sharedPrefProfile.getString(
                Constants.KCAL, "0.0"));

        TextView tvCalorie = findViewById(R.id.calorieTv);

        if(calorie != 0.0){
            tvCalorie.setText(calorie + "kcal");
        }

        TextView tvNickname = findViewById(R.id.salutoUtente);
        tvNickname.setText("Ciao " + nicknameUtente + " !");
    }

    /**
     * Gestisco i 3 switch che controllano corsa, camminata e ciclismo
     */
    private void manageSwitch(){
        Switch sw = findViewById(R.id.switchAT);

        Switch swCamminata = findViewById(R.id.swCamminata);
        Switch swCorsa = findViewById(R.id.swCorsa);
        Switch swCiclismo = findViewById(R.id.swCiclismo);

        sw.setChecked(activityTrackingEnabled);

        swCamminata.setClickable(activityTrackingEnabled);
        swCorsa.setClickable(activityTrackingEnabled);
        swCiclismo.setClickable(activityTrackingEnabled);
        swCamminata.setEnabled(activityTrackingEnabled);
        swCorsa.setEnabled(activityTrackingEnabled);
        swCiclismo.setEnabled(activityTrackingEnabled);

        swCamminata.setChecked(walkingStatus);
        swCorsa.setChecked(runningStatus);
        swCiclismo.setChecked(cyclingStatus);
    }

    /**
     * Metodi che gestiscono se considerare o no ogni attività
     * Comandati dagli switch piccoli
     * @param v
     */
    public void manageWalking(View v){
        walkingStatus = !walkingStatus;

        activityTransitionList = buildTransition();

        saveInformation();
        resumeInformation();

        printToScreen("ManageWalking: \n\n" + activityTransitionList.toString() + "\n\n");
    }
    public void manageRunning(View v){
        runningStatus = !runningStatus;

        activityTransitionList = buildTransition();

        saveInformation();
        resumeInformation();

        printToScreen("ManageRunning: \n\n" + activityTransitionList.toString() + "\n\n");
    }
    public void manageCycling(View v){
        cyclingStatus = !cyclingStatus;

        activityTransitionList = buildTransition();

        saveInformation();
        resumeInformation();

        printToScreen("ManageCycling: \n\n" + activityTransitionList.toString() + "\n\n");
    }

    /** Metodo che gestisce il Dialog contenente le informazioni sull'ActivityTransition */
    public void infoDialog(){
        InfoDialog infoDialog = new InfoDialog();
        infoDialog.setType("home");
        infoDialog.show(getSupportFragmentManager(), "Dialog informativo");
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // Start activity recognition se si ha il permesso
        if (activityRecognitionPermissionApproved() && !activityTrackingEnabled) {
            enableActivityTransitions();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    /**
     * Registers callbacks for {@link ActivityTransition} events via a custom
     * {@link BroadcastReceiver}
     */
    private void enableActivityTransitions() {
        // Costruisco la lista delle transazioni da riconoscere
        activityTransitionList = buildTransition();

        //Creo richiesta passando come oggetto la lista precedentemente creata
        ActivityTransitionRequest request = new ActivityTransitionRequest(activityTransitionList);

        //Chiamo il requestActivityTransitionUpdate passando la RICHIESTA appena inizializzata e l'intent dell'onCreate
        Task<Void> task =
                ActivityRecognition.getClient(this)
                        .requestActivityTransitionUpdates(request, mActivityTransitionsPendingIntent);

        //In questo modo so quando l'utente attiva e disattiva il servizio
        task.addOnSuccessListener(
                new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        activityTrackingEnabled = true;
                        saveInformation();
                        manageSwitch();
                        printToScreen("Transitions Api was successfully registered.");
                    }
                });
        task.addOnFailureListener(
                new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        printToScreen("Transitions Api could NOT be registered: " + e);
                    }
                });
    }
    /**
     * Unregisters callbacks for {@link ActivityTransition} events via a custom
     * {@link BroadcastReceiver}
     */
    private void disableActivityTransitions() {
        // Mi fermo, non ascolto più se ci sono cambiamenti

        Snackbar.make(findViewById(android.R.id.content), "ActivityTracker disattivato",
                Snackbar.LENGTH_SHORT).show();

        //Rimuovere gli update quando non più necessari
        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(mActivityTransitionsPendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        activityTrackingEnabled = false;
                        saveInformation();
                        manageSwitch();
                        printToScreen("Transitions successfully unregistered.");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        printToScreen("Transitions could not be unregistered: " + e);
                    }
                });
    }

    /**
     * Ogni volta che viene attivato/disattivato lo switch dell'activityRecognition
     * viene invocato questo metodo
     * @param view
     */
    public void onClickEnableOrDisableActivityRecognition(View view) {

        if (activityRecognitionPermissionApproved()) {

            if (activityTrackingEnabled) {
                disableActivityTransitions();

            } else {
                enableActivityTransitions();
                Snackbar.make(findViewById(android.R.id.content), "ActivityTracker attivato",
                        Snackbar.LENGTH_SHORT).show();
            }

        } else {
            Intent startIntent = new Intent(this, PermissionRationalActivity.class);
            startActivityForResult(startIntent, 0);
        }
    }

    /**
     * Handles intents from from the Transitions API.
     */
    public class TransitionsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String info = "";
            String enterActivity = "";

            printToScreen("onReceive(): " + intent);

            if (!TextUtils.equals(TRANSITIONS_RECEIVER_ACTION, intent.getAction())) {
                printToScreen("Received an unsupported action in TransitionsReceiver: action = " +
                        intent.getAction());
                return;
            }

            if (ActivityTransitionResult.hasResult(intent)) {
                ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
                for (ActivityTransitionEvent event : result.getTransitionEvents()) {

                    info = "Transition: " +
                            toActivityString(event.getActivityType()) +
                            " (" + toTransitionType(event.getTransitionType()) + ")" + "   " +
                            new SimpleDateFormat("HH:mm:ss", Locale.ITALIAN).format(new Date());

//                    printToScreen(info);

                    /** Salvo solo la transazione di INGRESSO */
                    if(toTransitionType(event.getTransitionType())
                            .equalsIgnoreCase("ENTER")){
                        enterActivity = toActivityString(event.getActivityType());

                    }

                }
            }

            if(enterActivity!=null && enterActivity!="IN_VEHICLE") {
                /** Creo l'intent che mi permetterà di viaggiare da un'activity all'altra */

                Intent intentOut = new Intent(
                        HomeActivity.this,
                        RunningActivity.class);
                intentOut.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                switch(enterActivity){
                    case "RUNNING":
                        intentOut.putExtra(Constants.ATTIVITA_RILEVATA, "Corsa");
                        break;
                    case "WALKING":
                        intentOut.putExtra(Constants.ATTIVITA_RILEVATA, "Camminata");
                        break;
                    case "ON_BICYCLE":
                        intentOut.putExtra(Constants.ATTIVITA_RILEVATA, "Ciclismo");
                        break;
                    default:
                        intentOut.putExtra(Constants.ATTIVITA_RILEVATA, enterActivity);
                        break;
                }

                PendingIntent pendingIntentOut = PendingIntent.getActivity(HomeActivity.this,
                        0, intentOut, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationManagerCompat notificationManagerCompat =
                        NotificationManagerCompat.from(HomeActivity.this);

                /** Creo la notifica e gli assegno il PendingIntent appena creato */

                int icon = 0;
                String testo = "Vuoi avviare un allenamento specifico di tipo ";
                String attivitaITA = "";

                switch(enterActivity){
                    case "WALKING":
                        icon = R.drawable.ic_walking_color;
                        testo += "camminata";
                        attivitaITA = "CAMMINATA";
                        break;
                    case "RUNNING":
                        icon = R.drawable.ic_running_color;
                        testo += "corsa";
                        attivitaITA = "CORSA";
                        break;
                    case "ON_BICYCLE":
                        icon = R.drawable.ic_cycling_color;
                        testo += "ciclismo";
                        attivitaITA = "CICLISMO";
                        break;
                    default:
                        icon = R.drawable.ic_notifications_black_24dp;
                        testo = "STILL o IN_VEHICLE";
                        attivitaITA = "STILL o IN_VEHICLE";
                        break;
                }

                testo += "?\nSe SI, allora premi sulla notifica\n\nAltrimenti ignora il messaggio!";

                Notification notification = new NotificationCompat.Builder(
                        HomeActivity.this,
                        NOTIFICATION_CHANNEL)
                        .setSmallIcon(icon)
                        .setContentTitle("Nuova attivita riconosciuta! - " + attivitaITA)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(testo)
                        )
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_EVENT)
                        .setContentIntent(pendingIntentOut)
                        .addAction(icon,
                                "Avvia attività (" + attivitaITA + ")",
                                pendingIntentOut)
                        .setAutoCancel(true)
                        .build();

                /** Recupero informazioni relative alla notifica */
                SharedPreferences sharedPrefHome = getSharedPreferences(
                        Constants.HOME_INFO_FILENAME,
                        Context.MODE_PRIVATE);
                notificationStatus = sharedPrefHome.getBoolean(
                        Constants.NOTIFICATION_STATUS, false);

                if(notificationStatus) notificationManagerCompat.notify(1, notification);
            }
        }
    }

    public void goToHistoryActivity(View v){
        Intent intent = new Intent(getApplicationContext(), WorkoutHistoryActivity.class);
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
    public void goToTipsActivity(View v){
        Intent intent = new Intent(getApplicationContext(), TipsActivity.class);
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
}