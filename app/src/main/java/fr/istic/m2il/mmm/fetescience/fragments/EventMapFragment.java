package fr.istic.m2il.mmm.fetescience.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import fr.istic.m2il.mmm.fetescience.R;
import fr.istic.m2il.mmm.fetescience.loaders.AsyncTaskMap;
import fr.istic.m2il.mmm.fetescience.loaders.AsyncEventLoader;
import fr.istic.m2il.mmm.fetescience.services.GMapV2Direction;
import fr.istic.m2il.mmm.fetescience.models.Event;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link EventMapFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class EventMapFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Event>> {

    private static final String TAG = EventMapFragment.class.getSimpleName();

    private List<Event> events = new ArrayList<>();
    private OnFragmentInteractionListener mListener;
    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private ClusterManager mClusterManager;
    private boolean itinerary;

    private FusedLocationProviderClient mFusedLocationClient;

    public EventMapFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            events = getArguments().getParcelableArrayList("events");
            itinerary = getArguments().getBoolean("itinerary");
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
        View rootView = inflater.inflate(R.layout.fragment_event_map, container, false);
        mClusterManager = new ClusterManager<Event>(getActivity(), mMap);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            mapFragment.getMapAsync(googleMap -> {
                MapStyleOptions mapStyleOptions = MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.google_map_style);
                mMap = googleMap;
                mMap.setMapStyle(mapStyleOptions);
                mClusterManager = new ClusterManager<Event>(getActivity(), mMap);
                mMap.setOnCameraIdleListener(mClusterManager);
                mMap.setOnMarkerClickListener(mClusterManager);
                mMap.setOnInfoWindowClickListener(mClusterManager);
                mClusterManager.cluster();
            });
            getLoaderManager().initLoader(0, null, this).forceLoad();
            getChildFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
        }
        return rootView;
    }

    /**
     *
     * @param e The current event
     */
    public void onEventClicked(Event e) {
        if (mListener != null) {
            mListener.onItemSelected(e);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public Loader<List<Event>> onCreateLoader(int id, Bundle args) {
        return new AsyncEventLoader(getActivity());
    }


    @Override
    public void onLoadFinished(Loader<List<Event>> loader, List<Event> data) {
        // cas normal
        // affichage de tous les evenements
        if(!itinerary){
            this.events = data;
            addMarkers();
        }
        /* cas intineraire
           affichage des éléments de l'itinerary
        */
        else {
            searchCurrentLocationUser();
        }
    }

    @Override
    public void onLoaderReset(Loader<List<Event>> loader) {

    }

    /**
     * fonction qui permet d'ajouter les marqueurs
     * sur la totalité des évenements de l'application
     */
    public void addMarkers() {
        LatLng pEvent = new LatLng(48.8534, 2.3488);
        for (Event event : this.events) {
            // on ajoute le marker sur la map par l'intermediaire du cluster manager
            if(event.getGeolocalisation()!= null)
                mClusterManager.addItem(event);
        }
        ClusterManager.OnClusterItemInfoWindowClickListener<Event> listener = (event -> {
            try {
                new Handler().postDelayed(() -> {
                    if(event.getTag() != null)
                        onEventClicked(event);
                }, 100);
            } catch (Exception e) {
                Log.e(TAG, "Exception :: " + e.getMessage());
            }
        });

        mClusterManager.setOnClusterItemInfoWindowClickListener(listener);
        // on se fixe sur le dernier evt vu
        // à garder pour la fin ?
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pEvent, 6));
    }

    public void addMarker(Event event) {
        /* on récupère la liste de la localistion de l'evt
           et on créé le latlng
        */
        List<Double> locEvent = event.getGeolocalisation();
        // on vérifie que l'evenement possède une localisation précise
        if (locEvent != null) {
            // on récupère la localisation
            LatLng pEvent = new LatLng(locEvent.get(0), locEvent.get(1));
            // on créé le nouveau marker
            mMap.addMarker(new MarkerOptions()
                    .title(event.getTitre_fr())
                    .snippet(event.getDescription_fr())
                    .position(pEvent))
                    .setTag(event);
        }
    }

    /**
     * fonction qui permet de créer l'itinéraire à partir
     * d'une liste d'evenements
     */
    public void createRoute(String navigationMode , Location locationUser){
        List<Double> geo;
        Event prevEvent = null;
        // à chaque event, nous allons créer
        for (Event event : events) {
            // création de l'Async task
            AsyncTaskMap asyncTaskMap = new AsyncTaskMap();
            asyncTaskMap.setMode(navigationMode);
            geo = event.getGeolocalisation();
            if (geo != null) {
                // la première fois on utilise notre position
                // les autres fois les events récupérés
                // on récupère le point de départ de l'itinéraire
                if(prevEvent != null){
                    List<Double> geo2 = prevEvent.getGeolocalisation();
                    LatLng latlng1 = new LatLng(geo2.get(0),geo2.get(1));
                    asyncTaskMap.setStartPoint(latlng1);
                }
                else {
                    LatLng latlng1 = new LatLng(locationUser.getLatitude(), locationUser.getLongitude());
                    asyncTaskMap.setStartPoint(latlng1);
                }

                // on récupère le point d'arrive de l'itinéraire
                //geo = event.getGeolocalisation();
                LatLng latlng2 = new LatLng(geo.get(0),geo.get(1));
                asyncTaskMap.setArrivalPoint(latlng2);
                addMarker(event);

                prevEvent = event;
                PolylineOptions rectLine = null;
                try {
                    rectLine = asyncTaskMap.execute().get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                // on ajoute l'itinerary à la map
                mMap.addPolyline(rectLine);

                // on positionne sur la map
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng2, 6));
            }
        }
        mMap.setOnInfoWindowClickListener((marker) -> {
            marker.hideInfoWindow();
            try {
                new Handler().postDelayed(() -> {
                    onEventClicked((Event) marker.getTag());
                }, 100);

                return;
            } catch (Exception e) {
                Log.e(TAG, "Exception :: " + e.getMessage());
            }
        });
    }

    public void searchCurrentLocationUser(){
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            Toast.makeText(getActivity(), "Vous devez acceptez pour utiliser toutes les features !", Toast.LENGTH_LONG).show();
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        }
        // scotch race condition
        // le thread attend la bonne connexion au serveur
        SystemClock.sleep(300);

        mFusedLocationClient.getLastLocation().addOnSuccessListener((location) -> {
            // scotch race condition
            // le thread attend la bonne connexion au serveur
            SystemClock.sleep(200);

            if (location != null) {
                createRoute(GMapV2Direction.MODE_DRIVING, location);
            }
            else {
                Log.i(TAG, "Location is null");

            }
        });
    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onItemSelected(Event item);
    }
}
