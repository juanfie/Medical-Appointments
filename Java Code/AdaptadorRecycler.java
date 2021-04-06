package example.com.miscitasmedicas;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.services.calendar.model.CalendarListEntry;

import java.util.List;


public class AdaptadorRecycler extends RecyclerView.Adapter<AdaptadorRecycler.ViewHolderDatos> {

    Context context;
    List<Calendario> Datos;

    public AdaptadorRecycler(Context context, List<Calendario> datos) {
        this.context = context;
        this.Datos = datos;
    }

    @Override
    public AdaptadorRecycler.ViewHolderDatos onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_item, parent, false);
        return new ViewHolderDatos(view, Datos);
    }

    @Override
    public void onBindViewHolder(AdaptadorRecycler.ViewHolderDatos holder, int position) {
        holder.TxtNombre.setText(Datos.get(position).getNombre());
        holder.TxtNombre.setBackgroundColor(Color.parseColor(Datos.get(position).getColor()));
        //holder.cardView.setBackgroundColor(Color.parseColor(Datos.get(position).getBackgroundColor()));
    }

    @Override
    public int getItemCount() {
        return Datos.size();
    }

    public class ViewHolderDatos extends RecyclerView.ViewHolder implements View.OnClickListener {

        CardView cardView;
        TextView TxtNombre;
        ImageButton BtnCompartir;
        ImageButton BtnBorrar;
        ImageButton BtnEditar;
        CheckBox Selector;
        List<Calendario> DatosCalendarios;


        public ViewHolderDatos(final View itemView, List<Calendario> Datos) {
            super(itemView);

            DatosCalendarios = Datos;   //almacenamos los datos para poder utilizarlos a lo largo de esta clase.

            cardView = (CardView) itemView.findViewById(R.id.CardView);
            TxtNombre = (TextView) itemView.findViewById(R.id.nombre);

            BtnCompartir = (ImageButton) itemView.findViewById(R.id.compartir);
            BtnCompartir.setOnClickListener(this);

            BtnBorrar = (ImageButton) itemView.findViewById(R.id.borrar);
            BtnBorrar.setOnClickListener(this);

            BtnEditar = (ImageButton) itemView.findViewById(R.id.editar);
            BtnEditar.setOnClickListener(this);

            Selector = (CheckBox) itemView.findViewById(R.id.seleccion);
            Selector.setOnClickListener(this);
        }


        @Override
        public void onClick(View view) {
            int posicion = getAdapterPosition();

            if(view.getId() == BtnBorrar.getId()){

                Intent intent = new Intent(context, EliminarCalendario.class);
                intent.putExtra("nombre",Datos.get(posicion).getNombre());
                intent.putExtra("posicion", posicion);
                ((AppCompatActivity) context).startActivityForResult(intent, 12345);

            }else if(view.getId() == BtnCompartir.getId()){
                if(Datos.get(posicion).getPermisos().equals("owner")){

                    Intent intent = new Intent(context, CompartirCalendario.class);
                    intent.putExtra("nombre", Datos.get(posicion).getNombre());
                    intent.putExtra("id", Datos.get(posicion).getId());
                    context.startActivity(intent);

                }else{
                    Toast.makeText(view.getContext(), "No tienes permisos para realizar esta acción", Toast.LENGTH_SHORT).show();
                }


            }else if(view.getId() == BtnEditar.getId()){
                if(Datos.get(posicion).getPermisos().equals("owner")){

                    Intent intent = new Intent(context, EditarCalendario.class);
                    intent.putExtra("nombre",Datos.get(posicion).getNombre());
                    intent.putExtra("posicion", posicion);
                    ((AppCompatActivity) context).startActivityForResult(intent, 54321);

                }else{
                    Toast.makeText(view.getContext(), "No tienes permisos para realizar esta acción", Toast.LENGTH_SHORT).show();
                }

            }else if(view.getId() == Selector.getId()){
                if( ((CheckBox) view).isChecked() ){
                    DatosCalendarios.get(posicion).setSeleccionado(true);
                    Toast.makeText(view.getContext(), "Seleccionado " + posicion, Toast.LENGTH_SHORT).show();
                }else{
                    DatosCalendarios.get(posicion).setSeleccionado(false);
                    Toast.makeText(view.getContext(), "No Seleccionado " + posicion, Toast.LENGTH_SHORT).show();
                }

            }
        }
    }
}
