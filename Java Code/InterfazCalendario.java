package example.com.miscitasmedicas;


import android.content.Intent;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.alamkanak.weekview.MonthLoader;
import com.alamkanak.weekview.WeekView;
import com.alamkanak.weekview.WeekViewEvent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class InterfazCalendario extends AppCompatActivity implements WeekView.EventClickListener, MonthLoader.MonthChangeListener, WeekView.EventLongPressListener, WeekView.EmptyViewLongPressListener {
    private WeekView VistaCalendario;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calendario);

        // Get a reference for the week view in the layout.
        VistaCalendario = (WeekView) findViewById(R.id.weekView);

        // Show a toast message about the touched event.
        VistaCalendario.setOnEventClickListener(this);

        // The week view has infinite scrolling horizontally. We have to provide the events of a
        // month every time the month changes on the week view.
        VistaCalendario.setMonthChangeListener(this);

        // Set long press listener for events.
        VistaCalendario.setEventLongPressListener(this);

        // Set long press listener for empty view
        VistaCalendario.setEmptyViewLongPressListener(this);


    }


    protected String getEventTitle(Calendar time) {
        return String.format("Event of %02d:%02d %s/%d", time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE), time.get(Calendar.MONTH)+1, time.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void onEventClick(WeekViewEvent event, RectF eventRect) {
        //Toast.makeText(this, "Clicked " + event.getName(), Toast.LENGTH_SHORT).show();

        SimpleDateFormat TimeFormat = new SimpleDateFormat("hh:mm a");
        SimpleDateFormat DateFormat = new SimpleDateFormat("EEEE, d 'de' MMMM 'del' yyyy"); //4 letras o mas para usar el formato completo

        Intent intent = new Intent(this,detalle_evento.class);
        intent.putExtra("id",event.getId());
        intent.putExtra("nombre",event.getName());
        intent.putExtra("start", DateFormat.format(event.getStartTime().getTime()) + "   " + TimeFormat.format(event.getStartTime().getTime()));
        intent.putExtra("end", DateFormat.format(event.getEndTime().getTime()) + "   " + TimeFormat.format(event.getEndTime().getTime()));
        startActivityForResult(intent,4321);

    }

    @Override
    public void onEventLongPress(WeekViewEvent event, RectF eventRect) {
        Toast.makeText(this, "Long pressed event: " + event.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onEmptyViewLongPress(Calendar time) {
        Toast.makeText(this, "Empty view long pressed: " + getEventTitle(time), Toast.LENGTH_SHORT).show();
        Intent i = new Intent(this, NuevoEvento.class);
        //mandar datos a otra actividad
        i.putExtra("fecha", time.getTimeInMillis());    //Se manda un long con la fecha en milisegundos

        startActivityForResult(i,1234);
        //finish();   //terminar la actividad despues de lanzar la interfaz de nuevo evento
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode==1234){
            recreate(); //recargar activity
        }else if(requestCode==4321 && resultCode==RESULT_OK){
            finish();
        }
    }

    @Override
    public List<WeekViewEvent> onMonthChange(int newYear, int newMonth) {
        // Populate the week view with some events.
        //Los meses se empiezan a contar desde 0
        List<WeekViewEvent> events = getEvents(newYear, newMonth-1);
        //List<WeekViewEvent> events = Menu_inicial.getEventosCalendario();
        //List<WeekViewEvent> events = new ArrayList<WeekViewEvent>();

        return events;
    }

    public WeekView getWeekView() {
        return VistaCalendario;
    }

    //Busca los eventos dentro de EventosCalendario que coincidan con las variables anio y mes
    public List<WeekViewEvent> getEvents(int anio, int mes){
        Calendar Fecha;
        List<WeekViewEvent> Aux = new ArrayList<WeekViewEvent>();

        for (WeekViewEvent Evento: Principal.getEventosCalendario()){
            Fecha = Evento.getStartTime();

            if (mes == Fecha.get(Calendar.MONTH) && anio == Fecha.get(Calendar.YEAR)){
                Aux.add(Evento);
            }
        }

        return Aux;
    }

}
