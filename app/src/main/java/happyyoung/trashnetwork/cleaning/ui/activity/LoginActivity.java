package happyyoung.trashnetwork.cleaning.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import happyyoung.trashnetwork.cleaning.R;
import happyyoung.trashnetwork.cleaning.database.model.LoginUserRecord;
import happyyoung.trashnetwork.cleaning.net.PublicResultCode;
import happyyoung.trashnetwork.cleaning.net.http.HttpApi;
import happyyoung.trashnetwork.cleaning.net.http.HttpApiJsonListener;
import happyyoung.trashnetwork.cleaning.net.http.HttpApiJsonRequest;
import happyyoung.trashnetwork.cleaning.net.model.request.LoginRequest;
import happyyoung.trashnetwork.cleaning.net.model.result.GroupListResult;
import happyyoung.trashnetwork.cleaning.net.model.result.LoginResult;
import happyyoung.trashnetwork.cleaning.net.model.result.Result;
import happyyoung.trashnetwork.cleaning.net.model.result.TrashListResult;
import happyyoung.trashnetwork.cleaning.net.model.result.UserListResult;
import happyyoung.trashnetwork.cleaning.net.model.result.UserResult;
import happyyoung.trashnetwork.cleaning.util.DatabaseUtil;
import happyyoung.trashnetwork.cleaning.util.GlobalInfo;

/**
 * Created by shengyun-zhou <GGGZ-1101-28@Live.cn> on 2017-02-12
 */
public class LoginActivity extends AppCompatActivity {
    public static final String BUNDLE_KEY_AUTO_USER_ID = "AutoUserId";

    // UI references.
    @BindView(R.id.edit_id) AutoCompleteTextView mIdView;
    @BindView(R.id.edit_password) EditText mPasswordView;
    @BindView(R.id.login_progress) ProgressBar mProgress;
    @BindView(R.id.login_portrait) ImageView mPortraitView;
    @BindView(R.id.btn_sign_in) Button mSignInButton;

    private List<LoginUserRecord> loginRecords;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        setSupportActionBar((Toolbar) ButterKnife.findById(this, R.id.toolbar));

        loginRecords = DatabaseUtil.findAllLoginUserRecords(10);
        List<String> loginIdRecords = new ArrayList<>();
        for(LoginUserRecord lur : loginRecords)
            loginIdRecords.add(Long.toString(lur.getUserId()));
        mIdView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, loginIdRecords));
        long autoId = getIntent().getLongExtra(BUNDLE_KEY_AUTO_USER_ID, -1);
        if(autoId > 0)
            mIdView.setText(Long.toString(autoId));
    }

    @OnTextChanged(value = R.id.edit_id, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void onIdTextChanged(Editable s) {
        try{
            if(!s.toString().isEmpty()) {
                long idNum = Long.valueOf(s.toString());
                for (LoginUserRecord lur : loginRecords) {
                    if (lur.getUserId() == idNum) {
                        mPortraitView.setImageBitmap(lur.getPortrait());
                        return;
                    }
                }
            }
        }catch (NumberFormatException ignored){}
        mPortraitView.setImageResource(R.mipmap.ic_launcher);
    }

    @OnEditorAction(R.id.edit_password)
    boolean onPasswordEditAction(TextView textView, int id, KeyEvent keyEvent){
        if(id == EditorInfo.IME_ACTION_DONE) {
            attemptLogin();
            return true;
        }
        return false;
    }

    @OnClick(R.id.btn_sign_in)
    void onBtnSignInClick(View v){
        attemptLogin();
    }

    private void attemptLogin(){
        final String idNum = mIdView.getText().toString();
        String password = mPasswordView.getText().toString();
        if(idNum.isEmpty()){
            mIdView.setError(getString(R.string.error_field_required));
            return;
        }else if(password.isEmpty()){
            mPasswordView.setError(getString(R.string.error_field_required));
            return;
        }
        try{
            LoginRequest lr = new LoginRequest(Long.valueOf(idNum), password);
            mProgress.setVisibility(View.VISIBLE);
            mSignInButton.setEnabled(false);
            HttpApi.startRequest(new HttpApiJsonRequest(this, HttpApi.getApiUrl(HttpApi.AccountApi.LOGIN), Request.Method.PUT, null, lr,
                    new HttpApiJsonListener<LoginResult>(LoginResult.class) {
                        @Override
                        public boolean onDataCorrupted(Throwable e) {
                            mProgress.setVisibility(View.GONE);
                            mSignInButton.setEnabled(true);
                            return false;
                        }

                        @Override
                        public boolean onNetworkError(Throwable e) {
                            mProgress.setVisibility(View.GONE);
                            mSignInButton.setEnabled(true);
                            return false;
                        }

                        @Override
                        public void onResponse(LoginResult data) {
                            GlobalInfo.token = data.getToken();
                            afterLogin(LoginActivity.this, Long.valueOf(idNum), new HttpApiJsonListener<Result>(Result.class) {
                                @Override
                                public void onResponse(Result data) {}

                                @Override
                                public boolean onErrorResponse(int statusCode, Result errorInfo) {
                                    mProgress.setVisibility(View.GONE);
                                    mSignInButton.setEnabled(true);
                                    return false;
                                }

                                @Override
                                public boolean onDataCorrupted(Throwable e) {
                                    mProgress.setVisibility(View.GONE);
                                    mSignInButton.setEnabled(true);
                                    return false;
                                }

                                @Override
                                public boolean onNetworkError(Throwable e) {
                                    mProgress.setVisibility(View.GONE);
                                    mSignInButton.setEnabled(true);
                                    return false;
                                }
                            });
                        }

                        @Override
                        public boolean onErrorResponse(int statusCode, Result errorInfo) {
                            mProgress.setVisibility(View.GONE);
                            mSignInButton.setEnabled(true);
                            if(errorInfo.getResultCode() == PublicResultCode.LOGIN_USER_NOT_EXIST){
                                mIdView.setError(errorInfo.getMessage());
                                return true;
                            }else if (errorInfo.getResultCode() == PublicResultCode.LOGIN_INCORRECT_PASSWORD){
                                mPasswordView.setError(errorInfo.getMessage());
                                return true;
                            }
                            return false;
                        }
                    }));
        }catch (NumberFormatException nfe){
            nfe.printStackTrace();
            mIdView.setError(getString(R.string.error_illegal_id));
        }
    }

    public static void afterLogin(final Activity activity, long userId, final HttpApiJsonListener<Result> listener){
        HttpApi.startRequest(new HttpApiJsonRequest(activity, HttpApi.getApiUrl(HttpApi.AccountApi.USER_INFO_BY_ID, Long.toString(userId)), Request.Method.GET,
                GlobalInfo.token, null, new HttpApiJsonListener<UserResult>(UserResult.class) {
            @Override
            public void onResponse(UserResult data) {
                GlobalInfo.user = data.getUser();
                getContacts(activity, listener);
            }

            @Override
            public boolean onErrorResponse(int statusCode, Result errorInfo) {
                return listener.onErrorResponse(statusCode, errorInfo);
            }

            @Override
            public boolean onDataCorrupted(Throwable e) {
                return listener.onDataCorrupted(e);
            }

            @Override
            public boolean onNetworkError(Throwable e) {
                return listener.onNetworkError(e);
            }
        }));
    }

    private static void getContacts(final Activity activity, final HttpApiJsonListener<Result> listener){
        HttpApi.startRequest(new HttpApiJsonRequest(activity, HttpApi.getApiUrl(HttpApi.AccountApi.ALL_GROUP_USERS), Request.Method.GET,
                GlobalInfo.token, null, new HttpApiJsonListener<UserListResult>(UserListResult.class) {
            @Override
            public void onResponse(UserListResult data) {
                GlobalInfo.groupWorkers = data.getUserList();
                getGroups(activity, listener);
            }

            @Override
            public boolean onErrorResponse(int statusCode, Result errorInfo) {
                return listener.onErrorResponse(statusCode, errorInfo);
            }

            @Override
            public boolean onDataCorrupted(Throwable e) {
                return listener.onDataCorrupted(e);
            }

            @Override
            public boolean onNetworkError(Throwable e) {
                return listener.onNetworkError(e);
            }
        }));
    }

    private static void getGroups(final Activity activity, final HttpApiJsonListener<Result> listener){
        HttpApi.startRequest(new HttpApiJsonRequest(activity, HttpApi.getApiUrl(HttpApi.GroupApi.ALL_GROUPS), Request.Method.GET,
                GlobalInfo.token, null, new HttpApiJsonListener<GroupListResult>(GroupListResult.class) {
            @Override
            public void onResponse(GroupListResult data) {
                GlobalInfo.groupList = data.getGroupList();
                getTrashInfo(activity, listener);
            }

            @Override
            public boolean onErrorResponse(int statusCode, Result errorInfo) {
                if(errorInfo.getResultCode() == PublicResultCode.GROUP_NOT_FOUND){
                    getTrashInfo(activity, listener);
                    return true;
                }
                return listener.onErrorResponse(statusCode, errorInfo);
            }

            @Override
            public boolean onDataCorrupted(Throwable e) {
                return listener.onDataCorrupted(e);
            }

            @Override
            public boolean onNetworkError(Throwable e) {
                return listener.onNetworkError(e);
            }
        }));
    }

    private static void getTrashInfo(final Activity activity, final HttpApiJsonListener<Result> listener){
        HttpApi.startRequest(new HttpApiJsonRequest(activity, HttpApi.getApiUrl(HttpApi.PublicApi.ALL_TRASHES), Request.Method.GET,
                null, null, new HttpApiJsonListener<TrashListResult>(TrashListResult.class) {
            @Override
            public void onResponse(TrashListResult data) {
                GlobalInfo.trashList = data.getTrashList();
                enterMainActivity(activity);
            }

            @Override
            public boolean onErrorResponse(int statusCode, Result errorInfo) {
                return listener.onErrorResponse(statusCode, errorInfo);
            }

            @Override
            public boolean onDataCorrupted(Throwable e) {
                return listener.onDataCorrupted(e);
            }

            @Override
            public boolean onNetworkError(Throwable e) {
                return listener.onNetworkError(e);
            }
        }));
    }

    private static void enterMainActivity(Activity activity){
        Intent intent = new Intent(activity, MainActivity.class);
        activity.startActivity(intent);
        activity.finish();
    }
}

