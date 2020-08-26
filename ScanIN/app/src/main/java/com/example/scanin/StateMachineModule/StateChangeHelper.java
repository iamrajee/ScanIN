package com.example.scanin.StateMachineModule;

import android.util.Log;

import com.example.scanin.PdfFragment;
import com.example.scanin.R;
import com.example.scanin.ScanActivity;

public class StateChangeHelper {
    public static void CameraActionChange(int action, ScanActivity context){
        Integer nextState = StateMachine.getNextState(MachineStates.CAMERA, action);
        if(nextState.equals(MachineStates.EDIT_1)){
            context.imageEditFragment.setCurrentMachineState(nextState);
            context.imageEditFragment.setCurrentAdapterPosition(context.documentAndImageInfo.getImages().size()-1);
            Log.d ("cameraAction", "Position = " + context.documentAndImageInfo.getImages().size());
            context.getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_edit, context.imageEditFragment)
                    .commit();
        }else if(nextState.equals(MachineStates.GRID_1)){
            context.imageGridFragment.setCurrentMachineState(nextState);
            context.getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_grid, context.imageGridFragment)
                    .commit();
        }
    }

    public static void GridActionChange(int action, ScanActivity context, Integer pos){
        Integer nextState = StateMachine.getNextState(context.CurrentMachineState, action);
        if(context.CurrentMachineState == MachineStates.GRID_1){
            if(nextState.equals(MachineStates.CAMERA)) {
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.imageGridFragment)
                        .commit();
                context.setCamera(nextState);
            }
            else if (nextState.equals(MachineStates.EDIT_1)) {
                context.imageEditFragment.setCurrentMachineState(nextState);
                context.imageEditFragment.setCurrentAdapterPosition(pos);
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.imageGridFragment)
                        .add(R.id.fragment_edit, context.imageEditFragment)
                        .commit();
            } else if (nextState.equals(MachineStates.PDF1)) {
                context.pdfFragment.setCurrentMachineState(nextState);
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.imageGridFragment)
                        .add(R.id.fragment_pdf, context.pdfFragment)
                        .commit();
            }
        }else if(context.CurrentMachineState == MachineStates.GRID_2){
            if(nextState.equals(MachineStates.CAMERA)) {
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.imageGridFragment)
                        .commit();
                context.startCamera();
            }
            else if (nextState.equals(MachineStates.EDIT_2)){
                context.imageEditFragment.setCurrentMachineState(nextState);
                context.imageEditFragment.setCurrentAdapterPosition(pos);
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.imageGridFragment)
                        .add(R.id.fragment_edit, context.imageEditFragment)
                        .commit();
            } else if (nextState.equals(MachineStates.PDF1)) {
                context.pdfFragment.setCurrentMachineState(nextState);
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.imageGridFragment)
                        .add(R.id.fragment_pdf, context.pdfFragment)
                        .commit();
            }
        }
    }

    public static void EditActionChange(int action, ScanActivity context){
        Integer nextState = StateMachine.getNextState(context.CurrentMachineState, action);
        Log.d ("stateMachine", "current = " + context.CurrentMachineState +
                ", next = " + nextState);
        if(context.CurrentMachineState == MachineStates.EDIT_1){
            if(nextState.equals(MachineStates.CAMERA)){
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.imageEditFragment)
                        .commit();
                context.setCamera(nextState);
            }else if(nextState.equals(MachineStates.GRID_1)){
                context.imageGridFragment.setCurrentMachineState(nextState);
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.imageEditFragment)
                        .add(R.id.fragment_grid, context.imageGridFragment)
                        .commit();
            }else if (nextState.equals(MachineStates.PDF1) || nextState.equals(MachineStates.PDF2)) {
                context.pdfFragment.setCurrentMachineState(nextState);
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.imageEditFragment)
                        .add(R.id.fragment_pdf, context.pdfFragment)
                        .commit();
            }
        }else if(context.CurrentMachineState == MachineStates.EDIT_2){
            if(nextState.equals(MachineStates.CAMERA)){
                Log.d("StateChange", "from edit2->camera");
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.imageEditFragment)
                        .commit();
                context.startCamera();
            }else if(nextState.equals(MachineStates.GRID_2)){
                context.imageGridFragment.setCurrentMachineState(nextState);
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.imageEditFragment)
                        .add(R.id.fragment_grid, context.imageGridFragment)
                        .commit();
            } else if (nextState.equals(MachineStates.PDF1) || nextState.equals(MachineStates.PDF2)) {
                context.pdfFragment.setCurrentMachineState(nextState);
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.imageEditFragment)
                        .add(R.id.fragment_pdf, context.pdfFragment)
                        .commit();
            }
        } else if (context.CurrentMachineState == MachineStates.EDIT_3) {
            if(nextState.equals(MachineStates.CAMERA)){
                Log.d("StateChange", "from edit2->camera");
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.imageEditFragment)
                        .commit();
                context.startCamera();
            }else if(nextState.equals(MachineStates.GRID_2)){
                context.imageGridFragment.setCurrentMachineState(nextState);
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.imageEditFragment)
                        .add(R.id.fragment_grid, context.imageGridFragment)
                        .commit();
            } else if (nextState.equals(MachineStates.PDF1) || nextState.equals(MachineStates.PDF2)) {
                context.pdfFragment.setCurrentMachineState(nextState);
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.imageEditFragment)
                        .add(R.id.fragment_pdf, context.pdfFragment)
                        .commit();
            }
        }
    }

    public static void HomeActionChange(int action, ScanActivity context){
        int nextState = StateMachine.getNextState(context.CurrentMachineState, action);
        if(nextState == MachineStates.CAMERA){
            context.startCamera();
        }else if(nextState == MachineStates.EDIT_2){
            context.imageEditFragment.setCurrentMachineState(nextState);
            context.imageEditFragment.setCurrentAdapterPosition(0);
            context.getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_edit, context.imageEditFragment)
                    .commit();
        } else if (nextState == MachineStates.PDF2) {
            context.pdfFragment.setCurrentMachineState(nextState);
            context.getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_pdf, context.pdfFragment)
                    .commit();
        }
    }

    public static void PdfActionChange(int action, ScanActivity context) {
        Integer nextState = StateMachine.getNextState(context.CurrentMachineState, action);
        if (context.CurrentMachineState == MachineStates.PDF1) {
            if (nextState.equals (MachineStates.EDIT_3)) {
                context.imageEditFragment.setCurrentMachineState(nextState);
                context.imageEditFragment.setCurrentAdapterPosition(0);
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.pdfFragment)
                        .add(R.id.fragment_edit, context.imageEditFragment)
                        .commit();
            } else if (nextState.equals (MachineStates.GRID_2)) {
                context.imageGridFragment.setCurrentMachineState(nextState);
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.pdfFragment)
                        .add(R.id.fragment_grid, context.imageGridFragment)
                        .commit();
            } else if (nextState.equals(MachineStates.HOME) || nextState.equals (MachineStates.ABORT)) {
                context.pdfGoHome();
            }
        } else if (context.CurrentMachineState == MachineStates.PDF2) {
            if (nextState.equals (MachineStates.EDIT_3)) {
                context.imageEditFragment.setCurrentMachineState(nextState);
                context.imageEditFragment.setCurrentAdapterPosition(0);
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.pdfFragment)
                        .add(R.id.fragment_edit, context.imageEditFragment)
                        .commit();
            } else if (nextState.equals (MachineStates.GRID_2)) {
                context.imageGridFragment.setCurrentMachineState(nextState);
                context.getSupportFragmentManager().beginTransaction()
                        .remove(context.pdfFragment)
                        .add(R.id.fragment_grid, context.imageGridFragment)
                        .commit();
            } else if (nextState.equals(MachineStates.HOME) || nextState.equals (MachineStates.ABORT)) {
                context.pdfGoHome();
            }
        }
    }

    public static void AnonymousActionChange(int currentState, int action, ScanActivity context){
        if(currentState == MachineStates.CAMERA){
            CameraActionChange(action, context);
        }else if(currentState == MachineStates.EDIT_1 || currentState ==MachineStates.EDIT_2 ||
        currentState == MachineStates.EDIT_3){
            EditActionChange(action, context);
        }else if(currentState == MachineStates.GRID_1 || currentState == MachineStates.GRID_2){
            GridActionChange(action, context, null);
        } else if(currentState == MachineStates.PDF1 || currentState == MachineStates.PDF2) {
            PdfActionChange(action, context);
        }
    }
}
