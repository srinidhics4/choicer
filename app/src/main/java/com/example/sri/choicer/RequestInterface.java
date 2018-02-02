package com.example.sri.choicer;

import com.example.sri.choicer.models.Choice;
import com.example.sri.choicer.models.ServerRequest;
import com.example.sri.choicer.models.ServerResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * Created by Sri on 10/27/2017.
 * Retrofit 2 RequestInterface defines the Request method along with the end point for the method
 */

public interface RequestInterface {

    //Request method is POST and end point is MAD/
    @POST("MAD/")
    Call<ServerResponse> operation(@Body ServerRequest request);
    @POST("MAD/")
    Call<List<Choice>> getChoices(@Body ServerRequest request);

}