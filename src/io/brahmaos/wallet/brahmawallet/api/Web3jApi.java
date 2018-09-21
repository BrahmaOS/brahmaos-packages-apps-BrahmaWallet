package io.brahmaos.wallet.brahmawallet.api;

import java.util.List;
import java.util.Map;

import retrofit2.http.POST;
import retrofit2.http.Body;
import rx.Observable;

/**
 * web3j api
 */
public interface Web3jApi {

    @POST("/Gy3Csyt4bzKIGsctm3g0")
    Observable<Web3jRespResult> callEthRequest(@Body Map<String, Object> body);
}
