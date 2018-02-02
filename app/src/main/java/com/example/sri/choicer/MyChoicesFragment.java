package com.example.sri.choicer;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.example.sri.choicer.models.Choice;
import com.example.sri.choicer.models.ServerRequest;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by Sri on 11/3/2017.
 * MyChoicesFragment class displays all the choices of the current user in a custom ListView
 */

public class MyChoicesFragment extends Fragment{

    private ListView listView;
    private String[] myChoices;
    private String[] imgname1,imgname2;
    private int[] ids;
    private ProgressBar retrieveProgress;
    private SharedPreferences pref;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_my_choices, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view){
        pref = getActivity().getPreferences(0);

        listView = (ListView) view.findViewById(R.id.listView);
        retrieveProgress = (ProgressBar)view.findViewById(R.id.retrieveProgress);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String myChoice = myChoices[i];
                String image1 = imgname1[i];
                String image2 = imgname2[i];
                int id = ids[i];
                Fragment status = new MyChoiceStatusFragment();
                Bundle bundle = new Bundle();
                bundle.putString("title",myChoice);
                bundle.putString("image_name_1",image1);
                bundle.putString("image_name_2",image2);
                bundle.putInt("id",id);
                status.setArguments(bundle);
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_frame,status);
                ft.addToBackStack("");
                ft.commit();
            }
        });
        getMyChoices();
    }

    //Method to fetch records from database and load it into listview
    private void getMyChoices(){
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        client.addInterceptor(loggingInterceptor);

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                retrieveProgress.setVisibility(View.VISIBLE);
            }
        });

        final String email = pref.getString("email","");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(client.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        RequestInterface requestInterface = retrofit.create(RequestInterface.class);

        Choice choice = new Choice();
        choice.setEmail(email);

        ServerRequest request = new ServerRequest();
        request.setOperation(Constants.MY_CHOICES);
        request.setChoice(choice);

        Call<List<Choice>> call = requestInterface.getChoices(request);

        call.enqueue(new Callback<List<Choice>>() {
            @Override
            public void onResponse(Call<List<Choice>> call, Response<List<Choice>> response) {
                List<Choice> choiceList = response.body();

                myChoices = new String[choiceList.size()];
                imgname1 = new String[choiceList.size()];
                imgname2 = new String[choiceList.size()];
                ids = new int[choiceList.size()];

                for (int i = 0; i < choiceList.size(); i++) {
                    myChoices[i] = choiceList.get(i).getTitle();
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
                Snackbar.make(getView(),"Connection Timed-out!!", Snackbar.LENGTH_LONG).show();
            }
        });


    }

}
