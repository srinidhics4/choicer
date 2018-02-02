package com.example.sri.choicer;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.internal.SnackbarContentLayout;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.example.sri.choicer.models.Choice;
import com.example.sri.choicer.models.ServerRequest;
import com.example.sri.choicer.models.ServerResponse;
import com.example.sri.choicer.models.User;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Sri on 10/28/2017.
 * Choice Fragment where User can post new choices
 */

public class ChoiceFragment extends Fragment implements View.OnClickListener{

    ImageButton imgbutt1, imgbutt2;
    EditText et1, et2;
    Button post_button;
    private String uploadServerUri = null;
    private String path1 = null;
    private String path2 = null;
    private String imgName1 = " ", imgName2 = " ";
    private ProgressBar uploadProgress;
    private int serverResponseCode = 1;

    private SharedPreferences pref;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_choice,container,false);
        uploadServerUri = "http://192.168.225.133/MAD/uploadToServer.php";
        initViews(view);
        return view;
    }

    private void initViews(View view){
        pref = getActivity().getPreferences(0);

        imgbutt1 = (ImageButton)view.findViewById(R.id.imageButton);
        imgbutt2 = (ImageButton)view.findViewById(R.id.imageButton2);

        et1 = (EditText)view.findViewById(R.id.editText);
        et2 = (EditText)view.findViewById(R.id.editText2);

        post_button = (Button)view.findViewById(R.id.button);
        uploadProgress = (ProgressBar)view.findViewById(R.id.uploadProgress);

        imgbutt1.setOnClickListener(this);
        imgbutt2.setOnClickListener(this);
        post_button.setOnClickListener(this);
    }


    @Override
    public void onClick(View v){
        switch(v.getId()) {
            case R.id.imageButton:
                startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), 1);
                break;

            case R.id.imageButton2:
                startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI), 2);
                break;

            case R.id.button:
                onPost(path1, path2);
                break;

            default:
                break;
        }
    }

    //Method to load image from storage and set to image button
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(requestCode > 0 && resultCode == Activity.RESULT_OK){
            Uri selectedImage = data.getData();
            Bitmap bitmap = null;
            try{
                bitmap = MediaStore.Images.Media.getBitmap(this.getActivity().getContentResolver(), selectedImage);
                Drawable drawable = new BitmapDrawable(this.getResources(),bitmap);
                if(requestCode == 1) {
                    imgbutt1.setImageResource(android.R.color.transparent);
                    path1 = getPath(selectedImage);
                    imgbutt1.setBackground(drawable);

                }
                else if(requestCode == 2) {
                    imgbutt2.setImageResource(android.R.color.transparent);
                    path2 = getPath(selectedImage);
                    imgbutt2.setBackground(drawable);
                }
            }
            catch(FileNotFoundException e){
                e.printStackTrace();
            }
            catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    //Method to post new choice to API, where its inserted into database and files are uploaded to server.
    public void onPost(final String imagePath1, final String imagePath2) {

        //CountDownLatch to wait for concurrent threads to finish execution
        final CountDownLatch latch = new CountDownLatch(2);
        final String title = et1.getText().toString() + "V/S" + et2.getText().toString();
        if(imagePath1 == null || imagePath2 == null) {
            Snackbar.make(getView(), "Fields are empty!", Snackbar.LENGTH_LONG).show();
        }
        else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    uploadFile(imagePath1, 1);
                    latch.countDown();
                }
            }).start();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    uploadFile(imagePath2, 2);
                    latch.countDown();
                }
            }).start();

            try {
                latch.await();
                insertChoice(title, imgName1, imgName2);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
    }

    //Method to get image file path from storage
    public String getPath(Uri uri){
        String res = null;
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = this.getActivity().getContentResolver().query(uri, projection, null, null, null);
        if(cursor.moveToFirst()){
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    //Method to upload files to server.
    public synchronized int uploadFile(String sourceFileUri, int ch){
        final String fileName = sourceFileUri;

        //For unique file names, use calendar instance
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmssSSS", Locale.US);
        String fName = dateFormat.format(calendar.getTime());
        if(ch == 1)
            imgName1 = fName;
        else if(ch == 2)
            imgName2 = fName;
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1024*1024;
        File sourceFile = new File(sourceFileUri);
        if(!sourceFile.isFile())
        {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    uploadProgress.setVisibility(View.INVISIBLE);
                    Log.e("uploadFile", "Source file does not exist:" + fileName);
                    Snackbar.make(getView(), "Source file does not exist", Snackbar.LENGTH_LONG).show();
                }
            });
            return 0;
        }
        else
        {
            try{
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        uploadProgress.setVisibility(View.VISIBLE);
                    }
                });
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(uploadServerUri);

                conn = (HttpURLConnection)url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection","Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
                conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens+boundary+lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""+fName+".jpg"+"\""+lineEnd);

                dos.writeBytes(lineEnd);

                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while(bytesRead > 0){
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens+boundary+lineEnd);

                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.i("uploadFile", "HTTP Response is:"+serverResponseMessage+":"+serverResponseCode);

                if(serverResponseCode == 200){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            uploadProgress.setVisibility(View.INVISIBLE);
                            Snackbar.make(getView(),"File Upload Complete",Snackbar.LENGTH_LONG).show();
                        }
                    });

                }

                fileInputStream.close();
                dos.flush();
                dos.close();
            }
            catch(MalformedURLException ex){
                ex.printStackTrace();
                Log.e("Upload file to server","error:"+ex.getMessage(),ex);
            }
            catch(Exception e){
                e.printStackTrace();
                Log.e("Upload file to server","error:"+e.getMessage(),e);
            }
            return serverResponseCode;

        }

    }

    //Method to invoke retrofit 2 http POST call to pass JSON object to server
    private void insertChoice(String title, String imgName1, String imgName2){

        //OkHttpClient to log Http calls

        OkHttpClient.Builder client = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        client.addInterceptor(loggingInterceptor);

        final String email = pref.getString("email","");

        //Building a retrofit object and adding GsonConverter
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(client.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RequestInterface requestInterface = retrofit.create(RequestInterface.class);

        Choice choice = new Choice();
        choice.setEmail(email);
        choice.setTitle(title);
        choice.setImgLink1(imgName1);
        choice.setImgLink2(imgName2);
        ServerRequest request = new ServerRequest();
        request.setOperation(Constants.NEW_CHOICE);
        request.setChoice(choice);
        Call<ServerResponse> response = requestInterface.operation(request);

        response.enqueue(new Callback<ServerResponse>() {
            @Override
            public void onResponse(Call<ServerResponse> call, retrofit2.Response<ServerResponse> response) {

                ServerResponse resp = response.body();
                Snackbar.make(getView(), resp.getMessage(), Snackbar.LENGTH_LONG).show();
                goToProfile();
            }

            @Override
            public void onFailure(Call<ServerResponse> call, Throwable t) {

                Log.d(Constants.TAG,"failed");
                Snackbar.make(getView(), t.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();

            }
        });
    }
    private void goToProfile(){

        Fragment profile = new ProfileFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_frame,profile);
        ft.commit();
    }

}
