package com.appchefs.quoty.main.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.appchefs.quoty.R
import com.appchefs.quoty.databinding.ActivityMainBinding
import com.appchefs.quoty.main.base.BaseActivity
import com.appchefs.quoty.main.viewmodel.MainViewModel
import com.appchefs.quoty.utils.NetworkUtils
import com.appchefs.quoty.utils.Status
import com.appchefs.quoty.worker.NotificationWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
@AndroidEntryPoint
class MainActivity : BaseActivity<MainViewModel, ActivityMainBinding>() {

    private val TAG = "MainActivity"
    override val mViewModel: MainViewModel by viewModels()
    private var currentTag: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mViewBinding.root)
        clickEvents()
        setupObservers()
        Log.i(TAG, "On created Called")
    }

    private fun startNotificationWorK() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicNotificationWorkRequest =
            PeriodicWorkRequestBuilder<NotificationWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

        WorkManager.getInstance(applicationContext)
            .enqueue(periodicNotificationWorkRequest)

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(periodicNotificationWorkRequest.id)
            .observe(this, { workInfo ->
                if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                    Log.i("WorkStatus", "Success")
                } else {
                    Log.i("WorkStatus", "Error")
                }
            })
    }


    private fun setupObservers() {
        getRandomQuoteObserver()
        getQuoteObserver()
    }

    override fun getViewBinding(): ActivityMainBinding = ActivityMainBinding.inflate(layoutInflater)

    override fun onStart() {
        super.onStart()
        networkCheck()
        startNotificationWorK()
    }

    override fun onResume() {
        super.onResume()
        loadRandomQuoteByDefault()
    }

    private fun loadRandomQuoteByDefault() {
        mViewBinding.btnToggleGroup.check(R.id.btn_random)
        mViewModel.getRandomQuote()
    }

    private fun networkCheck() {
        NetworkUtils.getNetworkLiveData(applicationContext).observe(this) { isConnected ->
            if (!isConnected) {
                mViewBinding.textViewNetworkStatus.text =
                    getString(R.string.network_status_no_connections)
                mViewBinding.networkStatusLayout.apply {
                    show()
                    setBackgroundColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.networkNotAvailable
                        )
                    )
                }
            } else {
                if (mViewModel.randomQuote.value is Status.Error) {
                    loadRandomQuoteByDefault()
                    mViewBinding.btnToggleGroup.check(R.id.btn_random)
                }

                mViewBinding.textViewNetworkStatus.text = getString(R.string.network_status_online)
                mViewBinding.networkStatusLayout.apply {
                    setBackgroundColor(
                        ContextCompat.getColor(
                            applicationContext,
                            R.color.networkConnected
                        )
                    )

                    animate()
                        .alpha(1f)
                        .setStartDelay(1000L)
                        .setDuration(1000L)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                hide()
                            }
                        })
                }
            }
        }
    }

    private fun clickEvents() {
        mViewBinding.btnRandom.setOnClickListener {
            currentTag = "random"
            mViewModel.getRandomQuote()
        }

        mViewBinding.btnWisdom.setOnClickListener {
            currentTag = "wisdom"
            mViewModel.getQuote("wisdom")
        }


        mViewBinding.btnLife.setOnClickListener {
            currentTag = "life"
            mViewModel.getQuote("life")
        }


        mViewBinding.btnTech.setOnClickListener {
            currentTag = "technology"
            mViewModel.getQuote("technology")
        }

        mViewBinding.fabNewQuote.setOnClickListener {
            when (currentTag) {
                "random" -> mViewModel.getRandomQuote()
                "wisdom" -> mViewModel.getQuote("wisdom")
                "life" -> mViewModel.getQuote("life")
                "technology" -> mViewModel.getQuote("technology")
                else -> mViewModel.getRandomQuote()
            }
        }

    }

    private fun getRandomQuoteObserver() {
        mViewModel.randomQuote.observe(this, { state ->
            when (state) {
                is Status.Error -> {
                    showToast(state.message)
                }
                is Status.Success -> {
                    mViewBinding.tvQuote.text = state.data?.quoteContent ?: "Loading"
                    mViewBinding.tvAuthor.text = state.data?.author ?: "..."
                }
                is Status.Loading -> {
                    mViewBinding.tvQuote.text = getString(R.string.toast_msg_loading)
                    mViewBinding.tvAuthor.text = "..."
                }
                else -> {
                    mViewBinding.tvQuote.text = getString(R.string.toast_msg_loading)
                    mViewBinding.tvAuthor.text = "..."
                }
            }
        })
    }

    private fun getQuoteObserver() {
        mViewModel.quote.observe(this, { state ->
            when (state) {
                is Status.Error -> {
                    showToast(state.message)
                }
                is Status.Success -> {
                    mViewBinding.tvQuote.text = state.data?.quoteContent ?: "loading"
                    mViewBinding.tvAuthor.text = state.data?.author ?: "..."
                }
                is Status.Loading -> {
                    mViewBinding.tvQuote.text = getString(R.string.toast_msg_loading)
                    mViewBinding.tvAuthor.text = "..."
                }
                else -> {
                    mViewBinding.tvQuote.text = getString(R.string.toast_msg_loading)
                    mViewBinding.tvAuthor.text = "..."
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.theme_menu_icon -> {
                // Get new mode.
                val mode =
                    if ((resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_NO
                    ) {
                        // Dark Theme by default
                        AppCompatDelegate.MODE_NIGHT_YES
                    } else {
                        // uses dark theme when the Phone is in Battery Saver mode.
                        AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
                    }

                // Change UI Mode
                AppCompatDelegate.setDefaultNightMode(mode)
                //TODO: Change the Icon.
                true
            }

            R.id.saved_item_icon -> {
                startActivity(Intent(this, AllQuotesActivity::class.java))
                true
            }

            else -> true
        }
    }


    fun View.show() {
        visibility = View.VISIBLE
    }

    fun View.hide() {
        visibility = View.GONE
    }

    fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}