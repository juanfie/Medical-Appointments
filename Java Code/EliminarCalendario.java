package example.com.miscitasmedicas;

import android.app.ActionBar;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class EliminarCalendario extends AppCompatActivity {
    TextView nombre;
    Button eliminar, noEliminar;
    String name;
    int pos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eliminar_calendario);

        nombre = (TextView) findViewById(R.id.nombreCalendario);

        Bundle extras = getIntent().getExtras();
        name = extras.getString("nombre");
        pos = extras.getInt("posicion");

        nombre.setText("¿Eliminar calendario " + name + "?");

        eliminar = (Button) findViewById(R.id.botonSi);
        eliminar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent x = new Intent();
                x.putExtra("nombre", name);
                x.putExtra("posicion", pos);
                setResult(RESULT_OK, x);

                finish();
            }
        });

        noEliminar = (Button) findViewById(R.id.botonNo);
        noEliminar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

    }
}
