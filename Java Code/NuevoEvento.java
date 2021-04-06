package example.com.miscitasmedicas;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
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
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

public class NuevoEvento extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {
    GoogleAccountCredential mCredential;
    public TextView mOutputText, StartTimeText, StartDateText, EndTimeText, EndDateText;
    public EditText TittleText;
    public Button mCallApiButton;
    public Spinner spinner;
    ProgressDialog mProgress;
    Calendar Start, End;        //Start = fecha de inicio del evento,  End = fecha de termino del evento
    ColorStateList ColorOriginal;
    boolean flag;   //true = Inicio del evento, false = Fin del evento.
    SimpleDateFormat TimeFormat = new SimpleDateFormat("hh:mm a");
    SimpleDateFormat DateFormat = new SimpleDateFormat("EEEE, d 'de' MMMM 'del' yyyy"); //4 letras o mas para usar el formato completo
    Calendario CalendarioPadre;  //Calendario donde se insertará el nuevo evento


    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;

    private static final String[] SCOPES = { CalendarScopes.CALENDAR };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.interfaz_nuevo_evento);

        Bundle extras = getIntent().getExtras();    //obtener informacion enviada desde la activity anterior

        //Inicializa calendarios
        Start = Calendar.getInstance();
        Start.setTimeInMillis(extras.getLong("fecha")); //Se inicializa con la fecha en milis enviada del activity anterior
        End = (Calendar) Start.clone();
        End.add(Calendar.HOUR, 1);

        TittleText = (EditText) findViewById(R.id.textotitulo);

        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter adaptador = new ArrayAdapter(this,android.R.layout.simple_dropdown_item_1line,Principal.getNombresCalendariosSeleccionados());
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adaptador);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                CalendarioPadre = Principal.getCalendarioSeleccionado(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        StartTimeText = (TextView) findViewById(R.id.horaInicio);
        StartTimeText.setText(TimeFormat.format(Start.getTime()));
        ColorOriginal = StartTimeText.getTextColors();  //Extraer colores originales del TextView
        StartTimeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = true;
                LanzarTimePickerDialog(Start.get(Calendar.HOUR_OF_DAY), Start.get(Calendar.MINUTE));
            }
        });

        StartDateText = (TextView) findViewById(R.id.fechaInicio);
        StartDateText.setText(DateFormat.format(Start.getTime()));
        StartDateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = true;
                LanzarDatePickerDialog(Start.get(Calendar.YEAR), Start.get(Calendar.MONTH), Start.get(Calendar.DAY_OF_MONTH));
            }
        });

        EndTimeText = (TextView) findViewById(R.id.horaFin);
        EndTimeText.setText(TimeFormat.format(End.getTime()));
        EndTimeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = false;
                LanzarTimePickerDialog(End.get(Calendar.HOUR_OF_DAY), End.get(Calendar.MINUTE));
            }
        });

        EndDateText = (TextView) findViewById(R.id.fechaFin);
        EndDateText.setText(DateFormat.format(End.getTime()));
        EndDateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = false;
                LanzarDatePickerDialog(End.get(Calendar.YEAR), End.get(Calendar.MONTH), End.get(Calendar.DAY_OF_MONTH));
            }
        });

        mCallApiButton = (Button) findViewById(R.id.Boton);
        mCallApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton.setEnabled(false);
                mOutputText.setText("");

                if(End.after(Start)){
                    getResultsFromApi();
                }else{
                    Toast.makeText(getApplicationContext(), "Error en las fechas del evento.", Toast.LENGTH_SHORT).show();
                }

                mCallApiButton.setEnabled(true);
            }
        });

        mOutputText = (TextView) findViewById(R.id.Texto);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Google Calendar API ...");

        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

    }

    //Escuchador del DatePickerDialog
    public void onDateSet(DatePicker view, int year, int month, int day){
        // Do something with the date chosen by the user
        if(flag){
            Start.set(Calendar.YEAR, year);
            Start.set(Calendar.MONTH, month); //los meses se empiezan a contar dede 0
            Start.set(Calendar.DAY_OF_MONTH, day);
            StartDateText.setText(DateFormat.format(Start.getTime()));

            //Si se cambia la fecha de inicio, automaticamente se asigna la misma fecha como fecha final del evento.
            End.set(Calendar.YEAR, year);
            End.set(Calendar.MONTH, month); //los meses se empiezan a contar dede 0
            End.set(Calendar.DAY_OF_MONTH, day);
            EndDateText.setText(DateFormat.format(End.getTime()));
        }else{
            End.set(Calendar.YEAR, year);
            End.set(Calendar.MONTH, month); //los meses se empiezan a contar dede 0
            End.set(Calendar.DAY_OF_MONTH, day);
            EndDateText.setText(DateFormat.format(End.getTime()));
        }

        if(End.after(Start)){
            StartDateText.setTextColor(ColorOriginal);
            StartTimeText.setTextColor(ColorOriginal);
        }else{
            StartDateText.setTextColor(Color.RED);
            StartTimeText.setTextColor(Color.RED);
        }

    }

    //Escuchador del TimePickerDialog
    public void onTimeSet(TimePicker view, int hourOfDay, int minute){
        // Do something with the time chosen by the user
        if(flag){
            Start.set(Calendar.HOUR_OF_DAY, hourOfDay);
            Start.set(Calendar.MINUTE, minute);
            StartTimeText.setText(TimeFormat.format(Start.getTime()));

            //Si se cambia la hora de inicio, automaticamente se asigna una hora después como hora final del evento.
            End.set(Calendar.DAY_OF_MONTH,Start.get(Calendar.DAY_OF_MONTH));    //Actualizar el dia de la variable End para corregir error de las 12:00am
            End.set(Calendar.HOUR_OF_DAY, hourOfDay+1);     //Si (hourOfDay+1 = 12:00am) automaticamente cambia al siguiente dia la fecha de la variable End
            End.set(Calendar.MINUTE, minute);
            EndTimeText.setText(TimeFormat.format(End.getTime()));
            EndDateText.setText(DateFormat.format(End.getTime()));
        }else{
            End.set(Calendar.HOUR_OF_DAY, hourOfDay);
            End.set(Calendar.MINUTE, minute);
            EndTimeText.setText(TimeFormat.format(End.getTime()));
        }

        if(End.after(Start)){
            StartDateText.setTextColor(ColorOriginal);
            StartTimeText.setTextColor(ColorOriginal);
        }else{
            StartDateText.setTextColor(Color.RED);
            StartTimeText.setTextColor(Color.RED);
        }

    }

    public void LanzarDatePickerDialog(int anio, int mes, int dia){
        SelectorFecha newFragment = new SelectorFecha();
        newFragment.setOnDateSetListener(this);

        Bundle argumentos = new Bundle();
        argumentos.putInt("anio", anio);
        argumentos.putInt("mes", mes);
        argumentos.putInt("dia", dia);
        newFragment.setArguments(argumentos);

        newFragment.show(getFragmentManager(),"DatePicker");
    }

    public void LanzarTimePickerDialog(int hora, int minutos){
        SelectorHora fragmento = new SelectorHora();
        fragmento.setOnTimeSetListener(this);

        Bundle argumentos = new Bundle();
        argumentos.putInt("hora", hora);
        argumentos.putInt("minutos", minutos);
        fragmento.setArguments(argumentos);

        fragmento.show(getFragmentManager(), "TimePicker");
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                NuevoEvento.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private void getResultsFromApi() {
        mCredential.setSelectedAccountName(Principal.getCuentaGoogle()); //Se manda llamar al valor de CuentaGoogle del Main Activity
        new MakeRequestTask(mCredential).execute();
    }


    private class MakeRequestTask extends AsyncTask<Void, Void, String> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;
        WeekViewEvent NuevoEvento;

        MakeRequestTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Mis Citas Medicas")
                    .build();
        }

        /**
         * Background task to call Google Calendar API.
         * @param params no parameters needed for this task.
         */
        @Override
        protected String doInBackground(Void... params) {
            try {
                return getDataFromApi();
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }
        }

        /**
         * Creates an Event
         */
        private String getDataFromApi() throws IOException {
            // Change the scope to CalendarScopes.CALENDAR and delete any stored
            // credentials.
            Event event = new Event()
                    .setSummary(TittleText.getText().toString());
                    //.setLocation(CalendarioPadre.getLocation());
                    //.setDescription("Probando zona horaria");

            //DateTime startDateTime = new DateTime("2017-05-02T09:00:00-07:00");
            //DateTime startDateTime = new DateTime("2017-06-09T18:00:00-05:00");
            DateTime startDateTime = new DateTime(Start.getTime(), Start.getTimeZone());
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime)
                    //.setTimeZone("America/Mexico_City");
                    .setTimeZone(Start.getTimeZone().getID());
            event.setStart(start);

            //DateTime endDateTime = new DateTime("2017-06-09T19:00:00-05:00");
            DateTime endDateTime = new DateTime(End.getTime(), End.getTimeZone());
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime)
                    //.setTimeZone("America/Mexico_City");
                    .setTimeZone(End.getTimeZone().getID());    //getID() devuelve el String "America/Mexico_City"
            event.setEnd(end);

            //Hacer el evento recurrente
            /*String[] recurrence = new String[] {"RRULE:FREQ=DAILY;COUNT=2"};
            event.setRecurrence(Arrays.asList(recurrence));

            EventAttendee[] attendees = new EventAttendee[] {
                    new EventAttendee().setEmail("lpage@example.com"),
                    new EventAttendee().setEmail("sbrin@example.com"),
            };
            event.setAttendees(Arrays.asList(attendees));*/

            //Recordatorio del evento
            EventReminder[] reminderOverrides = new EventReminder[] {
                    new EventReminder().setMethod("popup").setMinutes(15),
                    //new EventReminder().setMethod("popup").setMinutes(10),
            };
            Event.Reminders reminders = new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(reminderOverrides));
            event.setReminders(reminders);

            String calendarId = CalendarioPadre.getId();
            event = mService.events().insert(calendarId, event).execute();

            NuevoEvento = new WeekViewEvent(Principal.getEventosCalendarioSize(), event.getSummary()+" - "+CalendarioPadre.getNombre(), Start, End);
            NuevoEvento.setColor(Color.parseColor(CalendarioPadre.getColor()));
            Principal.AddEventosCalendario(NuevoEvento, event);

            //System.out.printf("Event created: %s\n", event.getHtmlLink());
            String s = String.format("Event created: %s\n", event.getHtmlLink());
            return s;
        }


        @Override
        protected void onPreExecute() {
            mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(String output) {
            mProgress.hide();
            mOutputText.setText(output);
            Toast.makeText(NuevoEvento.this, output, Toast.LENGTH_LONG).show();

            /*Intent calendar = new Intent(getApplicationContext(), InterfazCalendario.class);
            startActivity(calendar);*/

            finish();   //finalizar activity
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

    /*@Override
    protected void onDestroy(){
        super.onDestroy();
        Intent calendar = new Intent(getApplicationContext(), InterfazCalendario.class);
        startActivity(calendar);
    }*/

}
