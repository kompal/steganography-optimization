package com.example.kompal_paliwal.firecast_app;

/**
 * Created by Mitali on 21-03-2017.
 */

import android.content.Context;
import android.util.Log;
import android.view.View;
import jade.core.Agent;
import android.content.*;
import android.widget.TextView;
import jade.core.behaviours.OneShotBehaviour;

public class Simpleagent extends Agent {
    private Context context;
    private String agentName = "Simpleagent";
    public String operation;
    private TextView overlayText;
    private static final String TAG = "Agent";
    public String news;


    protected void setup() {

        Object[] args = getArguments();     //passing arguments to initialize the agent
        if (args != null && args.length > 0) {
            news = (String) args[0];
        }
        addBehaviour(new overlay());
    }

    public class overlay extends OneShotBehaviour {     //adding functionality to the agent
        public void action() {
            overlayText.setVisibility(news.length() > 0 ? View.VISIBLE : View.INVISIBLE);
            overlayText.setText(news.toString());
        }
    }
    protected void takeDown() {     //destroy the agent
    }

}






