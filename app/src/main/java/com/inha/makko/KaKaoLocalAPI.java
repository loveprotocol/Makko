package com.inha.makko;

import com.inha.makko.model.KakaoCoord2AddressResponse;


import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface KaKaoLocalAPI {
    @Headers("Authorization: KakaoAK 4ca0af2308d4ad179f7c56a846a6211e")
    @GET("/v2/local/geo/coord2address.json")
    Call<KakaoCoord2AddressResponse> coord2address(
            @Query("x") double x,
            @Query("y") double y);
}
