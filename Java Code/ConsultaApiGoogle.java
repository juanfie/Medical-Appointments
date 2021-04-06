package example.com.miscitasmedicas;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.alamkanak.weekview.WeekViewEvent;
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
import com.google.api.services.calendar.model.AclRule;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * An asynchronous task that handles the Google Calendar API call.
 * Placing the API calls in their own task ensures the UI stays responsive.
 */
class ConsultaApiGoogle extends AsyncTask<Integer, Void, Integer> {

    private com.google.api.services.calendar.Calendar mService = null;
    private Exception mLastError = null;
    private ProgressDialog ProgresoConsulta;
    private Context context;
    private List<CalendarListEntry> CalendariosGoogle;
    private List<Calendario> Calendarios;
    private RecyclerView recycler;
    private String id_evento, id_calendario, nombre_evento, nombre_calendario, correo_compartir, permisos_compartir;
    AclRule createdRule;

    private List<Calendario> SelectedCalendars;
    private List<Event> listaCompleta = new ArrayList<Event>();      //Lista donde se almacenaran los eventos devueltos por Google
    private List<WeekViewEvent> Eventos = new ArrayList<WeekViewEvent>();  //Lista donde se almacenaran los eventos para la intefaz calendario
    private long contador;
    GoogleAccountCredential credenciales;
    private static final String[] SCOPES2 = { CalendarScopes.CALENDAR }; //Permisos para consultar eventos

    //Constructor para consultar calendarios o eventos
    ConsultaApiGoogle(Context contexto, GoogleAccountCredential credential, int consulta) {
        context = contexto;
        credenciales = credential;
        ProgresoConsulta = new ProgressDialog(context);
        ProgresoConsulta.setMessage("Conectando con Google Calendar API ...");
        Calendarios = new ArrayList<>();

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, credenciales)
                .setApplicationName("Mis Citas Medicas")
                .build();

        if(Principal.EVENTOS == consulta){    //Inicializar variables relacionadas a la consulta de eventos.
            SelectedCalendars = Principal.getCalendariosSeleccionados();
            contador = 0;   //para contar todos los eventos consultados
        }
    }

    //Constructor para Eliminar Eventos, Eliminar Calendarios, Crear Acl's, Actualizar Calendario o Crear Calendario
    ConsultaApiGoogle(Context contexto, GoogleAccountCredential credential, String id_cal, String cal_name, String id_event, String event_name, int tipo){
        context = contexto;
        id_calendario = id_cal;
        nombre_calendario = cal_name;
        credenciales = credential;

        if(tipo == Principal.COMPARTIR_CALENDARIO){
            correo_compartir = id_event;
            permisos_compartir = event_name;
        }else{
            id_evento = id_event;
            nombre_evento = event_name;
        }

        ProgresoConsulta = new ProgressDialog(context);
        ProgresoConsulta.setMessage("Conectando con Google Calendar API ...");

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.calendar.Calendar.Builder(
                transport, jsonFactory, credenciales)
                .setApplicationName("Mis Citas Medicas")
                .build();
    }

    /**
     * Background task to call Google Calendar API.
     * @param n consult events or calendars.
     * @return integer than represents events or calendars.
     */
    @Override
    protected Integer doInBackground(Integer... n) {
        try {
            if(Principal.CALENDARIOS == n[0]){
                getCalendarsFromApi();

            }else if(Principal.EVENTOS == n[0]){
                for (int i=0;i<SelectedCalendars.size();i++){
                    getEventsFromApi(SelectedCalendars.get(i));
                }
            }else if(Principal.ELIMINAR_EVENTO == n[0]){
                deleteEventFromApi();
            }else if(Principal.ELIMINAR_CALENDARIO == n[0]){
                deleteCalendarFromApi();
            }else if(Principal.ELIMINAR_CALENDARIO_LISTA == n[0]){
                deleteCalendarFromListApi();
            }else if(Principal.COMPARTIR_CALENDARIO == n[0]){
                shareCalendarFromApi();
            }else if(Principal.ACTUALIZAR_CALENDARIO == n[0]){
                updateCalendarFromApi();
            }else if(Principal.NUEVO_CALENDARIO == n[0]){
                newCalendarFromApi();
            }

            return n[0];
        } catch (Exception e) {
            mLastError = e;
            Log.e("My App", "Erro: ",e);
            cancel(true);
            return null;
        }
    }

    /**
     * Fetch a list of the calendars from the Google account.
     * @throws IOException
     */
    private void getCalendarsFromApi() throws IOException {

        // Iterate through entries in calendar list
        String pageToken = null;
        do {
            CalendarList calendarList = mService.calendarList().list().setPageToken(pageToken).execute();
            CalendariosGoogle = calendarList.getItems();

            for (CalendarListEntry calendarListEntry : CalendariosGoogle) {

                if(calendarListEntry.isPrimary()){
                    Calendarios.add(new Calendario(calendarListEntry.getId(),"Principal",calendarListEntry.getBackgroundColor(),calendarListEntry.getAccessRole()));
                }else{
                    Calendarios.add(new Calendario(calendarListEntry.getId(),calendarListEntry.getSummary(),calendarListEntry.getBackgroundColor(),calendarListEntry.getAccessRole()));
                }
                //calendarNames.add(String.format("%s", calendarListEntry.getSummary()));
            }
            pageToken = calendarList.getNextPageToken();
        } while (pageToken != null);

    }

    /**
     * Fetch a list of the events from calendar.
     * @throws IOException
     * @param calendar is the Id of the calendar for the consult.
     */
    private void getEventsFromApi(Calendario calendar) throws IOException {
        DateTime FechaHora;
        String anio, mes, dia, horas, minutos, segundos;
        Calendar Inicio;
        Calendar Fin;
        WeekViewEvent evento;

        Calendar FechaConsulta = Calendar.getInstance();
        FechaConsulta.add(Calendar.MONTH, -3);
        DateTime startDateTime = new DateTime(FechaConsulta.getTime(), FechaConsulta.getTimeZone());
        Events events = mService.events().list(calendar.getId())   //Calendario de donde se sacaran los eventos
                //.setMaxResults(10)
                .setTimeMin(startDateTime)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();
        List<Event> items = events.getItems();

        for (Event event : items) {
            listaCompleta.add(event);

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
                // All-day events don't have end times, so just use
                // the end date.

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

            if(calendar.getPermisos().equals("owner") || event.getCreator().getEmail().equals(Principal.getCuentaGoogle()) || calendar.getNombre().equals("Festivos en México")){    //comprobar si el usuario es el dueño del calendario o es el creador del evento
                evento = new WeekViewEvent(contador, event.getSummary()+" - "+calendar.getNombre(), event.getLocation(), Inicio, Fin);
            }else{
                evento = new WeekViewEvent(contador, "Ocupado"+" - "+calendar.getNombre(),Inicio, Fin);
            }

            evento.setColor(Color.parseColor(calendar.getColor()));
            //evento.setColor(Color.MAGENTA);
            //color = event.getColorId();
            Eventos.add(evento);
            contador++;   //contador de eventos
        }
    }

    /**
     * Delete an event of a calendar from the Google account.
     * @throws IOException
     */
    private void deleteEventFromApi() throws IOException {
        mService.events().delete(id_calendario, id_evento).execute();
    }

    /**
     * Delete a calendar from the Google account.
     * @throws IOException
     */
    private void deleteCalendarFromApi() throws IOException {
        mService.calendars().delete(id_calendario).execute();
    }

    /**
     * Delete a calendar list entry from the Google account.
     * @throws IOException
     */
    private void deleteCalendarFromListApi() throws IOException{
        mService.calendarList().delete(id_calendario).execute();
    }

    /**
     * Share a calendar from the Google account.
     * @throws IOException
     */
    private void shareCalendarFromApi() throws IOException{
        // Create access rule with associated scope
        AclRule rule = new AclRule();
        AclRule.Scope scope = new AclRule.Scope();
        scope.setType("user").setValue(correo_compartir);
        rule.setScope(scope).setRole(permisos_compartir);

        // Insert new access rule
        createdRule = mService.acl().insert(id_calendario, rule).execute();
        //System.out.println(createdRule.getId());
    }

    /**
     * Update a calendar from the Google account.
     * @throws IOException
     */
    private void updateCalendarFromApi() throws IOException{
        // Retrieve a calendar
        com.google.api.services.calendar.model.Calendar calendar =
                mService.calendars().get(id_calendario).execute();

        // Make a change
        calendar.setSummary(nombre_calendario);

        // Update the altered calendar
        //com.google.api.services.calendar.model.Calendar updatedCalendar =
        mService.calendars().update(calendar.getId(), calendar).execute();

        //System.out.println(updatedCalendar.getEtag());
    }

    /**
     * Create a new calendar from the Google account.
     * @throws IOException
     */
    private void newCalendarFromApi() throws IOException{
        Calendar cal = Calendar.getInstance();

        // Create a new calendar
        com.google.api.services.calendar.model.Calendar calendar = new com.google.api.services.calendar.model.Calendar();
        calendar.setSummary(nombre_calendario);
        calendar.setTimeZone(cal.getTimeZone().getID());

        // Insert the new calendar
        //Calendar createdCalendar =
        mService.calendars().insert(calendar).execute();

        //System.out.println(createdCalendar.getId());
    }

    @Override
    protected void onPreExecute() {
        ProgresoConsulta.show();
    }

    @Override
    protected void onPostExecute(Integer n) {
        ProgresoConsulta.hide();
        //((NuevoEvento) context).TittleText.getText();

        if(Principal.CALENDARIOS == n){
            //Ordenar Alfabéticamente los calendarios
            Collections.sort(Calendarios, new Comparator<Calendario>() {
                public int compare(Calendario obj1, Calendario obj2) {
                    return obj1.getNombre().compareTo(obj2.getNombre());
                }
            });

            Principal.ActualizaCalendarios(Calendarios);
            //((AppCompatActivity) context).recreate();

            recycler = (RecyclerView) ((AppCompatActivity) context).findViewById(R.id.recycler);
            AdaptadorRecycler adaptador = new AdaptadorRecycler(context, Calendarios);
            Principal.ActualizaAdaptador(adaptador);
            recycler.setAdapter(adaptador);

        }else if(Principal.EVENTOS == n){
            Principal.ActualizaEventosGoogle(listaCompleta);
            Principal.ActualizaEventos(Eventos);
            //Toast.makeText(context,"Eventos Sincronizados ",Toast.LENGTH_SHORT).show();

            //Iniciar la nueva actividad
            Intent i = new Intent(context,InterfazCalendario.class);
            context.startActivity(i);

        }else if(Principal.ELIMINAR_EVENTO == n){
            credenciales = GoogleAccountCredential.usingOAuth2(
                    context, Arrays.asList(SCOPES2))    //permisos para consultar eventos.
                    .setBackOff(new ExponentialBackOff());

            credenciales.setSelectedAccountName(Principal.getCuentaGoogle()); //Seleccionar cuenta de Google

            new ConsultaApiGoogle(context, credenciales, Principal.EVENTOS).execute(Principal.EVENTOS);

            ((AppCompatActivity) context).finish();
            Toast.makeText(context, "Evento:"+nombre_evento+" - eliminado", Toast.LENGTH_LONG).show();

        }else if(Principal.ELIMINAR_CALENDARIO == n || Principal.ELIMINAR_CALENDARIO_LISTA == n){
            Toast.makeText(context, "Calendario "+nombre_calendario+" - eliminado", Toast.LENGTH_LONG).show();

        }else if(Principal.COMPARTIR_CALENDARIO == n){
            Toast.makeText(context, createdRule.getId(), Toast.LENGTH_SHORT).show();
            ((AppCompatActivity) context).finish();

        }else if(Principal.ACTUALIZAR_CALENDARIO == n){
            Toast.makeText(context, "Calendario "+nombre_calendario+" actualizado", Toast.LENGTH_SHORT).show();

        }else if(Principal.NUEVO_CALENDARIO == n){
            credenciales = GoogleAccountCredential.usingOAuth2(
                    context, Arrays.asList(SCOPES2))    //permisos
                    .setBackOff(new ExponentialBackOff());

            credenciales.setSelectedAccountName(Principal.getCuentaGoogle()); //Seleccionar cuenta de Google

            new ConsultaApiGoogle(context, credenciales, Principal.CALENDARIOS).execute(Principal.CALENDARIOS);

            Toast.makeText(context, "Calendario "+nombre_calendario+" creado", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    protected void onCancelled() {
        ProgresoConsulta.hide();
        if (mLastError != null) {
            if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                showGooglePlayServicesAvailabilityErrorDialog(
                        ((GooglePlayServicesAvailabilityIOException) mLastError)
                                .getConnectionStatusCode());
            } else if (mLastError instanceof UserRecoverableAuthIOException) {
                //Abre una ventana nueva para dar permisos para el uso del calendario de Google
                ((AppCompatActivity) context).startActivityForResult(((UserRecoverableAuthIOException) mLastError).getIntent(), Principal.REQUEST_AUTHORIZATION);
            } else {
                Toast.makeText(context, "The following error occurred:\n"
                        + mLastError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "Request cancelled.", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     * @param connectionStatusCode code describing the presence (or lack of)
     *     Google Play Services on this device.
     */
    private void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Toast.makeText(context, apiAvailability.getErrorString(connectionStatusCode), Toast.LENGTH_SHORT).show();
    }

}
