package com.raywenderlich.podplay.ui

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager


import com.raywenderlich.podplay.R
import com.raywenderlich.podplay.adapter.PodcastListAdapter
import com.raywenderlich.podplay.databinding.ActivityPodcastBinding
import com.raywenderlich.podplay.repository.ItunesRepo
import com.raywenderlich.podplay.repository.PodcastRepo
import com.raywenderlich.podplay.service.ItunesService
import com.raywenderlich.podplay.viewmodel.PodcastViewModel
import com.raywenderlich.podplay.viewmodel.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.String as String

class PodcastActivity : AppCompatActivity(), PodcastListAdapter.PodcastListAdapterListener {

    private val searchViewModel by viewModels<SearchViewModel>()
    private val podcastViewModel by viewModels<PodcastViewModel>()

    private lateinit var searchMenuItem: MenuItem
    private lateinit var podcastListAdapter: PodcastListAdapter
    private lateinit var databinding: ActivityPodcastBinding

    private val TAG = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        databinding = ActivityPodcastBinding.inflate(layoutInflater)
        setContentView(databinding.root)

        val itunesService = ItunesService.instance
        val itunesRepo = ItunesRepo(itunesService)

        GlobalScope.launch {
            val results = itunesRepo.searchByTerm("Android Developer")
            Log.i(TAG, "Results = ${results.body()}")
        }

        setupToolbar()
        setupViewModels()
        updateControls()
        handleIntent(intent)
        addBackStackListener()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        //1
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_search, menu)

        //2
        searchMenuItem = menu.findItem(R.id.search_item)
        val searchView = searchMenuItem.actionView as SearchView

        //3
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        //4
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        if (supportFragmentManager.backStackEntryCount > 0) {
            databinding.podcastRecyclerView.visibility = View.VISIBLE
        }

        if (databinding.podcastRecyclerView.visibility == View.INVISIBLE) {
            searchMenuItem.isVisible = false
        }

        return true
    }

    private fun performSearch(term: String) {
        showProgressBar()
        GlobalScope.launch {
            val results = searchViewModel.searchPodcast(term)
            withContext(Dispatchers.Main) {
                hideProgressBar()
                databinding.toolbar.title = term
                podcastListAdapter.setSearchData(results)

            }
        }
    }

    private fun handleIntent(intent: Intent) {
        if(Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY) ?:
            return
            performSearch(query)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun setupToolbar() {
        setSupportActionBar(databinding.toolbar)
    }

    private fun setupViewModels() {
        val service = ItunesService.instance
        searchViewModel.itunesRepo = ItunesRepo(service)
        podcastViewModel.podcastRepo = PodcastRepo()
    }

    private fun updateControls() {
        databinding.podcastRecyclerView.setHasFixedSize(true)

        val layoutManager = LinearLayoutManager(this)
        databinding.podcastRecyclerView.layoutManager = layoutManager

        val dividerItemDecoration = DividerItemDecoration(
            databinding.podcastRecyclerView.context,
            layoutManager.orientation)

        databinding.podcastRecyclerView.addItemDecoration(dividerItemDecoration)

        podcastListAdapter = PodcastListAdapter(null, this, this)
        databinding.podcastRecyclerView.adapter = podcastListAdapter

    }

    override fun onShowDetails(podcastSummaryViewData: SearchViewModel.PodcastSummaryViewData) {
        //1
        val feedUrl = podcastSummaryViewData.feedUrl ?: return
        //2
        showProgressBar()
        //3
        val podcast = podcastViewModel.getPodcast(podcastSummaryViewData)
        //4
        hideProgressBar()
        if (podcast != null) {
            //5
            showDetailsFragment()
        } else {
            //6
            showError("Error loading feed $feedUrl")
        }
    }

    private fun showProgressBar() {
        databinding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        databinding.progressBar.visibility = View.INVISIBLE
    }

    private fun createPodcastDetailsFragment(): PodcastDetailsFragment {

        //1
        var podcastDetailsFragment = supportFragmentManager.findFragmentByTag(TAG_DETAILS_FRAGMENT) as PodcastDetailsFragment?

        //2
        if (podcastDetailsFragment == null) {
            podcastDetailsFragment = PodcastDetailsFragment.newInstance()
        }

        return podcastDetailsFragment
    }

    private fun showDetailsFragment() {
        //1
        val podcastDetailsFragment = createPodcastDetailsFragment()
        //2
        supportFragmentManager.beginTransaction().add(
            R.id.podcastDetailsContainer,
            podcastDetailsFragment, TAG_DETAILS_FRAGMENT)
            .addToBackStack("DetailsFragment").commit()
        //3
        databinding.podcastRecyclerView.visibility = View.INVISIBLE
        //4
        searchMenuItem.isVisible = false
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton(getString(R.string.ok_button), null)
            .create()
            .show()
    }

    private fun addBackStackListener() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                databinding.podcastRecyclerView.visibility = View.VISIBLE
            }
        }
    }

    companion object {
        private const val TAG_DETAILS_FRAGMENT = "DetailsFragment"
    }


}