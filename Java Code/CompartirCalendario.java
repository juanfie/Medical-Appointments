package example.com.miscitasmedicas;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompartirCalendario extends AppCompatActivity {

    String id,nombre,correo_compartir,permisos_compartir;
    Button Bcerrar, Bcompartir;
    EditText correo;
    Spinner spinner;
    GoogleAccountCredential mCredential;
    private static final String[] SCOPES2 = { CalendarScopes.CALENDAR }; //Permisos para crear Acl


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compartir_calendario);

        Bundle extras = getIntent().getExtras();
        nombre = extras.getString("nombre");
        id = extras.getString("id");


        final List<String> role = new ArrayList<String>();
        role.add("reader");
        role.add("writer");
        role.add("owner");

        // Spinner Drop down elements
        List<String> categorias = new ArrayList<String>();
        categorias.add("Lectura");
        categorias.add("Escritura");
        categorias.add("Propietario");

        spinner = (Spinner) findViewById(R.id.spinner_compartir);
        ArrayAdapter adaptador = new ArrayAdapter(this,android.R.layout.simple_dropdown_item_1line, categorias);
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adaptador);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                permisos_compartir = role.get(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        correo = (EditText) findViewById(R.id.correo_compartir);

        Bcerrar = (Button) findViewById(R.id.cerrar_compartir);
        Bcerrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        Bcompartir = (Button) findViewById(R.id.compartir_calendario);
        Bcompartir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                correo_compartir = correo.getText().toString();

                if(correo_compartir.equals("")){
                    Toast.makeText(CompartirCalendario.this, "Inserta un correo de Gmail", Toast.LENGTH_SHORT).show();
                }else{
                    CompartirCalendarioApi(id, nombre);
                }


            }
        });
    }

    private void CompartirCalendarioApi(String id_cal, String cal_name) {
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES2))    //Scopes2 (permisos) para eliminar calendario.
                .setBackOff(new ExponentialBackOff());

        mCredential.setSelectedAccountName(Principal.getCuentaGoogle()); //Cuenta de Google

        new ConsultaApiGoogle(this, mCredential, id_cal, cal_name, correo_compartir, permisos_compartir,Principal.COMPARTIR_CALENDARIO).execute(Principal.COMPARTIR_CALENDARIO);
    }

}
