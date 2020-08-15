package com.example.scanin;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.scanin.DatabaseModule.AppDatabase;
import com.example.scanin.DatabaseModule.DocumentAndImageInfo;
import com.example.scanin.DatabaseModule.Repository;
import com.example.scanin.StateMachineModule.MachineActions;
import com.example.scanin.StateMachineModule.MachineStates;
import com.example.scanin.StateMachineModule.StateChangeHelper;
import com.example.scanin.StateMachineModule.StateMachine;

import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class PdfActivity extends AppCompatActivity {
    String TAG = "Pdf-Activity";
    public int CurrentMachineState = -1;
    private AppDatabase appDatabase;
    private String documentName = null;
    private long current_document_id = -1;
    private Repository repository;

    //Schedulers for Image and Database
    private Scheduler preview_executor = Schedulers.newThread();
    private final CompositeDisposable disposable = new CompositeDisposable();

    public DocumentAndImageInfo documentAndImageInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_pdf);
        overridePendingTransition(R.anim.trans_left_in, R.anim.trans_left_out);
    }

    @Override
    public void onBackPressed() {
        int nextState = StateMachine.getNextState(CurrentMachineState, MachineActions.BACK);
        if(nextState == MachineStates.ABORT){
            CurrentMachineState = -1;
            super.onBackPressed();
            overridePendingTransition(R.anim.trans_right_in, R.anim.trans_right_out);
        }else{
            StateChangeHelper.AnonymousActionChange(this.CurrentMachineState, MachineActions.BACK, ScanActivity.this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause Called");
        if(CurrentMachineState == MachineStates.CAMERA) {
            findViewById(R.id.fragment_tools).setBackgroundColor(Color.parseColor("#000000"));
        }
    }
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.fragment_tools).setBackgroundColor(Color.parseColor("#00000000"));
            }
        }, 600);
        super.onResume();
    }
}
