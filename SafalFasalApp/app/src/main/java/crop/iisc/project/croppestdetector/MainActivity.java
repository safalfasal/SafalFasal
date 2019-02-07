package crop.iisc.project.croppestdetector;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private static final int REQUEST_PERMISSION = 12;
    private static final int REQUEST_IMAGE_CAPTURE = 13;
    //private TextView messageText;
    private EditText etxtUpload;
    private ImageView imageview;
    private ProgressDialog dialog = null;
    private JSONObject jsonObject;
    private String mCurrentPhotoPath;
    private boolean def = false;
    private double latitude;
    private double longitude;
    private int loc = 0;
    private Uri uri;
    private String encodedImage;
    private Bitmap image;
    private JsonObjectRequest jsonObjectRequest;


    //private Button btnShowLocation;
    //String[] mPermission = {Manifest.permission.ACCESS_FINE_LOCATION};
                            //Manifest.permission.WRITE_EXTERNAL_STORAGE};
    GPSTracker gps = new GPSTracker();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Runtime.getRuntime().gc();
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION);

        }

        Button uploadButton = (Button) findViewById(R.id.uploadButton);
        Button btnselectpic = (Button) findViewById(R.id.button_selectpic);
        //messageText  = (TextView)findViewById(R.id.messageText);
        imageview = (ImageView)findViewById(R.id.imageView_pic);
        etxtUpload = (EditText)findViewById(R.id.etxtUpload);
        Button clickCamera = (Button) findViewById(R.id.button_clickcamera);

        //btnShowLocation = (Button) findViewById(R.id.buttonloc);

        loc = gpstry();

        if(loc == 1 && longitude != 0.0 && latitude != 0.0)
        {
            int done = 0;
            etxtUpload.setVisibility(View.GONE);
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

            String city="",state = "",zip = "" ,country = "";
            try {
                List<Address> addresses  = geocoder.getFromLocation(latitude,longitude, 1);
                city = addresses.get(0).getLocality();
                state = addresses.get(0).getAdminArea();
                zip = addresses.get(0).getPostalCode();
                country = addresses.get(0).getCountryName();
                done = 1;
            }
            catch (Exception e){
                e.printStackTrace();
            }

            if(done == 1)
            {
                etxtUpload.setText(city+" "+state+" "+zip+" "+country);
            }
            else
            {
                loc = 0;
                etxtUpload.setVisibility(View.VISIBLE);
            }

        }

        btnselectpic.setOnClickListener(this);
        uploadButton.setOnClickListener(this);
        clickCamera.setOnClickListener(this);
        //btnShowLocation.setOnClickListener(this);

        dialog = new ProgressDialog(this);
        dialog.setMessage("Detecting Your Crop");
        dialog.setCancelable(false);





    }

    private int gpstry() {
        gps.GPSTracker2(MainActivity.this);

        // check if GPS enabled
        if(gps.canGetLocation()){

            latitude = gps.getLatitude();
            longitude = gps.getLongitude();

            // \n is for new line
            //Toast.makeText(getApplicationContext(), "Your Location is - \nLat: "
             //       + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();
            return 1;
        }else{
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
            //Toast.makeText(getApplicationContext(), "Location Not Allowed", Toast.LENGTH_LONG).show();
        }
        return 0;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.button_selectpic:
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, Utils.REQCODE);
                break;

            case R.id.button_clickcamera:
                opencamera();
                break;

            case R.id.uploadButton:
                if(!def || !isNetworkAvailable())
                {
                    Runtime.getRuntime().gc();
                    //Runtime.getRuntime().freeMemory();

                    Toast.makeText(this, "Please select or capture an image first or Check for your internet connection", Toast.LENGTH_SHORT).show();
                    break;
                }


                dialog.show();

                if(encodedImage==null) {
                    image = ((BitmapDrawable) imageview.getDrawable()).getBitmap();
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    image.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream);

                    encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);

                    byteArrayOutputStream = null;
                }
                jsonObject = new JSONObject();
                //Toast.makeText(this,">"+encodedImage.substring(0,6)+"<",Toast.LENGTH_LONG).show();
                try {
                    jsonObject.put(Utils.imageName, etxtUpload.getText().toString().trim());
                    //Log.e("Image name", etxtUpload.getText().toString().trim());
                    jsonObject.put(Utils.image, encodedImage);
                } catch (JSONException e) {
                   /// Log.e("JSONObject Here", e.toString());
                }
                final Intent result = new Intent(getApplicationContext(),ResultActivity.class);

                if(jsonObjectRequest == null) {
                    jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, Utils.urlUpload, jsonObject,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject jsonObj) {

                                    //Log.e("Message from server", jsonObject.toString());
                                    dialog.dismiss();
                                    //messageText.setText("Image Uploaded Successfully");
                                    Toast.makeText(getApplication(), "Image Detected Successfully", Toast.LENGTH_SHORT).show();

                                    result.putExtra("information",jsonObj.toString());
                                    image.recycle();


                                    ((BitmapDrawable) imageview.getDrawable()).getBitmap().recycle();


                                    encodedImage = null;
                                    jsonObject=null;

                                    Runtime.getRuntime().gc();
                                    startActivity(result);

                                    //Toast.makeText(getApplication(),"fgdfg",Toast.LENGTH_LONG).show();
                                    finish();
                                    Runtime.getRuntime().gc();

                                    //Toast.makeText(getApplication(), "After Finish", Toast.LENGTH_SHORT).show();
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                            Runtime.getRuntime().gc();
                            image.recycle();
                            //Toast.makeText(getApplicationContext(), ">" + encodedImage.substring(0, 6) + "3<", Toast.LENGTH_LONG).show();
                            Toast.makeText(getApplication(), "Network Error, Please Try Again Later", Toast.LENGTH_SHORT).show();

                            dialog.dismiss();

                        }

                    });


                    jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(5000,
                          DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                }
                Volley.newRequestQueue(getApplicationContext()).add(jsonObjectRequest);
                //jsonObjectRequest = null;
                Runtime.getRuntime().gc();
                break;
        }
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void opencamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex)
            {
                ex.printStackTrace();
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "cpd.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }

    }

    private File createImageFile() throws IOException{
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){

        if (requestCode == Utils.REQCODE && resultCode == RESULT_OK && data != null) {

            //Uri selectedImageUri = data.getData();
            Runtime.getRuntime().gc();

            imageview.setImageURI(data.getData());
            encodedImage = null;
            //imageview.setImageDrawable(null);
            //System.gc();
            //imageview.setVisibility(View.GONE);
            def = true;

         }
        else if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK)
        {
            Runtime.getRuntime().gc();
            imageview.setImageURI(Uri.parse(mCurrentPhotoPath));
            encodedImage = null;
            def = true;

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Toast.makeText(this, "Thanks for granting Permission", Toast.LENGTH_SHORT).show();
            }

            else {
                Toast.makeText(this, "The app wont work properly, Please grant permission", Toast.LENGTH_SHORT).show();
            }

            /*if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Thanks for granting Permission", Toast.LENGTH_SHORT).show();
            }else {
                Toast.makeText(this, "The app wont work properly", Toast.LENGTH_SHORT).show();
            }*/
        }
    }

}

