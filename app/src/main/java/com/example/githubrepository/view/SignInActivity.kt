package com.example.githubrepository.view

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.isGone
import com.example.githubrepository.BuildConfig
import com.example.githubrepository.databinding.ActivitySignInBinding
import kevin.exam.github.utillity.AuthTokenProvider
import kevin.exam.github.utillity.RetrofitUtil
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class SignInActivity : AppCompatActivity(), CoroutineScope {

    private lateinit var binding: ActivitySignInBinding
    val job: Job = Job()

    private val authTokenProvider by lazy { AuthTokenProvider(this) }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(checkAuthCodeExist()){
            launchMainActivity()
        }else {
            initViews()
        }
    }

    private fun initViews() = with(binding) {
        btnLogin.setOnClickListener {
            loginGithub()
        }
    }

    private fun launchMainActivity() {
        startActivity(Intent(this@SignInActivity, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun checkAuthCodeExist(): Boolean = authTokenProvider.token.isNullOrEmpty().not()

    private fun loginGithub(){
        val loginUri = Uri.Builder().scheme("https").authority("github.com")
            .appendPath("login")
            .appendPath("oauth")
            .appendPath("authorize")
            .appendQueryParameter("client_id", BuildConfig.GITHUB_CLIENT_ID)
            .build()
        CustomTabsIntent.Builder().build().also {
            it.launchUrl(this, loginUri)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        intent?.data?.getQueryParameter("code")?.let {
            launch(coroutineContext) {
                showProgress()
                getAccessToken(it)
                // getAccessToken()이 토큰 값(it)을 받아올 때 까지 suspend(중지) 됨.
                dismissProgress()
            }
        }
    }

    private suspend fun showProgress() = withContext(coroutineContext){
        with(binding){
            btnLogin.isGone = true
            progressBar.isGone = false
            txtLoading.isGone = false
        }
    }

    private suspend fun dismissProgress() = withContext(coroutineContext){
        with(binding){
            btnLogin.isGone = false
            progressBar.isGone = true
            txtLoading.isGone = true
        }
    }

    private suspend fun getAccessToken(code: String) = withContext(Dispatchers.IO){
        val response = RetrofitUtil.authApiService.getAccessToken(
            clientId = BuildConfig.GITHUB_CLIENT_ID,
            clientSecret = BuildConfig.GITHUB_CLIENT_SECRET,
            code = code
        )
        if(response.isSuccessful){
            val accessToken = response.body()?.accessToken ?: ""
            Log.e("accessToken", accessToken)
            if(accessToken.isNotEmpty()){
                authTokenProvider.updateToken(accessToken)
            } else{
                Toast.makeText(this@SignInActivity, "accessToken 값이 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}