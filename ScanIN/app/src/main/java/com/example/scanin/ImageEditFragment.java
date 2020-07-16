package com.example.scanin;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.scanin.DatabaseModule.DocumentAndImageInfo;
import com.example.scanin.StateMachineModule.MachineActions;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ImageEditFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ImageEditFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private String TAG = "EDIT_FRAG";
    private View mainView;
    private View cropView;
    private PolygonView polygonView;
    private ProgressBar progressBar;

    RecyclerViewEditAdapter mAdapter = null;
    int CurrentMachineState = -1;
    Integer adapterPosition=0;
    RecyclerView recyclerView;

    public ImageEditFragment() {
        // Required empty public constructor
    }

    ImageEditFragment.ImageEditFragmentCallback imageEditFragmentCallback;

    public interface ImageEditFragmentCallback{
        void onCreateEditCallback();
        void onClickEditCallback(int action);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            imageEditFragmentCallback = (ImageEditFragment.ImageEditFragmentCallback) context;
        }
        catch (ClassCastException e){
            throw new ClassCastException(context.toString()
                    + "must implement imageEditFragmentCallback");
        }
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ImageEditFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ImageEditFragment newInstance(String param1, String param2) {
        ImageEditFragment fragment = new ImageEditFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_image_edit, container, false);
        ((ScanActivity)getActivity()).CurrentMachineState = this.CurrentMachineState;

        cropView = rootView.findViewById(R.id.rlContainer);
        mainView = rootView.findViewById(R.id.edit_main);
        cropView.setVisibility(View.GONE);
        polygonView = rootView.findViewById(R.id.polygonView);
        progressBar = rootView.findViewById(R.id.progressBar);
        ImageView cropImageView = rootView.findViewById(R.id.imageView);

        recyclerView = (RecyclerView)rootView.findViewById(R.id.recyclerview_image);

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
//        SpeedyLinearLayoutManager layoutManager = new SpeedyLinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        mAdapter = new RecyclerViewEditAdapter(null, (ScanActivity) getActivity());
        recyclerView.setAdapter(mAdapter);
        LinearSnapHelper pagerSnapHelper = new LinearSnapHelper();
//        PagerSnapHelper pagerSnapHelper = new PagerSnapHelper();
        pagerSnapHelper.attachToRecyclerView(recyclerView);

        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);

        rootView.findViewById(R.id.edit_add_more).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setClickable(false);
                imageEditFragmentCallback.onClickEditCallback(MachineActions.EDIT_ADD_MORE);
            }
        });

        rootView.findViewById(R.id.reorder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.setClickable(false);
                imageEditFragmentCallback.onClickEditCallback(MachineActions.REORDER);
            }
        });

        // crop button in main
        rootView.findViewById(R.id.crop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainView.setVisibility(View.GONE);
                cropView.setVisibility(View.VISIBLE);
//                Picasso.with(getContext()).load(recyclerView.getAd)
            }
        });

        // check button in crop
        rootView.findViewById(R.id.crop_apply).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cropView.setVisibility(View.GONE);
                mainView.setVisibility(View.VISIBLE);

            }
        });

        // back button in crop
        rootView.findViewById(R.id.crop_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cropView.setVisibility(View.GONE);
                mainView.setVisibility(View.VISIBLE);

            }
        });

        // auto-detect button in crop
        rootView.findViewById(R.id.crop_auto_detect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        // no-crop button in crop
        rootView.findViewById(R.id.crop_no_crop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        // rotate button in crop
        rootView.findViewById(R.id.crop_rotate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        imageEditFragmentCallback.onCreateEditCallback();
        return rootView;
    }

    public void setImagePathList(DocumentAndImageInfo documentAndImageInfo) {
        mAdapter.setmDataset(documentAndImageInfo);
    }

    public void setCurrentAdapterPosition(Integer position){
        adapterPosition = position;
    }

    public void setCurrentMachineState(int currentMachineState) {
        this.CurrentMachineState = currentMachineState;
    }

    @Override
    public void onDestroyView() {
        mAdapter.notifyDataSetChanged();
        super.onDestroyView();
    }
}