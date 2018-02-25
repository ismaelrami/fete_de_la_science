package fr.istic.m2il.mmm.fetescience.activities;

import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import java.util.List;

import fr.istic.m2il.mmm.fetescience.R;
import fr.istic.m2il.mmm.fetescience.fragments.EventFragment;
import fr.istic.m2il.mmm.fetescience.fragments.EventMapFragment;
import fr.istic.m2il.mmm.fetescience.models.Event;

public class EventMapActivity extends BaseActivity implements EventMapFragment.OnFragmentInteractionListener, EventFragment.OnEventFragmentInteractionListener {

    FragmentManager fragmentManager;
    private List<Event> events;

    //private MyLocationOverlay myLocation = null;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_map);
        EventMapFragment eventMapFragment = new EventMapFragment();
        Bundle bundle = new Bundle();
        if(getIntent().getExtras() != null){
            bundle.putBoolean("itinerary", true);
            bundle.putParcelableArrayList("events", getIntent().getExtras().getParcelableArrayList("events"));
            events = getIntent().getExtras().getParcelableArrayList("events");
            for(Event e : events){
                Log.v("test event",e.getGeolocalisation().toString());
            }
        }
        else {
            bundle.putBoolean("itinerary", false);
        }
        eventMapFragment.setArguments(bundle);
        fragmentManager = getSupportFragmentManager();

        if(savedInstanceState != null){
            return;
        }

        eventMapFragment.getMapAsync(eventMapFragment);
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.map_event, eventMapFragment);
        fragmentTransaction.commit();
        fragmentManager.executePendingTransactions();
    }

    @Override
    public void onItemSelected(Event event) {
        EventFragment eventFragment = new EventFragment();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.map_event, eventFragment);
        fragmentTransaction.commit();
        fragmentManager.executePendingTransactions();
        eventFragment.update(event);
    }

    @Override
    public void onEventInteraction(Event event) {}


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_events:
                Intent intent = new Intent(EventMapActivity.this, EventActivity.class);
                startActivity(intent);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}


