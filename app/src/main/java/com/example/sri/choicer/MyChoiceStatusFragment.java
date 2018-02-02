package com.example.sri.choicer;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.sri.choicer.models.Choice;
import com.example.sri.choicer.models.ServerRequest;
import com.example.sri.choicer.models.ServerResponse;

import org.w3c.dom.Text;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Sri on 11/3/2017.
 * This class shows the current status of the user's choices and allows the user to end his choice.
 */

public class MyChoiceStatusFragment extends Fragment implements View.OnClickListener{

    ImageView imageView, imageView2;
    TextView tv1, tv2, winnerTV1, winnerTV2, voteCount1, voteCount2;
    Button end;

    private SharedPreferences pref;
    private char[] name1, name2;
    private String imgName1,imgName2;
    private ProgressBar progressBar;
    private int index;
    private int id;

    private String win = "WINNER!!";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_my_choice_status, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view) {
        pref = getActivity().getPreferences(0);

        imageView = (ImageView)view.findViewById(R.id.imageView);
        imageView2 = (ImageView)view.findViewById(R.id.imageView2);
        tv1 = (TextView)view.findViewById(R.id.textView2);
        tv2 = (TextView)view.findViewById(R.id.textView3);
        winnerTV1 = (TextView)view.findViewById(R.id.winner_text1);
        winnerTV2 = (TextView)view.findViewById(R.id.winner_text2);
        voteCount1 = (TextView)view.findViewById(R.id.vote_count1);
        voteCount2 = (TextView)view.findViewById(R.id.vote_count2);
        end = (Button)view.findViewById(R.id.end_button);
        progressBar = (ProgressBar)view.findViewById(R.id.progressBar);

        end.setOnClickListener(this);

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
        final CountDownLatch latch = new CountDownLatch(2);

        new Thread(new Runnable() {
            @Override
            public void run() {
                getImage(imgName1,1);
                latch.countDown();
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                getImage(imgName2,2);
                latch.countDown();
            }
        }).start();
        try{
            latch.await();
            getVoteStatus(view);
        }
        catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    //Method to retrieve image from server and load it into ImageView. Uses AsyncTask for background process
    private void getImage(final String imgName, final int ch){
        class GetImage extends AsyncTask<String,Void,Bitmap> {

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
        end.setEnabled(false);
        onEnd();

    }

    //Method to fetch current vote status of the user's choice
    private void getVoteStatus(final View view) {
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        client.addInterceptor(loggingInterceptor);

        final String email = pref.getString("email","");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(client.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RequestInterface requestInterface = retrofit.create(RequestInterface.class);

        Choice choice = new Choice();
        choice.setEmail(email);
        choice.setId(id);

        ServerRequest request = new ServerRequest();
        request.setOperation(Constants.VOTE_STATUS);
        request.setChoice(choice);
        Call<List<Choice>> call = requestInterface.getChoices(request);

        call.enqueue(new Callback<List<Choice>>() {
            @Override
            public void onResponse(Call<List<Choice>> call, Response<List<Choice>> response) {

                List<Choice> choiceList = response.body();

                int vote1 = 0;
                int vote2 = 0;
                int status = 0;
                for (int i = 0; i < choiceList.size(); i++) {
                    vote1 = choiceList.get(i).getVoteCount1();
                    vote2 = choiceList.get(i).getVoteCount2();
                    status = choiceList.get(i).getStatus();
                }
                if(status == 1){
                    onClick(view);
                }
                voteCount1.setText(String.format("%d",vote1));
                voteCount2.setText(String.format("%d",vote2));

            }

            @Override
            public void onFailure(Call<List<Choice>> call, Throwable t) {

                Log.d(Constants.TAG, "failed");
                Snackbar.make(getView(), t.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();

            }
        });

    }

    //Method to end the user's choice. Removes the choice from the ListView of other users
    private void onEnd() {
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        client.addInterceptor(loggingInterceptor);

        final String email = pref.getString("email","");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(client.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RequestInterface requestInterface = retrofit.create(RequestInterface.class);

        Choice choice = new Choice();
        choice.setEmail(email);
        choice.setId(id);

        ServerRequest request = new ServerRequest();
        request.setOperation(Constants.END_CHOICE);
        request.setChoice(choice);
        Call<List<Choice>> call = requestInterface.getChoices(request);

        call.enqueue(new Callback<List<Choice>>() {
            @Override
            public void onResponse(Call<List<Choice>> call, Response<List<Choice>> response) {

                List<Choice> choiceList = response.body();

                int vote1 = 0;
                int vote2 = 0;
                for (int i = 0; i < choiceList.size(); i++) {
                    vote1 = choiceList.get(i).getVoteCount1();
                    vote2 = choiceList.get(i).getVoteCount2();
                }
                voteCount1.setText(String.format("%d",vote1));
                voteCount2.setText(String.format("%d",vote2));
                displayWinner(vote1, vote2);
            }

            @Override
            public void onFailure(Call<List<Choice>> call, Throwable t) {

                Log.d(Constants.TAG, "failed");
                Snackbar.make(getView(), t.getLocalizedMessage(), Snackbar.LENGTH_LONG).show();

            }
        });

    }

    //Method to display the winner of the choice based on the votes
    private void displayWinner(int vote1, int vote2){
        RotateAnimation rotateAnimation = new RotateAnimation(0, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);

        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(500);
        rotateAnimation.setRepeatCount(Animation.INFINITE);

        Animation anim = new AlphaAnimation(0.0f,1.0f);
        anim.setDuration(50);
        anim.setStartOffset(20);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);

        Snackbar.make(getView(),"Choice Ended", Snackbar.LENGTH_LONG).show();

        if(vote1>vote2){
            winnerTV1.setText(win);
            winnerTV1.startAnimation(anim);
            imageView.startAnimation(rotateAnimation);
        }
        else {
            winnerTV2.setText(win);
            winnerTV2.startAnimation(anim);
            imageView2.startAnimation(rotateAnimation);
        }
    }
}
