package com.thegalos.petbook.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.thegalos.petbook.objects.Pet;
import com.thegalos.petbook.R;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.app.Activity.RESULT_OK;


public class AddFeed extends Fragment {


    private static List<Pet> petArrayList = new ArrayList<>();
    private static List<String> petNameList = new ArrayList<>();
    SharedPreferences preferences;
    private static final int CAMERA_REQUEST = 999;
    ImageView ivPickPhoto;
    ImageView ivPhoto;
    Uri uri;
    ProgressBar progressBar;
    String imageLink;
    FirebaseUser user;
    DatabaseReference db;
    TextView etDetails;
    Spinner spnPet;
    Button btnPostFeed;
    ConstraintLayout postingLayout;
    boolean PhotoSelected = false;
    RadioGroup rgIsFree, rgWhoPays;
    EditText etAmount;
    boolean isFree = true;
    Context context;
    private Uri imageUri = null;




    public AddFeed() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.add_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view,  Bundle savedInstanceState) {
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        context = getContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        user = FirebaseAuth.getInstance().getCurrentUser();
        spnPet = view.findViewById(R.id.spnPet);
        progressBar = view.findViewById(R.id.progressBar);
        etDetails = view.findViewById(R.id.etDetails);
        btnPostFeed = view.findViewById(R.id.btnPostFeed);
        postingLayout = view.findViewById(R.id.postingLayout);
        rgIsFree = view.findViewById(R.id.rgIsFree);
        rgWhoPays = view.findViewById(R.id.rgWhoPays);
        etAmount = view.findViewById(R.id.etAmount);
        ivPhoto = view.findViewById(R.id.ivPhoto);
        ivPickPhoto = view.findViewById(R.id.ivPickPhoto);

        rgIsFree.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbFree) {
                    rgWhoPays.clearCheck();
                    rgWhoPays.setVisibility(View.INVISIBLE);
                    etAmount.setText("");
                    etAmount.setVisibility(View.INVISIBLE);
                    isFree = true;
                } else if (checkedId == R.id.rbPay) {
                    rgWhoPays.setVisibility(View.VISIBLE);
                    isFree = false;
                }
            }
        });
        rgWhoPays.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.rbToGet) {
                    etAmount.setVisibility(View.VISIBLE);
                    etAmount.setText("");
                } else if (checkedId == R.id.rbToPay) {
                    etAmount.setVisibility(View.VISIBLE);
                    etAmount.setText("");
                    rgWhoPays.setVisibility(View.VISIBLE);
                }
            }
        });

        loadData();
        ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(getActivity(), R.layout.color_spinner_layout, petNameList);
        nameAdapter.setDropDownViewResource(R.layout.spinner_dropdown_layout);
        spnPet.setAdapter(nameAdapter);
        ivPickPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickFromGallery();
            }
        });

        view.findViewById(R.id.btnPostFeed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (etDetails.getText().toString().equals("")) {
                    Toast.makeText(context, R.string.please_add_details_to_your_post, Toast.LENGTH_SHORT).show();
                    return;
                }
                if ((rgIsFree.getCheckedRadioButtonId() == R.id.rbPay)
                        && (rgWhoPays.getCheckedRadioButtonId() == R.id.rbToPay
                        || rgWhoPays.getCheckedRadioButtonId() == R.id.rbToGet)
                        && etAmount.getText().toString().equals("")) {
                    Toast.makeText(context, R.string.please_set_amount, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (spnPet == null || spnPet.getSelectedItem() == null) {
                    Toast.makeText(context, R.string.must_add_a_pet_to_advertise, Toast.LENGTH_SHORT).show();
                    return;
                }
                showProgress(true);
            }

        });
    }
    //Upload picture from gallery
    private void pickFromGallery() {
        // start picker to get image for cropping and then use the image in cropping activity
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(16,9)
                .start(context, this);
    }

    private void takePicture() {
        String pictureName = String.valueOf(System.currentTimeMillis());
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), pictureName + ".jpg");
        imageUri = FileProvider.getUriForFile(context,"com.thegalos.petbook.provider", file);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    private void showProgress(Boolean show) {
        if (show) {
            btnPostFeed.setEnabled(false);
            etDetails.setEnabled(false);
            spnPet.setEnabled(false);
            postingLayout.setVisibility(View.VISIBLE);
            if (PhotoSelected)
                uploadFiles();
            else
                justPost();
        } else {
            btnPostFeed.setEnabled(true);
            etDetails.setEnabled(true);
            spnPet.setEnabled(true);
            postingLayout.setVisibility(View.GONE);
        }

    }

    private void justPost() {
        db = FirebaseDatabase.getInstance().getReference().child("Posts").push();
        db.child("Time").setValue(ServerValue.TIMESTAMP);
        db.child("PostText").setValue(etDetails.getText().toString());
        db.child("SelectedPet").setValue(spnPet.getSelectedItem().toString());
        db.child("Pet").setValue(petArrayList.get(spnPet.getSelectedItemPosition()));
        db.child("Owner").setValue(user.getDisplayName());
        db.child("OwnerUID").setValue(user.getUid());
        db.child("ImageURL").setValue("null");
        if (!isFree) {
            db.child("IsFree").setValue("no");
            if (rgWhoPays.getCheckedRadioButtonId() == R.id.rbToGet)
                db.child("WhoPays").setValue("user");
            else
                db.child("WhoPays").setValue("owner");
            db.child("Amount").setValue(etAmount.getText().toString());
        } else {
            db.child("IsFree").setValue("yes");
            db.child("WhoPays").setValue("free");
            db.child("Amount").setValue("0");
        }

        showProgress(false);
        changeFragment();
    }

    private void uploadFiles() {
        FirebaseStorage storage;
        StorageReference storageReference;
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        if (uri != null) {
            final StorageReference ref = storageReference.child(user.getUid()).child("images/"+ UUID.randomUUID().toString());
            ref.putFile(uri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            db = FirebaseDatabase.getInstance().getReference().child("Posts").push();
                            db.child("Time").setValue(ServerValue.TIMESTAMP);
                            db.child("PostText").setValue(etDetails.getText().toString());
                            db.child("SelectedPet").setValue(spnPet.getSelectedItem().toString());
                            db.child("Pet").setValue(petArrayList.get(spnPet.getSelectedItemPosition()));
                            db.child("Owner").setValue(user.getDisplayName());
                            db.child("OwnerUID").setValue(user.getUid());
                            db.child("ImageURL").setValue("null");
                            if (!isFree) {
                                db.child("IsFree").setValue("no");
                                if (rgWhoPays.getCheckedRadioButtonId() == R.id.rbToGet)
                                    db.child("WhoPays").setValue("user");
                                else
                                    db.child("WhoPays").setValue("owner");
                                db.child("Amount").setValue(etAmount.getText().toString());
                            } else {
                                db.child("IsFree").setValue("yes");
                                db.child("WhoPays").setValue("free");
                                db.child("Amount").setValue("0");
                            }
                            ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    imageLink = (uri.toString());
                                    db.child("ImageURL").setValue(imageLink);
                                    showProgress(false);
                                    changeFragment();
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            showProgress(false);
                            Toast.makeText(context, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void changeFragment() {
        progressBar.setVisibility(View.GONE);
        FragmentTransaction ft = getParentFragmentManager().beginTransaction();
        ft.replace(R.id.flFragment, new MainFeed(), "MainFeed").commit();
    }


    public void onActivityResult(int requestCode, int resultCode,  Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                PhotoSelected = true;
//                uri = CropImage.getPickImageResultUri(context, data);
                uri = result.getUri();
                Glide.with(this).load(uri).into(ivPhoto);
//                Log.d("URI_Galos", "uri is: " + uri + " imageUri is: " + imageUri + " resultUri is: "/* + resultUri*/);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

        //New photo taken
        if (requestCode == CAMERA_REQUEST) {
            if (resultCode == RESULT_OK) {
                PhotoSelected = true;
//                Log.d("URI_Galos", "uri is: " + uri + " imageUri is: " + imageUri);
                uri = imageUri;
                UCrop.of(imageUri, uri)
                        .withAspectRatio(16, 10)
                        .withMaxResultSize(300, 200)
                        .start(getActivity());
            }
        }
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
        }
    }



    // load data
    private void loadData() {
        petNameList.clear();
        Gson gson = new Gson();
        String json = preferences.getString("PetList", null);
        Type type = new TypeToken<ArrayList<Pet>>() { }.getType();
        petArrayList = gson.fromJson(json, type);
        if (petArrayList == null)
            petArrayList = new ArrayList<>();

        for (Pet pet : petArrayList)
            petNameList.add(pet.getName());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==CAMERA_REQUEST) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
                Toast.makeText(context, R.string.must_give_permission, Toast.LENGTH_SHORT).show();
            else
                takePicture();
        }
    }
}