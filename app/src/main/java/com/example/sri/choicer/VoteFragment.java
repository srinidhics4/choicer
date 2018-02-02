package com.example.sri.choicer;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.sri.choicer.models.Choice;
import com.example.sri.choicer.models.ServerRequest;
import com.example.sri.choicer.models.ServerResponse;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Sri on 10/31/2017.
 * VoteFragment class enables users to vote for other user's choices.
 */

public class VoteFragment extends Fragment implements View.OnClickListener{

    ImageView imageView, imageView2;
    TextView tv1, tv2;
    Button vote1, vote2;
    char[] name1, name2;
    String imgName1,imgName2;
    ProgressBar progressBar;
    int index;
    int id;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_vote, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view) {
        imageView = (ImageView)view.findViewById(R.id.imageView);
        imageView2 = (ImageView)view.findViewById(R.id.imageView2);
        tv1 = (TextView)view.findViewById(R.id.textView2);
        tv2 = (TextView)view.findViewById(R.id.textView3);
        vote1 = (Button)view.findViewById(R.id.vote_button);
        vote2 = (Button)view.findViewById(R.id.vote_button2);
        progressBar = (ProgressBar)view.findViewById(R.id.progressBar);

        vote1.setOnClickListener(this);
        vote2.setOnClickListener(this);

        Bundle bundle = this.getArguments();
        if(bundle!=null){
            String title = bundle.getString("title","");
            imgName1 = bundle.getString("image_name_1","");
            imgName2 = bundle.getString("image_name_2","");
            id = bundle.getInt("id",0);
            char[] names = title.toCharArray();
            for(int i=0; i<title.length(); i++)
                if(names[i] == '/') {
                    index = i;
                    break;
                }
            name1 = new char[index-1];
            name2 = new char[names.length-index-1];
            System.arraycopy(names, 0, name1,0, index-1);
            System.arraycopy(names, index+2, name2, 0, names.length-index-2);
            String s1 = new String(name1);
            String s2 = new String(name2);
            tv1.setText(s1);
            tv2.setText(s2);
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                getImage(imgName1,1);
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                getImage(imgName2,2);
            }
        }).start();

    }

    //Method to retrieve image from server and load it into ImageView. Uses AsyncTask for background process
    private void getImage(final String imgName, final int ch){
        class GetImage extends AsyncTask<String,Void,Bitmap>{

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            protected void onPostExecute(Bitmap b){
                super.onPostExecute(b);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.INVISIBLE);
                    }
                });
                if(ch==1) {
                    imageView.setImageBitmap(null);
                    imageView.setImageBitmap(b);
                }
                else {
                    imageView2.setImageBitmap(null);
                    imageView2.setImageBitmap(b);
                }
            }

            @Override
            protected Bitmap doInBackground(String...params){
                String add = "http://192.168.225.133/MAD/uploads/"+imgName+".jpg";
                URL url = null;
                Bitmap image = null;
                try{
                    url = new URL(add);
                    image = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                }
                catch(MalformedURLException e){
                    e.printStackTrace();
                }
                catch (IOException e){
                    e.printStackTrace();
                }
                return image;
            }
        }
        GetImage gi = new GetImage();
        gi.execute();

    }
    public void onClick(View view)
    {
        switch(view.getId()){
            case R.id.vote_button:
                vote2.setEnabled(false);
                onVote(1);
                break;
            case R.id.vote_button2:
                vote1.setEnabled(false);
                onVote(2);
                break;
            default:
                break;
        }
    }

    //Method to vote for a choice. Updates vote count in database
    private void onVote(int ch) {
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        client.addInterceptor(loggingInterceptor);


        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(client.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RequestInterface requestInterface = retrofit.create(RequestInterface.class);

        Choice choice = new Choice();
        choice.setId(id);
        choice.setCh(ch);

        ServerRequest request = new ServerRequest();
        request.setOperation(Constants.VOTE_CHOICE);
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

                Log.d(Constants.TAG, "failed");
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
