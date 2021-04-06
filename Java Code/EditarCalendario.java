package example.com.miscitasmedicas;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class EditarCalendario extends AppCompatActivity {

    String name, new_name;
    int pos;
    Button Editar,Cerrar;
    EditText Nombre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_calendario);

        Bundle extras = getIntent().getExtras();
        name = extras.getString("nombre");
        pos = extras.getInt("posicion");

        Nombre = (EditText) findViewById(R.id.nombre_editar_calendario);
        Nombre.setText(name);

        Editar = (Button) findViewById(R.id.editar_calendario);
        Editar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                new_name = Nombre.getText().toString();

                if(new_name.equals("")){
                    Toast.makeText(EditarCalendario.this, "Inserta un nombre", Toast.LENGTH_SHORT).show();
                }else{
                    Intent x = new Intent();
                    x.putExtra("nombre", new_name);
                    x.putExtra("posicion", pos);
                    setResult(RESULT_OK, x);

                    finish();
                }

            }
        });

        Cerrar = (Button) findViewById(R.id.cancelar_editar);
        Cerrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }
}
