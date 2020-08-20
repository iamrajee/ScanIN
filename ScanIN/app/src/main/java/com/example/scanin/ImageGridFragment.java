package com.example.scanin;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scanin.DatabaseModule.Document;
import com.example.scanin.DatabaseModule.DocumentAndImageInfo;
import com.example.scanin.DatabaseModule.ImageInfo;
import com.example.scanin.DatabaseModule.Repository;
import com.example.scanin.StateMachineModule.MachineActions;
import com.example.scanin.Utils.FileUtils;
import com.woxthebox.draglistview.DragListView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import io.reactivex.disposables.CompositeDisposable;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImageGridFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImageGridFragment extends Fragment implements RecyclerViewGridAdapter.GridAdapterOnClickHandler {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int NUM_IMAGES_ALLOWED = 10000;
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private String TAG = "GRID_FRAG";
    private DocumentAndImageInfo documentAndImageInfo;
//    RecyclerViewGridAdapter mAdapter = null;
    private RecyclerViewGridAdapter mAdapter = null;
    private DragListView mDragListView = null;
    int CurrentMachineState = -1;
    private Repository mRepository;
    private ArrayList<Pair<Long, String>> mItemArray;

    public ImageGridFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ImageGridFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ImageGridFragment newInstance(String param1, String param2) {
        ImageGridFragment fragment = new ImageGridFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    ImageGridFragmentCallback imageGridFragmentCallback;

    public interface ImageGridFragmentCallback{
        void onCreateGridCallback();
        void onClickGridCallback(int action, Integer position);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        Log.d(TAG, "onCreateCalled");
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            imageGridFragmentCallback = (ImageGridFragmentCallback) context;
        }
        catch (ClassCastException e){
            throw new ClassCastException(context.toString()
                    + "must implement imageGridFragmentCallback");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_image_grid, container, false);
        ((ScanActivity)getActivity()).CurrentMachineState = this.CurrentMachineState;
        mRepository = ((ScanActivity)getActivity()).getRepository();
        mDragListView = (DragListView) rootView.findViewById(R.id.drag_list_view);

        mDragListView.getRecyclerView().setVerticalScrollBarEnabled(true);
//        mDragListView.getRecyclerView().setHorizontalScrollBarEnabled(true);

        mItemArray = new ArrayList<>();
        for (int i = 0; i < NUM_IMAGES_ALLOWED; i++) {
            mItemArray.add(new Pair<>((long) i, "Item " + i));
        }

        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        mDragListView.setLayoutManager(layoutManager);
        mAdapter = new RecyclerViewGridAdapter(documentAndImageInfo, this, mItemArray, R.id.grid_item, true);
        mDragListView.setAdapter(mAdapter, true);
        mDragListView.setCanDragHorizontally(true);
//        mDragListView.setCanDragVertically(true);
        mDragListView.setCustomDragItem(null);

        mDragListView.setDragListListener(new DragListView.DragListListenerAdapter() {
            @Override
            public void onItemDragEnded(int fromPosition, int toPosition) {
                if (fromPosition != toPosition) {
                    for (int i = 0; i < documentAndImageInfo.getImages().size(); i++) {
                        Log.d ("fragmentGrid", "" + i + " = " +
                                documentAndImageInfo.getImages().get(i).getPosition());
                    }
                    List<ImageInfo> images = documentAndImageInfo.getImages();
                    if (fromPosition < toPosition) {
                        images.add (toPosition + 1, images.get(fromPosition));
                        images.remove(fromPosition);
                    } else {
                        images.add (toPosition, images.get(fromPosition));
                        images.remove(fromPosition + 1);
                    }
                    for (int i = min (fromPosition, toPosition); i <= max(fromPosition, toPosition); i++) {
                        images.get(i).setPosition(i + 1);
                        mRepository.updateImage(images.get(i));
                    }
                    for (int i = 0; i < documentAndImageInfo.getImages().size(); i++) {
                        Log.d ("fragmentGrid", "" + i + " = " +
                                documentAndImageInfo.getImages().get(i).getPosition());
                    }
                }
            }
        });

        imageGridFragmentCallback.onCreateGridCallback();

        for (int i = 0; i < documentAndImageInfo.getImages().size(); i++) {
            Log.d ("fragmentGrid", "" + i + " = " +
                    documentAndImageInfo.getImages().get(i).getPosition());
        }

        TextView fileName = rootView.findViewById(R.id.file_name_edit);
        if (documentAndImageInfo != null)
            fileName.setText(documentAndImageInfo.getDocument().getDocumentName());

        rootView.findViewById(R.id.grid_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rootView.setClickable(false);
                imageGridFragmentCallback.onClickGridCallback(MachineActions.GRID_ADD_SCAN, null);
            }
        });

        rootView.findViewById(R.id.grid_save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //imageGridFragmentCallback.onClickGridCallback(MachineActions.GRID_ON_SAVE, null);
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext(), R.style.AlertDialogCustom);

                final EditText edittext = new EditText(getContext());
                edittext.setTextColor(getResources().getColor(R.color.black));
                edittext.setText(documentAndImageInfo.getDocument().getDocumentName());

                TextView textView = new TextView(getContext());
                textView.setText("Enter Name");
                textView.setPadding(20, 30, 20, 30);
                textView.setTextSize(20F);
                textView.setTextColor(Color.BLACK);
                alert.setCustomTitle(textView);

                alert.setView(edittext);

                alert.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String new_name = edittext.getText().toString();
                        if (FileUtils.validateFileName(new_name)) {
                            Document document = documentAndImageInfo.getDocument();
                            ((ScanActivity) Objects.requireNonNull(getActivity())).renameDoc(document, new_name);

                            imageGridFragmentCallback.onClickGridCallback(MachineActions.GRID_OPEN_PDF, null);
                        } else {
                            Toast.makeText(getContext(),
                                    "Allowed characters A-Z, a-z, 0-9, _, -", Toast.LENGTH_LONG)
                                    .show();
                        }
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // what ever you want to do with No option.
                    }
                });

                alert.show();
            }
        });

        return rootView;
    }

    @Override
    public void onClick(int position) {
//        ScanActivity scanActivity = (ScanActivity)getActivity();
//        scanActivity.imageEditFragment.setImagePathList(documentAndImageInfo);
        imageGridFragmentCallback.onClickGridCallback(MachineActions.GRID_ON_CLICK, position);
    }

    @Override
    public void onDestroyView() {
        Log.d("Scan-Activity2", "imageFragmentDestroyed");

        super.onDestroyView();
    }

    public void setImagePathList(DocumentAndImageInfo documentAndImageInfo) {
        this.documentAndImageInfo = documentAndImageInfo;
        mAdapter.setmDataset(documentAndImageInfo);
        mAdapter.notifyDataSetChanged();
    }

    public void setCurrentMachineState(int currentMachineState) {
        this.CurrentMachineState = currentMachineState;
    }
}