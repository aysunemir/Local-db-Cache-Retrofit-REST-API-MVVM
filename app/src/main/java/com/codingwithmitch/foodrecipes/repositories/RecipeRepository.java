package com.codingwithmitch.foodrecipes.repositories;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.codingwithmitch.foodrecipes.AppExecutors;
import com.codingwithmitch.foodrecipes.models.Recipe;
import com.codingwithmitch.foodrecipes.persistence.RecipeDao;
import com.codingwithmitch.foodrecipes.persistence.RecipeDatabase;
import com.codingwithmitch.foodrecipes.requests.ServiceGenerator;
import com.codingwithmitch.foodrecipes.requests.responses.ApiResponse;
import com.codingwithmitch.foodrecipes.requests.responses.RecipeResponse;
import com.codingwithmitch.foodrecipes.requests.responses.RecipeSearchResponse;
import com.codingwithmitch.foodrecipes.util.Constants;
import com.codingwithmitch.foodrecipes.util.NetworkBoundResources;
import com.codingwithmitch.foodrecipes.util.Resource;

import java.util.List;

public class RecipeRepository {

    private static final String TAG = "RecipeRepository";

    private static RecipeRepository instance;
    private RecipeDao recipeDao;

    private RecipeRepository(Context context) {
        recipeDao = RecipeDatabase.getInstance(context).getRecipeDao();
    }

    public static RecipeRepository getInstance(Context context) {
        if (instance == null) {
            instance = new RecipeRepository(context);
        }
        return instance;
    }

    public LiveData<Resource<List<Recipe>>> searchRecipeApi(final String query,
                                                            final int pageNumber) {
        return new NetworkBoundResources<List<Recipe>, RecipeSearchResponse>(
                AppExecutors.getInstance()) {

            @Override
            protected void saveCallResult(@NonNull RecipeSearchResponse item) {
                if (item.getRecipes() != null) {
                    Recipe[] recipes = new Recipe[item.getRecipes().size()];

                    int index = 0;
                    for (long rowId : recipeDao.insertRecipes(item.getRecipes().toArray(recipes))) {
                        if (rowId == -1) {
                            Log.d(TAG, "saveCallResult: CONFLICT... This recipe is already " +
                                    "in the cache");
                            Recipe recipe = recipes[index];
                            recipeDao.updateRecipe(recipe.getRecipe_id(), recipe.getTitle(),
                                    recipe.getPublisher(), recipe.getImage_url(),
                                    recipe.getSocial_rank());
                        }
                        index++;
                    }
                }
            }

            @Override
            protected boolean shouldFetch(@Nullable List<Recipe> data) {
                return true;
            }

            @NonNull
            @Override
            protected LiveData<List<Recipe>> loadFromDb() {
                return recipeDao.searchRecipes(query, pageNumber);
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<RecipeSearchResponse>> createCall() {
                return ServiceGenerator.getRecipeApi()
                                       .searchRecipe(query, String.valueOf(pageNumber));
            }
        }.getAsLiveData();
    }

    public LiveData<Resource<Recipe>> searchRecipesApi(final String recipeId) {
        return new NetworkBoundResources<Recipe, RecipeResponse>(AppExecutors.getInstance()) {

            @Override
            protected void saveCallResult(@NonNull RecipeResponse item) {
                if (item.getRecipe() != null) {
                    item.getRecipe().setTimestamp((int) (System.currentTimeMillis() / 100));
                    recipeDao.insertRecipe(item.getRecipe());
                }
            }

            @Override
            protected boolean shouldFetch(@Nullable Recipe data) {
                Log.d(TAG, "shouldFetch: recipe: " + data.toString());
                int currentTime = (int) (System.currentTimeMillis() / 100);
                Log.d(TAG, "shouldFetch: current time: " + currentTime);
                int lastRefresh = data.getTimestamp();
                Log.d(TAG, "shouldFetch: last refresh: " + lastRefresh);
                Log.d(TAG, "shouldFetch: it's been " + ((currentTime - lastRefresh) / 60 / 60 / 24)
                        + " days since this recipe was refreshed. 30 days must elapsed before "
                        + "refreshing. ");
                Log.d(TAG, "shouldFetch: SHOULD REFRESH RECIPE?: " + (currentTime - lastRefresh
                        >= Constants.RECIPE_REFRESH_TIME));
                return currentTime - lastRefresh >= Constants.RECIPE_REFRESH_TIME;
            }

            @NonNull
            @Override
            protected LiveData<Recipe> loadFromDb() {
                return recipeDao.getRecipe(recipeId);
            }

            @NonNull
            @Override
            protected LiveData<ApiResponse<RecipeResponse>> createCall() {
                return ServiceGenerator.getRecipeApi().getRecipe(recipeId);
            }
        }.getAsLiveData();
    }
}
