package example.com.miscitasmedicas;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

public class SelectorFecha extends DialogFragment {

    private DatePickerDialog.OnDateSetListener escuchador;

    public void setOnDateSetListener(DatePickerDialog.OnDateSetListener escuchador){
        this.escuchador = escuchador;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        // Use the current date as the default date in the picker
        int year=1991,month=5,day=15;

        Bundle args = this.getArguments();
        if(args != null){
            year = args.getInt("anio");
            month = args.getInt("mes");
            day = args.getInt("dia");
        }

        // Create a new instance of DatePickerDialog and return it
        return new DatePickerDialog(getActivity(), escuchador, year, month, day);
    }

}
