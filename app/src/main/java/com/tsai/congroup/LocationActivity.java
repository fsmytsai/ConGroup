package com.tsai.congroup;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;

import java.io.IOException;

import MyMethod.SharedService;
import ViewModel.NearbySearchView;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LocationActivity extends MySharedActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final int REQUEST_LOCATION = 673;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private double Latitude, Longitude;
    private String next_page_token = "";


    private NearbySearchView nearbySearchView;
    private RecyclerView rv_LocationList;
    private LocationListAdapter locationListAdapter;

    private boolean isFirstLoad = true;
    private boolean isLoading = true;
    private boolean isFinishLoad = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        InitView(true, "");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_LOCATION);
        } else {
            mGoogleApiClient.connect();
        }
    }

    @Override
    public Intent getSupportParentActivityIntent() {
        finish();
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mGoogleApiClient.connect();
            } else {
                finish();
//                SharedService.ShowTextToast("您可以輸入地名打卡", this);
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            Latitude = mLastLocation.getLatitude();
            Longitude = mLastLocation.getLongitude();
            nearbySearchView = new NearbySearchView();
            rv_LocationList = (RecyclerView) findViewById(R.id.rv_LocationList);
            NearbySearch();
        } else {
            SharedService.ShowTextToast("偵測不到定位，請確認定位功能已開啟。", this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i("Location : ", "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("Location : ", "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    private void NearbySearch() {
        Request request = new Request.Builder()
                .url(getString(R.string.BackEndPath) + "Api/PostApi/NearbySearch?lat=" + Latitude + "&lng=" + Longitude + "&next_page_token=" + next_page_token)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        SharedService.ShowTextToast("請檢察網路連線", LocationActivity.this);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final int StatusCode = response.code();
                final String ResMsg = response.body().string();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (StatusCode == 200) {
                            //請求完畢
                            isLoading = false;
                            Gson gson = new Gson();
                            NearbySearchView tempView = gson.fromJson(ResMsg, NearbySearchView.class);
                            nearbySearchView.results.addAll(tempView.results);
                            next_page_token = tempView.next_page_token;
                            if (next_page_token == null)
                                isFinishLoad = true;
                            if (isFirstLoad) {
                                isFirstLoad = false;
                                rv_LocationList.setLayoutManager(new LinearLayoutManager(LocationActivity.this, LinearLayoutManager.VERTICAL, false));
                                locationListAdapter = new LocationListAdapter();
                                rv_LocationList.setAdapter(locationListAdapter);
                            } else {
                                locationListAdapter.notifyDataSetChanged();
                            }
                            SharedService.ShowTextToast("資料來源:Google Place Api", LocationActivity.this);
                        } else if (StatusCode == 400) {
                            SharedService.ShowTextToast("正在為您加載地點資料，請稍後。", LocationActivity.this);
                        } else {
                            SharedService.ShowErrorDialog(StatusCode + "", LocationActivity.this);
                        }
                    }
                });
            }
        });
    }


    public class LocationListAdapter extends RecyclerView.Adapter<LocationListAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            View view = LayoutInflater.from(context).inflate(R.layout.location_block, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {

            holder.tv_LocationName.setText(nearbySearchView.results.get(position).name);
            holder.tv_LocationName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getIntent().putExtra("LocationName", nearbySearchView.results.get(position).name);
                    getIntent().putExtra("PlaceId", nearbySearchView.results.get(position).place_id);
                    setResult(RESULT_OK, getIntent());
                    finish();
                }
            });

            //避免重複請求
            if (position > nearbySearchView.results.size() * 0.6 && !isFinishLoad && !isLoading) {
                isLoading = true;
                NearbySearch();
            }
        }

        @Override
        public int getItemCount() {
            return nearbySearchView.results.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            private TextView tv_LocationName;

            public ViewHolder(View itemView) {
                super(itemView);
                tv_LocationName = (TextView) itemView.findViewById(R.id.tv_LocationName);
            }
        }
    }
}
