package example.com.miscitasmedicas;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alamkanak.weekview.WeekViewEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks  {
    GoogleAccountCredential mCredential;
    private TextView mOutputText;
    ProgressDialog mProgress;
    ListView lista;
    List<String> calendarNames; //Aqui se almacenan los nombres de los calendarios para mostrarlos en la lista de la UI
    List<CalendarListEntry> Calendarios;    //Aqui se almacenan todos los calendarios asociados a la cuenta
    Button lanzador;
    FloatingActionButton IrCalendario;

    private static String CuentaGoogle;
    private static List<CalendarListEntry> CalendariosSeleccionados;   //Aqui se alacenan los calendarios sleccionados
    private static List<WeekViewEvent> EventosCalendario = new ArrayList<WeekViewEvent>();  //Lista donde se almacenaran los eventos


    static final int EVENTOS = 1;
    static final int CALENDARIOS = 2;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "Call Google Calendar API";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = { CalendarScopes.CALENDAR_READONLY };    //Premisos para consultar calendarios
    private static final String[] SCOPES2 = { CalendarScopes.CALENDAR }; //Permisos para consultar eventos

    /**
     * Create the main activity.
     * @param savedInstanceState previously saved instance data.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Google Calendar API ...");

        mOutputText = (TextView)findViewById(R.id.mensajes);
        mOutputText.setText("");

        lista = (ListView) findViewById(android.R.id.list);
        lista.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        IrCalendario = (FloatingActionButton) findViewById(R.id.lanzarcalendario);
        IrCalendario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(lista.getCheckedItemCount() == 0){   //revisa si se ha seleccionado algun calendario
                    Toast.makeText(getApplicationContext(),"No se ha seleccionado ningún calendario.",Toast.LENGTH_LONG).show();
                }else{

                    if(isDeviceOnline()){
                        ConsultaEventosApi();
                    }else{
                        Toast.makeText(getApplicationContext(),"No hay conexión a Internet.",Toast.LENGTH_LONG).show();
                    }

                }
            }
        });

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        getResultsFromApi();

    }

    public static int AddEventosCalndario(WeekViewEvent nuevo){
        EventosCalendario.add(nuevo);
        return 0;
    }

    public static String getCuentaGoogle(){ return CuentaGoogle; }

    public static List<WeekViewEvent> getEventosCalendario(){
        return EventosCalendario;
    }

    //Extraer el calendario i de la lista de CalendariosSeleccionados
    public static CalendarListEntry getCalendar(int i){
        return CalendariosSeleccionados.get(i);
    }

    //Devuelve una lista con los nombres de los calendarios seleccionados
    public static List<String> getSelectedCaalendars(){
        int contador,i;
        List<String> nombres = new ArrayList<String>();

        contador = CalendariosSeleccionados.size();

        for(i=0; i<contador; i++){
            nombres.add(CalendariosSeleccionados.get(i).getSummary());
        }

        return nombres;
    }

    public void LanzarCalendario(){
        //ConsultaEventosApi();

        Intent i = new Intent(this,InterfazCalendario.class);

        //Iniciar la nueva actividad
        startActivity(i);
    }

    private void ConsultaEventosApi() {
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES2))    //Scopes (permisos) para consultar eventos.
                .setBackOff(new ExponentialBackOff());

        mCredential.setSelectedAccountName(CuentaGoogle); //Cuenta de Google

        new MakeRequestTask(mCredential, EVENTOS).execute(EVENTOS);

    }

    private void ActualizarVista(){
        lista.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice, calendarNames));
    }


    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void getResultsFromApi() {
        if (! isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            mOutputText.setText("No network connection available.");
        } else {
           new MakeRequestTask(mCredential, CALENDARIOS).execute(CALENDARIOS);
        }
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            CuentaGoogle = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (CuentaGoogle != null) {
                mCredential.setSelectedAccountName(CuentaGoogle);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    CuentaGoogle =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (CuentaGoogle != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, CuentaGoogle);
                        editor.apply();
                        mCredential.setSelectedAccountName(CuentaGoogle);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
        }
    }

    /**
     * Respond to requests for permissions at runtime for API 23 and above.
     * @param requestCode The request code passed in
     *     requestPermissions(android.app.Activity, String, int, String[])
     * @param permissions The requested permissions. Never null.
     * @param grantResults The grant results for the corresponding permissions
     *     which is either PERMISSION_GRANTED or PERMISSION_DENIED. Never null.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    /**
     * Callback for when a permission is granted using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Callback for when a permission is denied using the EasyPermissions
     * library.
     * @param requestCode The request code associated with the requested
     *         permission
     * @param list The requested permission list. Never null.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }

    /**
     * Checks whether the device currently has a network connection.
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     * @return true if Google Play Services is available and up to
     *     date on this device; false otherwise.
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    /**
     * Attempt to resolve a missing, out-of-date, invalid or disabled Google
     * Play Services installation via a user dialog, if possible.
     */
    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    /**
     * An asynchronous task that handles the Google Calendar API call.
     * Placing the API calls in their own task ensures the UI stays responsive.
     */
    private class MakeRequestTask extends AsyncTask<Integer, Void, Integer> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;
        DateTime FechaHora;
        String anio, mes, dia, horas, minutos, segundos;
        Calendar Inicio;
        Calendar Fin;
        Calendar FechaConsulta;
        WeekViewEvent evento;
        int contador, i, a;

        MakeRequestTask(GoogleAccountCredential credential, int consulta) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Mis Citas Medicas")
                    .build();

            if(EVENTOS == consulta){    //Inicializar variables relacionadas a la consulta de eventos.
                CalendariosSeleccionados = new ArrayList<CalendarListEntry>();
                EventosCalendario.clear();

                contador = lista.getAdapter().getCount();

                for(i=0; i<contador; i++){  //revisa todos los elementos de la lista (la lista de la UI)

                    if(lista.isItemChecked(i)){ //si el elemento actual esta seleccionado se guarda
                        CalendariosSeleccionados.add(Calendarios.get(i));
                    }
                }

            }
        }

        /**
         * Background task to call Google Calendar API.
         * @param n consult events or calendars.
         * @return integer than represents events or calendars.
         */
        @Override
        protected Integer doInBackground(Integer... n) {
            try {
                if(CALENDARIOS == n[0]){
                    getCalendarsFromApi();

                }else if(EVENTOS == n[0]){
                    contador = CalendariosSeleccionados.size();

                    for(i=0; i<contador; i++){
                        getEventsFromApi(CalendariosSeleccionados.get(i));
                    }
                }

                return n[0];
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Fetch a list of the calendars from the Google account.
         * @throws IOException
         */
        private void getCalendarsFromApi() throws IOException {

            calendarNames = new ArrayList<String>();

            // Iterate through entries in calendar list
            String pageToken = null;
            do {
                CalendarList calendarList = mService.calendarList().list().setPageToken(pageToken).execute();
                Calendarios = calendarList.getItems();

                for (CalendarListEntry calendarListEntry : Calendarios) {
                    calendarNames.add(
                            String.format("%s", calendarListEntry.getSummary()));
                }
                pageToken = calendarList.getNextPageToken();
            } while (pageToken != null);

        }

        /**
         * Fetch a list of the events from calendar.
         * @throws IOException
         * @param calendar is the Id of the calendar for the consult.
         */
        private void getEventsFromApi(CalendarListEntry calendar) throws IOException {
            // List the next 10 events from the primary calendar.
            FechaConsulta = Calendar.getInstance();
            FechaConsulta.add(Calendar.MONTH, -1);
            DateTime startDateTime = new DateTime(FechaConsulta.getTime(), FechaConsulta.getTimeZone());
            Events events = mService.events().list(calendar.getId())   //Calendario de donde se sacaran los eventos
                    .setMaxResults(10)
                    .setTimeMin(startDateTime)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            List<Event> items = events.getItems();

            for (Event event : items) {
                //Longitud de Fecha: 10
                //Longitud de FechaHora: 29

                if (event.getStart().getDateTime() == null) {
                    // All-day events don't have start times, so just use
                    // the start date.
                    FechaHora = event.getStart().getDate();

                    anio = FechaHora.toString().substring(0,4);
                    mes = FechaHora.toString().substring(5,7);
                    dia = FechaHora.toString().substring(8,10);
                    horas = "00";
                    minutos = "01";
                    segundos = "00";

                }else{
                    FechaHora = event.getStart().getDateTime();

                    anio = FechaHora.toString().substring(0,4);
                    mes = FechaHora.toString().substring(5,7);
                    dia = FechaHora.toString().substring(8,10);
                    horas = FechaHora.toString().substring(11,13);
                    minutos = FechaHora.toString().substring(14,16);
                    segundos = FechaHora.toString().substring(17,19);
                }

                //Los meses se inician desde 0
                Inicio = new GregorianCalendar(Integer.parseInt(anio), Integer.parseInt(mes)-1, Integer.parseInt(dia), Integer.parseInt(horas), Integer.parseInt(minutos), Integer.parseInt(segundos));
                //Inicio.setTimeInMillis(FechaHora.getValue());

                if (event.getEnd().getDateTime() == null) {
                    // All-day events don't have start times, so just use
                    // the start date.

                    //Las variables FechaHora, anio, mes y dia se quedan con el mismo valor
                    horas = "23";
                    minutos = "59";
                    segundos = "00";

                }else{
                    FechaHora = event.getEnd().getDateTime();

                    anio = FechaHora.toString().substring(0,4);
                    mes = FechaHora.toString().substring(5,7);
                    dia = FechaHora.toString().substring(8,10);
                    horas = FechaHora.toString().substring(11,13);
                    minutos = FechaHora.toString().substring(14,16);
                    segundos = FechaHora.toString().substring(17,19);
                }

                Fin = new GregorianCalendar(Integer.parseInt(anio), Integer.parseInt(mes)-1, Integer.parseInt(dia), Integer.parseInt(horas), Integer.parseInt(minutos), Integer.parseInt(segundos));
                //Fin.setTimeInMillis(FechaHora.getValue());
                //Fin = (Calendar) Inicio.clone();
                //Fin.add(Calendar.HOUR, 1);

                if(calendar.getAccessRole().equals("owner") || event.getCreator().getEmail().equals(CuentaGoogle)){    //comprobar si el usuario es el dueño del calendario o es el creador del evento
                    evento = new WeekViewEvent(1, event.getSummary(), Inicio, Fin);
                }else{
                    evento = new WeekViewEvent(1, "Ocupado", Inicio, Fin);
                }

                evento.setColor(Color.parseColor(calendar.getBackgroundColor()));
                //evento.setColor(Color.MAGENTA);
                //color = event.getColorId();
                EventosCalendario.add(evento);
                //contador++;   //contador de eventos
            }
        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(Integer n) {
            mProgress.hide();

            if(CALENDARIOS == n){
                ActualizarVista();

            }else if(EVENTOS == n){
                LanzarCalendario();
                Toast.makeText(getApplicationContext(),"Eventos Sincronizados ",Toast.LENGTH_SHORT).show();
                //Toast.makeText(MainActivity.this, "Color: "+color, Toast.LENGTH_LONG).show();
            }

        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    mOutputText.setText("The following error occurred:\n"
                            + mLastError.getMessage());
                }
            } else {
                mOutputText.setText("Request cancelled.");
            }
        }
    }

}
