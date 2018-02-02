package com.example.sri.choicer;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.sri.choicer.models.Choice;
import com.example.sri.choicer.models.ServerRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Sri on 10/31/2017.
 * ViewChoices class displays all the choices from other users in a ListView
 */

public class ViewChoicesFragment extends Fragment{

    ListView listView;
    String[] choices;
    String[] imgname1,imgname2;
    int[] ids;
    ProgressBar retrieveProgress;
    private SharedPreferences pref;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        pref = getActivity().getPreferences(0);
        View view = inflater.inflate(R.layout.fragment_view_choices, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view){
        listView = (ListView) view.findViewById(R.id.listView);
        retrieveProgress = (ProgressBar)view.findViewById(R.id.retrieveProgress);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String choice = choices[i];
                String image1 = imgname1[i];
                String image2 = imgname2[i];
                int id = ids[i];
                Fragment vote = new VoteFragment();
                Bundle bundle = new Bundle();
                bundle.putString("title",choice);
                bundle.putString("image_name_1",image1);
                bundle.putString("image_name_2",image2);
                bundle.putInt("id",id);
                vote.setArguments(bundle);
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_frame,vote);
                ft.addToBackStack("");
                ft.commit();
            }
        });
        getChoices();
    }

    //Method to fetch choices from database and load it into listview
    private void getChoices(){
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        client.addInterceptor(loggingInterceptor);

        String email = pref.getString("email","");

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                retrieveProgress.setVisibility(View.VISIBLE);
            }
        });
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(client.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RequestInterface requestInterface = retrofit.create(RequestInterface.class);
        ServerRequest request = new ServerRequest();
        Choice choice = new Choice();
        choice.setEmail(email);
        request.setOperation(Constants.VIEW_CHOICES);
        request.setChoice(choice);
        Call<List<Choice>> call = requestInterface.getChoices(request);

        call.enqueue(new Callback<List<Choice>>() {
            @Override
            public void onResponse(Call<List<Choice>> call, Response<List<Choice>> response) {
                List<Choice> choiceList = response.body();

                choices = new String[choiceList.size()];
                imgname1 = new String[choiceList.size()];
                imgname2 = new String[choiceList.size()];
                ids = new int[choiceList.size()];

                for (int i = 0; i < choiceList.size(); i++) {
                    choices[i] = choiceList.get(i).getTitle();
                    imgname1[i] = choiceList.get(i).getImgLink1();
                    imgname2[i] = choiceList.get(i).getImgLink2();
                    ids[i] = choiceList.get(i).getId();

                }

                CustomAdapter adapter = new CustomAdapter(choiceList,getActivity().getApplicationContext());
                listView.setAdapter(adapter);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        retrieveProgress.setVisibility(View.INVISIBLE);
                    }
                });

            }

            @Override
            public void onFailure(Call<List<Choice>> call, Throwable t) {
                Snackbar.make(getView(), "Connection Timed-out!!", Snackbar.LENGTH_LONG).show();
            }
        });


    }

}

