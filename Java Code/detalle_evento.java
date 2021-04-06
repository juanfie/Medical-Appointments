package example.com.miscitasmedicas;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;

import java.util.Arrays;
import java.util.List;

public class detalle_evento extends AppCompatActivity implements EliminarDialogFragment.EliminarDialogListener{
    TextView titulo, calendario, inicio, fin;
    Button eliminar,modificar;
    String nombre_evento, nombre_calendario;
    long id;

    List<Event> eventos;
    Event evento_eliminar;
    Calendario calendar;
    GoogleAccountCredential mCredential;
    private static final String[] SCOPES2 = { CalendarScopes.CALENDAR }; //Permisos para eliminar eventos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_evento);

        Bundle extras = getIntent().getExtras();

        id = extras.getLong("id");
        String[] nombres = extras.getString("nombre").split(" - ");
        nombre_evento = nombres[0];
        nombre_calendario = nombres[1];


        titulo = (TextView) findViewById(R.id.event_title);
        titulo.setText(nombre_evento);

        calendario = (TextView) findViewById(R.id.calendar);
        calendario.setText(nombre_calendario);

        inicio = (TextView) findViewById(R.id.event_start);
        inicio.setText(extras.getString("start"));

        fin = (TextView) findViewById(R.id.event_end);
        fin.setText(extras.getString("end"));

        eliminar = (Button) findViewById(R.id.eliminar_evento);
        eliminar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showEliminarDialog();

            }
        });

        modificar = (Button) findViewById(R.id.modificar_evento);
        modificar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(),ModificarEvento.class);
                intent.putExtra("nombre_cal",nombre_calendario);
                intent.putExtra("nombre_evento",nombre_evento);
                intent.putExtra("id_evento",id);    //valor de tipo long
                startActivityForResult(intent,5555);
            }
        });

        if(nombre_evento.equals("Ocupado")){
            eliminar.setClickable(false);
            eliminar.setTextColor(Color.BLACK);

            modificar.setClickable(false);
            modificar.setTextColor(Color.BLACK);
        }

    }

    public void showEliminarDialog() {
        // Create an instance of the dialog fragment and show it
        DialogFragment dialog = new EliminarDialogFragment();
        dialog.show(getSupportFragmentManager(), "EliminarDialogFragment");
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        eventos = Principal.getEventosGoogle();
        evento_eliminar = eventos.get((int) id);

        //Se retornara el resultado ok que siginifica que se debe cerrar la ventana interfazCalendario
        Intent intent = new Intent();
        setResult(RESULT_OK,intent);

        //crear credenciales para Google
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES2))    //Scopes2 (permisos) para consultar eventos.
                .setBackOff(new ExponentialBackOff());

        mCredential.setSelectedAccountName(Principal.getCuentaGoogle()); //Seleccionar cuenta de Google

        if(nombre_calendario.equals("Principal")){
            new ConsultaApiGoogle(detalle_evento.this, mCredential, "primary", "Principal" ,evento_eliminar.getId(), evento_eliminar.getSummary(),Principal.ELIMINAR_EVENTO).execute(Principal.ELIMINAR_EVENTO);

        }else{
            calendar = Principal.BuscaCalendarioPorNombre(nombre_calendario);

            if(calendar != null){
                new ConsultaApiGoogle(detalle_evento.this, mCredential, calendar.getId(), calendar.getNombre() ,evento_eliminar.getId(), evento_eliminar.getSummary(),Principal.ELIMINAR_EVENTO).execute(Principal.ELIMINAR_EVENTO);
            }

        }

    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.dismiss();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode==5555 && resultCode==RESULT_OK){
            //Se retornara el resultado ok que siginifica que se debe cerrar la ventana interfazCalendario
            Intent intent = new Intent();
            setResult(RESULT_OK,intent);

            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_edicion, menu);
        return true;
    }


}
