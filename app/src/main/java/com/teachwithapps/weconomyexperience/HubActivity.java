package com.teachwithapps.weconomyexperience;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.teachwithapps.weconomyexperience.firebase.FireAuthHelper;
import com.teachwithapps.weconomyexperience.firebase.util.Returnable;
import com.teachwithapps.weconomyexperience.firebase.util.ReturnableChange;
import com.teachwithapps.weconomyexperience.model.GameData;
import com.teachwithapps.weconomyexperience.view.GameRecyclerAdapter;

import org.parceler.Parcels;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.teachwithapps.weconomyexperience.firebase.FireAuthHelper.RC_SIGN_IN;

/**
 * Created by mint on 26-7-17.
 */

public class HubActivity extends AppCompatActivity {

    private static final String TAG = HubActivity.class.getName();

    //views
    @BindView(R.id.recyclerview_game)
    protected RecyclerView gameRecyclerView;

    //hub game attributes
    private Map<String, GameData> gameDataMap;

    private GameRecyclerAdapter.OnClickListener clickGameListener = new GameRecyclerAdapter.OnClickListener() {
        @Override
        public void onClick(GameData gameData) {
            clickHubGame(gameData);
        }
    };

    private GameRecyclerAdapter.OnClickListener clickRemoveGameListener = new GameRecyclerAdapter.OnClickListener() {
        @Override
        public void onClick(GameData gameData) {
            removeHubGame(gameData);
        }
    };

    //firebase attributes
    private FireDatabaseTransactions fireDatabaseTransactions;
    private FireAuthHelper fireAuthHelper;

    private FireAuthHelper.FireAuthCallback fireAuthCallback = new FireAuthHelper.FireAuthCallback() {
        @Override
        public void userReady(FirebaseUser firebaseUser) {
            observeHubGames();
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_hub);

        ButterKnife.bind(this);

        //set up the list of games present in the hub
        gameDataMap = new HashMap<>();
        gameRecyclerView.setAdapter(
                new GameRecyclerAdapter(
                        gameDataMap,
                        clickGameListener,
                        clickRemoveGameListener
                )
        );
        gameRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //set up firebase helper classes
        fireDatabaseTransactions = new FireDatabaseTransactions();
        fireAuthHelper = new FireAuthHelper(this);
        fireAuthHelper.withUser(this, fireAuthCallback);
    }

    /**
     * Needed for firebase to handle login by google account
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "Receiving broadcast request " + requestCode + " Result " + resultCode);

        // Result returned from FireAuthHelper, pass it along
        if (requestCode == RC_SIGN_IN) {
            fireAuthHelper.passActivityResult(data, resultCode);
        }
    }

    /**
     * request list of games in the hub
     */
    private void observeHubGames() {
        fireDatabaseTransactions.observeHubGames(
                new ReturnableChange<String>() {
                    @Override
                    public void onChildAdded(String data) {
                        getGameData(data);
                        Log.d(TAG, "add " + data);
                    }

                    @Override
                    public void onChildChanged(String data) {

                    }

                    @Override
                    public void onChildRemoved(String data) {
                        gameDataMap.remove(data);
                        gameRecyclerView.getAdapter().notifyDataSetChanged();
                        Log.d(TAG, "remove " + data);
                    }

                    @Override
                    public void onChildMoved(String data) {

                    }
                }
        );
    }

    private void getGameData(final String gameKey) {
        fireDatabaseTransactions.getGameData(gameKey, new Returnable<GameData>() {
            @Override
            public void onResult(GameData data) {
                gameDataMap.put(gameKey, data);
                gameRecyclerView.getAdapter().notifyDataSetChanged();
            }
        });
    }

    /**
     * remove game from the hub
     *
     * @param gameData
     */
    private void removeHubGame(GameData gameData) {
        fireDatabaseTransactions.removeHubGame(gameData);
    }

    /**
     * handle when user clicks on a game to join
     *
     * @param gameData
     */
    private void clickHubGame(GameData gameData) {
        Intent intent = new Intent(HubActivity.this, GameActivity.class);
        intent.putExtra(Constants.KEY_GAME_DATA_PARCEL, Parcels.wrap(gameData));
        startActivity(intent);
    }

    /**
     * user wants to make a new game
     */
    @OnClick(R.id.button_start_new_game)
    protected void startNewGame() {
        GameData gameData = new GameData("Test " + gameDataMap.keySet().size(), "default_library");
        fireDatabaseTransactions.registerNewGame(gameData);
    }
}
