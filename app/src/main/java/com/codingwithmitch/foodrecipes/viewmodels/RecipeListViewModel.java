package com.codingwithmitch.foodrecipes.viewmodels;


import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.codingwithmitch.foodrecipes.models.Recipe;
import com.codingwithmitch.foodrecipes.repositories.RecipeRepository;
import com.codingwithmitch.foodrecipes.util.Resource;

import java.util.List;

public class RecipeListViewModel extends AndroidViewModel {

    private static final String TAG = "RecipeListViewModel";

    private MutableLiveData<ViewState> viewState;
    private MediatorLiveData<Resource<List<Recipe>>> recipes = new MediatorLiveData<>();

    private RecipeRepository recipeRepository;

    public RecipeListViewModel(@NonNull Application application) {
        super(application);
        recipeRepository = RecipeRepository.getInstance(application);
        init();
    }

    private void init() {
        if (viewState == null) {
            viewState = new MutableLiveData<>();
            viewState.setValue(ViewState.CATEGORIES);
        }
    }

    public LiveData<ViewState> getViewState() {
        return viewState;
    }

    public LiveData<Resource<List<Recipe>>> getRecipes() {
        return recipes;
    }

    public void searchRecipesApi(String query, int pageNumber) {
        final LiveData<Resource<List<Recipe>>> repositorySource = recipeRepository
                .searchRecipeApi(query, pageNumber);
        recipes.addSource(repositorySource, listResource -> recipes.setValue(listResource));
    }

    public enum ViewState {CATEGORIES, RECIPES}
}















