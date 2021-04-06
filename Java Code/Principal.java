package example.com.miscitasmedicas;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import com.alamkanak.weekview.WeekViewEvent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class Principal extends AppCompatActivity {

    static final int EVENTOS = 1;
    static final int CALENDARIOS = 2;
    static final int ELIMINAR_EVENTO = 3;
    static final int ELIMINAR_CALENDARIO = 4;
    static final int ELIMINAR_CALENDARIO_LISTA = 5;
    static final int COMPARTIR_CALENDARIO = 6;
    static final int ACTUALIZAR_CALENDARIO = 7;
    static final int NUEVO_CALENDARIO = 8;

    GoogleAccountCredential mCredential;
    private static String CuentaGoogle;    //Nombre de la cuenta de Google que se usara para la consulta
    ImageButton NuevoCalendario;
    FloatingActionButton LanzarCalendario;
    RecyclerView recycler;
    private static AdaptadorRecycler adaptador;

    private static List<Calendario> Calendarios;   //Aqui se almacenan todos los calendarios asociados a la cuenta
    private static List<Calendario> CalendariosSeleccionados;   //Aqui se almacenan los calendarios seleccionados para consultar sus eventos
    private static List<WeekViewEvent> EventosCalendario;   //Aqui se almacenan todos los eventos consultados para la interfaz calendario
    private static List<Event> EventosGoogle;       //Aqui se almacenan los eventos devueltos por Google


    private static final String PREF_ACCOUNT_NAME = "accountName";
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    private static final String[] SCOPES = { CalendarScopes.CALENDAR_READONLY };    //Premisos para consultar calendarios
    private static final String[] SCOPES2 = { CalendarScopes.CALENDAR }; //Permisos para consultar eventos


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.principal_layout);

        recycler = (RecyclerView) findViewById(R.id.recycler);
        recycler.setHasFixedSize(true);
        recycler.setLayoutManager(new LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false));

        //AdaptadorRecycler adaptador = new AdaptadorRecycler(Calendarios);
        //recycler.setAdapter(adaptador);

        NuevoCalendario = (ImageButton) findViewById(R.id.nuevo_calendario);
        NuevoCalendario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(getApplicationContext(),NuevoCalendario.class);
                startActivityForResult(i,6789);
            }
        });

        LanzarCalendario = (FloatingActionButton) findViewById(R.id.lanzar);
        LanzarCalendario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CalendariosSeleccionados = RevisaCalendarios(); //revisa si se ha seleccionado algun calendario

                if(CalendariosSeleccionados.size() == 0){
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
                getApplicationContext(), Arrays.asList(SCOPES2))
                .setBackOff(new ExponentialBackOff());

        getResultsFromApi();
    }

    static String getCuentaGoogle(){
        return CuentaGoogle;
    }

    //Extraer el calendario i de la lista de CalendariosSeleccionados para la clase NuevoEvento
    static Calendario getCalendarioSeleccionado(int i){
        return CalendariosSeleccionados.get(i);
    }

    static List<Calendario> getCalendariosSeleccionados(){
        return CalendariosSeleccionados;
    }

    //Devuelve una lista con los nombres de los calendarios seleccionados para la clase NuevoEvento
    public static List<String> getNombresCalendariosSeleccionados(){
        int i;
        List<String> nombres = new ArrayList<String>();

        for(i=0; i<CalendariosSeleccionados.size(); i++){
            nombres.add(CalendariosSeleccionados.get(i).getNombre());
        }

        return nombres;
    }

    static Calendario BuscaCalendarioPorNombre(String nombre){

        for(Calendario cal: CalendariosSeleccionados){
            if(cal.getNombre().equals(nombre)){
                return cal;
            }
        }

        return null;
    }

    static Event BuscaEventoGooglePorPosicion(int posicion){
        return EventosGoogle.get(posicion);
    }

    static List<Event> getEventosGoogle(){
        return EventosGoogle;
    }

    static List<WeekViewEvent> getEventosCalendario(){
        return EventosCalendario;
    }

    static int getEventosCalendarioSize(){return EventosCalendario.size();}

    static void AddEventosCalendario(WeekViewEvent nuevoEvento, Event nuevoEventoGoogle){
        EventosCalendario.add(nuevoEvento);
        EventosGoogle.add(nuevoEventoGoogle);
    }

    static void ActualizaCalendarios(List<Calendario> Datos){
        Calendarios = Datos;
    }

    static void ActualizaEventos(List<WeekViewEvent> Eventos){ EventosCalendario = Eventos; }

    static void ActualizaEventosGoogle(List<Event> Eventos){ EventosGoogle = Eventos; }

    static void ActualizaAdaptador(AdaptadorRecycler adapter){
        adaptador = adapter;
    }

    private List<Calendario> RevisaCalendarios(){
        List<Calendario> Aux = new ArrayList<>();

        for(Calendario Cal : Calendarios){

            if(Cal.getSeleccionado()){
                Aux.add(Cal);
            }
        }

        return Aux;
    }


    private void ConsultaEventosApi() {
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES2))    //Scopes2 (permisos) para consultar eventos.
                .setBackOff(new ExponentialBackOff());

        mCredential.setSelectedAccountName(CuentaGoogle); //Cuenta de Google

        new ConsultaApiGoogle(this, mCredential, EVENTOS).execute(EVENTOS);

    }

    private void EliminaCalendarioApi(String id_cal, String cal_name, int eliminar) {
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES2))    //Scopes2 (permisos) para eliminar calendario.
                .setBackOff(new ExponentialBackOff());

        mCredential.setSelectedAccountName(CuentaGoogle); //Cuenta de Google

        if(eliminar == ELIMINAR_CALENDARIO){
            new ConsultaApiGoogle(this, mCredential, id_cal, cal_name, "", "", ELIMINAR_CALENDARIO).execute(ELIMINAR_CALENDARIO);

        }else if(eliminar == ELIMINAR_CALENDARIO_LISTA){
            new ConsultaApiGoogle(this, mCredential, id_cal, cal_name, "", "", ELIMINAR_CALENDARIO).execute(ELIMINAR_CALENDARIO_LISTA);

        }

    }

    private void ActualizarCalendarioApi(String id_cal, String cal_name){
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES2))    //Scopes2 (permisos) para eliminar calendario.
                .setBackOff(new ExponentialBackOff());

        mCredential.setSelectedAccountName(CuentaGoogle); //Cuenta de Google

        new ConsultaApiGoogle(this, mCredential, id_cal, cal_name, "", "", ACTUALIZAR_CALENDARIO).execute(ACTUALIZAR_CALENDARIO);
    }

    private void NuevoCalendarioApi(String cal_name){
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES2))    //Scopes2 (permisos) para eliminar calendario.
                .setBackOff(new ExponentialBackOff());

        mCredential.setSelectedAccountName(CuentaGoogle); //Cuenta de Google

        new ConsultaApiGoogle(this, mCredential, "", cal_name, "", "", NUEVO_CALENDARIO).execute(NUEVO_CALENDARIO);
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
            Toast.makeText(this, "No network connection available.", Toast.LENGTH_SHORT).show();
        } else {
            new ConsultaApiGoogle(this, mCredential, CALENDARIOS).execute(CALENDARIOS);
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
                    Toast.makeText(this, "This app requires Google Play Services. Please install " +
                            "Google Play Services on your device and relaunch this app.", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(this, CuentaGoogle, Toast.LENGTH_LONG).show();
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

            case 12345:     //Eliminar Calendario
                if(resultCode == RESULT_OK){
                    int pos = data.getExtras().getInt("posicion");
                    Calendario cal = Calendarios.get(pos);

                    if(cal.getPermisos().equals("owner")){

                        if(!cal.getNombre().equals("Principal")){

                            Calendarios.remove(pos);
                            adaptador.notifyItemRemoved(pos);

                            EliminaCalendarioApi(cal.getId(), cal.getNombre(), ELIMINAR_CALENDARIO);
                            //Toast.makeText(this, "Calendario "+cal.getNombre()+" eliminado", Toast.LENGTH_SHORT).show();

                        }else{
                            Toast.makeText(this, "No puedes eliminar tu calendario Principal", Toast.LENGTH_SHORT).show();
                        }

                    }else{
                        Calendarios.remove(pos);
                        adaptador.notifyItemRemoved(pos);

                        EliminaCalendarioApi(cal.getId(), cal.getNombre(), ELIMINAR_CALENDARIO_LISTA);
                        //Toast.makeText(this, "No tienes permisos para eliminar este calendario", Toast.LENGTH_SHORT).show();
                    }

                }
                break;

            case 54321:
                if(resultCode == RESULT_OK){

                    String nuevo_nombre = data.getExtras().getString("nombre");
                    int pos = data.getExtras().getInt("posicion");

                    String cal_id = Calendarios.get(pos).getId();

                    //Actualizar el nombre del calendario en la lista de calendarios
                    Calendarios.get(pos).setNombre(nuevo_nombre);

                    //Reordenar alfabéticamente los calendarios
                    Collections.sort(Calendarios, new Comparator<Calendario>() {
                        public int compare(Calendario obj1, Calendario obj2) {
                            return obj1.getNombre().compareTo(obj2.getNombre());
                        }
                    });

                    //Comunicar al adaptador quese realizaron cambios
                    adaptador.notifyDataSetChanged();

                    ActualizarCalendarioApi(cal_id, nuevo_nombre);
                }
                break;

            case 6789:
                if(resultCode == RESULT_OK){
                    //Toast.makeText(this, "Calendario Creado", Toast.LENGTH_SHORT).show();

                    NuevoCalendarioApi(data.getExtras().getString("nombre"));
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
                Principal.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_calendarios, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_actualizar:
                getResultsFromApi();
                return true;
            case R.id.menu_cambiar_cuenta:
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
