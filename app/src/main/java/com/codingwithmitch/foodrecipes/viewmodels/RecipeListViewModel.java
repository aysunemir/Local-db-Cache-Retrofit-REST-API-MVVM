package com.codingwithmitch.foodrecipes.viewmodels;

import android.app.Application;
import android.util.Log;

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

    public static final String QUERY_EXHAUSTED = "No more results";
    private static final String TAG = "RecipeListViewModel";
    private MutableLiveData<ViewState> viewState;
    private MediatorLiveData<Resource<List<Recipe>>> recipes = new MediatorLiveData<>();
    private RecipeRepository recipeRepository;

    // query extras
    private boolean isQueryExhausted;
    private boolean isPerformingQuery;
    private int pageNumber;
    private String query;

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

    public int getPageNumber() {
        return pageNumber;
    }

    public void searchRecipesApi(String query, int pageNumber) {
        if (!isPerformingQuery) {
            if (pageNumber == 0) {
                pageNumber = 1;
            }
            this.pageNumber = pageNumber;
            this.query = query;
            isQueryExhausted = false;
            executeSearch();
        }
    }

    private void executeSearch() {
        isPerformingQuery = true;
        viewState.setValue(ViewState.RECIPES);
        final LiveData<Resource<List<Recipe>>> repositorySource = recipeRepository
                .searchRecipeApi(query, pageNumber);

        recipes.addSource(repositorySource, listResource -> {
            if (listResource != null) {
                recipes.setValue(listResource);
                if (listResource.status == Resource.Status.SUCCESS) {
                    isPerformingQuery = false;
                    if (listResource.data != null && listResource.data.size() == 0) {
                        Log.d(TAG, "executeSearch: query is exhausted...");
                        recipes.setValue(new Resource<>(Resource.Status.ERROR, listResource.data,
                                QUERY_EXHAUSTED));
                    }
                    recipes.removeSource(repositorySource);
                } else if (listResource.status == Resource.Status.ERROR) {
                    isPerformingQuery = false;
                    recipes.removeSource(repositorySource);
                }
            } else {
                recipes.removeSource(repositorySource);
            }
        });
    }

    public enum ViewState {CATEGORIES, RECIPES}
}















