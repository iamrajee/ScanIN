package com.example.scanin;


import android.content.res.Resources;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.recyclerview.widget.RecyclerView;

import com.example.scanin.DatabaseModule.DocumentAndImageInfo;
import com.example.scanin.ImageDataModule.BitmapTransform;
import com.squareup.picasso.Picasso;

public class RecyclerViewEditAdapter extends RecyclerView.Adapter<RecyclerViewEditAdapter.EditViewHolder> {
    private DocumentAndImageInfo documentAndImageInfo;
    private ProgressBar progressBar;
    private ScanActivity context;
    private static final int MAX_WIDTH = 1024;
    private static final int MAX_HEIGHT = 768;

    public class EditViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public EditViewHolder(View view){
            super(view);
            imageView =view.findViewById(R.id.image_edit_item);
        }
    }

    public RecyclerViewEditAdapter(DocumentAndImageInfo documentAndImageInfo, ScanActivity context){
        this.documentAndImageInfo = documentAndImageInfo;
        this.context = context;
    }

    public RecyclerViewEditAdapter.EditViewHolder onCreateViewHolder(ViewGroup parent, int viewtype){
        int layoutIdForImageAdapter =R.layout.image_edit_item;
        LayoutInflater inflater =LayoutInflater.from(parent.getContext());
        View view =inflater.inflate(layoutIdForImageAdapter, parent, false);
        return new EditViewHolder(view);
    }

    private void setViewInteract(View view, boolean canDo) {
        view.setEnabled(canDo);
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setViewInteract(((ViewGroup) view).getChildAt(i), canDo);
            }
        }
    }

    protected void showProgressBar(View view) {
        RelativeLayout rlContainer = view.findViewById(R.id.rlContainer);
        setViewInteract(rlContainer, false);
        progressBar.setVisibility(View.VISIBLE);
    }

    protected void hideProgressBar(View view) {
        RelativeLayout rlContainer = view.findViewById(R.id.rlContainer);
        setViewInteract(rlContainer, true);
        progressBar.setVisibility(View.GONE);
    }

//    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
//                                                         int reqWidth, int reqHeight) {
//
//        // First decode with inJustDecodeBounds=true to check dimensions
//        final BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inJustDecodeBounds = true;
//        BitmapFactory.decodeResource(res, resId, options);
//
//        // Calculate inSampleSize
//        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
//
//        // Decode bitmap with inSampleSize set
//        options.inJustDecodeBounds = false;
//        return BitmapFactory.decodeResource(res, resId, options);
//    }

    @Override
    public void onBindViewHolder(RecyclerViewEditAdapter.EditViewHolder holder, int position) {
        Uri uri = documentAndImageInfo.getImages().get(position).getUri();

        int size = (int) Math.ceil(Math.sqrt(MAX_WIDTH * MAX_HEIGHT));

// Loads given image
//        int h = holder.itemView.getLayoutParams().height;
//        int w = 0;
//        int x = 10;
//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        context.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        int h = holder.itemView.getHeight();
//        int w = holder.itemView.getWidth();
//        Log.d("holder_h", String.valueOf(w));
//        Log.d("holder_w", String.valueOf(w));
        int w = Resources.getSystem().getDisplayMetrics().widthPixels;
        if(position == 0) {
            holder.itemView.setPadding(40, 0, 0, 0);
            w+=40;
        }
        else if(position == getItemCount()-1){
            holder.itemView.setPadding(0, 0, 40, 0);
            w+=40;
        }
//        int h = w*3/4;
////        Log.d("holder_h", String.valueOf(h));
//        Log.d("holder_w", String.valueOf(w));
////        int h = displayMetrics.heightPixels;
//        holder.itemView.setLayoutParams(new ViewGroup.LayoutParams(w, h));
        holder.itemView.setLayoutParams(new ViewGroup.LayoutParams(w-80, ViewGroup.LayoutParams.MATCH_PARENT));
//        int size = (int) Math.ceil(Math.sqrt(w * h));
//        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.itemView.getLayoutParams();
//        layoutParams.setMargins();
        Picasso.with(holder.imageView.getContext())
                .load(uri)
                .transform(new BitmapTransform(MAX_WIDTH, MAX_HEIGHT))
                .resize(size, size)
                .centerInside()
                .into(holder.imageView);
        if(position != getItemCount() - 1){

//            Picasso.with()
//                    .load(documentAndImageInfo.getImages().get(position + 1).getUri())
//                    .into(new Target() {
//                        @Override
//                        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
//                        }
//
//                        @Override
//                        public void onBitmapFailed( Drawable errorDrawable) {
//                            // Error handling
//                        }
//
//                        @Override
//                        public void onPrepareLoad(Drawable placeHolderDrawable) {
//                        }
//                    });
        }
    }

    @Override
    public int getItemCount() {
        if(documentAndImageInfo == null) return 0;
        return documentAndImageInfo.getImages().size();
    }

    public void setmDataset(DocumentAndImageInfo documentAndImageInfo)
    {
        this.documentAndImageInfo = documentAndImageInfo;
        this.notifyDataSetChanged();
    }
}
