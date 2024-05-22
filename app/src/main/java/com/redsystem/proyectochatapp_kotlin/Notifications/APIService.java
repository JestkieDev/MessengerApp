package com.redsystem.proyectochatapp_kotlin.Notifications;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {

    @Headers({
        "Content-Type:application/json",
        "Authorization:key=AAAAFqYz6tA:APA91bEKEaImTUY-KWsPEOo_RGJWb-vT22TV_ZECulRzLTWPCtxT0HXiDQtlKpUrsq6OdLQn8fD1lrgcreVb_qYI1_uDDRbg36sOHySJ1Kx-u6mytuTZWFxDtt36bD66oNvGtJUbdmcC"
    })

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);

}
