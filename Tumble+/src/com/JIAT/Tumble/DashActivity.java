package com.JIAT.Tumble;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.*;
import com.google.gson.Gson;

/**
 * Created with IntelliJ IDEA.
 * User: Arsen
 * Date: 7/30/13
 * Time: 9:44 AM
 * To change this template use File | Settings | File Templates.
 */
public class DashActivity extends Activity implements View.OnClickListener, ScrollViewListener
{
    String jsonData;

    //UI Elements
    private Button refreshButton;
    private ObservableScrollView scrollView;

    public void onCreate(Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_NO_TITLE); //Placing this elsewhere will cause a crash
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dash);

        refresh();

        //Setup the listeners
        refreshButton = (Button)findViewById(R.id.refreshButton);
        refreshButton.setOnClickListener(this);
        refreshButton.setTextColor(Color.BLACK);

        scrollView = (ObservableScrollView)findViewById(R.id.scrollView);
        scrollView.setScrollViewListener(this);
    }

    //Refreshes JSON data
    private void refresh()
    {
        jsonData = OAuthHelper.OAuthRequest("http://api.tumblr.com/v2/user/dashboard", "GET", OAuthHelper.oauthAccessToken, OAuthHelper.oauthAccessSecret, "", "&reblog_info=true");


        LinearLayout dashListLayout = (LinearLayout)findViewById(R.id.dashListLayout);
        //Clear all child objects
        dashListLayout.removeAllViews();

        //Create the page object based off the JSON string
        Page page = new Gson().fromJson(jsonData, Page.class);

        //Print out all posts in the page
        Log.v("RAW JSON: ", jsonData);
        Log.v("JSON: ", page.toString());

        //Setup parameters for the post layouts
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        layoutParams.setMargins(20, 20, 20, 0);

        for(int i = 0; i < page.getPosts().size(); i++)
        {

            Post post = page.getPosts().get(i);
            String type = post.getType();

            PostLayout postLayout = null;

            //Can't run a string switch, use if/else chain
            if(type.equals("answer"))
            {
                postLayout = new AnswerPostLayout(this, post);
            }
            else if(type.equals("audio"))
            {
                postLayout = new AudioPostLayout(this, post);
            }
            else if(type.equals("chat"))
            {
                postLayout = new ChatPostLayout(this, post);
            }
            else if(type.equals("photo"))
            {
                postLayout = new PhotoPostLayout(this, post);
            }
            else if(type.equals("link"))
            {
                postLayout = new LinkPostLayout(this, post);
            }
            else if(type.equals("quote"))
            {
                postLayout = new QuotePostLayout(this, post);
            }
            else if(type.equals("text"))
            {
                postLayout = new TextPostLayout(this, post);
            }
            else if(type.equals("video"))
            {
                postLayout = new VideoPostLayout(this, post);
            }
            else
            {
                postLayout = new PostLayout(this, post);
            }

            dashListLayout.addView(postLayout, layoutParams);
        }
    }

    //Handle the refresh press here
    public void onClick(View view)
    {
        if(scrollView.getScrollY() != 0)
        {
            scrollView.smoothScrollTo(0, 0);
        }
        else
        {
            refresh();
        }
    }

    //Handle the scroll change
    public void onScrollChanged(ObservableScrollView  scrollView, int x, int y, int oldX, int oldY)
    {
        /*
        * If the user isn't at the top, change the refresh button
        * to let the user know it can go to the top
        * */

        if(y > 0)
        {
            refreshButton.setText("To Top");
        }
        else
        {
            refreshButton.setText("Refresh");
        }
    }

    //Don't wan the back button to send us to the MainActivity
    public void onBackPressed()
    {

    }

}
