package com.amt.andalucismos.ui.nueva;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.amt.andalucismos.MainActivity;
import com.amt.andalucismos.R;
import com.amt.andalucismos.utils.Notificaciones;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class NuevaFragment extends Fragment {
    private View v;
    private Context c;
    ArrayAdapter<CharSequence> adapter;
    private EditText txtPalabra, txtSignificado, txtEjemplo, txtPoblacion, txtComarca, txtTags;
    private Spinner spnrProvincia;
    private Button btnAnadir;

    FirebaseUser fbUser;
    DatabaseReference database;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_nueva, container, false);

        c = getContext();
        database = FirebaseDatabase.getInstance().getReference();
        fbUser = FirebaseAuth.getInstance().getCurrentUser();
        inicializarVariables();

        return v;
    } //onCreateView()

    /**
     * Enlaza los elementos de la vista con las variables del código, configura el spinner para las
     * provincias y genera el evento de click para el botón de añadir.
     */
    private void inicializarVariables(){
        txtPalabra = v.findViewById(R.id.txtPalabra);
        txtSignificado = v.findViewById(R.id.txtSignificado);
        txtEjemplo = v.findViewById(R.id.txtEjemplo);
        txtPoblacion = v.findViewById(R.id.txtPoblacion);
        txtComarca = v.findViewById(R.id.txtComarca);
        txtTags = v.findViewById(R.id.txtTags);
        spnrProvincia = v.findViewById(R.id.spnrProvincia);
        btnAnadir = v.findViewById(R.id.btnAnadir);

        adapter = ArrayAdapter.createFromResource(c, R.array.provincias, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnrProvincia.setAdapter(adapter);

        btnAnadir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                anadirPalabra();
            }
        });
    } // inicializarVariables()

    /**
     * Valida los datos introducidos en el formulario de añadir palabra.
     * @return <b>boolean</b> Devuelve <i>true</i> si todos los campos obligatorios introducidos son
     * válidos y <i>false</i> si hay algún campo que no lo es.
     */
    private boolean validarFormulario(){
        String sPalabra = txtPalabra.getText().toString().trim();
        String sSignificado = txtSignificado.getText().toString().trim();
        String sEjemplo = txtEjemplo.getText().toString().trim();
        String sPoblacion = txtPoblacion.getText().toString().trim();
        String sComarca = txtComarca.getText().toString().trim();
        String sTags = txtTags.getText().toString();

        if(sPalabra.isEmpty() || sPalabra.equals("")){
            Notificaciones.makeToast(c, "La palabra no puede estar vacía", Toast.LENGTH_SHORT);
            return false;
        }
        else if(sSignificado.isEmpty() ){
            Notificaciones.makeToast(c, "Debes indicar el significado", Toast.LENGTH_SHORT);
            return false;
        }
        else if(sEjemplo.isEmpty()){
            Notificaciones.makeToast(c, "Debes indicar un ejemplo de uso", Toast.LENGTH_SHORT);
            return false;
        }
        if(sPoblacion.isEmpty()){
            txtComarca.setText("");
        }
        if(sComarca.isEmpty()){
            txtComarca.setText("");
        }
        if(sTags.isEmpty()){
            txtTags.setText("");
        }
        return true;
    } // validarFormulario()

    private void anadirPalabra() {
        if(hayConexion()){
            if(validarFormulario()){
                String sUsuarioId = fbUser.getUid();
                DatabaseReference dbrExpresionId = database.child("contribuciones").push();
                String sExpresionId = dbrExpresionId.getKey();
                Map<String, Object> datosPalabra = new HashMap<>();
                String sTags = new Gson().toJson(txtTags.getText().toString().toLowerCase().split("\\s"));

                datosPalabra.put("expresionId", sExpresionId);
                datosPalabra.put("palabra", capitalizarPrimeraLetra(txtPalabra.getText().toString().trim()));
                datosPalabra.put("significado", capitalizarPrimeraLetra(txtSignificado.getText().toString().trim()));
                datosPalabra.put("ejemplo", capitalizarPrimeraLetra(txtEjemplo.getText().toString().trim()));
                datosPalabra.put("poblacion", capitalizarPrimeraLetra(txtPoblacion.getText().toString().trim()));
                datosPalabra.put("provincia", spnrProvincia.getSelectedItem().toString());
                datosPalabra.put("comarca", capitalizarPrimeraLetra(txtComarca.getText().toString().trim()));
                datosPalabra.put("usuarioId", sUsuarioId);
                datosPalabra.put("numFavoritas", 0);
                datosPalabra.put("fechaAnadida", new Date().getTime()+"");
                datosPalabra.put("revisado", false);
                datosPalabra.put("tags", sTags);

                try {
                    dbrExpresionId.setValue(datosPalabra).addOnSuccessListener(unused -> {
                        limpiarCampos();
                        Notificaciones.makeDialog(c, "", "¡Gracias! Tenemos que revisar tu palabra, pero no te procupes, enseguida estará disponible."
                                , "", "", "ACEPTAR", new Notificaciones.RespuestaDialog() {
                                    @Override
                                    public void onPositivo() {}

                                    @Override
                                    public void onNegativo() {}

                                    @Override
                                    public void onNeutral() {
                                        Notificaciones.makeToast(c, "Palabra añadida", Toast.LENGTH_SHORT);
                                    }
                                });
                    }).addOnFailureListener(e -> {});
                }catch (Exception e){
                    Log.e("anadirPalabra", "Error al guardar la palabra.", e);
                    Notificaciones.makeToast(c, "Error al guardar la palabra. Por favor, inténtalo de nuevo.", Toast.LENGTH_SHORT);
                }
            }
        }else {
            Notificaciones.makeToast(c, "No hay conexión a Internet. No se puede añadir la palabra.", Toast.LENGTH_SHORT);
        }
    } // anadirPalabra()

    /**
     * Reinicia todos los campos de entrada del formulario a cadenas vacías.
     *
     * <br><br>Este método se utiliza para restablecer los campos de texto del formulario después de
     * una operación exitosa (como enviar datos al servidor) o para limpiar el formulario
     * antes de un nuevo uso. Afecta a los campos para palabra, significado, ejemplo,
     * población, comarca y etiquetas (tags).
     */
    private void limpiarCampos() {
        txtPalabra.setText("");
        txtSignificado.setText("");
        txtEjemplo.setText("");
        txtPoblacion.setText("");
        txtComarca.setText("");
        txtTags.setText("");
    } // limpiarCampos()

    /**
     * Convierte la primera letra de una frase completa a mayúscula.
     *
     * <br><br>Este método es útil para formatear cadenas en situaciones donde se requiere que la primera letra
     * de una frase esté en mayúscula, por ejemplo, al inicio de una oración o en un campo de texto que debe comenzar
     * con mayúscula por razones de formato. Si la cadena de entrada es nula o vacía, devuelve una cadena vacía.
     * Solo la primera letra de toda la frase se convierte a mayúscula; el resto de la frase no se modifica.
     *
     * @param texto La cadena de texto cuya primera letra se desea capitalizar.
     * @return Una cadena de texto con la primera letra en mayúscula si el texto no es nulo ni vacío;
     *         de lo contrario, devuelve una cadena vacía.
     */
    private String capitalizarPrimeraLetra(String texto) {
        if (texto == null || texto.isEmpty()) {
            return "";
        }
        return texto.substring(0, 1).toUpperCase() + texto.substring(1);
    } // capitalizarPrimeraLetra()

    /**
     * Verifica si hay una conexión de red activa y disponible.
     * <br><br>Este método comprueba si el dispositivo está conectado a una red y si esa
     * red tiene acceso a Internet. Utiliza el {@link ConnectivityManager} para
     * obtener detalles de la red activa y comprobar si está conectada o en proceso
     * de conexión.
     *
     * @return true si hay una conexión a Internet disponible, false en caso contrario.
     */
    private boolean hayConexion(){
        ConnectivityManager cm = (ConnectivityManager)getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }
}