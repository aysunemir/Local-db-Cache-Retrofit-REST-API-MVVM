package com.codingwithmitch.foodrecipes.repositories;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.codingwithmitch.foodrecipes.AppExecutors;
import com.codingwithmitch.foodrecipes.models.Recipe;
import com.codingwithmitch.foodrecipes.persistence.RecipeDao;
import com.codingwithmitch.foodrecipes.persistence.RecipeDatabase;
import com.codingwithmitch.foodrecipes.requests.ServiceGenerator;
import com.codingwithmitch.foodrecipes.requests.responses.ApiResponse;
import com.codingwithmitch.foodrecipes.requests.responses.RecipeSearchResponse;
import com.codingwithmitch.foodrecipes.util.NetworkBoundResources;
import com.codingwithmitch.foodrecipes.util.Resource;

import java.util.List;

public class RecipeRepository {

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
}
