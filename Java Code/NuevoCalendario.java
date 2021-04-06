package example.com.miscitasmedicas;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class NuevoCalendario extends AppCompatActivity {
    Button cancelar, crear;
    EditText nombre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nuevo_calendario);

        nombre = (EditText) findViewById(R.id.nombre_nuevo_calendario);

        crear = (Button) findViewById(R.id.crear_calendario);
        crear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(nombre.getText().toString().equals("")){
                    Toast.makeText(getApplicationContext(), "Inserta un nombre", Toast.LENGTH_SHORT).show();

                }else{
                    Intent x = new Intent();
                    x.putExtra("nombre", nombre.getText().toString());
                    setResult(RESULT_OK, x);

                    finish();
                }
            }
        });

        cancelar = (Button) findViewById(R.id.cancelar_nuevo_calendario);
        cancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }
}
