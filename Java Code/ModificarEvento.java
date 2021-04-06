package example.com.miscitasmedicas;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

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
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;

public class ModificarEvento extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {
    long id_evento;
    String nombre_calendario, nombre_evento;
    Event event;
    Calendar Start, End;        //Start = fecha de inicio del evento,  End = fecha de termino del evento
    EditText TittleText;
    TextView CalendarName, StartTimeText, StartDateText, EndTimeText, EndDateText;
    Button UpdateButton;
    boolean flag;   //true = Inicio del evento, false = Fin del evento.
    ColorStateList ColorOriginal;
    ProgressDialog mProgress;
    GoogleAccountCredential mCredential;

    SimpleDateFormat TimeFormat = new SimpleDateFormat("hh:mm a");
    SimpleDateFormat DateFormat = new SimpleDateFormat("EEEE, d 'de' MMMM 'del' yyyy"); //4 letras o mas para usar el formato completo
    Calendario CalendarioPadre;  //Calendario donde se insertará el nuevo evento


    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;

    private static final String[] SCOPES = { CalendarScopes.CALENDAR };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.interfaz_modificar_evento);

        Bundle extras = getIntent().getExtras();
        id_evento = extras.getLong("id_evento");
        nombre_calendario = extras.getString("nombre_cal");
        nombre_evento = extras.getString("nombre_evento");

        CalendarioPadre = Principal.BuscaCalendarioPorNombre(nombre_calendario);
        event = Principal.BuscaEventoGooglePorPosicion((int) id_evento);

        //Inicializa calendarios
        Start = Calendar.getInstance();
        Start.setTimeInMillis(event.getStart().getDateTime().getValue()); //Se inicializa con la fecha en milis del evento a modificar
        End = Calendar.getInstance();
        End.setTimeInMillis(event.getEnd().getDateTime().getValue());

        TittleText = (EditText) findViewById(R.id.noombre_modificar);
        TittleText.setText(event.getSummary());

        CalendarName = (TextView) findViewById(R.id.calendario_modificar);
        String cal = "Calendario "+CalendarioPadre.getNombre();
        CalendarName.setText(cal);

        StartTimeText = (TextView) findViewById(R.id.hora_inicio_modificar);
        StartTimeText.setText(TimeFormat.format(Start.getTime()));
        ColorOriginal = StartTimeText.getTextColors();  //Extraer colores originales del TextView
        StartTimeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = true;
                LanzarTimePickerDialog(Start.get(Calendar.HOUR_OF_DAY), Start.get(Calendar.MINUTE));
            }
        });

        StartDateText = (TextView) findViewById(R.id.fecha_inicio_modificar);
        StartDateText.setText(DateFormat.format(Start.getTime()));
        StartDateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = true;
                LanzarDatePickerDialog(Start.get(Calendar.YEAR), Start.get(Calendar.MONTH), Start.get(Calendar.DAY_OF_MONTH));
            }
        });

        EndTimeText = (TextView) findViewById(R.id.hora_fin_modificar);
        EndTimeText.setText(TimeFormat.format(End.getTime()));
        EndTimeText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = false;
                LanzarTimePickerDialog(End.get(Calendar.HOUR_OF_DAY), End.get(Calendar.MINUTE));
            }
        });

        EndDateText = (TextView) findViewById(R.id.fecha_fin_modificar);
        EndDateText.setText(DateFormat.format(End.getTime()));
        EndDateText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flag = false;
                LanzarDatePickerDialog(End.get(Calendar.YEAR), End.get(Calendar.MONTH), End.get(Calendar.DAY_OF_MONTH));
            }
        });

        UpdateButton = (Button) findViewById(R.id.actualizar);
        UpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UpdateButton.setEnabled(false);

                if(End.after(Start)){
                    //Se retornara el resultado ok que siginifica que se debe cerrar la ventana detalle_evento
                    Intent intent = new Intent();
                    setResult(RESULT_OK,intent);

                    ModificaEventoApi();

                }else{
                    Toast.makeText(getApplicationContext(), "Error en las fechas del evento.", Toast.LENGTH_SHORT).show();
                }

                UpdateButton.setEnabled(true);
            }
        });

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
                ModificarEvento.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    private void ModificaEventoApi() {
        mCredential.setSelectedAccountName(Principal.getCuentaGoogle()); //Se manda llamar al valor de CuentaGoogle del Main Activity
        new ModificarEvento.MakeRequestTask(mCredential).execute();
    }


    private class MakeRequestTask extends AsyncTask<Void, Void, String> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

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
         * Update an Event
         */
        private String getDataFromApi() throws IOException {

            // Change the scope to CalendarScopes.CALENDAR and delete any stored credentials.
            // Retrieve the event from the API
            Event EventoAModificar = mService.events().get(CalendarioPadre.getId(), event.getId()).execute();

            // Make a change
            EventoAModificar.setSummary(TittleText.getText().toString());

            DateTime startDateTime = new DateTime(Start.getTime(), Start.getTimeZone());
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime)
                    //.setTimeZone("America/Mexico_City");
                    .setTimeZone(Start.getTimeZone().getID());
            EventoAModificar.setStart(start);

            //DateTime endDateTime = new DateTime("2017-06-09T19:00:00-05:00");
            DateTime endDateTime = new DateTime(End.getTime(), End.getTimeZone());
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime)
                    //.setTimeZone("America/Mexico_City");
                    .setTimeZone(End.getTimeZone().getID());    //getID() devuelve el String "America/Mexico_City"
            EventoAModificar.setEnd(end);

            // Update the event
            Event updatedEvent = mService.events().update(CalendarioPadre.getId(), EventoAModificar.getId(), EventoAModificar).execute();

            //System.out.println(updatedEvent.getUpdated());

            return String.format("Evento Modificado: %s\n", updatedEvent.getUpdated());
        }

        @Override
        protected void onPreExecute() {
            mProgress.show();
        }

        @Override
        protected void onPostExecute(String output) {
            mProgress.hide();
            Toast.makeText(ModificarEvento.this, output, Toast.LENGTH_LONG).show();

            mCredential = GoogleAccountCredential.usingOAuth2(
                    ModificarEvento.this, Arrays.asList(SCOPES))    //permisos para consultar eventos.
                    .setBackOff(new ExponentialBackOff());

            mCredential.setSelectedAccountName(Principal.getCuentaGoogle()); //Seleccionar cuenta de Google

            new ConsultaApiGoogle(ModificarEvento.this, mCredential, Principal.EVENTOS).execute(Principal.EVENTOS);

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
                    Toast.makeText(ModificarEvento.this, "The following error occurred:\n"
                            + mLastError.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(ModificarEvento.this, "Request cancelled.", Toast.LENGTH_LONG).show();
            }
        }
    }

}
