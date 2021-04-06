package example.com.miscitasmedicas;


public class Calendario {
    private String Id;
    private String Nombre;
    private String Color;
    private String Permisos;
    private Boolean Seleccionado;

    public Calendario(String id, String nombre, String color, String permisos) {
        Id = id;
        Nombre = nombre;
        Color = color;
        Permisos = permisos;
        Seleccionado=false;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getNombre() {
        return Nombre;
    }

    public void setNombre(String nombre) {
        Nombre = nombre;
    }

    public String getColor() {
        return Color;
    }

    public void setColor(String color) {
        Color = color;
    }

    public String getPermisos() {
        return Permisos;
    }

    public void setPermisos(String permisos) {
        Permisos = permisos;
    }

    public Boolean getSeleccionado() {
        return Seleccionado;
    }

    public void setSeleccionado(Boolean seleccionado) {
        Seleccionado = seleccionado;
    }
}
