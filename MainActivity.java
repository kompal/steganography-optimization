package com.example.kompal_paliwal.firecast_app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import jade.android.AgentContainerHandler;
import jade.android.AgentHandler;
import jade.android.RuntimeCallback;
import jade.android.RuntimeService;
import jade.android.RuntimeServiceBinder;
import jade.wrapper.StaleProxyException;

public class MainActivity extends AppCompatActivity {

private RuntimeServiceBinder runtimeServiceBinder;
    private ServiceConnection mServiceConnection;
    private AgentContainerHandler mainContainerHandler;

    private static final String TAG = "MainActivity";
    private TextView logConsole;
    private Handler handler;

    private FirebaseStorage storage=FirebaseStorage.getInstance(); // make a reference to firebase storage singleton instance , an instance of storage on firebase
    private View imageContainer;
    private TextView overlayText;
    private ProgressBar progressBar;
    private Button uploadButton;
    private TextView downloadUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Finds and initializes the views in the activity in xml file
        setContentView(R.layout.activity_main);
        imageContainer=findViewById(R.id.image_container);
        overlayText=(TextView) findViewById(R.id.overlay_text);
        overlayText.setText("");
        overlayText.setVisibility(View.INVISIBLE);
        EditText textInput =(EditText) findViewById(R.id.text_input);
        textInput.addTextChangedListener(new InputTextWatcher());
        uploadButton= (Button) findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(new UploadOnClickListener());
        progressBar= (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);
        downloadUrl= (TextView) findViewById(R.id.download_url);
	logConsole = (TextView) findViewById(R.id.logConsole);
        //Initial share preferences
       // sharedPreferences = this.getSharedPreferences(Constants.PREFS_FILE_NAME, Context.MODE_PRIVATE);
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                logConsole.append("\n" + msg.obj.toString());
                super.handleMessage(msg);
            }
        };
    }

    private class InputTextWatcher implements TextWatcher{
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after)
        {       }
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count)
        {        }
        @Override
        public void afterTextChanged(Editable s)
        {
            bindService();          //runtimeServiceBinder object is used to bind to the service
            String news= s.toString();
            createAgent("Simpleagent",news,Simpleagent.class.getName());
            overlayText.setVisibility(s.length() > 0 ? View.VISIBLE : View.INVISIBLE);
            overlayText.setText(s.toString());
            Log.i(TAG, "creating agent..");
        }
    }

    private class UploadOnClickListener implements View.OnClickListener
    {
      public void onClick(View view)
      {
          imageContainer.setDrawingCacheEnabled(true); //Step 1 : using android'S api's to get the bitmap of the images that we need to upload
          imageContainer.buildDrawingCache();
          Bitmap bitmap= imageContainer.getDrawingCache();
          ByteArrayOutputStream baos= new ByteArrayOutputStream(); //  Step 2 : compressing that bitmap to png format
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
          imageContainer.setDrawingCacheEnabled(false);
          byte[] data= baos.toByteArray(); // raw pixels in the byte_array

          String path= "drawable/"+ UUID.randomUUID()+".png";
         // String path="drawable/kompal.JPG";
          StorageReference firememeRef = storage.getReference(path);

          StorageMetadata metadata=new StorageMetadata.Builder() // create a meta data storage on  the cloud
                  .setCustomMetadata("text", overlayText.getText().toString())
                  .build();

          progressBar.setVisibility(View.VISIBLE);
          uploadButton.setEnabled(false);

          UploadTask uploadTask = firememeRef.putBytes(data, metadata);
          uploadTask.addOnSuccessListener(MainActivity.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
              @Override
              public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                  progressBar.setVisibility(View.GONE);
                  uploadButton.setEnabled(true);

                  Uri url=taskSnapshot.getDownloadUrl();
                  downloadUrl.setText(url.toString());
                  downloadUrl.setVisibility(View.VISIBLE);
              }
          });
      }
    }
private void bindService() {
                //Check runtime service
                if (runtimeServiceBinder == null) {
                    //The binder is a user defined interface exposed by an Android service when another Android component binds to that service through the Context.bindService() method
                    //Create Runtime Service Binder here
                    //retrieve a runtimeServiceBinder object using a subclass of ServiceConnection (in package android.content)
                    mServiceConnection = new ServiceConnection() {
                        @Override
                        public void onServiceConnected(ComponentName componentName, IBinder service) {
                            exportLogConsole("Creating new connection ");
                            runtimeServiceBinder = (RuntimeServiceBinder) service;
                            Log.i(TAG, "###Gateway successfully bound to RuntimeService");
                            exportLogConsole("Gateway successfully bound to RuntimeService ");
                            startMainContainer();
                        }

                        @Override
                        public void onServiceDisconnected(ComponentName componentName) {
                            Log.i(TAG, "###Gateway unbound from RuntimeService");
                        }
                    };
                    Log.i(TAG, "###Binding Gateway to RuntimeService...");
                    //application context of the current Android application is passed to the agent as first argument
                    bindService(new Intent(getApplicationContext(), RuntimeService.class), mServiceConnection, Context.BIND_AUTO_CREATE);  //
                } else {
                    startMainContainer();
                }
            }
 private void startMainContainer() {
                //starting the JADE container using runtimeServiceBinder object
                runtimeServiceBinder.createMainAgentContainer(new RuntimeCallback<AgentContainerHandler>() {

                    @Override
                    public void onSuccess(AgentContainerHandler agentContainerHandler) {
                        mainContainerHandler = agentContainerHandler;
                        Log.i(TAG, "###Main-Container created...");
                        exportLogConsole("Main-Container created...");
                    }


                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.i(TAG, "###Failed to create Main Container");
                    }
                });
            }

            private void createAgent(String name,String news,String className) {
                if (mainContainerHandler != null) {
                    Object[] args = new Object[1];
                    args[0] =news;
                    //all operations are asynchronous and the result is made available by means of a RuntimeCallback (in package jade.android) object
                    mainContainerHandler.createNewAgent(name, className,
                           args, new RuntimeCallback<AgentHandler>() {
                                @Override
                                public void onSuccess(AgentHandler agentHandler) {
                                    try {
                                        Log.i(TAG, "###Success to create agent: " + agentHandler.getAgentController().getName());
                                        exportLogConsole("Success to create agent: " + agentHandler.getAgentController().getName());
                                        agentHandler.getAgentController().start();
                                    } catch (StaleProxyException e) {
                                        e.printStackTrace();
                                    }
                                }

                                @Override
                                public void onFailure(Throwable throwable) {
                                    Log.i(TAG, "###Failed to created an Agent");
                                }
                            });

                } else {
                    Log.e(TAG, "###Can't get Main-Container to create agent");
                }
            }

            public void exportLogConsole(String log) {
                Message logMessage = new Message();
                logMessage.obj = log;
                handler.sendMessage(logMessage);
            }


}
