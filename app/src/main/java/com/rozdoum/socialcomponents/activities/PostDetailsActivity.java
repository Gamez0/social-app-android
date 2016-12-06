package com.rozdoum.socialcomponents.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.rozdoum.socialcomponents.ApplicationHelper;
import com.rozdoum.socialcomponents.Bootstrap;
import com.rozdoum.socialcomponents.R;
import com.rozdoum.socialcomponents.adapters.CommentsAdapter;
import com.rozdoum.socialcomponents.enums.ProfileStatus;
import com.rozdoum.socialcomponents.managers.ProfileManager;
import com.rozdoum.socialcomponents.managers.listeners.OnCountChangedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnDataChangedListener;
import com.rozdoum.socialcomponents.managers.listeners.OnObjectExistListener;
import com.rozdoum.socialcomponents.model.Comment;
import com.rozdoum.socialcomponents.model.Like;
import com.rozdoum.socialcomponents.model.Post;
import com.rozdoum.socialcomponents.utils.ImageUtil;

import java.util.List;

public class PostDetailsActivity extends BaseActivity {

    public static final String POST_EXTRA_KEY = "PostDetailsActivity.POST_EXTRA_KEY";
    private static final int ANIMATION_DURATION = 300;

    private EditText commentEditText;
    private Post post;
    private ScrollView scrollView;
    private ViewGroup likesContainer;
    private ImageView likesImageView;
    private TextView commentsLabel;
    private TextView likeCounterTextView;

    private AnimationType likeAnimationType;
    private boolean isLiked = false;
    private boolean likeIconInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_details);
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        post = (Post) getIntent().getSerializableExtra(POST_EXTRA_KEY);

        TextView titleTextView = (TextView) findViewById(R.id.titleTextView);
        TextView descriptionEditText = (TextView) findViewById(R.id.descriptionEditText);
        ImageView postImageView = (ImageView) findViewById(R.id.postImageView);
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
        LinearLayout commentsContainer = (LinearLayout) findViewById(R.id.commentsContainer);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        commentsLabel = (TextView) findViewById(R.id.commentsLabel);
        commentEditText = (EditText) findViewById(R.id.commentEditText);
        ImageButton sendButton = (ImageButton) findViewById(R.id.sendButton);
        likesContainer = (ViewGroup) findViewById(R.id.likesContainer);
        likesImageView = (ImageView) findViewById(R.id.likesImageView);
        likeCounterTextView = (TextView) findViewById(R.id.likeCounterTextView);


        titleTextView.setText(post.getTitle());
        descriptionEditText.setText(post.getDescription());

        String imageUrl = post.getImagePath();

        ImageUtil imageUtil = ImageUtil.getInstance(this);
        imageUtil.getImage(imageUrl, postImageView, progressBar, R.drawable.ic_stub, R.drawable.ic_stub);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileStatus profileStatus = ProfileManager.getInstance(PostDetailsActivity.this).checkProfile();

                if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
                    sendComment();
                } else {
                    doAuthorization(profileStatus);
                }
            }
        });

        CommentsAdapter commentsAdapter = new CommentsAdapter(commentsContainer);
        ApplicationHelper.getDatabaseHelper().getCommentsList(post.getId(), createOnPostChangedDataListener(commentsAdapter));

        postImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImageDetailScreen();
            }
        });

        initLikes();
    }

    private OnDataChangedListener<Comment> createOnPostChangedDataListener(final CommentsAdapter commentsAdapter) {
        return new OnDataChangedListener<Comment>() {
            @Override
            public void onListChanged(List<Comment> list) {
                commentsAdapter.setList(list);
                if (list.size() > 0) {
                    commentsLabel.setVisibility(View.VISIBLE);
                    commentsLabel.setText(String.format(getString(R.string.label_comments), list.size()));
                } else {
                    commentsLabel.setVisibility(View.GONE);
                }
            }
        };
    }

    private OnCountChangedListener<Like> createOnLikeCountChangedListener() {
        return new OnCountChangedListener<Like>() {
            @Override
            public void onCountChanged(long count) {
                String likeTextFormat = getString(R.string.label_likes);
                likeCounterTextView.setText(String.format(likeTextFormat, count));
            }
        };
    }

    private void openImageDetailScreen() {
        Intent intent = new Intent(this, ImageDetailActivity.class);
        intent.putExtra(ImageDetailActivity.IMAGE_URL_EXTRA_KEY, post.getImagePath());
        startActivity(intent);
    }

    private OnObjectExistListener<Like> createOnLikeObjectExistListener() {
        return new OnObjectExistListener<Like>() {
            @Override
            public void onDataChanged(boolean exist) {
                if (!likeIconInitialized) {
                    likesImageView.setImageResource(exist ? R.drawable.ic_favorite_24px : R.drawable.ic_favorite_border_24px);
                    likeIconInitialized = true;
                }

                isLiked = exist;
            }
        };
    }

    private void initLikes() {
        ApplicationHelper.getDatabaseHelper().hasCurrentUserLike(post.getId(), Bootstrap.USER_ID, createOnLikeObjectExistListener());

        //set default animation type
        likeAnimationType = AnimationType.BOUNCE_ANIM;

        ApplicationHelper.getDatabaseHelper().getLikesCount(post.getId(), createOnLikeCountChangedListener());

        likesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileStatus profileStatus = ProfileManager.getInstance(PostDetailsActivity.this).checkProfile();

                if (profileStatus.equals(ProfileStatus.PROFILE_CREATED)) {
                    likeClickAction();
                } else {
                    doAuthorization(profileStatus);
                }
            }
        });

        //long click for changing animation
        likesContainer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (likeAnimationType == AnimationType.BOUNCE_ANIM) {
                    likeAnimationType = AnimationType.COLOR_ANIM;
                } else {
                    likeAnimationType = AnimationType.BOUNCE_ANIM;
                }

                Snackbar snackbar = Snackbar
                        .make(likesContainer, "Animation was changed", Snackbar.LENGTH_LONG);

                snackbar.show();
                return true;
            }
        });
    }

    private void likeClickAction() {
        startAnimateLikeButton(likeAnimationType);

        if (!isLiked) {
            addLike();
        } else {
            removeLike();
        }
    }

    private void startAnimateLikeButton(AnimationType animationType) {
        switch (animationType) {
            case BOUNCE_ANIM:
                bounceAnimateImageView();
                break;
            case COLOR_ANIM:
                colorAnimateImageView();
                break;
        }
    }

    public void colorAnimateImageView() {
        final int activatedColor = getResources().getColor(R.color.like_icon_activated);

        final ValueAnimator colorAnim = !isLiked ? ObjectAnimator.ofFloat(0f, 1f)
                : ObjectAnimator.ofFloat(1f, 0f);
        colorAnim.setDuration(ANIMATION_DURATION);
        colorAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float mul = (Float) animation.getAnimatedValue();
                int alpha = adjustAlpha(activatedColor, mul);
                likesImageView.setColorFilter(alpha, PorterDuff.Mode.SRC_ATOP);
                if (mul == 0.0) {
                    likesImageView.setColorFilter(null);
                }
            }
        });

        colorAnim.start();
    }

    public void bounceAnimateImageView() {
        AnimatorSet animatorSet = new AnimatorSet();

        ObjectAnimator bounceAnimX = ObjectAnimator.ofFloat(likesImageView, "scaleX", 0.2f, 1f);
        bounceAnimX.setDuration(ANIMATION_DURATION);
        bounceAnimX.setInterpolator(new BounceInterpolator());

        ObjectAnimator bounceAnimY = ObjectAnimator.ofFloat(likesImageView, "scaleY", 0.2f, 1f);
        bounceAnimY.setDuration(ANIMATION_DURATION);
        bounceAnimY.setInterpolator(new BounceInterpolator());
        bounceAnimY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                likesImageView.setImageResource(!isLiked ? R.drawable.ic_favorite_24px
                        : R.drawable.ic_favorite_border_24px);
            }
        });

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
            }
        });

        animatorSet.play(bounceAnimX).with(bounceAnimY);
        animatorSet.start();
    }

    public int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private void addLike() {
        ApplicationHelper.getDatabaseHelper().createOrUpdateLike(post.getId(), Bootstrap.USER_ID);
    }

    private void removeLike() {
        ApplicationHelper.getDatabaseHelper().removeLike(post.getId(), Bootstrap.USER_ID);
    }

    private void sendComment() {
        String commentText = commentEditText.getText().toString();

        if (commentText.length() > 0) {
            ApplicationHelper.getDatabaseHelper().createOrUpdateComment(commentText, post.getId());
            commentEditText.setText(null);
            commentEditText.clearFocus();
            hideKeyBoard();
            scrollView.fullScroll(ScrollView.FOCUS_UP);
        }
    }

    private void hideKeyBoard() {
        // Check if no view has focus:
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    enum AnimationType {
        COLOR_ANIM, BOUNCE_ANIM
    }
}
