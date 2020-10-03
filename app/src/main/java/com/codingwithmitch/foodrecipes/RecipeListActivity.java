package com.codingwithmitch.foodrecipes;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.codingwithmitch.foodrecipes.adapters.OnRecipeListener;
import com.codingwithmitch.foodrecipes.adapters.RecipeRecyclerAdapter;
import com.codingwithmitch.foodrecipes.util.VerticalSpacingItemDecorator;
import com.codingwithmitch.foodrecipes.viewmodels.RecipeListViewModel;

import static com.codingwithmitch.foodrecipes.viewmodels.RecipeListViewModel.QUERY_EXHAUSTED;


public class RecipeListActivity extends BaseActivity implements OnRecipeListener {

    private static final String TAG = "RecipeListActivity";

    private RecipeListViewModel mRecipeListViewModel;
    private RecyclerView mRecyclerView;
    private RecipeRecyclerAdapter mAdapter;
    private SearchView mSearchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_list);
        mRecyclerView = findViewById(R.id.recipe_list);
        mSearchView = findViewById(R.id.search_view);

        mRecipeListViewModel = new ViewModelProvider(this).get(RecipeListViewModel.class);

        initRecyclerView();
        initSearchView();
        setSupportActionBar(findViewById(R.id.toolbar));
        subscribeObservers();
    }

    private void subscribeObservers() {
        mRecipeListViewModel.getRecipes().observe(this, listResource -> {
            if (listResource != null) {
                Log.d(TAG, "onChanged: status: " + listResource.status);

                if (listResource.data != null) {
                    switch (listResource.status) {
                        case LOADING:
                            if (mRecipeListViewModel.getPageNumber() > 1) {
                                mAdapter.displayLoading();
                            } else {
                                mAdapter.displayOnlyLoading();
                            }
                            break;
                        case SUCCESS:
                            Log.d(TAG, "onChanged: cache has ben refreshed.");
                            Log.d(TAG, "onChanged: status: SUCCESS, #recipes: " +
                                    listResource.data.size());
                            mAdapter.setRecipes(listResource.data);
                            break;
                        case ERROR:
                            Log.d(TAG, "onChanged: cannot refresh the cache.");
                            Log.d(TAG, "onChanged: ERROR message: " + listResource.message);
                            Log.d(TAG, "onChanged: status: ERROR, #recipes: " +
                                    listResource.data.size());
                            mAdapter.hideLoading();
                            mAdapter.setRecipes(listResource.data);
                            Toast.makeText(this, listResource.message, Toast.LENGTH_SHORT).show();

                            if (listResource.message.equals(QUERY_EXHAUSTED)) {
                                mAdapter.setQueryExhausted();
                            }
                            break;
                    }
                    mAdapter.setRecipes(listResource.data);
                }
            }
        });
        mRecipeListViewModel.getViewState().observe(this, viewState -> {
            if (viewState != null) {
                switch (viewState) {
                    case RECIPES:
                        // recipes will show automatically from another observer
                        break;
                    case CATEGORIES:
                        displaySearchCategories();
                        break;
                }
            }
        });
    }

    private RequestManager initGlide() {
        RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.white_bg)
                .error(R.drawable.white_bg);
        return Glide.with(this)
                    .setDefaultRequestOptions(options);
    }

    private void displaySearchCategories() {
        mAdapter.displaySearchCategories();
    }

    private void searchRecipesApi(String query) {
        mRecyclerView.smoothScrollToPosition(0);
        mRecipeListViewModel.searchRecipesApi(query, 1);
        mSearchView.clearFocus();
    }

    private void initRecyclerView() {
        mAdapter = new RecipeRecyclerAdapter(this, initGlide());
        VerticalSpacingItemDecorator itemDecorator = new VerticalSpacingItemDecorator(30);
        mRecyclerView.addItemDecoration(itemDecorator);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (!mRecyclerView.canScrollVertically(1)
                        && mRecipeListViewModel.getViewState().getValue()
                        == RecipeListViewModel.ViewState.RECIPES) {
                    mRecipeListViewModel.searchNextPage();
                }
            }
        });
        mRecyclerView.setAdapter(mAdapter);
    }

    private void initSearchView() {
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                searchRecipesApi(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

    @Override
    public void onRecipeClick(int position) {
        Intent intent = new Intent(this, RecipeActivity.class);
        intent.putExtra("recipe", mAdapter.getSelectedRecipe(position));
        startActivity(intent);
    }

    @Override
    public void onCategoryClick(String category) {
        searchRecipesApi(category);
    }

}
