package example.com.miscitasmedicas;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.format.DateFormat;

public class SelectorHora extends DialogFragment {

    private TimePickerDialog.OnTimeSetListener escuchador;

    public void setOnTimeSetListener(TimePickerDialog.OnTimeSetListener escuchador){
        this.escuchador = escuchador;
    }

    @Override
    public Dialog onCreateDialog(Bundle saveInstanceState){
        // Use the current time as the default values for the picker
        //Calendar c = Calendar.getInstance();
        int hora=0,minutos=0;

        Bundle args = this.getArguments();
        if(args != null){
            hora = args.getInt("hora");
            minutos = args.getInt("minutos");
        }

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), escuchador, hora, minutos, DateFormat.is24HourFormat(getActivity()));
    }


}
